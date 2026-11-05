"""The team panel's read API (US-11.2), against real PostgreSQL.

Pixel (alice and carl) owns ``Pixel-Org/App``, with five PRs, all by alice:

- #8, open, updated last: a draft in its latest webhook event, and its newest review run was
  cancelled after one that failed.
- #7, open: reviewed, with 3 checklist findings, and analysed by RefactoringMiner on two pushes
  (2, then 3 refactorings, so 1 is new in the latest push).
- #6, merged: reviewed, with 4 checklist findings the panel doesn't count, and 1 refactoring
  analysed ten days ago.
- #5, merged: never reviewed, and no webhook event stored.
- #4, closed without merging: 5 refactorings, which the panel doesn't count.

Alpha (bob) owns ``Alpha-Org/Empty``, which has no PRs.
"""

# AI contribution: 50%

import asyncio
from uuid import UUID

import pytest

from app.db import connect
from app.db.models.refactorings import NewRefactoringRun, StoredRefactoring, StoredRefactorings
from app.db.repositories.refactorings import save_refactoring_run
from tests.api.courses_support import SignIn, TwoTeams, course_with_two_teams, run_sql

TEAM = "/api/repos/Pixel-Org/App/team"
GITHUB_REPO_ID = 600


def refactorings(count: int) -> StoredRefactorings:
    return StoredRefactorings(
        refactorings=[
            StoredRefactoring(
                type="Extract Method",
                description=f"helper{n}() extracted from run()",
                file_path="src/app.py",
                start_line=10 + n,
                end_line=12 + n,
                side="child",
            )
            for n in range(count)
        ]
    )


async def _save_run(url: str, number: int, head: str, count: int) -> None:
    async with await connect(url) as connection:
        saved = await save_refactoring_run(
            connection,
            NewRefactoringRun(
                github_repository_id=GITHUB_REPO_ID,
                pr_number=number,
                head_sha=head,
                base_sha="base000",
                pr_state="open",
                pr_merged=False,
                workflow_run_id=1,
                refactorings=refactorings(count),
            ),
        )
    assert saved


def _pull(url: str, repo: UUID, number: int, state: str, merged: bool, updated: str) -> UUID:
    rows = run_sql(
        url,
        """INSERT INTO pull_requests (repository_id, github_pr_id, pr_number, author_user_id,
               title, state, merged_at, github_created_at, github_updated_at)
           SELECT %s, %s, %s, id, %s, %s, CASE WHEN %s THEN now() END, '2026-10-01T00:00:00Z', %s
           FROM users WHERE github_login = 'alice' RETURNING id""",
        (repo, 7000 + number, number, f"PR {number}", state, merged, updated),
    )
    return UUID(str(rows[0]["id"]))


def _event(url: str, repo: UUID, pr: UUID, received: str, payload: str) -> None:
    run_sql(
        url,
        """INSERT INTO webhook_events (delivery_id, repository_id, pull_request_id, event_type,
               event_action, payload, status, received_at)
           VALUES (gen_random_uuid(), %s, %s, 'pull_request', 'synchronize', %s::jsonb,
                   'processed', %s)""",
        (repo, pr, payload, received),
    )


def _review(url: str, repo: UUID, team: str, pr: UUID, status: str, created: str) -> UUID:
    rows = run_sql(
        url,
        """INSERT INTO analysis_runs (repository_id, team_id_at_run, pull_request_id,
               analysis_type, tool_name, pr_state_at_run, pr_merged_at_run, status, created_at)
           VALUES (%s, %s, %s, 'llm_review', 'stub', 'open', false, %s, %s) RETURNING id""",
        (repo, team, pr, status, created),
    )
    return UUID(str(rows[0]["id"]))


def _checklist(url: str, run: UUID, findings: int, code_review: str = "{}") -> None:
    """Save the run's review output with ``findings`` findings; ``code_review`` is its metadata."""
    rows = run_sql(
        url,
        """INSERT INTO analysis_outputs (analysis_run_id, output_key, output_type, json_content,
               metadata)
           VALUES (%s, 'review', 'agent_result', '{}', jsonb_build_object('code_review', %s::jsonb))
           RETURNING id""",
        (run, code_review),
    )
    run_sql(
        url,
        """INSERT INTO findings (analysis_output_id, finding_key, finding_type, name)
           SELECT %s, 'finding-' || n, 'bug', 'Finding ' || n FROM generate_series(1, %s) n""",
        (rows[0]["id"], findings),
    )


def _reviewed(
    url: str, team: str, number: int, created: str, findings: int, code_review: str = "{}"
) -> None:
    """A completed review of PR ``number`` in ``Pixel-Org/App`` with its review output."""
    rows = run_sql(
        url,
        """SELECT p.repository_id, p.id FROM pull_requests p
           JOIN repositories r ON r.id = p.repository_id
           WHERE r.full_name = 'Pixel-Org/App' AND p.pr_number = %s""",
        (number,),
    )
    repo, pr = UUID(str(rows[0]["repository_id"])), UUID(str(rows[0]["id"]))
    _checklist(url, _review(url, repo, team, pr, "completed", created), findings, code_review)


