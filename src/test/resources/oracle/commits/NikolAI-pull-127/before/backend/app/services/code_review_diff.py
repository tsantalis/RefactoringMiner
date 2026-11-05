"""What the code-review agent is shown of a PR (US-9, 9.2), and which lines it may cite.

Each file's patch is shown with the new version's line numbers written in, so the model cites
real lines instead of counting through hunks. The same pass records each hunk's new-side line
range, which is how NikolAI later checks that a finding points inside the diff.

A hunk's range includes GitHub's unchanged context lines around the change, on purpose: a
change often breaks code right next to it (a removed check, a renamed variable still used on
the next line), and a removed line has no new-side number to cite, so the nearest context
line is where that issue shows.

What is reviewed (9.2, #62): files without a patch (binary, or too large for GitHub to send
one), lockfiles, generated, vendored and minified files, and any single file too large for one
model call are left out and named with the reason. The other files are packed, in GitHub's
order, into batches under one call's character budget, one model call per batch. A PR gets at
most ``MAX_BATCHES`` calls; files that don't fit in them are named as over the budget, so the
checklist can say how many files weren't reviewed.
"""

# AI contribution: 50%

import html
import re
from dataclasses import dataclass
from typing import Final

from app.agents.untrusted import description_for_prompt, neutralize_tags
from app.services.pull_request_details import PullRequestDetails

# Annotated diff characters per model call, about 8k tokens: inside the free models' context
# windows with room for the system prompt and the reply.
MAX_DIFF_CHARS: Final = 32_000
# Model calls for one PR's code. With the title review that's 5 requests, a quarter of the free
# models' 20 a minute, so a large PR can't starve the reviews of other PRs.
MAX_BATCHES: Final = 4
CODE_REVIEW_DESCRIPTION_CHARS: Final = 2_000

SKIPPED_NO_PATCH: Final = "no_patch"  # binary, or too large for GitHub to send a patch
SKIPPED_GENERATED: Final = "generated"  # a lockfile, vendored code, a build output or minified
SKIPPED_TOO_LARGE: Final = "too_large"  # one file's patch is over a whole call's budget
SKIPPED_OVER_BUDGET: Final = "over_budget"  # didn't fit in the PR's ``MAX_BATCHES`` calls

