"""Courses, course roles, teams and the single read-access rule (ADR 0001).

The API decides *whether* a signed-in user may act; this module owns the database rules:
join codes, one active role per user per course, team ownership and ``can_read_team``.
Callers own their connections, except inside the functions' own transactions.
"""

# AI contribution: 50%

import re
import secrets
from datetime import datetime
from typing import Final, cast
from uuid import UUID

from psycopg import AsyncConnection, errors
from psycopg.rows import DictRow

from app.db.models.courses import (
    Course,
    CourseRole,
    CourseWithRole,
    MyCourse,
    Team,
    TeamMember,
    TeamRole,
    TeamSummary,
)

JOIN_CODE_ALPHABET: Final = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
JOIN_CODE_LENGTH: Final = 8
COURSE_ROLES: Final = frozenset({"instructor", "ta", "student"})
TEAM_ROLES: Final = frozenset({"member", "lead"})
INSTRUCTOR: Final = "instructor"
_JOIN_CODE = re.compile(rf"^[{JOIN_CODE_ALPHABET}]{{{JOIN_CODE_LENGTH}}}$")
_NAME_LIMIT: Final = 200
_TEAM_NAME_LIMIT: Final = 100


class CourseError(ValueError):
    """A course rule was broken. Messages are written for the signed-in user, never SQL."""


class CourseNotFoundError(CourseError):
    """No active course has that ID."""


class TeamNotFoundError(CourseError):
    """No active team with that ID belongs to the course."""


class JoinCodeError(CourseError):
    """The join code is malformed, unknown or no longer active."""


class MembershipError(CourseError):
    """A team member must be a known, signed-up member of the course."""


class TeamExistsError(CourseError):
    """The course already has an active team with that name."""


def new_join_code() -> str:
    """Eight characters from an alphabet without 0/O/1/I/L (ADR 0001 decision 5)."""
    return "".join(secrets.choice(JOIN_CODE_ALPHABET) for _ in range(JOIN_CODE_LENGTH))


def normalize_join_code(value: str) -> str:
    """Join codes are typed by hand: trim and upper-case before comparing."""
    code = value.strip().upper()
    if not _JOIN_CODE.fullmatch(code):
        raise JoinCodeError("That join code is not valid.")
    return code


def _clean_name(value: str, *, limit: int = _NAME_LIMIT) -> str:
    name = value.strip()
    if not 1 <= len(name) <= limit:
        raise CourseError(f"The name must be 1 to {limit} characters.")
    return name


async def create_course(
    connection: AsyncConnection[DictRow], *, user_id: UUID, name: str
) -> CourseWithRole:
    """Create a course and make its creator the course instructor."""
    clean = _clean_name(name)
    async with connection.transaction():
        row: DictRow | None = None
        for _ in range(5):
            code = new_join_code()
            cursor = await connection.execute(
                """INSERT INTO courses (name, join_code, created_by_user_id)
                   VALUES (%s, %s, %s)
                   ON CONFLICT (join_code) WHERE archived_at IS NULL DO NOTHING
                   RETURNING id, name, join_code, created_at, archived_at""",
                (clean, code, user_id),
            )
            row = await cursor.fetchone()
            if row is not None:
                break
        if row is None:
            raise CourseError("Could not allocate a join code. Try again.")
        await connection.execute(
            """INSERT INTO course_memberships (course_id, user_id, role)
               VALUES (%s, %s, %s)""",
            (row["id"], user_id, INSTRUCTOR),
        )
    return CourseWithRole.model_validate({**row, "role": INSTRUCTOR})


async def get_course(connection: AsyncConnection[DictRow], course_id: UUID) -> Course | None:
    """An active course, or None so the API can answer 404."""
    cursor = await connection.execute(
        """SELECT id, name, join_code, created_at, archived_at FROM courses
           WHERE id = %s AND archived_at IS NULL""",
        (course_id,),
    )
    row = await cursor.fetchone()
    return Course.model_validate(row) if row is not None else None


