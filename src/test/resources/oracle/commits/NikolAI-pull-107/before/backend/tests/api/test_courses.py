"""Courses, join codes, teams, the roster and the access rule (US-10, ADR 0001 PR 2).

Requests run against real PostgreSQL. Sessions are opened directly with the sessions service,
since sign-in itself is covered by ``test_auth.py``.
"""

# AI contribution: 50%

import asyncio
import re
from collections.abc import Callable, Iterator
from typing import NamedTuple

import pytest
from fastapi.testclient import TestClient
from pydantic import SecretStr

from app.core.config import Settings, get_settings
from app.db import connect
from app.github.oauth import GitHubUser
from app.main import app
from app.services.sessions import SESSION_COOKIE, start_session

ORIGIN = "https://dashboard.test"
JOIN_CODE = re.compile(r"^[A-HJKMNP-Z2-9]{8}$")

SignIn = Callable[..., TestClient]


@pytest.fixture
def settings(database_url: str) -> Settings:
    return Settings(_env_file=None, database_url=SecretStr(database_url), allowed_origins=[ORIGIN])


@pytest.fixture
def sign_in(settings: Settings, database_url: str) -> Iterator[SignIn]:
    """``sign_in("login")`` returns a client holding that user's session cookie."""
    app.dependency_overrides[get_settings] = lambda: settings
    github_ids: dict[str, int] = {}

    def open_session(login: str, *, instructor: bool = False) -> TestClient:
        github_id = github_ids.setdefault(login, 1000 + len(github_ids))
        token = asyncio.run(_session(database_url, GitHubUser(id=github_id, login=login)))
        if instructor:
            run_sql(
                database_url,
                "UPDATE users SET is_instructor = true WHERE github_user_id = %s",
                (github_id,),
            )
        client = TestClient(app, base_url="https://testserver", headers={"Origin": ORIGIN})
        client.cookies.set(SESSION_COOKIE, token)
        return client

    try:
        yield open_session
    finally:
        app.dependency_overrides.clear()


async def _session(url: str, user: GitHubUser) -> str:
    async with await connect(url) as connection:
        return await start_session(connection, user)


def run_sql(url: str, statement: str, params: tuple[object, ...] = ()) -> list[dict[str, object]]:
    async def run() -> list[dict[str, object]]:
        async with await connect(url) as connection:
            cursor = await connection.execute(statement.encode(), params)
            return await cursor.fetchall() if cursor.description else []

    return asyncio.run(run())


def new_course(instructor: TestClient, name: str = "SOEN 490") -> dict[str, str]:
    response = instructor.post("/api/courses", json={"name": name})
    assert response.status_code == 201
    body: dict[str, str] = response.json()
    return body


def new_team(instructor: TestClient, course_id: str, name: str) -> str:
    response = instructor.post(f"/api/courses/{course_id}/teams", json={"name": name})
    assert response.status_code == 201
    return str(response.json()["id"])


def test_an_instructor_creates_a_course_and_gets_a_join_code(sign_in: SignIn) -> None:
    instructor = sign_in("prof", instructor=True)

    course = new_course(instructor, "  SOEN 490  ")

    assert course["name"] == "SOEN 490"
    assert course["role"] == "instructor"
    assert JOIN_CODE.fullmatch(course["join_code"])
    assert instructor.get("/api/me").json()["courses"][0]["id"] == course["id"]


def test_a_non_instructor_creating_a_course_gets_403(sign_in: SignIn, database_url: str) -> None:
    response = sign_in("student").post("/api/courses", json={"name": "Mine"})

    assert response.status_code == 403
    assert run_sql(database_url, "SELECT id FROM courses") == []


def test_a_blank_course_name_is_rejected(sign_in: SignIn) -> None:
    response = sign_in("prof", instructor=True).post("/api/courses", json={"name": "   "})

    assert response.status_code == 422


