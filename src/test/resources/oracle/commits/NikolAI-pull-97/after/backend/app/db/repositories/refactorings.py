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

from app.db.models.refactorings import NewRefactoringRun, StoredRefactoringRun

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
        cursor = await connection.execute(
            """SELECT p.id, p.repository_id, r.team_id FROM pull_requests p
               JOIN repositories r ON r.id = p.repository_id
               WHERE r.github_repository_id = %s AND p.pr_number = %s""",
            (run.github_repository_id, run.pr_number),
        )
        pr = await cursor.fetchone()
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
    connection: AsyncConnection[DictRow], pull_request_id: UUID, limit: int = 2
) -> list[StoredRefactoringRun]:
    """The PR's most recently analysed head commits, newest first, with their output."""
    cursor = await connection.execute(
        """SELECT a.child_commit_sha AS head_sha, o.json_content
           FROM analysis_runs a
           JOIN analysis_outputs o
             ON o.analysis_run_id = a.id AND o.output_key = %s AND o.output_type = %s
           WHERE a.pull_request_id = %s AND a.analysis_type = 'refactoring'
             AND a.status = 'completed'
           ORDER BY a.finished_at DESC, a.id DESC LIMIT %s""",
        (OUTPUT_KEY, OUTPUT_TYPE, pull_request_id, limit),
    )
    return [StoredRefactoringRun.model_validate(row) for row in await cursor.fetchall()]
