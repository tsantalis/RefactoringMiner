"""PR comments NikolAI posts through its GitHub App: size limit, lookup, the links to lines in
Files changed, and the link each comment ends with to the PR's NikolAI tab."""

# AI contribution: 50%

import hashlib
from typing import Final, Literal, cast

from githubkit import GitHub
from githubkit.auth import AppInstallationAuthStrategy
from githubkit.utils import Unset
from githubkit_schemas.latest.models import IssueComment

# GitHub rejects comments longer than 65,536 characters.
MAX_COMMENT_CHARS: Final = 60_000
_PAGE_SIZE: Final = 100

# The views a NikolAI comment link opens: ``checklist``, ``ai-review`` and ``refactorings``
# are the sections of the extension's PR tab (ids must match `SECTIONS` in
# frontend/apps/extension/src/views/pull/PullView.tsx); ``story-review`` is the inline
# Story card on an issue page, which issues have no tab for.
type TabView = Literal["checklist", "ai-review", "refactorings", "story-review"]
# The two sides of a diff, as finding locations store them: the PR's base, and its head.
type DiffSide = Literal["parent", "child"]


def pull_request_url(repository: str, number: int) -> str:
    """The PR's page on GitHub, from its ``owner/name`` repository and number."""
    return f"https://github.com/{repository}/pull/{number}"


def issue_url(repository: str, number: int) -> str:
    """The issue's page on GitHub, from its ``owner/name`` repository and number."""
    return f"https://github.com/{repository}/issues/{number}"


def file_line_url(pr_url: str, path: str, line: int | None, side: DiffSide) -> str:
    """A link to ``line`` of ``path`` in the Files changed tab of the PR at ``pr_url``.

    GitHub names each file's anchor ``diff-<sha256 of the path>``, followed by ``R<line>`` on
    the new side (``child``) or ``L<line>`` on the old side (``parent``); without a line, the
    link opens the file. The extension builds the same link (``fileLineUrl`` in
    ``frontend/apps/extension/src/shared/github-url.ts``). The path only reaches the URL as its
    hash, so an unusual file name can't change where the link goes.
    """
    return f"{pr_url.rstrip('/')}/files{_file_anchor(path, line, side)}"


def commit_url(pr_url: str, sha: str) -> str:
    """A link to commit ``sha`` in the PR at ``pr_url``."""
    return f"{pr_url.rstrip('/')}/commits/{sha}"


def commit_line_url(pr_url: str, sha: str, path: str, line: int) -> str:
    """A link to ``line`` of ``path`` in commit ``sha`` of the PR at ``pr_url``.

    The PR's commit view names its anchors the same way as Files changed (``file_line_url``),
    numbered as the commit left the file.
    """
    return f"{commit_url(pr_url, sha)}{_file_anchor(path, line, 'child')}"


def _file_anchor(path: str, line: int | None, side: DiffSide) -> str:
    digest = hashlib.sha256(path.encode()).hexdigest()
    anchor = "" if line is None else f"{'R' if side == 'child' else 'L'}{line}"
    return f"#diff-{digest}{anchor}"


def nikolai_tab_link(page_url: str, view: TabView) -> str:
    """A Markdown link that opens ``view`` where the extension shows it for ``page_url``.

    The extension opens a view from a page's URL ending in ``#nikolai/<view>``
    (``nikolaiRoute`` in ``frontend/apps/extension/src/shared/page.ts``).
    """
    return f"[Open in the NikolAI tab]({page_url.rstrip('/')}#nikolai/{view})"


async def find_app_comment(
    github: GitHub[AppInstallationAuthStrategy],
    owner: str,
    repo: str,
    issue_number: int,
    *,
    app_id: str | None,
    marker: str,
) -> IssueComment | None:
    """The first comment on the PR that contains ``marker`` and was made by App ``app_id``.

    Anyone can paste a marker into a comment, so only comments made through our App count.

    Raises:
        GitHubException: if GitHub refuses or fails a request.
    """
    page = 1
    while True:
        response = await github.rest.issues.async_list_comments(
            owner=owner, repo=repo, issue_number=issue_number, per_page=_PAGE_SIZE, page=page
        )
        # githubkit's `list` method hides the built-in in its annotations; restate the type.
        comments = cast("list[IssueComment]", response.parsed_data)
        for comment in comments:
            app = comment.performed_via_github_app
            if (
                marker in (comment.body or "")
                and app is not None
                and not isinstance(app, Unset)
                and str(app.id) == app_id
            ):
                return comment
        if len(comments) < _PAGE_SIZE:
            return None
        page += 1
