"""Store the Semgrep findings a team's workflow reports on a PR's changes (US-15, #151).

Each team's repository runs Semgrep in a workflow named "Semgrep" (the wiki's Static Analysis
page has the template). When a run completes, GitHub sends the App a ``workflow_run`` event, and
the webhook schedules ``handle_static_analysis_run`` in the background, which:

1. signs in as the App's installation on that repository;
2. skips runs for a commit that is no longer the PR's head: the newer push gets its own run;
3. downloads the run's ``semgrep-results`` artifact and reads ``semgrep.json``;
4. lists the PR's changed files, then checks the head again: after a push during the download,
   the list is the newer commit's, whose lines don't match this run's;
5. keeps the findings that touch a line the PR added, so a student isn't blamed for old code:
   a finding on an unchanged line, or in a file the PR didn't change, is dropped, and every line
   of a file the PR adds counts, even when GitHub sends no patch for it;
6. saves them for the PR and head commit, where the code review (#161) reads them.

A failure at any step is logged and the later steps are skipped: the next push brings a new run.
"""

# AI contribution: 50%

import bisect
import logging
from collections.abc import Sequence
from typing import Final

from githubkit import GitHub
from githubkit.auth import AppInstallationAuthStrategy
from githubkit.exception import GitHubException
from githubkit_schemas.latest.models import DiffEntry, PullRequest

from app.core.config import ConfigurationError, Settings
from app.core.logging_config import log_safe_exception
from app.db.models.static_findings import NewStaticAnalysisRun, StaticFinding
from app.db.repositories.static_findings import save_static_analysis_run
from app.db.steps import DatabaseStepError, database_url, in_database
from app.github.action_artifacts import ArtifactError, download_semgrep_json
from app.github.client import installation_client
from app.github.payloads import WorkflowRunEvent
from app.services.line_mapping import added_lines
from app.services.pull_request_details import list_changed_files
from app.services.semgrep_report import SemgrepReportError, parse_semgrep_findings
from app.services.workflow_runs import (
    is_current_head,
    pull_request_numbers,
    succeeded_on_pull_request,
)

logger = logging.getLogger(__name__)

# The workflow name in the wiki's template, which team repositories copy.
STATIC_ANALYSIS_WORKFLOW: Final = "Semgrep"


def wants_static_findings(event: WorkflowRunEvent) -> bool:
    """Read successful "Semgrep" runs that GitHub links to a PR."""
    return succeeded_on_pull_request(event, STATIC_ANALYSIS_WORKFLOW)


async def handle_static_analysis_run(event: WorkflowRunEvent, settings: Settings) -> None:
    """Background job for one completed run: store the findings of each of its PRs. Never raises."""
    repository = event.repository.full_name
    try:
        client = installation_client(settings, event.installation.id)
    except ConfigurationError as error:
        logger.error(
            "Cannot act as the GitHub App on %s (run %d): %s",
            repository,
            event.workflow_run.id,
            error,
        )
        return
    # One open client for the whole job, as the artifact streams in (see action_artifacts).
    async with client as github:
        for number in pull_request_numbers(event):
            label = f"{repository}#{number}"
            try:
                await _store_findings(github, event, number, settings, label)
            except (GitHubException, ArtifactError, SemgrepReportError) as error:
                # Only the type: the message can quote the team's artifact.
                logger.error(
                    "Could not store the static findings of %s (run %d): %s",
                    label,
                    event.workflow_run.id,
                    type(error).__name__,
                )
            except Exception as error:
                # Last resort: a bug here is logged without its values, and the run's other
                # PRs are still read.
                log_safe_exception(
                    logger,
                    "Unexpected error while storing the static findings of %s (run %d)",
                    label,
                    event.workflow_run.id,
                    error=error,
                )


