"""Sign-in, sessions and logout (US-11.1, ADR 0001) through the HTTP API.

Requests run against real PostgreSQL; only GitHub is faked. The client uses https:// because
the cookies are ``Secure``.
"""

# AI contribution: 50%

import asyncio
import hashlib
import re
from collections.abc import Iterator
from html import escape, unescape
from urllib.parse import parse_qs, quote, urlsplit

import httpx
import pytest
from fastapi.testclient import TestClient
from psycopg import OperationalError
from pydantic import SecretStr

from app.api import dependencies
from app.api.routes.auth import (
    NOT_LINKED,
    RETURN_TO_COOKIE,
    RETURN_TO_NOT_ALLOWED,
    get_github_transport,
)
from app.api.sign_in_page import COURSE_NOT_LINKED, SIGN_IN_PATH
from app.core.config import Settings, get_settings
from app.db import connect
from app.main import app

ORIGIN = "https://dashboard.test"
EXTENSION_ORIGIN = "chrome-extension://gppecagpegmfkmmafaohboobkfkkbhho"
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


def begin_login(client: TestClient, return_to: str | None = None) -> str:
    """Start sign-in and return the ``state`` sent to GitHub."""
    params = {} if return_to is None else {"return_to": return_to}
    response = client.get("/auth/github/login", params=params)
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
    assert response.headers["location"] == "/auth/signed-in"
    me = client.get("/api/me")
    assert me.status_code == 200
    assert me.json()["github_login"] == "octocat"
    assert me.json()["display_name"] == "Mona"
    assert me.json()["is_instructor"] is False


def test_sign_in_returns_to_an_allowed_dashboard_origin(client: TestClient) -> None:
    login = client.get("/auth/github/login", params={"return_to": ORIGIN})
    state = parse_qs(urlsplit(login.headers["location"]).query)["state"][0]

    response = client.get("/auth/github/callback", params={"code": "abc", "state": state})

    assert response.status_code == 303
    assert response.headers["location"] == ORIGIN
    assert "nikolai_session" in response.cookies
    assert RETURN_TO_COOKIE not in client.cookies


def test_sign_in_accepts_an_allowed_origin_with_a_trailing_slash(client: TestClient) -> None:
    login = client.get("/auth/github/login", params={"return_to": f"{ORIGIN}/"})
    state = parse_qs(urlsplit(login.headers["location"]).query)["state"][0]

    response = client.get("/auth/github/callback", params={"code": "abc", "state": state})

    assert response.status_code == 303
    assert response.headers["location"] == ORIGIN


@pytest.mark.parametrize(
    "return_to",
    [
        "https://evil.test",
        f"{ORIGIN}.evil.test",
        f"{ORIGIN}@evil.test",
        f"{ORIGIN}/path",
        "//evil.test",
        "*",
        "javascript:alert(1)",
        "",
    ],
)
def test_sign_in_refuses_a_return_to_outside_the_allowed_origins(
    client: TestClient, github: FakeGitHub, return_to: str
) -> None:
    response = client.get("/auth/github/login", params={"return_to": return_to})

    assert response.status_code == 400
    assert response.headers["content-type"].startswith("text/html")
    assert escape(RETURN_TO_NOT_ALLOWED) in response.text
    assert "location" not in response.headers
    assert github.requests == []


def test_sign_in_refuses_a_browser_extension_origin(
    client: TestClient, settings: Settings, github: FakeGitHub
) -> None:
    settings.allowed_origins.append(EXTENSION_ORIGIN)

    response = client.get("/auth/github/login", params={"return_to": EXTENSION_ORIGIN})

    assert response.status_code == 400
    assert escape(RETURN_TO_NOT_ALLOWED) in response.text
    assert github.requests == []


def _clears_return_to(response: httpx.Response) -> bool:
    """The response expires the return-to cookie."""
    return any(
        cookie.startswith(f"{RETURN_TO_COOKIE}=") and "Max-Age=0" in cookie
        for cookie in response.headers.get_list("set-cookie")
    )


def _retry_link(response: httpx.Response) -> str:
    """Where the problem page's "Try again" link sends the browser."""
    match = re.search(r'<a class="button" href="([^"]+)">Try again</a>', response.text)
    assert match is not None
    return str(unescape(match.group(1)))


def _dashboard_retry_link() -> str:
    """The sign-in path that comes back to the dashboard that asked."""
    return f"{SIGN_IN_PATH}?return_to={quote(ORIGIN, safe='')}"


def test_sign_in_without_a_return_to_forgets_a_stale_one(client: TestClient) -> None:
    response = client.get("/auth/github/login")

    assert response.is_redirect
    assert _clears_return_to(response)


