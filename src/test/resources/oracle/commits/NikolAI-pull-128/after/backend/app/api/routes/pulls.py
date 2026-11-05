"""Read API for stored PRs, their feedback (US-5.6, ADR 0001 PR 3), refactorings (US-8) and
insights (US-9).

The frontend generates its types from these response models (``@nikolai/api-client``,
``pnpm generate:api``) and checks them at runtime in
``frontend/apps/extension/src/shared/api/views-contract.ts``. Every request needs a session
(401) and passes ``readable_repository`` (``app.api.repository_access``) before any team data
is read: 403 without ``can_read_team``, 404 for an unknown repository. Unknown PRs are also
404, each with a ``code`` the extension shows.
"""

# AI contribution: 50%

from typing import Annotated

from fastapi import APIRouter, Depends, Path, Query

from app.api.dependencies import current_user, database
from app.api.repository_access import Owner, Repo, not_found, readable_repository
from app.core.config import Settings, get_settings
from app.db.models.feedback import PullRequestFeedback, PullRequestSummary
from app.db.models.pull_insights import PullInsights
from app.db.models.refactorings import PullRequestRefactorings
from app.db.models.users import User
from app.services.pull_insights import pull_insights
from app.services.pull_requests import pull_request_feedback, repository_pulls
from app.services.refactorings import pull_request_refactorings

router = APIRouter(prefix="/api/repos/{owner}/{repo}/pulls", tags=["pull requests"])

SignedIn = Annotated[User, Depends(current_user)]
AppSettings = Annotated[Settings, Depends(get_settings)]


@router.get("")
async def list_pulls(
    owner: Owner,
    repo: Repo,
    user: SignedIn,
    settings: AppSettings,
    limit: Annotated[int, Query(ge=1, le=100)] = 50,
    offset: Annotated[int, Query(ge=0)] = 0,
) -> list[PullRequestSummary]:
    """The repository's stored PRs, newest updated first."""
    async with database(settings) as connection:
        repository = await readable_repository(connection, owner, repo, user)
        return await repository_pulls(connection, repository, limit, offset)


@router.get("/{number}")
async def get_pull(
    owner: Owner,
    repo: Repo,
    number: Annotated[int, Path(ge=1)],
    user: SignedIn,
    settings: AppSettings,
) -> PullRequestFeedback:
    """One PR with every review run, newest first, and the latest review's findings."""
    async with database(settings) as connection:
        repository = await readable_repository(connection, owner, repo, user)
        feedback = await pull_request_feedback(connection, repository, number)
    if feedback is None:
        raise not_found("pull_request_not_found")
    return feedback


@router.get("/{number}/refactorings")
async def get_pull_refactorings(
    owner: Owner,
    repo: Repo,
    number: Annotated[int, Path(ge=1)],
    user: SignedIn,
    settings: AppSettings,
) -> PullRequestRefactorings:
    """The refactorings RefactoringMiner found in the PR's latest push, compared with the last."""
    async with database(settings) as connection:
        repository = await readable_repository(connection, owner, repo, user)
        refactorings = await pull_request_refactorings(connection, repository, number)
    if refactorings is None:
        raise not_found("pull_request_not_found")
    return refactorings


@router.get("/{number}/insights")
async def get_pull_insights(
    owner: Owner,
    repo: Repo,
    number: Annotated[int, Path(ge=1)],
    user: SignedIn,
    settings: AppSettings,
) -> PullInsights:
    """The PR tab's extras: the viewer's role, checklist status, withheld count and pushes."""
    async with database(settings) as connection:
        repository = await readable_repository(connection, owner, repo, user)
        insights = await pull_insights(connection, repository, number, user)
    if insights is None:
        raise not_found("pull_request_not_found")
    return insights