async def course_role(
    connection: AsyncConnection[DictRow], *, course_id: UUID, user_id: UUID
) -> CourseRole | None:
    """The user's active role in the course, or None when they are not a member."""
    cursor = await connection.execute(
        """SELECT role FROM course_memberships
           WHERE course_id = %s AND user_id = %s AND left_at IS NULL""",
        (course_id, user_id),
    )
    row = await cursor.fetchone()
    return None if row is None else cast(CourseRole, row["role"])


async def list_user_courses(
    connection: AsyncConnection[DictRow], user_id: UUID
) -> list[CourseWithRole]:
    """Active courses the user belongs to, by name."""
    cursor = await connection.execute(
        """SELECT c.id, c.name, c.join_code, c.created_at, c.archived_at, m.role
           FROM course_memberships m JOIN courses c ON c.id = m.course_id
           WHERE m.user_id = %s AND m.left_at IS NULL AND c.archived_at IS NULL
           ORDER BY lower(c.name), c.id""",
        (user_id,),
    )
    return [CourseWithRole.model_validate(row) for row in await cursor.fetchall()]


async def join_course(
    connection: AsyncConnection[DictRow], *, user_id: UUID, join_code: str
) -> CourseWithRole:
    """Join with a code as a student. Joining twice keeps one membership."""
    code = normalize_join_code(join_code)
    async with connection.transaction():
        cursor = await connection.execute(
            """SELECT id, name, join_code, created_at, archived_at FROM courses
               WHERE join_code = %s AND archived_at IS NULL FOR UPDATE""",
            (code,),
        )
        course = await cursor.fetchone()
        if course is None:
            raise JoinCodeError("That join code is not active.")
        cursor = await connection.execute(
            """SELECT role FROM course_memberships
               WHERE course_id = %s AND user_id = %s AND left_at IS NULL""",
            (course["id"], user_id),
        )
        existing = await cursor.fetchone()
        if existing is None:
            await connection.execute(
                """INSERT INTO course_memberships (course_id, user_id, role)
                   VALUES (%s, %s, 'student')""",
                (course["id"], user_id),
            )
            role = "student"
        else:
            role = str(existing["role"])
    return CourseWithRole.model_validate({**course, "role": role})


async def rotate_join_code(connection: AsyncConnection[DictRow], *, course_id: UUID) -> str:
    """Replace an active course's join code; the old one stops working immediately."""
    async with connection.transaction():
        cursor = await connection.execute(
            "SELECT id FROM courses WHERE id = %s AND archived_at IS NULL FOR UPDATE",
            (course_id,),
        )
        if await cursor.fetchone() is None:
            raise CourseNotFoundError("That course does not exist.")
        for _ in range(5):
            code = new_join_code()
            try:
                async with connection.transaction():
                    await connection.execute(
                        "UPDATE courses SET join_code = %s WHERE id = %s", (code, course_id)
                    )
            except errors.UniqueViolation:
                continue
            return code
    raise CourseError("Could not allocate a join code. Try again.")


async def create_team(
    connection: AsyncConnection[DictRow], *, course_id: UUID, name: str
) -> TeamSummary:
    """Create an active team in a course, refusing a duplicate name."""
    clean = _clean_name(name, limit=_TEAM_NAME_LIMIT)
    async with connection.transaction():
        await lock_course(connection, course_id)
        cursor = await connection.execute(
            """SELECT id FROM teams
               WHERE course_id = %s AND lower(name) = lower(%s) AND archived_at IS NULL""",
            (course_id, clean),
        )
        if await cursor.fetchone() is not None:
            raise TeamExistsError("That course already has a team with that name.")
        cursor = await connection.execute(
            "INSERT INTO teams (course_id, name) VALUES (%s, %s) RETURNING id, name",
            (course_id, clean),
        )
        row = await cursor.fetchone()
    if row is None:
        raise RuntimeError("Team insert returned no row")
    return TeamSummary(id=UUID(str(row["id"])), name=str(row["name"]), member_count=0)


