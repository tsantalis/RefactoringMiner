"""Courses, course memberships and course teams (ADR 0001, decisions 5 to 7)."""

# AI contribution: 50%

from enum import StrEnum
from typing import Literal, NamedTuple, cast
from uuid import UUID

from psycopg import AsyncConnection, sql
from psycopg.rows import DictRow

from app.db.models.courses import (
    CourseMember,
    CourseRole,
    CourseSummary,
    TeamDetail,
    TeamMember,
    TeamRole,
    TeamSummary,
)
from app.db.team_access_sql import GRANT_APPLIES

STAFF_ROLES = ("instructor", "ta")
COURSE_READERS: tuple[CourseRole, ...] = ("instructor", "stakeholder", "ta", "student")
# Every course member may read the requirement texts; the same four roles open the course itself.
REQUIREMENT_READERS: tuple[CourseRole, ...] = COURSE_READERS
REQUIREMENT_EDITORS: tuple[CourseRole, ...] = ("instructor", "stakeholder")
RequirementField = Literal["course_requirements", "project_requirements"]


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


async def get_requirement_text(
    connection: AsyncConnection[DictRow], course_id: UUID, field: RequirementField
) -> str:
    cursor = await connection.execute(
        sql.SQL("SELECT {field} FROM courses WHERE id = %s").format(field=sql.Identifier(field)),
        (course_id,),
    )
    row = await cursor.fetchone()
    if row is None:
        raise RuntimeError("Course vanished while reading requirements")
    return cast(str, row[field])


async def team_requirement_text(
    connection: AsyncConnection[DictRow], team_id: UUID, field: RequirementField
) -> str:
    """The requirement text of the course ``team_id`` belongs to; empty when it has none.

    A team of no course (or an unknown team) has no requirements to read.
    """
    cursor = await connection.execute(
        sql.SQL(
            "SELECT c.{field} FROM teams t JOIN courses c ON c.id = t.course_id WHERE t.id = %s"
        ).format(field=sql.Identifier(field)),
        (team_id,),
    )
    row = await cursor.fetchone()
    return cast(str, row[field]) if row is not None else ""


async def team_project_requirements(connection: AsyncConnection[DictRow], team_id: UUID) -> str:
    """The project requirements of the team's course; empty for a team in no course."""
    return await team_requirement_text(connection, team_id, "project_requirements")


async def team_course_requirements(connection: AsyncConnection[DictRow], team_id: UUID) -> str:
    """The course requirements of the team's course; empty for a team in no course."""
    return await team_requirement_text(connection, team_id, "course_requirements")


