"""The PR tab's insights (US-9), against real PostgreSQL.

Pixel's repository ``Pixel-Org/App`` has two PRs by alice:

- #7, reviewed on three pushes. The first push's code review kept findings ``a`` and ``b`` and
  withheld 1; the second kept ``b`` and ``c`` and withheld 2. A title edit then reviewed the
  same code again (``code_unchanged``), and reopening the PR reviewed that commit once more
  (``b``, ``c`` and ``d``, 3 withheld). The third push's code review failed, and its run
  names its commit only in ``child_commit_sha``. The latest run failed.
- #8, never reviewed.
"""

# AI contribution: 50%

import asyncio
from collections.abc import Mapping
from datetime import UTC, datetime
from uuid import UUID, uuid4

import pytest
from fastapi.testclient import TestClient
from psycopg import AsyncConnection
from psycopg.rows import DictRow
from psycopg.types.json import Jsonb

from app.agents.base import AgentResult, PullRequestRef
from app.db import connect
from tests.api.courses_support import SignIn, TwoTeams, course_with_two_teams

PULLS = "/api/repos/Pixel-Org/App/pulls"
INSIGHTS = f"{PULLS}/7/insights"
FIRST_SHA = "aaaaaaa1111111111111111111111111111111111"
SECOND_SHA = "bbbbbbb2222222222222222222222222222222222"
THIRD_SHA = "ccccccc3333333333333333333333333333333333"
RESULT = AgentResult(
    agent="stub",
    prompt_version="stub-v1",
    model="test-model",
    provider=None,
    generation_id="gen-1",
    content="Review",
    finish_reason="stop",
    latency_ms=10,
    usage=None,
    pull_request=PullRequestRef(repository="Pixel-Org/App", number=7, author="alice"),
    created_at=datetime(2026, 10, 1, tzinfo=UTC),
).model_dump(mode="json")


async def _pr(c: AsyncConnection[DictRow], repo: UUID, number: int) -> UUID:
    cursor = await c.execute(
        """INSERT INTO pull_requests (repository_id, github_pr_id, pr_number, author_user_id,
               title, state, github_created_at, github_updated_at)
           SELECT %s, %s, %s, id, %s, 'open', '2026-10-01T00:00:00Z', '2026-10-02T00:00:00Z'
           FROM users WHERE github_login = 'alice' RETURNING id""",
        (repo, 9000 + number, number, f"PR {number}"),
    )
    row = await cursor.fetchone()
    assert row is not None
    return UUID(str(row["id"]))


async def _review(
    c: AsyncConnection[DictRow],
    repo: UUID,
    pr: UUID,
    created: str,
    head_sha: str,
    code_review: Mapping[str, object],
    finding_keys: tuple[str, ...] = (),
    status: str = "completed",
) -> None:
    """A review run started by a webhook event naming ``head_sha``, or, when ``code_review``
    failed, a run that names it only in ``child_commit_sha``."""
    by_event = code_review.get("not_run") != "RateLimited"
    event = uuid4() if by_event else None
    if by_event:
        await c.execute(
            """INSERT INTO webhook_events (id, delivery_id, repository_id, pull_request_id,
                   event_type, event_action, payload, status, received_at)
               VALUES (%s, gen_random_uuid(), %s, %s, 'pull_request', 'synchronize', %s,
                       'processed', %s)""",
            (event, repo, pr, Jsonb({"pull_request": {"head": {"sha": head_sha}}}), created),
        )
    cursor = await c.execute(
        """INSERT INTO analysis_runs (repository_id, team_id_at_run, pull_request_id,
               trigger_event_id, analysis_type, tool_name, pr_state_at_run, pr_merged_at_run,
               child_commit_sha, comparison_basis, status, created_at)
           SELECT id, team_id, %s, %s, 'llm_review', 'stub', 'open', false, %s, %s, %s, %s
           FROM repositories WHERE id = %s RETURNING id""",
        (
            pr,
            event,
            None if by_event else head_sha,
            None if by_event else "merge_base",
            status,
            created,
            repo,
        ),
    )
    run = await cursor.fetchone()
    assert run is not None
    if status != "completed":
        return
    cursor = await c.execute(
        """INSERT INTO analysis_outputs (analysis_run_id, output_key, output_type, json_content,
               text_content, metadata)
           VALUES (%s, 'review', 'agent_result', %s, 'Review', %s) RETURNING id""",
        (run["id"], Jsonb(RESULT), Jsonb({"code_review": code_review})),
    )
    output = await cursor.fetchone()
    assert output is not None
    for rank, key in enumerate(finding_keys):
        await c.execute(
            """INSERT INTO findings (analysis_output_id, finding_key, finding_type, name,
                   severity, details)
               VALUES (%s, %s, 'bug', %s, 'medium', %s)""",
            (output["id"], key, f"Finding {key}", Jsonb({"rank": rank})),
        )


