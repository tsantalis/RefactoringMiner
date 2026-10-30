"""Test doubles shared by the suite. Nothing here touches the network."""

import json
from collections.abc import Sequence
from types import SimpleNamespace
from typing import Final
from unittest.mock import AsyncMock, MagicMock

import httpx

from app.github.signatures import compute_signature
from app.providers.base import ChatMessage, Completion, ReasoningEffort, TokenUsage
from app.providers.errors import LLMError
from app.providers.openrouter import OpenRouterProvider

TEST_API_KEY: Final = "sk-or-v1-test-not-a-real-key"
TEST_MODEL: Final = "cohere/north-mini-code:free"
TEST_PROVIDER: Final = "Cohere"
GENERATION_ID: Final = "gen-1759500000-abc123"
REPLY_TEXT: Final = "Clear title. Add a test for the empty-input case before merging."


def openrouter_reply(
    content: str | None = REPLY_TEXT, *, finish_reason: str | None = "stop"
) -> dict[str, object]:
    """A successful Chat Completions body, shaped like OpenRouter's API reference."""
    return {
        "id": GENERATION_ID,
        "object": "chat.completion",
        "created": 1_759_500_000,
        "model": TEST_MODEL,
        "provider": TEST_PROVIDER,
        "system_fingerprint": None,
        "choices": [
            {
                "index": 0,
                "finish_reason": finish_reason,
                "message": {"role": "assistant", "content": content},
            }
        ],
        "usage": {
            "prompt_tokens": 58,
            "completion_tokens": 412,
            "total_tokens": 470,
            "completion_tokens_details": {"reasoning_tokens": 371},
            "cost": 0,
        },
    }


def openrouter_error(code: int, message: str) -> dict[str, object]:
    """An OpenRouter error body."""
    return {"error": {"code": code, "message": message}}


class ScriptedTransport:
    """``httpx.MockTransport`` handler that plays scripted outcomes and records requests.

    Once the script runs out the last outcome repeats, so ``ScriptedTransport(r429)``
    answers 429 forever.
    """

    def __init__(self, *outcomes: httpx.Response | httpx.HTTPError) -> None:
        self._outcomes = list(outcomes)
        self.requests: list[httpx.Request] = []

    def __call__(self, request: httpx.Request) -> httpx.Response:
        self.requests.append(request)
        outcome = self._outcomes.pop(0) if len(self._outcomes) > 1 else self._outcomes[0]
        if isinstance(outcome, httpx.HTTPError):
            raise outcome
        # A fresh copy each time, because httpx binds a response to the request it answers.
        return httpx.Response(outcome.status_code, headers=outcome.headers, content=outcome.content)


class RecordingSleep:
    """Stands in for ``asyncio.sleep`` so retry tests run instantly and can check delays."""

    def __init__(self) -> None:
        self.delays: list[float] = []

    async def __call__(self, seconds: float) -> None:
        self.delays.append(seconds)


def make_openrouter(
    transport: ScriptedTransport,
    *,
    max_retries: int = 2,
    sleep: RecordingSleep | None = None,
    reasoning_effort: ReasoningEffort | None = None,
    fallback_models: Sequence[str] = (),
    fallback_reasoning_effort: ReasoningEffort | None = None,
) -> OpenRouterProvider:
    """An ``OpenRouterProvider`` whose HTTP calls go to ``transport``, not the network."""
    return OpenRouterProvider(
        api_key=TEST_API_KEY,
        model=TEST_MODEL,
        app_url="https://github.com/Pogut/NikolAI",
        app_title="NikolAI",
        max_retries=max_retries,
        reasoning_effort=reasoning_effort,
        fallback_models=fallback_models,
        fallback_reasoning_effort=fallback_reasoning_effort,
        transport=httpx.MockTransport(transport),
        sleep=sleep or RecordingSleep(),
    )


