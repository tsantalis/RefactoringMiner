"""The access rule: who may read a team's data (ADR 0001, decision 9), and who may answer for it.

``can_read_team`` is the only place reading is decided. Endpoints call it before touching any
team data and answer 403 when it says no (404 where the ID alone must not reveal anything, as
for a checklist finding). ``can_answer_for_team`` is its member branch narrowed to the course's
students, so it implies ``can_read_team``.
"""

# AI contribution: 50%

from typing import Final
from uuid import UUID

from psycopg import AsyncConnection, sql
from psycopg.rows import DictRow

from app.db.team_access_sql import GRANT_APPLIES

# The stakeholder role is refused everywhere a team read can be reached from: here, in
# ``_promote_student`` (memberships end in the same transaction), in ``add_team_member`` (409),
# and in ``claim_roster_entries`` (a stale row never places one on a team).
# An active member of team ``t``, still in its course if it has one. With ``students_only`` the
# course membership must also be a student's. Both rules share it, so they can't drift apart.
# A stakeholder never reads a team through membership; only a grant lets them
# (ADR 0002, decision 2).
_TEAM_MEMBER: Final = sql.SQL("""EXISTS (
        SELECT 1 FROM team_memberships tm
        WHERE tm.team_id = t.id AND tm.user_id = %(user)s AND tm.left_at IS NULL
          AND (t.course_id IS NULL OR EXISTS (
                SELECT 1 FROM course_memberships cm
                WHERE cm.course_id = t.course_id AND cm.user_id = %(user)s
                  AND cm.left_at IS NULL AND cm.role <> 'stakeholder'
                  AND (NOT %(students_only)s OR cm.role = 'student'))))""")

# Course staff, or a grant that applies (the shared fragment). The stakeholder rule is why the
# grant outlives the grant call; the outsider case is ADR 0001, decision 9.
_COURSE_STAFF_OR_GRANT: Final = sql.SQL("""EXISTS (
        SELECT 1 FROM course_memberships cm
        WHERE cm.course_id = t.course_id AND cm.user_id = %(user)s
          AND cm.left_at IS NULL AND cm.role IN ('instructor', 'ta'))
    OR {grant}""").format(grant=GRANT_APPLIES)


async def _active_team_where(
    connection: AsyncConnection[DictRow],
    condition: sql.Composable,
    params: dict[str, object],
) -> bool:
    """True when team ``%(team)s`` is active, not in an archived course, and meets ``condition``."""
    query = sql.SQL("""SELECT EXISTS (
            SELECT 1 FROM teams t LEFT JOIN courses c ON c.id = t.course_id
            WHERE t.id = %(team)s AND t.archived_at IS NULL AND c.archived_at IS NULL
              AND ({condition})) AS allowed""").format(condition=condition)
    cursor = await connection.execute(query, params)
    row = await cursor.fetchone()
    return bool(row and row["allowed"])


async def can_read_team(connection: AsyncConnection[DictRow], user_id: UUID, team_id: UUID) -> bool:
    """True for an active team that isn't in an archived course, when the user is

    - an active member of the team (still in its course, if it has one; a stakeholder never counts),
    - an active instructor or TA of the team's course, or
    - holding an active team access grant that applies (a non-member, or the course's stakeholder).
    """
    condition = sql.SQL("{member} OR {staff}").format(
        member=_TEAM_MEMBER, staff=_COURSE_STAFF_OR_GRANT
    )
    return await _active_team_where(
        connection, condition, {"team": team_id, "user": user_id, "students_only": False}
    )


async def can_answer_for_team(
    connection: AsyncConnection[DictRow], user_id: UUID, team_id: UUID
) -> bool:
    """True when the user may act as the team, like answering its checklist: ``can_read_team``'s
    member branch, with an active student role when the team is in a course.

    Course staff and access-grant holders read the team but don't answer for it, even when a
    roster or an instructor put them on the team.
    """
    return await _active_team_where(
        connection, _TEAM_MEMBER, {"team": team_id, "user": user_id, "students_only": True}
    )