async def _seed(url: str, team: str) -> None:
    async with await connect(url) as c, c.transaction():
        cursor = await c.execute(
            """INSERT INTO repositories (team_id, github_repository_id, full_name)
               VALUES (%s, 500, 'Pixel-Org/App') RETURNING id""",
            (team,),
        )
        row = await cursor.fetchone()
        assert row is not None
        repo = UUID(str(row["id"]))
        pr7 = await _pr(c, repo, 7)
        await _pr(c, repo, 8)
        first = {"withheld_count": 1}
        await _review(c, repo, pr7, "2026-10-01T10:00:00Z", FIRST_SHA, first, ("a", "b"))
        second = {"withheld_count": 2}
        await _review(c, repo, pr7, "2026-10-01T11:00:00Z", SECOND_SHA, second, ("b", "c"))
        edit = {"not_run": "code_unchanged"}
        await _review(c, repo, pr7, "2026-10-01T12:00:00Z", SECOND_SHA, edit)
        reopened = {"withheld_count": 3}
        await _review(c, repo, pr7, "2026-10-01T12:30:00Z", SECOND_SHA, reopened, ("b", "c", "d"))
        failed = {"not_run": "RateLimited"}
        await _review(c, repo, pr7, "2026-10-01T12:45:00Z", THIRD_SHA, failed)
        await _review(c, repo, pr7, "2026-10-01T13:00:00Z", SECOND_SHA, {}, status="failed")


@pytest.fixture
def setup(sign_in: SignIn, database_url: str) -> TwoTeams:
    teams = course_with_two_teams(sign_in)
    asyncio.run(_seed(database_url, teams.pixel))
    return teams


@pytest.mark.usefixtures("setup")
def test_a_student_gets_every_finding_open_and_no_marking(sign_in: SignIn) -> None:
    alice = sign_in("alice")

    body = alice.get(INSIGHTS).json()
    findings = alice.get(f"{PULLS}/7").json()["findings"]

    assert body["viewer"] == {"login": "alice", "role": "student"}
    assert [entry["finding_id"] for entry in body["checklist"]] == [f["id"] for f in findings]
    assert [f["name"] for f in findings] == ["Finding b", "Finding c", "Finding d"]
    assert {(entry["status"], entry["fixed_in"]) for entry in body["checklist"]} == {("open", None)}
    assert (body["your_review"], body["marking"]) == (None, None)


def _answer(client: TestClient, finding_id: str, action: str) -> None:
    body = {"action": action, "reason": "It is hashed." if action == "dispute" else ""}
    assert client.post(f"/api/findings/{finding_id}/actions", json=body).status_code == 204


def _statuses(client: TestClient) -> list[str]:
    return [entry["status"] for entry in client.get(INSIGHTS).json()["checklist"]]


@pytest.mark.usefixtures("setup")
def test_each_item_shows_the_teams_stored_answer(sign_in: SignIn) -> None:
    alice = sign_in("alice")
    b, c, _ = (entry["finding_id"] for entry in alice.get(INSIGHTS).json()["checklist"])

    _answer(alice, b, "acknowledge")
    _answer(alice, c, "dispute")

    assert _statuses(alice) == ["acknowledged", "disputed", "open"]


@pytest.mark.usefixtures("setup")
def test_the_latest_answer_wins(sign_in: SignIn) -> None:
    alice = sign_in("alice")
    b, c, _ = (entry["finding_id"] for entry in alice.get(INSIGHTS).json()["checklist"])

    _answer(alice, b, "dispute")
    _answer(alice, b, "acknowledge")
    _answer(alice, c, "acknowledge")
    _answer(alice, c, "dispute")

    assert _statuses(alice) == ["acknowledged", "disputed", "open"]


@pytest.mark.usefixtures("setup")
def test_withheld_count_and_pushes_come_from_the_stored_runs(sign_in: SignIn) -> None:
    body = sign_in("alice").get(INSIGHTS).json()

    # The failed code review left the reopened review's checklist and count in place.
    assert body["withheld_count"] == 3
    # One push per reviewed commit, told by its newest review: the second push added c and d
    # to the first push's checklist. The third push's code review didn't run, so it isn't one.
    assert body["pushes"] == [
        {"commit": "bbbbbbb", "message": "PR 7", "fixed": 0, "added": 2},
        {"commit": "aaaaaaa", "message": "PR 7", "fixed": 0, "added": 2},
    ]


@pytest.mark.usefixtures("setup")
def test_a_pr_without_reviews_has_empty_insights(sign_in: SignIn) -> None:
    body = sign_in("alice").get(f"{PULLS}/8/insights").json()

    assert (body["checklist"], body["withheld_count"], body["pushes"]) == ([], 0, [])


