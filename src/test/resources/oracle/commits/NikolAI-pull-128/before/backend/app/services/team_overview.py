"""Build the team panel's ``TeamOverview`` for one repository (US-11.2).

Access is the caller's job: the API runs ``readable_repository`` first. The viewer's course
role picks the layout, and staff-only data goes to the course's TAs and instructors only. Anyone
else who may read the team (a member, or a team access grant) gets the student layout.

Peer reviews, stories, tests and marking aren't stored yet, so those values are null (not 0),
and iterations and badges are empty; the extension shows "n/a" or leaves them out. A PR's
checklist counts are null until NikolAI's code review gives it a checklist, so an unreviewed PR
never shows 0 open items.
"""

# AI contribution: 50%

from collections.abc import Iterable
from datetime import UTC, datetime, timedelta
from typing import Final

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db.models.refactorings import PullRefactoringCount
from app.db.models.team_overview import (
    Badges,
    StoredChecklistCount,
    StoredTeamMember,
    StoredTeamPull,
    TeamMember,
    TeamName,
    TeamOverview,
    TeamPull,
    TeamStaff,
    TeamStats,
)
from app.db.models.users import User
from app.db.repositories.feedback import RegisteredRepository
from app.db.repositories.refactorings import refactoring_counts
from app.db.repositories.team_overview import (
    checklist_counts,
    list_members,
    list_open_pulls,
    pull_stats,
)
from app.services.viewers import team_viewer

STAFF_ROLES: Final = frozenset({"ta", "instructor"})
WEEK: Final = timedelta(days=7)


async def team_overview(
    connection: AsyncConnection[DictRow], repository: RegisteredRepository, user: User
) -> TeamOverview:
    team, viewer = await team_viewer(connection, repository, user)
    counts = {
        count.pull_request_id: count
        for count in await refactoring_counts(connection, repository.id)
    }
    checklists = {
        checklist.pull_request_id: checklist
        for checklist in await checklist_counts(connection, repository.id)
    }
    pulls = await list_open_pulls(connection, repository.id)
    pull_counts = await pull_stats(connection, repository.id)
    members = await list_members(connection, repository.team_id, repository.id)
    return TeamOverview(
        viewer=viewer,
        team=TeamName(name=team.name, course=team.course_name),
        iteration=None,
        next_deliverables=[],
        stats=TeamStats(
            open_checklist_items=_open_items(
                checklists[pull.pull_request_id]
                for pull in pulls
                if pull.pull_request_id in checklists
            ),
            refactorings=sum(count.refactorings for count in counts.values()),
            refactorings_this_week=_new_this_week(counts.values()),
            reviewed_pulls=pull_counts.reviewed_pulls,
            merged_pulls=pull_counts.merged_pulls,
        ),
        pulls=[
            _pull(pull, counts.get(pull.pull_request_id), checklists.get(pull.pull_request_id))
            for pull in pulls
        ],
        members=[_member(member) for member in members],
        badges=Badges(earned=[], next=None),
        staff=_staff() if viewer.role in STAFF_ROLES else None,
    )


def _new_this_week(counts: Iterable[PullRefactoringCount]) -> int:
    """Refactorings new in each PR's latest push, when it was analysed in the last seven days."""
    since = datetime.now(UTC) - WEEK
    return sum(
        count.new_refactorings
        for count in counts
        if count.analysed_at is not None and count.analysed_at >= since
    )


def _open_items(checklists: Iterable[StoredChecklistCount]) -> int | None:
    """Open items across the listed PRs with a checklist, or None when none has one yet.

    Only the PRs the panel lists (``MAX_TEAM_PULLS``) are summed, so the tile matches the rows.
    """
    opens = [checklist.open for checklist in checklists]
    return sum(opens) if opens else None


def _pull(
    pull: StoredTeamPull,
    count: PullRefactoringCount | None,
    checklist: StoredChecklistCount | None,
) -> TeamPull:
    return TeamPull(
        number=pull.number,
        title=pull.title,
        author=pull.author,
        updated_at=pull.updated_at,
        draft=pull.draft,
        checklist_open=checklist.open if checklist is not None else None,
        checklist_total=checklist.total if checklist is not None else None,
        refactorings=count.refactorings if count is not None else 0,
        review_status=pull.review_status or "none",
    )


def _member(member: StoredTeamMember) -> TeamMember:
    return TeamMember(
        login=member.login,
        commits=member.commits,
        pulls=member.pulls,
        reviews=None,
        substantive_reviews=None,
        fixes_with_evidence=None,
        last_commit_at=None,
    )


def _staff() -> TeamStaff:
    """Staff-only data: nothing is stored for marking, stories, tests or disputes yet."""
    return TeamStaff(
        marking=None,
        attention=[],
        disputes=[],
        stories_done=None,
        stories_planned=None,
        features_tested=None,
        features_total=None,
        ai_declared_percent=None,
    )
