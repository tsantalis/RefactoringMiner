"""Typed views of the GitHub webhook payloads NikolAI reads (US-2).

Includes the identifiers and PR lifecycle fields needed for persistence (US-4.7), and the
``workflow_run`` events that bring in the RefactoringMiner Action's results (US-8).
Pydantic ignores undeclared fields, so new GitHub fields never break parsing. Reference:
https://docs.github.com/en/webhooks/webhook-events-and-payloads
"""

# AI contribution: 50% or more AI-generated

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict


class _Payload(BaseModel):
    model_config = ConfigDict(frozen=True)


class GitHubUser(_Payload):
    id: int
    login: str
    # "User", or "Bot" for apps such as Dependabot.
    type: str = "User"


class PullRequest(_Payload):
    id: int  # GitHub's PR ID, distinct from the repository-local PR number.
    number: int
    title: str
    state: Literal["open", "closed"]
    created_at: datetime
    updated_at: datetime
    merged_at: datetime | None = None
    merged: bool = False
    body: str | None = None
    draft: bool = False
    user: GitHubUser


class RepositoryOwner(_Payload):
    login: str


class Repository(_Payload):
    id: int
    name: str
    full_name: str  # "owner/name"
    owner: RepositoryOwner


class Installation(_Payload):
    """The App installation that received the event, needed to act on that repository."""

    id: int


class PullRequestEvent(_Payload):
    """A ``pull_request`` webhook delivery (opened, reopened, ready_for_review, closed...)."""

    action: str
    installation: Installation
    repository: Repository
    pull_request: PullRequest
    sender: GitHubUser  # Who triggered the event; may differ from the PR author.


class RepositoryRef(_Payload):
    id: int


class BranchRef(_Payload):
    repo: RepositoryRef


class WorkflowRunPullRequest(_Payload):
    number: int
    base: BranchRef  # The repository the PR merges into.


class WorkflowRun(_Payload):
    id: int
    name: str
    event: str  # What started the run, e.g. "pull_request" or "push".
    head_sha: str
    conclusion: str | None = None  # "success", "failure"... None until the run completes.
    # Empty when the PR comes from a fork; can list several PRs sharing one head branch.
    pull_requests: tuple[WorkflowRunPullRequest, ...] = ()


class WorkflowRunEvent(_Payload):
    """A ``workflow_run`` webhook delivery (US-8), sent when an Actions workflow run changes."""

    action: str  # "requested", "in_progress" or "completed".
    installation: Installation
    repository: Repository
    workflow_run: WorkflowRun
