"""Courses, join codes, course teams and the roster (US-10, ADR 0001 PR 2).

Every endpoint needs a signed-in user; every write also needs an allowed ``Origin``, because
the session cookie is ``SameSite=None`` (decision 3). An unknown course is 404; a course the
user has no say in is 403. Course members can read the requirement texts; course instructors
and stakeholders can replace them. The member and team access grant endpoints are
instructor-only.
"""

# AI contribution: 50%

import logging
from typing import Annotated, Literal
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from psycopg import AsyncConnection
from psycopg.errors import UniqueViolation
from psycopg.rows import DictRow

from app.api.dependencies import current_user, database, require_allowed_origin
from app.core.config import Settings, get_settings
from app.db.models.courses import (
    CourseCreate,
    CourseMember,
    CourseRole,
    CourseSummary,
    JoinCode,
    JoinRequest,
    RepositoryLink,
    RequirementText,
    RosterImport,
    RosterResult,
    TeamAccessGrant,
    TeamAccessGrantCreate,
    TeamCreate,
    TeamDetail,
    TeamMemberAdd,
    TeamSummary,
)
from app.db.models.users import User
from app.db.repositories import courses as repo
from app.db.repositories import team_access as grant_repo
from app.db.repositories.courses import PromoteOutcome
from app.db.repositories.repository_registry import (
    RepositoryNotFoundError,
    RepositoryNotLinkableError,
    link_repository,
)
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


_ROLE_PLURALS: dict[CourseRole, str] = {
    "instructor": "instructors",
    "ta": "TAs",
    "stakeholder": "stakeholders",
    "student": "students",
}


def only_roles_message(roles: tuple[CourseRole, ...]) -> str:
    """Who may do something, for a course member whose role can't: "Only this course's
    instructors and TAs can do this." """
    names = [_ROLE_PLURALS[role] for role in roles]
    who = names[0] if len(names) == 1 else f"{', '.join(names[:-1])} and {names[-1]}"
    return f"Only this course's {who} can do this."


async def _require_role(
    connection: AsyncConnection[DictRow], course_id: UUID, user: User, *roles: CourseRole
) -> CourseRole:
    """The user's role in the course, or 404 (no such course) / 403 (not one of ``roles``).

    Someone outside the course is told they have no access; a member whose role can't do this
    is told which roles can, since they can see the course and "no access" would be wrong.
    """
    lookup = await repo.course_lookup(connection, course_id, user.id)
    if not lookup.exists:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Course not found")
    if lookup.role is None:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "You don't have access to this course")
    if lookup.role not in roles:
        raise HTTPException(status.HTTP_403_FORBIDDEN, only_roles_message(roles))
    return lookup.role


async def _require_team(
    connection: AsyncConnection[DictRow], course_id: UUID, team_id: UUID
) -> None:
    """404 when ``team_id`` isn't an active team of this course."""
    if not await repo.find_team(connection, course_id, team_id):
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Team not found")


async def _change_student_role(
    course_id: UUID,
    user_id: UUID,
    user: User,
    settings: Settings,
    role: Literal["ta", "stakeholder"],
) -> CourseMember:
    """Make a student of the course a TA (``ta``) or a stakeholder (``stakeholder``).

    404 when the target isn't in the course; 409 for any other course role.
    """
    async with database(settings) as connection:
        await _require_role(connection, course_id, user, "instructor")
        change = repo.promote_to_ta if role == "ta" else repo.promote_to_stakeholder
        result = await change(connection, course_id, user_id)
    if result.outcome is PromoteOutcome.NOT_A_MEMBER:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "That user isn't in this course")
    if result.outcome is PromoteOutcome.NOT_A_STUDENT:
        message = (
            "Only a student can be promoted to TA"
            if role == "ta"
            else "Only a student can be made a stakeholder"
        )
        raise HTTPException(status.HTTP_409_CONFLICT, message)
    member = result.member
    if member is None:
        logger.error(
            "A made %s for user %s vanished between the update and the read", role, user_id
        )
        raise RuntimeError("A changed member vanished between the update and the read")
    logger.info(
        "User %s changed %s to %s of course %s", user.github_login, user_id, role, course_id
    )
    return member


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
        await _require_role(connection, course_id, user, *repo.COURSE_READERS)
        return await _course_view(connection, course_id, user)


