"""Save and read RefactoringMiner runs of a PR (US-8) in ``analysis_runs``/``analysis_outputs``.

Each analysed head commit of a PR has one ``refactoring`` run, which a re-run on the same commit
replaces (``save_commit_analysis``). Callers own their connections.
"""

# AI contribution: 50%

from typing import Final, cast
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db.models.refactorings import (
    NewRefactoringRun,
    PullRefactoringCount,
    StoredRefactoringRun,
)
from app.db.repositories.commit_analyses import (
    AnalysisKind,
    save_commit_analysis,
    stored_pull_request,
)

REFACTORING_RUNS: Final = AnalysisKind(
    analysis_type="refactoring",
    tool_name="RefactoringMiner",
    output_key="refactorings",
    output_type="refactoring_miner",
    schema_version="1",
)


async def save_refactoring_run(
    connection: AsyncConnection[DictRow], run: NewRefactoringRun
) -> bool:
    """Save one analysed head commit's refactorings. False when NikolAI doesn't store the PR."""
    content = run.refactorings.model_dump()
    return await save_commit_analysis(connection, REFACTORING_RUNS, run, content)


async def latest_refactoring_runs(
    connection: AsyncConnection[DictRow],
    pull_request_id: UUID,
    limit: int = 2,
    exclude_sha: str | None = None,
) -> list[StoredRefactoringRun]:
    """The PR's most recently analysed head commits, newest first, with their output.

    This order decides which push a run is compared with, for the read API and the PR comment.
    ``exclude_sha`` leaves out one head commit's run.
    """
    cursor = await connection.execute(
        """SELECT a.child_commit_sha AS head_sha, o.json_content
           FROM analysis_runs a
           JOIN analysis_outputs o
             ON o.analysis_run_id = a.id AND o.output_key = %s AND o.output_type = %s
           WHERE a.pull_request_id = %s AND a.analysis_type = %s
             AND a.status = 'completed' AND a.child_commit_sha IS DISTINCT FROM %s
           ORDER BY a.finished_at DESC, a.id DESC LIMIT %s""",
        (
            REFACTORING_RUNS.output_key,
            REFACTORING_RUNS.output_type,
            pull_request_id,
            REFACTORING_RUNS.analysis_type,
            exclude_sha,
            limit,
        ),
    )
    return [StoredRefactoringRun.model_validate(row) for row in await cursor.fetchall()]


async def previous_refactoring_run(
    connection: AsyncConnection[DictRow], github_repository_id: int, pr_number: int, head_sha: str
) -> StoredRefactoringRun | None:
    """The PR's most recently analysed head commit other than ``head_sha``, with its output.

    Once ``head_sha`` is saved, this is the run the read API compares it with (``since_commit``).
    None before the PR's second analysed push, or when NikolAI doesn't store the PR.
    """
    pr = await stored_pull_request(connection, github_repository_id, pr_number)
    if pr is None:
        return None
    runs = await latest_refactoring_runs(
        connection, cast(UUID, pr["id"]), limit=1, exclude_sha=head_sha
    )
    return runs[0] if runs else None


async def refactoring_counts(
    connection: AsyncConnection[DictRow], repository_id: UUID
) -> list[PullRefactoringCount]:
    """How many refactorings each open or merged PR's latest analysed push has, counted in SQL.

    Runs whose ``refactorings`` output isn't a list are skipped, so an unreadable latest output
    falls back to the push before it, as the refactoring report does. (The report also drops a
    list whose items don't validate; counting in SQL keeps report bodies, up to 10 MB each, out
    of the request, so such a list is still counted here.) ``new_refactorings`` are the ones
    that aren't in the push before, by type and description, the same rule as ``compare``; on a
    PR's first analysed push, every refactoring is new. PRs closed without merging aren't
    counted.
    """
    cursor = await connection.execute(
        """WITH runs AS (
               SELECT a.pull_request_id, a.finished_at, o.json_content -> 'refactorings' AS items,
                      row_number() OVER (PARTITION BY a.pull_request_id
                                         ORDER BY a.finished_at DESC, a.id DESC) AS position
               FROM analysis_runs a
               JOIN analysis_outputs o
                 ON o.analysis_run_id = a.id AND o.output_key = %s AND o.output_type = %s
               JOIN pull_requests p ON p.id = a.pull_request_id
               WHERE a.repository_id = %s AND a.analysis_type = %s
                 AND a.status = 'completed'
                 AND (p.state = 'open' OR p.merged_at IS NOT NULL)
                 AND jsonb_typeof(o.json_content -> 'refactorings') = 'array')
           SELECT latest.pull_request_id, latest.finished_at AS analysed_at,
                  jsonb_array_length(latest.items) AS refactorings,
                  (SELECT count(*) FROM jsonb_array_elements(latest.items) item
                   WHERE previous.items IS NULL OR NOT EXISTS (
                       SELECT 1 FROM jsonb_array_elements(previous.items) old
                       WHERE old ->> 'type' = item ->> 'type'
                         AND old ->> 'description' = item ->> 'description'))
                      AS new_refactorings
           FROM runs latest
           LEFT JOIN runs previous
             ON previous.pull_request_id = latest.pull_request_id AND previous.position = 2
           WHERE latest.position = 1""",
        (
            REFACTORING_RUNS.output_key,
            REFACTORING_RUNS.output_type,
            repository_id,
            REFACTORING_RUNS.analysis_type,
        ),
    )
    return [PullRefactoringCount.model_validate(row) for row in await cursor.fetchall()]
