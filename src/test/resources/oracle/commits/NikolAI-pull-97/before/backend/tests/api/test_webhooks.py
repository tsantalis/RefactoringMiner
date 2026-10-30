"""Tests for the GitHub App webhook endpoint, including Team Alpha's two US-2 tests.

``TestClient`` runs background tasks before ``client.post`` returns, so each test sees the
whole flow: signature check, then the review job. GitHub and the model are faked.
"""

import json
from collections.abc import Iterator
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock
from uuid import UUID, uuid4

import httpx
import pytest
from fastapi.testclient import TestClient
from psycopg import OperationalError
from pydantic import SecretStr

from app.agents.base import AgentResult
from app.api.routes import webhooks
from app.api.routes.webhooks import get_llm_provider
from app.core.config import Settings, get_settings
from app.db.repositories.pr_events import RepositoryNotRegisteredError, SavedPREvent
from app.github.signatures import compute_signature
from app.main import app
from app.providers.errors import LLMRateLimitError
from app.services import pr_review
from app.services.pr_review import COMMENT_MARKER, FAILURE_MARKER
from tests.fakes import (
    PR_NUMBER,
    REPLY_TEXT,
    WEBHOOK_SECRET,
    FakeProvider,
    make_fake_github,
    pull_request_payload,
    signed,
)


@pytest.fixture
def settings(tmp_path: Path) -> Settings:
    return Settings(_env_file=None).model_copy(
        update={
            "github_app_id": "123",
            "github_webhook_secret": SecretStr(WEBHOOK_SECRET),
            "github_private_key": SecretStr("fake-key"),
            "pr_log_dir": tmp_path / "logs",
            "database_url": SecretStr("postgresql://test-only"),
        }
    )


@pytest.fixture
def provider() -> FakeProvider:
    return FakeProvider()


@pytest.fixture
def github(monkeypatch: pytest.MonkeyPatch) -> MagicMock:
    fake = make_fake_github()
    monkeypatch.setattr(pr_review, "installation_client", lambda _settings, _installation: fake)
    return fake


@pytest.fixture
def persistence(monkeypatch: pytest.MonkeyPatch) -> AsyncMock:
    monkeypatch.setattr(pr_review, "review_ownership", MagicMock(return_value=AsyncMock()))
    monkeypatch.setattr(pr_review, "start_review", AsyncMock(return_value=uuid4()))
    publication: dict[str, object] = {"status": "pending", "payload": pull_request_payload()}

    async def complete(
        url: str, run_id: UUID, result: AgentResult, *, queue_publication: bool
    ) -> UUID:
        assert queue_publication
        publication["json_content"] = result.model_dump(mode="json")
        return uuid4()

    monkeypatch.setattr(pr_review, "complete_review", AsyncMock(side_effect=complete))
    monkeypatch.setattr(pr_review, "get_publication", AsyncMock(return_value=publication))
    monkeypatch.setattr(pr_review, "mark_published", AsyncMock())
    monkeypatch.setattr(pr_review, "mark_publication_failed", AsyncMock())
    monkeypatch.setattr(pr_review, "fail_review", AsyncMock())
    monkeypatch.setattr(webhooks, "connect", AsyncMock(return_value=AsyncMock()))
    save = AsyncMock(return_value=SavedPREvent(uuid4(), created=True))
    monkeypatch.setattr(webhooks, "save_pr_event", save)
    return save


@pytest.fixture
def client(
    settings: Settings, provider: FakeProvider, github: MagicMock, persistence: AsyncMock
) -> Iterator[TestClient]:
    app.dependency_overrides[get_settings] = lambda: settings
    app.dependency_overrides[get_llm_provider] = lambda: provider
    try:
        # No `with`: the app's startup (which would build a real OpenRouter provider) is skipped.
        yield TestClient(app)
    finally:
        app.dependency_overrides.clear()


def deliver(client: TestClient, event: str, body: bytes, signature: str | None) -> httpx.Response:
    headers = {
        "X-GitHub-Event": event,
        "X-GitHub-Delivery": "00000000-0000-4000-8000-000000000001",
        "Content-Type": "application/json",
    }
    if signature is not None:
        headers["X-Hub-Signature-256"] = signature
    response: httpx.Response = client.post("/webhook", content=body, headers=headers)
    return response


def deliver_signed(client: TestClient, event: str, payload: dict[str, object]) -> httpx.Response:
    body, signature = signed(payload)
    return deliver(client, event, body, signature)


# ---- Team Alpha's US-2 tests, ported ------------------------------------------------------


