"""Build the PR tab's ``PullInsights`` for one PR (US-9).

Access is the caller's job: the API runs ``readable_repository`` first. The checklist is read
with the same rules as the findings of ``GET .../pulls/{n}``, so the two never disagree. An
item is ``acknowledged`` or ``disputed`` when the team's latest answer says so, otherwise
``open``; NikolAI doesn't tick fixed items yet, so ``fixed`` counts stay 0.
"""

# AI contribution: 50%

from typing import Final
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db.models.pull_insights import (
    ChecklistEntry,
    PullInsights,
    Push,
    StoredChecklist,
    StoredCodeReview,
)
from app.db.models.users import User
from app.db.repositories.feedback import RegisteredRepository, find_pull_request
from app.db.repositories.insights import checklist_review, list_code_reviews
from app.services.pull_requests import checklist_readable
from app.services.viewers import team_viewer

SHORT_SHA: Final = 7


async def pull_insights(
    connection: AsyncConnection[DictRow],
    repository: RegisteredRepository,
    number: int,
    user: User,
) -> PullInsights | None:
    """The PR's checklist status, withheld count and reviewed pushes, or None for an unknown PR."""
    pr = await find_pull_request(connection, repository.id, number)
    if pr is None:
        return None
    _, viewer = await team_viewer(connection, repository, user)
    checklist = await _checklist(connection, pr.id)
    return PullInsights(
        viewer=viewer,
        checklist=[
            ChecklistEntry(finding_id=finding_id, status=status, fixed_in=None)
            for finding_id, status in (
                zip(checklist.finding_ids, checklist.statuses, strict=True) if checklist else []
            )
        ],
        withheld_count=(checklist.withheld_count or 0) if checklist else 0,
        your_review=None,
        pushes=_pushes(await list_code_reviews(connection, pr.id), pr.title),
        marking=None,
    )


async def _checklist(
    connection: AsyncConnection[DictRow], pull_request_id: UUID
) -> StoredChecklist | None:
    """The checklist review, or None when there is none or it is hidden with its review."""
    if not await checklist_readable(connection, pull_request_id):
        return None
    return await checklist_review(connection, pull_request_id)


def _pushes(reviews: list[StoredCodeReview], title: str) -> list[Push]:
    """One entry per reviewed commit, newest first, with the findings its checklist added.

    A commit reviewed more than once (a reopened PR, a changed base) is one push, told by its
    newest review. Its findings are added when the checklist of the commit before it didn't
    have the same issue. Reviews whose code review didn't run aren't pushes NikolAI reviewed,
    and a run with no known head commit can't be named, so both are left out. GitHub's
    ``pull_request`` payload carries no commit message, so each push shows the title.
    """
    pushes: dict[str, Push] = {}
    commit, before, current = None, frozenset[str](), frozenset[str]()
    for review in reviews:
        if review.head_sha is None:
            continue
        if review.head_sha != commit:
            commit, before = review.head_sha, current
        current = frozenset(review.finding_keys)
        pushes.pop(review.head_sha, None)
        pushes[review.head_sha] = Push(
            commit=review.head_sha[:SHORT_SHA], message=title, fixed=0, added=len(current - before)
        )
    return list(reversed(pushes.values()))
