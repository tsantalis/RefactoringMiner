"""The ``issues`` webhook (US-12.1): checking a user story when it is opened or edited.

The delivery's signature is verified like every other event, and every delivery for a
registered repository is saved once per delivery ID (idempotently), so GitHub can
redeliver safely. Only issues labelled ``user story`` opened, or edited through their
title or body, are checked. This file pins the route's filter and dedupe; the storage,
the comment and the failure paths live in ``tests/services/test_story_check.py`` and
``tests/api/test_story_flow.py``.
"""

# AI contribution: 50%

from collections.abc import Iterator
from unittest.mock import ANY, AsyncMock
from uuid import UUID

import httpx
import pytest
from fastapi.testclient import TestClient
from psycopg import OperationalError
from pydantic import SecretStr

from app.api.routes import webhooks
from app.api.routes.webhooks import get_llm_provider
from app.core.config import Settings, get_settings
from app.db.repositories.issue_events import SavedIssueEvent
from app.db.repositories.pr_events import RepositoryNotRegisteredError
from app.main import app
from tests.fakes import WEBHOOK_SECRET, FakeProvider, issues_payload, signed

DELIVERY = "00000000-0000-4000-8000-000000000002"
SAVED_EVENT_ID = UUID("00000000-0000-4000-8000-000000000009")


@pytest.fixture
def settings() -> Settings:
    return Settings(_env_file=None).model_copy(
        update={
            "github_app_id": "123",
            "github_webhook_secret": SecretStr(WEBHOOK_SECRET),
            "github_private_key": SecretStr("fake-key"),
            "database_url": SecretStr("postgresql://test-only"),
        }
    )


@pytest.fixture
def provider() -> FakeProvider:
    return FakeProvider()


@pytest.fixture
def story_job(monkeypatch: pytest.MonkeyPatch) -> AsyncMock:
    """The background handler, replaced so the route's filter alone is on trial."""
    job = AsyncMock()
    monkeypatch.setattr(webhooks, "handle_issues_event", job)
    return job


@pytest.fixture
def persistence(monkeypatch: pytest.MonkeyPatch) -> AsyncMock:
    """The delivery save: the delivery is new and belongs to a registered repository."""
    saved = AsyncMock(return_value=SavedIssueEvent(SAVED_EVENT_ID, created=True))
    monkeypatch.setattr(webhooks, "save_issues_event", saved)
    return saved


@pytest.fixture
def repository_connection(monkeypatch: pytest.MonkeyPatch) -> AsyncMock:
    """The route's database connection, faked like ``test_webhooks.py``'s persistence does."""
    connection = AsyncMock(return_value=AsyncMock())
    monkeypatch.setattr(webhooks, "connect", connection)
    return connection


@pytest.fixture
def client(
    settings: Settings,
    provider: FakeProvider,
    story_job: AsyncMock,
    persistence: AsyncMock,
    repository_connection: AsyncMock,
) -> Iterator[TestClient]:
    app.dependency_overrides[get_settings] = lambda: settings
    app.dependency_overrides[get_llm_provider] = lambda: provider
    try:
        # No `with`: the app's startup (which would build a real OpenRouter provider) is skipped.
        yield TestClient(app)
    finally:
        app.dependency_overrides.clear()


def deliver_issues(
    client: TestClient, payload: dict[str, object], delivery: str = DELIVERY
) -> httpx.Response:
    body, signature = signed(payload)
    response: httpx.Response = client.post(
        "/webhook",
        content=body,
        headers={
            "X-GitHub-Event": "issues",
            "X-GitHub-Delivery": delivery,
            "X-Hub-Signature-256": signature,
            "Content-Type": "application/json",
        },
    )
    return response


# ---- Which issues get checked ---------------------------------------------------------------


def test_an_opened_user_story_is_checked(
    client: TestClient,
    story_job: AsyncMock,
    provider: FakeProvider,
    persistence: AsyncMock,
) -> None:
    response = deliver_issues(client, issues_payload("opened"))

    assert response.status_code == 202
    assert response.json() == {"ok": True}
    # The handler gets the parsed event and the delivery's saved event row.
    story_job.assert_awaited_once_with(ANY, provider, ANY, SAVED_EVENT_ID)
    saved_call = persistence.await_args
    assert saved_call is not None
    assert saved_call.args[1] == UUID(DELIVERY)
    assert saved_call.kwargs == {"ignored": False}


def test_a_title_or_body_edit_is_checked(
    client: TestClient, story_job: AsyncMock, provider: FakeProvider
) -> None:
    for changed in ("title", "body"):
        payload = issues_payload("edited")
        payload["changes"] = {changed: {"from": "The old text"}}

        response = deliver_issues(client, payload)

        assert response.status_code == 202
        story_job.assert_awaited_with(ANY, provider, ANY, ANY)


def test_an_edit_without_a_title_or_body_change_is_not_checked(
    client: TestClient, story_job: AsyncMock
) -> None:
    changes_ways: tuple[dict[str, object] | None, ...] = (
        {"milestone": {"from": "15", "to": "16"}},
        {"labels": {"from": []}},
        {},
        None,
    )
    for changes in changes_ways:
        payload = issues_payload("edited")
        if changes is None:
            payload.pop("changes", None)
        else:
            payload["changes"] = changes

        response = deliver_issues(client, payload)

        assert response.status_code == 202
        assert response.json() == {"ok": True}
    story_job.assert_not_called()


