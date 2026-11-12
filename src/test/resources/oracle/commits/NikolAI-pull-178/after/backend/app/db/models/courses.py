"""Courses, course roles, teams and the roster as the API reads and writes them (ADR 0001)."""

# AI contribution: 50%

from typing import Annotated, Literal
from uuid import UUID

from pydantic import BaseModel, Field, StringConstraints

from app.db.models.users import User

CourseRole = Literal["instructor", "ta", "student"]
TeamRole = Literal["member", "lead"]

# GitHub logins: letters, digits and single hyphens, at most 39 characters.
GITHUB_LOGIN_PATTERN = r"^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$"
GITHUB_LOGIN_MAX_LENGTH = 39
# One part of a repository's full name, the owner or the name: letters, digits, '.', '-' and
# '_', at most 100 characters (GitHub's limit for repository names).
REPOSITORY_NAME_PART = r"[A-Za-z0-9_.-]{1,100}"
# A repository's full name on GitHub, "owner/name".
REPOSITORY_NAME_PATTERN = rf"^{REPOSITORY_NAME_PART}/{REPOSITORY_NAME_PART}$"
MAX_ROSTER_ROWS = 1000
MAX_REQUIREMENT_TEXT_LENGTH = 100_000

Name = Annotated[str, StringConstraints(strip_whitespace=True, min_length=1, max_length=200)]
RequirementBody = Annotated[
    str,
    StringConstraints(strip_whitespace=True, max_length=MAX_REQUIREMENT_TEXT_LENGTH),
]


class TeamSummary(BaseModel):
    id: UUID
    name: str


class CourseSummary(BaseModel):
    """A course as one member sees it. Only the course's instructors see the join code."""

    id: UUID
    name: str
    role: CourseRole
    join_code: str | None
    teams: list[TeamSummary]


class Me(User):
    """``GET /api/me``: the user, with every course they are an active member of."""

    courses: list[CourseSummary]


class CourseMember(BaseModel):
    """One active member of a course, for the dashboard's people list."""

    user_id: UUID
    github_login: str
    display_name: str | None
    role: CourseRole


class TeamMember(BaseModel):
    user_id: UUID
    github_login: str
    display_name: str | None
    role: TeamRole


class TeamDetail(TeamSummary):
    course_id: UUID
    members: list[TeamMember]
    repositories: list[str]


class CourseCreate(BaseModel):
    name: Name


class RequirementText(BaseModel):
    text: RequirementBody


class JoinRequest(BaseModel):
    join_code: str = Field(min_length=1, max_length=40)


class JoinCode(BaseModel):
    join_code: str


class TeamCreate(BaseModel):
    name: Name


class RepositoryLink(BaseModel):
    """A registered repository to move into a course team, as ``owner/name``."""

    repository: str = Field(max_length=200, pattern=REPOSITORY_NAME_PATTERN)


class TeamMemberAdd(BaseModel):
    github_login: str = Field(max_length=GITHUB_LOGIN_MAX_LENGTH, pattern=GITHUB_LOGIN_PATTERN)
    role: TeamRole = "member"


class RosterImport(BaseModel):
    """Rows stay loose objects here so one malformed row is reported instead of failing all."""

    entries: list[object] = Field(max_length=MAX_ROSTER_ROWS)


class RosterRowError(BaseModel):
    row: int
    github_login: str
    reason: str


class RosterResult(BaseModel):
    """``imported`` rows were saved; ``applied`` of them matched someone who already signed in."""

    imported: int
    applied: int
    errors: list[RosterRowError]