_HUNK_HEADER = re.compile(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@")
# Files a student doesn't write by hand. They can be large and would use up the budget.
_GENERATED = re.compile(
    r"(?:^|/)(?:package-lock\.json|npm-shrinkwrap\.json|pnpm-lock\.yaml|yarn\.lock"
    r"|poetry\.lock|uv\.lock|Pipfile\.lock|Cargo\.lock|Gemfile\.lock|composer\.lock"
    r"|go\.sum|[^/]+\.lock)$"
    r"|\.min\.(?:js|css)$|\.map$|_pb2\.pyi?$|\.pb\.go$"
    r"|(?:^|/)(?:dist|build|node_modules|vendor|third_party|__generated__)/",
    re.IGNORECASE,
)


@dataclass(frozen=True, slots=True)
class ReviewableDiff:
    """One batch: the prompt of one model call, and the lines it may cite."""

    prompt: str
    # New-side line ranges (first, last) of each shown file's hunks.
    changed_lines: dict[str, tuple[tuple[int, int], ...]]

    def contains(self, file_path: str, start_line: int, end_line: int) -> bool:
        """Whether lines ``start_line`` to ``end_line`` of ``file_path`` lie in one shown hunk."""
        return start_line <= end_line and any(
            first <= start_line and end_line <= last
            for first, last in self.changed_lines.get(file_path, ())
        )


@dataclass(frozen=True, slots=True)
class ReviewPlan:
    """How a PR's code is reviewed: the batches, and the changed files left out."""

    batches: tuple[ReviewableDiff, ...]
    # Changed files no batch shows, with the reason.
    skipped_files: dict[str, str]
    # Changed files GitHub didn't list, so they have no name to skip.
    unfetched_files: int


@dataclass(frozen=True, slots=True)
class _ShownFile:
    path: str
    block: str
    ranges: tuple[tuple[int, int], ...]


def plan_review(details: PullRequestDetails) -> ReviewPlan:
    """Choose the PR's files worth reviewing and pack them into batches under the budget."""
    shown: list[_ShownFile] = []
    skipped: dict[str, str] = {}
    for path in details.files_changed:
        file = _shown_file(path, details.diffs.get(path, ""))
        if isinstance(file, _ShownFile):
            shown.append(file)
        else:
            skipped[path] = file
    groups, over = _pack(shown)
    skipped.update(dict.fromkeys(over, SKIPPED_OVER_BUDGET))
    hidden = not_reviewed_count(skipped, details.unfetched_files)
    batches = tuple(
        ReviewableDiff(
            _prompt(details, [file.block for file in group], (part, len(groups)), hidden),
            {file.path: file.ranges for file in group},
        )
        for part, group in enumerate(groups, start=1)
    )
    return ReviewPlan(batches, skipped, details.unfetched_files)


def not_reviewed_count(skipped_files: dict[str, str], unfetched_files: int) -> int:
    """Every changed file that wasn't reviewed: those named with a reason, and those GitHub
    didn't list."""
    return len(skipped_files) + unfetched_files


def _shown_file(path: str, patch: str) -> _ShownFile | str:
    """The file as the model would see it, or the reason it's never sent to the model."""
    if not patch:
        return SKIPPED_NO_PATCH
    if _GENERATED.search(path):
        return SKIPPED_GENERATED
    annotated, ranges = annotate_patch(patch)
    block = f'<file path="{html.escape(path, quote=True)}">\n{annotated}\n</file>'
    if len(block) > MAX_DIFF_CHARS:
        return SKIPPED_TOO_LARGE
    return _ShownFile(path, block, ranges)


def _pack(files: list[_ShownFile]) -> tuple[list[list[_ShownFile]], list[str]]:
    """First fit, in GitHub's order: each file goes in the first batch with room for it.

    A new batch opens when none has room, up to ``MAX_BATCHES``; files that still don't fit
    are returned by path. First fit lets a small file fill the space a large one left.
    """
    groups: list[list[_ShownFile]] = []
    room: list[int] = []
    over: list[str] = []
    for file in files:
        size = len(file.block)
        slot = next((i for i, left in enumerate(room) if size <= left), None)
        if slot is None and len(groups) < MAX_BATCHES:
            groups.append([])
            room.append(MAX_DIFF_CHARS)
            slot = len(groups) - 1
        if slot is None:
            over.append(file.path)
            continue
        groups[slot].append(file)
        room[slot] -= size
    return groups, over


def annotate_patch(patch: str) -> tuple[str, tuple[tuple[int, int], ...]]:
    """Write the new-side line number before each added or unchanged line of a patch.

    Returns the annotated patch (with our tags neutralized) and each hunk's new-side range.
    """
    lines: list[str] = []
    ranges: list[tuple[int, int]] = []
    number: int | None = None
    first: int | None = None
    # Only "\n" ends a diff line: splitlines() would also split on form feeds, U+2028 and
    # other characters inside a source line, and shift every later line number.
    for raw in patch.removesuffix("\n").split("\n"):
        raw = raw.removesuffix("\r")
        header = _HUNK_HEADER.match(raw)
        if header:
            if first is not None and number is not None:
                ranges.append((first, number - 1))
            number, first = int(header[1]), None
            lines.append(raw)
        elif number is None:
            continue  # nothing before the first hunk header is part of the diff
        elif raw.startswith(("-", "\\")):
            lines.append(f"{'':>6}{raw}")
        else:
            lines.append(f"{number:>5} {raw or ' '}")
            first = number if first is None else first
            number += 1
    if first is not None and number is not None:
        ranges.append((first, number - 1))
    return neutralize_tags("\n".join(lines)), tuple(ranges)


def _prompt(
    details: PullRequestDetails, blocks: list[str], part: tuple[int, int], hidden_count: int
) -> str:
    description = description_for_prompt(details.description, CODE_REVIEW_DESCRIPTION_CHARS)
    number, parts = part
    part_note = (
        f" The changes are reviewed in {parts} parts and this is part {number}: the other "
        "parts are reviewed separately, so review only the files shown here. Code in the "
        "other parts isn't shown: don't report a problem that depends on code you can't see, "
        "such as a call to a function or an import defined elsewhere."
        if parts > 1
        else ""
    )
    skipped_note = (
        f" {hidden_count} changed files aren't shown (binary, generated, too large, or over "
        "the size budget): don't comment on them."
        if hidden_count
        else ""
    )
    files = "\n".join(blocks)
    return (
        f"Review the code changes in this pull request.{part_note}{skipped_note}\n\n"
        "<pull_request>\n"
        f"<title>{neutralize_tags(details.title.strip())}</title>\n"
        f"<description>\n{description}\n</description>\n"
        f"<diff>\n{files}\n</diff>\n"
        "</pull_request>"
    )
