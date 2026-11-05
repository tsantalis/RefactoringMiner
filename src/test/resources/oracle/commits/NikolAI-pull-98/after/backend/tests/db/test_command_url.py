"""How a one-off operator command picks its database URL."""

# AI contribution: 50%

import pytest
from pydantic import SecretStr

from app.core.config import ConfigurationError, Settings
from app.db import command_url


def _use_database_url(monkeypatch: pytest.MonkeyPatch, url: str | None) -> None:
    secret = SecretStr(url) if url is not None else None
    settings = Settings(_env_file=None).model_copy(update={"database_url": secret})
    monkeypatch.setattr(command_url, "Settings", lambda: settings)


def test_without_a_prompt_the_url_comes_from_the_settings(monkeypatch: pytest.MonkeyPatch) -> None:
    _use_database_url(monkeypatch, " postgresql://from-env ")

    assert command_url.command_database_url(prompt=None) == "postgresql://from-env"


def test_a_prompt_wins_over_the_settings(monkeypatch: pytest.MonkeyPatch) -> None:
    _use_database_url(monkeypatch, "postgresql://from-env")
    monkeypatch.setattr(command_url, "getpass", lambda prompt: "postgresql://typed")

    assert command_url.command_database_url(prompt="URL: ") == "postgresql://typed"


@pytest.mark.parametrize("url", [None, "   "])
def test_no_url_is_a_configuration_error(monkeypatch: pytest.MonkeyPatch, url: str | None) -> None:
    _use_database_url(monkeypatch, url)

    with pytest.raises(ConfigurationError, match="DATABASE_URL"):
        command_url.command_database_url(prompt=None)
