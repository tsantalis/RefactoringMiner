"""Which GitHub repositories NikolAI serves: those registered to an active team."""

# AI contribution: 50% or more AI-generated

from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow

_ACTIVE_REPOSITORY_SQL = """SELECT r.id FROM repositories r JOIN teams t ON t.id = r.team_id
   WHERE r.github_repository_id = %s AND r.archived_at IS NULL AND t.archived_at IS NULL"""


async def active_repository_id(
    connection: AsyncConnection[DictRow], github_repository_id: int, *, lock: bool = False
) -> UUID | None:
    """Our ID of the repository, or None unless it and its team are registered and active.

    ``lock`` keeps the repository row locked until the caller's transaction ends.
    """
    query = _ACTIVE_REPOSITORY_SQL + (" FOR UPDATE OF r" if lock else "")
    cursor = await connection.execute(query, (github_repository_id,))
    row = await cursor.fetchone()
    return None if row is None else UUID(str(row["id"]))
