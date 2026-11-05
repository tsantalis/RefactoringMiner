"""Parse GitHub PR events, including the metadata needed for US-4.7 persistence."""

from datetime import UTC, datetime
from typing import cast

import pytest
from pydantic import ValidationError

from app.github.payloads import PullRequestEvent
from tests.fakes import INSTALLATION_ID, PR_NUMBER, PR_TITLE, pull_request_payload


def test_reads_the_fields_nikolai_uses_and_ignores_the_rest() -> None:
    event = PullRequestEvent.model_validate(pull_request_payload(body="Fixes #2"))

    assert event.action == "opened"
    assert event.installation.id == INSTALLATION_ID
    assert event.repository.id == 100
    assert event.repository.full_name == "octocat/demo"
    assert event.repository.owner.login == "octocat"
    assert event.pull_request.number == PR_NUMBER
    assert event.pull_request.id == 200
    assert event.pull_request.title == PR_TITLE
    assert event.pull_request.body == "Fixes #2"
    assert event.pull_request.user.login == "student1"
    assert event.pull_request.user.id == 10
    assert event.sender.id == 20
    assert event.sender.login == "maintainer"
    assert event.pull_request.state == "open"
    assert event.pull_request.created_at == datetime(2026, 10, 1, 10, tzinfo=UTC)
    assert event.pull_request.updated_at == datetime(2026, 10, 2, 12, tzinfo=UTC)
    assert "private" not in event.repository.model_dump()


def test_optional_fields_have_safe_defaults() -> None:
    event = PullRequestEvent.model_validate(
        {
            "action": "opened",
            "installation": {"id": 1},
            "sender": {"id": 20, "login": "actor"},
            "repository": {
                "id": 100,
                "name": "demo",
                "full_name": "o/demo",
                "owner": {"login": "o"},
            },
            "pull_request": {
                "id": 200,
                "number": 1,
                "title": "T",
                "state": "open",
                "created_at": "2026-10-01T10:00:00Z",
                "updated_at": "2026-10-02T12:00:00Z",
                "user": {"id": 10, "login": "u"},
            },
        }
    )

    assert event.pull_request.body is None
    assert event.pull_request.draft is False
    assert event.pull_request.user.type == "User"
    assert event.sender.type == "User"
    assert event.pull_request.merged_at is None
    assert event.pull_request.merged is False


@pytest.mark.parametrize("merged", [False, True])
def test_closed_prs_can_be_merged_or_unmerged(*, merged: bool) -> None:
    payload = pull_request_payload("closed")
    pr = cast("dict[str, object]", payload["pull_request"])
    pr["merged"] = merged
    pr["merged_at"] = "2026-10-02T12:00:00Z" if merged else None

    event = PullRequestEvent.model_validate(payload)

    assert event.pull_request.state == "closed"
    assert event.pull_request.merged is merged
    assert event.pull_request.merged_at == (
        datetime(2026, 10, 2, 12, tzinfo=UTC) if merged else None
    )


@pytest.mark.parametrize(
    "path",
    [
        ("repository", "id"),
        ("pull_request", "id"),
        ("pull_request", "user", "id"),
        ("pull_request", "created_at"),
        ("pull_request", "updated_at"),
        ("pull_request", "state"),
        ("sender",),
        ("sender", "id"),
    ],
)
def test_rejects_missing_persistence_metadata(path: tuple[str, ...]) -> None:
    payload = pull_request_payload()
    container = payload
    for key in path[:-1]:
        container = cast("dict[str, object]", container[key])
    del container[path[-1]]

    with pytest.raises(ValidationError) as error:
        PullRequestEvent.model_validate(payload)

    assert error.value.errors()[0]["loc"] == path


@pytest.mark.parametrize(
    ("field", "value"),
    [("state", "merged"), ("created_at", "not-a-date"), ("updated_at", "not-a-date")],
)
def test_rejects_invalid_pr_state_or_timestamps(field: str, value: str) -> None:
    payload = pull_request_payload()
    cast("dict[str, object]", payload["pull_request"])[field] = value

    with pytest.raises(ValidationError) as error:
        PullRequestEvent.model_validate(payload)

    assert error.value.errors()[0]["loc"] == ("pull_request", field)


def test_rejects_a_payload_without_an_installation() -> None:
    payload = pull_request_payload()
    del payload["installation"]

    with pytest.raises(ValidationError):
        PullRequestEvent.model_validate(payload)
