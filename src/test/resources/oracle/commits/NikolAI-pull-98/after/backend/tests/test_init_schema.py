"""Initial schema setup against disposable, genuinely empty PostgreSQL databases."""

# AI contribution: 50%

import asyncio
import sys
from pathlib import Path
from unittest.mock import AsyncMock

import pytest
from psycopg import OperationalError, errors
from pydantic import SecretStr

from app.core.config import Settings
from app.db import connect, init_schema


async def test_initializes_real_schema_and_refuses_rerun(empty_database_url: str) -> None:
    async with await connect(empty_database_url) as c:
        await init_schema.initialize_schema(c)
        rows = await (
            await c.execute("SELECT tablename FROM pg_tables WHERE schemaname='public'")
        ).fetchall()
        assert {
            "teams",
            "repositories",
            "pull_requests",
            "webhook_events",
            "analysis_runs",
            "analysis_outputs",
        } <= {row["tablename"] for row in rows}
        await c.execute("INSERT INTO teams (name) VALUES ('Keep this data')")
        with pytest.raises(init_schema.SchemaNotEmptyError):
            await init_schema.initialize_schema(c)
        assert await (await c.execute("SELECT name FROM teams")).fetchall() == [
            {"name": "Keep this data"}
        ]


async def test_refuses_partial_schema(empty_database_url: str) -> None:
    async with await connect(empty_database_url) as c:
        await c.execute("CREATE TABLE existing_data (value text)")
        await c.execute("INSERT INTO existing_data VALUES ('keep')")
        with pytest.raises(init_schema.SchemaNotEmptyError):
            await init_schema.initialize_schema(c)
        assert await (await c.execute("SELECT value FROM existing_data")).fetchall() == [
            {"value": "keep"}
        ]
        assert await (await c.execute("SELECT to_regclass('public.teams') AS name")).fetchone() == {
            "name": None
        }


async def test_sql_failure_rolls_back_all_changes(
    empty_database_url: str, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    broken = tmp_path / "broken.sql"
    broken.write_text("CREATE TABLE should_rollback (id integer); SELECT 1/0;", encoding="utf-8")
    monkeypatch.setattr(init_schema, "MIGRATION", broken)
    async with await connect(empty_database_url) as c:
        with pytest.raises(errors.DivisionByZero):
            await init_schema.initialize_schema(c)
        assert await (
            await c.execute("SELECT to_regclass('public.should_rollback') AS name")
        ).fetchone() == {"name": None}


async def test_concurrent_initialization_applies_once(empty_database_url: str) -> None:
    async with (
        await connect(empty_database_url) as first,
        await connect(empty_database_url) as second,
    ):
        results = await asyncio.gather(
            init_schema.initialize_schema(first),
            init_schema.initialize_schema(second),
            return_exceptions=True,
        )
        assert results.count(None) == 1
        assert sum(isinstance(result, init_schema.SchemaNotEmptyError) for result in results) == 1


@pytest.fixture
def cli(monkeypatch: pytest.MonkeyPatch) -> tuple[AsyncMock, AsyncMock]:
    monkeypatch.setattr(sys, "argv", ["init_schema"])
    settings = Settings(_env_file=None).model_copy(
        update={"database_url": SecretStr("postgresql://test-only")}
    )
    monkeypatch.setattr(init_schema, "Settings", lambda: settings)
    connect_mock = AsyncMock(return_value=AsyncMock())
    initialize_mock = AsyncMock()
    monkeypatch.setattr(init_schema, "connect", connect_mock)
    monkeypatch.setattr(init_schema, "initialize_schema", initialize_mock)
    return connect_mock, initialize_mock


@pytest.mark.parametrize("prompt", [False, True])
def test_cli_selects_database(
    cli: tuple[AsyncMock, AsyncMock],
    prompt: bool,
    monkeypatch: pytest.MonkeyPatch,
    capsys: pytest.CaptureFixture[str],
) -> None:
    if prompt:
        monkeypatch.setattr(sys, "argv", ["init_schema", "--prompt-database-url"])
        monkeypatch.setattr(init_schema, "getpass", lambda _: "postgresql://prompt-only")
    init_schema.main()
    cli[0].assert_awaited_once_with(
        "postgresql://prompt-only" if prompt else "postgresql://test-only"
    )
    cli[1].assert_awaited_once()
    assert capsys.readouterr().out == "Applied 0001_initial_schema.sql successfully.\n"


@pytest.mark.parametrize(
    "failure",
    [
        OperationalError("secret-connection"),
        OSError("secret-path"),
        init_schema.SchemaNotEmptyError("secret-table"),
    ],
)
def test_cli_errors_do_not_expose_details(
    cli: tuple[AsyncMock, AsyncMock], failure: Exception, capsys: pytest.CaptureFixture[str]
) -> None:
    cli[1].side_effect = failure
    with pytest.raises(SystemExit) as error:
        init_schema.main()
    assert error.value.code == 1
    output = capsys.readouterr()
    assert "secret-" not in output.err
    assert not output.out


def test_cli_requires_database(
    cli: tuple[AsyncMock, AsyncMock],
    monkeypatch: pytest.MonkeyPatch,
    capsys: pytest.CaptureFixture[str],
) -> None:
    monkeypatch.setattr(init_schema, "Settings", lambda: Settings(_env_file=None))
    with pytest.raises(SystemExit) as error:
        init_schema.main()
    assert error.value.code == 1
    assert "DATABASE_URL" in capsys.readouterr().err
    cli[0].assert_not_awaited()
