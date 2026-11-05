"""Fixtures shared by the API tests that need a signed-in user and a real database."""

# AI contribution: 50%

from collections.abc import Iterator

import httpx
import pytest
from fastapi.testclient import TestClient

from app.api.routes.auth import get_github_transport
from app.core.config import Settings, get_settings
from app.main import app
from tests.api.support import FakeGitHub, settings_for


@pytest.fixture
def github() -> FakeGitHub:
    return FakeGitHub()


@pytest.fixture
def settings(database_url: str) -> Settings:
    return settings_for(database_url)


@pytest.fixture
def client(settings: Settings, github: FakeGitHub) -> Iterator[TestClient]:
    app.dependency_overrides[get_settings] = lambda: settings
    app.dependency_overrides[get_github_transport] = lambda: httpx.MockTransport(github)
    try:
        yield TestClient(app, base_url="https://testserver", follow_redirects=False)
    finally:
        app.dependency_overrides.clear()