def test_the_callback_ignores_a_forged_return_to_cookie(client: TestClient) -> None:
    state = begin_login(client)
    client.cookies.set(RETURN_TO_COOKIE, "https://evil.test")

    response = client.get("/auth/github/callback", params={"code": "abc", "state": state})

    assert response.status_code == 303
    assert response.headers["location"] == "/auth/signed-in"


def test_a_failed_callback_forgets_a_stale_return_to(client: TestClient) -> None:
    response = client.get("/auth/github/callback", params={"code": "abc", "state": "wrong"})

    assert response.status_code == 400
    assert _clears_return_to(response)
    assert _retry_link(response) == SIGN_IN_PATH


def test_a_cancelled_sign_in_offers_to_try_again_on_the_dashboard(client: TestClient) -> None:
    state = begin_login(client, ORIGIN)

    response = client.get(
        "/auth/github/callback", params={"error": "access_denied", "state": state}
    )

    assert response.status_code == 400
    assert _clears_return_to(response)
    assert _retry_link(response) == _dashboard_retry_link()


def test_an_expired_sign_in_offers_to_try_again_on_the_dashboard(
    client: TestClient, monkeypatch: pytest.MonkeyPatch
) -> None:
    state = begin_login(client, ORIGIN)
    monkeypatch.setattr("app.services.sessions.time", lambda: 10.0**11)

    response = client.get("/auth/github/callback", params={"code": "abc", "state": state})

    assert response.status_code == 400
    assert _clears_return_to(response)
    assert _retry_link(response) == _dashboard_retry_link()


def test_a_failed_sign_in_ignores_a_return_to_outside_the_allowed_origins(
    client: TestClient,
) -> None:
    state = begin_login(client)
    client.cookies.set(RETURN_TO_COOKIE, "https://evil.test")

    response = client.get("/auth/github/callback", params={"code": "abc", "state": state + "x"})

    assert response.status_code == 400
    assert _clears_return_to(response)
    assert _retry_link(response) == SIGN_IN_PATH


def test_the_signed_in_page_names_the_user_and_survives_a_reload(client: TestClient) -> None:
    sign_in(client)

    first = client.get("/auth/signed-in")
    reloaded = client.get("/auth/signed-in")

    for page in (first, reloaded):
        assert page.status_code == 200
        assert page.headers["content-type"].startswith("text/html")
        assert "You're signed in as octocat" in page.text
    assert escape(COURSE_NOT_LINKED) not in first.text


def test_the_signed_in_page_offers_sign_in_without_a_session(client: TestClient) -> None:
    response = client.get("/auth/signed-in")

    assert response.status_code == 401
    assert response.headers["content-type"].startswith("text/html")
    assert "not signed in." in response.text
    assert 'href="/auth/github/login"' in response.text


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
    assert response.headers["content-type"].startswith("text/html")
    assert "Sign-in expired. Please try again." in response.text
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
    assert response.headers["content-type"].startswith("text/html")
    assert "Sign-in was cancelled." in response.text
    assert 'href="/auth/github/login"' in response.text
    assert "nikolai_session" not in response.cookies
    assert github.requests == []


@pytest.mark.parametrize(
    ("error", "logged"),
    [
        ("redirect_uri_mismatch", "redirect_uri_mismatch"),
        ("application_suspended", "application_suspended"),
        ("<b>not a code</b>", "no error code"),
        (None, "no error code"),
    ],
)
def test_github_coming_back_without_a_code_is_a_failure_for_the_operator(
    client: TestClient, caplog: pytest.LogCaptureFixture, error: str | None, logged: str
) -> None:
    state = begin_login(client)
    params = {"state": state} | ({"error": error} if error else {})

    response = client.get("/auth/github/callback", params=params)

    assert response.status_code == 502
    assert "GitHub sign-in failed. Try again." in response.text
    assert "cancelled" not in response.text
    assert f"GitHub sent the sign-in back without a code: {logged}" in caplog.text
    assert "<b>" not in caplog.text


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
    assert response.headers["content-type"].startswith("text/html")
    assert "GitHub sign-in failed. Try again." in response.text
    assert 'href="/auth/github/login"' in response.text
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
    assert response.headers["content-type"].startswith("text/html")
    assert "GitHub sign-in is not configured" in response.text


def test_a_callback_without_oauth_settings_shows_the_error_page(
    client: TestClient, settings: Settings
) -> None:
    settings.github_client_secret = None

    response = client.get("/auth/github/callback", params={"code": "abc", "state": "any"})

    assert response.status_code == 503
    assert response.headers["content-type"].startswith("text/html")
    assert "GitHub sign-in is not configured" in response.text


@pytest.fixture
def database_down(monkeypatch: pytest.MonkeyPatch) -> None:
    async def fail(url: str) -> None:
        raise OperationalError("connection failed: secret-password")

    monkeypatch.setattr(dependencies, "connect", fail)


