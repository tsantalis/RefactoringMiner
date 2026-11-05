"""The PR tab's insights, ``GET /api/repos/{owner}/{repo}/pulls/{n}/insights`` (US-9).

The shape is ``PullInsights`` in ``frontend/apps/extension/src/shared/api/views-contract.ts``.
Feedback on the viewer's own review and staff marking come in later iterations, so
``your_review`` and ``marking`` are always null; the contract rejects a student response with
``marking``.
"""

# AI contribution: 50%

from typing import Literal
from uuid import UUID

from pydantic import BaseModel

from app.db.models.team_overview import Viewer

ChecklistStatus = Literal["open", "fixed", "acknowledged", "disputed"]


class FixLocation(BaseModel):
    file_path: str
    start_line: int | None
    end_line: int | None


class FixedIn(BaseModel):
    """The push that fixed a checklist item, and where."""

    commit: str
    location: FixLocation


class ChecklistEntry(BaseModel):
    finding_id: UUID
    status: ChecklistStatus
    # None until NikolAI checks new pushes for fixes.
    fixed_in: FixedIn | None


class Push(BaseModel):
    """One reviewed push: its short head SHA and how its findings changed the checklist."""

    commit: str
    message: str
    fixed: int
    added: int


class PullInsights(BaseModel):
    viewer: Viewer
    checklist: list[ChecklistEntry]
    withheld_count: int
    your_review: None
    pushes: list[Push]
    marking: None


class StoredChecklist(BaseModel):
    """The checklist's review output: its findings in checklist order, each one's answered status
    (same order), and the withheld count it stored (None when none was stored or it isn't a
    sensible count)."""

    finding_ids: list[UUID]
    statuses: list[ChecklistStatus]
    withheld_count: int | None


class StoredCodeReview(BaseModel):
    """A completed review of the PR whose code review ran: the reviewed head commit, if known,
    and the keys of the findings it saved (a key names the same issue across pushes)."""

    head_sha: str | None
    finding_keys: list[str]
