"""Save and read RefactoringMiner runs of a PR (US-8) in ``analysis_runs``/``analysis_outputs``.

Each analysed head commit of a PR has one ``refactoring`` run. Another Action run on the same
commit (a re-run) replaces that run's output instead of adding a run, so consecutive runs of a
PR always compare two different pushes. Callers own their connections.
"""

# AI contribution: 50%

from typing import Final, cast
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow
from psycopg.types.json import Jsonb

from app.db.models.refactorings import (
    NewRefactoringRun,
    PullRefactoringCount,
    StoredRefactoringRun,
)

TOOL_NAME: Final = "RefactoringMiner"
OUTPUT_KEY: Final = "refactorings"
OUTPUT_TYPE: Final = "refactoring_miner"
SCHEMA_VERSION: Final = "1"


async def save_refactoring_run(
    connection: AsyncConnection[DictRow], run: NewRefactoringRun
) -> bool:
    """Save the refactorings of one analysed head commit. False when NikolAI doesn't store the PR.

    PRs are stored by the ``pull_request`` webhook, so a PR opened before the App was installed
    has no row yet; its next push stores it.
    """
    async with connection.transaction():
        pr = await _stored_pull_request(connection, run.github_repository_id, run.pr_number)
        if pr is None:
            return False
        # Two deliveries for the same commit must not both insert a run.
        await connection.execute(
            "SELECT pg_advisory_xact_lock(hashtextextended(%s, 0))",
            (f"refactoring:{pr['id']}:{run.head_sha}",),
        )
        cursor = await connection.execute(
            """SELECT id FROM analysis_runs WHERE pull_request_id = %s
               AND analysis_type = 'refactoring' AND child_commit_sha = %s""",
            (pr["id"], run.head_sha),
        )
        existing = await cursor.fetchone()
        content = Jsonb(run.refactorings.model_dump())
        if existing is None:
            await _insert_run(connection, run, pr, content)
        else:
            await _replace_output(connection, run, cast(UUID, existing["id"]), content)
    return True


async def _stored_pull_request(
    connection: AsyncConnection[DictRow], github_repository_id: int, pr_number: int
) -> DictRow | None:
    """The stored PR ``pr_number`` of a GitHub repository: its id, repository and team."""
    cursor = await connection.execute(
        """SELECT p.id, p.repository_id, r.team_id FROM pull_requests p
           JOIN repositories r ON r.id = p.repository_id
           WHERE r.github_repository_id = %s AND p.pr_number = %s""",
        (github_repository_id, pr_number),
    )
    return await cursor.fetchone()


async def _insert_run(
    connection: AsyncConnection[DictRow], run: NewRefactoringRun, pr: DictRow, content: Jsonb
) -> None:
    cursor = await connection.execute(
        """INSERT INTO analysis_runs
           (repository_id, team_id_at_run, pull_request_id, analysis_type, tool_name,
            configuration, parent_commit_sha, child_commit_sha, comparison_basis,
            pr_state_at_run, pr_merged_at_run, status, started_at, finished_at)
           VALUES (%s, %s, %s, 'refactoring', %s, %s, %s, %s, 'merge_base', %s, %s,
                   'completed', now(), now())
           RETURNING id""",
        (
            pr["repository_id"],
            pr["team_id"],
            pr["id"],
            TOOL_NAME,
            Jsonb({"workflow_run_id": run.workflow_run_id}),
            run.base_sha,
            run.head_sha,
            run.pr_state,
            run.pr_merged,
        ),
    )
    row = await cursor.fetchone()
    if row is None:  # pragma: no cover - INSERT ... RETURNING always returns the row.
        raise RuntimeError("The refactoring run was not saved")
    await connection.execute(
        """INSERT INTO analysis_outputs
           (analysis_run_id, output_key, output_type, schema_version, json_content)
           VALUES (%s, %s, %s, %s, %s)""",
        (row["id"], OUTPUT_KEY, OUTPUT_TYPE, SCHEMA_VERSION, content),
    )


async def _replace_output(
    connection: AsyncConnection[DictRow], run: NewRefactoringRun, run_id: UUID, content: Jsonb
) -> None:
    await connection.execute(
        """UPDATE analysis_runs SET configuration = %s, parent_commit_sha = %s,
           pr_state_at_run = %s, pr_merged_at_run = %s, status = 'completed',
           finished_at = now() WHERE id = %s""",
        (
            Jsonb({"workflow_run_id": run.workflow_run_id}),
            run.base_sha,
            run.pr_state,
            run.pr_merged,
            run_id,
        ),
    )
    await connection.execute(
        """INSERT INTO analysis_outputs
           (analysis_run_id, output_key, output_type, schema_version, json_content)
           VALUES (%s, %s, %s, %s, %s)
           ON CONFLICT (analysis_run_id, output_key)
           DO UPDATE SET json_content = EXCLUDED.json_content,
                         schema_version = EXCLUDED.schema_version""",
        (run_id, OUTPUT_KEY, OUTPUT_TYPE, SCHEMA_VERSION, content),
    )


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
           WHERE a.pull_request_id = %s AND a.analysis_type = 'refactoring'
             AND a.status = 'completed' AND a.child_commit_sha IS DISTINCT FROM %s
           ORDER BY a.finished_at DESC, a.id DESC LIMIT %s""",
        (OUTPUT_KEY, OUTPUT_TYPE, pull_request_id, exclude_sha, limit),
    )
    return [StoredRefactoringRun.model_validate(row) for row in await cursor.fetchall()]


async def previous_refactoring_run(
    connection: AsyncConnection[DictRow], github_repository_id: int, pr_number: int, head_sha: str
) -> StoredRefactoringRun | None:
    """The PR's most recently analysed head commit other than ``head_sha``, with its output.

    Once ``head_sha`` is saved, this is the run the read API compares it with (``since_commit``).
    None before the PR's second analysed push, or when NikolAI doesn't store the PR.
    """
    pr = await _stored_pull_request(connection, github_repository_id, pr_number)
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
               WHERE a.repository_id = %s AND a.analysis_type = 'refactoring'
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
        (OUTPUT_KEY, OUTPUT_TYPE, repository_id),
    )
    return [PullRefactoringCount.model_validate(row) for row in await cursor.fetchall()]
