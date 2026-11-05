"""A PR's refactorings: saved from each RefactoringMiner run, read back for the extension (US-8).

The report comes from a workflow the team controls, so its file paths are untrusted: a path
that isn't a plain repository-relative path is dropped, and the refactoring is kept without it.
Access is the caller's job: the API checks ``can_read_team`` for the repository's team first.
"""

# AI contribution: 50%

import logging
from typing import Final

from psycopg import AsyncConnection
from psycopg.rows import DictRow
from pydantic import ValidationError

from app.db.models.refactorings import (
    Change,
    PullRequestRefactorings,
    RefactoringItem,
    StoredRefactoring,
    StoredRefactoringRun,
    StoredRefactorings,
)
from app.db.repositories.feedback import RegisteredRepository, find_pull_request
from app.db.repositories.refactorings import latest_refactoring_runs
from app.services.refactoring_report import CodeLocation, Refactoring

logger = logging.getLogger(__name__)

MAX_PATH_CHARS: Final = 1_000


def to_stored(refactorings: list[Refactoring]) -> StoredRefactorings:
    """What a run saves: each refactoring with one location, its head side when it has one."""
    return StoredRefactorings(refactorings=[_stored(refactoring) for refactoring in refactorings])


def _stored(refactoring: Refactoring) -> StoredRefactoring:
    after = next((loc for loc in refactoring.after if _safe_path(loc.file_path)), None)
    before = next((loc for loc in refactoring.before if _safe_path(loc.file_path)), None)
    location: CodeLocation | None = after or before
    description = refactoring.description.removeprefix(f"{refactoring.type}\t")
    return StoredRefactoring(
        type=" ".join(refactoring.type.split()),
        description=" ".join(description.split()),
        file_path=location.file_path if location else None,
        start_line=_line(location.start_line) if location else None,
        end_line=_line(location.end_line) if location else None,
        side="child" if location is None or location is after else "parent",
    )


def _safe_path(path: str) -> bool:
    """A repository-relative path: no control characters, absolute start or ``..`` segment."""
    return (
        0 < len(path) <= MAX_PATH_CHARS
        and path.isprintable()
        and not path.startswith(("/", "\\"))
        and ".." not in path.replace("\\", "/").split("/")
    )


def _line(line: int | None) -> int | None:
    return line if line is not None and line >= 1 else None


async def pull_request_refactorings(
    connection: AsyncConnection[DictRow], repository: RegisteredRepository, number: int
) -> PullRequestRefactorings | None:
    """The latest analysed push's refactorings compared with the push before it, or None."""
    pr = await find_pull_request(connection, repository.id, number)
    if pr is None:
        return None
    runs = [
        (run.head_sha, items)
        for run in await latest_refactoring_runs(connection, pr.id)
        if (items := _items(run)) is not None
    ]
    if not runs:
        return PullRequestRefactorings(commit=None, since_commit=None, items=[])
    commit, latest = runs[0]
    since, previous = runs[1] if len(runs) > 1 else (None, None)
    return PullRequestRefactorings(
        commit=commit, since_commit=since, items=compare(latest, previous)
    )


def _items(run: StoredRefactoringRun) -> list[StoredRefactoring] | None:
    try:
        return StoredRefactorings.model_validate(run.json_content).refactorings
    except ValidationError:
        logger.warning("Skipped the unreadable refactoring output of commit %s", run.head_sha[:7])
        return None


def compare(
    latest: list[StoredRefactoring], previous: list[StoredRefactoring] | None
) -> list[RefactoringItem]:
    """Mark each refactoring new or unchanged since ``previous``, then list the undone ones.

    A refactoring is identified by its type and description. Without a previous push, every
    refactoring is new.
    """
    before = {(r.type, r.description) for r in previous or []}
    now = {(r.type, r.description) for r in latest}
    items = [_item(r, "unchanged" if (r.type, r.description) in before else "new") for r in latest]
    undone = [r for r in previous or [] if (r.type, r.description) not in now]
    return items + [_item(r, "undone") for r in undone]


def _item(refactoring: StoredRefactoring, change: Change) -> RefactoringItem:
    return RefactoringItem(
        type=refactoring.type,
        description=refactoring.description,
        change=change,
        file_path=refactoring.file_path,
        start_line=refactoring.start_line,
        side=refactoring.side,
    )
