"""Save and read the static findings of each analysed head commit of a PR (US-15).

Each analysed head commit has one ``static_analysis`` run, which a re-run on the same commit
replaces (``save_commit_analysis``). Callers own their connections.
"""

# AI contribution: 50%

from typing import Final

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db.models.static_findings import (
    NewStaticAnalysisRun,
    StaticFinding,
    StoredStaticFindings,
)
from app.db.repositories.commit_analyses import AnalysisKind, save_commit_analysis

STATIC_ANALYSIS_RUNS: Final = AnalysisKind(
    analysis_type="static_analysis",
    tool_name="Semgrep",
    output_key="static_findings",
    output_type="semgrep",
    schema_version="1",
)


async def save_static_analysis_run(
    connection: AsyncConnection[DictRow], run: NewStaticAnalysisRun
) -> bool:
    """Save the findings of one analysed head commit. False when NikolAI doesn't store the PR."""
    content = StoredStaticFindings(findings=run.findings).model_dump()
    return await save_commit_analysis(connection, STATIC_ANALYSIS_RUNS, run, content)


async def static_findings_for_commit(
    connection: AsyncConnection[DictRow], github_repository_id: int, pr_number: int, head_sha: str
) -> list[StaticFinding] | None:
    """The static findings stored for the PR's commit ``head_sha``, or None if there are none.

    An empty list means the commit was analysed and nothing was found on the lines the PR
    changed; None means no analysis of that commit was stored, so static analysis wasn't
    available for it.
    """
    cursor = await connection.execute(
        """SELECT o.json_content FROM analysis_runs a
           JOIN analysis_outputs o
             ON o.analysis_run_id = a.id AND o.output_key = %s AND o.output_type = %s
           JOIN pull_requests p ON p.id = a.pull_request_id
           JOIN repositories r ON r.id = p.repository_id
           WHERE r.github_repository_id = %s AND p.pr_number = %s
             AND a.analysis_type = %s AND a.child_commit_sha = %s AND a.status = 'completed'""",
        (
            STATIC_ANALYSIS_RUNS.output_key,
            STATIC_ANALYSIS_RUNS.output_type,
            github_repository_id,
            pr_number,
            STATIC_ANALYSIS_RUNS.analysis_type,
            head_sha,
        ),
    )
    row = await cursor.fetchone()
    if row is None:
        return None
    return StoredStaticFindings.model_validate(row["json_content"]).findings
