"""Courses, join codes, teams and the access rule (US-10, ADR 0001 PR 2).

Requests run against real PostgreSQL. Sessions are opened directly with the sessions service,
since sign-in itself is covered by ``test_auth.py``.
"""

# AI contribution: 50%

import re

import pytest

from tests.api.courses_support import (
    SignIn,
    course_with_two_teams,
    new_course,
    new_team,
    run_sql,
)

JOIN_CODE = re.compile(r"^[A-HJKMNP-Z2-9]{8}$")


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


def test_a_student_reads_their_own_team_but_another_team_gets_403(sign_in: SignIn) -> None:
    setup = course_with_two_teams(sign_in)
    alice = sign_in("alice")
    teams = f"/api/courses/{setup.course_id}/teams"

    own = alice.get(f"{teams}/{setup.pixel}")

    assert own.status_code == 200
    assert [m["github_login"] for m in own.json()["members"]] == ["alice"]
    assert alice.get(f"{teams}/{setup.alpha}").status_code == 403
    course = alice.get(f"/api/courses/{setup.course_id}").json()
    assert [t["name"] for t in course["teams"]] == ["Pixel"]


def test_a_ta_reads_every_team_in_the_course(sign_in: SignIn) -> None:
    setup = course_with_two_teams(sign_in)
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
    setup = course_with_two_teams(sign_in)
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
    setup = course_with_two_teams(sign_in)
    run_sql(
        database_url,
        """UPDATE course_memberships SET left_at = now() + interval '1 second'
           WHERE user_id = (SELECT id FROM users WHERE github_login = 'alice')""",
    )

    response = sign_in("alice").get(f"/api/courses/{setup.course_id}/teams/{setup.pixel}")

    assert response.status_code == 403


def test_an_unknown_team_is_404(sign_in: SignIn) -> None:
    setup = course_with_two_teams(sign_in)
    other = new_course(setup.instructor, "Other")
    unknown = "00000000-0000-0000-0000-000000000000"

    added = setup.instructor.post(
        f"/api/courses/{setup.course_id}/teams/{unknown}/members", json={"github_login": "alice"}
    )
    wrong_course = setup.instructor.get(f"/api/courses/{other['id']}/teams/{setup.pixel}")

    assert added.status_code == 404
    assert wrong_course.status_code == 404


def test_a_login_shared_by_two_users_is_refused(sign_in: SignIn, database_url: str) -> None:
    setup = course_with_two_teams(sign_in)
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


def test_an_archived_course_or_team_reads_as_403(sign_in: SignIn, database_url: str) -> None:
    setup = course_with_two_teams(sign_in)
    teams = f"/api/courses/{setup.course_id}/teams"
    ta, alice = sign_in("helper"), sign_in("alice")
    setup.instructor.post(
        f"/api/courses/{setup.course_id}/roster",
        json={"entries": [{"github_login": "helper", "role": "ta"}]},
    )

    run_sql(database_url, "UPDATE teams SET archived_at = now() WHERE id = %s", (setup.pixel,))
    assert alice.get(f"{teams}/{setup.pixel}").status_code == 403
    assert ta.get(f"{teams}/{setup.alpha}").status_code == 200

    run_sql(database_url, "UPDATE courses SET archived_at = now()")
    assert ta.get(f"{teams}/{setup.alpha}").status_code == 403
