"""Attach and detach issues to the repository's brainstorm epic (US-20.1, #244).

When an issue is labelled ``brainstorm`` in a registered repository, NikolAI creates
the repository's brainstorm epic the first time and attaches the issue as its sub-issue.
Removing the label detaches it. An issue already parented to another issue is left
where it is and the failure is logged with context. GitHub failures are logged and
never break other webhook handling.
"""

# AI contribution: 50%

import logging
from collections.abc import AsyncGenerator
from contextlib import AbstractAsyncContextManager, asynccontextmanager
from typing import Final, NamedTuple, cast

from githubkit import GitHub
from githubkit.auth import AppInstallationAuthStrategy
from githubkit.exception import GitHubException, RequestFailed
from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.core.config import Settings
from app.core.logging_config import log_safe_exception
from app.db import connect
from app.db.repositories.registrations import active_repository_id
from app.db.steps import DatabaseStepError, database_url, in_database
from app.github.client import installation_client
from app.github.comments import IssueRef
from app.github.payloads import Issue, IssueLabel, IssuesEvent

logger = logging.getLogger(__name__)

_BRAINSTORM_LABEL: Final = "brainstorm"
_GENAI_DISCLOSURE: Final = (
    "AI contribution: 50%\n\nAI-assisted files: see the pull request that created this issue."
)


class BrainstormEpic(NamedTuple):
    """The remembered brainstorm epic: its issue number."""

    number: int


def is_brainstorm_label(name: str) -> bool:
    """Whether ``name`` is the brainstorm label, matched without case."""
    return name.casefold() == _BRAINSTORM_LABEL


def brainstorm_label(event: IssuesEvent) -> IssueLabel | None:
    """The label this delivery added or removed, when it carries one."""
    return event.label


def is_brainstorm_event(event: IssuesEvent) -> bool:
    """Whether this delivery adds or removes the brainstorm label."""
    if event.action not in ("labeled", "unlabeled"):
        return False
    label = brainstorm_label(event)
    return label is not None and is_brainstorm_label(label.name)


async def get_brainstorm_epic(
    settings: Settings, github_repository_id: int
) -> BrainstormEpic | None:
    """The remembered epic, or None."""
    url = database_url(settings)
    if not url:
        return None
    try:
        result = await in_database(
            url, lambda connection: _read_brainstorm_epic(connection, github_repository_id)
        )
    except DatabaseStepError as error:
        log_safe_exception(
            logger,
            "Could not read the brainstorm epic of repository %s",
            github_repository_id,
            error=RuntimeError(str(error)),
        )
        return None
    if result is None:
        return None
    return BrainstormEpic(number=result)


async def _read_brainstorm_epic(
    connection: AsyncConnection[DictRow], github_repository_id: int
) -> int | None:
    from app.db.repositories import brainstorm_epics

    result = await brainstorm_epics.get(connection, github_repository_id)
    if result is None:
        return None
    return int(result)


async def save_brainstorm_epic(
    settings: Settings, github_repository_id: int, issue_number: int
) -> None:
    """Remember the epic for ``github_repository_id``."""
    url = database_url(settings)
    if not url:
        logger.error(
            "Could not remember the brainstorm epic of repository %s: no database configured",
            github_repository_id,
        )
        return
    try:
        await in_database(
            url,
            lambda connection: _save_brainstorm_epic_row(
                connection, github_repository_id, issue_number
            ),
        )
    except DatabaseStepError as error:
        log_safe_exception(
            logger,
            "Could not save the brainstorm epic of repository %s",
            github_repository_id,
            error=RuntimeError(str(error)),
        )


async def _save_brainstorm_epic_row(
    connection: AsyncConnection[DictRow],
    github_repository_id: int,
    issue_number: int,
) -> None:
    from app.db.repositories import brainstorm_epics

    return await brainstorm_epics.save(connection, github_repository_id, issue_number)


