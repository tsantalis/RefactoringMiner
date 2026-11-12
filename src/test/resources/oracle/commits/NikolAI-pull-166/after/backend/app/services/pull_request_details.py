"""What NikolAI fetches about a PR from GitHub, and the Markdown log it writes of it.

Team Alpha's US-2 webhook recorded each PR's commits, files and diffs in a log. The code-review
agent (US-9) reviews the same diffs, so the log is the record of what it was shown.
"""

# AI contribution: 50%

import logging
from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path
from typing import Final, cast

from githubkit import GitHub
from githubkit.auth import AppInstallationAuthStrategy
from githubkit.exception import GitHubException
from githubkit_schemas.latest.models import Commit, DiffEntry

from app.github.payloads import PullRequestEvent

logger = logging.getLogger(__name__)

# GitHub's largest page. The PR file listing stops at 3,000 files (30 pages): past that, the
# files are counted as not fetched, from the PR's ``changed_files``.
FILES_PAGE_SIZE: Final = 100
MAX_FILE_PAGES: Final = 30


@dataclass(frozen=True, slots=True)
class PullRequestDetails:
    """What Team Alpha's US-2 webhook records about a PR."""

    title: str
    description: str
    commit_messages: list[str]
    files_changed: list[str]
    additions: int
    deletions: int
    diffs: dict[str, str]
    # Changed files GitHub's listing didn't send (past its 3,000-file limit).
    unfetched_files: int = 0


async def fetch_pull_request_details(
    github: GitHub[AppInstallationAuthStrategy], event: PullRequestEvent
) -> PullRequestDetails:
    """Fetch the PR's changed files (every page GitHub lists) and its first 100 commits.

    The files are what the code review needs, so a failure to list them raises. The commits
    only go to the log: if they can't be listed, the log has none and the review goes on.

    Raises:
        GitHubException: GitHub didn't send the changed files.
    """
    owner = event.repository.owner.login
    repo = event.repository.name
    number = event.pull_request.number
    changed = await list_changed_files(github, owner, repo, number)
    return PullRequestDetails(
        title=event.pull_request.title,
        description=event.pull_request.body or "",
        commit_messages=await _commit_messages(github, owner, repo, number),
        files_changed=[file.filename for file in changed],
        additions=sum(file.additions for file in changed),
        deletions=sum(file.deletions for file in changed),
        # Binary and very large files come without a patch.
        diffs={file.filename: file.patch for file in changed if isinstance(file.patch, str)},
        unfetched_files=max(0, (event.pull_request.changed_files or 0) - len(changed)),
    )


async def list_changed_files(
    github: GitHub[AppInstallationAuthStrategy], owner: str, repo: str, number: int
) -> list[DiffEntry]:
    """Every changed file GitHub lists for the PR, a page at a time, up to its file limit.

    Raises:
        GitHubException: GitHub didn't send a page.
    """
    changed: list[DiffEntry] = []
    for page in range(1, MAX_FILE_PAGES + 1):
        response = await github.rest.pulls.async_list_files(
            owner=owner, repo=repo, pull_number=number, per_page=FILES_PAGE_SIZE, page=page
        )
        # githubkit's REST classes define a method named `list`, which hides the built-in in
        # their annotations, so mypy can't read these return types. Restate them.
        files = cast("list[DiffEntry]", response.parsed_data)
        changed.extend(files)
        if len(files) < FILES_PAGE_SIZE:
            break
    return changed


async def _commit_messages(
    github: GitHub[AppInstallationAuthStrategy], owner: str, repo: str, number: int
) -> list[str]:
    try:
        commits = await github.rest.pulls.async_list_commits(
            owner=owner, repo=repo, pull_number=number, per_page=100
        )
    except GitHubException as error:
        logger.warning(
            "Could not list the commits of %s/%s#%d for its log: %s",
            owner,
            repo,
            number,
            type(error).__name__,
        )
        return []
    return [commit.commit.message for commit in cast("list[Commit]", commits.parsed_data)]


def render_pull_request_log(number: int, details: PullRequestDetails) -> str:
    """The Markdown log of a PR, in the format of Team Alpha's US-2 webhook."""
    fence = "`" * 3
    lines = [
        f"# PR #{number}: {details.title}",
        f"\n## Description\n{details.description or '(none)'}",
        "\n## Commits",
        *[f"- {message}" for message in details.commit_messages],
        f"\n## Lines changed\n+{details.additions} -{details.deletions}",
        "\n## Files changed",
        *[f"- {name}" for name in details.files_changed],
    ]
    lines.extend(
        f"\n## Diff: {name}\n{fence}diff\n{patch}\n{fence}" for name, patch in details.diffs.items()
    )
    return "\n".join(lines)


def write_pull_request_log(
    log_dir: Path, repository: str, number: int, details: PullRequestDetails
) -> Path:
    """Write the Markdown log to ``log_dir/<owner>_<repo>_PR<number>_<UTC timestamp>.md``.

    ``repository`` is ``owner/name``: teams often share a template repository name, so the owner
    keeps their logs apart, and the microseconds keep two events of one PR apart.
    """
    log_dir.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now(UTC).strftime("%Y%m%d_%H%M%S_%f")
    path = log_dir / f"{repository.replace('/', '_')}_PR{number}_{timestamp}.md"
    path.write_text(render_pull_request_log(number, details), encoding="utf-8")
    return path
