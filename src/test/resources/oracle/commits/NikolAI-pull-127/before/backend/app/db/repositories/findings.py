"""Save the code-review agent's findings with the review output they belong to (US-9)."""

# AI contribution: 50%

from collections.abc import Sequence
from uuid import UUID

from psycopg import AsyncConnection, AsyncCursor
from psycopg.rows import DictRow
from psycopg.types.json import Jsonb

from app.db.models.findings import CodeFinding


async def save_findings(
    connection: AsyncConnection[DictRow], output_id: UUID, findings: Sequence[CodeFinding]
) -> None:
    """One ``findings`` row per finding and one new-side ``finding_locations`` row per range.

    Two batched statements, inside the caller's transaction, so a review is never saved
    without its findings.
    """
    if not findings:
        return
    async with connection.cursor() as cursor:
        await cursor.executemany(
            """INSERT INTO findings
               (analysis_output_id, finding_key, finding_type, name, description, severity,
                details)
               VALUES (%s, %s, %s, %s, %s, %s, %s) RETURNING id""",
            [
                (
                    output_id,
                    finding.key,
                    finding.category,
                    finding.name,
                    finding.description,
                    finding.severity,
                    Jsonb({"confidence": finding.confidence, "rank": finding.rank}),
                )
                for finding in findings
            ],
            returning=True,
        )
        ids = await _returned_ids(cursor)
        await cursor.executemany(
            """INSERT INTO finding_locations (finding_id, side, file_path, start_line, end_line)
               VALUES (%s, 'child', %s, %s, %s)""",
            [
                (finding_id, finding.file_path, first, last)
                for finding_id, finding in zip(ids, findings, strict=True)
                for first, last in finding.lines
            ],
        )


async def _returned_ids(cursor: AsyncCursor[DictRow]) -> list[UUID]:
    """The ``RETURNING id`` of each statement ``executemany(..., returning=True)`` ran."""
    ids: list[UUID] = []
    while True:
        row = await cursor.fetchone()
        if row is None:
            raise RuntimeError("Finding insert returned no ID")
        ids.append(row["id"])
        if not cursor.nextset():
            return ids
