"""Acknowledge or dispute a feedback item (US-10, task 10.4).

Only an active member of the team that received the feedback may respond to it; course TAs
and instructors may read it (``can_read_team``) but do not answer on the team's behalf.
"""

# AI contribution: 50%

from typing import Annotated, Literal
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field

from app.api.dependencies import current_user, database, require_allowed_origin
from app.core.config import Settings, get_settings
from app.db.models.courses import FeedbackActionRecord
from app.db.models.users import User
from app.db.repositories.courses import is_team_member
from app.db.repositories.feedback_actions import (
    FeedbackActionError,
    feedback_team,
    record_feedback_action,
)

router = APIRouter(tags=["feedback"])


class FeedbackActionRequest(BaseModel):
    """A student's response to one feedback item, optionally to one finding in it."""

    action: Literal["acknowledged", "disputed"]
    finding_id: UUID | None = None
    comment: str | None = Field(default=None, max_length=2000)


@router.post(
    "/api/feedback/{analysis_output_id}/actions",
    status_code=status.HTTP_201_CREATED,
    dependencies=[Depends(require_allowed_origin)],
)
async def record_feedback_action_route(
    analysis_output_id: UUID,
    body: FeedbackActionRequest,
    user: Annotated[User, Depends(current_user)],
    settings: Annotated[Settings, Depends(get_settings)],
) -> FeedbackActionRecord:
    """Save an acknowledgement or dispute after checking the caller belongs to the team."""
    async with database(settings) as connection:
        team_id = await feedback_team(connection, analysis_output_id)
        if team_id is None:
            raise HTTPException(status.HTTP_404_NOT_FOUND, "That feedback item does not exist.")
        if not await is_team_member(connection, user_id=user.id, team_id=team_id):
            raise HTTPException(
                status.HTTP_403_FORBIDDEN,
                "Only members of the team that received this feedback can respond to it.",
            )
        try:
            return await record_feedback_action(
                connection,
                user_id=user.id,
                analysis_output_id=analysis_output_id,
                action_type=body.action,
                finding_id=body.finding_id,
                comment=body.comment,
            )
        except FeedbackActionError as error:
            raise HTTPException(status.HTTP_400_BAD_REQUEST, str(error)) from None
