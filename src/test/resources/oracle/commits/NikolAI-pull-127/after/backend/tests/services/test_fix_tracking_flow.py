"""A push to a reviewed PR ticks the items it fixed, with evidence (US-9.4), on PostgreSQL.

The PR is first reviewed at ``FIRST_HEAD``, where the code review finds ``FINDING`` on line 1 of
a.py. The student then pushes ``FIX_COMMIT``, which changes that line; the fix-check agent's
reply is scripted, and NikolAI checks each citation against the push GitHub's compare API
reports.
"""

# AI contribution: 50%

import json
from pathlib import Path
from types import SimpleNamespace
from typing import Final
from unittest.mock import AsyncMock, MagicMock
from uuid import UUID, uuid4

import pytest
from pydantic import SecretStr

from app.core.config import Settings
from app.db import connect
from app.db.models.pull_insights import StoredChecklist
from app.db.repositories.feedback import find_pull_request, find_repository
from app.db.repositories.finding_actions import find_finding_target, record_finding_action
from app.db.repositories.insights import checklist_review
from app.db.repositories.pr_events import save_pr_event
from app.github.payloads import PullRequestEvent
from app.services import pr_review
from app.services.pull_requests import pull_request_feedback
from tests.fakes import (
    PR_NUMBER,
    FakeProvider,
    make_fake_github,
    pull_request_payload,
    sample_completion,
)

APP_ID: Final = "123"
FIRST_HEAD: Final = "1" * 40
FIX_COMMIT: Final = "2" * 40
LATER_COMMIT: Final = "3" * 40
UNRELATED_COMMIT: Final = "9" * 40
FINDING: Final = {
    "name": "Unclear variable name",
    "description": "Rename y to say what it holds.",
    "severity": "medium",
    "category": "maintainability",
    "file_path": "a.py",
    "start_line": 1,
    "end_line": 1,
    "confidence": 0.8,
}
# FIX_COMMIT renames y on line 1 of a.py.
FIX_PATCH: Final = "@@ -1 +1 @@\n-y = 0\n+total = 0"
FIX: Final = {"item": 1, "commit": FIX_COMMIT, "file_path": "a.py", "start_line": 1, "end_line": 1}
NEW_ISSUE: Final = FINDING | {
    "name": "Missing test",
    "description": "Test the renamed total.",
    "category": "testing",
    "commit": FIX_COMMIT,
}
PR_URL: Final = f"https://github.com/octocat/demo/pull/{PR_NUMBER}"


@pytest.fixture
def settings(database_url: str, tmp_path: Path) -> Settings:
    return Settings(_env_file=None).model_copy(
        update={
            "database_url": SecretStr(database_url),
            "pr_log_dir": tmp_path,
            "github_app_id": APP_ID,
        }
    )


@pytest.fixture
def github(monkeypatch: pytest.MonkeyPatch) -> MagicMock:
    fake = make_fake_github()
    monkeypatch.setattr(pr_review, "installation_client", lambda *_args: fake)
    return fake


def push(
    github: MagicMock,
    *commits: tuple[str, dict[str, str]],
    status: str = "ahead",
    merges: frozenset[str] = frozenset(),
) -> None:
    """GitHub's answers for a push of ``commits`` (SHA, then each changed file's patch); the
    ``merges`` have two parents."""
    github.rest.repos.async_compare_commits = AsyncMock(
        return_value=SimpleNamespace(
            parsed_data=SimpleNamespace(
                status=status,
                commits=[
                    SimpleNamespace(sha=sha, parents=[{}, {}] if sha in merges else [{}])
                    for sha, _ in commits
                ],
                total_commits=len(commits),
            )
        )
    )
    by_sha = {
        sha: SimpleNamespace(
            sha=sha,
            commit=SimpleNamespace(message=f"Commit {sha[:7]}"),
            files=[
                SimpleNamespace(
                    filename=path, patch=patch, status="modified", previous_filename=None
                )
                for path, patch in files.items()
            ],
        )
        for sha, files in commits
    }
    github.rest.repos.async_get_commit = AsyncMock(
        side_effect=lambda _owner, _repo, ref: SimpleNamespace(parsed_data=by_sha[ref])
    )