async def _store_findings(
    github: GitHub[AppInstallationAuthStrategy],
    event: WorkflowRunEvent,
    number: int,
    settings: Settings,
    label: str,
) -> None:
    owner, repo = event.repository.owner.login, event.repository.name
    run = event.workflow_run
    if await _current_pull(github, event, number, label) is None:
        return
    raw = await download_semgrep_json(github, owner, repo, run.id)
    if raw is None:
        logger.warning("Run %d on %s has no Semgrep results artifact", run.id, label)
        return
    findings = parse_semgrep_findings(raw)
    changed = await list_changed_files(github, owner, repo, number)
    # GitHub lists the PR's files as they are now, so the head is checked again after listing.
    pull = await _current_pull(github, event, number, label)
    if pull is None:
        return
    await _save(event, pull, number, _on_added_lines(findings, changed), settings, label)


async def _current_pull(
    github: GitHub[AppInstallationAuthStrategy], event: WorkflowRunEvent, number: int, label: str
) -> PullRequest | None:
    """The PR, or None (and a log line) once the run's commit is no longer its head."""
    owner, repo = event.repository.owner.login, event.repository.name
    pull = (
        await github.rest.pulls.async_get(owner=owner, repo=repo, pull_number=number)
    ).parsed_data
    if is_current_head(pull, event.workflow_run):
        return pull
    logger.info("Skipped run %d on %s: the PR has newer commits", event.workflow_run.id, label)
    return None


def _on_added_lines(findings: list[StaticFinding], changed: list[DiffEntry]) -> list[StaticFinding]:
    """The findings that touch a line the PR added, in Semgrep's order.

    A file without a patch (binary, or too large for GitHub to send one) has no line to match,
    unless the PR adds it: every line of a new file is new, so a finding there is kept when its
    lines fit in the file. GitHub still counts a new file's lines (``additions``) without a patch.
    """
    added = {
        file.filename: added_lines(file.patch) for file in changed if isinstance(file.patch, str)
    }
    unpatched_new_file_lengths = {
        file.filename: file.additions
        for file in changed
        if file.status == "added" and not isinstance(file.patch, str)
    }
    return [
        finding
        for finding in findings
        if _fits(finding, unpatched_new_file_lengths.get(finding.file_path, 0))
        or _touches(finding, added.get(finding.file_path, ()))
    ]


def _fits(finding: StaticFinding, length: int) -> bool:
    """Whether the finding's lines are within the first ``length`` lines of its file."""
    return finding.start_line <= finding.end_line <= length


def _touches(finding: StaticFinding, added: Sequence[int]) -> bool:
    """Whether one of the ascending ``added`` lines is within the finding's lines."""
    first_at_or_after_start = bisect.bisect_left(added, finding.start_line)
    return (
        first_at_or_after_start < len(added) and added[first_at_or_after_start] <= finding.end_line
    )


async def _save(
    event: WorkflowRunEvent,
    pull: PullRequest,
    number: int,
    findings: list[StaticFinding],
    settings: Settings,
    label: str,
) -> None:
    """Save the findings for the code review. Logs and returns on a database failure."""
    run = NewStaticAnalysisRun(
        github_repository_id=event.repository.id,
        pr_number=number,
        head_sha=event.workflow_run.head_sha,
        base_sha=pull.base.sha,
        pr_state=pull.state,
        pr_merged=bool(pull.merged),
        workflow_run_id=event.workflow_run.id,
        findings=findings,
    )
    run_id = run.workflow_run_id
    url = database_url(settings)
    if not url:  # The webhook schedules this job only once the database is configured.
        logger.error(
            "Did not store the static findings of %s (run %d): no database configured",
            label,
            run_id,
        )
        return
    try:
        saved = await in_database(url, lambda db: save_static_analysis_run(db, run))
    except DatabaseStepError as error:
        logger.error("Could not store the static findings of %s (run %d): %s", label, run_id, error)
        return
    if not saved:
        logger.info(
            "Did not store the static findings of %s (run %d): NikolAI doesn't store this PR",
            label,
            run_id,
        )
        return
    logger.info("Stored %d static findings of %s (run %d)", len(findings), label, run_id)
