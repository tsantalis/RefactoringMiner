"""``python -m app.cli grant-instructor`` against real PostgreSQL."""

# AI contribution: 50%

import asyncio

import pytest
from psycopg import OperationalError
from pydantic import SecretStr

from app import cli
from app.core.config import Settings
from app.db import connect


@pytest.fixture
async def settings(database_url: str) -> Settings:
    async with await connect(database_url) as connection:
        await connection.execute(
            "INSERT INTO users (github_user_id, github_login) VALUES (1, 'OctoCat')"
        )
    return Settings(_env_file=None, database_url=SecretStr(database_url))


async def is_instructor(database_url: str) -> bool:
    async with await connect(database_url) as connection:
        cursor = await connection.execute("SELECT is_instructor FROM users")
        row = await cursor.fetchone()
    assert row is not None
    return bool(row["is_instructor"])


async def _grant(settings: Settings, login: str) -> int:
    return await asyncio.to_thread(cli.main, ["grant-instructor", login], settings=settings)


async def test_the_grant_reaches_the_database_and_ignores_login_case(
    settings: Settings, database_url: str, capsys: pytest.CaptureFixture[str]
) -> None:
    assert await is_instructor(database_url) is False

    assert await _grant(settings, "octocat") == 0

    assert await is_instructor(database_url) is True
    assert "octocat is now an instructor" in capsys.readouterr().out


def test_an_unknown_login_fails_cleanly(
    settings: Settings, capsys: pytest.CaptureFixture[str]
) -> None:
    assert cli.main(["grant-instructor", "nobody"], settings=settings) == 1
    assert "error: No user with GitHub login 'nobody'" in capsys.readouterr().err


async def test_two_rows_with_one_login_are_refused(
    settings: Settings, database_url: str, capsys: pytest.CaptureFixture[str]
) -> None:
    async with await connect(database_url) as connection:
        await connection.execute(
            "INSERT INTO users (github_user_id, github_login) VALUES (2, 'octocat')"
        )

    assert await _grant(settings, "octocat") == 1

    assert "Several users" in capsys.readouterr().err
    async with await connect(database_url) as connection:
        cursor = await connection.execute("SELECT count(*) AS n FROM users WHERE is_instructor")
        assert await cursor.fetchone() == {"n": 0}


def test_a_missing_database_url_is_actionable(capsys: pytest.CaptureFixture[str]) -> None:
    assert cli.main(["grant-instructor", "x"], settings=Settings(_env_file=None)) == 1
    assert "DATABASE_URL" in capsys.readouterr().err


def test_a_database_failure_hides_connection_details(
    monkeypatch: pytest.MonkeyPatch, capsys: pytest.CaptureFixture[str]
) -> None:
    async def fail(url: str) -> None:
        raise OperationalError("connection failed: secret-password")

    monkeypatch.setattr(cli, "connect", fail)
    settings = Settings(_env_file=None, database_url=SecretStr("postgresql://test-only"))

    assert cli.main(["grant-instructor", "x"], settings=settings) == 1
    assert "secret-password" not in capsys.readouterr().err


def test_the_login_argument_is_required() -> None:
    with pytest.raises(SystemExit):
        cli.main(["grant-instructor"], settings=Settings(_env_file=None))
