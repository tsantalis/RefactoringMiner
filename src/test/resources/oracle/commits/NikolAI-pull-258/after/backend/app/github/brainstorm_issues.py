"""Read a repository's open ``brainstorm`` issues from GitHub (US-20.2, #245).

The brainstorm label rule lives here: the webhook handler that attaches brainstorm issues to the
brainstorm epic (``app/services/brainstorm.py``) matches the same label.
"""

# AI contribution: 50%

from typing import Final, NamedTuple
from urllib.parse import quote

from githubkit import GitHub
from githubkit.auth import AppInstallationAuthStrategy
from pydantic import BaseModel, TypeAdapter

BRAINSTORM_LABEL: Final = "brainstorm"
# One page of GitHub's issue list. Past it the dashboard links to GitHub's own label filter
# instead of reading every page on each visit.
MAX_LISTED: Final = 100


class BrainstormIssue(NamedTuple):
    number: int
    title: str
    url: str


class OpenBrainstormIssues(NamedTuple):
    issues: list[BrainstormIssue]
    # GitHub had a full page, so there may be more than were listed.
    more: bool


class _ListedIssue(BaseModel):
    """The fields read from GitHub's issue list (not githubkit's schema, which lags GitHub's)."""

    number: int
    title: str
    html_url: str
    pull_request: object | None = None


_LISTED_ISSUES: Final = TypeAdapter(list[_ListedIssue])


def is_brainstorm_label(name: str) -> bool:
    """Whether ``name`` is the brainstorm label, matched without case."""
    return name.casefold() == BRAINSTORM_LABEL


def all_open_on_github(full_name: str) -> str:
    """GitHub's own list of the repository's open brainstorm issues, for past ``MAX_LISTED``."""
    return f"https://github.com/{full_name}/issues?q=" + quote(
        f"is:issue is:open label:{BRAINSTORM_LABEL}"
    )


async def list_open_brainstorm_issues(
    github: GitHub[AppInstallationAuthStrategy], owner: str, repo: str
) -> OpenBrainstormIssues:
    """The repository's newest open brainstorm issues, up to ``MAX_LISTED``.

    GitHub's ``labels`` filter ignores case, as ``is_brainstorm_label`` does, so an issue
    labelled ``Brainstorm`` is listed too.

    Raises:
        githubkit.exception.GitHubException: GitHub refused or didn't answer.
        pydantic.ValidationError: GitHub's answer lacks the fields listed.
    """
    response = await github.rest.issues.async_list_for_repo(
        owner=owner, repo=repo, state="open", labels=BRAINSTORM_LABEL, per_page=MAX_LISTED
    )
    listed = _LISTED_ISSUES.validate_python(response.json())
    issues = [
        BrainstormIssue(issue.number, issue.title, issue.html_url)
        for issue in listed
        if issue.pull_request is None  # GitHub lists pull requests as issues too.
    ]
    return OpenBrainstormIssues(issues, more=len(listed) >= MAX_LISTED)
