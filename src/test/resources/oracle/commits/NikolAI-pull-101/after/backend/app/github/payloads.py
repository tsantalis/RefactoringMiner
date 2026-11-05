"""Typed views of the GitHub webhook payloads NikolAI reads (US-2, US-10.1).

Includes the identifiers and PR lifecycle fields needed for persistence (US-4.7), and the
installation events that register repositories automatically (US-10.1).
Pydantic ignores undeclared fields, so new GitHub fields never break parsing. Reference:
https://docs.github.com/en/webhooks/webhook-events-and-payloads
"""

# AI contribution: 50%

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


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


class RepositorySummary(_Payload):
    """The repository fields that installation deliveries include."""

    id: int
    name: str
    full_name: str  # "owner/name"
    owner: RepositoryOwner


class InstallationEvent(_Payload):
    """An ``installation`` delivery: the App was installed, uninstalled or changed."""

    action: str
    installation: Installation
    # GitHub sends every repository on "created"; other actions may omit the list.
    repositories: list[RepositorySummary] = Field(default_factory=list)
    sender: GitHubUser


class InstallationRepositoriesEvent(_Payload):
    """An ``installation_repositories`` delivery: repositories added or removed."""

    action: str
    installation: Installation
    repositories_added: list[RepositorySummary] = Field(default_factory=list)
    repositories_removed: list[RepositorySummary] = Field(default_factory=list)
    sender: GitHubUser
