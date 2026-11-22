"""Putting a course member on a team from the dashboard (#214, ADR 0001 decision 7).

A student who joins with the code is in no team, so the members list shows each member's
team and the instructor moves them onto one. Requests run against real PostgreSQL.
"""

# AI contribution: 50%

from tests.api.courses_support import (
    SignIn,
    course_with_two_teams_and_outsider,
    make_ta,
)


def test_the_members_list_shows_each_members_team_or_none(sign_in: SignIn) -> None:
    setup = course_with_two_teams_and_outsider(sign_in)
    course_id = setup.course["id"]
    sign_in("alice").post("/api/courses/join", json={"join_code": setup.course["join_code"]})
    added = setup.instructor.post(
        f"/api/courses/{course_id}/teams/{setup.pixel}/members", json={"github_login": "alice"}
    )
    assert added.status_code == 200

    response = setup.instructor.get(f"/api/courses/{course_id}/members")

    assert response.status_code == 200
    teams = {m["github_login"]: m["team"] for m in response.json()}
    assert teams["alice"] == {"id": setup.pixel, "name": "Pixel"}
    assert teams["owner"] is None
    assert teams["prof"] is None


def test_assigning_another_team_moves_the_member_and_their_access(sign_in: SignIn) -> None:
    setup = course_with_two_teams_and_outsider(sign_in)
    course_id = setup.course["id"]
    owner = sign_in("owner")
    teams = f"/api/courses/{course_id}/teams"
    first = setup.instructor.post(f"{teams}/{setup.pixel}/members", json={"github_login": "owner"})
    assert first.status_code == 200
    assert owner.get(f"{teams}/{setup.pixel}").status_code == 200

    moved = setup.instructor.post(f"{teams}/{setup.alpha}/members", json={"github_login": "owner"})

    assert moved.status_code == 200
    assert [m["github_login"] for m in moved.json()["members"]] == ["owner"]
    assert owner.get(f"{teams}/{setup.alpha}").status_code == 200
    assert owner.get(f"{teams}/{setup.pixel}").status_code == 403
    pixel = setup.instructor.get(f"{teams}/{setup.pixel}").json()
    assert pixel["members"] == []
    members = setup.instructor.get(f"/api/courses/{course_id}/members").json()
    assert next(m for m in members if m["github_login"] == "owner")["team"]["name"] == "Alpha"


def test_assigning_the_same_team_twice_keeps_one_membership(sign_in: SignIn) -> None:
    setup = course_with_two_teams_and_outsider(sign_in)
    members = f"/api/courses/{setup.course['id']}/teams/{setup.pixel}/members"

    for _ in range(2):
        response = setup.instructor.post(members, json={"github_login": "owner"})
        assert response.status_code == 200

    assert [m["github_login"] for m in response.json()["members"]] == ["owner"]


def test_only_the_instructor_assigns_teams(sign_in: SignIn) -> None:
    setup = course_with_two_teams_and_outsider(sign_in)
    course_id = setup.course["id"]
    ta = sign_in("helper")
    make_ta(setup.instructor, course_id, "helper")
    members = f"/api/courses/{course_id}/teams/{setup.pixel}/members"

    response = ta.post(members, json={"github_login": "owner"})

    assert response.status_code == 403
    listed = setup.instructor.get(f"/api/courses/{course_id}/members").json()
    assert next(m for m in listed if m["github_login"] == "owner")["team"] is None


def test_assigning_a_lead_their_own_team_again_keeps_them_lead(sign_in: SignIn) -> None:
    setup = course_with_two_teams_and_outsider(sign_in)
    members = f"/api/courses/{setup.course['id']}/teams/{setup.pixel}/members"
    lead = setup.instructor.post(members, json={"github_login": "owner", "role": "lead"})
    assert lead.status_code == 200

    again = setup.instructor.post(members, json={"github_login": "owner"})

    assert again.status_code == 200
    assert [(m["github_login"], m["role"]) for m in again.json()["members"]] == [("owner", "lead")]
