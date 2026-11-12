"""The user story check, from GitHub's webhooks to the database and the issue comment (US-12.1).

The webhook filter, the background job, the database writes and the comment are the real
ones; the model is ``FakeProvider`` and the story comment goes over a network-like fake of
GitHub's issue-comment API through a real githubkit client. GitHub's delivery comes from a
client with no session cookie and no Origin, as in production.
"""

# AI contribution: 50%

import asyncio
from collections.abc import Iterator
from typing import cast
from unittest.mock import AsyncMock
from uuid import uuid4

import httpx
import pytest
from fastapi.testclient import TestClient
from githubkit import GitHub
from githubkit.auth import TokenAuthStrategy
from pydantic import SecretStr

from app.agents.story_review import CouldNotCheck
from app.api.routes import webhooks
from app.api.routes.webhooks import get_llm_provider
from app.core.config import Settings, get_settings
from app.main import app
from app.providers.errors import LLMRateLimitError
from app.services import story_check
from app.services.story_check import handle_issues_event
from app.services.story_comment import STORY_COMMENT_MARKER
from tests.api.courses_support import read_sql, run_sql
from tests.fake_github_api import FIRST_COMMENT_ID, GitHubApi
from tests.fakes import (
    REPO_NAME,
    REPO_OWNER,
    TEST_MODEL,
    WEBHOOK_SECRET,
    FakeProvider,
    issues_payload,
    sample_completion,
    signed,
)

APP_ID: int = 123
ISSUE_NUMBER: int = 12
REPOSITORY_ID: int = 100
REPO_API = f"/repos/{REPO_OWNER}/{REPO_NAME}"
COMMENTS = f"{REPO_API}/issues/{ISSUE_NUMBER}/comments"


def deliver(
    github: TestClient, payload: dict[str, object], delivery: str | None = None
) -> httpx.Response:
    body, signature = signed(payload)
    response: httpx.Response = github.post(
        "/webhook",
        content=body,
        headers={
            "X-GitHub-Event": "issues",
            "X-GitHub-Delivery": delivery or str(uuid4()),
            "X-Hub-Signature-256": signature,
            "Content-Type": "application/json",
        },
    )
    return response


def edit_payload(**changes: dict[str, object]) -> dict[str, object]:
    payload = issues_payload("edited")
    payload["changes"] = changes
    return payload


@pytest.fixture
def settings(database_url: str) -> Settings:
    return Settings(_env_file=None).model_copy(
        update={
            "github_app_id": str(APP_ID),
            "github_webhook_secret": SecretStr(WEBHOOK_SECRET),
            "github_private_key": SecretStr("fake-key"),
            "database_url": SecretStr(database_url),
        }
    )


@pytest.fixture
def provider() -> FakeProvider:
    return FakeProvider()


@pytest.fixture
def api(monkeypatch: pytest.MonkeyPatch) -> GitHubApi:
    """GitHub's issue-comment API, reached through a real githubkit client."""
    api = GitHubApi(b"", app_id=APP_ID)
    monkeypatch.setattr(
        story_check,
        "installation_client",
        lambda _settings, _installation: GitHub(TokenAuthStrategy("token"), async_transport=api),
    )
    return api


@pytest.fixture
def webhook_client(settings: Settings, provider: FakeProvider) -> Iterator[TestClient]:
    app.dependency_overrides[get_settings] = lambda: settings
    app.dependency_overrides[get_llm_provider] = lambda: provider
    try:
        yield TestClient(app, base_url="https://testserver")
    finally:
        app.dependency_overrides.clear()


def comment_writes(api: GitHubApi) -> list[tuple[str, str]]:
    return [(r.method, r.url.path) for r in api.requests if r.method in ("POST", "PATCH")]


def story_runs(database_url: str) -> list[dict[str, object]]:
    return run_sql(
        database_url,
        "SELECT * FROM analysis_runs WHERE analysis_type = 'story_review'",
    )


def story_outputs(database_url: str) -> list[dict[str, object]]:
    return run_sql(
        database_url,
        """SELECT o.* FROM analysis_outputs o JOIN analysis_runs r ON r.id = o.analysis_run_id
           WHERE r.analysis_type = 'story_review'""",
    )


async def runs_by_delivery(database_url: str) -> dict[object, dict[str, object]]:
    """The story runs keyed by the delivery that triggered them."""
    rows = await read_sql(
        database_url,
        """SELECT r.status, r.error_message, r.started_at, r.finished_at,
                  e.id AS event_id
           FROM analysis_runs r JOIN webhook_events e ON e.id = r.trigger_event_id""",
    )
    return {row.pop("event_id"): row for row in rows}


