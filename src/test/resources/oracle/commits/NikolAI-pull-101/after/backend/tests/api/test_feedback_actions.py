"""Acknowledging and disputing feedback through the API (US-10, task 10.4)."""

# AI contribution: 50%

import asyncio
from uuid import UUID, uuid4

from fastapi.testclient import TestClient

from app.db import connect
from tests.api.support import (
    STUDENT_ONE_ID,
    STUDENT_TWO_ID,
    WRITE_HEADERS,
    FakeGitHub,
    execute,
    import_roster,
    instructor_course,
    new_client,
    request,
    sign_in,
    team_named,
)


async def _course_with_two_teams(
    database_url: str, client: TestClient, github: FakeGitHub
) -> tuple[TestClient, TestClient, UUID]:
    """An instructor and one signed-in student per team."""
    course_id, _ = await instructor_course(client, github, database_url)
    await import_roster(
        client,
        course_id,
        [
            {"github_login": "student1", "role": "student", "team": "Team A"},
            {"github_login": "student2", "role": "student", "team": "Team B"},
        ],
    )
    one, two = new_client(), new_client()
    await asyncio.to_thread(sign_in, one, github, STUDENT_ONE_ID, "student1")
    await asyncio.to_thread(sign_in, two, github, STUDENT_TWO_ID, "student2")
    return one, two, await team_named(database_url, "Team A")


async def _seed_feedback(database_url: str, team_id: UUID) -> UUID:
    """A stored review output for a PR owned by Team A."""
    run_id, output_id = uuid4(), uuid4()
    async with await connect(database_url) as connection, connection.transaction():
        author = await (
            await connection.execute("SELECT id FROM users WHERE github_login = 'student1'")
        ).fetchone()
        repository = await (
            await connection.execute(
                """INSERT INTO repositories (team_id, github_repository_id, full_name)
                   VALUES (%s, 900, 'octocat/feedback') RETURNING id""",
                (team_id,),
            )
        ).fetchone()
        assert author is not None
        assert repository is not None
        pull_request = await (
            await connection.execute(
                """INSERT INTO pull_requests
                       (repository_id, github_pr_id, pr_number, author_user_id, title, state,
                        github_created_at, github_updated_at)
                   VALUES (%s, 9001, 1, %s, 'A PR', 'open', now(), now()) RETURNING id""",
                (repository["id"], author["id"]),
            )
        ).fetchone()
        assert pull_request is not None
        await connection.execute(
            """INSERT INTO analysis_runs
                   (id, repository_id, team_id_at_run, pull_request_id, analysis_type, tool_name,
                    pr_state_at_run, pr_merged_at_run, status)
               VALUES (%s, %s, %s, %s, 'llm_review', 'stub', 'open', false, 'completed')""",
            (run_id, repository["id"], team_id, pull_request["id"]),
        )
        await connection.execute(
            """INSERT INTO analysis_outputs
                   (id, analysis_run_id, output_key, output_type, text_content)
               VALUES (%s, %s, 'review', 'agent_result', 'Looks good')""",
            (output_id, run_id),
        )
    return output_id


async def test_a_team_member_acknowledges_and_disputes(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    one, _, team_id = await _course_with_two_teams(database_url, client, github)
    output_id = await _seed_feedback(database_url, team_id)
    path = f"/api/feedback/{output_id}/actions"

    acknowledged = await request(
        one,
        "POST",
        path,
        json={"action": "acknowledged", "comment": "Will fix"},
        headers=WRITE_HEADERS,
    )
    disputed = await request(one, "POST", path, json={"action": "disputed"}, headers=WRITE_HEADERS)
    rows = await execute(
        database_url, "SELECT action_type, comment FROM user_actions ORDER BY action_type"
    )

    assert acknowledged.status_code == 201
    assert acknowledged.json()["action_type"] == "acknowledged"
    assert disputed.status_code == 201
    assert disputed.json()["action_type"] == "disputed"
    assert rows == [
        {"action_type": "acknowledged", "comment": "Will fix"},
        {"action_type": "disputed", "comment": None},
    ]


async def test_other_teams_and_bad_input_are_refused(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    one, two, team_id = await _course_with_two_teams(database_url, client, github)
    output_id = await _seed_feedback(database_url, team_id)
    path = f"/api/feedback/{output_id}/actions"
    headers = WRITE_HEADERS

    other_team = await request(two, "POST", path, json={"action": "acknowledged"}, headers=headers)
    instructor = await request(
        client, "POST", path, json={"action": "acknowledged"}, headers=headers
    )
    unknown = await request(
        one,
        "POST",
        f"/api/feedback/{uuid4()}/actions",
        json={"action": "acknowledged"},
        headers=headers,
    )
    bad_finding = await request(
        one,
        "POST",
        path,
        json={"action": "acknowledged", "finding_id": str(uuid4())},
        headers=headers,
    )

    assert other_team.status_code == 403
    assert instructor.status_code == 403
    assert unknown.status_code == 404
    assert bad_finding.status_code == 400


async def test_feedback_actions_need_a_session_and_an_allowed_origin(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    one, _, team_id = await _course_with_two_teams(database_url, client, github)
    output_id = await _seed_feedback(database_url, team_id)
    path = f"/api/feedback/{output_id}/actions"

    signed_out = await request(
        new_client(),
        "POST",
        path,
        json={"action": "acknowledged"},
        headers=WRITE_HEADERS,
    )
    missing_origin = await request(one, "POST", path, json={"action": "acknowledged"})

    assert signed_out.status_code == 401
    assert missing_origin.status_code == 403