def _checklists(body: dict[str, list[dict[str, object]]]) -> list[tuple[object, object, object]]:
    return [(p["number"], p["checklist_open"], p["checklist_total"]) for p in body["pulls"]]


def seed(url: str, teams: TwoTeams) -> None:
    rows = run_sql(
        url,
        """INSERT INTO repositories (team_id, github_repository_id, full_name)
           VALUES (%s, %s, 'Pixel-Org/App'), (%s, %s, 'Alpha-Org/Empty') RETURNING id""",
        (teams.pixel, GITHUB_REPO_ID, teams.alpha, GITHUB_REPO_ID + 1),
    )
    repo = UUID(str(rows[0]["id"]))
    p8 = _pull(url, repo, 8, "open", False, "2026-11-02T00:00:00Z")
    p7 = _pull(url, repo, 7, "open", False, "2026-11-01T00:00:00Z")
    p6 = _pull(url, repo, 6, "closed", True, "2026-10-20T00:00:00Z")
    _pull(url, repo, 5, "closed", True, "2026-10-10T00:00:00Z")
    _pull(url, repo, 4, "closed", False, "2026-10-05T00:00:00Z")
    _event(url, repo, p8, "2026-11-01T00:00:00Z", '{"pull_request": {"draft": false}}')
    _event(url, repo, p8, "2026-11-02T00:00:00Z", '{"pull_request": {"draft": true, "commits": 2}}')
    _event(url, repo, p7, "2026-11-01T00:00:00Z", '{"pull_request": {"commits": 3}}')
    _event(url, repo, p6, "2026-10-20T00:00:00Z", '{"pull_request": {"commits": 4}}')
    _review(url, repo, teams.pixel, p8, "failed", "2026-11-01T00:00:00Z")
    _review(url, repo, teams.pixel, p8, "cancelled", "2026-11-02T00:00:00Z")
    _checklist(url, _review(url, repo, teams.pixel, p7, "completed", "2026-11-01T00:00:00Z"), 3)
    _checklist(url, _review(url, repo, teams.pixel, p6, "completed", "2026-10-20T00:00:00Z"), 4)
    asyncio.run(_save_run(url, 6, "ccc3333", 1))
    run_sql(
        url,
        """UPDATE analysis_runs SET created_at = created_at - interval '10 days',
               started_at = started_at - interval '10 days',
               finished_at = finished_at - interval '10 days'
           WHERE child_commit_sha = 'ccc3333'""",
    )
    asyncio.run(_save_run(url, 7, "aaa1111", 2))
    asyncio.run(_save_run(url, 7, "bbb2222", 3))
    asyncio.run(_save_run(url, 4, "ddd4444", 5))


@pytest.fixture
def teams(sign_in: SignIn, database_url: str) -> TwoTeams:
    teams = course_with_two_teams(sign_in)
    course = teams.instructor.get(f"/api/courses/{teams.course_id}").json()
    sign_in("carl").post("/api/courses/join", json={"join_code": course["join_code"]})
    added = teams.instructor.post(
        f"/api/courses/{teams.course_id}/teams/{teams.pixel}/members",
        json={"github_login": "carl"},
    )
    assert added.status_code == 200
    seed(database_url, teams)
    return teams


def test_a_student_sees_their_teams_open_prs_members_and_stats(
    sign_in: SignIn, teams: TwoTeams
) -> None:
    response = sign_in("alice").get(TEAM)

    assert response.status_code == 200
    body = response.json()
    assert body["viewer"] == {"login": "alice", "role": "student"}
    assert body["team"] == {"name": "Pixel", "course": "SOEN 490"}
    assert [
        (p["number"], p["draft"], p["review_status"], p["refactorings"]) for p in body["pulls"]
    ] == [(8, True, "failed", 0), (7, False, "completed", 3)]
    assert body["pulls"][1]["author"] == "alice"
    assert body["stats"] == {
        "open_checklist_items": 3,
        "refactorings": 4,
        "refactorings_this_week": 1,
        "reviewed_pulls": 1,
        "merged_pulls": 2,
    }
    assert [(m["login"], m["pulls"], m["commits"]) for m in body["members"]] == [
        ("alice", 5, 9),
        ("carl", 0, 0),
    ]
    assert body["staff"] is None
    assert (body["iteration"], body["next_deliverables"]) == (None, [])
    assert body["badges"] == {"earned": [], "next": None}
    # #8 has no checklist review: null, never a made-up 0. Nor are peer reviews stored yet.
    assert _checklists(body) == [(8, None, None), (7, 3, 3)]
    assert {(m["reviews"], m["fixes_with_evidence"]) for m in body["members"]} == {(None, None)}


