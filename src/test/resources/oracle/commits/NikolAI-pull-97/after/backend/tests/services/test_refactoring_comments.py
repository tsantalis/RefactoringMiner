"""Tests for posting the RefactoringMiner report on a PR once the Action's run completes (US-8)."""

# AI contribution: 50% or more AI-generated

import io
import tarfile
import zipfile
from collections.abc import AsyncIterator
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock

import pytest
from githubkit.exception import GitHubException
from pydantic import SecretStr

from app.core.config import Settings
from app.github.payloads import WorkflowRunEvent
from app.services import refactoring_comments
from app.services.refactoring_comments import handle_refactoring_run, wants_refactoring_report
from app.services.refactoring_report import REPORT_MARKER
from tests.fakes import (
    HEAD_SHA,
    PR_NUMBER,
    REFACTORING_WORKFLOW_RUN_ID,
    make_fake_github,
    workflow_run_payload,
)

APP_ID = "123"
PR_URL = f"https://github.com/octocat/demo/pull/{PR_NUMBER}"
SAMPLE = Path(__file__).parents[1] / "fixtures/refactoringminer/refactorings.json"


def artifact_zip(files: dict[str, bytes]) -> bytes:
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as archive:
        for name, content in files.items():
            archive.writestr(name, content)
    return buffer.getvalue()


def download(content: bytes) -> SimpleNamespace:
    async def chunks() -> AsyncIterator[bytes]:
        yield content[:100]
        yield content[100:]

    return SimpleNamespace(aiter_bytes=chunks)


def app_comment(comment_id: int, body: str, app_id: int = int(APP_ID)) -> SimpleNamespace:
    return SimpleNamespace(
        id=comment_id, body=body, performed_via_github_app=SimpleNamespace(id=app_id)
    )


@pytest.fixture
def settings() -> Settings:
    return Settings(_env_file=None).model_copy(
        update={"github_app_id": APP_ID, "github_private_key": SecretStr("fake-key")}
    )


@pytest.fixture
def github(monkeypatch: pytest.MonkeyPatch) -> MagicMock:
    fake = make_fake_github()
    fake.rest.pulls.async_get = AsyncMock(
        return_value=SimpleNamespace(
            parsed_data=SimpleNamespace(html_url=PR_URL, head=SimpleNamespace(sha=HEAD_SHA))
        )
    )
    fake.rest.actions.async_list_workflow_run_artifacts = AsyncMock(
        return_value=SimpleNamespace(
            parsed_data=SimpleNamespace(
                artifacts=[
                    SimpleNamespace(
                        id=55, name="refactoring-diff", expired=False, size_in_bytes=2_000
                    )
                ]
            )
        )
    )
    fake.rest.actions.async_download_artifact = AsyncMock(
        return_value=download(artifact_zip({"refactorings.json": SAMPLE.read_bytes()}))
    )
    fake.rest.issues.async_update_comment = AsyncMock()
    monkeypatch.setattr(
        refactoring_comments, "installation_client", lambda _settings, _installation: fake
    )
    return fake


def run_event(**overrides: object) -> WorkflowRunEvent:
    return WorkflowRunEvent.model_validate(workflow_run_payload(**overrides))  # type: ignore[arg-type]


async def test_posts_the_report_grouped_by_type_on_the_pull_request(
    github: MagicMock, settings: Settings
) -> None:
    await handle_refactoring_run(run_event(), settings)

    github.rest.actions.async_list_workflow_run_artifacts.assert_awaited_once_with(
        owner="octocat", repo="demo", run_id=REFACTORING_WORKFLOW_RUN_ID, per_page=100
    )
    github.rest.issues.async_create_comment.assert_awaited_once()
    body = github.rest.issues.async_create_comment.await_args.kwargs["body"]
    assert github.rest.issues.async_create_comment.await_args.kwargs["issue_number"] == PR_NUMBER
    assert body.startswith(REPORT_MARKER)
    assert "Found 3 refactorings: 2 Rename Method, 1 Extract Method" in body
    assert f"]({PR_URL}/changes?diff=split#diff-aa11R101)" in body


