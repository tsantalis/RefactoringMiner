"""The PR tab's insights, ``GET /api/repos/{owner}/{repo}/pulls/{n}/insights`` (US-9).

The shape is ``PullInsights`` in ``frontend/apps/extension/src/shared/api/views-contract.ts``.
Feedback on the viewer's own review and staff marking come in later iterations, so
``your_review`` and ``marking`` are always null; the contract rejects a student response with
``marking``.
"""

# AI contribution: 50%

import logging
from typing import Annotated, Final, Literal, Self
from uuid import UUID

from pydantic import BaseModel, BeforeValidator, ConfigDict, Field, ValidationError, model_validator

from app.db.models.team_overview import Viewer

logger = logging.getLogger(__name__)

ChecklistStatus = Literal["open", "fixed", "acknowledged", "disputed"]
# How many characters of a commit's SHA the PR tab and the review comment show.
SHORT_SHA: Final = 7


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
    # The push that fixed the item; None while it is open, acknowledged or disputed.
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


class StoredFix(BaseModel):
    """A fix as ``findings.details`` stores it: the full SHA and the lines of its diff."""

    model_config = ConfigDict(frozen=True)

    commit: str
    file_path: str
    # The same bounds as the agent's ``ProposedFix``, plus a range that runs forward.
    start_line: int = Field(ge=1)
    end_line: int = Field(ge=1)

    @model_validator(mode="after")
    def _forward_range(self) -> Self:
        if self.end_line < self.start_line:
            raise ValueError("end_line comes before start_line")
        return self


def read_fix(value: object) -> StoredFix | None:
    """The stored fix, or None when there is none or it isn't a fix's shape. The one reader of
    ``findings.details`` fixes, so every caller treats a damaged one the same way: not fixed."""
    if isinstance(value, StoredFix):
        return value
    try:
        return StoredFix.model_validate(value) if isinstance(value, dict) else None
    except ValidationError as error:
        logger.debug("Treating a damaged stored fix as not fixed: %d errors", error.error_count())
        return None


class StoredChecklist(BaseModel):
    """The checklist's review output: its findings in checklist order, each one's status and
    fix (same order, None when not fixed), and the withheld count it stored (None when none was
    stored or it isn't a sensible count)."""

    finding_ids: list[UUID]
    statuses: list[ChecklistStatus]
    fixes: list[Annotated[StoredFix | None, BeforeValidator(read_fix)]]
    withheld_count: int | None


class StoredCodeReview(BaseModel):
    """A completed review of the PR whose code review ran: the reviewed head commit, if known,
    the keys of the findings it saved (a key names the same issue across pushes), and of those
    fixed."""

    head_sha: str | None
    finding_keys: list[str]
    fixed_keys: list[str]
