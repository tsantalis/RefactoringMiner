"""Read stored PRs and feedback for US-5 without calling GitHub or an AI provider.

IDs are database UUIDs, not GitHub numeric IDs. Filters can be combined and are
not authorization checks: the API must determine which users/teams may be read.
Callers own their connections. Queries include archived repositories/teams so
previously saved feedback remains available.
"""

# AI contribution: 50%

from typing import NamedTuple, cast
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db.models.feedback import (
    StoredFeedback,
    StoredFindingLocation,
    StoredPullRequest,
    StoredReviewRun,
)


class RegisteredRepository(NamedTuple):
    id: UUID
    team_id: UUID
    full_name: str


_PR_SELECT = """
    SELECT p.id, p.repository_id, r.full_name AS repository_name, r.team_id,
           p.author_user_id, u.github_login AS author_login, p.github_pr_id,
           p.pr_number, p.title, p.state, p.merged_at,
           p.github_created_at, p.github_updated_at
    FROM pull_requests p
    JOIN repositories r ON r.id = p.repository_id
    JOIN users u ON u.id = p.author_user_id
"""


def _validate_page(limit: int, offset: int) -> None:
    if not 1 <= limit <= 100:
        raise ValueError("limit must be between 1 and 100")
    if offset < 0:
        raise ValueError("offset must be nonnegative")


async def list_pull_requests(
    connection: AsyncConnection[DictRow],
    *,
    user_id: UUID | None = None,
    team_id: UUID | None = None,
    repository_id: UUID | None = None,
    limit: int = 50,
    offset: int = 0,
) -> list[StoredPullRequest]:
    """List PRs newest-updated first, filtered by author, current team and/or repository.

    Includes PRs with no review or only failed attempts. The UUID breaks timestamp
    ties so pagination is deterministic for unchanged data.
    """
    _validate_page(limit, offset)
    cursor = await connection.execute(
        _PR_SELECT
        + """ WHERE (%s::uuid IS NULL OR p.author_user_id = %s)
                AND (%s::uuid IS NULL OR r.team_id = %s)
                AND (%s::uuid IS NULL OR p.repository_id = %s)
              ORDER BY p.github_updated_at DESC, p.id DESC
              LIMIT %s OFFSET %s""",
        (user_id, user_id, team_id, team_id, repository_id, repository_id, limit, offset),
    )
    return [StoredPullRequest.model_validate(row) for row in await cursor.fetchall()]


async def get_pull_request(
    connection: AsyncConnection[DictRow], pull_request_id: UUID
) -> StoredPullRequest | None:
    """Return a stored PR, or None so the API can return 404 for an unknown PR."""
    cursor = await connection.execute(_PR_SELECT + " WHERE p.id = %s", (pull_request_id,))
    row = await cursor.fetchone()
    return StoredPullRequest.model_validate(row) if row is not None else None


async def list_feedback(
    connection: AsyncConnection[DictRow],
    *,
    user_id: UUID | None = None,
    team_id: UUID | None = None,
    pull_request_id: UUID | None = None,
    limit: int = 50,
    offset: int = 0,
) -> list[StoredFeedback]:
    """Read completed review history by PR author, team at run time, and/or PR.

    Returns one row per saved review, newest output first, with full text and JSON.
    Repeated reviews remain separate; failed/running attempts and other analysis
    types/outputs are excluded. Unknown IDs or PRs without feedback return [].
    Use get_pull_request to distinguish an unknown PR from one awaiting a review.
    """
    _validate_page(limit, offset)
    cursor = await connection.execute(
        """SELECT o.id AS output_id, a.id AS run_id, p.id AS pull_request_id,
                  p.repository_id, r.full_name AS repository_name, p.pr_number, p.title,
                  p.author_user_id, u.github_login AS author_login,
                  a.team_id_at_run AS team_id, a.tool_name, a.tool_version,
                  a.finished_at, o.created_at, o.text_content, o.json_content
           FROM analysis_outputs o
           JOIN analysis_runs a ON a.id = o.analysis_run_id
           JOIN pull_requests p ON p.id = a.pull_request_id
           JOIN repositories r ON r.id = p.repository_id
           JOIN users u ON u.id = p.author_user_id
           WHERE a.analysis_type = 'llm_review' AND a.status = 'completed'
             AND o.output_key = 'review' AND o.output_type = 'agent_result'
             AND (%s::uuid IS NULL OR p.author_user_id = %s)
             AND (%s::uuid IS NULL OR a.team_id_at_run = %s)
             AND (%s::uuid IS NULL OR p.id = %s)
           ORDER BY o.created_at DESC, o.id DESC
           LIMIT %s OFFSET %s""",
        (user_id, user_id, team_id, team_id, pull_request_id, pull_request_id, limit, offset),
    )
    return [StoredFeedback.model_validate(row) for row in await cursor.fetchall()]


