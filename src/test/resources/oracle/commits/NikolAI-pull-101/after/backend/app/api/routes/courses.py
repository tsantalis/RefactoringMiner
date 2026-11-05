"""Courses, teams and the roster API (US-10, ADR 0001 PR 2).

Every route needs a session (401 without one). Authorization is decided here, before any
team data is read: ``can_read_team`` for reads and a course role for instructor actions.
"""

# AI contribution: 50%

from typing import Annotated, Literal
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from psycopg import AsyncConnection
from psycopg.rows import DictRow
from pydantic import BaseModel, Field

from app.api.dependencies import current_user, database, require_allowed_origin
from app.core.config import Settings, get_settings
from app.db.models.courses import (
    Course,
    CourseDetail,
    CourseSummary,
    JoinCodeResponse,
    RosterImportResult,
    TeamDetail,
    TeamMember,
    TeamSummary,
)
from app.db.models.users import User
from app.db.repositories.courses import (
    CourseError,
    CourseNotFoundError,
    TeamExistsError,
    TeamNotFoundError,
    add_team_member,
    can_read_team,
    course_role,
    create_course,
    create_team,
    get_course,
    get_team,
    join_course,
    list_course_teams,
    list_team_members,
    list_user_courses,
    rotate_join_code,
)
from app.db.repositories.roster import (
    MAX_ROSTER_ROWS,
    RosterRow,
    claim_roster_entries,
    import_roster,
)

router = APIRouter(tags=["courses"])

# Cookie-authenticated writes need an Origin from ALLOWED_ORIGINS (ADR 0001 decision 3).
WRITE_DEPENDENCIES = [Depends(require_allowed_origin)]
INSTRUCTORS = frozenset({"instructor"})


class CreateCourseRequest(BaseModel):
    """The name of the course to create."""

    name: str = Field(min_length=1, max_length=200)


class JoinCourseRequest(BaseModel):
    """A join code shared by an instructor."""

    join_code: str = Field(min_length=1, max_length=32)


class CreateTeamRequest(BaseModel):
    """The name of a team to create in a course."""

    name: str = Field(min_length=1, max_length=100)


class AddTeamMemberRequest(BaseModel):
    """An existing course member to add to a team."""

    github_login: str = Field(min_length=1, max_length=39)
    role: Literal["member", "lead"] = "member"


class RosterEntryRequest(BaseModel):
    """One roster row, validated per row so one mistake does not reject the import."""

    github_login: str = Field(default="", max_length=100)
    role: str = Field(default="", max_length=20)
    team: str | None = Field(default=None, max_length=100)


class ImportRosterRequest(BaseModel):
    """The roster rows to import."""

    entries: list[RosterEntryRequest] = Field(max_length=MAX_ROSTER_ROWS)


def _http_error(error: CourseError) -> HTTPException:
    """Map a repository rule to the HTTP answer, without exposing internals."""
    if isinstance(error, CourseNotFoundError | TeamNotFoundError):
        return HTTPException(status.HTTP_404_NOT_FOUND, str(error))
    if isinstance(error, TeamExistsError):
        return HTTPException(status.HTTP_409_CONFLICT, str(error))
    # JoinCodeError, MembershipError and any other rule break the request itself.
    return HTTPException(status.HTTP_400_BAD_REQUEST, str(error))


async def _active_course(connection: AsyncConnection[DictRow], course_id: UUID) -> Course:
    """Load an active course or answer 404."""
    course = await get_course(connection, course_id)
    if course is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "That course does not exist.")
    return course


async def _require_instructor(
    connection: AsyncConnection[DictRow], *, course_id: UUID, user_id: UUID
) -> None:
    """Only a course instructor may change its teams, roster or join code."""
    role = await course_role(connection, course_id=course_id, user_id=user_id)
    if role not in INSTRUCTORS:
        raise HTTPException(
            status.HTTP_403_FORBIDDEN, "Only this course's instructors can do that."
        )


@router.post("/api/courses", status_code=status.HTTP_201_CREATED, dependencies=WRITE_DEPENDENCIES)
async def create_course_route(
    body: CreateCourseRequest,
    user: Annotated[User, Depends(current_user)],
    settings: Annotated[Settings, Depends(get_settings)],
) -> CourseDetail:
    """Create a course. Only a granted instructor may do this (decision 4)."""
    if not user.is_instructor:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Only instructors can create courses.")
    async with database(settings) as connection:
        try:
            course = await create_course(connection, user_id=user.id, name=body.name)
        except CourseError as error:
            raise _http_error(error) from None
    return CourseDetail(
        id=course.id,
        name=course.name,
        role=course.role,
        created_at=course.created_at,
        join_code=course.join_code,
        teams=[],
    )


@router.get("/api/courses")
async def list_courses_route(
    user: Annotated[User, Depends(current_user)],
    settings: Annotated[Settings, Depends(get_settings)],
) -> list[CourseSummary]:
    """The signed-in user's active courses and their role in each."""
    async with database(settings) as connection:
        courses = await list_user_courses(connection, user.id)
    return [
        CourseSummary(
            id=course.id, name=course.name, role=course.role, created_at=course.created_at
        )
        for course in courses
    ]


