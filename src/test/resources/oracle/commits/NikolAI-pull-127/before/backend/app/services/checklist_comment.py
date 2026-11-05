"""The checklist in NikolAI's review comment (US-9): one unticked item per finding.

Finding names and descriptions are model output about student code, so they are escaped as
plain text on one line. Each item links to its lines in Files changed; its paths are the ones
the code review checked against the PR's diff before it saved the finding
(``app.services.code_review``). Items keep the saved checklist order, most important first, and
end with their finding's id, hidden, so a later update of the comment can tell them apart.
"""

# AI contribution: 50%

from typing import Final

from app.db.models.checklist import Checklist, CommentChecklist
from app.db.models.feedback import Finding, FindingLocation
from app.github.comments import file_line_url
from app.github.markdown import plain

HEADING: Final = "#### Checklist"
# Room kept for the notes under the items, which are short.
_NOTES_ROOM: Final = 200
_NOT_REVIEWED_YET: Final = (
    "NikolAI hasn't reviewed the code changes yet. It will when you push a new commit."
)
_COULD_NOT_REVIEW: Final = (
    "NikolAI couldn't review the code changes this time. "
    "It will try again when you push a new commit."
)
_EARLIER_CHECKLIST: Final = (
    "NikolAI couldn't review the latest code changes, so this checklist is from an earlier "
    "review. It will try again when you push a new commit."
)
_NO_ISSUES: Final = "NikolAI found no issues in the code changes."
_NO_CONFIDENT_ISSUES: Final = "NikolAI found no issues it is confident about in the code changes."
_NOTHING_REVIEWED: Final = (
    "NikolAI had no code changes to review: the changed files are generated, binary or too "
    "large to review."
)


def render_checklist(comment: CommentChecklist, pr_url: str, room: int) -> str:
    """The checklist section of the comment, at most ``room`` characters long.

    When the items don't all fit, the least important ones are left out and a note says how
    many. ``pr_url`` is the PR's page, which the line links point into.
    """
    checklist = comment.checklist
    if checklist is None:
        note = _COULD_NOT_REVIEW if comment.failed_since else _NOT_REVIEWED_YET
        return f"{HEADING}\n\n{note}"
    blocks = [HEADING]
    if comment.failed_since:
        blocks.append(_EARLIER_CHECKLIST)
    if not checklist.findings:
        blocks.append(_no_findings_note(checklist))
    budget = room - sum(len(block) + 2 for block in blocks) - _NOTES_ROOM
    listed = _fitting_items(checklist.findings, pr_url, budget)
    blocks.append("\n".join(listed))
    blocks.extend(_notes(len(checklist.findings) - len(listed), checklist.withheld_count))
    return "\n\n".join(block for block in blocks if block)


def _no_findings_note(checklist: Checklist) -> str:
    """Why an empty checklist is empty: nothing to review, only unsure findings, or no issue."""
    if checklist.nothing_reviewed:
        return _NOTHING_REVIEWED
    return _NO_CONFIDENT_ISSUES if checklist.withheld_count else _NO_ISSUES


def _notes(hidden: int, withheld: int) -> list[str]:
    """What the items don't show: the ones left out for room, and the low-confidence ones."""
    notes: list[str] = []
    if hidden:
        notes.append(
            f"_{_count(hidden, 'more finding', 'is', 'are')} not listed, to fit in one comment._"
        )
    if withheld:
        notes.append(f"_{_count(withheld, 'low-confidence finding', 'was', 'were')} left out._")
    return notes


def _fitting_items(findings: list[Finding], pr_url: str, room: int) -> list[str]:
    """The rendered items, in order, until the next one would take more than ``room``."""
    listed: list[str] = []
    for finding in findings:
        rendered = _render_item(finding, pr_url)
        room -= len(rendered) + 1
        if room < 0:
            break
        listed.append(rendered)
    return listed


def _render_item(finding: Finding, pr_url: str) -> str:
    """``- [ ] **name** · Severity · category · path#L12``, then the description below it.

    The item is one Markdown line, the description after a ``<br>``: a description on a line
    of its own could start a heading or a list ("# x", "1. x").
    """
    labels = [f"**{plain(finding.name)}**"]
    if finding.severity is not None:
        labels.append(finding.severity.capitalize())
    if finding.category is not None:
        labels.append(plain(finding.category))
    labels.extend(_location_link(location, pr_url) for location in finding.locations)
    rendered = f"- [ ] {' · '.join(labels)}"
    if finding.description:
        rendered += f"<br>{plain(finding.description)}"
    return f"{rendered} <!-- nikolai:finding:{finding.id} -->"


def _location_link(location: FindingLocation, pr_url: str) -> str:
    """``[path#L12-L15](link)``, the way the NikolAI tab writes a location."""
    url = file_line_url(pr_url, location.file_path, location.start_line, location.side)
    start, end = location.start_line, location.end_line
    if start is None:
        lines = ""
    elif end is None or end == start:
        lines = f"#L{start}"
    else:
        lines = f"#L{start}-L{end}"
    return f"[{plain(location.file_path)}{lines}]({url})"


def _count(count: int, noun: str, singular_verb: str, plural_verb: str) -> str:
    """``1 more finding is`` or ``3 more findings are``."""
    return f"1 {noun} {singular_verb}" if count == 1 else f"{count} {noun}s {plural_verb}"
