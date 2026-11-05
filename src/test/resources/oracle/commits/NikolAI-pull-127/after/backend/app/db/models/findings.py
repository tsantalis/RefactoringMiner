"""A code-review finding in the shape ``save_findings`` stores it (US-9).

A checklist item keeps its identity across pushes (9.4) by its ``key``: when a push is reviewed,
every item of the previous checklist is copied into the new review with the same key and a link
to the finding it started as (``origin_id``). An item's status follows the key, so a student's
acknowledgement or dispute stays with the item, and ``fixed_in`` marks the item fixed.
"""

# AI contribution: 50%

from dataclasses import dataclass
from uuid import UUID

from app.db.models.pull_insights import StoredFix

# Keys of ``findings.details``. A finding is fixed exactly when its details have ``FIXED_IN``.
FIXED_IN = "fixed_in"
ORIGIN_ID = "origin_finding_id"


@dataclass(frozen=True, slots=True)
class CodeFinding:
    """A finding NikolAI keeps: validated, inside the diff, confident enough."""

    # Category, path and name, without line numbers, so the same issue keeps its key when a
    # later push moves it (9.4 follows an issue across pushes).
    key: str
    name: str
    description: str
    severity: str
    category: str
    file_path: str
    # New-side (first, last) line ranges: the same issue can be reported at several places.
    lines: tuple[tuple[int, int], ...]
    confidence: float
    # Position in the checklist, most important first, kept so the read API can order by it.
    rank: int
    # The finding this item was first saved as, when it was carried over from an earlier review.
    origin_id: UUID | None = None
    # Set once a push fixed the item; it stays ticked in later reviews unless the issue comes back.
    fixed_in: StoredFix | None = None

    def details(self) -> dict[str, object]:
        """The ``findings.details`` JSON: ``FIXED_IN`` and ``ORIGIN_ID`` only when set."""
        details: dict[str, object] = {"confidence": self.confidence, "rank": self.rank}
        if self.origin_id is not None:
            details[ORIGIN_ID] = str(self.origin_id)
        if self.fixed_in is not None:
            details[FIXED_IN] = self.fixed_in.model_dump()
        return details
