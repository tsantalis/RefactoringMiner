"""Read API for stored PRs, their feedback (US-5.6, ADR 0001 PR 3) and refactorings (US-8).

The response shapes follow ``frontend/apps/extension/src/shared/api/contract.ts``. Every request
needs a session (401) and passes ``can_read_team`` for the repository's team (403) before any
team data is read. Unknown repositories and PRs are 404 with a ``code`` the extension shows.
"""

# AI contribution: 50%

from typing import Annotated, Final

from fastapi import APIRouter, Depends, HTTPException, Path, Query, status
from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.api.dependencies import current_user, database
from app.core.config import Settings, get_settings
from app.db.models.feedback import PullRequestFeedback, PullRequestSummary
from app.db.models.refactorings import PullRequestRefactorings
from app.db.models.users import User
from app.db.repositories.feedback import RegisteredRepository, find_repository
from app.services.access import can_read_team
from app.services.pull_requests import pull_request_feedback, repository_pulls
from app.services.refactorings import pull_request_refactorings

router = APIRouter(prefix="/api/repos/{owner}/{repo}/pulls", tags=["pull requests"])

# GitHub owner and repository names: letters, digits, '.', '-' and '_'.
_NAME: Final = r"^[A-Za-z0-9._-]{1,100}$"

Owner = Annotated[str, Path(pattern=_NAME)]
Repo = Annotated[str, Path(pattern=_NAME)]
SignedIn = Annotated[User, Depends(current_user)]
AppSettings = Annotated[Settings, Depends(get_settings)]


def _not_found(code: str) -> HTTPException:
    return HTTPException(status.HTTP_404_NOT_FOUND, detail={"code": code})


async def _readable_repository(
    connection: AsyncConnection[DictRow], owner: str, repo: str, user: User
) -> RegisteredRepository:
    repository = await find_repository(connection, f"{owner}/{repo}")
    if repository is None:
        raise _not_found("repository_not_registered")
    if not await can_read_team(connection, user.id, repository.team_id):
        raise HTTPException(status.HTTP_403_FORBIDDEN, "You don't have access to this team")
    return repository


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
        repository = await _readable_repository(connection, owner, repo, user)
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
        repository = await _readable_repository(connection, owner, repo, user)
        feedback = await pull_request_feedback(connection, repository, number)
    if feedback is None:
        raise _not_found("pull_request_not_found")
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
        repository = await _readable_repository(connection, owner, repo, user)
        refactorings = await pull_request_refactorings(connection, repository, number)
    if refactorings is None:
        raise _not_found("pull_request_not_found")
    return refactorings
