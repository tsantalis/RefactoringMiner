"""Sign-in, sessions and logout (US-11.1, ADR 0001) through the HTTP API.

Requests run against real PostgreSQL; only GitHub is faked. The client uses https:// because
the cookies are ``Secure``.
"""

# AI contribution: 50%

import asyncio
import hashlib
from collections.abc import Iterator
from urllib.parse import parse_qs, urlsplit

import httpx
import pytest
from fastapi.testclient import TestClient
from psycopg import OperationalError
from pydantic import SecretStr

from app.api import dependencies
from app.api.routes.auth import get_github_transport
from app.core.config import Settings, get_settings
from app.db import connect
from app.main import app

ORIGIN = "https://dashboard.test"
GITHUB_TOKEN = "gho_test_user_token"


class FakeGitHub:
    """GitHub's two OAuth endpoints, scripted per test."""

    def __init__(self) -> None:
        self.requests: list[httpx.Request] = []
        self.token_reply = httpx.Response(200, json={"access_token": GITHUB_TOKEN})
        self.user_reply = httpx.Response(200, json={"id": 42, "login": "octocat", "name": "Mona"})

    def __call__(self, request: httpx.Request) -> httpx.Response:
        self.requests.append(request)
        if request.url.path == "/login/oauth/access_token":
            return self.token_reply
        return self.user_reply


@pytest.fixture
def github() -> FakeGitHub:
    return FakeGitHub()


@pytest.fixture
def settings(database_url: str) -> Settings:
    return Settings(
        _env_file=None,
        database_url=SecretStr(database_url),
        github_client_id="client-id",
        github_client_secret=SecretStr("client-secret"),
        session_secret=SecretStr("session-secret"),
        allowed_origins=[ORIGIN],
    )


@pytest.fixture
def client(settings: Settings, github: FakeGitHub) -> Iterator[TestClient]:
    app.dependency_overrides[get_settings] = lambda: settings
    app.dependency_overrides[get_github_transport] = lambda: httpx.MockTransport(github)
    try:
        yield TestClient(app, base_url="https://testserver", follow_redirects=False)
    finally:
        app.dependency_overrides.clear()


def begin_login(client: TestClient) -> str:
    """Start sign-in and return the ``state`` sent to GitHub."""
    response = client.get("/auth/github/login")
    assert response.is_redirect
    location: str = response.headers["location"]
    return parse_qs(urlsplit(location).query)["state"][0]


def sign_in(client: TestClient) -> str:
    """Complete sign-in and return the session token from the cookie."""
    state = begin_login(client)
    response = client.get("/auth/github/callback", params={"code": "abc", "state": state})
    assert response.status_code == 303
    return str(response.cookies["nikolai_session"])


async def query(url: str, statement: str) -> list[dict[str, object]]:
    async with await connect(url) as connection:
        cursor = await connection.execute(statement.encode())
        return await cursor.fetchall() if cursor.description else []


def test_login_sends_the_browser_to_github(client: TestClient) -> None:
    response = client.get("/auth/github/login")

    location = urlsplit(response.headers["location"])
    assert (location.netloc, location.path) == ("github.com", "/login/oauth/authorize")
    assert parse_qs(location.query)["client_id"] == ["client-id"]
    assert "nikolai_oauth_state" in response.cookies


def test_a_valid_callback_creates_the_user_and_a_session(client: TestClient) -> None:
    state = begin_login(client)

    response = client.get("/auth/github/callback", params={"code": "abc", "state": state})

    assert response.status_code == 303
    assert response.headers["location"] == "/api/me"
    me = client.get("/api/me")
    assert me.status_code == 200
    assert me.json()["github_login"] == "octocat"
    assert me.json()["display_name"] == "Mona"
    assert me.json()["is_instructor"] is False


def test_the_session_cookie_has_the_adr_attributes(client: TestClient) -> None:
    state = begin_login(client)

    response = client.get("/auth/github/callback", params={"code": "abc", "state": state})

    cookie = next(
        value
        for value in response.headers.get_list("set-cookie")
        if value.startswith("nikolai_session=")
    )
    lowered = cookie.lower()
    assert "httponly" in lowered
    assert "secure" in lowered
    assert "samesite=none" in lowered
    assert "max-age=1209600" in lowered


async def test_only_the_tokens_hash_is_stored(client: TestClient, database_url: str) -> None:
    token = await asyncio.to_thread(sign_in, client)

    rows = await query(database_url, "SELECT token_hash FROM user_sessions")

    assert rows == [{"token_hash": hashlib.sha256(token.encode()).hexdigest()}]


def test_the_github_exchange_sends_the_code_and_uses_the_token_for_the_user_call(
    client: TestClient, github: FakeGitHub
) -> None:
    sign_in(client)

    exchange, user_call = github.requests
    assert b"abc" in exchange.content
    assert user_call.url.path == "/user"
    assert user_call.headers["authorization"] == f"Bearer {GITHUB_TOKEN}"


async def test_signing_in_again_updates_the_same_user(
    client: TestClient, github: FakeGitHub, database_url: str
) -> None:
    await asyncio.to_thread(sign_in, client)
    github.user_reply = httpx.Response(200, json={"id": 42, "login": "renamed", "name": None})
    await asyncio.to_thread(sign_in, client)

    rows = await query(database_url, "SELECT github_login, display_name FROM users")

    assert rows == [{"github_login": "renamed", "display_name": None}]