async def _valid_epic(
    github: GitHub[AppInstallationAuthStrategy],
    ref: IssueRef,
    stored: BrainstormEpic | None,
) -> BrainstormEpic | None:
    """The stored epic if it still exists and is open; None otherwise."""
    if stored is None:
        return None
    try:
        response = await github.rest.issues.async_get(
            owner=ref.owner, repo=ref.repo, issue_number=stored.number
        )
    except RequestFailed as error:
        if error.response.status_code not in (404, 410):
            raise
        logger.info(
            "Brainstorm epic %s#%d is gone; a new one will be created",
            ref.full_name,
            stored.number,
        )
        return None
    state: str | None = None
    try:
        from app.github.payloads import Issue as IssueModel

        parsed = IssueModel.model_validate(cast(object, response.json()))
        state = parsed.state
    except Exception:
        fallback = getattr(response, "parsed_data", None)
        state = getattr(fallback, "state", None)
    if state != "open":
        logger.info(
            "Brainstorm epic %s#%d is closed; a new one will be created",
            ref.full_name,
            stored.number,
        )
        return None
    return BrainstormEpic(number=stored.number)


async def _create_epic(
    github: GitHub[AppInstallationAuthStrategy], ref: IssueRef
) -> BrainstormEpic | None:
    """Create a new brainstorm epic issue; returns the epic or None on failure."""
    try:
        response = await github.rest.issues.async_create(
            owner=ref.owner,
            repo=ref.repo,
            data={
                "title": "Brainstorm epic",
                "body": (
                    "This issue collects every brainstorm issue in this repository "
                    "as a sub-issue.\n\n" + _GENAI_DISCLOSURE
                ),
            },
        )
    except GitHubException as error:
        logger.error(
            "Could not create the brainstorm epic on %s: %s",
            ref.label,
            type(error).__name__,
        )
        return None
    try:
        from app.github.payloads import Issue as IssueModel

        issue = IssueModel.model_validate(cast(object, response.json()))
        return BrainstormEpic(number=int(issue.number))
    except Exception:
        fallback = getattr(response, "parsed_data", None)
        if fallback is None:
            raise
        return BrainstormEpic(
            number=int(getattr(fallback, "number")),  # noqa: B009
        )


async def _already_parented(
    github: GitHub[AppInstallationAuthStrategy], ref: IssueRef, issue: Issue
) -> int | None:
    """The parent issue number if ``issue`` is already a sub-issue, else None."""
    try:
        response = await github.rest.issues.async_get_parent(
            owner=ref.owner, repo=ref.repo, issue_number=issue.number
        )
    except RequestFailed as error:
        if error.response.status_code == 404:
            return None
        log_safe_exception(
            logger,
            "Could not check the parent of %s",
            ref.label,
            error=error,
        )
        return -1  # unknown -> treat as parented to be safe
    try:
        from app.github.payloads import Issue as IssueModel

        parent = IssueModel.model_validate(cast(object, response.json()))
        return int(parent.number)
    except Exception:
        parsed = getattr(response, "parsed_data", None)
        number = getattr(parsed, "number", None)
        return int(number) if number is not None else None


async def _brainstorm_epic_lock(
    settings: Settings, github_repository_id: int
) -> AbstractAsyncContextManager[None]:
    url = database_url(settings)
    if not url:

        @asynccontextmanager
        async def _noop() -> AsyncGenerator[None]:
            yield

        return _noop()

    @asynccontextmanager
    async def _locked() -> AsyncGenerator[None]:
        async with await connect(url) as connection:
            await connection.execute(
                "SELECT pg_advisory_lock(hashtextextended(%s, 0))",
                (f"brainstorm-epic:{github_repository_id}",),
            )
            try:
                yield
            finally:
                await connection.execute(
                    "SELECT pg_advisory_unlock(hashtextextended(%s, 0))",
                    (f"brainstorm-epic:{github_repository_id}",),
                )

    return _locked()


async def _repository_is_registered(settings: Settings, github_repository_id: int) -> bool:
    """Whether NikolAI serves the repository, checked again for redeliveries."""
    url = database_url(settings)
    if not url:
        return True
    try:
        found = await in_database(
            url,
            lambda connection: active_repository_id(connection, github_repository_id),
        )
    except DatabaseStepError as error:
        log_safe_exception(
            logger,
            "Could not check the registration of repository %s",
            github_repository_id,
            error=RuntimeError(str(error)),
        )
        return False
    return found is not None