def test_the_instructor_and_a_ta_get_their_staff_role(setup: TwoTeams, sign_in: SignIn) -> None:
    ta = sign_in("helper")
    setup.instructor.post(
        f"/api/courses/{setup.course_id}/roster",
        json={"entries": [{"github_login": "helper", "role": "ta"}]},
    )

    instructor = setup.instructor.get(INSIGHTS).json()
    assistant = ta.get(INSIGHTS).json()

    assert instructor["viewer"] == {"login": "prof", "role": "instructor"}
    assert assistant["viewer"] == {"login": "helper", "role": "ta"}
    assert instructor["marking"] is None


@pytest.mark.usefixtures("setup")
def test_access_is_checked_before_the_pr_is_read(sign_in: SignIn) -> None:
    signed_out = sign_in("alice")
    signed_out.cookies.clear()
    alice, bob = sign_in("alice"), sign_in("bob")

    repo = alice.get("/api/repos/nobody/nothing/pulls/7/insights")
    pr = alice.get(f"{PULLS}/99/insights")

    assert signed_out.get(INSIGHTS).status_code == 401
    assert bob.get(INSIGHTS).status_code == 403
    assert bob.get(f"{PULLS}/99/insights").status_code == 403  # No hint whether it exists.
    assert (repo.status_code, repo.json()) == (
        404,
        {"detail": {"code": "repository_not_registered"}},
    )
    assert (pr.status_code, pr.json()) == (404, {"detail": {"code": "pull_request_not_found"}})


@pytest.mark.parametrize("stored", ['"many"', "-1", "1e10"])
def test_a_withheld_count_that_is_not_a_sensible_count_reads_as_0(
    setup: TwoTeams, sign_in: SignIn, database_url: str, stored: str
) -> None:
    async def corrupt() -> None:
        async with await connect(database_url) as c:
            await c.execute(
                """UPDATE analysis_outputs
                   SET metadata = jsonb_build_object(
                       'code_review', jsonb_build_object('withheld_count', %s::jsonb))
                   WHERE metadata #>> '{code_review,withheld_count}' = '3'""",
                (stored,),
            )

    asyncio.run(corrupt())

    assert sign_in("alice").get(INSIGHTS).json()["withheld_count"] == 0


def test_an_unreadable_latest_review_hides_its_withheld_count_with_its_checklist(
    setup: TwoTeams, sign_in: SignIn, database_url: str
) -> None:
    async def corrupt() -> None:
        async with await connect(database_url) as c:
            await c.execute(
                """UPDATE analysis_outputs SET json_content = '{"model": 1}'
                   WHERE metadata #>> '{code_review,not_run}' = 'RateLimited'"""
            )

    asyncio.run(corrupt())

    body = sign_in("alice").get(INSIGHTS).json()

    assert (body["checklist"], body["withheld_count"]) == ([], 0)


def test_a_fixed_item_shows_its_fix_and_counts_on_its_push(
    setup: TwoTeams, sign_in: SignIn, database_url: str
) -> None:
    async def fix_c() -> None:
        async with await connect(database_url) as c:
            await c.execute(
                """UPDATE findings SET details = details || %s
                   WHERE finding_key = 'c' AND analysis_output_id = (
                       SELECT analysis_output_id FROM findings WHERE finding_key = 'd')""",
                (
                    Jsonb(
                        {
                            "fixed_in": {
                                "commit": SECOND_SHA,
                                "file_path": "src/app.py",
                                "start_line": 4,
                                "end_line": 6,
                            }
                        }
                    ),
                ),
            )

    asyncio.run(fix_c())

    body = sign_in("alice").get(INSIGHTS).json()

    assert [(entry["status"], entry["fixed_in"]) for entry in body["checklist"]] == [
        ("open", None),
        (
            "fixed",
            {
                "commit": "bbbbbbb",
                "location": {"file_path": "src/app.py", "start_line": 4, "end_line": 6},
            },
        ),
        ("open", None),
    ]
    assert [(push["commit"], push["fixed"]) for push in body["pushes"]] == [
        ("bbbbbbb", 1),
        ("aaaaaaa", 0),
    ]


def test_an_answer_on_an_earlier_review_stays_with_the_item(
    setup: TwoTeams, sign_in: SignIn, database_url: str
) -> None:
    async def acknowledge_the_first_reviews_b() -> None:
        # Saved while the first push's review was the checklist (it is the one with "a").
        async with await connect(database_url) as c:
            await c.execute(
                """INSERT INTO user_actions (user_id, analysis_output_id, finding_id, action_type)
                   SELECT u.id, f.analysis_output_id, f.id, 'acknowledged'
                   FROM findings f CROSS JOIN users u
                   WHERE f.finding_key = 'b' AND u.github_login = 'alice'
                     AND f.analysis_output_id = (
                         SELECT analysis_output_id FROM findings WHERE finding_key = 'a')"""
            )

    asyncio.run(acknowledge_the_first_reviews_b())

    # The checklist is a later review's b, c and d: b keeps its answer by its key.
    assert _statuses(sign_in("alice")) == ["acknowledged", "open", "open"]
