"""List the open brainstorm issues of a team's repositories (US-20.2, #245).

Access is the caller's job: the API runs ``may_read_team`` first. The issues come from GitHub,
read as the App's installation on each repository. A repository GitHub can't be read for is
``failed`` and logged, and the team's other repositories are still listed.
"""

# AI contribution: 50%

import asyncio
import logging

from githubkit.exception import GitHubException
from pydantic import ValidationError

from app.core.config import ConfigurationError, Settings
from app.db.models.planning import (
    BrainstormIssueLink,
    BrainstormList,
    BrainstormStatus,
    RepositoryBrainstorm,
)
from app.github.brainstorm_issues import (
    BRAINSTORM_LABEL,
    all_open_on_github,
    list_open_brainstorm_issues,
)
from app.github.client import repository_client

logger = logging.getLogger(__name__)


async def list_team_brainstorm(settings: Settings, full_names: list[str]) -> BrainstormList:
    """Each repository's open brainstorm issues, in the order given. Never raises for GitHub."""
    repositories = await asyncio.gather(
        *(_repository_brainstorm(settings, full_name) for full_name in full_names)
    )
    return BrainstormList(label=BRAINSTORM_LABEL, repositories=list(repositories))


async def _repository_brainstorm(settings: Settings, full_name: str) -> RepositoryBrainstorm:
    owner, repo = full_name.split("/", 1)
    try:
        client = await repository_client(settings, owner, repo)
        async with client as github:
            listed = await list_open_brainstorm_issues(github, owner, repo)
    except (GitHubException, ConfigurationError, ValidationError) as error:
        # By type only: the messages can echo secrets.
        logger.error(
            "Could not list the brainstorm issues of %s: %s", full_name, type(error).__name__
        )
        return _result(full_name, "failed", [], more=False)
    issues = [
        BrainstormIssueLink(number=issue.number, title=issue.title, url=issue.url)
        for issue in listed.issues
    ]
    return _result(full_name, "listed", issues, more=listed.more)


def _result(
    full_name: str,
    status: BrainstormStatus,
    issues: list[BrainstormIssueLink],
    *,
    more: bool,
) -> RepositoryBrainstorm:
    return RepositoryBrainstorm(
        full_name=full_name,
        status=status,
        issues=issues,
        more=more,
        all_url=all_open_on_github(full_name),
    )
