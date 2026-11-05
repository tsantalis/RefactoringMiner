"""Tests for loading the backend settings."""

from pathlib import Path

import pytest
from pydantic import ValidationError

from app.core.config import BACKEND_DIR, ConfigurationError, Settings, get_settings


@pytest.mark.parametrize(
    ("value", "expected"),
    [
        (" backup/one, ,main/model,backup/one, backup/two ", ["backup/one", "backup/two"]),
        ("", []),
        ("   ,  ", []),
    ],
)
def test_fallback_environment_preserves_order_and_removes_unusable_entries(
    monkeypatch: pytest.MonkeyPatch, value: str, expected: list[str]
) -> None:
    monkeypatch.setenv("OPENROUTER_MODEL", "main/model")
    monkeypatch.setenv("OPENROUTER_FALLBACK_MODELS", value)
    assert Settings(_env_file=None).openrouter_fallback_models == expected


def test_fallback_list_is_normalized_and_reasoning_is_loaded_from_file(tmp_path: Path) -> None:
    env_file = tmp_path / ".env"
    env_file.write_text(
        "OPENROUTER_FALLBACK_MODELS=backup/one,backup/two\n"
        "OPENROUTER_FALLBACK_REASONING_EFFORT=none\n",
        encoding="utf-8",
    )
    settings = Settings(_env_file=env_file)
    assert settings.openrouter_fallback_models == ["backup/one", "backup/two"]
    assert settings.openrouter_fallback_reasoning_effort == "none"
    normalized = Settings(
        _env_file=None,
        openrouter_fallback_models=[" backup/one ", "", "backup/one"],
    )
    assert normalized.openrouter_fallback_models == ["backup/one"]


def test_invalid_fallback_list_is_rejected() -> None:
    with pytest.raises(ValidationError, match="openrouter_fallback_models"):
        Settings.model_validate({"openrouter_fallback_models": 123})


@pytest.mark.parametrize("effort", ["", "extreme"])
def test_invalid_backup_reasoning_effort_is_rejected(
    monkeypatch: pytest.MonkeyPatch, effort: str
) -> None:
    monkeypatch.setenv("OPENROUTER_FALLBACK_REASONING_EFFORT", effort)
    with pytest.raises(ValidationError, match="openrouter_fallback_reasoning_effort"):
        Settings(_env_file=None)


def test_defaults_use_the_free_model_and_private_routing() -> None:
    settings = Settings(_env_file=None)

    assert settings.openrouter_model == "cohere/north-mini-code:free"
    assert settings.openrouter_data_collection == "deny"
    assert settings.openrouter_reasoning_effort is None
    assert settings.openrouter_api_key is None


def test_env_example_matches_the_code_defaults() -> None:
    """`.env.example` documents the defaults, so it must not drift from the code."""
    example = Settings(_env_file=BACKEND_DIR / ".env.example")
    defaults = Settings(_env_file=None)

    assert example.openrouter_model == defaults.openrouter_model
    assert example.openrouter_data_collection == defaults.openrouter_data_collection
    assert example.openrouter_reasoning_effort == defaults.openrouter_reasoning_effort
    assert example.openrouter_api_key is not None
    assert example.openrouter_api_key.get_secret_value() == ""  # no real key in the example


def test_reads_values_from_an_env_file_and_hides_the_key(tmp_path: Path) -> None:
    env_file = tmp_path / ".env"
    env_file.write_text(
        "OPENROUTER_API_KEY=sk-or-v1-from-file\n"
        "OPENROUTER_MODEL=anthropic/claude-haiku-4.5\n"
        "OPENROUTER_REASONING_EFFORT=low\n",
        encoding="utf-8",
    )
    settings = Settings(_env_file=env_file)

    assert settings.openrouter_api_key is not None
    assert settings.openrouter_api_key.get_secret_value() == "sk-or-v1-from-file"
    assert settings.openrouter_model == "anthropic/claude-haiku-4.5"
    assert settings.openrouter_reasoning_effort == "low"
    assert "sk-or-v1-from-file" not in repr(settings)


