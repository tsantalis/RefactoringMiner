"""Courses, course memberships and course teams (ADR 0001, decisions 5 to 7)."""

# AI contribution: 50%

from typing import NamedTuple, cast
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db.models.courses import (
    CourseRole,
    CourseSummary,
    TeamDetail,
    TeamMember,
    TeamRole,
    TeamSummary,
)

STAFF_ROLES = ("instructor", "ta")


class CourseLookup(NamedTuple):
    """Whether a course exists, and the user's active role in it (None when not a member)."""

    exists: bool
    role: CourseRole | None


async def create_course(
    connection: AsyncConnection[DictRow], *, name: str, join_code: str, creator_id: UUID
) -> UUID:
    """Create the course and make its creator an instructor of it, both or neither.

    Raises:
        psycopg.errors.UniqueViolation: if an active course already uses ``join_code``.
    """
    async with connection.transaction():
        cursor = await connection.execute(
            """INSERT INTO courses (name, join_code, created_by_user_id)
               VALUES (%s, %s, %s) RETURNING id""",
            (name, join_code, creator_id),
        )
        row = await cursor.fetchone()
        if row is None:
            raise RuntimeError("Course insert returned no ID")
        course_id = cast(UUID, row["id"])
        await set_course_role(connection, course_id, creator_id, "instructor")
    return course_id


async def course_lookup(
    connection: AsyncConnection[DictRow], course_id: UUID, user_id: UUID
) -> CourseLookup:
    cursor = await connection.execute(
        """SELECT m.role FROM courses c
           LEFT JOIN course_memberships m
             ON m.course_id = c.id AND m.user_id = %s AND m.left_at IS NULL
           WHERE c.id = %s AND c.archived_at IS NULL""",
        (user_id, course_id),
    )
    row = await cursor.fetchone()
    if row is None:
        return CourseLookup(exists=False, role=None)
    return CourseLookup(exists=True, role=cast(CourseRole | None, row["role"]))


async def find_course_by_join_code(
    connection: AsyncConnection[DictRow], join_code: str
) -> UUID | None:
    cursor = await connection.execute(
        "SELECT id FROM courses WHERE join_code = %s AND archived_at IS NULL", (join_code,)
    )
    row = await cursor.fetchone()
    return cast(UUID, row["id"]) if row is not None else None


async def rotate_join_code(
    connection: AsyncConnection[DictRow], course_id: UUID, join_code: str
) -> None:
    """Replace the course's join code; the old one stops working at once.

    Raises:
        psycopg.errors.UniqueViolation: if another active course already uses ``join_code``.
    """
    await connection.execute(
        "UPDATE courses SET join_code = %s, updated_at = now() WHERE id = %s",
        (join_code, course_id),
    )


async def join_course(
    connection: AsyncConnection[DictRow], course_id: UUID, user_id: UUID, role: CourseRole
) -> CourseRole:
    """Add the user with ``role`` unless already a member; return their active role."""
    await connection.execute(
        """INSERT INTO course_memberships (course_id, user_id, role) VALUES (%s, %s, %s)
           ON CONFLICT (course_id, user_id) WHERE left_at IS NULL DO NOTHING""",
        (course_id, user_id, role),
    )
    lookup = await course_lookup(connection, course_id, user_id)
    if lookup.role is None:
        raise RuntimeError("Course membership was not saved")
    return lookup.role


async def set_course_role(
    connection: AsyncConnection[DictRow], course_id: UUID, user_id: UUID, role: CourseRole
) -> None:
    """Give the user ``role`` in the course. An instructor is never demoted this way."""
    await join_course(connection, course_id, user_id, role)
    await connection.execute(
        """UPDATE course_memberships SET role = %s
           WHERE course_id = %s AND user_id = %s AND left_at IS NULL AND role <> 'instructor'""",
        (role, course_id, user_id),
    )


async def list_user_courses(
    connection: AsyncConnection[DictRow], user_id: UUID, course_id: UUID | None = None
) -> list[CourseSummary]:
    """The user's active courses (or just ``course_id``) with the teams they may see in each.

    Staff see every team of the course; a student sees the teams they belong to.
    """
    cursor = await connection.execute(
        """SELECT c.id, c.name, m.role,
                  CASE WHEN m.role = 'instructor' THEN c.join_code END AS join_code
           FROM course_memberships m JOIN courses c ON c.id = m.course_id
           WHERE m.user_id = %s AND m.left_at IS NULL AND c.archived_at IS NULL
             AND (%s::uuid IS NULL OR c.id = %s::uuid)
           ORDER BY c.created_at, c.id""",
        (user_id, course_id, course_id),
    )
    courses = await cursor.fetchall()
    teams = await _visible_teams(connection, user_id, [row["id"] for row in courses])
    return [
        CourseSummary(
            id=row["id"],
            name=row["name"],
            role=row["role"],
            join_code=row["join_code"],
            teams=teams.get(row["id"], []),
        )
        for row in courses
    ]


