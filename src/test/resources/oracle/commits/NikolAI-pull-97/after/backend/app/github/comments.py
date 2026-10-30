"""PR comments NikolAI posts through its GitHub App: size limit and lookup."""

# AI contribution: 50% or more AI-generated

from typing import Final, cast

from githubkit import GitHub
from githubkit.auth import AppInstallationAuthStrategy
from githubkit.utils import Unset
from githubkit_schemas.latest.models import IssueComment

# GitHub rejects comments longer than 65,536 characters.
MAX_COMMENT_CHARS: Final = 60_000
_PAGE_SIZE: Final = 100


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
