"""The team's planning page (US-19.1, #241): who opens it, and who may plan on it.

Members of the team plan; course staff and granted stakeholders read it with every control off;
anyone else gets 403 and a signed-out request 401. Requests run against real PostgreSQL.
"""

# AI contribution: 50%

from fastapi.testclient import TestClient

from app.main import app
from tests.api.courses_support import (
    SignIn,
    course_with_two_teams,
    make_stakeholder,
    make_ta,
    member_id,
)


def planning(team_id: str) -> str:
    return f"/api/teams/{team_id}/planning"


def test_a_member_opens_their_teams_planning_page_and_may_plan(sign_in: SignIn) -> None:
    teams = course_with_two_teams(sign_in)

    response = sign_in("alice").get(planning(teams.pixel))

    assert response.status_code == 200
    assert response.json() == {
        "team": {"id": teams.pixel, "name": "Pixel", "course": "SOEN 490"},
        "can_plan": True,
    }


def test_course_staff_open_any_team_read_only(sign_in: SignIn) -> None:
    teams = course_with_two_teams(sign_in)
    tara = sign_in("tara")
    make_ta(teams.instructor, teams.course_id, "tara")

    for staff in (teams.instructor, tara):
        response = staff.get(planning(teams.alpha))

        assert response.status_code == 200
        assert response.json()["can_plan"] is False


def test_a_stakeholder_reads_only_with_a_grant_and_never_plans(sign_in: SignIn) -> None:
    teams = course_with_two_teams(sign_in)
    course = teams.instructor.get(f"/api/courses/{teams.course_id}").json()
    sign_in("sam").post("/api/courses/join", json={"join_code": course["join_code"]})
    make_stakeholder(teams.instructor, teams.course_id, "sam")
    sam = sign_in("sam")
    assert sam.get(planning(teams.pixel)).status_code == 403

    granted = teams.instructor.post(
        f"/api/courses/{teams.course_id}/teams/{teams.pixel}/grants",
        json={"user_id": member_id(teams.instructor, teams.course_id, "sam")},
    )
    assert granted.status_code == 201

    response = sam.get(planning(teams.pixel))
    assert response.status_code == 200
    assert response.json()["can_plan"] is False


def test_a_student_of_another_team_is_refused(sign_in: SignIn) -> None:
    teams = course_with_two_teams(sign_in)

    response = sign_in("bob").get(planning(teams.pixel))

    assert response.status_code == 403


def test_an_unknown_team_is_refused_like_one_the_viewer_cant_read(sign_in: SignIn) -> None:
    course_with_two_teams(sign_in)

    response = sign_in("alice").get(planning("00000000-0000-4000-8000-000000000000"))

    assert response.status_code == 403


def test_a_signed_out_request_is_refused(sign_in: SignIn) -> None:
    teams = course_with_two_teams(sign_in)

    response = TestClient(app, base_url="https://testserver").get(planning(teams.pixel))

    assert response.status_code == 401


def test_a_malformed_team_id_is_rejected(sign_in: SignIn) -> None:
    response = sign_in("alice").get(planning("not-a-team"))

    assert response.status_code == 422
