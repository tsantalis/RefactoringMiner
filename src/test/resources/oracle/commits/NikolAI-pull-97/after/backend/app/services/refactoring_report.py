"""Read RefactoringMiner's ``refactorings.json`` and render NikolAI's refactoring report (US-8).

The RefactoringMiner GitHub Action writes the JSON in each team's repository. The file comes
from a workflow the team controls, so it is untrusted: it is validated here, and its Markdown
is escaped and its links checked before it reaches a PR comment.
"""

# AI contribution: 50% or more AI-generated

import re
from collections import Counter
from typing import Final

from pydantic import BaseModel, ConfigDict, Field, ValidationError

from app.github.comments import MAX_COMMENT_CHARS

# First line of every NikolAI refactoring report, so a later run can find and update it.
REPORT_MARKER: Final = "<!-- nikolai:refactorings -->"
# What may follow the PR's URL in a RefactoringMiner link ("/changes?diff=split#diff-…R12").
_SAFE_URL_TAIL: Final = re.compile(r"/[A-Za-z0-9/._~:?#=&%+-]*")
# Characters GitHub would read as emphasis or HTML in a code element's name.
_MARKUP_ESCAPES: Final = str.maketrans({"_": "\\_", "*": "\\*", "<": "&lt;", ">": "&gt;"})
# Plain text has no links or code spans, so their delimiters are escaped too.
_PLAIN_ESCAPES: Final = str.maketrans(
    {"_": "\\_", "*": "\\*", "<": "&lt;", ">": "&gt;", "[": "\\[", "]": "\\]", "`": "\\`"}
)


class RefactoringReportError(ValueError):
    """``refactorings.json`` is not the shape RefactoringMiner writes."""


class Refactoring(BaseModel):
    """One detected refactoring. ``markup`` already links each code element to its diff line."""

    model_config = ConfigDict(frozen=True)

    type: str = Field(min_length=1)
    description: str = ""
    markup: str = ""


class _Export(BaseModel):
    refactorings: list[Refactoring]


def parse_refactorings(raw: bytes) -> list[Refactoring]:
    """The refactorings in RefactoringMiner's export.

    Raises:
        RefactoringReportError: if ``raw`` isn't a RefactoringMiner export.
    """
    try:
        return _Export.model_validate_json(raw).refactorings
    except ValidationError as error:
        raise RefactoringReportError("Unreadable refactorings.json") from error


def render_report(refactorings: list[Refactoring], pr_url: str, head_sha: str) -> str:
    """The PR comment: refactorings grouped by type, most frequent type first.

    ``pr_url`` is the PR's ``html_url``: links that point anywhere else are dropped.
    """
    footer = (
        f"<sub>Detected by RefactoringMiner on commit {head_sha[:7]} · "
        "each link opens the lines in Files changed</sub>"
    )
    head = f"{REPORT_MARKER}\n### NikolAI refactoring report\n\n"
    if not refactorings:
        return f"{head}No refactorings detected in this pull request.\n\n{footer}"
    counts = Counter(r.type for r in refactorings)
    # Counter keeps first-seen order for ties, so the order is stable between runs.
    types = [kind for kind, _ in counts.most_common()]
    breakdown = ", ".join(f"{counts[kind]} {_plain(kind)}" for kind in types)
    head += f"Found {len(refactorings)} refactorings: {breakdown}"
    # Room for the note and the "#### <type> (n)" headings, which are short.
    room = MAX_COMMENT_CHARS - len(head) - len(footer) - 200 * (len(types) + 1)
    sections: list[str] = []
    listed = 0
    for kind in types:
        section = f"#### {_plain(kind)} ({counts[kind]})\n"
        for refactoring in (r for r in refactorings if r.type == kind):
            line = f"\n- {_render_item(refactoring, pr_url)}"
            if len(line) > room:
                break
            room -= len(line)
            section += line
            listed += 1
        sections.append(section)
    body = "\n\n".join(section for section in sections if "\n- " in section)
    hidden = len(refactorings) - listed
    note = (
        f"\n\n_{hidden} more refactorings are not listed, to fit in one comment._" if hidden else ""
    )
    return f"{head}\n\n{body}{note}\n\n{footer}"


def _render_item(refactoring: Refactoring, pr_url: str) -> str:
    if refactoring.markup:
        # Inside its type's section, the leading "**<type>** " would only repeat the heading.
        markup = _one_line(refactoring.markup).removeprefix(f"**{refactoring.type}** ")
        item = _render_markup(markup, pr_url)
    else:
        item = _plain(refactoring.description.removeprefix(f"{refactoring.type}\t"))
    # A leading "#" would turn the bullet into a heading.
    return f"\\{item}" if item.startswith("#") else item


def _render_markup(markup: str, pr_url: str) -> str:
    """Escape RefactoringMiner's markup, keeping its bold markers, code spans and PR links.

    A port of the Action's ``escapeMarkupEmphasis``: one left-to-right scan, so ``]`` or
    ``(`` inside a name such as ``String[] args`` doesn't break the link.
    """
    out: list[str] = []
    i = 0
    while i < len(markup):
        char = markup[i]
        if char == "`" and (end := markup.find("`", i + 1)) != -1:
            out.append(markup[i : end + 1])
            i = end + 1
        elif char == "[" and (link := _match_link(markup, i)) is not None:
            text, url, i = link
            out.append(_render_link(text, url, pr_url))
        elif markup.startswith("**", i):
            out.append("**")
            i += 2
        else:
            out.append(char.translate(_MARKUP_ESCAPES))
            i += 1
    return "".join(out)


def _match_link(markup: str, start: int) -> tuple[str, str, int] | None:
    """The text, URL and end of the ``[text](url)`` starting at ``start``, if there is one."""
    separator = markup.find("](", start + 1)
    end = markup.find(")", separator + 2) if separator != -1 else -1
    if end == -1:
        return None
    return markup[start + 1 : separator], markup[separator + 2 : end], end + 1


def _render_link(text: str, url: str, pr_url: str) -> str:
    escaped = text.translate(_MARKUP_ESCAPES)
    prefix = pr_url.rstrip("/")
    if url.startswith(prefix) and _SAFE_URL_TAIL.fullmatch(url[len(prefix) :]):
        return f"[{escaped}]({url})"
    return escaped


def _plain(text: str) -> str:
    return _one_line(text).translate(_PLAIN_ESCAPES)


def _one_line(text: str) -> str:
    """Collapse line breaks, so untrusted text can't start a new Markdown block."""
    return " ".join(text.split())