async def review(settings: Settings, provider: FakeProvider, action: str, head: str) -> None:
    payload = pull_request_payload(action, head_sha=head)
    assert settings.database_url is not None
    async with await connect(settings.database_url.get_secret_value()) as connection:
        saved = await save_pr_event(connection, uuid4(), payload)
    await pr_review.handle_pull_request_event(
        PullRequestEvent.model_validate(payload), provider, settings, saved.event_id
    )


def fix_check(fixed: list[dict[str, object]], findings: list[dict[str, object]]) -> FakeProvider:
    reply = json.dumps({"fixed": fixed, "findings": findings})
    return FakeProvider(fix_check=sample_completion(reply))


async def first_review(settings: Settings, github: MagicMock) -> None:
    """The PR's first review, which posts the checklist comment through NikolAI's App."""
    provider = FakeProvider(code_review=sample_completion(json.dumps({"findings": [FINDING]})))
    await review(settings, provider, "opened", FIRST_HEAD)
    body = github.rest.issues.async_create_comment.await_args.kwargs["body"]
    github.rest.issues.async_list_comments.return_value.parsed_data = [
        SimpleNamespace(
            id=12345,
            body=body,
            user=SimpleNamespace(type="Bot"),
            performed_via_github_app=SimpleNamespace(id=int(APP_ID)),
        )
    ]
    github.rest.issues.async_create_comment.reset_mock()


async def pull_request_id(database_url: str) -> UUID:
    async with await connect(database_url) as connection:
        repository = await find_repository(connection, "octocat/demo")
        assert repository is not None
        pr = await find_pull_request(connection, repository.id, PR_NUMBER)
        assert pr is not None
        return pr.id


async def checklist(database_url: str) -> StoredChecklist:
    async with await connect(database_url) as connection:
        stored = await checklist_review(connection, await pull_request_id(database_url))
    assert stored is not None
    return stored


async def checklist_names(database_url: str) -> list[str]:
    async with await connect(database_url) as connection:
        repository = await find_repository(connection, "octocat/demo")
        assert repository is not None
        feedback = await pull_request_feedback(connection, repository, PR_NUMBER)
        assert feedback is not None
        return [finding.name for finding in feedback.findings]


def edited_comment(github: MagicMock) -> str:
    """The body NikolAI last edited its review comment to."""
    edit = github.rest.issues.async_update_comment.await_args.kwargs
    assert edit["comment_id"] == 12345
    body: str = edit["body"]
    return body


async def test_a_cited_fix_ticks_the_item_in_the_comment_and_the_checklist(
    settings: Settings, database_url: str, github: MagicMock
) -> None:
    await first_review(settings, github)
    push(github, (FIX_COMMIT, {"a.py": FIX_PATCH}))
    provider = fix_check([FIX], [])

    await review(settings, provider, "synchronize", FIX_COMMIT)

    assert len(provider.fix_check_calls) == 1
    assert provider.code_review_calls == []  # the push isn't reviewed in full again
    stored = await checklist(database_url)
    assert stored.statuses == ["fixed"]
    fix = stored.fixes[0]
    assert fix is not None
    assert (fix.commit, fix.file_path, fix.start_line, fix.end_line) == (FIX_COMMIT, "a.py", 1, 1)
    # Edited in place, not posted again.
    github.rest.issues.async_create_comment.assert_not_awaited()
    github.rest.issues.async_delete_comment.assert_not_awaited()
    body = edited_comment(github)
    assert (
        "- [x] ~~**Unclear variable name**~~ · Fixed in "
        f"[2222222]({PR_URL}/commits/{FIX_COMMIT}): [a.py#L1]({PR_URL}/commits/{FIX_COMMIT}"
        # sha256("a.py"), as in the code review's flow test.
        "#diff-f0de093a8c24f6a3ed1c71a2eedb8d29cb68daa281b1dd2323b67d75ce3fb800R1)"
    ) in body
    assert "- [ ] **Unclear variable name**" not in body