async def test_a_new_push_updates_nikolais_existing_report_in_place(
    github: MagicMock, settings: Settings
) -> None:
    github.rest.issues.async_list_comments.return_value = SimpleNamespace(
        parsed_data=[
            app_comment(1, f"{REPORT_MARKER}\nforged by another app", app_id=999),
            SimpleNamespace(id=2, body="lgtm", performed_via_github_app=None),
            app_comment(3, f"{REPORT_MARKER}\nold report"),
        ]
    )

    await handle_refactoring_run(run_event(), settings)

    github.rest.issues.async_create_comment.assert_not_awaited()
    github.rest.issues.async_update_comment.assert_awaited_once()
    update = github.rest.issues.async_update_comment.await_args.kwargs
    assert update["comment_id"] == 3
    assert "Found 3 refactorings" in update["body"]


async def test_a_run_for_an_older_commit_does_not_replace_the_newer_report(
    github: MagicMock, settings: Settings
) -> None:
    github.rest.pulls.async_get.return_value.parsed_data.head.sha = "f" * 40

    await handle_refactoring_run(run_event(), settings)

    github.rest.actions.async_download_artifact.assert_not_awaited()
    github.rest.issues.async_create_comment.assert_not_awaited()


async def test_reads_the_report_from_the_pages_artifact_on_public_repositories(
    github: MagicMock, settings: Settings
) -> None:
    github.rest.actions.async_list_workflow_run_artifacts.return_value.parsed_data.artifacts = [
        SimpleNamespace(id=56, name="pages-store", expired=False, size_in_bytes=10),
        SimpleNamespace(id=57, name="github-pages", expired=False, size_in_bytes=10),
    ]
    site = io.BytesIO()
    with tarfile.open(fileobj=site, mode="w") as archive:
        report = SAMPLE.read_bytes()
        info = tarfile.TarInfo(f"./refactorings/pr-{PR_NUMBER}/refactorings.json")
        info.size = len(report)
        archive.addfile(info, io.BytesIO(report))
    github.rest.actions.async_download_artifact.return_value = download(
        artifact_zip({"artifact.tar": site.getvalue()})
    )

    await handle_refactoring_run(run_event(), settings)

    assert github.rest.actions.async_download_artifact.await_args.kwargs["artifact_id"] == 57
    github.rest.issues.async_create_comment.assert_awaited_once()


@pytest.mark.parametrize(
    "artifacts",
    [
        [],
        [SimpleNamespace(id=55, name="refactoring-diff", expired=True, size_in_bytes=10)],
        [SimpleNamespace(id=55, name="refactoring-diff", expired=False, size_in_bytes=10**10)],
    ],
)
async def test_without_a_usable_artifact_nothing_is_posted(
    github: MagicMock, settings: Settings, artifacts: list[SimpleNamespace]
) -> None:
    github.rest.actions.async_list_workflow_run_artifacts.return_value.parsed_data.artifacts = (
        artifacts
    )

    await handle_refactoring_run(run_event(), settings)

    github.rest.actions.async_download_artifact.assert_not_awaited()
    github.rest.issues.async_create_comment.assert_not_awaited()


async def test_github_and_report_errors_never_escape_the_background_job(
    github: MagicMock, settings: Settings
) -> None:
    github.rest.pulls.async_get.side_effect = GitHubException("boom")

    await handle_refactoring_run(run_event(pr_numbers=(PR_NUMBER, 8)), settings)

    assert github.rest.pulls.async_get.await_count == 2  # one PR failing doesn't stop the next
    github.rest.issues.async_create_comment.assert_not_awaited()


async def test_an_unreadable_report_is_not_posted(github: MagicMock, settings: Settings) -> None:
    github.rest.actions.async_download_artifact.return_value = download(
        artifact_zip({"refactorings.json": b"not json"})
    )

    await handle_refactoring_run(run_event(), settings)

    github.rest.issues.async_create_comment.assert_not_awaited()


@pytest.mark.parametrize(
    ("overrides", "expected"),
    [
        ({}, True),
        ({"name": "CI"}, False),
        ({"conclusion": "failure"}, False),
        ({"action": "requested", "conclusion": None}, False),
        ({"pr_numbers": ()}, False),  # a PR from a fork: GitHub doesn't say which PR
        ({"base_repository_id": 555}, False),  # a PR into another repository
    ],
)
def test_only_successful_refactoring_report_runs_on_a_pull_request_are_reported(
    overrides: dict[str, object], expected: bool
) -> None:
    assert wants_refactoring_report(run_event(**overrides)) is expected