async def _read_issue_now(
    github: GitHub[AppInstallationAuthStrategy], ref: IssueRef
) -> Issue | None:
    """The issue's current state from GitHub, or None when GitHub says it is gone."""
    try:
        response = await github.rest.issues.async_get(
            owner=ref.owner, repo=ref.repo, issue_number=ref.number
        )
    except RequestFailed as error:
        if error.response.status_code not in (404, 410):
            raise
        return None
    try:
        from app.github.payloads import Issue as IssueModel

        return IssueModel.model_validate(cast(object, response.json()))
    except Exception:
        fallback = getattr(response, "parsed_data", None)
        return fallback if isinstance(fallback, Issue) else None


async def handle_brainstorm_event(event: IssuesEvent, settings: Settings) -> None:
    """Handle one issues delivery for brainstorm labelling. Never raises."""
    owner = event.repository.owner.login
    repo = event.repository.name
    issue = event.issue
    ref = IssueRef(owner=owner, repo=repo, number=issue.number)
    github_repository_id = event.repository.id
    try:
        github = installation_client(settings, event.installation.id)
    except Exception as error:
        logger.error("Cannot act as the GitHub App on %s: %s", ref.label, type(error).__name__)
        return
    try:
        if not await _repository_is_registered(settings, github_repository_id):
            logger.info("No brainstorm handling for %s: repository is not registered", ref.label)
            return
        current = await _read_issue_now(github, ref)
        if current is None:
            logger.info(
                "Brainstorm handling skipped for %s: GitHub no longer has the issue", ref.label
            )
            return
        # The delivery's action can be stale: an old ``labeled`` may arrive after the label
        # was removed. The issue's current labels decide, not the delivery action.
        if any(is_brainstorm_label(label.name) for label in current.labels):
            await _attach(settings, github, ref, current, github_repository_id)
        else:
            await _detach(settings, github, ref, current, github_repository_id)
    except Exception as error:
        log_safe_exception(
            logger,
            "Brainstorm handling failed for %s",
            ref.label,
            error=error,
        )


async def _attach(
    settings: Settings,
    github: GitHub[AppInstallationAuthStrategy],
    ref: IssueRef,
    issue: Issue,
    github_repository_id: int,
) -> None:
    parent = await _already_parented(github, ref, issue)
    if parent == -1:
        logger.warning(
            "Issue %s: could not verify parent; not moving it to the brainstorm epic",
            ref.label,
        )
        return
    if parent is not None:
        logger.warning(
            "Issue %s is already a sub-issue of another issue (#%s); not moving it to the "
            "brainstorm epic",
            ref.label,
            parent,
        )
        return

    lock = await _brainstorm_epic_lock(settings, github_repository_id)
    async with lock:
        stored = await get_brainstorm_epic(settings, github_repository_id)
        epic = await _valid_epic(github, ref, stored)
        if epic is None:
            created = await _create_epic(github, ref)
            if created is None:
                return
            epic = created
            await save_brainstorm_epic(settings, github_repository_id, epic.number)
        try:
            await github.rest.issues.async_add_sub_issue(
                owner=ref.owner,
                repo=ref.repo,
                issue_number=epic.number,
                data={"sub_issue_id": issue.id},
            )
            logger.info("Attached %s to brainstorm epic #%d", ref.label, epic.number)
        except GitHubException as error:
            logger.error(
                "Could not attach %s to brainstorm epic #%d: %s",
                ref.label,
                epic.number,
                type(error).__name__,
            )


async def _detach(
    settings: Settings,
    github: GitHub[AppInstallationAuthStrategy],
    ref: IssueRef,
    issue: Issue,
    github_repository_id: int,
) -> None:
    stored = await get_brainstorm_epic(settings, github_repository_id)
    epic = await _valid_epic(github, ref, stored)
    if epic is None:
        logger.info("No brainstorm epic to detach %s from", ref.label)
        return
    try:
        await github.rest.issues.async_remove_sub_issue(
            owner=ref.owner,
            repo=ref.repo,
            issue_number=epic.number,
            data={"sub_issue_id": issue.id},
        )
        logger.info("Detached %s from brainstorm epic #%d", ref.label, epic.number)
    except GitHubException as error:
        logger.error(
            "Could not detach %s from brainstorm epic #%d: %s",
            ref.label,
            epic.number,
            type(error).__name__,
        )