@pytest.mark.parametrize(
    "citation",
    [
        FIX | {"commit": UNRELATED_COMMIT},  # a commit outside the push
        FIX | {"start_line": 5, "end_line": 5},  # a line outside the commit's diff
        FIX | {"file_path": "b.py"},  # a file the commit didn't change
        FIX | {"item": 2},  # an item the checklist doesn't have
    ],
)
async def test_an_unverifiable_citation_leaves_the_item_open(
    settings: Settings, database_url: str, github: MagicMock, citation: dict[str, object]
) -> None:
    await first_review(settings, github)
    push(github, (FIX_COMMIT, {"a.py": FIX_PATCH}))

    await review(settings, fix_check([citation], []), "synchronize", FIX_COMMIT)

    stored = await checklist(database_url)
    assert stored.statuses == ["open"]
    assert stored.fixes == [None]
    assert "- [ ] **Unclear variable name**" in edited_comment(github)


async def test_a_new_issue_in_the_push_is_added_as_an_open_item(
    settings: Settings, database_url: str, github: MagicMock
) -> None:
    await first_review(settings, github)
    push(github, (FIX_COMMIT, {"a.py": FIX_PATCH}))

    await review(settings, fix_check([FIX], [NEW_ISSUE]), "synchronize", FIX_COMMIT)

    # Open items first, the fixed one last.
    assert await checklist_names(database_url) == ["Missing test", "Unclear variable name"]
    assert (await checklist(database_url)).statuses == ["open", "fixed"]
    assert "- [ ] **Missing test** · Medium · testing" in edited_comment(github)


async def test_a_new_issue_follows_the_later_commits_to_the_head_lines(
    settings: Settings, database_url: str, github: MagicMock
) -> None:
    await first_review(settings, github)
    # FIX_COMMIT adds two lines to b.py; LATER_COMMIT then inserts three lines above them.
    push(
        github,
        (FIX_COMMIT, {"b.py": "@@ -1,0 +2,2 @@\n+a = 1\n+b = a"}),
        (LATER_COMMIT, {"b.py": "@@ -1,3 +1,6 @@\n+x\n+y\n+z\n first\n a = 1\n b = a"}),
    )
    issue = NEW_ISSUE | {"file_path": "b.py", "start_line": 3, "end_line": 3}

    await review(settings, fix_check([], [issue]), "synchronize", LATER_COMMIT)

    async with await connect(database_url) as connection:
        repository = await find_repository(connection, "octocat/demo")
        assert repository is not None
        feedback = await pull_request_feedback(connection, repository, PR_NUMBER)
    assert feedback is not None
    added = next(finding for finding in feedback.findings if finding.name == "Missing test")
    assert [(loc.file_path, loc.start_line) for loc in added.locations] == [("b.py", 6)]


async def test_an_acknowledged_item_stays_acknowledged_after_a_push(
    settings: Settings, database_url: str, github: MagicMock
) -> None:
    await first_review(settings, github)
    async with await connect(database_url) as connection:
        # The PR's author, saved with its first event.
        cursor = await connection.execute("SELECT id FROM users WHERE github_login = 'student1'")
        user = await cursor.fetchone()
        assert user is not None
        first_item = (await checklist(database_url)).finding_ids[0]
        target = await find_finding_target(connection, first_item)
        assert target is not None
        await record_finding_action(
            connection,
            target=target,
            user_id=user["id"],
            action_type="acknowledged",
            reason=None,
        )
    push(github, (FIX_COMMIT, {"a.py": FIX_PATCH}))

    await review(settings, fix_check([], []), "synchronize", FIX_COMMIT)

    stored = await checklist(database_url)
    assert stored.finding_ids[0] != target.finding_id  # a new review, the same item
    assert stored.statuses == ["acknowledged"]


async def test_a_force_push_gets_a_full_code_review(
    settings: Settings, database_url: str, github: MagicMock
) -> None:
    await first_review(settings, github)
    push(github, (FIX_COMMIT, {"a.py": FIX_PATCH}), status="diverged")
    provider = fix_check([FIX], [])

    await review(settings, provider, "synchronize", FIX_COMMIT)

    assert provider.fix_check_calls == []
    assert len(provider.code_review_calls) == 1
    # The full review found no issue this time (the fake's default reply).
    assert await checklist_names(database_url) == []


