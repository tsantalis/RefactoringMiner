"""The backlog coverage check as it is stored and served (US-13.2)."""

# AI contribution: 50%

from datetime import datetime
from typing import Literal

from pydantic import BaseModel

from app.db.models.team_overview import Viewer

CheckStatus = Literal["no_requirements", "completed", "failed"]


class CoveringStory(BaseModel):
    """A ``user story`` issue that covers a requirement."""

    number: int
    title: str
    url: str


class RequirementCoverage(BaseModel):
    """One project requirement and the stories covering it. No stories means uncovered."""

    requirement: str
    stories: list[CoveringStory]


class CoverageOutcome(BaseModel):
    """What one run of the check found. ``reason`` says why a ``failed`` check couldn't run."""

    status: CheckStatus
    reason: str | None
    requirements: list[RequirementCoverage]


class StoredCoverage(CoverageOutcome):
    """A repository's latest check."""

    checked_at: datetime


class BacklogCoverage(BaseModel):
    """``.../backlog-coverage``: the latest check, or ``not_run`` before the first."""

    viewer: Viewer
    status: CheckStatus | Literal["not_run"]
    reason: str | None
    checked_at: datetime | None
    requirements: list[RequirementCoverage]
