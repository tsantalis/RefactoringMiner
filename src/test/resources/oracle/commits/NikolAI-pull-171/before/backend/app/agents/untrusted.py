"""Keep text written by a PR's author, or by their team's workflows, from changing the structure
of NikolAI's prompts."""

# AI contribution: 50%

import re

# Every tag NikolAI's prompts wrap the author's text in, opening or closing, in any case.
_PROMPT_TAGS = re.compile(
    r"<(/?)(pull_request|title|description|diff|file|open_items|item|push|commit"
    r"|backlog_check|project_requirements|backlog|story|static_findings|finding)\b",
    re.IGNORECASE,
)


def neutralize_tags(text: str) -> str:
    """Stop the author's text from opening or closing our tags, a prompt-injection trick."""
    return _PROMPT_TAGS.sub(lambda match: f"&lt;{match[1]}{match[2]}", text)


def description_for_prompt(text: str | None, limit: int) -> str:
    """A PR description ready for a prompt: tags neutralized, a placeholder when empty, and
    cut to ``limit`` characters with a note so a huge description can't fill the context."""
    description = neutralize_tags((text or "").strip())
    if not description:
        return "(no description)"
    if len(description) > limit:
        return description[:limit] + "\n(truncated)"
    return description
