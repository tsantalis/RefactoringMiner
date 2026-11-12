"""The latest backlog coverage check of each repository (US-13.2)."""

# AI contribution: 50%

from typing import cast
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow
from psycopg.types.json import Jsonb

from app.db.models.backlog_coverage import CoverageOutcome, RequirementCoverage, StoredCoverage


async def team_project_requirements(connection: AsyncConnection[DictRow], team_id: UUID) -> str:
    """The project requirements of the team's course; empty for a team in no course."""
    cursor = await connection.execute(
        """SELECT c.project_requirements FROM teams t
           JOIN courses c ON c.id = t.course_id WHERE t.id = %s""",
        (team_id,),
    )
    row = await cursor.fetchone()
    return cast(str, row["project_requirements"]) if row is not None else ""


async def save_coverage(
    connection: AsyncConnection[DictRow], repository_id: UUID, outcome: CoverageOutcome
) -> None:
    """Store ``outcome`` as the repository's latest check, replacing the one before."""
    result = outcome.model_dump(mode="json", include={"requirements"})
    await connection.execute(
        """INSERT INTO backlog_coverage_checks (repository_id, status, reason, result)
           VALUES (%s, %s, %s, %s)
           ON CONFLICT (repository_id) DO UPDATE
           SET status = EXCLUDED.status, reason = EXCLUDED.reason, result = EXCLUDED.result,
               checked_at = now()""",
        (repository_id, outcome.status, outcome.reason, Jsonb(result)),
    )


async def find_coverage(
    connection: AsyncConnection[DictRow], repository_id: UUID
) -> StoredCoverage | None:
    cursor = await connection.execute(
        """SELECT status, reason, result, checked_at FROM backlog_coverage_checks
           WHERE repository_id = %s""",
        (repository_id,),
    )
    row = await cursor.fetchone()
    if row is None:
        return None
    requirements = row["result"].get("requirements", [])
    return StoredCoverage(
        status=row["status"],
        reason=row["reason"],
        checked_at=row["checked_at"],
        requirements=[RequirementCoverage.model_validate(entry) for entry in requirements],
    )
