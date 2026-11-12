"""The course members list and promoting a member to TA (#152, ADR 0001 decision 6).

Requests run against real PostgreSQL. Sessions are opened directly with the sessions service,
since sign-in itself is covered by ``test_auth.py``.
"""

# AI contribution: 50%

from tests.api.courses_support import (
    SignIn,
    course_with_student,
    make_ta,
    member_id,
    new_course,
    run_sql,
)


def test_an_instructor_lists_the_course_members(sign_in: SignIn) -> None:
    instructor, course, _ = course_with_student(sign_in)

    response = instructor.get(f"/api/courses/{course['id']}/members")

    assert response.status_code == 200
    by_login = {member["github_login"]: member for member in response.json()}
    assert by_login["alice"]["role"] == "student"
    assert by_login["prof"]["role"] == "instructor"


def test_a_ta_lists_the_members_read_only(sign_in: SignIn) -> None:
    instructor, course, _ = course_with_student(sign_in)
    ta = sign_in("helper")
    make_ta(instructor, course["id"], "helper")

    response = ta.get(f"/api/courses/{course['id']}/members")

    assert response.status_code == 200
    assert {member["github_login"] for member in response.json()} == {"prof", "alice", "helper"}


def test_a_student_gets_403_and_a_signed_out_user_401(sign_in: SignIn) -> None:
    _, course, student = course_with_student(sign_in)
    client = sign_in("someone")
    client.cookies.clear()

    assert student.get(f"/api/courses/{course['id']}/members").status_code == 403
    assert client.get(f"/api/courses/{course['id']}/members").status_code == 401


def test_an_unknown_course_is_404_and_an_outsider_gets_403(sign_in: SignIn) -> None:
    instructor = sign_in("prof", instructor=True)
    course = new_course(instructor)
    outsider = sign_in("outsider")
    unknown = "00000000-0000-0000-0000-000000000000"

    assert instructor.get(f"/api/courses/{unknown}/members").status_code == 404
    assert outsider.get(f"/api/courses/{course['id']}/members").status_code == 403


def test_an_instructor_promotes_a_student_to_ta(sign_in: SignIn) -> None:
    instructor, course, _ = course_with_student(sign_in)
    alice_id = member_id(instructor, course["id"], "alice")
    path = f"/api/courses/{course['id']}/members/{alice_id}/promote"

    response = instructor.post(path)

    assert response.status_code == 200
    assert response.json()["role"] == "ta"
    after = instructor.get(f"/api/courses/{course['id']}/members").json()
    assert next(m for m in after if m["github_login"] == "alice")["role"] == "ta"


def test_only_a_signed_in_instructor_can_promote(sign_in: SignIn) -> None:
    instructor, course, _ = course_with_student(sign_in)
    ta = sign_in("helper")
    make_ta(instructor, course["id"], "helper")
    signed_out = sign_in("signed-out")
    signed_out.cookies.clear()
    alice_id = member_id(instructor, course["id"], "alice")
    path = f"/api/courses/{course['id']}/members/{alice_id}/promote"

    assert ta.post(path).status_code == 403
    assert signed_out.post(path).status_code == 401


def test_promoting_a_user_outside_the_course_is_404(sign_in: SignIn) -> None:
    instructor, course, _ = course_with_student(sign_in)
    outsider = sign_in("outsider")
    outsider_id = outsider.get("/api/me").json()["id"]

    response = instructor.post(f"/api/courses/{course['id']}/members/{outsider_id}/promote")

    assert response.status_code == 404


def test_promoting_an_instructor_is_409(sign_in: SignIn) -> None:
    instructor, course, _ = course_with_student(sign_in)
    prof_id = member_id(instructor, course["id"], "prof")

    response = instructor.post(f"/api/courses/{course['id']}/members/{prof_id}/promote")

    assert response.status_code == 409
    after = instructor.get(f"/api/courses/{course['id']}/members").json()
    assert next(m for m in after if m["github_login"] == "prof")["role"] == "instructor"


def test_promoting_a_ta_is_409(sign_in: SignIn) -> None:
    instructor, course, _ = course_with_student(sign_in)
    sign_in("helper")
    make_ta(instructor, course["id"], "helper")
    helper_id = member_id(instructor, course["id"], "helper")

    response = instructor.post(f"/api/courses/{course['id']}/members/{helper_id}/promote")

    assert response.status_code == 409
    after = instructor.get(f"/api/courses/{course['id']}/members").json()
    assert next(m for m in after if m["github_login"] == "helper")["role"] == "ta"


def test_a_promote_from_another_origin_is_refused(sign_in: SignIn, database_url: str) -> None:
    instructor, course, _ = course_with_student(sign_in)
    alice_id = member_id(instructor, course["id"], "alice")

    response = instructor.post(
        f"/api/courses/{course['id']}/members/{alice_id}/promote",
        headers={"Origin": "https://evil.test"},
    )

    assert response.status_code == 403
    assert response.json()["detail"]["code"] == "origin_not_allowed"
    after = instructor.get(f"/api/courses/{course['id']}/members").json()
    assert next(m for m in after if m["github_login"] == "alice")["role"] == "student"


def test_a_member_who_left_is_not_listed_and_cannot_be_promoted(
    sign_in: SignIn, database_url: str
) -> None:
    instructor, course, _ = course_with_student(sign_in)
    alice_id = member_id(instructor, course["id"], "alice")
    run_sql(
        database_url,
        "UPDATE course_memberships SET left_at = now() WHERE user_id = %s",
        (alice_id,),
    )

    listed = instructor.get(f"/api/courses/{course['id']}/members").json()

    assert [m["github_login"] for m in listed] == ["prof"]
    promoted = instructor.post(f"/api/courses/{course['id']}/members/{alice_id}/promote")
    assert promoted.status_code == 404


def test_the_members_are_ordered_by_login_case_insensitively(sign_in: SignIn) -> None:
    instructor = sign_in("prof", instructor=True)
    course = new_course(instructor)
    for login in ("Bob", "carol", "alice"):
        member = sign_in(login)
        joined = member.post("/api/courses/join", json={"join_code": course["join_code"]})
        assert joined.status_code == 200

    response = instructor.get(f"/api/courses/{course['id']}/members")

    assert [m["github_login"] for m in response.json()] == ["alice", "Bob", "carol", "prof"]
