"""Roster entries: GitHub logins waiting to be matched to a user (ADR 0001, decision 8)."""

# AI contribution: 50%

from typing import cast
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db.models.courses import CourseRole
from app.db.repositories.courses import add_team_member, set_course_role


async def save_roster_entry(
    connection: AsyncConnection[DictRow],
    *,
    course_id: UUID,
    github_login: str,
    role: CourseRole,
    team_id: UUID | None,
    imported_by: UUID,
) -> None:
    """Store the row, replacing a pending row for the same login in this course."""
    await connection.execute(
        """INSERT INTO course_roster_entries
               (course_id, github_login, course_role, team_id, imported_by_user_id)
           VALUES (%s, %s, %s, %s, %s)
           ON CONFLICT (course_id, lower(github_login)) WHERE claimed_at IS NULL
           DO UPDATE SET github_login = EXCLUDED.github_login,
                         course_role = EXCLUDED.course_role,
                         team_id = EXCLUDED.team_id,
                         imported_by_user_id = EXCLUDED.imported_by_user_id,
                         imported_at = now()""",
        (course_id, github_login, role, team_id, imported_by),
    )


async def claim_roster_entries(
    connection: AsyncConnection[DictRow],
    *,
    user_id: UUID,
    github_login: str,
    course_id: UUID | None = None,
) -> int:
    """Apply every pending row for this login (in one course, or all) to the user.

    Each row gives the user its course role and, if it names a team, a team membership.
    Returns how many rows were claimed.
    """
    async with connection.transaction():
        cursor = await connection.execute(
            """SELECT r.id, r.course_id, r.course_role, r.team_id
               FROM course_roster_entries r JOIN courses c ON c.id = r.course_id
               WHERE lower(r.github_login) = lower(%s) AND r.claimed_at IS NULL
                 AND c.archived_at IS NULL AND (%s::uuid IS NULL OR r.course_id = %s::uuid)
               ORDER BY r.imported_at
               FOR UPDATE OF r""",
            (github_login, course_id, course_id),
        )
        entries = await cursor.fetchall()
        for entry in entries:
            await set_course_role(
                connection, entry["course_id"], user_id, cast(CourseRole, entry["course_role"])
            )
            if entry["team_id"] is not None:
                await add_team_member(connection, entry["team_id"], user_id, "member")
            await connection.execute(
                """UPDATE course_roster_entries SET claimed_by_user_id = %s, claimed_at = now()
                   WHERE id = %s""",
                (user_id, entry["id"]),
            )
    return len(entries)


async def user_for_github_id(
    connection: AsyncConnection[DictRow], github_user_id: int
) -> tuple[UUID, str] | None:
    """The NikolAI user ID and current login of a GitHub account, if it has signed in."""
    cursor = await connection.execute(
        "SELECT id, github_login FROM users WHERE github_user_id = %s", (github_user_id,)
    )
    row = await cursor.fetchone()
    return (cast(UUID, row["id"]), cast(str, row["github_login"])) if row is not None else None
