"""A code-review finding in the shape ``save_findings`` stores it (US-9)."""

# AI contribution: 50%

from dataclasses import dataclass


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
