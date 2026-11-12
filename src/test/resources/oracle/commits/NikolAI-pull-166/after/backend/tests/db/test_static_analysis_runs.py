"""Real SQL tests of the database's rules for static analysis runs (US-15). Need TEST_DATABASE_URL.

A static analysis run analyses one commit of a repository, as a refactoring run does, so the
database refuses one that doesn't say which. PostgreSQL checks a table's CHECK constraints in
alphabetical order by name, so each test names the constraint that must refuse its row. The
conftest's database has one repository, GitHub ID 100, on one team.
"""

# AI contribution: 50%

import pytest
from psycopg import errors

from app.db import connect

HEAD_SHA = "0123456789abcdef0123456789abcdef01234567"


async def test_a_static_analysis_run_needs_the_commit_it_was_compared_with(
    database_url: str,
) -> None:
    async with await connect(database_url) as connection:
        with pytest.raises(errors.CheckViolation) as refused:
            await connection.execute(
                """INSERT INTO analysis_runs (repository_id, team_id_at_run, analysis_type,
                       tool_name, child_commit_sha, comparison_basis)
                   SELECT id, team_id, 'static_analysis', 'Semgrep', %s, 'merge_base'
                   FROM repositories WHERE github_repository_id = 100""",
                (HEAD_SHA,),
            )
    assert refused.value.diag.constraint_name == "analysis_runs_parent_sha_chk"


async def test_a_static_analysis_run_needs_a_repository_and_a_commit(database_url: str) -> None:
    async with await connect(database_url) as connection:
        with pytest.raises(errors.CheckViolation) as refused:
            await connection.execute(
                """INSERT INTO analysis_runs (analysis_type, tool_name)
                   VALUES ('static_analysis', 'Semgrep')"""
            )
    assert refused.value.diag.constraint_name == "analysis_runs_code_analysis_chk"
