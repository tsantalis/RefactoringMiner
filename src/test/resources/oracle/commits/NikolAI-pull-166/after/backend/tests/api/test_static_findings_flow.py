"""Semgrep's findings on a PR, from GitHub's ``workflow_run`` webhook to storage (US-15, #151).

The webhook, the background job and the database are the real ones. GitHub's HTTP API is faked
through a real githubkit client, as for the refactoring report, and PR #7 is stored by its own
``pull_request`` delivery (a draft, so not reviewed). The stored findings are read back the way
the code review (#161) reads them.

``fixtures/semgrep/semgrep.json`` is Semgrep 1.183.0's real output (``semgrep scan --config
p/default --json``) on the PR's head commit:

- ``app/legacy.py``: ``eval`` on line 2;
- ``app/storage.py``: ``pickle.loads(`` over lines 5 to 7;
- ``app/tasks.py``: ``shell=True`` on lines 5 and 11, and ``eval`` on line 17.
"""

# AI contribution: 50%

import asyncio
import json
import logging
from collections.abc import Iterator
from pathlib import Path
from typing import Final

import httpx
import pytest
from fastapi.testclient import TestClient

from app.core.config import Settings, get_settings
from app.db import connect
from app.db.models.static_findings import StaticFinding
from app.db.repositories.static_findings import static_findings_for_commit
from app.main import app
from tests.api.installation_support import deliver
from tests.fake_github_api import (
    GitHubApi,
    artifact_zip,
    changed_file,
    pull_request,
    use_fake_github,
)
from tests.fakes import HEAD_SHA, PR_NUMBER, pull_request_payload, workflow_run_payload

SEMGREP: Final = "Semgrep"  # The workflow name in the wiki's template.
REPOSITORY_ID: Final = 100  # octocat/demo's GitHub ID in the payloads.
NEXT_SHA: Final = "fedcba9876543210fedcba9876543210fedcba98"
PULL_API: Final = f"/repos/octocat/demo/pulls/{PR_NUMBER}"
REPORT: Final = Path(__file__).parents[1] / "fixtures/semgrep/semgrep.json"
# The report's findings, in Semgrep's order.
EVAL_IN_LEGACY, PICKLE_IN_STORAGE, SHELL_IN_RUN, SHELL_IN_RUN_ALL, EVAL_IN_EVALUATE = range(5)

# The PR added evaluate() at the end of app/tasks.py (lines 14 to 17)...
TASKS_PATCH: Final = (
    "@@ -11,3 +11,7 @@ def run_all(commands):\n"
    "         shell=True,\n"
    "         check=False,\n"
    "     )\n"
    "+\n"
    "+\n"
    "+def evaluate(expression):\n"
    "+    return eval(expression)"
)
# ...and renamed what app/storage.py unpickles (line 6). It didn't touch app/legacy.py.
STORAGE_PATCH: Final = (
    "@@ -3,5 +3,5 @@ import pickle\n"
    " \n"
    " def restore(blob):\n"
    "     return pickle.loads(\n"
    "-        data\n"
    "+        blob\n"
    "     )"
)
EVAL_MESSAGE: Final = (
    "Detected the use of eval(). eval() can be dangerous if used to evaluate dynamic content. "
    "If this content can be input from outside the program, this may be a code injection "
    "vulnerability. Ensure evaluated content is not definable by external sources."
)


def semgrep_artifact(*kept: int, moved_to: str | None = None) -> bytes:
    """The workflow's artifact, with only the report's findings at the ``kept`` indices.

    ``moved_to`` puts those findings in another file, at the same lines.
    """
    report = json.loads(REPORT.read_text(encoding="utf-8"))
    report["results"] = [report["results"][index] for index in kept]
    for result in report["results"]:
        result["path"] = moved_to or result["path"]
    return artifact_zip({"semgrep.json": json.dumps(report).encode()})


