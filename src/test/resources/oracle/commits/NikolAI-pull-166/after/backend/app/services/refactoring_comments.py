"""Post the RefactoringMiner Action's report on the PR as a NikolAI comment (US-8.2, US-8.4).

Each team's repository runs the RefactoringMiner GitHub Action in a "Refactoring Report"
workflow. When a run completes, GitHub sends the App a ``workflow_run`` event, and the webhook
schedules ``handle_refactoring_run`` in the background, which:

1. signs in as the App's installation on that repository;
2. skips runs for a commit that is no longer the PR's head, so a slow old run can't replace
   the report of a newer push;
3. downloads the run's artifact and reads the PR's ``refactorings.json``;
4. compares them with the PR's previous analysed push, as the extension's Refactorings tab does;
5. posts the report grouped by refactoring type, each refactoring marked new or unchanged and
   the undone ones listed last, or updates NikolAI's earlier report in place;
6. saves the refactorings, so the extension's Refactorings tab can read them.

A failure at any step is logged and the later steps are skipped: the next push brings a new
run. The database steps fail on their own: during an outage the report is posted unmarked.
"""

# AI contribution: 50%

import logging
from typing import Final

from githubkit import GitHub
from githubkit.auth import AppInstallationAuthStrategy
from githubkit.exception import GitHubException
from githubkit_schemas.latest.models import PullRequest

from app.core.config import ConfigurationError, Settings
from app.core.logging_config import log_safe_exception
from app.db.models.refactorings import NewRefactoringRun
from app.db.repositories.refactorings import previous_refactoring_run, save_refactoring_run
from app.db.steps import DatabaseStepError, database_url, in_database
from app.github.action_artifacts import ArtifactError, download_refactorings_json
from app.github.client import installation_client
from app.github.comments import find_app_comment
from app.github.payloads import WorkflowRunEvent
from app.services.refactoring_report import (
    REPORT_MARKER,
    Refactoring,
    RefactoringReportError,
    ReportComparison,
    parse_refactorings,
    render_report,
)
from app.services.refactorings import report_comparison, to_stored
from app.services.workflow_runs import (
    is_current_head,
    pull_request_numbers,
    succeeded_on_pull_request,
)

logger = logging.getLogger(__name__)

# The workflow name in the Action's quick start, which team repositories copy.
REFACTORING_WORKFLOW: Final = "Refactoring Report"


def wants_refactoring_report(event: WorkflowRunEvent) -> bool:
    """Report successful "Refactoring Report" runs that GitHub links to a PR."""
    return succeeded_on_pull_request(event, REFACTORING_WORKFLOW)


async def handle_refactoring_run(event: WorkflowRunEvent, settings: Settings) -> None:
    """Background job for one completed run: report on each of its PRs. Never raises."""
    repository = event.repository.full_name
    try:
        client = installation_client(settings, event.installation.id)
    except ConfigurationError as error:
        logger.error("Cannot act as the GitHub App on %s: %s", repository, error)
        return
    # One open client for the whole job: it keeps the connection open while the artifact streams
    # in, and reuses it for every call instead of opening a new one per request.
    async with client as github:
        for number in pull_request_numbers(event):
            label = f"{repository}#{number}"
            try:
                await _report_on(github, event, number, settings, label)
            except (GitHubException, ArtifactError, RefactoringReportError) as error:
                logger.error(
                    "Could not post the refactorings of %s: %s", label, type(error).__name__
                )
            except Exception as error:
                # Last resort: a bug here must not take the server down.
                log_safe_exception(
                    logger, "Unexpected error while reporting %s", label, error=error
                )


async def _report_on(
    github: GitHub[AppInstallationAuthStrategy],
    event: WorkflowRunEvent,
    number: int,
    settings: Settings,
    label: str,
) -> None:
    owner, repo = event.repository.owner.login, event.repository.name
    run = event.workflow_run
    pull = (
        await github.rest.pulls.async_get(owner=owner, repo=repo, pull_number=number)
    ).parsed_data
    if not is_current_head(pull, run):
        logger.info("Skipped run %d on %s: the PR has newer commits", run.id, label)
        return
    raw = await download_refactorings_json(github, owner, repo, run.id, number)
    if raw is None:
        logger.warning("Run %d on %s has no RefactoringMiner report artifact", run.id, label)
        return
    refactorings = parse_refactorings(raw)
    comparison = await _comparison(event, number, refactorings, settings, label)
    body = render_report(refactorings, pull.html_url, run.head_sha, comparison)
    existing = await find_app_comment(
        github, owner, repo, number, app_id=settings.github_app_id, marker=REPORT_MARKER
    )
    if existing is None:
        await github.rest.issues.async_create_comment(
            owner=owner, repo=repo, issue_number=number, body=body
        )
    else:
        await github.rest.issues.async_update_comment(
            owner=owner, repo=repo, comment_id=existing.id, body=body
        )
    logger.info("Posted the refactorings of %s (run %d)", label, run.id)
    await _save(event, pull, number, refactorings, settings, label)


async def _comparison(
    event: WorkflowRunEvent,
    number: int,
    refactorings: list[Refactoring],
    settings: Settings,
    label: str,
) -> ReportComparison | None:
    """How this push compares with the PR's previous analysed one, or None to post it unmarked.

    None on the PR's first analysed push, and when the database can't tell: a missing
    comparison must not stop the report from being posted.
    """
    url = database_url(settings)
    if not url:
        return None
    repository_id, head_sha = event.repository.id, event.workflow_run.head_sha
    try:
        previous = await in_database(
            url, lambda db: previous_refactoring_run(db, repository_id, number, head_sha)
        )
    except DatabaseStepError as error:
        logger.warning(
            "Posting the refactorings of %s unmarked: database unavailable (%s)", label, error
        )
        return None
    return report_comparison(refactorings, previous)


async def _save(
    event: WorkflowRunEvent,
    pull: PullRequest,
    number: int,
    refactorings: list[Refactoring],
    settings: Settings,
    label: str,
) -> None:
    """Save the run's refactorings for the read API. Logs and returns on a database failure."""
    url = database_url(settings)
    if not url:
        logger.warning("Did not save the refactorings of %s: no database configured", label)
        return
    run = NewRefactoringRun(
        github_repository_id=event.repository.id,
        pr_number=number,
        head_sha=event.workflow_run.head_sha,
        base_sha=pull.base.sha,
        pr_state=pull.state,
        pr_merged=bool(pull.merged),
        workflow_run_id=event.workflow_run.id,
        refactorings=to_stored(refactorings),
    )
    try:
        saved = await in_database(url, lambda db: save_refactoring_run(db, run))
    except DatabaseStepError as error:
        logger.error("Could not save the refactorings of %s: %s", label, error)
        return
    if not saved:
        logger.info("Did not save the refactorings of %s: NikolAI doesn't store this PR", label)