def test_a_write_from_another_origin_is_refused(sign_in: SignIn, database_url: str) -> None:
    instructor = sign_in("prof", instructor=True)

    response = instructor.post(
        "/api/courses", json={"name": "SOEN 490"}, headers={"Origin": "https://evil.test"}
    )

    assert response.status_code == 403
    assert run_sql(database_url, "SELECT id FROM courses") == []


def test_signed_out_gets_401(sign_in: SignIn) -> None:
    client = sign_in("someone")
    client.cookies.clear()

    assert client.get("/api/courses").status_code == 401


def test_a_student_joins_with_the_code_however_it_is_typed(sign_in: SignIn) -> None:
    course = new_course(sign_in("prof", instructor=True))
    student = sign_in("student")
    typed = f" {course['join_code'][:4].lower()}-{course['join_code'][4:].lower()} "

    response = student.post("/api/courses/join", json={"join_code": typed})

    assert response.status_code == 200
    assert response.json()["role"] == "student"
    assert response.json()["join_code"] is None
    assert [c["id"] for c in student.get("/api/courses").json()] == [course["id"]]


def test_joining_twice_keeps_one_membership(sign_in: SignIn, database_url: str) -> None:
    course = new_course(sign_in("prof", instructor=True))
    student = sign_in("student")

    for _ in range(2):
        response = student.post("/api/courses/join", json={"join_code": course["join_code"]})
        assert response.status_code == 200

    rows = run_sql(
        database_url, "SELECT count(*) AS n FROM course_memberships WHERE role = 'student'"
    )
    assert rows == [{"n": 1}]


def test_the_instructor_joining_their_own_course_stays_instructor(sign_in: SignIn) -> None:
    instructor = sign_in("prof", instructor=True)
    course = new_course(instructor)

    response = instructor.post("/api/courses/join", json={"join_code": course["join_code"]})

    assert response.json()["role"] == "instructor"


def test_a_bad_or_rotated_join_code_is_rejected(sign_in: SignIn) -> None:
    instructor = sign_in("prof", instructor=True)
    course = new_course(instructor)
    rotated = instructor.post(f"/api/courses/{course['id']}/join-code")
    student = sign_in("student")

    assert rotated.status_code == 200
    assert rotated.json()["join_code"] != course["join_code"]
    assert JOIN_CODE.fullmatch(rotated.json()["join_code"])
    for code in (course["join_code"], "NOPE2345"):
        assert student.post("/api/courses/join", json={"join_code": code}).status_code == 404
    joined = student.post("/api/courses/join", json={"join_code": rotated.json()["join_code"]})
    assert joined.status_code == 200


def test_only_the_course_instructors_can_rotate_its_code(sign_in: SignIn) -> None:
    course = new_course(sign_in("prof", instructor=True))
    student = sign_in("student")
    student.post("/api/courses/join", json={"join_code": course["join_code"]})

    assert student.post(f"/api/courses/{course['id']}/join-code").status_code == 403


def test_an_unknown_course_is_404_and_someone_elses_is_403(sign_in: SignIn) -> None:
    course = new_course(sign_in("prof", instructor=True))
    outsider = sign_in("outsider")

    assert outsider.get(f"/api/courses/{course['id']}").status_code == 403
    assert outsider.get("/api/courses/00000000-0000-0000-0000-000000000000").status_code == 404


def test_team_names_are_unique_within_a_course_only(sign_in: SignIn) -> None:
    instructor = sign_in("prof", instructor=True)
    first, second = new_course(instructor, "A"), new_course(instructor, "B")
    new_team(instructor, first["id"], "Team 1")

    duplicate = instructor.post(f"/api/courses/{first['id']}/teams", json={"name": "team 1"})

    assert duplicate.status_code == 409
    new_team(instructor, second["id"], "Team 1")


