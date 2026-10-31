"""The migration runner against disposable, real PostgreSQL databases."""

# AI contribution: 50%

import sys
from pathlib import Path
from unittest.mock import AsyncMock

import pytest
from psycopg import OperationalError, errors
from pydantic import SecretStr

from app.core.config import Settings
from app.db import connect, init_schema, migrate


async def _recorded(connection: migrate.Connection) -> list[str]:
    cursor = await connection.execute("SELECT filename FROM schema_migrations ORDER BY filename")
    return [row["filename"] for row in await cursor.fetchall()]


async def test_applies_every_migration_to_an_empty_database(empty_database_url: str) -> None:
    async with await connect(empty_database_url) as c:
        applied = await migrate.apply_migrations(c)

        assert applied[:2] == ["0001_initial_schema.sql", "0002_user_sessions.sql"]
        assert await _recorded(c) == applied


async def test_a_database_set_up_by_init_schema_gets_only_the_newer_migrations(
    empty_database_url: str,
) -> None:
    async with await connect(empty_database_url) as c:
        await init_schema.initialize_schema(c)
        await c.execute("INSERT INTO teams (name) VALUES ('Keep this data')")

        applied = await migrate.apply_migrations(c)

        assert "0001_initial_schema.sql" not in applied
        assert "0002_user_sessions.sql" in applied
        assert "0001_initial_schema.sql" in await _recorded(c)
        cursor = await c.execute("SELECT name FROM teams")
        assert await cursor.fetchall() == [{"name": "Keep this data"}]


async def test_a_second_run_applies_nothing(empty_database_url: str) -> None:
    async with await connect(empty_database_url) as c:
        await migrate.apply_migrations(c)

        assert await migrate.apply_migrations(c) == []


async def test_a_failing_migration_rolls_back_the_whole_run(
    empty_database_url: str, tmp_path: Path
) -> None:
    (tmp_path / "0001_good.sql").write_text("CREATE TABLE good (id integer);", encoding="utf-8")
    (tmp_path / "0002_bad.sql").write_text("SELECT 1/0;", encoding="utf-8")
    async with await connect(empty_database_url) as c:
        with pytest.raises(errors.DivisionByZero):
            await migrate.apply_migrations(c, directory=tmp_path)

        cursor = await c.execute(
            "SELECT to_regclass('public.good') AS good, "
            "to_regclass('public.schema_migrations') AS log"
        )
        assert await cursor.fetchone() == {"good": None, "log": None}


@pytest.fixture
def cli(monkeypatch: pytest.MonkeyPatch) -> AsyncMock:
    monkeypatch.setattr(sys, "argv", ["migrate"])
    settings = Settings(_env_file=None).model_copy(
        update={"database_url": SecretStr("postgresql://test-only")}
    )
    monkeypatch.setattr(migrate, "Settings", lambda: settings)
    connect_mock = AsyncMock(return_value=AsyncMock())
    monkeypatch.setattr(migrate, "connect", connect_mock)
    return connect_mock


def test_cli_reports_the_applied_files(
    cli: AsyncMock, monkeypatch: pytest.MonkeyPatch, capsys: pytest.CaptureFixture[str]
) -> None:
    monkeypatch.setattr(migrate, "apply_migrations", AsyncMock(return_value=["0002_x.sql"]))

    migrate.main()

    assert capsys.readouterr().out == "Applied 0002_x.sql\n"


def test_cli_says_when_the_database_is_up_to_date(
    cli: AsyncMock, monkeypatch: pytest.MonkeyPatch, capsys: pytest.CaptureFixture[str]
) -> None:
    monkeypatch.setattr(migrate, "apply_migrations", AsyncMock(return_value=[]))

    migrate.main()

    assert capsys.readouterr().out == "Database is up to date.\n"


def test_cli_failure_hides_connection_details(
    cli: AsyncMock, capsys: pytest.CaptureFixture[str]
) -> None:
    cli.side_effect = OperationalError("connection failed: secret-password")

    with pytest.raises(SystemExit) as error:
        migrate.main()

    assert error.value.code == 1
    assert "secret-password" not in capsys.readouterr().err
