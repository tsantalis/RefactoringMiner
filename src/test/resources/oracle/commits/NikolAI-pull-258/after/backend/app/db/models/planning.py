"""The team's planning page as the API reads it (US-19.1, spec #234).

Terms follow ``GLOSSARY.md`` (Planning). The Brainstorm list (US-20.2) has its own endpoint, so a
GitHub failure never hides the page. Later stories add planning sessions, availability and
burndown to this page.
"""

# AI contribution: 50%

from typing import Literal

from pydantic import BaseModel

from app.db.models.team_overview import TeamName

BrainstormStatus = Literal["listed", "failed"]


class PlanningPage(BaseModel):
    """``GET /api/teams/{team_id}/planning``: the team, and whether the viewer may plan for it.

    ``can_plan`` is ``can_plan_for_team``: true for the team's student members. Staff and granted
    stakeholders read the page with it false, and the dashboard turns every control off.
    """

    team: TeamName
    can_plan: bool


class BrainstormIssueLink(BaseModel):
    """An open brainstorm issue as the dashboard lists it, linking to GitHub."""

    number: int
    title: str
    url: str


class RepositoryBrainstorm(BaseModel):
    """One repository's open brainstorm issues.

    ``failed`` when GitHub couldn't be read: ``issues`` is then empty, and the other repositories
    are still listed. ``more`` is true when GitHub has more than were listed; ``all_url`` is
    GitHub's own list of them.
    """

    full_name: str
    status: BrainstormStatus
    issues: list[BrainstormIssueLink]
    more: bool
    all_url: str


class BrainstormList(BaseModel):
    """``GET /api/teams/{team_id}/planning/brainstorm``: each of the team's repositories.

    ``label`` is the brainstorm label, sent so the dashboard never spells it out itself.
    """

    label: str
    repositories: list[RepositoryBrainstorm]
