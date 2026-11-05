"""Operator command chooses the intended database and never prints connection secrets."""

# AI contribution: 50%

import sys
from unittest.mock import AsyncMock
from uuid import uuid4

import pytest
from psycopg import OperationalError
from pydantic import SecretStr

from app.core.config import Settings
from app.db import command_url
from app.db import register_repository as registration


@pytest.fixture
def cli(monkeypatch: pytest.MonkeyPatch) -> tuple[AsyncMock, AsyncMock]:
    monkeypatch.setattr(
        sys,
        "argv",
        [
            "register_repository",
            "--team-id",
            str(uuid4()),
            "--team-name",
            "Test team",
            "--github-repository-id",
            "1403683565",
            "--repository",
            "R-Mauricio13/Capstone_test_repo",
        ],
    )
    settings = Settings(_env_file=None).model_copy(
        update={"database_url": SecretStr("postgresql://test-only")}
    )
    monkeypatch.setattr(command_url, "Settings", lambda: settings)
    connect = AsyncMock(return_value=AsyncMock())
    register = AsyncMock(return_value=uuid4())
    monkeypatch.setattr(registration, "connect", connect)
    monkeypatch.setattr(registration, "register_repository", register)
    return connect, register


@pytest.mark.parametrize("prompt", [False, True])
def test_register_command(
    cli: tuple[AsyncMock, AsyncMock],
    monkeypatch: pytest.MonkeyPatch,
    capsys: pytest.CaptureFixture[str],
    prompt: bool,
) -> None:
    connect, register = cli
    if prompt:
        monkeypatch.setattr(sys, "argv", [*sys.argv, "--prompt-database-url"])
        monkeypatch.setattr(command_url, "getpass", lambda _: "postgresql://prompt-only")
    registration.main()
    connect.assert_awaited_once_with(
        "postgresql://prompt-only" if prompt else "postgresql://test-only"
    )
    register.assert_awaited_once()
    assert register.await_args is not None
    assert register.await_args.kwargs["github_repository_id"] == 1403683565
    output = capsys.readouterr().out
    assert "Registered repository" in output
    assert "postgresql" not in output


def test_database_failure_hides_credentials(
    cli: tuple[AsyncMock, AsyncMock],
    capsys: pytest.CaptureFixture[str],
) -> None:
    cli[0].side_effect = OperationalError("connection failed: secret-password")
    with pytest.raises(SystemExit) as error:
        registration.main()
    assert error.value.code == 1
    output = capsys.readouterr().err
    assert "check database connectivity" in output
    assert "secret-password" not in output


def test_missing_database_is_actionable(
    cli: tuple[AsyncMock, AsyncMock],
    monkeypatch: pytest.MonkeyPatch,
    capsys: pytest.CaptureFixture[str],
) -> None:
    settings = Settings(_env_file=None).model_copy(update={"database_url": None})
    monkeypatch.setattr(command_url, "Settings", lambda: settings)
    with pytest.raises(SystemExit) as error:
        registration.main()
    assert error.value.code == 1
    assert "DATABASE_URL" in capsys.readouterr().err
    cli[0].assert_not_awaited()
