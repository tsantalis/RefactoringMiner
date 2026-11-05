"""What the fix-check agent is shown of a push (US-9.4), and which citations it may make.

A push is every commit between the head the checklist was made for and the PR's new head, from
GitHub's compare API. Each commit's own diff is fetched and annotated with its line numbers,
the way the code review's diff is (``app.services.code_review_diff``), so the agent can cite
the commit, file and lines that fixed an item, and NikolAI can check that the citation points
inside that commit's diff.

The diffs share one model call's budget. When they don't all fit, the newest commits' files go
first (a fix usually comes last), and the files left out are named with the reason. A push of
more than ``MAX_PUSH_COMMITS`` commits shows only its newest ones.

A merge commit (the base branch merged into the PR) isn't shown or citable: its diff against its
first parent repeats the changes of the base branch's commits, which the push lists on their own.
"""

# AI contribution: 50%

import html
from dataclasses import dataclass
from typing import Final, cast

from githubkit import GitHub
from githubkit.auth import AppInstallationAuthStrategy
from githubkit_schemas.latest.models import DiffEntry

from app.agents.untrusted import neutralize_tags
from app.db.models.pull_insights import SHORT_SHA, StoredFix
from app.services.code_review_diff import (
    MAX_DIFF_CHARS,
    SKIPPED_OVER_BUDGET,
    ShownFile,
    lines_inside,
    shown_file,
)
from app.services.line_mapping import map_line

MAX_PUSH_COMMITS: Final = 20
# Short SHAs shorter than this are too likely to name two commits.
MIN_SHA_PREFIX: Final = 7
# Compare statuses: the new head has every commit of the old one; the old head already has every
# commit of the new one (a late or retried event), or they are the same commit.
_AHEAD: Final = "ahead"
_COVERED: Final = frozenset({"behind", "identical"})


@dataclass(frozen=True, slots=True)
class PushCommit:
    """One commit of the push, with each changed file's patch (None for binary or huge ones)."""

    sha: str
    message: str
    patches: dict[str, str | None]
    # Paths the commit removed or renamed away: a line in them has no place in the head.
    gone: frozenset[str]


@dataclass(frozen=True, slots=True)
class Push:
    """The commits after the checklist's head, oldest first, up to the PR's new head."""

    base: str
    head: str
    commits: tuple[PushCommit, ...]
    # Older commits of the push that weren't fetched, past ``MAX_PUSH_COMMITS``.
    unshown_commits: int


@dataclass(frozen=True, slots=True)
class PushPlan:
    """The push as the agent sees it: the diff blocks, and the lines each commit may cite."""

    push: Push
    diff: str
    # The shown hunks' line ranges, by commit SHA, then by path.
    changed_lines: dict[str, dict[str, tuple[tuple[int, int], ...]]]
    # Files of the push the agent isn't shown, as "path@short-sha", with the reason.
    skipped_files: dict[str, str]

    def resolve(self, commit: str) -> str | None:
        """The full SHA of the one shown commit that ``commit`` names, or None."""
        commit = commit.strip().lower()
        if len(commit) < MIN_SHA_PREFIX:
            return None
        matches = [sha for sha in self.changed_lines if sha.startswith(commit)]
        return matches[0] if len(matches) == 1 else None

    def contains(self, cited: StoredFix) -> bool:
        """Whether the cited lines lie in one hunk of their file that their commit showed."""
        return lines_inside(
            self.changed_lines.get(cited.commit, {}),
            cited.file_path,
            cited.start_line,
            cited.end_line,
        )

    def to_head(self, sha: str, file_path: str, lines: tuple[int, int]) -> tuple[int, int] | None:
        """Lines of ``file_path`` as commit ``sha`` left them, numbered as in the PR's head.

        None when a later commit of the push changed one of the lines, or removed or renamed
        the file, or changed it without a patch to follow.
        """
        first, last = lines
        for commit in self._after(sha):
            if file_path in commit.gone:
                return None
            if file_path not in commit.patches:
                continue
            patch = commit.patches[file_path]
            new_first = map_line(patch, first) if patch else None
            new_last = map_line(patch, last) if patch else None
            if new_first is None or new_last is None:
                return None
            first, last = new_first, new_last
        return first, last

    def _after(self, sha: str) -> tuple[PushCommit, ...]:
        """The commits of the push after commit ``sha``."""
        for index, commit in enumerate(self.push.commits):
            if commit.sha == sha:
                return self.push.commits[index + 1 :]
        return ()


