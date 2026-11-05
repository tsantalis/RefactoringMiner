"""Repositories registered from GitHub App installation webhooks (US-10.1), against real
PostgreSQL with signed deliveries. Linking them to course teams is in test_repository_links.py.
"""

# AI contribution: 50%

import asyncio

import pytest
from psycopg import OperationalError

from app.api.routes import webhooks
from app.db import connect
from app.db.repositories.registrations import active_repository_id
from tests.api.courses_support import SignIn, TwoTeams, course_with_two_teams, run_sql
from tests.api.installation_support import (
    PIXEL_APP,
    PIXEL_DOCS,
    change_repositories,
    install,
    registered,
)


@pytest.fixture
def setup(sign_in: SignIn) -> TwoTeams:
    return course_with_two_teams(sign_in)


def _served(url: str, github_repository_id: int) -> bool:
    async def check() -> bool:
        async with await connect(url) as connection:
            return await active_repository_id(connection, github_repository_id) is not None

    return asyncio.run(check())


def test_installing_registers_each_repository_in_a_placeholder_team(
    setup: TwoTeams, database_url: str
) -> None:
    assert install(setup.instructor, "created", PIXEL_APP, PIXEL_DOCS) == 202
    assert install(setup.instructor, "created", PIXEL_APP, PIXEL_DOCS) == 202  # Redelivered.

    rows = registered(database_url)
    assert [(r["full_name"], r["team"], r["course_id"]) for r in rows] == [
        ("Pixel-Org/App", "Pixel-Org/App", None),
        ("Pixel-Org/Docs", "Pixel-Org/Docs", None),
    ]
    assert {r["installed_by_login"] for r in rows} == {"alice"}


def test_an_unlinked_installation_is_not_reviewed(setup: TwoTeams, database_url: str) -> None:
    install(setup.instructor, "created", PIXEL_APP)

    assert _served(database_url, 501) is False
    assert _served(database_url, 100) is True  # Registered by an operator: still reviewed.


def test_reinstalling_on_a_team_with_members_keeps_it_reviewed(
    setup: TwoTeams, database_url: str
) -> None:
    # A team from before courses (ADR 0001 decision 7) can have members, so it isn't a
    # placeholder and its repository can't be linked: recording an installer would strand it.
    run_sql(
        database_url,
        """INSERT INTO team_memberships (team_id, user_id, role)
           SELECT r.team_id, u.id, 'member' FROM repositories r, users u
           WHERE r.github_repository_id = 100 AND u.github_login = 'bob'""",
    )
    demo = {"id": 100, "full_name": "octocat/demo"}

    change_repositories(setup.instructor, "removed", removed=[demo])
    change_repositories(setup.instructor, "added", added=[demo])

    assert _served(database_url, 100) is True
    assert run_sql(
        database_url,
        "SELECT installed_by_login FROM repositories WHERE github_repository_id = 100",
    ) == [{"installed_by_login": None}]


def test_removing_a_repository_archives_it_and_its_placeholder_team(
    setup: TwoTeams, database_url: str
) -> None:
    install(setup.instructor, "created", PIXEL_APP)

    assert change_repositories(setup.instructor, "added", added=[PIXEL_DOCS]) == 202
    assert change_repositories(setup.instructor, "removed", removed=[PIXEL_APP]) == 202

    assert [(r["full_name"], r["active"], r["team_active"]) for r in registered(database_url)] == [
        ("Pixel-Org/App", False, False),
        ("Pixel-Org/Docs", True, True),
    ]


def test_reinstalling_brings_the_same_row_back_in_a_fresh_placeholder(
    setup: TwoTeams, database_url: str
) -> None:
    install(setup.instructor, "created", PIXEL_APP)
    first = run_sql(database_url, "SELECT id FROM repositories WHERE github_repository_id = 501")

    install(setup.instructor, "deleted", PIXEL_APP)
    install(setup.instructor, "created", {"id": 501, "full_name": "Pixel-Org/Renamed"})

    assert (
        run_sql(database_url, "SELECT id FROM repositories WHERE github_repository_id = 501")
        == first
    )
    row = registered(database_url)[0]
    assert (row["full_name"], row["active"], row["team_active"]) == (
        "Pixel-Org/Renamed",
        True,
        True,
    )


def test_reinstalling_a_repository_of_an_archived_course_team(
    setup: TwoTeams, database_url: str
) -> None:
    install(setup.instructor, "created", PIXEL_APP)
    setup.instructor.post(
        f"/api/courses/{setup.course_id}/teams/{setup.pixel}/repositories",
        json={"repository": "Pixel-Org/App"},
    )
    run_sql(database_url, "UPDATE teams SET archived_at = now() WHERE id = %s", (setup.pixel,))

    install(setup.instructor, "created", PIXEL_APP)

    row = registered(database_url)[0]
    assert (row["team_active"], row["course_id"]) == (True, None)
    assert str(row["team"]).startswith("Pixel-Org/App")


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
    assert install(setup.instructor, "created", sender={"login": "no id"}) == 400

    async def fail(url: str) -> None:
        raise OperationalError("connection failed: secret-password")

    monkeypatch.setattr(webhooks, "connect", fail)
    assert install(setup.instructor, "created", PIXEL_APP) == 503


def test_a_failure_part_way_saves_none_of_the_event(
    setup: TwoTeams, database_url: str, monkeypatch: pytest.MonkeyPatch
) -> None:
    install(setup.instructor, "created", PIXEL_APP)

    async def fail(*args: object) -> int:
        raise OperationalError("lost the connection")

    monkeypatch.setattr("app.services.installations.archive_repositories", fail)

    code = change_repositories(setup.instructor, "added", added=[PIXEL_DOCS], removed=[PIXEL_APP])

    assert code == 503
    assert [r["full_name"] for r in registered(database_url)] == ["Pixel-Org/App"]
