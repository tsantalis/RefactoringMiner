"""Test doubles shared by the suite. Nothing here touches the network."""

# AI contribution: 50%

import asyncio
import json
from collections.abc import Sequence
from types import SimpleNamespace
from typing import Final, cast
from unittest.mock import AsyncMock, MagicMock

import httpx

from app.agents.code_review import SYSTEM_PROMPT as CODE_REVIEW_PROMPT
from app.agents.fix_check import SYSTEM_PROMPT as FIX_CHECK_PROMPT
from app.agents.static_findings import SYSTEM_PROMPT as STATIC_FINDINGS_PROMPT
from app.agents.story_review import SYSTEM_PROMPT as STORY_REVIEW_PROMPT
from app.db.models.static_findings import NewStaticAnalysisRun, StaticFinding
from app.github.signatures import compute_signature
from app.providers.base import ChatMessage, Completion, ReasoningEffort, TokenUsage
from app.providers.errors import LLMError
from app.providers.openrouter import OpenRouterProvider

TEST_API_KEY: Final = "sk-or-v1-test-not-a-real-key"
TEST_MODEL: Final = "cohere/north-mini-code:free"
TEST_PROVIDER: Final = "Cohere"
GENERATION_ID: Final = "gen-1759500000-abc123"
NO_FINDINGS: Final = '{"findings": []}'
STORY_REVIEW: Final = (
    '{"score": 8, "max_score": 10, "checks": ['
    '{"text": "The story sentence is present", "passed": true},'
    '{"text": "Acceptance criteria are specific", "passed": true},'
    '{"text": "The goal is measurable", "passed": false}],'
    ' "current_text": null, "suggested_text": null}'
)
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
    """``LLMProvider`` double: returns canned completions (or raises) and records calls.

    The code-review agent's calls (recognised by its system prompt) get ``code_review``, by
    default a reply with no findings, and are recorded in ``code_review_calls``; the fix-check
    agent's get ``fix_check`` (nothing fixed, nothing new) in ``fix_check_calls``; the
    story-review agent's get ``story_review`` in ``story_review_calls``; the
    static-findings agent's get ``static_findings`` (nothing explained) in
    ``static_findings_calls``; every other call gets ``completion`` and is recorded in ``calls``.
    ``error`` is raised on every call, ``code_review_error`` only on the code review's, as when the
    second request hits a 429, ``static_findings_error`` only on the static findings', and
    ``story_review_error`` only on the story review's. ``story_review_gate`` holds a story-review
    call until a test sets it, so two checks of the same issue can race deterministically.
    """

    # Wider than the constructor's: a failure test can raise anything a model call
    # could raise, not just an LLMError.
    error: Exception | None
    code_review_error: Exception | None
    story_review_error: Exception | None
    static_findings_error: Exception | None

    def __init__(
        self,
        completion: Completion | None = None,
        *,
        error: LLMError | None = None,
        code_review: Completion | None = None,
        code_review_error: LLMError | None = None,
        fix_check: Completion | None = None,
        story_review: Completion | None = None,
        story_review_error: LLMError | None = None,
        static_findings: Completion | None = None,
        static_findings_error: LLMError | None = None,
    ) -> None:
        self.completion = completion or sample_completion()
        self.code_review = code_review or sample_completion(NO_FINDINGS)
        self.fix_check = fix_check or sample_completion('{"fixed": [], "findings": []}')
        self.story_review = story_review or sample_completion(STORY_REVIEW)
        self.static_findings = static_findings or sample_completion('{"explanations": []}')
        self.fix_check_calls: list[list[ChatMessage]] = []
        self.story_review_calls: list[list[ChatMessage]] = []
        self.static_findings_calls: list[list[ChatMessage]] = []
        self.error = error
        self.code_review_error = code_review_error
        self.story_review_error = story_review_error
        self.static_findings_error = static_findings_error
        # A concurrency test can hold the story-review call on this gate until it decides.
        self.story_review_gate: asyncio.Event | None = None
        self.calls: list[list[ChatMessage]] = []
        self.code_review_calls: list[list[ChatMessage]] = []
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
        is_code_review = messages[0].content == CODE_REVIEW_PROMPT
        if messages[0].content == FIX_CHECK_PROMPT:
            self.fix_check_calls.append(list(messages))
            if self.error is not None:
                raise self.error
            return self.fix_check
        if messages[0].content == STORY_REVIEW_PROMPT:
            self.story_review_calls.append(list(messages))
            if self.error is not None:
                raise self.error
            if self.story_review_error is not None:
                raise self.story_review_error
            if self.story_review_gate is not None:
                await self.story_review_gate.wait()
            return self.story_review
        if messages[0].content == STATIC_FINDINGS_PROMPT:
            return self._static_findings(messages)
        (self.code_review_calls if is_code_review else self.calls).append(list(messages))
        self.max_tokens.append(max_tokens)
        if self.error is not None:
            raise self.error
        if is_code_review and self.code_review_error is not None:
            raise self.code_review_error
        return self.code_review if is_code_review else self.completion

    def _static_findings(self, messages: Sequence[ChatMessage]) -> Completion:
        self.static_findings_calls.append(list(messages))
        if self.error is not None:
            raise self.error
        if self.static_findings_error is not None:
            raise self.static_findings_error
        return self.static_findings

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
    head_sha: str | None = None,
) -> dict[str, object]:
    """A ``pull_request`` webhook payload with the fields NikolAI reads, plus an extra one.

    ``head_sha`` adds the PR's head commit, which fix tracking needs to follow a push.
    """
    payload: dict[str, object] = {
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
    if head_sha is not None:
        cast("dict[str, object]", payload["pull_request"])["head"] = {"sha": head_sha}
    return payload


REFACTORING_WORKFLOW_RUN_ID: Final = 900
HEAD_SHA: Final = "0123456789abcdef0123456789abcdef01234567"

ISSUE_NUMBER: Final = 12
STORY_TITLE: Final = "US-12: Check my user story's format and clarity"
STORY_BODY: Final = (
    "As a student, I want NikolAI to check each user story I write for format and clarity, "
    "so that my team's stories follow the course's rules.\n\n"
    "### Story points\n\n8\n"
)


def issues_payload(
    action: str = "opened",
    *,
    title: str = STORY_TITLE,
    body: str | None = STORY_BODY,
    labels: tuple[str, ...] = ("user story",),
    issue_number: int = ISSUE_NUMBER,
    issue_state: str = "open",
    updated_at: str = "2026-10-02T12:00:00Z",
) -> dict[str, object]:
    """An ``issues`` webhook payload with the fields NikolAI reads, shaped like GitHub's."""
    payload: dict[str, object] = {
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
        "issue": {
            "id": 300,
            "number": issue_number,
            "title": title,
            "state": issue_state,
            "created_at": "2026-10-01T10:00:00Z",
            "updated_at": updated_at,
            "body": body,
            "user": {"id": 10, "login": "student1", "type": "User"},
            "labels": [{"id": 5, "name": name} for name in labels],
        },
    }
    return payload


def workflow_run_payload(
    *,
    name: str = "Refactoring Report",
    conclusion: str | None = "success",
    action: str = "completed",
    pr_numbers: tuple[int, ...] = (PR_NUMBER,),
    base_repository_id: int = 100,
    head_sha: str = HEAD_SHA,
) -> dict[str, object]:
    """A ``workflow_run`` webhook payload for a RefactoringMiner Action run on a PR (US-8)."""
    return {
        "action": action,
        "installation": {"id": INSTALLATION_ID},
        "repository": {
            "id": 100,
            "name": REPO_NAME,
            "full_name": f"{REPO_OWNER}/{REPO_NAME}",
            "owner": {"login": REPO_OWNER},
        },
        "workflow_run": {
            "id": REFACTORING_WORKFLOW_RUN_ID,
            "name": name,
            "event": "pull_request",
            "status": "completed",
            "conclusion": conclusion,
            "head_sha": head_sha,
            "pull_requests": [
                {"number": number, "base": {"ref": "main", "repo": {"id": base_repository_id}}}
                for number in pr_numbers
            ],
        },
    }


def static_analysis_run(head_sha: str, findings: Sequence[StaticFinding]) -> NewStaticAnalysisRun:
    """What the Semgrep job (US-15) saves when Semgrep found ``findings`` at PR #7's commit
    ``head_sha``."""
    return NewStaticAnalysisRun(
        github_repository_id=100,
        pr_number=PR_NUMBER,
        head_sha=head_sha,
        base_sha="0" * 40,
        pr_state="open",
        pr_merged=False,
        workflow_run_id=REFACTORING_WORKFLOW_RUN_ID,
        findings=list(findings),
    )


def signed(payload: dict[str, object], secret: str = WEBHOOK_SECRET) -> tuple[bytes, str]:
    """The raw body GitHub would send for ``payload``, and its X-Hub-Signature-256 header."""
    body = json.dumps(payload).encode("utf-8")
    return body, compute_signature(secret, body)


def make_fake_github() -> MagicMock:
    """Stands in for the githubkit installation client (ported from Team Alpha's US-2 tests).

    It answers the PR commit and file listings, and records the comments posted and edited.
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
    rest.issues.async_update_comment = AsyncMock(
        return_value=SimpleNamespace(
            parsed_data=SimpleNamespace(
                id=12345, html_url="https://github.com/octocat/demo/issues/7#issuecomment-12345"
            )
        )
    )
    # NikolAI edits its comment in place; tests check that nothing is deleted.
    rest.issues.async_delete_comment = AsyncMock()
    # Opening the client (`async with github as opened:`) gives back the same fake.
    github.__aenter__.return_value = github
    return github
