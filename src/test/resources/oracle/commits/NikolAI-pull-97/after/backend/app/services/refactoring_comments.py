"""Post the RefactoringMiner Action's report on the PR as a NikolAI comment (US-8.2, US-8.4).

Each team's repository runs the RefactoringMiner GitHub Action in a "Refactoring Report"
workflow. When a run completes, GitHub sends the App a ``workflow_run`` event, and the webhook
schedules ``handle_refactoring_run`` in the background, which:

1. signs in as the App's installation on that repository;
2. skips runs for a commit that is no longer the PR's head, so a slow old run can't replace
   the report of a newer push;
3. downloads the run's artifact and reads the PR's ``refactorings.json``;
4. posts the report grouped by refactoring type, or updates NikolAI's earlier report in place.

A failure at any step is logged and nothing is posted: the next push brings a new run.
"""

# AI contribution: 50% or more AI-generated

import logging
from typing import Final

from githubkit import GitHub
from githubkit.auth import AppInstallationAuthStrategy
from githubkit.exception import GitHubException

from app.core.config import ConfigurationError, Settings
from app.core.logging_config import log_safe_exception
from app.github.action_artifacts import ArtifactError, download_refactorings_json
from app.github.client import installation_client
from app.github.comments import find_app_comment
from app.github.payloads import WorkflowRunEvent
from app.services.refactoring_report import (
    REPORT_MARKER,
    RefactoringReportError,
    parse_refactorings,
    render_report,
)

logger = logging.getLogger(__name__)

# The workflow name in the Action's quick start, which team repositories copy.
REFACTORING_WORKFLOW: Final = "Refactoring Report"


def wants_refactoring_report(event: WorkflowRunEvent) -> bool:
    """Report successful "Refactoring Report" runs that GitHub links to a PR."""
    run = event.workflow_run
    return (
        event.action == "completed"
        and run.event == "pull_request"
        and run.name == REFACTORING_WORKFLOW
        and run.conclusion == "success"
        and bool(_reported_pull_requests(event))
    )


def _reported_pull_requests(event: WorkflowRunEvent) -> list[int]:
    """The numbers of the run's PRs into the repository that sent the event.

    GitHub leaves the list empty for PRs from forks, so those runs are not reported.
    """
    return [
        pull_request.number
        for pull_request in event.workflow_run.pull_requests
        if pull_request.base.repo.id == event.repository.id
    ]


async def handle_refactoring_run(event: WorkflowRunEvent, settings: Settings) -> None:
    """Background job for one completed run: report on each of its PRs. Never raises."""
    repository = event.repository.full_name
    try:
        github = installation_client(settings, event.installation.id)
    except ConfigurationError as error:
        logger.error("Cannot act as the GitHub App on %s: %s", repository, error)
        return
    for number in _reported_pull_requests(event):
        label = f"{repository}#{number}"
        try:
            await _report_on(github, event, number, settings, label)
        except (GitHubException, ArtifactError, RefactoringReportError) as error:
            logger.error("Could not post the refactorings of %s: %s", label, type(error).__name__)
        except Exception as error:
            # Last resort: a bug here must not take the server down.
            log_safe_exception(logger, "Unexpected error while reporting %s", label, error=error)


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
    if pull.head.sha != run.head_sha:
        logger.info("Skipped run %d on %s: the PR has newer commits", run.id, label)
        return
    raw = await download_refactorings_json(github, owner, repo, run.id, number)
    if raw is None:
        logger.warning("Run %d on %s has no RefactoringMiner report artifact", run.id, label)
        return
    body = render_report(parse_refactorings(raw), pull.html_url, run.head_sha)
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