def test_a_pr_whose_newest_review_failed_counts_its_latest_completed_one(
    sign_in: SignIn, teams: TwoTeams, database_url: str
) -> None:
    _reviewed(database_url, teams.pixel, 8, "2026-10-30T00:00:00Z", 2)
    _reviewed(database_url, teams.pixel, 8, "2026-10-29T00:00:00Z", 5)

    body = sign_in("alice").get(TEAM).json()

    assert _checklists(body) == [(8, 2, 2), (7, 3, 3)]
    assert body["stats"]["open_checklist_items"] == 5


def test_a_review_whose_code_review_did_not_run_gives_no_checklist(
    sign_in: SignIn, teams: TwoTeams, database_url: str
) -> None:
    not_run = '{"not_run": "RateLimited"}'
    # A newer title-only review keeps #7's checklist; #8 was only ever reviewed that way.
    _reviewed(database_url, teams.pixel, 7, "2026-11-03T00:00:00Z", 0, not_run)
    _reviewed(database_url, teams.pixel, 8, "2026-10-30T00:00:00Z", 0, not_run)

    body = sign_in("alice").get(TEAM).json()

    assert _checklists(body) == [(8, None, None), (7, 3, 3)]


def test_a_reviewed_pr_with_no_findings_has_0_of_0(
    sign_in: SignIn, teams: TwoTeams, database_url: str
) -> None:
    run_sql(database_url, "DELETE FROM findings")

    body = sign_in("alice").get(TEAM).json()

    assert _checklists(body) == [(8, None, None), (7, 0, 0)]
    assert body["stats"]["open_checklist_items"] == 0


def test_a_push_that_adds_no_refactoring_adds_none_this_week(
    sign_in: SignIn, teams: TwoTeams, database_url: str
) -> None:
    asyncio.run(_save_run(database_url, 7, "eee5555", 3))

    body = sign_in("alice").get(TEAM).json()

    assert body["pulls"][1]["refactorings"] == 3
    assert (body["stats"]["refactorings"], body["stats"]["refactorings_this_week"]) == (4, 0)


def test_course_staff_get_the_staff_fields(
    sign_in: SignIn, teams: TwoTeams, database_url: str
) -> None:
    sign_in("tara")
    course = teams.instructor.get(f"/api/courses/{teams.course_id}").json()
    sign_in("tara").post("/api/courses/join", json={"join_code": course["join_code"]})
    run_sql(
        database_url,
        """UPDATE course_memberships SET role = 'ta' WHERE user_id =
           (SELECT id FROM users WHERE github_login = 'tara')""",
    )

    ta = sign_in("tara").get(TEAM).json()
    instructor = teams.instructor.get(TEAM).json()

    assert ta["viewer"] == {"login": "tara", "role": "ta"}
    assert instructor["viewer"]["role"] == "instructor"
    assert ta["staff"] == {
        "marking": None,
        "attention": [],
        "disputes": [],
        "stories_done": None,
        "stories_planned": None,
        "features_tested": None,
        "features_total": None,
        "ai_declared_percent": None,
    }
    assert instructor["pulls"] == ta["pulls"]


def test_a_team_with_no_prs_has_an_empty_panel(sign_in: SignIn, teams: TwoTeams) -> None:
    body = sign_in("bob").get("/api/repos/Alpha-Org/Empty/team").json()

    assert body["pulls"] == []
    assert body["stats"]["merged_pulls"] == 0
    assert body["stats"]["refactorings"] == 0
    assert body["stats"]["open_checklist_items"] is None
    assert [(m["login"], m["pulls"], m["commits"]) for m in body["members"]] == [("bob", 0, 0)]


def test_an_unreadable_latest_report_counts_the_push_before_it(
    sign_in: SignIn, teams: TwoTeams, database_url: str
) -> None:
    run_sql(
        database_url,
        """UPDATE analysis_outputs SET json_content = '{"refactorings": "oops"}'
           WHERE analysis_run_id = (SELECT id FROM analysis_runs
                                    WHERE child_commit_sha = 'bbb2222')""",
    )

    body = sign_in("alice").get(TEAM).json()

    assert body["pulls"][1]["refactorings"] == 2
    assert body["stats"]["refactorings"] == 3


def test_a_student_of_another_team_is_forbidden(sign_in: SignIn, teams: TwoTeams) -> None:
    assert sign_in("bob").get(TEAM).status_code == 403


def test_signed_out_is_unauthorized(sign_in: SignIn, teams: TwoTeams) -> None:
    client = sign_in("alice")
    client.cookies.clear()

    assert client.get(TEAM).status_code == 401


def test_an_unknown_repository_is_not_registered(sign_in: SignIn, teams: TwoTeams) -> None:
    response = sign_in("alice").get("/api/repos/Nobody/Nothing/team")

    assert response.status_code == 404
    assert response.json() == {"detail": {"code": "repository_not_registered"}}
