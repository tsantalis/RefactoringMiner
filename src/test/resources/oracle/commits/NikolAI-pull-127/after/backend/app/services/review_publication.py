"""Write NikolAI's review comment on a PR, editing the one already there (US-9.4).

A PR has one review comment. A new review edits it in place, the way the refactoring report
does, so the checklist a student is working through stays where it is and GitHub doesn't
notify everyone again. Only a comment made through NikolAI's App counts, since anyone can paste
the marker into a comment of their own.
"""

# AI contribution: 50%

import logging

from githubkit import GitHub
from githubkit.auth import AppInstallationAuthStrategy

from app.github.comments import find_app_comment
from app.github.payloads import PullRequestEvent
from app.services.review_comment import COMMENT_MARKER

logger = logging.getLogger(__name__)


async def write_review_comment(
    github: GitHub[AppInstallationAuthStrategy],
    event: PullRequestEvent,
    body: str,
    *,
    app_id: str | None,
) -> tuple[int, str]:
    """Edit NikolAI's review comment on the PR to ``body``, or post it when there is none.

    Returns the comment's id and URL.

    Raises:
        GitHubException: if GitHub refuses or fails a request.
    """
    owner, repo = event.repository.owner.login, event.repository.name
    number = event.pull_request.number
    existing = await find_app_comment(
        github, owner, repo, number, app_id=app_id, marker=COMMENT_MARKER
    )
    if existing is None:
        posted = await github.rest.issues.async_create_comment(
            owner=owner, repo=repo, issue_number=number, body=body
        )
        return posted.parsed_data.id, posted.parsed_data.html_url
    edited = await github.rest.issues.async_update_comment(
        owner=owner, repo=repo, comment_id=existing.id, body=body
    )
    logger.info("Edited the review comment %d on %s#%d", existing.id, repo, number)
    return edited.parsed_data.id, edited.parsed_data.html_url
