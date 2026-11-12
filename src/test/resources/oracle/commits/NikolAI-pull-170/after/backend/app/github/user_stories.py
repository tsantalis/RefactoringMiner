"""Read a repository's ``user story`` issues from GitHub (US-13.2)."""

# AI contribution: 50%

from typing import Final, NamedTuple

from githubkit import GitHub
from githubkit.auth import AppInstallationAuthStrategy
from pydantic import BaseModel, TypeAdapter

USER_STORY_LABEL: Final = "user story"  # The label .github/ISSUE_TEMPLATE/user-story.yml sets.
# Past this, the prompt would no longer fit a model's context, and a backlog cut short would
# report requirements as uncovered when a story left out covers them.
MAX_USER_STORIES: Final = 200
_PAGE_SIZE: Final = 100


class UserStory(NamedTuple):
    number: int
    title: str
    body: str
    url: str


class _ListedIssue(BaseModel):
    """The fields NikolAI reads from GitHub's issue list.

    Not githubkit's ``parsed_data``: its schema rejects fields GitHub has added since, such as a
    label's ``archived_by``, which would fail every check on a repository with such a label.
    """

    number: int
    title: str
    body: str | None = None
    html_url: str
    pull_request: object | None = None


_LISTED_ISSUES: Final = TypeAdapter(list[_ListedIssue])


class TooManyUserStoriesError(Exception):
    """The repository has more than ``MAX_USER_STORIES`` user stories."""


async def list_user_stories(
    github: GitHub[AppInstallationAuthStrategy], owner: str, repo: str
) -> list[UserStory]:
    """Every open and closed user story of the repository, in GitHub's order.

    Raises:
        TooManyUserStoriesError: past ``MAX_USER_STORIES``.
        githubkit.exception.GitHubException: GitHub refused or didn't answer.
    """
    stories: list[UserStory] = []
    page = 1
    while True:
        response = await github.rest.issues.async_list_for_repo(
            owner=owner,
            repo=repo,
            state="all",
            labels=USER_STORY_LABEL,
            per_page=_PAGE_SIZE,
            page=page,
        )
        issues = _LISTED_ISSUES.validate_python(response.json())
        stories += [
            UserStory(issue.number, issue.title, issue.body or "", issue.html_url)
            for issue in issues
            if issue.pull_request is None  # GitHub lists pull requests as issues too.
        ]
        if len(stories) > MAX_USER_STORIES:
            raise TooManyUserStoriesError
        if len(issues) < _PAGE_SIZE:
            return stories
        page += 1