async def _visible_teams(
    connection: AsyncConnection[DictRow], user_id: UUID, course_ids: list[UUID]
) -> dict[UUID, list[TeamSummary]]:
    if not course_ids:
        return {}
    cursor = await connection.execute(
        """SELECT t.course_id, t.id, t.name
           FROM teams t
           JOIN course_memberships m
             ON m.course_id = t.course_id AND m.user_id = %s AND m.left_at IS NULL
           WHERE t.course_id = ANY(%s) AND t.archived_at IS NULL
             AND (m.role = ANY(%s) OR EXISTS (
                   SELECT 1 FROM team_memberships tm
                   WHERE tm.team_id = t.id AND tm.user_id = %s AND tm.left_at IS NULL))
           ORDER BY lower(t.name), t.id""",
        (user_id, course_ids, list(STAFF_ROLES), user_id),
    )
    teams: dict[UUID, list[TeamSummary]] = {}
    for row in await cursor.fetchall():
        teams.setdefault(row["course_id"], []).append(TeamSummary(id=row["id"], name=row["name"]))
    return teams


async def create_team(connection: AsyncConnection[DictRow], course_id: UUID, name: str) -> UUID:
    """Raises ``psycopg.errors.UniqueViolation`` if the course already has a team of that name."""
    cursor = await connection.execute(
        "INSERT INTO teams (name, course_id) VALUES (%s, %s) RETURNING id", (name, course_id)
    )
    row = await cursor.fetchone()
    if row is None:
        raise RuntimeError("Team insert returned no ID")
    return cast(UUID, row["id"])


async def find_team(connection: AsyncConnection[DictRow], course_id: UUID, team_id: UUID) -> bool:
    """Whether ``team_id`` is an active team of this course."""
    cursor = await connection.execute(
        "SELECT 1 FROM teams WHERE id = %s AND course_id = %s AND archived_at IS NULL",
        (team_id, course_id),
    )
    return await cursor.fetchone() is not None


async def team_ids_by_name(
    connection: AsyncConnection[DictRow], course_id: UUID
) -> dict[str, UUID]:
    """The course's active teams, keyed by lowercased name."""
    cursor = await connection.execute(
        "SELECT id, lower(name) AS name FROM teams WHERE course_id = %s AND archived_at IS NULL",
        (course_id,),
    )
    return {row["name"]: row["id"] for row in await cursor.fetchall()}


async def users_by_logins(
    connection: AsyncConnection[DictRow], github_logins: list[str]
) -> dict[str, list[UUID]]:
    """User IDs for each lowercased login. A login can map to several users (renames)."""
    cursor = await connection.execute(
        "SELECT id, lower(github_login) AS login FROM users WHERE lower(github_login) = ANY(%s)",
        ([login.lower() for login in github_logins],),
    )
    users: dict[str, list[UUID]] = {}
    for row in await cursor.fetchall():
        users.setdefault(row["login"], []).append(row["id"])
    return users


async def add_team_member(
    connection: AsyncConnection[DictRow], team_id: UUID, user_id: UUID, role: TeamRole
) -> None:
    """Add the user to the team, or set their role if they're already in it."""
    cursor = await connection.execute(
        """UPDATE team_memberships SET role = %s
           WHERE team_id = %s AND user_id = %s AND left_at IS NULL""",
        (role, team_id, user_id),
    )
    if cursor.rowcount == 0:
        # ON CONFLICT DO NOTHING also covers the no-overlap exclusion constraint on a race.
        await connection.execute(
            """INSERT INTO team_memberships (team_id, user_id, role) VALUES (%s, %s, %s)
               ON CONFLICT DO NOTHING""",
            (team_id, user_id, role),
        )


async def move_to_team(
    connection: AsyncConnection[DictRow], course_id: UUID, team_id: UUID, user_id: UUID
) -> None:
    """Make ``team_id`` the user's only team in this course, keeping their role if already in it.

    ``clock_timestamp()`` keeps ``left_at`` after ``joined_at`` even for a membership opened
    earlier in the same transaction, where ``now()`` would equal it.
    """
    await connection.execute(
        """UPDATE team_memberships SET left_at = clock_timestamp()
           WHERE user_id = %s AND left_at IS NULL AND team_id <> %s
             AND team_id IN (SELECT id FROM teams WHERE course_id = %s)""",
        (user_id, team_id, course_id),
    )
    await connection.execute(
        """INSERT INTO team_memberships (team_id, user_id, role)
           SELECT %s, %s, 'member'
           WHERE NOT EXISTS (SELECT 1 FROM team_memberships
                             WHERE team_id = %s AND user_id = %s AND left_at IS NULL)""",
        (team_id, user_id, team_id, user_id),
    )


async def team_detail(
    connection: AsyncConnection[DictRow], course_id: UUID, team_id: UUID
) -> TeamDetail | None:
    cursor = await connection.execute(
        """SELECT id, name, course_id FROM teams
           WHERE id = %s AND course_id = %s AND archived_at IS NULL""",
        (team_id, course_id),
    )
    team = await cursor.fetchone()
    if team is None:
        return None
    cursor = await connection.execute(
        """SELECT u.id AS user_id, u.github_login, u.display_name, tm.role
           FROM team_memberships tm JOIN users u ON u.id = tm.user_id
           WHERE tm.team_id = %s AND tm.left_at IS NULL
           ORDER BY lower(u.github_login), u.id""",
        (team_id,),
    )
    members = [TeamMember.model_validate(row) for row in await cursor.fetchall()]
    cursor = await connection.execute(
        """SELECT full_name FROM repositories
           WHERE team_id = %s AND archived_at IS NULL ORDER BY lower(full_name)""",
        (team_id,),
    )
    repositories = [cast(str, row["full_name"]) for row in await cursor.fetchall()]
    return TeamDetail(
        id=team["id"],
        name=team["name"],
        course_id=team["course_id"],
        members=members,
        repositories=repositories,
    )