class PushBeforeTheFilesAreListed(GitHubApi):
    """GitHub, where PR #7's head is NEXT_SHA by the time its files are listed.

    The student pushed while the artifact downloaded, so the file list is the newer commit's.
    """

    async def handle_async_request(self, request: httpx.Request) -> httpx.Response:
        if request.url.path == f"{PULL_API}/files" and self.pull is not None:
            self.pull["head"]["sha"] = NEXT_SHA
        return await super().handle_async_request(request)


def fake_github(
    monkeypatch: pytest.MonkeyPatch,
    artifact: bytes,
    head_sha: str = HEAD_SHA,
    api_class: type[GitHubApi] = GitHubApi,
) -> GitHubApi:
    """GitHub, where PR #7's head is ``head_sha`` and the Semgrep run uploaded ``artifact``."""
    api = api_class(
        artifact,
        pull=pull_request(PR_NUMBER, head_sha),
        artifact_name="semgrep-results",
        files=[
            changed_file("app/storage.py", STORAGE_PATCH),
            changed_file("app/tasks.py", TASKS_PATCH),
        ],
    )
    use_fake_github(monkeypatch, api)
    return api


def stored_findings(database_url: str, head_sha: str = HEAD_SHA) -> list[StaticFinding] | None:
    """What the code review (#161) reads for commit ``head_sha`` of PR #7."""

    async def read() -> list[StaticFinding] | None:
        async with await connect(database_url) as connection:
            return await static_findings_for_commit(connection, REPOSITORY_ID, PR_NUMBER, head_sha)

    return asyncio.run(read())


def places(findings: list[StaticFinding] | None) -> list[tuple[str, int, int]] | None:
    """Each finding's file and first and last lines."""
    if findings is None:
        return None
    return [(finding.file_path, finding.start_line, finding.end_line) for finding in findings]


@pytest.fixture
def github(course_settings: Settings) -> Iterator[TestClient]:
    """GitHub's side of the webhook: no session cookie and no Origin header."""
    app.dependency_overrides[get_settings] = lambda: course_settings
    try:
        yield TestClient(app, base_url="https://testserver")
    finally:
        app.dependency_overrides.clear()


@pytest.fixture
def stored_pull_request(github: TestClient) -> None:
    """PR #7 is stored by its own delivery: a draft, so it isn't reviewed."""
    assert deliver(github, "pull_request", pull_request_payload("opened", draft=True)) == 202


pytestmark = pytest.mark.usefixtures("stored_pull_request")


def test_a_finding_on_a_line_the_pull_request_added_is_stored_for_its_head_commit(
    github: TestClient, database_url: str, monkeypatch: pytest.MonkeyPatch
) -> None:
    fake_github(monkeypatch, semgrep_artifact(EVAL_IN_EVALUATE))

    status = deliver(github, "workflow_run", workflow_run_payload(name=SEMGREP))

    assert status == 202
    assert stored_findings(database_url) == [
        StaticFinding(
            rule_id="python.lang.security.audit.eval-detected.eval-detected",
            message=EVAL_MESSAGE,
            severity="WARNING",
            category="security",
            file_path="app/tasks.py",
            start_line=17,
            end_line=17,
        )
    ]


def test_only_findings_touching_a_line_the_pull_request_added_are_kept(
    github: TestClient, database_url: str, monkeypatch: pytest.MonkeyPatch
) -> None:
    fake_github(monkeypatch, semgrep_artifact(*range(5)))

    deliver(github, "workflow_run", workflow_run_payload(name=SEMGREP))

    # Dropped: app/legacy.py, which the PR didn't change; shell=True on line 5 of app/tasks.py,
    # outside the change; and shell=True on line 11, an unchanged line shown around the change.
    assert places(stored_findings(database_url)) == [
        ("app/storage.py", 5, 7),  # pickle.loads( starts on an unchanged line, ends past line 6
        ("app/tasks.py", 17, 17),
    ]