@router.get("/api/courses/{course_id}")
async def get_course_route(
    course_id: UUID,
    user: Annotated[User, Depends(current_user)],
    settings: Annotated[Settings, Depends(get_settings)],
) -> CourseDetail:
    """One course, its join code (instructors only) and the teams the user may read."""
    async with database(settings) as connection:
        course = await _active_course(connection, course_id)
        role = await course_role(connection, course_id=course.id, user_id=user.id)
        if role is None:
            raise HTTPException(status.HTTP_403_FORBIDDEN, "You are not a member of this course.")
        teams = [
            team
            for team in await list_course_teams(connection, course.id)
            if await can_read_team(connection, user_id=user.id, team_id=team.id)
        ]
    return CourseDetail(
        id=course.id,
        name=course.name,
        role=role,
        created_at=course.created_at,
        join_code=course.join_code if role == "instructor" else None,
        teams=teams,
    )


@router.post("/api/courses/join", dependencies=WRITE_DEPENDENCIES)
async def join_course_route(
    body: JoinCourseRequest,
    user: Annotated[User, Depends(current_user)],
    settings: Annotated[Settings, Depends(get_settings)],
) -> CourseSummary:
    """Join with a code. A pending roster row decides the real role and team."""
    async with database(settings) as connection:
        try:
            course = await join_course(connection, user_id=user.id, join_code=body.join_code)
        except CourseError as error:
            raise _http_error(error) from None
        await claim_roster_entries(connection, user_id=user.id, github_login=user.github_login)
        role = await course_role(connection, course_id=course.id, user_id=user.id)
    return CourseSummary(
        id=course.id,
        name=course.name,
        role=role or course.role,
        created_at=course.created_at,
    )


@router.post("/api/courses/{course_id}/join-code", dependencies=WRITE_DEPENDENCIES)
async def rotate_join_code_route(
    course_id: UUID,
    user: Annotated[User, Depends(current_user)],
    settings: Annotated[Settings, Depends(get_settings)],
) -> JoinCodeResponse:
    """Replace a leaked join code; the old code stops working immediately."""
    async with database(settings) as connection:
        course = await _active_course(connection, course_id)
        await _require_instructor(connection, course_id=course.id, user_id=user.id)
        try:
            code = await rotate_join_code(connection, course_id=course.id)
        except CourseError as error:
            raise _http_error(error) from None
    return JoinCodeResponse(join_code=code)


@router.get("/api/courses/{course_id}/teams/{team_id}")
async def get_team_route(
    course_id: UUID,
    team_id: UUID,
    user: Annotated[User, Depends(current_user)],
    settings: Annotated[Settings, Depends(get_settings)],
) -> TeamDetail:
    """One team's members, only to those ``can_read_team`` allows (decision 9)."""
    async with database(settings) as connection:
        course = await _active_course(connection, course_id)
        team = await get_team(connection, course_id=course.id, team_id=team_id)
        if team is None:
            raise HTTPException(status.HTTP_404_NOT_FOUND, "That team is not part of this course.")
        if not await can_read_team(connection, user_id=user.id, team_id=team.id):
            raise HTTPException(status.HTTP_403_FORBIDDEN, "You cannot read this team.")
        members = await list_team_members(connection, team.id)
    return TeamDetail(id=team.id, name=team.name, course_id=course.id, members=members)


@router.post(
    "/api/courses/{course_id}/teams",
    status_code=status.HTTP_201_CREATED,
    dependencies=WRITE_DEPENDENCIES,
)
async def create_team_route(
    course_id: UUID,
    body: CreateTeamRequest,
    user: Annotated[User, Depends(current_user)],
    settings: Annotated[Settings, Depends(get_settings)],
) -> TeamSummary:
    """Create a team in a course."""
    async with database(settings) as connection:
        course = await _active_course(connection, course_id)
        await _require_instructor(connection, course_id=course.id, user_id=user.id)
        try:
            return await create_team(connection, course_id=course.id, name=body.name)
        except CourseError as error:
            raise _http_error(error) from None


@router.post(
    "/api/courses/{course_id}/teams/{team_id}/members",
    status_code=status.HTTP_201_CREATED,
    dependencies=WRITE_DEPENDENCIES,
)
async def add_team_member_route(
    course_id: UUID,
    team_id: UUID,
    body: AddTeamMemberRequest,
    user: Annotated[User, Depends(current_user)],
    settings: Annotated[Settings, Depends(get_settings)],
) -> TeamMember:
    """Add an existing course member to a team, or change their team role."""
    async with database(settings) as connection:
        course = await _active_course(connection, course_id)
        await _require_instructor(connection, course_id=course.id, user_id=user.id)
        try:
            return await add_team_member(
                connection,
                course_id=course.id,
                team_id=team_id,
                github_login=body.github_login,
                role=body.role,
            )
        except CourseError as error:
            raise _http_error(error) from None


@router.post("/api/courses/{course_id}/roster", dependencies=WRITE_DEPENDENCIES)
async def import_roster_route(
    course_id: UUID,
    body: ImportRosterRequest,
    user: Annotated[User, Depends(current_user)],
    settings: Annotated[Settings, Depends(get_settings)],
) -> RosterImportResult:
    """Import a roster; bad rows are reported and the rest are saved."""
    async with database(settings) as connection:
        course = await _active_course(connection, course_id)
        await _require_instructor(connection, course_id=course.id, user_id=user.id)
        rows = [RosterRow(entry.github_login, entry.role, entry.team) for entry in body.entries]
        return await import_roster(connection, course_id=course.id, rows=rows)
