"""Roster import and the roster claim (US-10 task 10.2, ADR 0001 decision 8)."""

# AI contribution: 50%

import asyncio
from typing import cast
from uuid import UUID

import pytest

from app.db import connect
from app.db.models.courses import MAX_ROSTER_ROWS
from app.services.courses import claim_roster_at_sign_in
from tests.api.courses_support import (
    SignIn,
    course_with_two_teams,
    run_sql,
)


def test_roster_import_reports_bad_rows_and_imports_the_rest(
    sign_in: SignIn, database_url: str
) -> None:
    setup = course_with_two_teams(sign_in)
    entries: list[object] = [
        {"github_login": "Dana", "team": "pixel"},
        {"github_login": "not a login!"},
        {"github_login": "erin", "role": "professor"},
        {"github_login": "frank", "team": "Nowhere"},
        {"github_login": "dana"},
        {"github_login": 7},
        {"github_login": "gus", "role": "ta"},
        None,
        "just text",
        {"github_login": "hank", "role": ["ta"]},
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
        (8, ""),
        (9, ""),
        (10, "hank"),
    ]
    assert body["errors"][5]["reason"] == "row must be an object"
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
    setup = course_with_two_teams(sign_in)

    response = sign_in("alice").post(
        f"/api/courses/{setup.course_id}/roster", json={"entries": [{"github_login": "x"}]}
    )

    assert response.status_code == 403


def test_reimporting_a_login_replaces_its_pending_row(sign_in: SignIn, database_url: str) -> None:
    setup = course_with_two_teams(sign_in)
    roster = f"/api/courses/{setup.course_id}/roster"

    setup.instructor.post(roster, json={"entries": [{"github_login": "zoe", "team": "Pixel"}]})
    setup.instructor.post(roster, json={"entries": [{"github_login": "ZOE", "role": "ta"}]})

    rows = run_sql(
        database_url, "SELECT github_login, course_role, team_id FROM course_roster_entries"
    )
    assert rows == [{"github_login": "ZOE", "course_role": "ta", "team_id": None}]


def test_a_full_size_roster_imports_in_one_request(sign_in: SignIn, database_url: str) -> None:
    setup = course_with_two_teams(sign_in)
    entries = [{"github_login": f"student-{n}", "team": "Pixel"} for n in range(MAX_ROSTER_ROWS)]

    response = setup.instructor.post(
        f"/api/courses/{setup.course_id}/roster", json={"entries": entries}
    )

    assert response.json()["imported"] == MAX_ROSTER_ROWS
    count = run_sql(database_url, "SELECT count(*) AS n FROM course_roster_entries")
    assert count == [{"n": MAX_ROSTER_ROWS}]


def test_a_failure_part_way_saves_none_of_the_roster(
    sign_in: SignIn, database_url: str, monkeypatch: pytest.MonkeyPatch
) -> None:
    setup = course_with_two_teams(sign_in)

    async def fail(*args: object, **kwargs: object) -> int:
        raise RuntimeError("database went away")

    monkeypatch.setattr("app.services.courses.claim_roster_entries", fail)

    with pytest.raises(RuntimeError):
        setup.instructor.post(
            f"/api/courses/{setup.course_id}/roster",
            json={"entries": [{"github_login": "zoe"}, {"github_login": "alice"}]},
        )

    assert run_sql(database_url, "SELECT id FROM course_roster_entries") == []


def test_the_roster_moves_a_student_to_another_team(sign_in: SignIn) -> None:
    setup = course_with_two_teams(sign_in)
    teams = f"/api/courses/{setup.course_id}/teams"

    roster = setup.instructor.post(
        f"/api/courses/{setup.course_id}/roster",
        json={"entries": [{"github_login": "alice", "team": "Alpha"}]},
    )

    alice = sign_in("alice")
    assert roster.json()["applied"] == 1
    assert alice.get(f"{teams}/{setup.alpha}").status_code == 200
    assert alice.get(f"{teams}/{setup.pixel}").status_code == 403


def test_a_claim_skips_a_team_archived_since_the_import(sign_in: SignIn, database_url: str) -> None:
    setup = course_with_two_teams(sign_in)
    setup.instructor.post(
        f"/api/courses/{setup.course_id}/roster",
        json={"entries": [{"github_login": "newbie", "team": "Pixel"}]},
    )
    run_sql(database_url, "UPDATE teams SET archived_at = now() WHERE id = %s", (setup.pixel,))
    newbie = sign_in("newbie")
    user_id = run_sql(database_url, "SELECT id FROM users WHERE github_login = 'newbie'")[0]["id"]

    asyncio.run(_claim(database_url, user_id))

    course = newbie.get(f"/api/courses/{setup.course_id}").json()
    assert (course["role"], course["teams"]) == ("student", [])
    memberships = run_sql(
        database_url, "SELECT id FROM team_memberships WHERE user_id = %s", (user_id,)
    )
    assert memberships == []


async def _claim(url: str, user_id: object) -> None:
    async with await connect(url) as connection:
        await claim_roster_at_sign_in(connection, cast(UUID, user_id), "newbie")
