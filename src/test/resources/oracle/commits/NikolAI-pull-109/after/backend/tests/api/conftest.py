"""Fixtures for the course and roster API tests: one signed-in client per test user."""

# AI contribution: 50%

import asyncio
from collections.abc import Iterator

import pytest
from fastapi.testclient import TestClient
from pydantic import SecretStr

from app.core.config import Settings, get_settings
from app.github.oauth import GitHubUser
from app.main import app
from app.services.sessions import SESSION_COOKIE
from tests.api.courses_support import ORIGIN, SignIn, open_session, run_sql
from tests.fakes import WEBHOOK_SECRET


@pytest.fixture
def course_settings(database_url: str) -> Settings:
    return Settings(
        _env_file=None,
        database_url=SecretStr(database_url),
        allowed_origins=[ORIGIN],
        github_webhook_secret=SecretStr(WEBHOOK_SECRET),
    )


@pytest.fixture
def sign_in(course_settings: Settings, database_url: str) -> Iterator[SignIn]:
    """``sign_in("login")`` returns a client holding that user's session cookie."""
    app.dependency_overrides[get_settings] = lambda: course_settings
    github_ids: dict[str, int] = {}

    def client_for(login: str, *, instructor: bool = False) -> TestClient:
        github_id = github_ids.setdefault(login, 1000 + len(github_ids))
        token = asyncio.run(open_session(database_url, GitHubUser(id=github_id, login=login)))
        if instructor:
            run_sql(
                database_url,
                "UPDATE users SET is_instructor = true WHERE github_user_id = %s",
                (github_id,),
            )
        client = TestClient(app, base_url="https://testserver", headers={"Origin": ORIGIN})
        client.cookies.set(SESSION_COOKIE, token)
        return client

    try:
        yield client_for
    finally:
        app.dependency_overrides.clear()
