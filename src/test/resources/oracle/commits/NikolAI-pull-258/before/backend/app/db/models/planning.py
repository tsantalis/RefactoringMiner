"""The team's planning page as the API reads it (US-19.1, spec #234).

Terms follow ``GLOSSARY.md`` (Planning). Later stories add the brainstorm issues, planning
sessions, availability and burndown to this page.
"""

# AI contribution: 50%

from pydantic import BaseModel

from app.db.models.team_overview import TeamName


class PlanningPage(BaseModel):
    """``GET /api/teams/{team_id}/planning``: the team, and whether the viewer may plan for it.

    ``can_plan`` is ``can_plan_for_team``: true for the team's student members. Staff and granted
    stakeholders read the page with it false, and the dashboard turns every control off.
    """

    team: TeamName
    can_plan: bool
