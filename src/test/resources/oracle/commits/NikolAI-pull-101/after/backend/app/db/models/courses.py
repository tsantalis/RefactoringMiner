"""Read models for courses, teams, the roster and feedback actions (ADR 0001)."""

# AI contribution: 50%

from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel

from app.db.models.users import User

CourseRole = Literal["instructor", "ta", "student"]
TeamRole = Literal["member", "lead"]
FeedbackActionType = Literal["acknowledged", "disputed"]


class Course(BaseModel):
    """A course row, including its join code. Never return it to a student."""

    id: UUID
    name: str
    join_code: str
    created_at: datetime
    archived_at: datetime | None


class CourseWithRole(Course):
    """A course and the caller's role in it."""

    role: CourseRole


class CourseSummary(BaseModel):
    """One course in a user's list: no join code, no team data."""

    id: UUID
    name: str
    role: CourseRole
    created_at: datetime


class TeamSummary(BaseModel):
    """A team in a course, with the number of active members."""

    id: UUID
    name: str
    member_count: int


class Team(BaseModel):
    """A team row; ``course_id`` is None for teams created before courses."""

    id: UUID
    name: str
    course_id: UUID | None
    archived_at: datetime | None


class TeamMember(BaseModel):
    """One active member of a team."""

    user_id: UUID
    github_login: str
    display_name: str | None
    role: TeamRole
    joined_at: datetime


class TeamDetail(BaseModel):
    """One team's data, returned only to those ``can_read_team`` allows."""

    id: UUID
    name: str
    course_id: UUID
    members: list[TeamMember]


class CourseDetail(BaseModel):
    """A course as its own members see it.

    ``join_code`` is set only for the course's instructors, and ``teams`` holds only the
    teams the caller may read (``can_read_team``).
    """

    id: UUID
    name: str
    role: CourseRole
    created_at: datetime
    join_code: str | None
    teams: list[TeamSummary]


class MyCourse(CourseSummary):
    """A course in ``GET /api/me``, with the teams the user may read."""

    teams: list[TeamSummary]


class MeResponse(User):
    """The signed-in user, their instructor flag, and their courses and teams."""

    courses: list[MyCourse]


class JoinCodeResponse(BaseModel):
    """The new join code after a rotation."""

    join_code: str


class RosterRowError(BaseModel):
    """One roster row the import refused, with its 1-based position in the request."""

    row: int
    error: str


class RosterImportResult(BaseModel):
    """The outcome of a roster import: rows saved and rows refused."""

    imported: int
    errors: list[RosterRowError]


class FeedbackActionRecord(BaseModel):
    """A saved acknowledgement or dispute."""

    id: UUID
    action_type: FeedbackActionType
    created_at: datetime
