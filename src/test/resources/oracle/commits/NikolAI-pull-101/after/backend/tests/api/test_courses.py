"""Courses, teams and the roster through the HTTP API (US-10, ADR 0001 PR 2).

Requests run against real PostgreSQL; only GitHub is faked. Each user gets their own
``TestClient`` so the session cookies stay separate.
"""

# AI contribution: 50%

import asyncio

from fastapi.testclient import TestClient

from tests.api.support import (
    INSTRUCTOR_ID,
    STUDENT_ONE_ID,
    STUDENT_TWO_ID,
    TA_ID,
    WRITE_HEADERS,
    FakeGitHub,
    add_team_member,
    course_view,
    create_course,
    create_team,
    execute,
    import_roster,
    instructor_course,
    join_course,
    make_instructor,
    new_client,
    request,
    set_course_role,
    sign_in,
)


async def test_a_non_instructor_cannot_create_a_course(
    client: TestClient, github: FakeGitHub
) -> None:
    await asyncio.to_thread(sign_in, client, github, STUDENT_ONE_ID, "student1")

    response = await create_course(client)

    assert response.status_code == 403
    assert response.json() == {"detail": "Only instructors can create courses."}


async def test_an_instructor_creates_a_course_with_a_join_code(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    course_id, join_code = await instructor_course(client, github, database_url)
    detail = await course_view(client, course_id)

    assert detail.name == "SOEN 490"
    assert detail.role == "instructor"
    assert len(join_code) == 8
    assert set(join_code) <= set("23456789ABCDEFGHJKMNPQRSTUVWXYZ")
    rows = await execute(
        database_url,
        """SELECT m.role FROM course_memberships m
           JOIN users u ON u.id = m.user_id WHERE u.github_login = 'prof'""",
    )
    assert rows == [{"role": "instructor"}]


async def test_course_reads_require_membership(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    course_id, _ = await instructor_course(client, github, database_url)

    outsider = new_client()
    await asyncio.to_thread(sign_in, outsider, github, STUDENT_TWO_ID, "outsider")
    forbidden = await request(outsider, "GET", f"/api/courses/{course_id}")
    missing = await request(outsider, "GET", "/api/courses/00000000-0000-0000-0000-000000000000")
    signed_out = await request(new_client(), "GET", f"/api/courses/{course_id}")

    assert forbidden.status_code == 403
    assert missing.status_code == 404
    assert signed_out.status_code == 401


async def test_joining_twice_keeps_one_membership_and_hides_the_join_code(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    course_id, join_code = await instructor_course(client, github, database_url)

    student = new_client()
    await asyncio.to_thread(sign_in, student, github, STUDENT_ONE_ID, "student1")
    for _ in range(2):
        joined = await join_course(student, join_code)
        assert joined.status_code == 200
        assert joined.json()["role"] == "student"

    rows = await execute(
        database_url,
        """SELECT count(*) AS n FROM course_memberships m
           JOIN users u ON u.id = m.user_id
           WHERE u.github_login = 'student1' AND m.left_at IS NULL""",
    )
    detail = await course_view(student, course_id)
    bad_code = await join_course(student, "ZZZZZZZZ")

    assert rows == [{"n": 1}]
    assert detail.join_code is None
    assert detail.role == "student"
    assert bad_code.status_code == 400


async def test_rotating_the_join_code_invalidates_the_old_one(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    course_id, old_code = await instructor_course(client, github, database_url)
    first = new_client()
    await asyncio.to_thread(sign_in, first, github, STUDENT_ONE_ID, "student1")
    await join_course(first, old_code)

    rotated = await request(
        client, "POST", f"/api/courses/{course_id}/join-code", headers=WRITE_HEADERS
    )
    forbidden = await request(
        first, "POST", f"/api/courses/{course_id}/join-code", headers=WRITE_HEADERS
    )
    second = new_client()
    await asyncio.to_thread(sign_in, second, github, STUDENT_TWO_ID, "student2")
    old_join = await join_course(second, old_code)
    new_code = rotated.json()["join_code"]
    new_join = await join_course(second, new_code)

    assert rotated.status_code == 200
    assert new_code != old_code
    assert forbidden.status_code == 403
    assert old_join.status_code == 400
    assert new_join.status_code == 200


async def test_a_student_sees_only_their_own_team(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    course_id, join_code = await instructor_course(client, github, database_url)
    team_a = (await create_team(client, course_id, "Team A")).json()["id"]
    await create_team(client, course_id, "Team B")

    student = new_client()
    await asyncio.to_thread(sign_in, student, github, STUDENT_ONE_ID, "student1")
    await join_course(student, join_code)
    added = await add_team_member(
        client, course_id=course_id, team_id=team_a, login="student1", role="lead"
    )
    student_view = await course_view(student, course_id)

    assert added.status_code == 201
    assert added.json()["role"] == "lead"
    assert [team.name for team in student_view.teams] == ["Team A"]
    assert student_view.join_code is None


async def test_course_staff_see_every_team(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    course_id, join_code = await instructor_course(client, github, database_url)
    await create_team(client, course_id, "Team A")
    await create_team(client, course_id, "Team B")

    ta = new_client()
    await asyncio.to_thread(sign_in, ta, github, TA_ID, "ta1")
    await join_course(ta, join_code)
    await set_course_role(database_url, course_id=course_id, login="ta1", role="ta")

    ta_view = await course_view(ta, course_id)
    instructor_view = await course_view(client, course_id)

    assert [team.name for team in ta_view.teams] == ["Team A", "Team B"]
    assert [team.name for team in instructor_view.teams] == ["Team A", "Team B"]
    assert instructor_view.join_code is not None


async def test_a_team_is_readable_only_by_its_members_and_course_staff(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    course_id, join_code = await instructor_course(client, github, database_url)
    team_a = (await create_team(client, course_id, "Team A")).json()["id"]
    team_b = (await create_team(client, course_id, "Team B")).json()["id"]

    member = new_client()
    await asyncio.to_thread(sign_in, member, github, STUDENT_ONE_ID, "student1")
    await join_course(member, join_code)
    await add_team_member(client, course_id=course_id, team_id=team_a, login="student1")
    other = new_client()
    await asyncio.to_thread(sign_in, other, github, STUDENT_TWO_ID, "student2")
    await join_course(other, join_code)
    ta = new_client()
    await asyncio.to_thread(sign_in, ta, github, TA_ID, "ta1")
    await join_course(ta, join_code)
    await set_course_role(database_url, course_id=course_id, login="ta1", role="ta")

    own = await request(member, "GET", f"/api/courses/{course_id}/teams/{team_a}")
    other_team = await request(member, "GET", f"/api/courses/{course_id}/teams/{team_b}")
    other_student = await request(other, "GET", f"/api/courses/{course_id}/teams/{team_a}")
    staff = await request(ta, "GET", f"/api/courses/{course_id}/teams/{team_b}")
    missing = await request(
        member, "GET", f"/api/courses/{course_id}/teams/00000000-0000-0000-0000-000000000000"
    )

    assert own.status_code == 200
    assert [person["github_login"] for person in own.json()["members"]] == ["student1"]
    assert other_team.status_code == 403
    assert other_student.status_code == 403
    assert staff.status_code == 200
    assert missing.status_code == 404


async def test_a_duplicate_team_name_is_rejected(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    course_id, _ = await instructor_course(client, github, database_url)

    first = await create_team(client, course_id, "Team A")
    second = await create_team(client, course_id, "team a")

    assert first.status_code == 201
    assert second.status_code == 409


async def test_only_instructors_create_teams_and_import_the_roster(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    course_id, join_code = await instructor_course(client, github, database_url)
    student = new_client()
    await asyncio.to_thread(sign_in, student, github, STUDENT_ONE_ID, "student1")
    await join_course(student, join_code)

    team = await create_team(student, course_id, "Team A")
    roster = await import_roster(student, course_id, [])

    assert team.status_code == 403
    assert roster.status_code == 403


async def test_adding_a_team_member_requires_a_course_member(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    course_id, join_code = await instructor_course(client, github, database_url)
    team_id = (await create_team(client, course_id, "Team A")).json()["id"]
    student = new_client()
    await asyncio.to_thread(sign_in, student, github, STUDENT_ONE_ID, "student1")

    not_joined = await add_team_member(
        client, course_id=course_id, team_id=team_id, login="student1"
    )
    unknown = await add_team_member(client, course_id=course_id, team_id=team_id, login="nobody")
    await join_course(student, join_code)
    joined = await add_team_member(client, course_id=course_id, team_id=team_id, login="student1")

    assert not_joined.status_code == 400
    assert unknown.status_code == 400
    assert joined.status_code == 201


async def test_roster_import_reports_bad_rows_and_imports_the_rest(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    course_id, _ = await instructor_course(client, github, database_url)

    response = await import_roster(
        client,
        course_id,
        [
            {"github_login": "student1", "role": "student", "team": "Team A"},
            {"github_login": "not a login", "role": "student", "team": "Team A"},
            {"github_login": "STUDENT1", "role": "student", "team": "Team B"},
            {"github_login": "student2", "role": "student"},
            {"github_login": "ta1", "role": "ta"},
        ],
    )

    body = response.json()
    rows = await execute(
        database_url,
        "SELECT github_login, role FROM course_roster_entries ORDER BY github_login",
    )
    teams = await execute(database_url, "SELECT name FROM teams WHERE course_id = %s", (course_id,))
    assert response.status_code == 200
    assert body["imported"] == 2
    assert [error["row"] for error in body["errors"]] == [2, 3, 4]
    assert rows == [
        {"github_login": "student1", "role": "student"},
        {"github_login": "ta1", "role": "ta"},
    ]
    assert teams == [{"name": "Team A"}]


async def test_a_roster_login_is_claimed_on_first_sign_in(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    course_id, _ = await instructor_course(client, github, database_url)
    await import_roster(
        client,
        course_id,
        [
            {"github_login": "student1", "role": "student", "team": "Team A"},
            {"github_login": "ta1", "role": "ta"},
        ],
    )
    before = await execute(database_url, "SELECT claimed_by_user_id FROM course_roster_entries")

    student = new_client()
    await asyncio.to_thread(sign_in, student, github, STUDENT_ONE_ID, "student1")
    me = (await request(student, "GET", "/api/me")).json()
    claimed = await execute(
        database_url,
        """SELECT m.role AS course_role, t.name AS team
           FROM course_roster_entries e
           JOIN course_memberships m ON m.user_id = e.claimed_by_user_id
           JOIN team_memberships tm ON tm.user_id = e.claimed_by_user_id AND tm.left_at IS NULL
           JOIN teams t ON t.id = tm.team_id
           WHERE e.github_login = 'student1'""",
    )
    ta = new_client()
    await asyncio.to_thread(sign_in, ta, github, TA_ID, "ta1")
    ta_role = await execute(
        database_url,
        """SELECT m.role FROM course_memberships m
           JOIN users u ON u.id = m.user_id WHERE u.github_login = 'ta1'""",
    )

    assert before == [{"claimed_by_user_id": None}, {"claimed_by_user_id": None}]
    assert claimed == [{"course_role": "student", "team": "Team A"}]
    assert ta_role == [{"role": "ta"}]
    assert me["courses"][0]["name"] == "SOEN 490"
    assert [team["name"] for team in me["courses"][0]["teams"]] == ["Team A"]


async def test_writes_need_a_session_and_an_allowed_origin(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    signed_out = await request(
        new_client(), "POST", "/api/courses", json={"name": "SOEN 490"}, headers=WRITE_HEADERS
    )
    await asyncio.to_thread(sign_in, client, github, INSTRUCTOR_ID, "prof")
    await make_instructor(database_url, "prof")
    missing_origin = await request(client, "POST", "/api/courses", json={"name": "SOEN 490"})
    evil_origin = await request(
        client,
        "POST",
        "/api/courses",
        json={"name": "SOEN 490"},
        headers={"Origin": "https://evil.test"},
    )

    assert signed_out.status_code == 401
    assert missing_origin.status_code == 403
    assert evil_origin.status_code == 403
