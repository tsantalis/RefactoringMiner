"""Helpers shared by the courses and feedback API tests."""

# AI contribution: 50%

import asyncio
from urllib.parse import parse_qs, urlsplit
from uuid import UUID

import httpx
from fastapi.testclient import TestClient
from pydantic import SecretStr

from app.core.config import Settings
from app.db import connect
from app.db.models.courses import CourseDetail
from app.main import app

ORIGIN = "https://dashboard.test"
INSTRUCTOR_ID = 100
STUDENT_ONE_ID = 101
STUDENT_TWO_ID = 102
TA_ID = 103
WRITE_HEADERS = {"Origin": ORIGIN}


class FakeGitHub:
    """GitHub's user endpoint, scripted per sign-in."""

    def __init__(self) -> None:
        self.user_reply = httpx.Response(200, json={"id": 1, "login": "someone", "name": None})

    def __call__(self, request: httpx.Request) -> httpx.Response:
        if request.url.path == "/login/oauth/access_token":
            return httpx.Response(200, json={"access_token": "gho_test"})
        return self.user_reply


def settings_for(database_url: str) -> Settings:
    """Settings that let the OAuth flow and the Origin check run in tests."""
    return Settings(
        _env_file=None,
        database_url=SecretStr(database_url),
        github_client_id="client-id",
        github_client_secret=SecretStr("client-secret"),
        session_secret=SecretStr("session-secret"),
        allowed_origins=[ORIGIN],
    )


def new_client() -> TestClient:
    """Another browser, with its own cookie jar, for a different user."""
    return TestClient(app, base_url="https://testserver", follow_redirects=False)


def sign_in(client: TestClient, github: FakeGitHub, user_id: int, login: str) -> None:
    """Complete the OAuth flow as ``login``."""
    github.user_reply = httpx.Response(200, json={"id": user_id, "login": login, "name": login})
    response = client.get("/auth/github/login")
    state = parse_qs(urlsplit(response.headers["location"]).query)["state"][0]
    callback = client.get("/auth/github/callback", params={"code": "abc", "state": state})
    assert callback.status_code == 303


async def request(
    client: TestClient,
    method: str,
    path: str,
    *,
    json: object = None,
    headers: dict[str, str] | None = None,
) -> httpx.Response:
    return await asyncio.to_thread(client.request, method, path, json=json, headers=headers)


async def execute(
    database_url: str, statement: str, params: tuple[object, ...] = ()
) -> list[dict[str, object]]:
    async with await connect(database_url) as connection:
        cursor = await connection.execute(statement, params)
        return await cursor.fetchall() if cursor.description else []


async def make_instructor(database_url: str, login: str) -> None:
    await execute(
        database_url,
        "UPDATE users SET is_instructor = true WHERE lower(github_login) = lower(%s)",
        (login,),
    )


async def set_course_role(database_url: str, *, course_id: str, login: str, role: str) -> None:
    """Change a course role directly, as a promotion outside this API's scope would."""
    await execute(
        database_url,
        """UPDATE course_memberships SET role = %s
           WHERE course_id = %s AND user_id = (SELECT id FROM users WHERE github_login = %s)""",
        (role, course_id, login),
    )


async def create_course(client: TestClient, name: str = "SOEN 490") -> httpx.Response:
    return await request(client, "POST", "/api/courses", json={"name": name}, headers=WRITE_HEADERS)


async def instructor_course(
    client: TestClient, github: FakeGitHub, database_url: str
) -> tuple[str, str]:
    """Sign in the instructor, grant the flag, create a course; return its id and join code."""
    await asyncio.to_thread(sign_in, client, github, INSTRUCTOR_ID, "prof")
    await make_instructor(database_url, "prof")
    response = await create_course(client)
    assert response.status_code == 201
    body = CourseDetail.model_validate(response.json())
    assert body.join_code is not None
    return str(body.id), body.join_code


async def join_course(client: TestClient, join_code: str) -> httpx.Response:
    return await request(
        client, "POST", "/api/courses/join", json={"join_code": join_code}, headers=WRITE_HEADERS
    )


async def create_team(client: TestClient, course_id: str, name: str) -> httpx.Response:
    return await request(
        client,
        "POST",
        f"/api/courses/{course_id}/teams",
        json={"name": name},
        headers=WRITE_HEADERS,
    )


async def add_team_member(
    client: TestClient, *, course_id: str, team_id: str, login: str, role: str = "member"
) -> httpx.Response:
    return await request(
        client,
        "POST",
        f"/api/courses/{course_id}/teams/{team_id}/members",
        json={"github_login": login, "role": role},
        headers=WRITE_HEADERS,
    )


async def import_roster(
    client: TestClient, course_id: str, entries: list[dict[str, str]]
) -> httpx.Response:
    return await request(
        client,
        "POST",
        f"/api/courses/{course_id}/roster",
        json={"entries": entries},
        headers=WRITE_HEADERS,
    )


async def course_view(client: TestClient, course_id: str) -> CourseDetail:
    """``GET /api/courses/{id}`` validated against the response model the API documents."""
    response = await request(client, "GET", f"/api/courses/{course_id}")
    assert response.status_code == 200
    return CourseDetail.model_validate(response.json())


async def team_named(database_url: str, name: str) -> UUID:
    """The UUID of the single team with this name, for seeding feedback."""
    rows = await execute(database_url, "SELECT id FROM teams WHERE name = %s", (name,))
    assert len(rows) == 1
    return UUID(str(rows[0]["id"]))
