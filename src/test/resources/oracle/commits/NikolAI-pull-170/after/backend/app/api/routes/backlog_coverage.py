"""The backlog coverage check (US-13.2): run it on demand, and read the latest result.

The response shape is ``backlogCoverage`` in
``frontend/apps/extension/src/shared/api/views-contract.ts``. Like the other repository routes,
a request needs a session (401) and passes ``readable_repository`` before any team data is read:
404 ``repository_not_registered`` for an unknown repository, 403 without ``can_read_team``.
"""

# AI contribution: 50%

from typing import Annotated

from fastapi import APIRouter, Depends

from app.api.dependencies import current_user, database, require_allowed_origin
from app.api.repository_access import Owner, Repo, readable_repository
from app.api.routes.webhooks import get_llm_provider
from app.core.config import Settings, get_settings
from app.db.models.backlog_coverage import BacklogCoverage
from app.db.models.users import User
from app.db.repositories.backlog_coverage import save_coverage, team_project_requirements
from app.providers.base import LLMProvider
from app.services.backlog_coverage import check_backlog, latest_coverage

router = APIRouter(prefix="/api/repos/{owner}/{repo}/backlog-coverage", tags=["backlog coverage"])

SignedIn = Annotated[User, Depends(current_user)]
AppSettings = Annotated[Settings, Depends(get_settings)]


@router.get("")
async def get_backlog_coverage(
    owner: Owner, repo: Repo, user: SignedIn, settings: AppSettings
) -> BacklogCoverage:
    """The repository's latest coverage check, or ``not_run`` before the first."""
    async with database(settings) as connection:
        repository = await readable_repository(connection, owner, repo, user)
        return await latest_coverage(connection, repository, user)


@router.post("", dependencies=[Depends(require_allowed_origin)])
async def run_backlog_coverage(
    owner: Owner,
    repo: Repo,
    user: SignedIn,
    settings: AppSettings,
    provider: Annotated[LLMProvider | None, Depends(get_llm_provider)],
) -> BacklogCoverage:
    """Check the team's user stories against the project requirements, and return the result.

    The check runs between two database blocks because ``database`` limits each block to a few
    seconds, and a model call takes far longer.
    """
    async with database(settings) as connection:
        repository = await readable_repository(connection, owner, repo, user)
        requirements = await team_project_requirements(connection, repository.team_id)
    outcome = await check_backlog(requirements, repository.full_name, settings, provider)
    async with database(settings) as connection:
        await save_coverage(connection, repository.id, outcome)
        return await latest_coverage(connection, repository, user)
