"""The checklist a saved review's PR comment lists (US-9), read by ``load_comment_checklist``."""

# AI contribution: 50%

from uuid import UUID

from pydantic import BaseModel, ConfigDict

from app.db.models.feedback import Finding


class PublishedReview(BaseModel):
    """The saved review a publication posts: its run and its PR."""

    model_config = ConfigDict(frozen=True)

    run_id: UUID
    pull_request_id: UUID


class StoredChecklistSource(BaseModel):
    """The checklist review a comment lists, from ``comment_checklist_source``."""

    model_config = ConfigDict(frozen=True)

    # None when no review of the PR, up to the comment's, had a code review that ran.
    output_id: UUID | None
    # None when none was stored or it isn't a sensible count.
    withheld_count: int | None
    # Its code review ran but had no reviewable change to send the model.
    nothing_reviewed: bool
    # A code review after it, up to the comment's review, couldn't run.
    failed_since: bool


class Checklist(BaseModel):
    """The findings of a review whose code review ran, most important first."""

    model_config = ConfigDict(frozen=True)

    findings: list[Finding]
    # Low-confidence findings that review left out.
    withheld_count: int
    # The PR had no reviewable change (only skipped files), so the model wasn't asked.
    nothing_reviewed: bool = False


class CommentChecklist(BaseModel):
    """What the PR comment of one saved review lists."""

    model_config = ConfigDict(frozen=True)

    # The checklist as it stood when the review ran. None when no review of the PR, up to this
    # one, had a code review that ran.
    checklist: Checklist | None
    # A code review after that checklist (or, without one, any code review), up to this one,
    # couldn't run: the checklist is older than the code.
    failed_since: bool