async def _read_requirement_text(
    course_id: UUID, user: User, settings: Settings, field: repo.RequirementField
) -> RequirementText:
    async with database(settings) as connection:
        await _require_role(connection, course_id, user, *repo.REQUIREMENT_READERS)
        text = await repo.get_requirement_text(connection, course_id, field)
    return RequirementText(text=text)


async def _replace_requirement_text(
    course_id: UUID,
    body: RequirementText,
    user: User,
    settings: Settings,
    field: repo.RequirementField,
) -> RequirementText:
    async with database(settings) as connection:
        await _require_role(connection, course_id, user, *repo.REQUIREMENT_EDITORS)
        text = await repo.replace_requirement_text(connection, course_id, field, body.text)
    logger.info("User %s replaced %s for course %s", user.github_login, field, course_id)
    return RequirementText(text=text)


@router.get("/{course_id}/course-requirements")
async def get_course_requirements(
    course_id: UUID, user: SignedIn, settings: AppSettings
) -> RequirementText:
    return await _read_requirement_text(course_id, user, settings, "course_requirements")


@router.put("/{course_id}/course-requirements", dependencies=WRITE)
async def replace_course_requirements(
    course_id: UUID, body: RequirementText, user: SignedIn, settings: AppSettings
) -> RequirementText:
    return await _replace_requirement_text(course_id, body, user, settings, "course_requirements")


@router.get("/{course_id}/project-requirements")
async def get_project_requirements(
    course_id: UUID, user: SignedIn, settings: AppSettings
) -> RequirementText:
    return await _read_requirement_text(course_id, user, settings, "project_requirements")


@router.put("/{course_id}/project-requirements", dependencies=WRITE)
async def replace_project_requirements(
    course_id: UUID, body: RequirementText, user: SignedIn, settings: AppSettings
) -> RequirementText:
    return await _replace_requirement_text(course_id, body, user, settings, "project_requirements")


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


@router.get("/{course_id}/members")
async def list_members(
    course_id: UUID, user: SignedIn, settings: AppSettings
) -> list[CourseMember]:
    """The course's people, for the dashboard's instructor and TA views. Students get 403."""
    async with database(settings) as connection:
        await _require_role(connection, course_id, user, "instructor", "ta")
        return await repo.list_course_members(connection, course_id)


@router.post("/{course_id}/members/{user_id}/promote", dependencies=WRITE)
async def promote_member(
    course_id: UUID, user_id: UUID, user: SignedIn, settings: AppSettings
) -> CourseMember:
    """Make a student a TA (ADR 0001, decision 6); 409 for any other role, 404 for a non-member."""
    return await _change_student_role(course_id, user_id, user, settings, "ta")


@router.post("/{course_id}/members/{user_id}/stakeholder", dependencies=WRITE)
async def make_stakeholder(
    course_id: UUID, user_id: UUID, user: SignedIn, settings: AppSettings
) -> CourseMember:
    """Make a student a stakeholder (ADR 0002, decision 1); 409 for any other role, 404 outside."""
    return await _change_student_role(course_id, user_id, user, settings, "stakeholder")


@router.get("/{course_id}/grants")
async def list_team_access_grants(
    course_id: UUID, user: SignedIn, settings: AppSettings
) -> list[TeamAccessGrant]:
    """Every active team access grant in the course, for the dashboard's access controls."""
    async with database(settings) as connection:
        await _require_role(connection, course_id, user, "instructor")
        return await grant_repo.list_team_access_grants(connection, course_id)


