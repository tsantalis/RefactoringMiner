"""Save a student's acknowledgement or dispute of a feedback item (US-10, task 10.4).

A feedback item is one ``analysis_outputs`` row (one saved review). The action applies to
the output as a whole, or to one finding inside it. The API checks team membership before
calling ``record_feedback_action``; this module owns the row-level rules.
"""

# AI contribution: 50%

from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db.models.courses import FeedbackActionRecord, FeedbackActionType

ACTION_TYPES: frozenset[str] = frozenset({"acknowledged", "disputed"})
_COMMENT_LIMIT = 2000


class FeedbackActionError(ValueError):
    """The feedback item or finding is unknown, or the action is invalid."""


async def feedback_team(
    connection: AsyncConnection[DictRow], analysis_output_id: UUID
) -> UUID | None:
    """The team the reviewed work belonged to, or None for an unknown output."""
    cursor = await connection.execute(
        """SELECT a.team_id_at_run AS team_id
           FROM analysis_outputs o JOIN analysis_runs a ON a.id = o.analysis_run_id
           WHERE o.id = %s""",
        (analysis_output_id,),
    )
    row = await cursor.fetchone()
    if row is None or row["team_id"] is None:
        return None
    return UUID(str(row["team_id"]))


async def record_feedback_action(
    connection: AsyncConnection[DictRow],
    *,
    user_id: UUID,
    analysis_output_id: UUID,
    action_type: FeedbackActionType,
    finding_id: UUID | None = None,
    comment: str | None = None,
) -> FeedbackActionRecord:
    """Store one action. Repeated actions keep their history rather than overwriting it."""
    if action_type not in ACTION_TYPES:
        raise FeedbackActionError("The action must be acknowledged or disputed.")
    text = comment.strip() if comment is not None else None
    if text is not None and len(text) > _COMMENT_LIMIT:
        raise FeedbackActionError(f"A comment can hold at most {_COMMENT_LIMIT} characters.")
    async with connection.transaction():
        cursor = await connection.execute(
            "SELECT 1 FROM analysis_outputs WHERE id = %s", (analysis_output_id,)
        )
        if await cursor.fetchone() is None:
            raise FeedbackActionError("That feedback item does not exist.")
        if finding_id is not None:
            cursor = await connection.execute(
                """SELECT 1 FROM findings
                   WHERE id = %s AND analysis_output_id = %s""",
                (finding_id, analysis_output_id),
            )
            if await cursor.fetchone() is None:
                raise FeedbackActionError("That finding does not belong to this feedback item.")
        cursor = await connection.execute(
            """INSERT INTO user_actions
                   (user_id, analysis_output_id, finding_id, action_type, comment)
               VALUES (%s, %s, %s, %s, %s)
               RETURNING id, action_type, created_at""",
            (user_id, analysis_output_id, finding_id, action_type, text),
        )
        row = await cursor.fetchone()
    if row is None:
        raise RuntimeError("User action insert returned no row")
    return FeedbackActionRecord.model_validate(row)