def test_new_commit_retries_failed_review_and_replaces_failure_note(
    client: TestClient, github: MagicMock, provider: FakeProvider
) -> None:
    provider.error = LLMRateLimitError("Models busy")
    first = deliver_signed(client, "pull_request", pull_request_payload("opened"))

    assert first.status_code == 202
    assert len(provider.calls) == 1
    github.rest.issues.async_create_comment.assert_awaited_once()
    failure_body = github.rest.issues.async_create_comment.await_args.kwargs["body"]
    assert FAILURE_MARKER in failure_body
    github.rest.issues.async_list_comments.return_value.parsed_data = [
        SimpleNamespace(
            id=42,
            body=failure_body,
            user=SimpleNamespace(type="Bot"),
            performed_via_github_app=None,
        ),
    ]
    provider.error = None
    github.rest.issues.reset_mock()

    second = deliver_signed(client, "pull_request", pull_request_payload("synchronize"))

    assert second.status_code == 202
    assert len(provider.calls) == 2
    github.rest.issues.async_create_comment.assert_awaited_once()
    body = github.rest.issues.async_create_comment.await_args.kwargs["body"]
    assert REPLY_TEXT in body
    assert provider.completion.model in body
    assert FAILURE_MARKER not in body
    github.rest.issues.async_delete_comment.assert_awaited_once_with(
        owner="octocat", repo="demo", comment_id=42
    )
    methods = [call[0] for call in github.rest.issues.mock_calls]
    assert methods.index("async_create_comment") < methods.index("async_delete_comment")


def test_opened_pr_posts_comment_and_writes_log(
    client: TestClient, github: MagicMock, settings: Settings
) -> None:
    """Ported from Team Alpha. The comment is now NikolAI's review of the PR."""
    response = deliver_signed(client, "pull_request", pull_request_payload("opened"))

    assert response.status_code == 202
    assert response.json() == {"ok": True}
    github.rest.issues.async_create_comment.assert_awaited_once()
    body = github.rest.issues.async_create_comment.await_args.kwargs["body"]
    assert body.startswith(COMMENT_MARKER)
    assert REPLY_TEXT in body
    assert len(list(settings.pr_log_dir.glob(f"demo_PR{PR_NUMBER}_*.md"))) == 1


def test_non_opened_action_is_ignored(
    client: TestClient, github: MagicMock, provider: FakeProvider
) -> None:
    """Ported from Team Alpha."""
    response = deliver_signed(client, "pull_request", pull_request_payload("closed"))

    assert response.status_code == 202
    github.rest.issues.async_create_comment.assert_not_called()
    assert provider.calls == []


# ---- Which pull-request events get a review -----------------------------------------------


@pytest.mark.parametrize("action", ["reopened", "ready_for_review"])
def test_reopened_and_ready_prs_are_reviewed(
    client: TestClient, github: MagicMock, action: str
) -> None:
    deliver_signed(client, "pull_request", pull_request_payload(action))

    github.rest.issues.async_create_comment.assert_awaited_once()


@pytest.mark.parametrize(("draft", "author_type"), [(True, "User"), (False, "Bot")])
def test_drafts_and_bot_prs_are_skipped(
    client: TestClient, github: MagicMock, provider: FakeProvider, draft: bool, author_type: str
) -> None:
    payload = pull_request_payload("opened", draft=draft, author_type=author_type)

    response = deliver_signed(client, "pull_request", payload)

    assert response.status_code == 202
    github.rest.issues.async_create_comment.assert_not_called()
    assert provider.calls == []


def test_reviews_use_the_provider_created_at_startup(
    client: TestClient, github: MagicMock, provider: FakeProvider
) -> None:
    # Undo the fixture's override, so the endpoint reads app.state like in production.
    del app.dependency_overrides[get_llm_provider]
    app.state.llm_provider = provider
    try:
        deliver_signed(client, "pull_request", pull_request_payload("opened"))
    finally:
        del app.state.llm_provider

    assert len(provider.calls) == 1
    github.rest.issues.async_create_comment.assert_awaited_once()


def test_without_a_provider_prs_are_logged_but_not_reviewed(
    client: TestClient, github: MagicMock, settings: Settings
) -> None:
    app.dependency_overrides[get_llm_provider] = lambda: None

    response = deliver_signed(client, "pull_request", pull_request_payload("opened"))

    assert response.status_code == 202
    github.rest.issues.async_create_comment.assert_awaited_once()
    assert len(list(settings.pr_log_dir.glob("*.md"))) == 1


# ---- Signatures and other events ----------------------------------------------------------


def test_rejects_a_delivery_without_a_signature(client: TestClient, github: MagicMock) -> None:
    body, _ = signed(pull_request_payload())

    response = deliver(client, "pull_request", body, signature=None)

    assert response.status_code == 401
    github.rest.issues.async_create_comment.assert_not_called()


def test_rejects_a_delivery_signed_with_another_secret(
    client: TestClient, github: MagicMock
) -> None:
    body, signature = signed(pull_request_payload(), secret="someone-elses-secret")

    response = deliver(client, "pull_request", body, signature)

    assert response.status_code == 401
    github.rest.issues.async_create_comment.assert_not_called()


