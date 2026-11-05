"""Linking an installed repository to a course team (US-10.1), against real PostgreSQL.

Alice (a Pixel student) installs the App, so an instructor of her course can link it.
"""

# AI contribution: 50%

from uuid import uuid4

import pytest
from fastapi.testclient import TestClient

from tests.api.courses_support import SignIn, TwoTeams, course_with_two_teams, run_sql
from tests.api.installation_support import PIXEL_APP, PIXEL_DOCS, change_repositories, install

NOT_LINKABLE = (409, "repository_not_linkable")


@pytest.fixture
def setup(sign_in: SignIn) -> TwoTeams:
    return course_with_two_teams(sign_in)


def link(client: TestClient, course_id: str, team_id: str, repository: str) -> tuple[int, object]:
    response = client.post(
        f"/api/courses/{course_id}/teams/{team_id}/repositories", json={"repository": repository}
    )
    if response.status_code == 200:
        return 200, None
    detail = response.json()["detail"]
    return response.status_code, detail["code"] if isinstance(detail, dict) else detail


def test_a_linked_repository_is_readable_by_its_team_and_reviewed(
    setup: TwoTeams, sign_in: SignIn, database_url: str
) -> None:
    install(setup.instructor, "created", PIXEL_APP)

    response = setup.instructor.post(
        f"/api/courses/{setup.course_id}/teams/{setup.pixel}/repositories",
        json={"repository": "pixel-org/app"},
    )

    assert response.status_code == 200
    assert response.json()["repositories"] == ["Pixel-Org/App"]
    placeholder = run_sql(
        database_url,
        "SELECT archived_at IS NOT NULL AS archived FROM teams WHERE name = 'Pixel-Org/App'",
    )
    assert placeholder == [{"archived": True}]
    assert sign_in("alice").get("/api/repos/Pixel-Org/App/pulls").json() == []
    assert sign_in("bob").get("/api/repos/Pixel-Org/App/pulls").status_code == 403


def test_every_refusal_but_not_registered_is_the_same(setup: TwoTeams, sign_in: SignIn) -> None:
    install(setup.instructor, "created", PIXEL_APP, PIXEL_DOCS)
    install(setup.instructor, "deleted", PIXEL_DOCS)
    other = sign_in("prof2", instructor=True)
    course = other.post("/api/courses", json={"name": "Other"}).json()["id"]
    team = other.post(f"/api/courses/{course}/teams", json={"name": "X"}).json()["id"]
    pixel = setup.course_id, setup.pixel

    assert link(setup.instructor, *pixel, "Nobody/Nothing") == (404, "repository_not_registered")
    assert link(setup.instructor, *pixel, "Pixel-Org/Docs") == NOT_LINKABLE  # Uninstalled.
    assert link(other, course, team, "Pixel-Org/App") == NOT_LINKABLE  # Alice isn't in it.
    assert link(setup.instructor, *pixel, "Pixel-Org/App")[0] == 200
    assert link(other, course, team, "Pixel-Org/App") == NOT_LINKABLE  # In another course now.
    assert link(sign_in("alice"), *pixel, "Pixel-Org/App")[0] == 403
    assert link(setup.instructor, setup.course_id, str(uuid4()), "Pixel-Org/App") == (
        404,
        "Team not found",
    )


def test_rostering_the_installer_is_not_enough(setup: TwoTeams, sign_in: SignIn) -> None:
    install(setup.instructor, "created", PIXEL_APP, sender={"id": 1500, "login": "stranger"})
    setup.instructor.post(
        f"/api/courses/{setup.course_id}/roster", json={"entries": [{"github_login": "stranger"}]}
    )

    assert link(setup.instructor, setup.course_id, setup.pixel, "Pixel-Org/App") == NOT_LINKABLE


def test_an_operator_registered_repository_needs_an_installer(
    setup: TwoTeams, database_url: str
) -> None:
    pixel = setup.course_id, setup.pixel

    assert link(setup.instructor, *pixel, "octocat/demo") == NOT_LINKABLE

    change_repositories(setup.instructor, "added", added=[{"id": 100, "full_name": "octocat/demo"}])

    assert link(setup.instructor, *pixel, "octocat/demo")[0] == 200


def test_a_team_with_members_is_not_a_placeholder(setup: TwoTeams, database_url: str) -> None:
    install(setup.instructor, "created", PIXEL_APP)
    run_sql(
        database_url,
        """INSERT INTO team_memberships (team_id, user_id, role)
           SELECT t.id, u.id, 'member' FROM teams t, users u
           WHERE t.name = 'Pixel-Org/App' AND u.github_login = 'bob'""",
    )

    assert link(setup.instructor, setup.course_id, setup.pixel, "Pixel-Org/App") == NOT_LINKABLE


def test_a_repository_moves_between_teams_of_the_same_course(
    setup: TwoTeams, sign_in: SignIn
) -> None:
    install(setup.instructor, "created", PIXEL_APP)
    for team in (setup.pixel, setup.alpha):
        assert link(setup.instructor, setup.course_id, team, "Pixel-Org/App")[0] == 200

    assert sign_in("bob").get("/api/repos/Pixel-Org/App/pulls").status_code == 200
    assert sign_in("alice").get("/api/repos/Pixel-Org/App/pulls").status_code == 403
