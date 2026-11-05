"""Operator commands, and a way to try the US-3 stub agent against the real model.

Grant someone the instructor flag (ADR 0001); NikolAI must already know their login:

    python -m app.cli grant-instructor <github-login>

Run the stub agent from ``backend/`` once ``.env`` holds an OpenRouter key (see README.md):

    uv run python -m app.cli "Give feedback on this PR title: fix stuff"
    git diff main | uv run python -m app.cli -          # prompt read from stdin
    uv run python -m app.cli --runs 5 "Hello"           # latency over several calls
    uv run python -m app.cli --model anthropic/claude-haiku-4.5 "Hello"
    uv run python -m app.cli --json "Hello"             # results as JSON
"""

# AI contribution: 50%

import argparse
import asyncio
import io
import json
import logging
import statistics
import sys
from collections.abc import Callable, Sequence

from psycopg import Error as DatabaseError

from app.agents.base import AgentContext, AgentResult
from app.agents.stub import StubAgent
from app.core.config import Settings, get_settings
from app.db import connect
from app.db.repositories.users import InstructorGrantError, grant_instructor
from app.providers.base import LLMProvider
from app.providers.errors import LLMError
from app.providers.factory import create_provider

ProviderFactory = Callable[[Settings], LLMProvider]


def _positive_int(value: str) -> int:
    number = int(value)
    if number < 1:
        raise argparse.ArgumentTypeError("must be 1 or more")
    return number


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="python -m app.cli",
        description="Send a prompt to the NikolAI stub agent and print its reply and latency.",
        epilog="Operators: python -m app.cli grant-instructor <github-login>",
    )
    parser.add_argument("prompt", help="the prompt, or '-' to read it from stdin")
    parser.add_argument(
        "--runs", type=_positive_int, default=1, help="number of calls to make (default: 1)"
    )
    parser.add_argument(
        "--model",
        help="OpenRouter model slug, overriding OPENROUTER_MODEL (no fallback models are tried)",
    )
    parser.add_argument("--json", action="store_true", help="print the results as JSON")
    parser.add_argument("-v", "--verbose", action="store_true", help="log every call and retry")
    return parser


async def run_stub_agent(
    context: AgentContext, provider: LLMProvider, runs: int
) -> list[AgentResult]:
    """Run the stub agent ``runs`` times in a row, then close the provider."""
    agent = StubAgent(provider)
    try:
        return [await agent.run(context) for _ in range(runs)]
    finally:
        await provider.aclose()


def format_result(result: AgentResult) -> str:
    """Human-readable summary of one result, followed by the model's reply."""
    tokens = cost = "not reported"
    if result.usage is not None:
        usage = result.usage
        tokens = f"{usage.prompt_tokens:,} in / {usage.completion_tokens:,} out"
        if usage.reasoning_tokens:
            tokens += f" ({usage.reasoning_tokens:,} of them reasoning)"
        if usage.cost_usd is not None:
            cost = f"${usage.cost_usd:.6f}"
    return "\n".join(
        [
            f"Model       {result.model} (provider: {result.provider or 'not reported'})",
            f"Latency     {result.latency_ms:,.0f} ms",
            f"Tokens      {tokens}",
            f"Cost        {cost}",
            f"Finish      {result.finish_reason}",
            f"Generation  {result.generation_id}",
            "-" * 72,
            result.content,
        ]
    )


def format_latency_summary(results: Sequence[AgentResult]) -> str:
    """Per-run latencies plus min / median / max, ready to paste into the US-3 issue."""
    latencies = [result.latency_ms for result in results]
    lines = [f"Run {i}: {ms:,.0f} ms" for i, ms in enumerate(latencies, start=1)]
    lines.append(
        f"Latency over {len(latencies)} runs: min {min(latencies):,.0f} ms, "
        f"median {statistics.median(latencies):,.0f} ms, max {max(latencies):,.0f} ms"
    )
    return "\n".join(lines)


async def _grant_instructor(database_url: str, github_login: str) -> None:
    async with await connect(database_url) as connection:
        await grant_instructor(connection, github_login)


def run_grant_instructor(argv: Sequence[str], settings: Settings) -> int:
    """``grant-instructor <github-login>``. Returns the process exit code."""
    parser = argparse.ArgumentParser(
        prog="python -m app.cli grant-instructor",
        description="Allow a known user to create courses.",
    )
    parser.add_argument("github_login")
    login = parser.parse_args(argv).github_login
    database_url = settings.database_url.get_secret_value() if settings.database_url else ""
    if not database_url.strip():
        print("error: set DATABASE_URL first", file=sys.stderr)
        return 1
    try:
        asyncio.run(_grant_instructor(database_url, login))
    except InstructorGrantError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    except (DatabaseError, OSError) as error:
        # Only the type: psycopg's message can echo the whole URL, password included.
        print(f"error: database request failed ({type(error).__name__})", file=sys.stderr)
        return 1
    print(f"{login} is now an instructor")
    return 0


def main(
    argv: Sequence[str] | None = None,
    *,
    settings: Settings | None = None,
    provider_factory: ProviderFactory = create_provider,
) -> int:
    """Command-line entry point. Returns the process exit code."""
    arguments = sys.argv[1:] if argv is None else argv
    if arguments[:1] == ["grant-instructor"]:
        return run_grant_instructor(arguments[1:], settings or get_settings())
    args = build_parser().parse_args(arguments)
    _use_utf8_output()
    logging.basicConfig(
        level=logging.INFO if args.verbose else logging.WARNING,
        format="%(levelname)s %(name)s: %(message)s",
    )
    prompt = sys.stdin.read() if args.prompt == "-" else args.prompt
    if not prompt.strip():
        print("error: the prompt is empty", file=sys.stderr)
        return 2

    settings = settings or get_settings()
    if args.model:
        # One model, no backups: otherwise a failing model would quietly be measured as another.
        settings = settings.model_copy(
            update={"openrouter_model": args.model, "openrouter_fallback_models": []}
        )
    try:
        provider = provider_factory(settings)
        results = asyncio.run(run_stub_agent(AgentContext(prompt=prompt), provider, args.runs))
    except LLMError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1

    if args.json:
        print(json.dumps([result.model_dump(mode="json") for result in results], indent=2))
    else:
        print(format_result(results[0]))
        if len(results) > 1:
            print("-" * 72)
            print(format_latency_summary(results))
    return 0


def _use_utf8_output() -> None:
    """Model replies often contain characters that a Windows code page cannot encode."""
    for stream in (sys.stdout, sys.stderr):
        if isinstance(stream, io.TextIOWrapper) and stream.encoding.lower() != "utf-8":
            stream.reconfigure(encoding="utf-8", errors="replace")


if __name__ == "__main__":
    raise SystemExit(main())