def test_a_team_member_must_have_joined_the_course(sign_in: SignIn) -> None:
    instructor = sign_in("prof", instructor=True)
    course = new_course(instructor)
    team_id = new_team(instructor, course["id"], "Pixel")
    student = sign_in("Alice")
    members = f"/api/courses/{course['id']}/teams/{team_id}/members"

    assert instructor.post(members, json={"github_login": "ghost"}).status_code == 404
    assert instructor.post(members, json={"github_login": "alice"}).status_code == 409
    student.post("/api/courses/join", json={"join_code": course["join_code"]})
    added = instructor.post(members, json={"github_login": "alice", "role": "lead"})

    assert added.status_code == 200
    assert [(m["github_login"], m["role"]) for m in added.json()["members"]] == [("Alice", "lead")]


class TwoTeams(NamedTuple):
    instructor: TestClient
    course_id: str
    pixel: str
    alpha: str


def _course_with_two_teams(sign_in: SignIn) -> TwoTeams:
    """Alice is in Pixel and Bob is in Alpha, both students of the same course."""
    instructor = sign_in("prof", instructor=True)
    course = new_course(instructor)
    pixel, alpha = (new_team(instructor, course["id"], name) for name in ("Pixel", "Alpha"))
    for login, team in (("alice", pixel), ("bob", alpha)):
        sign_in(login).post("/api/courses/join", json={"join_code": course["join_code"]})
        added = instructor.post(
            f"/api/courses/{course['id']}/teams/{team}/members", json={"github_login": login}
        )
        assert added.status_code == 200
    return TwoTeams(instructor, course["id"], pixel, alpha)


def test_a_student_reads_their_own_team_but_another_team_gets_403(sign_in: SignIn) -> None:
    setup = _course_with_two_teams(sign_in)
    alice = sign_in("alice")
    teams = f"/api/courses/{setup.course_id}/teams"

    own = alice.get(f"{teams}/{setup.pixel}")

    assert own.status_code == 200
    assert [m["github_login"] for m in own.json()["members"]] == ["alice"]
    assert alice.get(f"{teams}/{setup.alpha}").status_code == 403
    course = alice.get(f"/api/courses/{setup.course_id}").json()
    assert [t["name"] for t in course["teams"]] == ["Pixel"]


def test_a_ta_reads_every_team_in_the_course(sign_in: SignIn) -> None:
    setup = _course_with_two_teams(sign_in)
    ta = sign_in("helper")
    roster = setup.instructor.post(
        f"/api/courses/{setup.course_id}/roster",
        json={"entries": [{"github_login": "helper", "role": "ta"}]},
    )
    teams = f"/api/courses/{setup.course_id}/teams"

    assert roster.json()["applied"] == 1
    assert ta.get(f"{teams}/{setup.pixel}").status_code == 200
    assert ta.get(f"{teams}/{setup.alpha}").status_code == 200
    course = ta.get(f"/api/courses/{setup.course_id}").json()
    assert (course["role"], course["join_code"]) == ("ta", None)
    assert sorted(t["name"] for t in course["teams"]) == ["Alpha", "Pixel"]


def test_a_team_access_grant_reads_only_that_team(sign_in: SignIn, database_url: str) -> None:
    setup = _course_with_two_teams(sign_in)
    assessor = sign_in("assessor")
    run_sql(
        database_url,
        """INSERT INTO team_access_grants (team_id, user_id, access_role, granted_by_user_id)
           SELECT %s, a.id, 'assessor', p.id
           FROM users a, users p WHERE a.github_login = 'assessor' AND p.github_login = 'prof'""",
        (setup.pixel,),
    )
    teams = f"/api/courses/{setup.course_id}/teams"

    assert assessor.get(f"{teams}/{setup.pixel}").status_code == 200
    assert assessor.get(f"{teams}/{setup.alpha}").status_code == 403


def test_a_student_who_left_the_course_loses_their_team(sign_in: SignIn, database_url: str) -> None:
    setup = _course_with_two_teams(sign_in)
    run_sql(
        database_url,
        """UPDATE course_memberships SET left_at = now() + interval '1 second'
           WHERE user_id = (SELECT id FROM users WHERE github_login = 'alice')""",
    )

    response = sign_in("alice").get(f"/api/courses/{setup.course_id}/teams/{setup.pixel}")

    assert response.status_code == 403