async def fetch_push(
    github: GitHub[AppInstallationAuthStrategy], owner: str, repo: str, base: str, head: str
) -> Push | None:
    """The push from ``base`` to ``head``, or None when ``head`` doesn't build on ``base``
    (a force push or a rebase), so the checklist can't be followed commit by commit. A push with
    no commits means the checklist already covers ``head``.

    Raises:
        GitHubException: GitHub didn't answer.
    """
    response = await github.rest.repos.async_compare_commits(
        owner=owner, repo=repo, basehead=f"{base}...{head}"
    )
    comparison = response.parsed_data
    if comparison.status in _COVERED:
        return Push(base, head, (), 0)
    if comparison.status != _AHEAD:
        return None
    own = [entry for entry in comparison.commits if len(entry.parents) <= 1]
    listed = own[-MAX_PUSH_COMMITS:]
    commits = [await _push_commit(github, owner, repo, entry.sha) for entry in listed]
    unshown = max(0, comparison.total_commits - len(comparison.commits)) + len(own) - len(listed)
    return Push(base, head, tuple(commits), unshown)


async def _push_commit(
    github: GitHub[AppInstallationAuthStrategy], owner: str, repo: str, sha: str
) -> PushCommit:
    commit = (await github.rest.repos.async_get_commit(owner, repo, sha)).parsed_data
    files = cast("list[DiffEntry]", commit.files or [])
    return PushCommit(
        sha=commit.sha,
        message=commit.commit.message,
        patches={
            file.filename: file.patch if isinstance(file.patch, str) else None for file in files
        },
        gone=frozenset(path for file in files for path in _gone_paths(file)),
    )


def _gone_paths(file: DiffEntry) -> list[str]:
    """The paths a changed file takes away: itself when removed, its old name when renamed."""
    if file.status == "removed":
        return [file.filename]
    if file.status == "renamed" and isinstance(file.previous_filename, str):
        return [file.previous_filename]
    return []


def plan_push(push: Push) -> PushPlan:
    """Pack the push's files into one call's budget, newest commits first, and show them oldest
    first, each commit with its SHA and message."""
    shown: dict[str, list[ShownFile]] = {}
    skipped: dict[str, str] = {}
    room = MAX_DIFF_CHARS
    for commit in reversed(push.commits):
        files = shown.setdefault(commit.sha, [])
        for path, patch in commit.patches.items():
            file = shown_file(path, patch or "")
            if isinstance(file, ShownFile) and len(file.block) > room:
                file = SKIPPED_OVER_BUDGET
            if isinstance(file, str):
                skipped[f"{path}@{commit.sha[:SHORT_SHA]}"] = file
                continue
            files.append(file)
            room -= len(file.block)
    blocks = [_commit_block(commit, shown[commit.sha]) for commit in push.commits]
    return PushPlan(
        push=push,
        diff="\n".join(blocks),
        changed_lines={
            sha: {file.path: file.ranges for file in files} for sha, files in shown.items()
        },
        skipped_files=skipped,
    )


def _commit_block(commit: PushCommit, files: list[ShownFile]) -> str:
    message = neutralize_tags(commit.message.strip().splitlines()[0] if commit.message else "")
    body = "\n".join(file.block for file in files) or "(no file of this commit is shown)"
    return (
        f'<commit sha="{commit.sha}" message="{html.escape(message, quote=True)}">\n'
        f"{body}\n</commit>"
    )