async def find_repository(
    connection: AsyncConnection[DictRow], full_name: str
) -> RegisteredRepository | None:
    """The registered repository ``owner/name`` (any case), archived or not.

    Names aren't unique (a repo deleted and recreated, or registered again next term), so the
    active row wins, then the newest.
    """
    cursor = await connection.execute(
        """SELECT id, team_id, full_name FROM repositories WHERE lower(full_name) = lower(%s)
           ORDER BY archived_at IS NULL DESC, created_at DESC, id DESC LIMIT 1""",
        (full_name,),
    )
    row = await cursor.fetchone()
    if row is None:
        return None
    return RegisteredRepository(
        cast(UUID, row["id"]), cast(UUID, row["team_id"]), cast(str, row["full_name"])
    )


async def find_pull_request(
    connection: AsyncConnection[DictRow], repository_id: UUID, number: int
) -> StoredPullRequest | None:
    cursor = await connection.execute(
        _PR_SELECT + " WHERE p.repository_id = %s AND p.pr_number = %s", (repository_id, number)
    )
    row = await cursor.fetchone()
    return StoredPullRequest.model_validate(row) if row is not None else None


async def list_review_runs(
    connection: AsyncConnection[DictRow], pull_request_id: UUID
) -> list[StoredReviewRun]:
    """Every ``llm_review`` run of the PR, newest first, failed ones included.

    Each row carries the run's review output (if it completed) and the URL of the comment that
    published it (if one was posted).
    """
    cursor = await connection.execute(
        """SELECT a.id, a.status, a.tool_name, a.tool_version, a.created_at, a.started_at,
                  a.finished_at, a.error_message, a.retry_of_run_id,
                  o.text_content, o.json_content,
                  (SELECT g.url FROM github_publications g
                   WHERE g.analysis_output_id = o.id AND g.publication_key = 'pr_review_comment'
                     AND g.status = 'published' AND g.url IS NOT NULL
                   ORDER BY g.published_at DESC NULLS LAST LIMIT 1) AS comment_url
           FROM analysis_runs a
           LEFT JOIN analysis_outputs o
             ON o.analysis_run_id = a.id AND o.output_key = 'review'
            AND o.output_type = 'agent_result'
           WHERE a.pull_request_id = %s AND a.analysis_type = 'llm_review'
           ORDER BY a.created_at DESC, a.id DESC""",
        (pull_request_id,),
    )
    return [StoredReviewRun.model_validate(row) for row in await cursor.fetchall()]


async def list_latest_findings(
    connection: AsyncConnection[DictRow], pull_request_id: UUID
) -> list[StoredFindingLocation]:
    """The findings of the PR's latest completed review, one row per finding location.

    Only the run's ``review`` output counts, the same output ``list_review_runs`` returns.
    A finding without locations comes back once, with None location fields.
    """
    cursor = await connection.execute(
        """WITH latest AS (
               SELECT id FROM analysis_runs
               WHERE pull_request_id = %s AND analysis_type = 'llm_review'
                 AND status = 'completed'
               ORDER BY created_at DESC, id DESC LIMIT 1)
           SELECT f.id, f.name, f.description, f.severity, f.finding_type AS category,
                  l.file_path, l.side, l.start_line, l.end_line
           FROM latest
           JOIN analysis_outputs o
             ON o.analysis_run_id = latest.id AND o.output_key = 'review'
            AND o.output_type = 'agent_result'
           JOIN findings f ON f.analysis_output_id = o.id
           LEFT JOIN finding_locations l ON l.finding_id = f.id
           ORDER BY f.finding_key, f.id, l.file_path, l.start_line NULLS FIRST, l.id""",
        (pull_request_id,),
    )
    return [StoredFindingLocation.model_validate(row) for row in await cursor.fetchall()]
