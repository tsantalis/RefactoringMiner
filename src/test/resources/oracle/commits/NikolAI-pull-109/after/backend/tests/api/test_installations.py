"""Repositories registered from GitHub App installation webhooks, and linked to course teams
(US-10.1), against real PostgreSQL with signed deliveries.
"""

# AI contribution: 50%

from uuid import uuid4

import pytest
from fastapi.testclient import TestClient
from psycopg import OperationalError
from pydantic import SecretStr

from app.api.routes import webhooks
from app.core.config import Settings
from tests.api.courses_support import ORIGIN, SignIn, TwoTeams, course_with_two_teams, run_sql
from tests.fakes import WEBHOOK_SECRET, signed

APP: dict[str, object] = {"id": 77}
PIXEL_APP: dict[str, object] = {"id": 501, "full_name": "Pixel-Org/App"}
PIXEL_DOCS: dict[str, object] = {"id": 502, "full_name": "Pixel-Org/Docs"}


@pytest.fixture
def course_settings(database_url: str) -> Settings:
    """Overrides the course fixture's settings so the same app also accepts webhooks."""
    return Settings(
        _env_file=None,
        database_url=SecretStr(database_url),
        allowed_origins=[ORIGIN],
        github_webhook_secret=SecretStr(WEBHOOK_SECRET),
    )


def deliver(client: TestClient, event: str, payload: dict[str, object]) -> int:
    body, signature = signed(payload)
    response = client.post(
        "/webhook",
        content=body,
        headers={
            "X-GitHub-Event": event,
            "X-GitHub-Delivery": str(uuid4()),
            "X-Hub-Signature-256": signature,
            "Content-Type": "application/json",
        },
    )
    code: int = response.status_code
    return code


def install(client: TestClient, action: str, *repos: dict[str, object]) -> int:
    return deliver(
        client, "installation", {"action": action, "installation": APP, "repositories": list(repos)}
    )


def registered(url: str) -> list[dict[str, object]]:
    return run_sql(
        url,
        """SELECT r.full_name, r.archived_at IS NULL AS active, t.name AS team, t.course_id
           FROM repositories r JOIN teams t ON t.id = r.team_id
           WHERE r.github_repository_id >= 500 ORDER BY r.github_repository_id""",
    )


@pytest.fixture
def setup(sign_in: SignIn) -> TwoTeams:
    return course_with_two_teams(sign_in)


def test_installing_registers_each_repository_in_a_team_of_its_own(
    setup: TwoTeams, database_url: str
) -> None:
    assert install(setup.instructor, "created", PIXEL_APP, PIXEL_DOCS) == 202
    assert install(setup.instructor, "created", PIXEL_APP, PIXEL_DOCS) == 202  # Redelivered.

    assert registered(database_url) == [
        {"full_name": "Pixel-Org/App", "active": True, "team": "Pixel-Org/App", "course_id": None},
        {
            "full_name": "Pixel-Org/Docs",
            "active": True,
            "team": "Pixel-Org/Docs",
            "course_id": None,
        },
    ]


def test_adding_and_removing_repositories_follows_the_installation(
    setup: TwoTeams, database_url: str
) -> None:
    install(setup.instructor, "created", PIXEL_APP)

    changes: dict[str, object] = {
        "action": "added",
        "installation": APP,
        "repositories_added": [PIXEL_DOCS],
    }
    assert deliver(setup.instructor, "installation_repositories", changes) == 202
    changes = {"action": "removed", "installation": APP, "repositories_removed": [PIXEL_APP]}
    assert deliver(setup.instructor, "installation_repositories", changes) == 202

    assert [(r["full_name"], r["active"]) for r in registered(database_url)] == [
        ("Pixel-Org/App", False),
        ("Pixel-Org/Docs", True),
    ]


def test_reinstalling_brings_the_same_row_back_under_its_new_name(
    setup: TwoTeams, database_url: str
) -> None:
    install(setup.instructor, "created", PIXEL_APP)
    first = run_sql(database_url, "SELECT id FROM repositories WHERE github_repository_id = 501")

    install(setup.instructor, "deleted", PIXEL_APP)
    archived = registered(database_url)
    install(setup.instructor, "created", {"id": 501, "full_name": "Pixel-Org/Renamed"})

    assert archived[0]["active"] is False
    assert (
        run_sql(database_url, "SELECT id FROM repositories WHERE github_repository_id = 501")
        == first
    )
    assert [(r["full_name"], r["active"]) for r in registered(database_url)] == [
        ("Pixel-Org/Renamed", True)
    ]