def test_a_wrong_state_is_rejected(client: TestClient, github: FakeGitHub) -> None:
    begin_login(client)

    response = client.get("/auth/github/callback", params={"code": "abc", "state": "forged"})

    assert response.status_code == 400
    assert "nikolai_session" not in response.cookies
    assert github.requests == []


def test_a_state_signed_with_another_secret_is_rejected(
    client: TestClient, settings: Settings
) -> None:
    state = begin_login(client)
    settings.session_secret = SecretStr("rotated-secret")

    response = client.get("/auth/github/callback", params={"code": "abc", "state": state})

    assert response.status_code == 400


def test_an_expired_state_is_rejected(client: TestClient, monkeypatch: pytest.MonkeyPatch) -> None:
    state = begin_login(client)
    monkeypatch.setattr("app.services.sessions.time", lambda: 10.0**11)

    response = client.get("/auth/github/callback", params={"code": "abc", "state": state})

    assert response.status_code == 400


def test_a_callback_without_the_state_cookie_is_rejected(client: TestClient) -> None:
    state = begin_login(client)
    client.cookies.clear()

    response = client.get("/auth/github/callback", params={"code": "abc", "state": state})

    assert response.status_code == 400


def test_declining_on_github_gets_a_clean_error(client: TestClient, github: FakeGitHub) -> None:
    state = begin_login(client)

    response = client.get(
        "/auth/github/callback", params={"error": "access_denied", "state": state}
    )

    assert response.status_code == 400
    assert github.requests == []


@pytest.mark.parametrize(
    ("token_reply", "user_reply"),
    [
        (httpx.Response(200, json={"error": "bad_verification_code"}), None),
        (httpx.Response(500, text="down"), None),
        (httpx.Response(200, json=["not", "an", "object"]), None),
        (None, httpx.Response(401, json={"message": "Bad credentials"})),
        (None, httpx.Response(200, text="not json")),
        (None, httpx.Response(200, json={"id": "not-a-number"})),
    ],
)
def test_a_github_failure_gets_a_clean_error_and_no_session(
    client: TestClient,
    github: FakeGitHub,
    token_reply: httpx.Response | None,
    user_reply: httpx.Response | None,
) -> None:
    github.token_reply = token_reply or github.token_reply
    github.user_reply = user_reply or github.user_reply
    state = begin_login(client)

    response = client.get("/auth/github/callback", params={"code": "abc", "state": state})

    assert response.status_code == 502
    assert response.json() == {"detail": "GitHub sign-in failed. Try again."}
    assert "nikolai_session" not in response.cookies


def test_me_needs_a_session(client: TestClient) -> None:
    assert client.get("/api/me").status_code == 401


def test_me_rejects_an_unknown_session(client: TestClient) -> None:
    client.cookies.set("nikolai_session", "made-up")

    assert client.get("/api/me").status_code == 401


async def test_an_expired_session_gets_401(client: TestClient, database_url: str) -> None:
    await asyncio.to_thread(sign_in, client)
    await query(
        database_url,
        "UPDATE user_sessions SET created_at = now() - interval '15 days', "
        "expires_at = now() - interval '1 day'",
    )

    response = await asyncio.to_thread(client.get, "/api/me")

    assert response.status_code == 401


def test_logout_revokes_the_session(client: TestClient) -> None:
    token = sign_in(client)

    response = client.post("/auth/logout", headers={"Origin": ORIGIN})

    assert response.status_code == 204
    client.cookies.set("nikolai_session", token)  # replaying the old cookie must not work
    assert client.get("/api/me").status_code == 401


def test_logout_clears_the_cookie(client: TestClient) -> None:
    sign_in(client)

    client.post("/auth/logout", headers={"Origin": ORIGIN})

    assert "nikolai_session" not in client.cookies


def test_logout_without_a_session_succeeds(client: TestClient) -> None:
    assert client.post("/auth/logout", headers={"Origin": ORIGIN}).status_code == 204


@pytest.mark.parametrize("headers", [{}, {"Origin": "https://evil.test"}])
def test_logout_refuses_an_origin_outside_the_allowed_list(
    client: TestClient, headers: dict[str, str]
) -> None:
    sign_in(client)

    assert client.post("/auth/logout", headers=headers).status_code == 403
    assert client.get("/api/me").status_code == 200


def test_login_is_unavailable_until_oauth_is_configured(
    client: TestClient, settings: Settings
) -> None:
    settings.github_client_secret = None

    response = client.get("/auth/github/login")

    assert response.status_code == 503
    assert response.json() == {"detail": "GitHub sign-in is not configured"}


def test_me_reports_a_database_outage_cleanly(
    client: TestClient, monkeypatch: pytest.MonkeyPatch
) -> None:
    async def fail(url: str) -> None:
        raise OperationalError("connection failed: secret-password")

    monkeypatch.setattr(dependencies, "connect", fail)
    client.cookies.set("nikolai_session", "token")

    response = client.get("/api/me")

    assert response.status_code == 503
    assert "secret-password" not in response.text
