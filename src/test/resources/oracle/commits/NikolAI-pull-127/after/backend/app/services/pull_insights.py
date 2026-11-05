"""Build the PR tab's ``PullInsights`` for one PR (US-9).

Access is the caller's job: the API runs ``readable_repository`` first. The checklist is read
with the same rules as the findings of ``GET .../pulls/{n}``, so the two never disagree. An
item is ``fixed`` once a push review recorded the commit, file and lines that fixed it, else
``acknowledged`` or ``disputed`` when the team's latest answer says so, otherwise ``open``.
"""

# AI contribution: 50%

from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db.models.pull_insights import (
    SHORT_SHA,
    ChecklistEntry,
    FixedIn,
    FixLocation,
    PullInsights,
    Push,
    StoredChecklist,
    StoredCodeReview,
    StoredFix,
)
from app.db.models.users import User
from app.db.repositories.feedback import RegisteredRepository, find_pull_request
from app.db.repositories.insights import checklist_review, list_code_reviews
from app.services.pull_requests import checklist_readable
from app.services.viewers import team_viewer


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
            ChecklistEntry(finding_id=finding_id, status=status, fixed_in=_fixed_in(fix))
            for finding_id, status, fix in (
                zip(checklist.finding_ids, checklist.statuses, checklist.fixes, strict=True)
                if checklist
                else []
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


def _fixed_in(fix: StoredFix | None) -> FixedIn | None:
    """The fix as the PR tab shows it, with the commit's short SHA."""
    if fix is None:
        return None
    return FixedIn(
        commit=fix.commit[:SHORT_SHA],
        location=FixLocation(
            file_path=fix.file_path, start_line=fix.start_line, end_line=fix.end_line
        ),
    )


def _pushes(reviews: list[StoredCodeReview], title: str) -> list[Push]:
    """One entry per reviewed commit, newest first, with the findings its checklist added and
    the items it fixed.

    A commit reviewed more than once (a reopened PR, a changed base) is one push, told by its
    newest review. Its findings are added when the checklist of the commit before it didn't
    have the same issue, and fixed when that checklist had them open. Reviews whose code review
    didn't run aren't pushes NikolAI reviewed, and a run with no known head commit can't be
    named, so both are left out. GitHub's ``pull_request`` payload carries no commit message,
    so each push shows the title.
    """
    pushes: dict[str, Push] = {}
    commit, before, current = None, frozenset[str](), frozenset[str]()
    fixed_before, fixed = frozenset[str](), frozenset[str]()
    for review in reviews:
        if review.head_sha is None:
            continue
        if review.head_sha != commit:
            commit, before, fixed_before = review.head_sha, current, fixed
        current, fixed = frozenset(review.finding_keys), frozenset(review.fixed_keys)
        pushes.pop(review.head_sha, None)
        pushes[review.head_sha] = Push(
            commit=review.head_sha[:SHORT_SHA],
            message=title,
            fixed=len(fixed - fixed_before),
            added=len(current - before),
        )
    return list(reversed(pushes.values()))
