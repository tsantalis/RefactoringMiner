"""Courses, join codes, course teams and the roster (US-10, ADR 0001 PR 2).

Every endpoint needs a signed-in user; every write also needs an allowed ``Origin``, because
the session cookie is ``SameSite=None`` (decision 3). An unknown course is 404; a course the
user has no say in is 403.
"""

# AI contribution: 50%

import logging
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from psycopg import AsyncConnection
from psycopg.errors import UniqueViolation
from psycopg.rows import DictRow

from app.api.dependencies import current_user, database, require_allowed_origin
from app.core.config import Settings, get_settings
from app.db.models.courses import (
    CourseCreate,
    CourseRole,
    CourseSummary,
    JoinCode,
    JoinRequest,
    RosterImport,
    RosterResult,
    TeamCreate,
    TeamDetail,
    TeamMemberAdd,
    TeamSummary,
)
from app.db.models.users import User
from app.db.repositories import courses as repo
from app.services.access import can_read_team
from app.services.courses import (
    JoinCodeExhaustedError,
    import_roster,
    normalize_join_code,
    open_course,
    replace_join_code,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/courses", tags=["courses"])
WRITE = [Depends(require_allowed_origin)]

SignedIn = Annotated[User, Depends(current_user)]
AppSettings = Annotated[Settings, Depends(get_settings)]


async def _require_role(
    connection: AsyncConnection[DictRow], course_id: UUID, user: User, *roles: CourseRole
) -> CourseRole:
    """The user's role in the course, or 404 (no such course) / 403 (not one of ``roles``)."""
    lookup = await repo.course_lookup(connection, course_id, user.id)
    if not lookup.exists:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Course not found")
    if lookup.role is None or lookup.role not in roles:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "You don't have access to this course")
    return lookup.role


async def _course_view(
    connection: AsyncConnection[DictRow], course_id: UUID, user: User
) -> CourseSummary:
    courses = await repo.list_user_courses(connection, user.id, course_id)
    if not courses:
        raise RuntimeError("A course member's course was not listed")
    return courses[0]


def _no_join_code(error: JoinCodeExhaustedError) -> HTTPException:
    logger.error("Join code generation failed: %s", error)
    return HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, "Try again in a moment")


@router.post("", status_code=status.HTTP_201_CREATED, dependencies=WRITE)
async def create_course(body: CourseCreate, user: SignedIn, settings: AppSettings) -> CourseSummary:
    """Instructors only (decision 4). The response carries the join code to share."""
    if not user.is_instructor:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Only instructors can create courses")
    async with database(settings) as connection:
        try:
            course_id = await open_course(connection, body.name, user.id)
        except JoinCodeExhaustedError as error:
            raise _no_join_code(error) from None
        logger.info("User %s created course %s", user.github_login, course_id)
        return await _course_view(connection, course_id, user)


@router.get("")
async def list_courses(user: SignedIn, settings: AppSettings) -> list[CourseSummary]:
    async with database(settings) as connection:
        return await repo.list_user_courses(connection, user.id)


@router.post("/join", dependencies=WRITE)
async def join_course(body: JoinRequest, user: SignedIn, settings: AppSettings) -> CourseSummary:
    """Join as a student. Joining again changes nothing; a bad or rotated code is 404."""
    async with database(settings) as connection:
        course_id = await repo.find_course_by_join_code(
            connection, normalize_join_code(body.join_code)
        )
        if course_id is None:
            raise HTTPException(status.HTTP_404_NOT_FOUND, "No course has this join code")
        await repo.join_course(connection, course_id, user.id, "student")
        return await _course_view(connection, course_id, user)


@router.get("/{course_id}")
async def get_course(course_id: UUID, user: SignedIn, settings: AppSettings) -> CourseSummary:
    async with database(settings) as connection:
        await _require_role(connection, course_id, user, "instructor", "ta", "student")
        return await _course_view(connection, course_id, user)


@router.post("/{course_id}/join-code", dependencies=WRITE)
async def rotate_join_code(course_id: UUID, user: SignedIn, settings: AppSettings) -> JoinCode:
    """Replace a leaked join code. Members who already joined stay in the course."""
    async with database(settings) as connection:
        await _require_role(connection, course_id, user, "instructor")
        try:
            code = await replace_join_code(connection, course_id)
        except JoinCodeExhaustedError as error:
            raise _no_join_code(error) from None
    logger.info("User %s rotated the join code of course %s", user.github_login, course_id)
    return JoinCode(join_code=code)


@router.post("/{course_id}/teams", status_code=status.HTTP_201_CREATED, dependencies=WRITE)
async def create_team(
    course_id: UUID, body: TeamCreate, user: SignedIn, settings: AppSettings
) -> TeamSummary:
    async with database(settings) as connection:
        await _require_role(connection, course_id, user, "instructor")
        try:
            team_id = await repo.create_team(connection, course_id, body.name)
        except UniqueViolation:
            raise HTTPException(
                status.HTTP_409_CONFLICT, "This course already has a team with that name"
            ) from None
    return TeamSummary(id=team_id, name=body.name)


@router.get("/{course_id}/teams/{team_id}")
async def get_team(
    course_id: UUID, team_id: UUID, user: SignedIn, settings: AppSettings
) -> TeamDetail:
    """A team's members and repositories, for whoever ``can_read_team`` allows."""
    async with database(settings) as connection:
        if not await can_read_team(connection, user.id, team_id):
            raise HTTPException(status.HTTP_403_FORBIDDEN, "You don't have access to this team")
        team = await repo.team_detail(connection, course_id, team_id)
    if team is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Team not found")
    return team


@router.post("/{course_id}/teams/{team_id}/members", dependencies=WRITE)
async def add_team_member(
    course_id: UUID, team_id: UUID, body: TeamMemberAdd, user: SignedIn, settings: AppSettings
) -> TeamDetail:
    """Put a course member in a team (decision 7). They must have signed in and joined."""
    async with database(settings) as connection:
        await _require_role(connection, course_id, user, "instructor")
        if await repo.find_team(connection, course_id, team_id=team_id) is None:
            raise HTTPException(status.HTTP_404_NOT_FOUND, "Team not found")
        member_id = await _course_member_by_login(connection, course_id, body.github_login)
        await repo.add_team_member(connection, team_id, member_id, body.role)
        team = await repo.team_detail(connection, course_id, team_id)
    if team is None:
        raise RuntimeError("A team vanished while adding a member")
    return team


async def _course_member_by_login(
    connection: AsyncConnection[DictRow], course_id: UUID, github_login: str
) -> UUID:
    users = await repo.users_with_login(connection, github_login)
    if not users:
        raise HTTPException(
            status.HTTP_404_NOT_FOUND, "Nobody with that GitHub login has signed in yet"
        )
    if len(users) > 1:
        raise HTTPException(
            status.HTTP_409_CONFLICT, "Several users have that login. Ask them to sign in again."
        )
    if (await repo.course_lookup(connection, course_id, users[0])).role is None:
        raise HTTPException(status.HTTP_409_CONFLICT, "That user hasn't joined this course")
    return users[0]


@router.post("/{course_id}/roster", dependencies=WRITE)
async def import_course_roster(
    course_id: UUID, body: RosterImport, user: SignedIn, settings: AppSettings
) -> RosterResult:
    """Map GitHub logins to a course role and team. Bad rows are reported, never fatal."""
    async with database(settings) as connection:
        await _require_role(connection, course_id, user, "instructor")
        return await import_roster(connection, course_id, body.entries, user.id)
