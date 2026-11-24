"""The team's planning page (US-19.1, spec #234): ``GET /api/teams/{team_id}/planning``.

A request needs a session (401) and ``can_read_team`` (403) before any team data is read
(ADR 0001, decision 9). The answer says whether the viewer may plan, ``can_plan_for_team``,
the rule every planning write checks.
"""

# AI contribution: 50%

from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status

from app.api.dependencies import current_user, database
from app.api.repository_access import may_read_team
from app.core.config import Settings, get_settings
from app.db.models.planning import PlanningPage
from app.db.models.team_overview import TeamName
from app.db.models.users import User
from app.db.repositories.team_overview import find_team
from app.services.access import can_plan_for_team

router = APIRouter(prefix="/api/teams/{team_id}", tags=["planning"])

SignedIn = Annotated[User, Depends(current_user)]
AppSettings = Annotated[Settings, Depends(get_settings)]


@router.get("/planning")
async def get_planning(team_id: UUID, user: SignedIn, settings: AppSettings) -> PlanningPage:
    """The team's planning page, read-only unless the viewer is one of its students."""
    async with database(settings) as connection:
        if not await may_read_team(connection, user, team_id, "the planning page"):
            raise HTTPException(status.HTTP_403_FORBIDDEN, "You don't have access to this team")
        team = await find_team(connection, team_id, user.id)
        can_plan = await can_plan_for_team(connection, user.id, team_id)
    if team is None:
        # can_read_team is false for an unknown team, so only a team deleted between the two
        # reads lands here, as in add_team_member.
        raise RuntimeError("A team vanished after its access check")
    return PlanningPage(
        team=TeamName(id=team_id, name=team.name, course=team.course_name),
        can_plan=can_plan,
    )