@router.post(
    "/{course_id}/teams/{team_id}/grants", status_code=status.HTTP_201_CREATED, dependencies=WRITE
)
async def grant_team_access(
    course_id: UUID,
    team_id: UUID,
    body: TeamAccessGrantCreate,
    user: SignedIn,
    settings: AppSettings,
) -> TeamAccessGrant:
    """Give a stakeholder of this course read access to one team (ADR 0002, decision 2)."""
    async with database(settings) as connection:
        await _require_role(connection, course_id, user, "instructor")
        await _require_team(connection, course_id, team_id)
        result = await grant_repo.create_team_access_grant(
            connection, course_id, team_id, body.user_id, user.id
        )
    if result.outcome is grant_repo.GrantOutcome.NOT_A_MEMBER:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "That user isn't in this course")
    if result.outcome is grant_repo.GrantOutcome.NOT_A_STAKEHOLDER:
        raise HTTPException(
            status.HTTP_409_CONFLICT,
            {
                "code": "not_a_stakeholder",
                "message": "Only a stakeholder of this course can be granted access",
            },
        )
    if result.outcome is grant_repo.GrantOutcome.ALREADY_GRANTED:
        raise HTTPException(status.HTTP_409_CONFLICT, {"code": "already_granted"})
    if result.grant is None:
        logger.error("A made grant for user %s on team %s was not returned", body.user_id, team_id)
        raise RuntimeError("A made grant was not returned")
    logger.info(
        "User %s granted %s access to team %s in course %s",
        user.github_login,
        body.user_id,
        team_id,
        course_id,
    )
    return result.grant


@router.delete(
    "/{course_id}/teams/{team_id}/grants/{user_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    dependencies=WRITE,
)
async def revoke_team_access(
    course_id: UUID, team_id: UUID, user_id: UUID, user: SignedIn, settings: AppSettings
) -> None:
    """Revoke the stakeholder's access to one team. 404 when there is no active grant."""
    async with database(settings) as connection:
        await _require_role(connection, course_id, user, "instructor")
        await _require_team(connection, course_id, team_id)
        revoked = await grant_repo.revoke_team_access_grant(connection, team_id, user_id)
    if not revoked:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "No active access grant for that person")
    logger.info(
        "User %s revoked %s's access to team %s in course %s",
        user.github_login,
        user_id,
        team_id,
        course_id,
    )


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
        await _require_team(connection, course_id, team_id)
        member_id = await _course_member_by_login(connection, course_id, body.github_login)
        member = await repo.get_course_member(connection, course_id, member_id)
        if member is not None and member.role == "stakeholder":
            raise HTTPException(
                status.HTTP_409_CONFLICT,
                "A stakeholder can't be on a team. Give them team access instead.",
            )
        await repo.add_team_member(connection, team_id, member_id, body.role)
        team = await repo.team_detail(connection, course_id, team_id)
    if team is None:
        raise RuntimeError("A team vanished while adding a member")
    return team


async def _course_member_by_login(
    connection: AsyncConnection[DictRow], course_id: UUID, github_login: str
) -> UUID:
    users = (await repo.users_by_logins(connection, [github_login])).get(github_login.lower(), [])
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


@router.post("/{course_id}/teams/{team_id}/repositories", dependencies=WRITE)
async def link_team_repository(
    course_id: UUID, team_id: UUID, body: RepositoryLink, user: SignedIn, settings: AppSettings
) -> TeamDetail:
    """Move a repository the App is installed on into this course team (US-10.1).

    Any refusal other than "not registered" is the same 409, so the answer doesn't reveal
    whether a repository belongs to another course (ADR 0001, Consequences).
    """
    async with database(settings) as connection:
        await _require_role(connection, course_id, user, "instructor")
        await _require_team(connection, course_id, team_id)
        try:
            await link_repository(connection, course_id, team_id, body.repository)
        except RepositoryNotFoundError:
            raise HTTPException(
                status.HTTP_404_NOT_FOUND, {"code": "repository_not_registered"}
            ) from None
        except RepositoryNotLinkableError as error:
            logger.info("Refused to link %s to team %s: %s", body.repository, team_id, error)
            raise HTTPException(
                status.HTTP_409_CONFLICT,
                {
                    "code": "repository_not_linkable",
                    "message": "Link a repository that someone in this course installed the "
                    "GitHub App on, and that isn't in another course.",
                },
            ) from None
        team = await repo.team_detail(connection, course_id, team_id)
    logger.info("User %s linked %s to team %s", user.github_login, body.repository, team_id)
    if team is None:
        raise RuntimeError("A team vanished while linking a repository")
    return team