def test_housekeeping_actions_are_not_checked(client: TestClient, story_job: AsyncMock) -> None:
    for action in (
        "labeled",
        "unlabeled",
        "assigned",
        "unassigned",
        "milestoned",
        "demilestoned",
        "closed",
    ):
        response = deliver_issues(client, issues_payload(action))

        assert response.status_code == 202
        assert response.json() == {"ok": True}
    story_job.assert_not_called()


def test_an_issue_without_the_user_story_label_is_not_checked(
    client: TestClient, story_job: AsyncMock
) -> None:
    response = deliver_issues(client, issues_payload("opened", labels=("bug",)))

    assert response.status_code == 202
    assert response.json() == {"ok": True}
    story_job.assert_not_called()


def test_an_issue_with_several_labels_including_user_story_is_checked(
    client: TestClient, story_job: AsyncMock
) -> None:
    payload = issues_payload("opened", labels=("bug", "user story", "requirements-quality"))

    response = deliver_issues(client, payload)

    assert response.status_code == 202
    story_job.assert_awaited_once()


def test_the_label_matches_without_case(client: TestClient, story_job: AsyncMock) -> None:
    response = deliver_issues(client, issues_payload("opened", labels=("User Story",)))

    assert response.status_code == 202
    story_job.assert_awaited_once()


def test_the_handler_receives_the_parsed_event(client: TestClient, story_job: AsyncMock) -> None:
    deliver_issues(client, issues_payload("opened"))

    assert story_job.await_args is not None
    event = story_job.await_args.args[0]
    assert event.action == "opened"
    assert event.issue.number == 12
    assert event.issue.title == "US-12: Check my user story's format and clarity"
    assert event.repository.full_name == "octocat/demo"
    assert event.installation.id == 42


# ---- Every delivery is saved, once ----------------------------------------------------------


def test_a_housekeeping_delivery_is_saved_but_not_checked(
    client: TestClient, story_job: AsyncMock, persistence: AsyncMock
) -> None:
    response = deliver_issues(client, issues_payload("labeled"))

    assert response.status_code == 202
    assert response.json() == {"ok": True}
    persistence.assert_awaited_once()
    saved_call = persistence.await_args
    assert saved_call is not None
    assert saved_call.kwargs == {"ignored": True}
    story_job.assert_not_called()


def test_a_redelivered_story_delivery_is_scheduled_again(
    client: TestClient, story_job: AsyncMock, provider: FakeProvider, persistence: AsyncMock
) -> None:
    # A redelivery may finish a check the first delivery never reached (the job's run
    # claim keeps it idempotent: an already-processed delivery is a no-op).
    persistence.return_value = SavedIssueEvent(SAVED_EVENT_ID, created=False)

    response = deliver_issues(client, issues_payload("opened"))

    assert response.status_code == 202
    assert response.json() == {"ok": True}
    story_job.assert_awaited_once_with(ANY, provider, ANY, SAVED_EVENT_ID)


def test_a_redelivered_delivery_that_is_not_checked_stays_a_noop(
    client: TestClient, story_job: AsyncMock, persistence: AsyncMock
) -> None:
    persistence.return_value = SavedIssueEvent(SAVED_EVENT_ID, created=False)

    response = deliver_issues(client, issues_payload("labeled"))

    assert response.status_code == 202
    story_job.assert_not_called()


def test_a_delivery_without_a_github_id_is_rejected(
    client: TestClient, story_job: AsyncMock, persistence: AsyncMock
) -> None:
    response = deliver_issues(client, issues_payload("opened"), delivery="not-a-uuid")

    assert response.status_code == 400
    persistence.assert_not_awaited()
    story_job.assert_not_called()


def test_a_repository_not_linked_to_a_team_is_not_checked(
    client: TestClient, story_job: AsyncMock, persistence: AsyncMock
) -> None:
    # An installation an instructor hasn't linked to a course team is not served (ADR 0001).
    persistence.side_effect = RepositoryNotRegisteredError("repository 100 is not registered")

    response = deliver_issues(client, issues_payload("opened"))

    assert response.status_code == 202
    assert response.json() == {"ok": True, "ignored": True}
    persistence.assert_awaited_once()
    story_job.assert_not_called()


def test_a_failed_save_asks_github_to_redeliver(
    client: TestClient, story_job: AsyncMock, persistence: AsyncMock
) -> None:
    persistence.side_effect = OperationalError("connection refused")

    response = deliver_issues(client, issues_payload("opened"))

    assert response.status_code == 503
    story_job.assert_not_called()


def test_a_missing_database_rejects_the_delivery(
    client: TestClient, settings: Settings, story_job: AsyncMock, persistence: AsyncMock
) -> None:
    app.dependency_overrides[get_settings] = lambda: settings.model_copy(
        update={"database_url": None}
    )

    response = deliver_issues(client, issues_payload("opened"))

    assert response.status_code == 503
    persistence.assert_not_awaited()
    story_job.assert_not_called()


def test_rejects_an_unreadable_issues_payload(client: TestClient, story_job: AsyncMock) -> None:
    response = deliver_issues(client, {"action": "opened"})

    assert response.status_code == 400
    story_job.assert_not_called()
