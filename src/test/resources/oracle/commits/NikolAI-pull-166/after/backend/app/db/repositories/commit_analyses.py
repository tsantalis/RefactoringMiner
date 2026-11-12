"""Save one analysis run per analysed head commit of a PR (US-8, US-15).

Runs are stored in ``analysis_runs`` and ``analysis_outputs``. Each analysed head commit of a PR
has one run of each kind, such as RefactoringMiner's refactorings or Semgrep's findings. Another
Action run on the same commit (a re-run) replaces that run's output instead of adding a run, so
consecutive runs of a PR always compare two different pushes. Callers own their connections.
"""

# AI contribution: 50%

from dataclasses import dataclass
from typing import cast
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow
from psycopg.types.json import Jsonb

from app.db.models.commit_analyses import NewCommitAnalysis


@dataclass(frozen=True, slots=True)
class AnalysisKind:
    """Which analysis a run holds, and the output its content is saved as."""

    analysis_type: str
    tool_name: str
    output_key: str
    output_type: str
    schema_version: str


async def save_commit_analysis(
    connection: AsyncConnection[DictRow],
    kind: AnalysisKind,
    analysis: NewCommitAnalysis,
    content: dict[str, object],
) -> bool:
    """Save ``content`` as the ``kind`` run of the analysed head commit.

    Returns False when NikolAI doesn't store the PR: PRs are stored by the ``pull_request``
    webhook, so a PR opened before the App was installed has no row yet; its next push stores it.
    """
    async with connection.transaction():
        pr = await stored_pull_request(
            connection, analysis.github_repository_id, analysis.pr_number
        )
        if pr is None:
            return False
        # Two deliveries for the same commit must not both insert a run.
        await connection.execute(
            "SELECT pg_advisory_xact_lock(hashtextextended(%s, 0))",
            (f"{kind.analysis_type}:{pr['id']}:{analysis.head_sha}",),
        )
        cursor = await connection.execute(
            """SELECT id FROM analysis_runs WHERE pull_request_id = %s
               AND analysis_type = %s AND child_commit_sha = %s""",
            (pr["id"], kind.analysis_type, analysis.head_sha),
        )
        existing = await cursor.fetchone()
        if existing is None:
            await _insert_run(connection, kind, analysis, pr, Jsonb(content))
        else:
            run_id = cast(UUID, existing["id"])
            await _replace_output(connection, kind, analysis, run_id, Jsonb(content))
    return True


async def stored_pull_request(
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
    connection: AsyncConnection[DictRow],
    kind: AnalysisKind,
    analysis: NewCommitAnalysis,
    pr: DictRow,
    content: Jsonb,
) -> None:
    cursor = await connection.execute(
        """INSERT INTO analysis_runs
           (repository_id, team_id_at_run, pull_request_id, analysis_type, tool_name,
            configuration, parent_commit_sha, child_commit_sha, comparison_basis,
            pr_state_at_run, pr_merged_at_run, status, started_at, finished_at)
           VALUES (%s, %s, %s, %s, %s, %s, %s, %s, 'merge_base', %s, %s,
                   'completed', now(), now())
           RETURNING id""",
        (
            pr["repository_id"],
            pr["team_id"],
            pr["id"],
            kind.analysis_type,
            kind.tool_name,
            Jsonb({"workflow_run_id": analysis.workflow_run_id}),
            analysis.base_sha,
            analysis.head_sha,
            analysis.pr_state,
            analysis.pr_merged,
        ),
    )
    row = await cursor.fetchone()
    if row is None:  # pragma: no cover - INSERT ... RETURNING always returns the row.
        raise RuntimeError("The analysis run was not saved")
    await connection.execute(
        """INSERT INTO analysis_outputs
           (analysis_run_id, output_key, output_type, schema_version, json_content)
           VALUES (%s, %s, %s, %s, %s)""",
        (row["id"], kind.output_key, kind.output_type, kind.schema_version, content),
    )


async def _replace_output(
    connection: AsyncConnection[DictRow],
    kind: AnalysisKind,
    analysis: NewCommitAnalysis,
    run_id: UUID,
    content: Jsonb,
) -> None:
    await connection.execute(
        """UPDATE analysis_runs SET configuration = %s, parent_commit_sha = %s,
           pr_state_at_run = %s, pr_merged_at_run = %s, status = 'completed',
           finished_at = now() WHERE id = %s""",
        (
            Jsonb({"workflow_run_id": analysis.workflow_run_id}),
            analysis.base_sha,
            analysis.pr_state,
            analysis.pr_merged,
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
        (run_id, kind.output_key, kind.output_type, kind.schema_version, content),
    )