async def get_team(
    connection: AsyncConnection[DictRow], *, course_id: UUID, team_id: UUID
) -> Team | None:
    """An active team of this course, or None."""
    cursor = await connection.execute(
        """SELECT id, name, course_id, archived_at FROM teams
           WHERE id = %s AND course_id = %s AND archived_at IS NULL""",
        (team_id, course_id),
    )
    row = await cursor.fetchone()
    return Team.model_validate(row) if row is not None else None


async def list_course_teams(
    connection: AsyncConnection[DictRow], course_id: UUID
) -> list[TeamSummary]:
    """Every active team in a course, with its active member count. Filter with can_read_team."""
    cursor = await connection.execute(
        """SELECT t.id, t.name,
                  count(m.id) FILTER (WHERE m.left_at IS NULL) AS member_count
           FROM teams t LEFT JOIN team_memberships m ON m.team_id = t.id
           WHERE t.course_id = %s AND t.archived_at IS NULL
           GROUP BY t.id, t.name
           ORDER BY lower(t.name), t.id""",
        (course_id,),
    )
    return [TeamSummary.model_validate(row) for row in await cursor.fetchall()]


async def list_team_members(
    connection: AsyncConnection[DictRow], team_id: UUID
) -> list[TeamMember]:
    """The active members of a team, by login. Call ``can_read_team`` before returning this."""
    cursor = await connection.execute(
        """SELECT m.user_id, u.github_login, u.display_name, m.role, m.joined_at
           FROM team_memberships m JOIN users u ON u.id = m.user_id
           WHERE m.team_id = %s AND m.left_at IS NULL
           ORDER BY lower(u.github_login)""",
        (team_id,),
    )
    return [TeamMember.model_validate(row) for row in await cursor.fetchall()]


async def can_read_team(
    connection: AsyncConnection[DictRow], *, user_id: UUID, team_id: UUID
) -> bool:
    """The only read-access rule (ADR 0001 decision 9).

    True for an active team member, an active ``instructor`` or ``ta`` of the team's
    course, or an active ``team_access_grants`` row. No endpoint decides this itself.
    """
    cursor = await connection.execute(
        """SELECT EXISTS (
               SELECT 1 FROM team_memberships m
               WHERE m.team_id = %(team)s AND m.user_id = %(user)s AND m.left_at IS NULL
           ) OR EXISTS (
               SELECT 1 FROM teams t
               JOIN course_memberships c ON c.course_id = t.course_id
               WHERE t.id = %(team)s AND c.user_id = %(user)s AND c.left_at IS NULL
                 AND c.role IN ('instructor', 'ta')
           ) OR EXISTS (
               SELECT 1 FROM team_access_grants g
               WHERE g.team_id = %(team)s AND g.user_id = %(user)s AND g.revoked_at IS NULL
           ) AS allowed""",
        {"team": team_id, "user": user_id},
    )
    row = await cursor.fetchone()
    return bool(row and row["allowed"])


async def is_team_member(
    connection: AsyncConnection[DictRow], *, user_id: UUID, team_id: UUID
) -> bool:
    """Whether the user is an active member of the team (the rule for responding to feedback)."""
    cursor = await connection.execute(
        """SELECT EXISTS (
               SELECT 1 FROM team_memberships
               WHERE team_id = %s AND user_id = %s AND left_at IS NULL
           ) AS member""",
        (team_id, user_id),
    )
    row = await cursor.fetchone()
    return bool(row and row["member"])


async def my_courses(connection: AsyncConnection[DictRow], user_id: UUID) -> list[MyCourse]:
    """The user's courses, each with only the teams ``can_read_team`` allows."""
    courses = await list_user_courses(connection, user_id)
    result: list[MyCourse] = []
    for course in courses:
        teams = [
            team
            for team in await list_course_teams(connection, course.id)
            if await can_read_team(connection, user_id=user_id, team_id=team.id)
        ]
        result.append(
            MyCourse(
                id=course.id,
                name=course.name,
                role=course.role,
                created_at=course.created_at,
                teams=teams,
            )
        )
    return result


