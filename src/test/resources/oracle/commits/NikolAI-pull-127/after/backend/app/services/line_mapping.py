"""Follow a line of a file through a later commit's patch (US-9.4).

A push review shows each commit's own diff, so a new finding is reported at line numbers of the
file as that commit left it. The checklist links into the PR's Files changed, which shows the
head's version, so each later commit of the push that changed the file moves the line: lines
added or removed above it shift it, and a line the later commit changed or removed has no
place in the head's version any more.
"""

# AI contribution: 50%

import re
from typing import Final

_HUNK_HEADER: Final = re.compile(r"^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@")


def map_line(patch: str, line: int) -> int | None:
    """The number of old-side ``line`` on the new side of ``patch``, or None if it changed.

    Hunks come in file order. A line before a hunk moves by what the earlier hunks added or
    removed; a line inside one keeps its place only as an unchanged context line.
    """
    offset = 0
    lines = patch.removesuffix("\n").split("\n")
    for index, raw in enumerate(lines):
        header = _HUNK_HEADER.match(raw.removesuffix("\r"))
        if header is None:
            continue
        old_start, old_count, new_start, new_count = _hunk_range(header)
        if line < old_start:
            return line + offset
        if line < old_start + old_count:
            return _inside_hunk(lines[index + 1 :], line, old_start, new_start)
        offset = (new_start + new_count) - (old_start + old_count)
    return line + offset


def _hunk_range(header: re.Match[str]) -> tuple[int, int, int, int]:
    """Start and count of each side. A count left out is 1; an empty side (count 0) names the
    line before the hunk, so its start moves one down to stay "the first line of the hunk"."""
    old_start, new_start = int(header[1]), int(header[3])
    old_count = 1 if header[2] is None else int(header[2])
    new_count = 1 if header[4] is None else int(header[4])
    return (
        old_start + (old_count == 0),
        old_count,
        new_start + (new_count == 0),
        new_count,
    )


def _inside_hunk(body: list[str], line: int, old: int, new: int) -> int | None:
    """Walk the hunk's lines to ``line``: its new number if it is context, else None."""
    for raw in body:
        if raw.startswith("@@"):
            break
        if raw.startswith("\\"):
            continue  # "\ No newline at end of file"
        if raw.startswith("+"):
            new += 1
            continue
        if old == line:
            return None if raw.startswith("-") else new
        new += 0 if raw.startswith("-") else 1
        old += 1
    return None
