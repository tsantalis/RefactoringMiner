"""Durable status for publishing saved reviews to GitHub."""

# AI contribution: 50%

from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db import connect
from app.db.models.checklist import PublishedReview


async def find_published_review(
    connection: AsyncConnection[DictRow], publication_id: UUID
) -> PublishedReview | None:
    """The review that publication ``publication_id`` posts, or None if there's no such one."""
    cursor = await connection.execute(
        """SELECT a.id AS run_id, a.pull_request_id
           FROM github_publications p
           JOIN analysis_outputs o ON o.id = p.analysis_output_id
           JOIN analysis_runs a ON a.id = o.analysis_run_id
           WHERE p.id = %s""",
        (publication_id,),
    )
    row = await cursor.fetchone()
    return PublishedReview.model_validate(row) if row is not None else None


async def get_publication(url: str, publication_id: UUID) -> DictRow | None:
    async with await connect(url) as connection:
        cursor = await connection.execute(
            """SELECT p.status, o.json_content, e.payload
               FROM github_publications p
               JOIN analysis_outputs o ON o.id = p.analysis_output_id
               JOIN analysis_runs a ON a.id = o.analysis_run_id
               JOIN webhook_events e ON e.id = a.trigger_event_id
               WHERE p.id = %s AND p.publication_key = 'pr_review_comment'
                 AND p.publication_type = 'issue_comment' AND a.status = 'completed'""",
            (publication_id,),
        )
        return await cursor.fetchone()


async def mark_published(url: str, publication_id: UUID, comment_id: int, comment_url: str) -> None:
    async with await connect(url) as connection:
        await connection.execute(
            """UPDATE github_publications SET status = 'published', github_object_id = %s,
               url = %s, published_at = now(), error_message = NULL WHERE id = %s""",
            (comment_id, comment_url, publication_id),
        )


async def mark_publication_failed(url: str, publication_id: UUID, error_type: str) -> None:
    """Keep only a safe error category, never a raw HTTP response or credential."""
    async with await connect(url) as connection:
        await connection.execute(
            """UPDATE github_publications SET status = 'failed', error_message = %s
               WHERE id = %s AND status <> 'published'""",
            (error_type, publication_id),
        )


async def is_superseded(url: str, publication_id: UUID) -> bool:
    """Whether a later review of the same PR completed after this publication's review began.

    The PR's review comment is edited in place, so publishing an older review after a newer one
    would put the older checklist back.
    """
    async with await connect(url) as connection:
        cursor = await connection.execute(
            """SELECT EXISTS (
                   SELECT 1 FROM github_publications p
                   JOIN analysis_outputs o ON o.id = p.analysis_output_id
                   JOIN analysis_runs a ON a.id = o.analysis_run_id
                   JOIN analysis_runs later
                     ON later.pull_request_id = a.pull_request_id
                    AND later.analysis_type = 'llm_review' AND later.status = 'completed'
                    AND (later.created_at, later.id) > (a.created_at, a.id)
                   WHERE p.id = %s) AS superseded""",
            (publication_id,),
        )
        row = await cursor.fetchone()
        return bool(row and row["superseded"])