async def add_team_member(
    connection: AsyncConnection[DictRow],
    *,
    course_id: UUID,
    team_id: UUID,
    github_login: str,
    role: TeamRole,
) -> TeamMember:
    """Put an existing course member on a team, or change their team role."""
    if role not in TEAM_ROLES:
        raise MembershipError("The team role must be member or lead.")
    login = github_login.strip()
    if not login:
        raise MembershipError("A GitHub login is required.")
    async with connection.transaction():
        await _lock_team(connection, course_id=course_id, team_id=team_id)
        user = await _find_user(connection, login)
        await _require_course_member(connection, course_id=course_id, user_id=user["id"])
        joined_at = await _set_team_role(connection, team_id=team_id, user_id=user["id"], role=role)
    return TeamMember(
        user_id=UUID(str(user["id"])),
        github_login=str(user["github_login"]),
        display_name=user["display_name"],
        role=role,
        joined_at=joined_at,
    )


async def _lock_team(
    connection: AsyncConnection[DictRow], *, course_id: UUID, team_id: UUID
) -> None:
    """Lock an active team of the course, or answer that it is not part of it."""
    cursor = await connection.execute(
        """SELECT id FROM teams
           WHERE id = %s AND course_id = %s AND archived_at IS NULL FOR UPDATE""",
        (team_id, course_id),
    )
    if await cursor.fetchone() is None:
        raise TeamNotFoundError("That team is not part of this course.")


async def _require_course_member(
    connection: AsyncConnection[DictRow], *, course_id: UUID, user_id: UUID
) -> None:
    """A team member must already belong to the course; the roster or join code does that."""
    cursor = await connection.execute(
        """SELECT 1 FROM course_memberships
           WHERE course_id = %s AND user_id = %s AND left_at IS NULL""",
        (course_id, user_id),
    )
    if await cursor.fetchone() is None:
        raise MembershipError("That user has not joined the course yet.")


async def _set_team_role(
    connection: AsyncConnection[DictRow], *, team_id: UUID, user_id: UUID, role: TeamRole
) -> datetime:
    """Insert the team membership or change its role, returning the original join time."""
    cursor = await connection.execute(
        """SELECT id FROM team_memberships
           WHERE team_id = %s AND user_id = %s AND left_at IS NULL FOR UPDATE""",
        (team_id, user_id),
    )
    existing = await cursor.fetchone()
    if existing is None:
        cursor = await connection.execute(
            """INSERT INTO team_memberships (team_id, user_id, role)
               VALUES (%s, %s, %s) RETURNING joined_at""",
            (team_id, user_id, role),
        )
    else:
        cursor = await connection.execute(
            """UPDATE team_memberships SET role = %s WHERE id = %s RETURNING joined_at""",
            (role, existing["id"]),
        )
    joined = await cursor.fetchone()
    if joined is None:
        raise RuntimeError("Team membership returned no row")
    joined_at: datetime = joined["joined_at"]
    return joined_at


async def _find_user(connection: AsyncConnection[DictRow], github_login: str) -> DictRow:
    cursor = await connection.execute(
        """SELECT id, github_login, display_name FROM users
           WHERE lower(github_login) = lower(%s) FOR UPDATE""",
        (github_login,),
    )
    rows = await cursor.fetchall()
    if not rows:
        raise MembershipError("No signed-in user has that GitHub login.")
    if len(rows) > 1:
        raise MembershipError("Several users have that GitHub login. Ask them to sign in again.")
    return rows[0]


async def lock_course(connection: AsyncConnection[DictRow], course_id: UUID) -> None:
    """Serialize team creation and roster import for one course."""
    await connection.execute(
        "SELECT pg_advisory_xact_lock(hashtextextended(%s, 0))",
        (f"course:{course_id}",),
    )
