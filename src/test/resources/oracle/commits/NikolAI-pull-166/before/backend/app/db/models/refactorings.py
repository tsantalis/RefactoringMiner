"""Stored RefactoringMiner runs and the read API's refactoring report (US-8)."""

# AI contribution: 50%

from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, JsonValue

Side = Literal["parent", "child"]
Change = Literal["new", "undone", "unchanged"]


class StoredRefactoring(BaseModel):
    """One refactoring as saved in a run's ``refactorings`` output."""

    model_config = ConfigDict(frozen=True)

    type: str
    description: str
    file_path: str | None
    start_line: int | None
    end_line: int | None
    side: Side


class StoredRefactorings(BaseModel):
    """The JSON of a run's ``refactorings`` output."""

    refactorings: list[StoredRefactoring]


class StoredRefactoringRun(BaseModel):
    """A completed RefactoringMiner run of one PR head, with its raw output."""

    head_sha: str
    json_content: JsonValue


class PullRefactoringCount(BaseModel):
    """A PR's latest analysed push: when, how many refactorings, and how many are new in it."""

    pull_request_id: UUID
    analysed_at: datetime | None
    refactorings: int
    new_refactorings: int


class NewRefactoringRun(BaseModel):
    """What the webhook job saves after RefactoringMiner analysed a PR's head commit."""

    github_repository_id: int
    pr_number: int
    head_sha: str
    base_sha: str
    pr_state: Literal["open", "closed"]
    pr_merged: bool
    workflow_run_id: int
    refactorings: StoredRefactorings


class RefactoringItem(BaseModel):
    """One refactoring in the read API, and how it compares with the previous analysed push.

    ``change`` is None on a PR's first analysed push, when there is nothing to compare with.
    """

    type: str
    description: str
    change: Change | None
    file_path: str | None
    start_line: int | None
    side: Side


class PullRequestRefactorings(BaseModel):
    """``GET /api/repos/{owner}/{repo}/pulls/{number}/refactorings``.

    ``commit`` is the head RefactoringMiner last analysed (None before its first run), and
    ``since_commit`` the head analysed before it, which ``change`` compares with (None, and no
    ``change``, on the first analysed push).
    """

    commit: str | None
    since_commit: str | None
    items: list[RefactoringItem]