def sample_completion(content: str = REPLY_TEXT) -> Completion:
    return Completion(
        generation_id=GENERATION_ID,
        model=TEST_MODEL,
        provider=TEST_PROVIDER,
        content=content,
        finish_reason="stop",
        usage=TokenUsage(
            prompt_tokens=58,
            completion_tokens=412,
            total_tokens=470,
            reasoning_tokens=371,
            cost_usd=0.0,
        ),
        latency_ms=1234.0,
        attempts=1,
    )


class FakeProvider:
    """``LLMProvider`` double: returns a canned completion (or raises) and records calls."""

    def __init__(
        self, completion: Completion | None = None, *, error: LLMError | None = None
    ) -> None:
        self.completion = completion or sample_completion()
        self.error = error
        self.calls: list[list[ChatMessage]] = []
        self.max_tokens: list[int] = []
        self.closed = False

    async def complete(
        self,
        messages: Sequence[ChatMessage],
        *,
        max_tokens: int,
        temperature: float | None = None,
        model: str | None = None,
    ) -> Completion:
        self.calls.append(list(messages))
        self.max_tokens.append(max_tokens)
        if self.error is not None:
            raise self.error
        return self.completion

    async def aclose(self) -> None:
        self.closed = True


# ---- GitHub App (US-2) ------------------------------------------------------------------

WEBHOOK_SECRET: Final = "test-webhook-secret"
INSTALLATION_ID: Final = 42
REPO_OWNER: Final = "octocat"
REPO_NAME: Final = "demo"
PR_NUMBER: Final = 7
PR_TITLE: Final = "Add feature"


def pull_request_payload(
    action: str = "opened",
    *,
    body: str | None = None,
    draft: bool = False,
    author_type: str = "User",
) -> dict[str, object]:
    """A ``pull_request`` webhook payload with the fields NikolAI reads, plus an extra one."""
    return {
        "action": action,
        "installation": {"id": INSTALLATION_ID},
        "sender": {"id": 20, "login": "maintainer", "type": "User"},
        "repository": {
            "id": 100,
            "name": REPO_NAME,
            "full_name": f"{REPO_OWNER}/{REPO_NAME}",
            "owner": {"login": REPO_OWNER},
            "private": True,  # unknown fields must be ignored
        },
        "pull_request": {
            "id": 200,
            "number": PR_NUMBER,
            "title": PR_TITLE,
            "state": "closed" if action == "closed" else "open",
            "created_at": "2026-10-01T10:00:00Z",
            "updated_at": "2026-10-02T12:00:00Z",
            "merged_at": None,
            "merged": False,
            "body": body,
            "draft": draft,
            "user": {"id": 10, "login": "student1", "type": author_type},
        },
    }


def signed(payload: dict[str, object], secret: str = WEBHOOK_SECRET) -> tuple[bytes, str]:
    """The raw body GitHub would send for ``payload``, and its X-Hub-Signature-256 header."""
    body = json.dumps(payload).encode("utf-8")
    return body, compute_signature(secret, body)


def make_fake_github() -> MagicMock:
    """Stands in for the githubkit installation client (ported from Team Alpha's US-2 tests).

    It answers the PR commit and file listings, and records ``async_create_comment`` calls.
    """
    github = MagicMock()
    rest = github.rest
    rest.pulls.async_list_commits = AsyncMock(
        return_value=SimpleNamespace(
            parsed_data=[SimpleNamespace(commit=SimpleNamespace(message="fix bug"))]
        )
    )
    rest.pulls.async_list_files = AsyncMock(
        return_value=SimpleNamespace(
            parsed_data=[
                SimpleNamespace(
                    filename="a.py", additions=3, deletions=1, patch="@@ -1 +1 @@\n-x\n+y"
                )
            ]
        )
    )
    rest.issues.async_list_comments = AsyncMock(return_value=SimpleNamespace(parsed_data=[]))
    rest.issues.async_create_comment = AsyncMock(
        return_value=SimpleNamespace(
            parsed_data=SimpleNamespace(
                id=12345, html_url="https://github.com/octocat/demo/issues/7#issuecomment-12345"
            )
        )
    )
    rest.issues.async_delete_comment = AsyncMock()
    return github