def test_refuses_every_delivery_when_no_secret_is_configured(
    client: TestClient, github: MagicMock, settings: Settings
) -> None:
    app.dependency_overrides[get_settings] = lambda: settings.model_copy(
        update={"github_webhook_secret": None}
    )

    response = deliver_signed(client, "pull_request", pull_request_payload())

    assert response.status_code == 503
    github.rest.issues.async_create_comment.assert_not_called()


def test_the_signature_covers_the_exact_raw_bytes(client: TestClient) -> None:
    body = b'{"zen":   "Keep it logically awesome."}'
    signature = compute_signature(WEBHOOK_SECRET, body)
    reencoded = json.dumps(json.loads(body)).encode("utf-8")

    assert deliver(client, "ping", body, signature).status_code == 202
    assert deliver(client, "ping", reencoded, signature).status_code == 401


def test_accepts_githubs_ping(client: TestClient) -> None:
    response = deliver_signed(client, "ping", {"zen": "Design for failure.", "hook_id": 1})

    assert response.status_code == 202
    assert response.json() == {"ok": True}


def test_ignores_other_events(
    client: TestClient, github: MagicMock, provider: FakeProvider
) -> None:
    response = deliver_signed(client, "issues", {"action": "opened"})

    assert response.status_code == 202
    github.rest.issues.async_create_comment.assert_not_called()
    assert provider.calls == []


def test_rejects_an_unreadable_pull_request_payload(client: TestClient, github: MagicMock) -> None:
    response = deliver_signed(client, "pull_request", {"action": "opened"})

    assert response.status_code == 400
    github.rest.issues.async_create_comment.assert_not_called()


@pytest.mark.parametrize("action", ["opened", "closed"])
def test_saves_original_payload(client: TestClient, persistence: AsyncMock, action: str) -> None:
    payload = pull_request_payload(action)
    assert deliver_signed(client, "pull_request", payload).status_code == 202
    persistence.assert_awaited_once()
    assert persistence.await_args is not None
    assert persistence.await_args.kwargs == {"ignored": action == "closed"}
    assert persistence.await_args.args[1:] == (
        UUID("00000000-0000-4000-8000-000000000001"),
        payload,
    )


def test_duplicate_does_not_review(
    client: TestClient, persistence: AsyncMock, provider: FakeProvider
) -> None:
    persistence.return_value = SavedPREvent(uuid4(), created=False)
    assert deliver_signed(client, "pull_request", pull_request_payload()).status_code == 202
    assert provider.calls == []


@pytest.mark.parametrize(
    "error",
    [
        OperationalError("unavailable"),
        TimeoutError(),
        ValueError("identity mismatch"),
    ],
)
def test_failed_save_does_not_review(
    client: TestClient, persistence: AsyncMock, provider: FakeProvider, error: Exception
) -> None:
    persistence.side_effect = error
    assert deliver_signed(client, "pull_request", pull_request_payload()).status_code == 503
    assert provider.calls == []


def test_unregistered_repository_is_acknowledged_with_actionable_warning(
    client: TestClient,
    persistence: AsyncMock,
    provider: FakeProvider,
    github: MagicMock,
    caplog: pytest.LogCaptureFixture,
) -> None:
    persistence.side_effect = RepositoryNotRegisteredError("missing")
    response = deliver_signed(client, "pull_request", pull_request_payload())
    assert response.status_code == 202
    assert response.json() == {"ok": True, "ignored": True}
    assert provider.calls == []
    github.rest.issues.async_create_comment.assert_not_called()
    assert "repository ID 100 has no active registration" in caplog.text
    assert "app.db.register_repository" in caplog.text
    assert "00000000-0000-4000-8000-000000000001" in caplog.text


def test_missing_database_rejects_pr(
    client: TestClient, settings: Settings, persistence: AsyncMock, provider: FakeProvider
) -> None:
    app.dependency_overrides[get_settings] = lambda: settings.model_copy(
        update={"database_url": None}
    )
    assert deliver_signed(client, "pull_request", pull_request_payload()).status_code == 503
    persistence.assert_not_awaited()
    assert provider.calls == []


@pytest.mark.parametrize("delivery", [None, "invalid"])
def test_invalid_delivery_id_is_rejected(
    client: TestClient, persistence: AsyncMock, delivery: str | None
) -> None:
    body, signature = signed(pull_request_payload())
    headers = {"X-GitHub-Event": "pull_request", "X-Hub-Signature-256": signature}
    if delivery is not None:
        headers["X-GitHub-Delivery"] = delivery
    assert client.post("/webhook", content=body, headers=headers).status_code == 400
    persistence.assert_not_awaited()


def test_invalid_signature_never_reaches_database(
    client: TestClient, persistence: AsyncMock
) -> None:
    body, _ = signed(pull_request_payload())
    assert deliver(client, "pull_request", body, None).status_code == 401
    persistence.assert_not_awaited()
