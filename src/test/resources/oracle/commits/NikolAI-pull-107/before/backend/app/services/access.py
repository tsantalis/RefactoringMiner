"""The access rule: who may read a team's data (ADR 0001, decision 9).

``can_read_team`` is the only place this is decided. Endpoints call it before touching any
team data and answer 403 when it says no.
"""

# AI contribution: 50%

from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow


async def can_read_team(connection: AsyncConnection[DictRow], user_id: UUID, team_id: UUID) -> bool:
    """True for an active member of the team (who is still in its course, if it has one),
    an active instructor or TA of the team's course, or an active team access grant.
    """
    cursor = await connection.execute(
        """SELECT EXISTS (
                   SELECT 1 FROM team_memberships tm JOIN teams t ON t.id = tm.team_id
                   WHERE tm.team_id = %(team)s AND tm.user_id = %(user)s AND tm.left_at IS NULL
                     AND (t.course_id IS NULL OR EXISTS (
                           SELECT 1 FROM course_memberships cm
                           WHERE cm.course_id = t.course_id AND cm.user_id = %(user)s
                             AND cm.left_at IS NULL)))
               OR EXISTS (
                   SELECT 1 FROM teams t JOIN course_memberships cm ON cm.course_id = t.course_id
                   WHERE t.id = %(team)s AND cm.user_id = %(user)s AND cm.left_at IS NULL
                     AND cm.role IN ('instructor', 'ta'))
               OR EXISTS (
                   SELECT 1 FROM team_access_grants g
                   WHERE g.team_id = %(team)s AND g.user_id = %(user)s AND g.revoked_at IS NULL)
               AS allowed""",
        {"team": team_id, "user": user_id},
    )
    row = await cursor.fetchone()
    return bool(row and row["allowed"])