def test_environment_variables_override_the_defaults(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("OPENROUTER_MODEL", "anthropic/claude-haiku-4.5")
    assert Settings(_env_file=None).openrouter_model == "anthropic/claude-haiku-4.5"


@pytest.mark.parametrize(
    ("variable", "value"),
    [
        ("OPENROUTER_REASONING_EFFORT", "extreme"),
        ("OPENROUTER_DATA_COLLECTION", "sometimes"),
        ("OPENROUTER_MAX_RETRIES", "-1"),
    ],
)
def test_rejects_invalid_values(monkeypatch: pytest.MonkeyPatch, variable: str, value: str) -> None:
    monkeypatch.setenv(variable, value)
    with pytest.raises(ValidationError):
        Settings(_env_file=None)


def test_get_settings_is_cached() -> None:
    get_settings.cache_clear()
    try:
        assert get_settings() is get_settings()
    finally:
        get_settings.cache_clear()


# ---- GitHub App settings (US-2) -----------------------------------------------------------


def test_github_settings_accept_team_alphas_env_names(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("APP_ID", "123456")
    monkeypatch.setenv("WEBHOOK_SECRET", "s3cret-value")
    settings = Settings(_env_file=None)

    assert settings.github_app_id == "123456"
    assert settings.github_webhook_secret is not None
    assert settings.github_webhook_secret.get_secret_value() == "s3cret-value"
    assert "s3cret-value" not in repr(settings)


def test_github_settings_also_accept_prefixed_env_names(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("GITHUB_APP_ID", "654321")
    monkeypatch.setenv("GITHUB_WEBHOOK_SECRET", "other-secret")
    settings = Settings(_env_file=None)

    assert settings.github_app_id == "654321"
    assert settings.github_webhook_secret is not None
    assert settings.github_webhook_secret.get_secret_value() == "other-secret"


def test_pr_log_dir_can_be_moved(monkeypatch: pytest.MonkeyPatch, tmp_path: Path) -> None:
    monkeypatch.setenv("PR_LOG_DIR", str(tmp_path))
    assert Settings(_env_file=None).pr_log_dir == tmp_path


def test_private_key_from_env_gets_its_line_breaks_back(
    monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    # Railway-style: the whole PEM key on one line, with literal "\n" sequences.
    monkeypatch.setenv("GITHUB_PRIVATE_KEY", r"-----BEGIN KEY-----\nabc\n-----END KEY-----")
    settings = Settings(_env_file=None, github_private_key_path=tmp_path / "unused.pem")

    assert settings.read_github_private_key() == "-----BEGIN KEY-----\nabc\n-----END KEY-----"


def test_private_key_falls_back_to_the_key_file(tmp_path: Path) -> None:
    key_file = tmp_path / "private-key.pem"
    key_file.write_text("key-from-file", encoding="utf-8")

    settings = Settings(_env_file=None, github_private_key_path=key_file)

    assert settings.read_github_private_key() == "key-from-file"


def test_a_blank_private_key_env_var_falls_back_to_the_file(
    monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    monkeypatch.setenv("GITHUB_PRIVATE_KEY", "   ")
    key_file = tmp_path / "private-key.pem"
    key_file.write_text("key-from-file", encoding="utf-8")

    settings = Settings(_env_file=None, github_private_key_path=key_file)

    assert settings.read_github_private_key() == "key-from-file"


def test_a_missing_private_key_explains_how_to_add_it(tmp_path: Path) -> None:
    settings = Settings(_env_file=None, github_private_key_path=tmp_path / "missing.pem")

    with pytest.raises(ConfigurationError, match="GITHUB_PRIVATE_KEY"):
        settings.read_github_private_key()


def test_env_example_holds_no_real_github_values() -> None:
    example = Settings(_env_file=BACKEND_DIR / ".env.example")

    assert not example.github_app_id
    assert (
        example.github_webhook_secret is None
        or not example.github_webhook_secret.get_secret_value()
    )
    assert example.github_private_key is None


# ---- Production settings (US-6) ----------------------------------------------------------


def test_production_lists_every_missing_setting() -> None:
    settings = Settings(
        _env_file=None,
        nikolai_env="production",
        github_private_key_path=Path("/nonexistent/key.pem"),
    )

    assert settings.missing_required_settings() == [
        "OPENROUTER_API_KEY",
        "DATABASE_URL",
        "APP_ID",
        "WEBHOOK_SECRET",
        "GITHUB_PRIVATE_KEY",
    ]


def test_production_raises_listing_every_missing_name() -> None:
    settings = Settings(
        _env_file=None,
        nikolai_env="production",
        github_private_key_path=Path("/nonexistent/key.pem"),
    )

    with pytest.raises(
        ConfigurationError,
        match="missing or blank settings: OPENROUTER_API_KEY, DATABASE_URL, APP_ID",
    ):
        settings.require_production_ready()


def test_blank_values_count_as_missing(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("OPENROUTER_API_KEY", "")
    monkeypatch.setenv("DATABASE_URL", "   ")
    settings = Settings(
        _env_file=None,
        nikolai_env="production",
        github_private_key_path=Path("/nonexistent/key.pem"),
    )

    missing = settings.missing_required_settings()
    assert "OPENROUTER_API_KEY" in missing
    assert "DATABASE_URL" in missing


def _set_complete_production_env(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("OPENROUTER_API_KEY", "sk-or-v1-test-value")
    monkeypatch.setenv("DATABASE_URL", "postgresql+psycopg://nikolai:nikolai@db:5432/nikolai")
    monkeypatch.setenv("APP_ID", "123456")
    monkeypatch.setenv("WEBHOOK_SECRET", "test-webhook-secret")
    monkeypatch.delenv("GITHUB_PRIVATE_KEY", raising=False)


def test_private_key_file_counts_as_present(
    monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    _set_complete_production_env(monkeypatch)
    key_file = tmp_path / "private-key.pem"
    key_file.write_text("fake-key", encoding="utf-8")
    settings = Settings(_env_file=None, nikolai_env="production", github_private_key_path=key_file)

    assert settings.missing_required_settings() == []
    settings.require_production_ready()  # must not raise


def test_an_empty_private_key_file_counts_as_missing(
    monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    _set_complete_production_env(monkeypatch)
    key_file = tmp_path / "private-key.pem"
    key_file.write_text("", encoding="utf-8")
    settings = Settings(_env_file=None, nikolai_env="production", github_private_key_path=key_file)

    assert settings.missing_required_settings() == ["GITHUB_PRIVATE_KEY"]
    with pytest.raises(ConfigurationError, match="GITHUB_PRIVATE_KEY"):
        settings.read_github_private_key()
    with pytest.raises(ConfigurationError, match="missing or blank settings: GITHUB_PRIVATE_KEY"):
        settings.require_production_ready()


def test_a_whitespace_only_private_key_file_counts_as_missing(
    monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    _set_complete_production_env(monkeypatch)
    key_file = tmp_path / "private-key.pem"
    key_file.write_text("  \n\t", encoding="utf-8")
    settings = Settings(_env_file=None, nikolai_env="production", github_private_key_path=key_file)

    assert settings.missing_required_settings() == ["GITHUB_PRIVATE_KEY"]
    with pytest.raises(ConfigurationError, match="GITHUB_PRIVATE_KEY"):
        settings.read_github_private_key()


def test_an_unreadable_private_key_file_counts_as_missing(
    monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    _set_complete_production_env(monkeypatch)
    key_file = tmp_path / "private-key.pem"
    key_file.write_text("fake-key", encoding="utf-8")

    def denied(self: Path, *args: object, **kwargs: object) -> str:
        raise PermissionError("permission denied")

    monkeypatch.setattr(Path, "read_text", denied)
    settings = Settings(_env_file=None, nikolai_env="production", github_private_key_path=key_file)

    assert settings.missing_required_settings() == ["GITHUB_PRIVATE_KEY"]
    with pytest.raises(ConfigurationError, match="GITHUB_PRIVATE_KEY"):
        settings.read_github_private_key()


def test_development_never_requires_settings() -> None:
    settings = Settings(
        _env_file=None,
        github_private_key_path=Path("/nonexistent/key.pem"),
    )

    settings.require_production_ready()  # must not raise even though nothing is set
