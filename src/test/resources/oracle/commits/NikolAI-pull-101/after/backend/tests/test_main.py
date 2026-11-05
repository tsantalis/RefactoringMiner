"""Tests for the FastAPI app: the health check and the shared LLM provider's lifecycle."""

# AI contribution: 50%

import asyncio
from pathlib import Path

import pytest
from fastapi.testclient import TestClient
from pydantic import SecretStr

import app.main as main_module
from app.core.config import ConfigurationError, Settings
from tests.fakes import FakeProvider


def test_health_check() -> None:
    response = TestClient(main_module.app).get("/")

    assert response.status_code == 200
    assert response.json() == {"message": "NikolAI backend running"}


def test_startup_creates_one_provider_and_shutdown_closes_it(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    provider = FakeProvider()
    monkeypatch.setattr(main_module, "get_settings", lambda: Settings(_env_file=None))
    monkeypatch.setattr(main_module, "create_provider", lambda _settings: provider)

    with TestClient(main_module.app):
        assert main_module.app.state.llm_provider is provider
        assert not provider.closed

    assert provider.closed


def test_reviews_are_disabled_without_an_openrouter_key(
    monkeypatch: pytest.MonkeyPatch, caplog: pytest.LogCaptureFixture
) -> None:
    monkeypatch.setattr(main_module, "get_settings", lambda: Settings(_env_file=None))

    with TestClient(main_module.app):
        assert main_module.app.state.llm_provider is None

    assert "PR reviews are disabled" in caplog.text


async def test_lifespan_refuses_production_startup_when_settings_are_missing(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    settings = Settings(
        _env_file=None,
        nikolai_env="production",
        github_private_key_path=Path("/nonexistent/key.pem"),
    )
    monkeypatch.setattr(main_module, "get_settings", lambda: settings)

    with pytest.raises(ConfigurationError, match="missing or blank settings"):
        await main_module.lifespan(main_module.app).__aenter__()


def test_lifespan_starts_when_production_settings_are_complete(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    provider = FakeProvider()
    settings = Settings(
        _env_file=None,
        nikolai_env="production",
        openrouter_api_key=SecretStr("sk-or-v1-test-value"),
        database_url=SecretStr("postgresql+psycopg://nikolai:nikolai@db:5432/nikolai"),
        github_app_id="123456",
        github_webhook_secret=SecretStr("test-webhook-secret"),
        github_private_key=SecretStr("fake-key"),
        github_client_id="test-client-id",
        github_client_secret=SecretStr("test-client-secret"),
        session_secret=SecretStr("test-session-secret"),
        allowed_origins=["https://dashboard.test"],
    )
    monkeypatch.setattr(main_module, "get_settings", lambda: settings)
    monkeypatch.setattr(main_module, "create_provider", lambda _settings: provider)

    with TestClient(main_module.app):
        assert main_module.app.state.llm_provider is provider


async def test_lifespan_starts_and_cancels_recovery_before_closing_provider(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    provider = FakeProvider()
    settings = Settings(_env_file=None).model_copy(update={"database_url": SecretStr("test")})
    monkeypatch.setattr(main_module, "get_settings", lambda: settings)
    monkeypatch.setattr(main_module, "create_provider", lambda _settings: provider)
    entered = asyncio.Event()
    stopped = asyncio.Event()

    async def worker(config: Settings, llm: object) -> None:
        entered.set()
        try:
            await asyncio.Event().wait()
        finally:
            assert not provider.closed
            stopped.set()

    monkeypatch.setattr(main_module, "run_review_worker", worker)
    async with main_module.lifespan(main_module.app):
        await asyncio.wait_for(entered.wait(), 1)
    assert stopped.is_set()
    assert provider.closed