# ---- An opened story -------------------------------------------------------------------------


def test_an_opened_story_is_stored_and_commented(
    webhook_client: TestClient,
    database_url: str,
    api: GitHubApi,
    provider: FakeProvider,
) -> None:
    response = deliver(webhook_client, issues_payload("opened"))

    assert (response.status_code, response.json()) == (202, {"ok": True})
    assert provider.story_review_calls != []
    assert comment_writes(api) == [("POST", COMMENTS)]
    (comment,) = api.posted_comments
    assert comment.startswith(STORY_COMMENT_MARKER)
    assert "Score: **2 / 3**" in comment
    # The run and its output are saved with the issue they belong to.
    (issue,) = run_sql(
        database_url,
        "SELECT github_issue_id, issue_number, title, state FROM issues",
    )
    assert issue["github_issue_id"] == 300
    assert (issue["issue_number"], issue["state"]) == (12, "open")
    (run,) = story_runs(database_url)
    assert run["status"] == "completed"
    assert run["issue_id"] is not None
    (output,) = story_outputs(database_url)
    # The model that wrote the review stays attributable to the stored run.
    assert cast("dict[str, object]", output["metadata"])["model"] == TEST_MODEL
    stored = cast("dict[str, object]", output["json_content"])
    review = cast("dict[str, object]", stored["review"])
    assert review["score"] == 2
    assert stored["reason"] is None


def test_an_edit_rechecks_the_story_and_edits_the_comment_in_place(
    webhook_client: TestClient,
    database_url: str,
    api: GitHubApi,
) -> None:
    deliver(webhook_client, issues_payload("opened"))

    response = deliver(webhook_client, edit_payload(title={"from": "older title"}))

    assert (response.status_code, response.json()) == (202, {"ok": True})
    assert comment_writes(api) == [
        ("POST", COMMENTS),
        ("PATCH", f"{REPO_API}/issues/comments/{FIRST_COMMENT_ID}"),
    ]
    ((comment_id, body),) = api.patched_comments
    assert comment_id == FIRST_COMMENT_ID
    assert body.startswith(STORY_COMMENT_MARKER)
    assert len(story_runs(database_url)) == 2


def test_housekeeping_and_label_deliveries_change_nothing(
    webhook_client: TestClient,
    database_url: str,
    api: GitHubApi,
) -> None:
    for payload in (
        issues_payload("labeled"),
        edit_payload(milestone={"from": "15", "to": "16"}),
        issues_payload("closed"),
    ):
        response = deliver(webhook_client, payload)

        assert (response.status_code, response.json()) == (202, {"ok": True})

    # The deliveries are saved (status ``ignored``) with their issue, but nothing runs.
    (issue,) = run_sql(database_url, "SELECT issue_number, state FROM issues")
    assert (issue["issue_number"], issue["state"]) == (12, "open")
    assert story_runs(database_url) == []
    assert story_outputs(database_url) == []
    assert comment_writes(api) == []


def test_an_issue_without_the_user_story_label_is_not_checked(
    webhook_client: TestClient,
    database_url: str,
    api: GitHubApi,
) -> None:
    response = deliver(webhook_client, issues_payload("opened", labels=("bug",)))

    assert (response.status_code, response.json()) == (202, {"ok": True})
    # The delivery itself is saved (ignored) with the issue it carries; no check runs.
    (issue,) = run_sql(database_url, "SELECT issue_number FROM issues")
    assert issue["issue_number"] == 12
    assert story_runs(database_url) == []
    assert story_outputs(database_url) == []
    assert comment_writes(api) == []


def test_an_unlinked_installation_is_ignored(
    webhook_client: TestClient,
    database_url: str,
    api: GitHubApi,
) -> None:
    # An App installation an instructor has not yet linked to a course team is not served.
    run_sql(
        database_url,
        "UPDATE repositories SET installed_by_login = 'alice' WHERE github_repository_id = 100",
    )

    response = deliver(webhook_client, issues_payload("opened"))

    assert (response.status_code, response.json()) == (202, {"ok": True, "ignored": True})
    assert story_runs(database_url) == []
    assert comment_writes(api) == []