def test_suspending_archives_and_other_actions_change_nothing(
    setup: TwoTeams, database_url: str
) -> None:
    install(setup.instructor, "created", PIXEL_APP)

    install(setup.instructor, "new_permissions_accepted", PIXEL_APP, PIXEL_DOCS)
    install(setup.instructor, "suspend", PIXEL_APP)
    suspended = registered(database_url)
    install(setup.instructor, "unsuspend", PIXEL_APP)

    assert [(r["full_name"], r["active"]) for r in suspended] == [("Pixel-Org/App", False)]
    assert [(r["full_name"], r["active"]) for r in registered(database_url)] == [
        ("Pixel-Org/App", True)
    ]


def test_a_taken_team_name_gets_the_repository_id(setup: TwoTeams, database_url: str) -> None:
    run_sql(database_url, "INSERT INTO teams (name) VALUES ('Pixel-Org/App')")

    install(setup.instructor, "created", PIXEL_APP)

    assert registered(database_url)[0]["team"] == "Pixel-Org/App (501)"


def test_a_repository_with_an_unexpected_name_is_skipped(
    setup: TwoTeams, database_url: str
) -> None:
    assert (
        install(setup.instructor, "created", {"id": 503, "full_name": "no slash"}, PIXEL_APP) == 202
    )

    assert [r["full_name"] for r in registered(database_url)] == ["Pixel-Org/App"]


def test_unreadable_or_unsaved_installation_events_are_refused(
    setup: TwoTeams, monkeypatch: pytest.MonkeyPatch
) -> None:
    assert deliver(setup.instructor, "installation", {"action": "created"}) == 400

    async def fail(url: str) -> None:
        raise OperationalError("connection failed: secret-password")

    monkeypatch.setattr(webhooks, "connect", fail)
    assert install(setup.instructor, "created", PIXEL_APP) == 503


def test_an_installed_repository_linked_to_a_team_is_readable_by_that_team(
    setup: TwoTeams, sign_in: SignIn, database_url: str
) -> None:
    install(setup.instructor, "created", PIXEL_APP)
    link = f"/api/courses/{setup.course_id}/teams/{setup.pixel}/repositories"

    response = setup.instructor.post(link, json={"repository": "pixel-org/app"})

    assert response.status_code == 200
    assert response.json()["repositories"] == ["Pixel-Org/App"]
    placeholder = run_sql(
        database_url,
        "SELECT archived_at IS NOT NULL AS archived FROM teams WHERE name = %s",
        ("Pixel-Org/App",),
    )
    assert placeholder == [{"archived": True}]
    assert sign_in("alice").get("/api/repos/Pixel-Org/App/pulls").json() == []
    assert sign_in("bob").get("/api/repos/Pixel-Org/App/pulls").status_code == 403


def test_linking_is_refused_with_a_reason(setup: TwoTeams, sign_in: SignIn) -> None:
    install(setup.instructor, "created", PIXEL_APP, PIXEL_DOCS)
    install(setup.instructor, "deleted", PIXEL_DOCS)
    teams = f"/api/courses/{setup.course_id}/teams"

    def link(client: TestClient, team: str, repository: str) -> tuple[int, object]:
        response = client.post(f"{teams}/{team}/repositories", json={"repository": repository})
        detail = response.json()["detail"]
        return response.status_code, detail["code"] if isinstance(detail, dict) else detail

    assert link(setup.instructor, setup.pixel, "Nobody/Nothing") == (
        404,
        "repository_not_registered",
    )
    assert link(setup.instructor, setup.pixel, "Pixel-Org/Docs") == (409, "repository_uninstalled")
    assert link(sign_in("alice"), setup.pixel, "Pixel-Org/App")[0] == 403
    assert link(setup.instructor, str(uuid4()), "Pixel-Org/App") == (404, "Team not found")

    other = sign_in("prof2", instructor=True)
    course = other.post("/api/courses", json={"name": "Other"}).json()
    team = other.post(f"/api/courses/{course['id']}/teams", json={"name": "X"}).json()["id"]
    assert (
        setup.instructor.post(
            f"{teams}/{setup.pixel}/repositories", json={"repository": "Pixel-Org/App"}
        ).status_code
        == 200
    )
    taken = other.post(
        f"/api/courses/{course['id']}/teams/{team}/repositories",
        json={"repository": "Pixel-Org/App"},
    )
    assert (taken.status_code, taken.json()["detail"]["code"]) == (
        409,
        "repository_in_another_course",
    )


def test_a_repository_moves_between_teams_of_the_same_course(
    setup: TwoTeams, sign_in: SignIn
) -> None:
    install(setup.instructor, "created", PIXEL_APP)
    teams = f"/api/courses/{setup.course_id}/teams"
    for team in (setup.pixel, setup.alpha):
        moved = setup.instructor.post(
            f"{teams}/{team}/repositories", json={"repository": "Pixel-Org/App"}
        )
        assert moved.status_code == 200

    assert sign_in("bob").get("/api/repos/Pixel-Org/App/pulls").status_code == 200
    assert sign_in("alice").get("/api/repos/Pixel-Org/App/pulls").status_code == 403