async def replace_requirement_text(
    connection: AsyncConnection[DictRow],
    course_id: UUID,
    field: RequirementField,
    text: str,
) -> str:
    cursor = await connection.execute(
        sql.SQL(
            "UPDATE courses SET {field} = %s, updated_at = now() WHERE id = %s RETURNING {field}"
        ).format(field=sql.Identifier(field)),
        (text, course_id),
    )
    row = await cursor.fetchone()
    if row is None:
        raise RuntimeError("Course vanished while replacing requirements")
    return cast(str, row[field])


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
    """Give the user ``role`` in the course. Instructors and stakeholders are never demoted.

    Only the members API changes a stakeholder's role (ADR 0002, decision 1).
    """
    await join_course(connection, course_id, user_id, role)
    await connection.execute(
        """UPDATE course_memberships SET role = %s
           WHERE course_id = %s AND user_id = %s AND left_at IS NULL
             AND role NOT IN ('instructor', 'stakeholder')""",
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
    """The teams of the course the user may see: every team for staff, the granted teams for a
    stakeholder, the user's own teams otherwise."""
    if not course_ids:
        return {}
    cursor = await connection.execute(
        sql.SQL("""SELECT t.course_id, t.id, t.name
           FROM teams t
           JOIN course_memberships m
             ON m.course_id = t.course_id AND m.user_id = %(user)s AND m.left_at IS NULL
           WHERE t.course_id = ANY(%(courses)s) AND t.archived_at IS NULL
             AND (m.role = ANY(%(staff_roles)s)
                  OR (m.role <> 'stakeholder' AND EXISTS (
                        SELECT 1 FROM team_memberships tm
                        WHERE tm.team_id = t.id AND tm.user_id = %(user)s AND tm.left_at IS NULL))
                  OR (m.role = 'stakeholder' AND {grant}))
           ORDER BY lower(t.name), t.id""").format(grant=GRANT_APPLIES),
        {"user": user_id, "courses": course_ids, "staff_roles": list(STAFF_ROLES)},
    )
    teams: dict[UUID, list[TeamSummary]] = {}
    for row in await cursor.fetchall():
        teams.setdefault(row["course_id"], []).append(TeamSummary(id=row["id"], name=row["name"]))
    return teams


async def create_team(
    connection: AsyncConnection[DictRow], course_id: UUID | None, name: str
) -> UUID:
    """A team of ``course_id``, or of no course when it is None.

    Raises ``psycopg.errors.UniqueViolation`` if that course (or no course) already has a team
    of that name.
    """
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


class PromoteOutcome(StrEnum):
    PROMOTED = "promoted"
    NOT_A_MEMBER = "not_a_member"
    NOT_A_STUDENT = "not_a_student"


class PromoteResult(NamedTuple):
    """The outcome of a promote call, and the member afterwards (None when not in the course)."""

    outcome: PromoteOutcome
    member: CourseMember | None


# A member's team is their active team in this course (the latest one, for memberships made
# before teams were exclusive); None until someone puts them on one (#214).
_MEMBER_SELECT = """SELECT u.id AS user_id, u.github_login, u.display_name, m.role,
              CASE WHEN mt.id IS NULL THEN NULL
                   ELSE json_build_object('id', mt.id, 'name', mt.name) END AS team
       FROM course_memberships m JOIN users u ON u.id = m.user_id
       LEFT JOIN LATERAL (
           SELECT t.id, t.name FROM team_memberships tm JOIN teams t ON t.id = tm.team_id
           WHERE tm.user_id = m.user_id AND tm.left_at IS NULL
             AND t.course_id = m.course_id AND t.archived_at IS NULL
           ORDER BY tm.joined_at DESC, t.id LIMIT 1
       ) mt ON true"""
_MEMBER_BY_ID_SQL = (
    _MEMBER_SELECT + " WHERE m.course_id = %s AND m.user_id = %s AND m.left_at IS NULL"
)
_MEMBERS_OF_COURSE_SQL = (
    _MEMBER_SELECT + " WHERE m.course_id = %s AND m.left_at IS NULL"
    " ORDER BY lower(u.github_login), u.id"
)


async def get_course_member(
    connection: AsyncConnection[DictRow], course_id: UUID, user_id: UUID, *, lock: bool = False
) -> CourseMember | None:
    """The one active member row for (course, user), or None.

    With ``lock``, the membership row stays locked until the transaction ends, so a role change
    or another team move for the same member waits instead of racing this one.
    """
    statement = _MEMBER_BY_ID_SQL + (" FOR UPDATE OF m" if lock else "")
    cursor = await connection.execute(statement, (course_id, user_id))
    row = await cursor.fetchone()
    return CourseMember.model_validate(row) if row is not None else None


async def list_course_members(
    connection: AsyncConnection[DictRow], course_id: UUID
) -> list[CourseMember]:
    """Every active member of the course, ordered by login."""
    cursor = await connection.execute(_MEMBERS_OF_COURSE_SQL, (course_id,))
    return [CourseMember.model_validate(row) for row in await cursor.fetchall()]


async def _promote_student(
    connection: AsyncConnection[DictRow],
    course_id: UUID,
    user_id: UUID,
    role: Literal["ta", "stakeholder"],
) -> PromoteResult:
    """Move a student of the course to ``role``. Any other role is left as it is.

    Making someone a stakeholder also ends their team memberships in this course, in the same
    transaction, so the new role never inherits team access (ADR 0002, decision 2).
    """
    async with connection.transaction():
        cursor = await connection.execute(
            """UPDATE course_memberships SET role = %s
               WHERE course_id = %s AND user_id = %s AND left_at IS NULL AND role = 'student'
               RETURNING user_id""",
            (role, course_id, user_id),
        )
        promoted = await cursor.fetchone() is not None
        if promoted and role == "stakeholder":
            await connection.execute(
                """UPDATE team_memberships tm SET left_at = clock_timestamp()
                   WHERE tm.user_id = %s AND tm.left_at IS NULL
                     AND tm.team_id IN (SELECT t.id FROM teams t WHERE t.course_id = %s)""",
                (user_id, course_id),
            )
    member = await get_course_member(connection, course_id, user_id)
    if member is None:
        return PromoteResult(PromoteOutcome.NOT_A_MEMBER, None)
    outcome = PromoteOutcome.PROMOTED if promoted else PromoteOutcome.NOT_A_STUDENT
    return PromoteResult(outcome, member)


async def promote_to_ta(
    connection: AsyncConnection[DictRow], course_id: UUID, user_id: UUID
) -> PromoteResult:
    """Make a student of the course a TA (ADR 0001, decision 6). Any other role is left as it is."""
    return await _promote_student(connection, course_id, user_id, "ta")


async def promote_to_stakeholder(
    connection: AsyncConnection[DictRow], course_id: UUID, user_id: UUID
) -> PromoteResult:
    """Make a student of the course a stakeholder (ADR 0002, decision 1). Other roles are kept."""
    return await _promote_student(connection, course_id, user_id, "stakeholder")


async def move_to_team(
    connection: AsyncConnection[DictRow],
    course_id: UUID,
    team_id: UUID,
    user_id: UUID,
    role: TeamRole | None = None,
) -> None:
    """Make ``team_id`` the user's only team in this course (the roster and #214 share it).

    A newcomer joins as ``role`` (default ``member``); someone already on the team keeps their
    role unless ``role`` is given. ``clock_timestamp()`` keeps ``left_at`` after ``joined_at``
    even for a membership opened earlier in the same transaction, where ``now()`` would equal it.
    """
    await connection.execute(
        """UPDATE team_memberships SET left_at = clock_timestamp()
           WHERE user_id = %s AND left_at IS NULL AND team_id <> %s
             AND team_id IN (SELECT id FROM teams WHERE course_id = %s)""",
        (user_id, team_id, course_id),
    )
    await connection.execute(
        """INSERT INTO team_memberships (team_id, user_id, role)
           SELECT %s, %s, %s
           WHERE NOT EXISTS (SELECT 1 FROM team_memberships
                             WHERE team_id = %s AND user_id = %s AND left_at IS NULL)""",
        (team_id, user_id, role or "member", team_id, user_id),
    )
    if role is not None:
        await connection.execute(
            """UPDATE team_memberships SET role = %s
               WHERE team_id = %s AND user_id = %s AND left_at IS NULL""",
            (role, team_id, user_id),
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
