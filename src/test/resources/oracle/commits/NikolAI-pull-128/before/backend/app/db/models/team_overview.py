"""The team panel's response, ``GET /api/repos/{owner}/{repo}/team`` (US-11.2).

The shape is ``TeamOverview`` in ``frontend/apps/extension/src/shared/api/views-contract.ts``.
Values that later stories measure (peer reviews, stories, tests, AI declarations, marking) are
null until those stories store the data, never a made-up 0, and so are a PR's checklist counts
until NikolAI's code review has given it a checklist.
Iterations and badges are empty.
"""

# AI contribution: 50%

from datetime import date, datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel

CourseRole = Literal["student", "ta", "instructor"]
ReviewStatus = Literal["none", "queued", "running", "completed", "failed"]


class Viewer(BaseModel):
    login: str
    role: CourseRole


class TeamName(BaseModel):
    name: str
    # Teams created before courses have none.
    course: str | None


class Progress(BaseModel):
    name: str
    due_on: date
    tasks_done: int
    tasks_total: int


class TeamStats(BaseModel):
    # None while none of the team's open PRs has a checklist review.
    open_checklist_items: int | None
    refactorings: int
    refactorings_this_week: int
    reviewed_pulls: int
    merged_pulls: int


class TeamPull(BaseModel):
    number: int
    title: str
    author: str
    updated_at: datetime
    draft: bool
    # None until NikolAI's code review has given the PR a checklist.
    checklist_open: int | None
    checklist_total: int | None
    refactorings: int
    review_status: ReviewStatus


class TeamMember(BaseModel):
    login: str
    commits: int
    pulls: int
    # None until peer reviews and checklist fixes are stored.
    reviews: int | None
    substantive_reviews: int | None
    fixes_with_evidence: int | None
    last_commit_at: datetime | None


class Badge(BaseModel):
    id: str
    name: str


class NextBadge(BaseModel):
    name: str
    progress: int
    goal: int


class Badges(BaseModel):
    earned: list[Badge]
    next: NextBadge | None


class RubricRow(BaseModel):
    category: str
    suggested_level: Literal["poor", "ok", "expected", "exceptional"]
    evidence: str
    mark: str | None


class Marking(BaseModel):
    iteration: str
    state: Literal["draft", "published"]
    rubric: list[RubricRow]


class Attention(BaseModel):
    kind: Literal["untested", "open-threads", "story", "ai-use", "inactive"]
    text: str
    pull_number: int | None


class Dispute(BaseModel):
    finding_id: str
    login: str
    finding_name: str
    pull_number: int
    reason: str


class TeamStaff(BaseModel):
    """What only the course's TAs and instructors receive. Null for everyone else."""

    marking: Marking | None
    attention: list[Attention]
    disputes: list[Dispute]
    # None until NikolAI reads the team's stories and tests.
    stories_done: int | None
    stories_planned: int | None
    features_tested: int | None
    features_total: int | None
    ai_declared_percent: int | None


class TeamOverview(BaseModel):
    viewer: Viewer
    team: TeamName
    iteration: Progress | None
    next_deliverables: list[Progress]
    stats: TeamStats
    pulls: list[TeamPull]
    members: list[TeamMember]
    badges: Badges
    staff: TeamStaff | None


class StoredTeam(BaseModel):
    """A team, its course and the viewer's active role in that course (None without one)."""

    name: str
    course_name: str | None
    course_role: CourseRole | None


class StoredTeamPull(BaseModel):
    """An open PR as the panel lists it, with what its latest webhook event and runs say."""

    pull_request_id: UUID
    number: int
    title: str
    author: str
    updated_at: datetime
    draft: bool
    review_status: Literal["queued", "running", "completed", "failed"] | None


class StoredChecklistCount(BaseModel):
    """An open PR's checklist items: how many its checklist review has, and how many are open."""

    pull_request_id: UUID
    total: int
    open: int


class StoredPullStats(BaseModel):
    merged_pulls: int
    reviewed_pulls: int


class StoredTeamMember(BaseModel):
    login: str
    pulls: int
    commits: int