async def test_a_push_to_a_pr_without_a_checklist_gets_a_full_code_review(
    settings: Settings, github: MagicMock
) -> None:
    provider = FakeProvider()

    await review(settings, provider, "synchronize", FIX_COMMIT)

    assert provider.fix_check_calls == []
    assert len(provider.code_review_calls) == 1


async def test_a_late_event_for_a_commit_the_checklist_covers_leaves_it_as_it_is(
    settings: Settings, database_url: str, github: MagicMock
) -> None:
    await first_review(settings, github)
    push(github, (FIX_COMMIT, {"a.py": FIX_PATCH}))
    await review(settings, fix_check([FIX], []), "synchronize", FIX_COMMIT)
    # A retried event for the first head arrives after the push was reviewed.
    push(github, status="behind")
    provider = fix_check([], [])

    await review(settings, provider, "synchronize", FIRST_HEAD)

    assert (provider.fix_check_calls, provider.code_review_calls) == ([], [])
    assert (await checklist(database_url)).statuses == ["fixed"]


async def test_a_merge_commit_is_not_shown_or_citable(
    settings: Settings, database_url: str, github: MagicMock
) -> None:
    await first_review(settings, github)
    # The base branch merged in: its diff repeats the base's changes to a.py.
    push(github, (FIX_COMMIT, {"a.py": FIX_PATCH}), merges=frozenset({FIX_COMMIT}))
    provider = fix_check([FIX], [])

    await review(settings, provider, "synchronize", FIX_COMMIT)

    assert provider.fix_check_calls == []  # nothing of the student's to show
    assert (await checklist(database_url)).statuses == ["open"]


async def test_an_older_review_published_late_leaves_the_newer_comment(
    settings: Settings, database_url: str, github: MagicMock
) -> None:
    await first_review(settings, github)
    push(github, (FIX_COMMIT, {"a.py": FIX_PATCH}))
    await review(settings, fix_check([FIX], []), "synchronize", FIX_COMMIT)
    # GitHub now holds the edited comment, with the push review's publication marker.
    comment = github.rest.issues.async_list_comments.return_value.parsed_data[0]
    comment.body = edited_comment(github)
    github.rest.issues.async_update_comment.reset_mock()
    async with await connect(database_url) as connection:
        # The first review's publication, as if its post had failed and were retried now.
        cursor = await connection.execute(
            """UPDATE github_publications SET status = 'failed'
               WHERE id = (SELECT id FROM github_publications ORDER BY created_at LIMIT 1)
               RETURNING id"""
        )
        first = await cursor.fetchone()
    assert first is not None

    assert await pr_review.publish_saved_review(settings, first["id"])

    github.rest.issues.async_update_comment.assert_not_awaited()
    github.rest.issues.async_create_comment.assert_not_awaited()
    async with await connect(database_url) as connection:
        cursor = await connection.execute(
            "SELECT status, error_message FROM github_publications WHERE id = %s", (first["id"],)
        )
        assert await cursor.fetchone() == {"status": "failed", "error_message": "Superseded"}


async def test_a_fixed_issue_that_comes_back_reopens_its_item(
    settings: Settings, database_url: str, github: MagicMock
) -> None:
    await first_review(settings, github)
    push(github, (FIX_COMMIT, {"a.py": FIX_PATCH}))
    await review(settings, fix_check([FIX], []), "synchronize", FIX_COMMIT)
    first_item = (await checklist(database_url)).finding_ids[0]
    # LATER_COMMIT brings the unclear name back, on line 2 of a.py.
    push(github, (LATER_COMMIT, {"a.py": "@@ -1 +1,2 @@\n total = 0\n+y = total"}))
    back = FINDING | {"commit": LATER_COMMIT, "start_line": 2, "end_line": 2}

    await review(settings, fix_check([], [back]), "synchronize", LATER_COMMIT)

    stored = await checklist(database_url)
    assert stored.statuses == ["open"]  # the same item, reopened, not a second one
    assert stored.fixes == [None]
    assert stored.finding_ids[0] != first_item
    assert "- [ ] **Unclear variable name**" in edited_comment(github)
    assert "a.py#L2" in edited_comment(github)
