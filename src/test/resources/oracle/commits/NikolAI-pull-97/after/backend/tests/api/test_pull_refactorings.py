"""The refactorings read API (US-8) and the runs it reads, against real PostgreSQL.

Pixel's repository ``Pixel-Org/App`` has PR #7, analysed on two pushes, and PR #8, which
RefactoringMiner hasn't analysed yet. Runs are saved with ``save_refactoring_run``, the
function the webhook job uses.
"""

# AI contribution: 50%

import asyncio
from uuid import UUID

import pytest

from app.db import connect
from app.db.models.refactorings import NewRefactoringRun, StoredRefactoring, StoredRefactorings
from app.db.repositories.refactorings import save_refactoring_run
from tests.api.courses_support import SignIn, TwoTeams, course_with_two_teams, run_sql

URL = "/api/repos/Pixel-Org/App/pulls"
GITHUB_REPO_ID = 500


def refactoring(kind: str, description: str, line: int = 10) -> StoredRefactoring:
    return StoredRefactoring(
        type=kind,
        description=description,
        file_path="src/app.py",
        start_line=line,
        end_line=line + 4,
        side="child",
    )


EXTRACT = refactoring("Extract Method", "check() extracted from login()")
RENAME = refactoring("Rename Method", "go() renamed to submit()", 30)
MOVE = refactoring("Move Class", "Form moved to forms", 1)


def run(head: str, *items: StoredRefactoring, number: int = 7) -> NewRefactoringRun:
    return NewRefactoringRun(
        github_repository_id=GITHUB_REPO_ID,
        pr_number=number,
        head_sha=head,
        base_sha="base000",
        pr_state="open",
        pr_merged=False,
        workflow_run_id=1,
        refactorings=StoredRefactorings(refactorings=list(items)),
    )


async def save(url: str, new_run: NewRefactoringRun) -> bool:
    async with await connect(url) as connection:
        return await save_refactoring_run(connection, new_run)


def seed(url: str, team: str) -> None:
    rows = run_sql(
        url,
        """INSERT INTO repositories (team_id, github_repository_id, full_name)
           VALUES (%s, %s, 'Pixel-Org/App') RETURNING id""",
        (team, GITHUB_REPO_ID),
    )
    repo = UUID(str(rows[0]["id"]))
    for number in (7, 8):
        run_sql(
            url,
            """INSERT INTO pull_requests (repository_id, github_pr_id, pr_number, author_user_id,
                   title, state, github_created_at, github_updated_at)
               SELECT %s, %s, %s, id, 'A PR', 'open', now(), now()
               FROM users WHERE github_login = 'alice'""",
            (repo, 9000 + number, number),
        )
    assert asyncio.run(save(url, run("aaa1111", EXTRACT, MOVE)))
    assert asyncio.run(save(url, run("bbb2222", EXTRACT, RENAME)))


@pytest.fixture
def setup(sign_in: SignIn, database_url: str) -> TwoTeams:
    teams = course_with_two_teams(sign_in)
    seed(database_url, teams.pixel)
    return teams


pytestmark = pytest.mark.usefixtures("setup")


def test_the_latest_push_is_compared_with_the_one_before(sign_in: SignIn) -> None:
    response = sign_in("alice").get(f"{URL}/7/refactorings")

    assert response.status_code == 200
    body = response.json()
    assert (body["commit"], body["since_commit"]) == ("bbb2222", "aaa1111")
    assert [(item["type"], item["change"]) for item in body["items"]] == [
        ("Extract Method", "unchanged"),
        ("Rename Method", "new"),
        ("Move Class", "undone"),
    ]
    assert body["items"][1] == {
        "type": "Rename Method",
        "description": "go() renamed to submit()",
        "change": "new",
        "file_path": "src/app.py",
        "start_line": 30,
        "side": "child",
    }


def test_a_rerun_on_the_same_commit_replaces_its_report(sign_in: SignIn, database_url: str) -> None:
    assert asyncio.run(save(database_url, run("bbb2222", RENAME)))

    body = sign_in("alice").get(f"{URL}/7/refactorings").json()

    assert body["since_commit"] == "aaa1111"
    assert [(item["type"], item["change"]) for item in body["items"]] == [
        ("Rename Method", "new"),
        ("Extract Method", "undone"),
        ("Move Class", "undone"),
    ]
    runs = run_sql(database_url, "SELECT count(*) AS n FROM analysis_runs")
    assert runs[0]["n"] == 2


def test_a_pr_not_analysed_yet_has_no_commit_and_no_items(sign_in: SignIn) -> None:
    body = sign_in("alice").get(f"{URL}/8/refactorings").json()

    assert body == {"commit": None, "since_commit": None, "items": []}


def test_a_pr_nikolai_does_not_store_is_not_saved(database_url: str) -> None:
    assert not asyncio.run(save(database_url, run("ccc3333", EXTRACT, number=99)))


def test_an_unreadable_output_is_skipped(sign_in: SignIn, database_url: str) -> None:
    run_sql(
        database_url,
        """UPDATE analysis_outputs SET json_content = '{"refactorings": "oops"}'
           WHERE analysis_run_id = (SELECT id FROM analysis_runs
                                    WHERE child_commit_sha = 'bbb2222')""",
    )

    body = sign_in("alice").get(f"{URL}/7/refactorings").json()

    assert (body["commit"], body["since_commit"]) == ("aaa1111", None)
    assert {item["change"] for item in body["items"]} == {"new"}


def test_unknown_pr_signed_out_and_another_team(sign_in: SignIn) -> None:
    alice = sign_in("alice")
    missing = alice.get(f"{URL}/99/refactorings")
    assert (missing.status_code, missing.json()) == (
        404,
        {"detail": {"code": "pull_request_not_found"}},
    )
    assert sign_in("bob").get(f"{URL}/7/refactorings").status_code == 403
    alice.cookies.clear()
    assert alice.get(f"{URL}/7/refactorings").status_code == 401


def test_the_instructor_reads_every_team(setup: TwoTeams) -> None:
    assert setup.instructor.get(f"{URL}/7/refactorings").status_code == 200