def test_roster_import_reports_bad_rows_and_imports_the_rest(
    sign_in: SignIn, database_url: str
) -> None:
    setup = _course_with_two_teams(sign_in)
    entries: list[dict[str, object]] = [
        {"github_login": "Dana", "team": "pixel"},
        {"github_login": "not a login!"},
        {"github_login": "erin", "role": "professor"},
        {"github_login": "frank", "team": "Nowhere"},
        {"github_login": "dana"},
        {"github_login": 7},
        {"github_login": "gus", "role": "ta"},
    ]

    response = setup.instructor.post(
        f"/api/courses/{setup.course_id}/roster", json={"entries": entries}
    )

    assert response.status_code == 200
    body = response.json()
    assert (body["imported"], body["applied"]) == (2, 0)
    assert [(e["row"], e["github_login"]) for e in body["errors"]] == [
        (2, "not a login!"),
        (3, "erin"),
        (4, "frank"),
        (5, "dana"),
        (6, ""),
    ]
    assert "Nowhere" in body["errors"][2]["reason"]
    rows = run_sql(
        database_url,
        "SELECT github_login, course_role FROM course_roster_entries ORDER BY github_login",
    )
    assert rows == [
        {"github_login": "Dana", "course_role": "student"},
        {"github_login": "gus", "course_role": "ta"},
    ]


def test_only_instructors_import_a_roster(sign_in: SignIn) -> None:
    setup = _course_with_two_teams(sign_in)

    response = sign_in("alice").post(
        f"/api/courses/{setup.course_id}/roster", json={"entries": [{"github_login": "x"}]}
    )

    assert response.status_code == 403


def test_reimporting_a_login_replaces_its_pending_row(sign_in: SignIn, database_url: str) -> None:
    setup = _course_with_two_teams(sign_in)
    roster = f"/api/courses/{setup.course_id}/roster"

    setup.instructor.post(roster, json={"entries": [{"github_login": "zoe", "team": "Pixel"}]})
    setup.instructor.post(roster, json={"entries": [{"github_login": "ZOE", "role": "ta"}]})

    rows = run_sql(
        database_url, "SELECT github_login, course_role, team_id FROM course_roster_entries"
    )
    assert rows == [{"github_login": "ZOE", "course_role": "ta", "team_id": None}]


def test_an_unknown_team_is_404(sign_in: SignIn) -> None:
    setup = _course_with_two_teams(sign_in)
    other = new_course(setup.instructor, "Other")
    unknown = "00000000-0000-0000-0000-000000000000"

    added = setup.instructor.post(
        f"/api/courses/{setup.course_id}/teams/{unknown}/members", json={"github_login": "alice"}
    )
    wrong_course = setup.instructor.get(f"/api/courses/{other['id']}/teams/{setup.pixel}")

    assert added.status_code == 404
    assert wrong_course.status_code == 404


def test_a_login_shared_by_two_users_is_refused(sign_in: SignIn, database_url: str) -> None:
    setup = _course_with_two_teams(sign_in)
    run_sql(database_url, "INSERT INTO users (github_user_id, github_login) VALUES (9, 'Alice')")

    response = setup.instructor.post(
        f"/api/courses/{setup.course_id}/teams/{setup.alpha}/members",
        json={"github_login": "alice"},
    )

    assert response.status_code == 409


def test_running_out_of_join_codes_is_a_clean_503(
    sign_in: SignIn, monkeypatch: pytest.MonkeyPatch
) -> None:
    instructor = sign_in("prof", instructor=True)
    course, taken = new_course(instructor, "A"), new_course(instructor, "B")
    monkeypatch.setattr("app.services.courses.new_join_code", lambda: taken["join_code"])

    created = instructor.post("/api/courses", json={"name": "Again"})
    rotated = instructor.post(f"/api/courses/{course['id']}/join-code")

    assert (created.status_code, rotated.status_code) == (503, 503)
    assert created.json() == {"detail": "Try again in a moment"}