def test_a_run_with_no_finding_on_the_changed_lines_stores_an_empty_result(
    github: TestClient, database_url: str, monkeypatch: pytest.MonkeyPatch
) -> None:
    # An empty result tells the code review the commit was analysed; None says it wasn't.
    fake_github(monkeypatch, semgrep_artifact(EVAL_IN_LEGACY, SHELL_IN_RUN))

    deliver(github, "workflow_run", workflow_run_payload(name=SEMGREP))

    assert stored_findings(database_url) == []


@pytest.mark.parametrize(
    ("status", "lines", "kept"),
    [
        pytest.param("added", 3, [("app/generated.py", 2, 2)], id="added: every line is new"),
        pytest.param("added", 1, [], id="added: past the end of the file"),
        pytest.param("modified", 3, [], id="modified: no line to match"),
    ],
)
def test_a_file_without_a_patch_keeps_its_findings_only_when_the_pull_request_adds_it(
    github: TestClient,
    database_url: str,
    monkeypatch: pytest.MonkeyPatch,
    status: str,
    lines: int,
    kept: list[tuple[str, int, int]],
) -> None:
    # GitHub sends no patch for a diff too large to show, but still counts the lines added.
    # The finding is on line 2.
    api = fake_github(monkeypatch, semgrep_artifact(EVAL_IN_LEGACY, moved_to="app/generated.py"))
    api.files.append(changed_file("app/generated.py", None, status=status, additions=lines))

    deliver(github, "workflow_run", workflow_run_payload(name=SEMGREP))

    assert places(stored_findings(database_url)) == kept


def test_a_run_for_a_commit_that_is_no_longer_the_head_is_ignored(
    github: TestClient,
    database_url: str,
    monkeypatch: pytest.MonkeyPatch,
    caplog: pytest.LogCaptureFixture,
) -> None:
    caplog.set_level(logging.INFO, logger="app.services.static_findings")
    # The student pushed NEXT_SHA while Semgrep was still analysing HEAD_SHA.
    api = fake_github(monkeypatch, semgrep_artifact(EVAL_IN_EVALUATE), head_sha=NEXT_SHA)

    deliver(github, "workflow_run", workflow_run_payload(name=SEMGREP, head_sha=HEAD_SHA))

    assert [request.url.path for request in api.requests] == [PULL_API]  # nothing downloaded
    assert stored_findings(database_url, HEAD_SHA) is None
    assert "Skipped run 900 on octocat/demo#7: the PR has newer commits" in caplog.text


def test_a_push_during_the_download_is_not_matched_against_this_run(
    github: TestClient,
    database_url: str,
    monkeypatch: pytest.MonkeyPatch,
    caplog: pytest.LogCaptureFixture,
) -> None:
    caplog.set_level(logging.INFO, logger="app.services.static_findings")
    # The head only moves as the files are listed, so the job must check it after the listing.
    fake_github(
        monkeypatch, semgrep_artifact(EVAL_IN_EVALUATE), api_class=PushBeforeTheFilesAreListed
    )

    deliver(github, "workflow_run", workflow_run_payload(name=SEMGREP, head_sha=HEAD_SHA))

    assert stored_findings(database_url, HEAD_SHA) is None
    assert "Skipped run 900 on octocat/demo#7: the PR has newer commits" in caplog.text


@pytest.mark.parametrize(
    "artifact",
    [
        pytest.param(b"PK\x03\x04 cut short", id="corrupt zip"),
        pytest.param(artifact_zip({"semgrep.json": b'{"results": "none"}'}), id="not Semgrep JSON"),
        pytest.param(artifact_zip({"semgrep.sarif": b"{}"}), id="no semgrep.json"),
    ],
)
def test_a_broken_artifact_is_logged_and_skipped(
    github: TestClient,
    database_url: str,
    monkeypatch: pytest.MonkeyPatch,
    caplog: pytest.LogCaptureFixture,
    artifact: bytes,
) -> None:
    fake_github(monkeypatch, artifact)

    status = deliver(github, "workflow_run", workflow_run_payload(name=SEMGREP))

    assert status == 202
    assert stored_findings(database_url) is None
    problems = [
        record.getMessage()
        for record in caplog.records
        if record.levelno >= logging.WARNING and "octocat/demo#7" in record.getMessage()
    ]
    assert len(problems) == 1, problems