def test_a_model_failure_stores_a_failed_run_and_posts_the_couldnt_check_comment(
    webhook_client: TestClient,
    database_url: str,
    api: GitHubApi,
    provider: FakeProvider,
) -> None:
    provider.story_review_error = LLMRateLimitError("models busy")

    response = deliver(webhook_client, issues_payload("opened"))

    assert (response.status_code, response.json()) == (202, {"ok": True})
    assert comment_writes(api) == [("POST", COMMENTS)]
    (comment,) = api.posted_comments
    assert "NikolAI's AI models are busy or unavailable." in comment
    (run,) = story_runs(database_url)
    assert run["status"] == "failed"
    assert run["error_message"] == CouldNotCheck.MODEL_UNAVAILABLE
    (output,) = story_outputs(database_url)
    stored = cast("dict[str, object]", output["json_content"])
    assert stored["review"] is None
    assert stored["reason"] == CouldNotCheck.MODEL_UNAVAILABLE


# ---- Two deliveries for one story ------------------------------------------------------------


def test_a_repeated_delivery_is_handled_once(
    webhook_client: TestClient, database_url: str, api: GitHubApi, provider: FakeProvider
) -> None:
    # GitHub retries a delivery on a timeout: the second one must change nothing.
    payload = issues_payload("opened")
    delivery = "00000000-0000-4000-8000-000000000005"

    for _ in range(2):
        response = deliver(webhook_client, payload, delivery=delivery)
        assert (response.status_code, response.json()) == (202, {"ok": True})

    assert len(provider.story_review_calls) == 1
    assert comment_writes(api) == [("POST", COMMENTS)]
    assert len(story_runs(database_url)) == 1
    assert len(story_outputs(database_url)) == 1


