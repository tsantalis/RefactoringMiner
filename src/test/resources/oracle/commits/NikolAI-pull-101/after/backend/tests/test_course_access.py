"""The single read-access rule and join-code rules (US-10, ADR 0001 decisions 5 and 9)."""

# AI contribution: 50%

from uuid import UUID, uuid4

import pytest
from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db import connect
from app.db.repositories.courses import (
    JoinCodeError,
    can_read_team,
    is_team_member,
    my_courses,
    new_join_code,
    normalize_join_code,
)

ALPHABET = frozenset("23456789ABCDEFGHJKMNPQRSTUVWXYZ")


def test_join_codes_are_eight_unambiguous_characters() -> None:
    codes = {new_join_code() for _ in range(50)}

    assert len(codes) == 50
    for code in codes:
        assert len(code) == 8
        assert set(code) <= ALPHABET


def test_join_codes_are_trimmed_and_upper_cased() -> None:
    assert normalize_join_code(" abcd2345 ") == "ABCD2345"


@pytest.mark.parametrize(
    "code",
    ["", "ABCD234", "ABCD23456", "ABCD234O", "ABCD2341", "ABCD234L", "ABCD 345"],
)
def test_malformed_join_codes_are_rejected(code: str) -> None:
    with pytest.raises(JoinCodeError):
        normalize_join_code(code)


_PEOPLE = ("instructor", "ta", "member", "other", "outsider", "revoked")
_ROLES = (
    ("instructor", "instructor"),
    ("ta", "ta"),
    ("member", "student"),
    ("other", "student"),
)


async def _seed_people(connection: AsyncConnection[DictRow], ids: dict[str, UUID]) -> None:
    """The six users, the course, its two teams and the course roles."""
    for index, name in enumerate(_PEOPLE, start=1):
        await connection.execute(
            "INSERT INTO users (id, github_user_id, github_login) VALUES (%s, %s, %s)",
            (ids[name], index, name),
        )
    await connection.execute(
        "INSERT INTO courses (id, name, join_code, created_by_user_id) VALUES (%s, %s, %s, %s)",
        (ids["course"], "SOEN 490", "ABCD2345", ids["instructor"]),
    )
    for name in ("team_a", "team_b"):
        await connection.execute(
            "INSERT INTO teams (id, name, course_id) VALUES (%s, %s, %s)",
            (ids[name], name.replace("_", " ").title(), ids["course"]),
        )
    for name, role in _ROLES:
        await connection.execute(
            "INSERT INTO course_memberships (course_id, user_id, role) VALUES (%s, %s, %s)",
            (ids["course"], ids[name], role),
        )
    for name, team in (("member", "team_a"), ("other", "team_b")):
        await connection.execute(
            "INSERT INTO team_memberships (team_id, user_id, role) VALUES (%s, %s, 'member')",
            (ids[team], ids[name]),
        )


async def _seed_access_matrix(database_url: str) -> dict[str, UUID]:
    """One course with two teams, a member per team, a TA, an instructor and two grants."""
    ids = {name: uuid4() for name in (*_PEOPLE, "course", "team_a", "team_b")}
    async with await connect(database_url) as connection, connection.transaction():
        await _seed_people(connection, ids)
        await connection.execute(
            """INSERT INTO team_access_grants
                   (team_id, user_id, access_role, granted_by_user_id)
               VALUES (%s, %s, 'viewer', %s)""",
            (ids["team_a"], ids["outsider"], ids["instructor"]),
        )
        await connection.execute(
            """INSERT INTO team_access_grants
                   (team_id, user_id, access_role, granted_by_user_id, granted_at, revoked_at)
               VALUES (%s, %s, 'assessor', %s, now() - interval '1 day', now())""",
            (ids["team_a"], ids["revoked"], ids["instructor"]),
        )
    return ids


async def test_can_read_team_matrix(database_url: str) -> None:
    ids = await _seed_access_matrix(database_url)

    async with await connect(database_url) as connection:
        access = {
            name: await can_read_team(connection, user_id=ids[name], team_id=ids["team_a"])
            for name in ("member", "other", "ta", "instructor", "outsider", "revoked")
        }
        member_of = {
            name: await is_team_member(connection, user_id=ids[name], team_id=ids["team_a"])
            for name in ("member", "ta", "outsider")
        }

    assert access == {
        "member": True,
        "other": False,
        "ta": True,
        "instructor": True,
        "outsider": True,
        "revoked": False,
    }
    assert member_of == {"member": True, "ta": False, "outsider": False}


async def test_an_ended_team_membership_loses_access(database_url: str) -> None:
    ids = await _seed_access_matrix(database_url)

    async with await connect(database_url) as connection:
        await connection.execute(
            """UPDATE team_memberships
               SET left_at = GREATEST(now(), joined_at + interval '1 microsecond')
               WHERE user_id = %s AND team_id = %s""",
            (ids["member"], ids["team_a"]),
        )
        assert (
            await can_read_team(connection, user_id=ids["member"], team_id=ids["team_a"]) is False
        )


async def test_my_courses_shows_only_the_teams_the_user_may_read(database_url: str) -> None:
    ids = await _seed_access_matrix(database_url)

    async with await connect(database_url) as connection:
        member_courses = await my_courses(connection, ids["member"])
        ta_courses = await my_courses(connection, ids["ta"])

    assert [team.name for team in member_courses[0].teams] == ["Team A"]
    assert [team.name for team in ta_courses[0].teams] == ["Team A", "Team B"]