def test_a_rerun_on_the_same_commit_replaces_its_findings(
    github: TestClient, database_url: str, monkeypatch: pytest.MonkeyPatch
) -> None:
    api = fake_github(monkeypatch, semgrep_artifact(PICKLE_IN_STORAGE, EVAL_IN_EVALUATE))
    deliver(github, "workflow_run", workflow_run_payload(name=SEMGREP))
    # The job is re-run on the same commit, and Semgrep's rules no longer flag pickle.
    api.artifact_zip = semgrep_artifact(EVAL_IN_EVALUATE)

    deliver(github, "workflow_run", workflow_run_payload(name=SEMGREP))

    assert places(stored_findings(database_url)) == [("app/tasks.py", 17, 17)]


@pytest.mark.parametrize(
    "run",
    [
        # Semgrep only fails on an error of its own, so its results may be missing or partial.
        pytest.param(workflow_run_payload(name=SEMGREP, conclusion="failure"), id="failed run"),
        # GitHub doesn't link a run to a PR from a fork.
        pytest.param(workflow_run_payload(name=SEMGREP, pr_numbers=()), id="PR from a fork"),
    ],
)
def test_a_run_that_did_not_succeed_on_a_pull_request_is_ignored(
    github: TestClient, database_url: str, monkeypatch: pytest.MonkeyPatch, run: dict[str, object]
) -> None:
    api = fake_github(monkeypatch, semgrep_artifact(EVAL_IN_EVALUATE))

    status = deliver(github, "workflow_run", run)

    assert status == 202
    assert api.requests == []  # GitHub isn't even asked
    assert stored_findings(database_url) is None


def test_the_findings_of_a_pull_request_nikolai_does_not_store_are_skipped(
    github: TestClient, monkeypatch: pytest.MonkeyPatch, caplog: pytest.LogCaptureFixture
) -> None:
    caplog.set_level(logging.INFO, logger="app.services.static_findings")
    # PR #8 was opened before the App was installed, so no pull_request delivery stored it.
    api = fake_github(monkeypatch, semgrep_artifact(EVAL_IN_EVALUATE))
    api.pull = pull_request(8, HEAD_SHA)

    status = deliver(github, "workflow_run", workflow_run_payload(name=SEMGREP, pr_numbers=(8,)))

    assert status == 202
    assert (
        "Did not store the static findings of octocat/demo#8 (run 900): "
        "NikolAI doesn't store this PR" in caplog.text
    )


def test_a_job_that_cannot_act_as_the_app_logs_the_run(
    github: TestClient, course_settings: Settings, caplog: pytest.LogCaptureFixture
) -> None:
    # No fake GitHub: the job builds the real App client, and this server has no App ID.
    app.dependency_overrides[get_settings] = lambda: course_settings.model_copy(
        update={"github_app_id": None}
    )

    status = deliver(github, "workflow_run", workflow_run_payload(name=SEMGREP))

    assert status == 202
    assert "Cannot act as the GitHub App on octocat/demo (run 900): APP_ID is not set" in (
        caplog.text
    )


def test_an_unreadable_reply_from_github_is_logged_with_the_run(
    github: TestClient, monkeypatch: pytest.MonkeyPatch, caplog: pytest.LogCaptureFixture
) -> None:
    api = fake_github(monkeypatch, semgrep_artifact(EVAL_IN_EVALUATE))
    api.pull = {"number": PR_NUMBER}  # not a pull request githubkit can read

    status = deliver(github, "workflow_run", workflow_run_payload(name=SEMGREP))

    assert status == 202
    assert "Unexpected error while storing the static findings of octocat/demo#7 (run 900)" in (
        caplog.text
    )