async def test_an_older_check_finishing_last_cannot_overwrite_the_newest_verdict(
    webhook_client: TestClient,
    database_url: str,
    api: GitHubApi,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """GitHub delivered an edit right after the opening, and both jobs run at once."""
    # Keep the route from scheduling the jobs: they are run by hand, out of order.
    scheduled = AsyncMock()
    monkeypatch.setattr(webhooks, "handle_issues_event", scheduled)
    deliver(
        webhook_client,
        issues_payload("opened", title="The old story", updated_at="2026-10-01T08:00:00Z"),
    )
    deliver(webhook_client, edit_payload(title={"from": "older title"}))
    deliveries = [(call.args[0], call.args[2], call.args[3]) for call in scheduled.call_args_list]
    ((old_event, old_settings, old_id), (new_event, new_settings, new_id)) = deliveries

    # The older check is a slower model: it is still thinking when the newer finishes.
    gate = asyncio.Event()
    older = FakeProvider()
    older.story_review_gate = gate
    newer = FakeProvider()
    old_task = asyncio.create_task(handle_issues_event(old_event, older, old_settings, old_id))
    new_task = asyncio.create_task(handle_issues_event(new_event, newer, new_settings, new_id))
    try:
        for _ in range(100):
            runs = await runs_by_delivery(database_url)
            (old_run, new_run) = (runs.get(old_id), runs.get(new_id))
            if (
                old_run is not None
                and new_run is not None
                and old_run["status"] == "running"
                and new_run["status"] == "completed"
            ):
                break
            await asyncio.sleep(0.05)
        else:
            pytest.fail("the newer check never completed while the older one was still running")
        # The check in progress is visible: its run is recorded as running.
        assert old_run["started_at"] is not None
        assert old_run["finished_at"] is None
    finally:
        # Resume a gated model call so a failed wait cannot hang the test; the newer
        # verdict is already stored either way.
        gate.set()
        await asyncio.gather(old_task, new_task)

    runs = await runs_by_delivery(database_url)
    (stale, current) = (runs[old_id], runs[new_id])  # KeyError, not a false pass, if absent.
    assert (stale["status"], stale["error_message"]) == ("cancelled", "Superseded")
    assert current["status"] == "completed"
    # The stale check posted nothing: the comment is the newest verdict only.
    assert comment_writes(api) == [("POST", COMMENTS)]
    events = await read_sql(
        database_url, "SELECT status FROM webhook_events WHERE event_type = 'issues'"
    )
    assert {row["status"] for row in events} == {"processed"}


# ---- Other e2e cases -------------------------------------------------------------------------


def test_a_body_edit_rechecks_and_edits_the_comment_in_place(
    webhook_client: TestClient,
    database_url: str,
    api: GitHubApi,
) -> None:
    deliver(webhook_client, issues_payload("opened"))

    response = deliver(webhook_client, edit_payload(body={"from": "older body"}))

    assert (response.status_code, response.json()) == (202, {"ok": True})
    assert comment_writes(api) == [
        ("POST", COMMENTS),
        ("PATCH", f"{REPO_API}/issues/comments/{FIRST_COMMENT_ID}"),
    ]
    assert len(story_runs(database_url)) == 2


def test_an_invalid_reply_stores_a_failed_run_and_posts_a_couldnt_check_comment(
    webhook_client: TestClient,
    database_url: str,
    api: GitHubApi,
    provider: FakeProvider,
) -> None:
    provider.story_review = sample_completion("Prose, not JSON.")

    response = deliver(webhook_client, issues_payload("opened"))

    assert (response.status_code, response.json()) == (202, {"ok": True})
    assert comment_writes(api) == [("POST", COMMENTS)]
    (comment,) = api.posted_comments
    assert "The AI model's answer couldn't be used." in comment
    (run,) = story_runs(database_url)
    assert (run["status"], run["error_message"]) == ("failed", CouldNotCheck.INVALID_REPLY)
    (output,) = story_outputs(database_url)
    stored = cast("dict[str, object]", output["json_content"])
    assert stored["review"] is None
    assert stored["reason"] == CouldNotCheck.INVALID_REPLY


def test_without_a_provider_the_story_could_not_be_checked(
    webhook_client: TestClient,
    database_url: str,
    api: GitHubApi,
) -> None:
    app.dependency_overrides[get_llm_provider] = lambda: None

    response = deliver(webhook_client, issues_payload("opened"))

    assert (response.status_code, response.json()) == (202, {"ok": True})
    assert comment_writes(api) == [("POST", COMMENTS)]
    (comment,) = api.posted_comments
    assert "NikolAI has no AI model configured right now." in comment
    (run,) = story_runs(database_url)
    assert (run["status"], run["error_message"]) == ("failed", CouldNotCheck.NOT_CONFIGURED)


def test_the_stored_result_ignores_server_facts_a_reply_claims(
    webhook_client: TestClient,
    database_url: str,
    api: GitHubApi,
    provider: FakeProvider,
) -> None:
    # The reply must not decide facts the server owns.
    provider.story_review = sample_completion(
        '{"checks": [{"text": "The story is clear", "passed": true}],'
        '"signed_off": true, "linked_pulls": [999, 1000]}'
    )

    deliver(webhook_client, issues_payload("opened"))

    (output,) = story_outputs(database_url)
    stored = cast("dict[str, object]", output["json_content"])
    review = cast("dict[str, object]", stored["review"])
    assert review["signed_off"] is False
    assert review["linked_pulls"] == []


def test_a_rewrite_is_rendered_and_escaped_in_the_posted_comment(
    webhook_client: TestClient,
    database_url: str,
    api: GitHubApi,
    provider: FakeProvider,
) -> None:
    provider.story_review = sample_completion(
        '{"checks": [{"text": "@octocat see [x](https://evil.example)", "passed": false}],'
        '"current_text": "As a <role>",'
        '"suggested_text": "As a student <img> @bob"}'
    )

    deliver(webhook_client, issues_payload("opened"))

    (comment,) = api.posted_comments
    assert "@octocat" not in comment
    assert "[x](https://evil.example)" not in comment
    assert "<img" not in comment
    assert "@&#8203;octocat" in comment
    assert "&lt;role&gt;" in comment
    assert "&lt;img&gt;" in comment
    (output,) = story_outputs(database_url)
    stored = cast("dict[str, object]", output["json_content"])
    assert stored["review"] is not None


def test_a_failure_after_a_success_edits_the_comment_in_place(
    webhook_client: TestClient,
    database_url: str,
    api: GitHubApi,
    provider: FakeProvider,
) -> None:
    deliver(webhook_client, issues_payload("opened"))
    provider.story_review_error = LLMRateLimitError("models busy")

    response = deliver(webhook_client, edit_payload(title={"from": "older title"}))

    assert (response.status_code, response.json()) == (202, {"ok": True})
    assert comment_writes(api) == [
        ("POST", COMMENTS),
        ("PATCH", f"{REPO_API}/issues/comments/{FIRST_COMMENT_ID}"),
    ]
    ((comment_id, body),) = api.patched_comments
    assert comment_id == FIRST_COMMENT_ID
    assert "NikolAI's AI models are busy or unavailable." in body
    runs = story_runs(database_url)
    assert len(runs) == 2
    statuses = {str(r["status"]) for r in runs}
    assert statuses == {"completed", "failed"}
