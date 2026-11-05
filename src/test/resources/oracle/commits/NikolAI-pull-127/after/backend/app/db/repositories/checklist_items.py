"""Read the checklist a push review starts from (US-9.4): its head commit and its items.

The checklist is the PR's ``CHECKLIST_OUTPUT`` as it stood when the review run was created, the
same one the run's comment would list. Each item comes back as the ``CodeFinding`` it was saved
as, so the push review can carry it into the new review with its key, origin and fix.
"""

# AI contribution: 50%

from dataclasses import dataclass
from typing import cast
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db.models.findings import FIXED_IN, ORIGIN_ID, CodeFinding
from app.db.models.pull_insights import read_fix
from app.db.repositories.feedback import CHECKLIST_OUTPUT, FINDING_ORDER
from app.db.repositories.insights import MAX_WITHHELD, WITHHELD_COUNT, WITHHELD_VALUE


@dataclass(frozen=True, slots=True)
class StoredItem:
    """A checklist item: the saved finding's id, and the finding."""

    id: UUID
    finding: CodeFinding


@dataclass(frozen=True, slots=True)
class PreviousChecklist:
    """The checklist before a push: the commit it was made for, and its items in order."""

    head_sha: str | None
    withheld_count: int
    items: list[StoredItem]


_ITEMS = f"""
    WITH {CHECKLIST_OUTPUT}
    SELECT coalesce(a.child_commit_sha, e.payload #>> '{{pull_request,head,sha}}') AS head_sha,
           {WITHHELD_COUNT} AS withheld_count,
           f.id, f.finding_key, f.name, f.description, f.severity, f.finding_type, f.details,
           l.file_path, l.start_line, l.end_line
    FROM checklist_output
    {WITHHELD_VALUE}
    JOIN analysis_runs a ON a.id = checklist_output.run_id
    LEFT JOIN webhook_events e ON e.id = a.trigger_event_id
    LEFT JOIN findings f ON f.analysis_output_id = checklist_output.id
    LEFT JOIN finding_locations l ON l.finding_id = f.id AND l.side = 'child'
    ORDER BY {FINDING_ORDER}, l.start_line NULLS FIRST, l.id"""  # noqa: S608 - module constants


async def previous_checklist(
    connection: AsyncConnection[DictRow], run_id: UUID
) -> PreviousChecklist | None:
    """The checklist review run ``run_id`` follows, or None when the PR has none yet."""
    cursor = await connection.execute(
        "SELECT pull_request_id FROM analysis_runs WHERE id = %s", (run_id,)
    )
    run = await cursor.fetchone()
    if run is None or run["pull_request_id"] is None:
        return None
    cursor = await connection.execute(
        _ITEMS, {"pull_request": run["pull_request_id"], "up_to_run": run_id, "max": MAX_WITHHELD}
    )
    rows = await cursor.fetchall()
    if not rows:
        return None
    return PreviousChecklist(
        head_sha=cast("str | None", rows[0]["head_sha"]),
        # A missing or nonsensical stored count reads as none withheld, as in the PR insights.
        withheld_count=rows[0]["withheld_count"] or 0,
        items=_fold(rows),
    )


def _fold(rows: list[DictRow]) -> list[StoredItem]:
    """One item per finding, with all its new-side line ranges, in checklist order."""
    items: dict[UUID, tuple[DictRow, list[tuple[int, int]]]] = {}
    for row in rows:
        if row["id"] is None:
            continue  # a checklist with no items
        _, lines = items.setdefault(row["id"], (row, []))
        if row["start_line"] is not None:
            lines.append((row["start_line"], row["end_line"]))
    return [
        StoredItem(finding_id, _finding(row, lines)) for finding_id, (row, lines) in items.items()
    ]


def _finding(row: DictRow, lines: list[tuple[int, int]]) -> CodeFinding:
    details = cast("dict[str, object]", row["details"] or {})
    confidence = details.get("confidence")
    rank = details.get("rank")
    origin = details.get(ORIGIN_ID)
    return CodeFinding(
        key=row["finding_key"],
        name=row["name"],
        description=row["description"] or "",
        severity=row["severity"] or "info",
        category=row["finding_type"],
        file_path=row["file_path"] or "",
        lines=tuple(lines),
        confidence=float(confidence) if isinstance(confidence, int | float) else 0.0,
        rank=rank if isinstance(rank, int) else 0,
        origin_id=UUID(origin) if isinstance(origin, str) else None,
        fixed_in=read_fix(details.get(FIXED_IN)),
    )