@pytest.mark.usefixtures("database_down")
def test_me_reports_a_database_outage_cleanly(client: TestClient) -> None:
    client.cookies.set("nikolai_session", "token")

    response = client.get("/api/me")

    assert response.status_code == 503
    assert "secret-password" not in response.text


@pytest.mark.usefixtures("database_down")
def test_a_database_outage_at_sign_in_shows_the_error_page(client: TestClient) -> None:
    state = begin_login(client)

    response = client.get("/auth/github/callback", params={"code": "abc", "state": state})

    assert response.status_code == 503
    assert response.headers["content-type"].startswith("text/html")
    assert 'href="/auth/github/login"' in response.text
    assert "secret-password" not in response.text
    assert "nikolai_session" not in response.cookies


@pytest.mark.usefixtures("database_down")
def test_the_signed_in_page_reports_a_database_outage_as_a_page(client: TestClient) -> None:
    client.cookies.set("nikolai_session", "token")

    response = client.get("/auth/signed-in")

    assert response.status_code == 503
    assert response.headers["content-type"].startswith("text/html")
    assert "Database unavailable" in response.text
    assert "secret-password" not in response.text


def test_a_session_that_cannot_be_saved_shows_the_error_page(
    client: TestClient, monkeypatch: pytest.MonkeyPatch, caplog: pytest.LogCaptureFixture
) -> None:
    async def fail(*args: object) -> None:
        raise RuntimeError("User insert returned no ID")

    monkeypatch.setattr("app.api.routes.auth.start_session", fail)
    state = begin_login(client)

    response = client.get("/auth/github/callback", params={"code": "abc", "state": state})

    assert response.status_code == 500
    assert response.headers["content-type"].startswith("text/html")
    assert "Sign-in failed. Try again." in response.text
    assert 'href="/auth/github/login"' in response.text
    assert "nikolai_session" not in response.cookies
    assert "Saving the session failed for GitHub user octocat" in caplog.text
    assert "returned no ID" not in caplog.text


async def test_a_roster_login_is_claimed_on_first_sign_in(
    client: TestClient, database_url: str
) -> None:
    await query(
        database_url,
        """WITH prof AS (INSERT INTO users (github_user_id, github_login)
                         VALUES (7, 'prof') RETURNING id),
                course AS (INSERT INTO courses (name, join_code, created_by_user_id)
                           SELECT 'SOEN 490', 'ABCD2345', id FROM prof RETURNING id),
                team AS (INSERT INTO teams (name, course_id) SELECT 'Pixel', id FROM course
                         RETURNING id)
           INSERT INTO course_roster_entries
               (course_id, github_login, course_role, team_id, imported_by_user_id)
           SELECT course.id, 'OctoCat', 'student', team.id, prof.id FROM course, team, prof""",
    )

    await asyncio.to_thread(sign_in, client)
    me = (await asyncio.to_thread(client.get, "/api/me")).json()

    assert [(c["name"], c["role"]) for c in me["courses"]] == [("SOEN 490", "student")]
    assert [t["name"] for t in me["courses"][0]["teams"]] == ["Pixel"]
    claimed = await query(
        database_url, "SELECT claimed_at IS NOT NULL AS done FROM course_roster_entries"
    )
    assert claimed == [{"done": True}]


async def test_signing_in_again_claims_nothing_twice(client: TestClient, database_url: str) -> None:
    await asyncio.to_thread(sign_in, client)
    await asyncio.to_thread(sign_in, client)

    me = (await asyncio.to_thread(client.get, "/api/me")).json()

    assert me["courses"] == []


def test_a_failed_roster_claim_does_not_block_sign_in(
    client: TestClient, monkeypatch: pytest.MonkeyPatch, caplog: pytest.LogCaptureFixture
) -> None:
    async def fail(*args: object) -> int:
        raise RuntimeError("roster claim broke")

    monkeypatch.setattr("app.api.routes.auth.claim_roster_at_sign_in", fail)

    state = begin_login(client)

    response = client.get("/auth/github/callback", params={"code": "abc", "state": state})
    page = client.get(response.headers["location"])

    assert response.status_code == 303
    assert client.get("/api/me").status_code == 200
    assert "Roster claim failed for GitHub user octocat" in caplog.text
    assert "You're signed in as octocat" in page.text
    assert escape(COURSE_NOT_LINKED) in page.text


def test_a_failed_roster_claim_sends_the_dashboard_to_its_notice(
    client: TestClient, monkeypatch: pytest.MonkeyPatch
) -> None:
    async def fail(*args: object) -> int:
        raise RuntimeError("roster claim broke")

    monkeypatch.setattr("app.api.routes.auth.claim_roster_at_sign_in", fail)
    state = begin_login(client, ORIGIN)

    response = client.get("/auth/github/callback", params={"code": "abc", "state": state})

    assert response.status_code == 303
    assert response.headers["location"] == f"{ORIGIN}/?course={NOT_LINKED}"
    assert "nikolai_session" in response.cookies
