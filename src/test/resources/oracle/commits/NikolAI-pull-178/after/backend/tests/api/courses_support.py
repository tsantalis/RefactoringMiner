"""Shared setup for the course and roster API tests (US-10, ADR 0001 PR 2)."""

# AI contribution: 50%

import asyncio
from collections.abc import Callable
from typing import NamedTuple

from fastapi.testclient import TestClient

from app.db import connect
from app.github.oauth import GitHubUser
from app.services.sessions import start_session

ORIGIN = "https://dashboard.test"
# The ID the manifest's ``key`` pins for every unpacked install of the extension.
EXTENSION_ORIGIN = "chrome-extension://gppecagpegmfkmmafaohboobkfkkbhho"

SignIn = Callable[..., TestClient]


async def open_session(url: str, user: GitHubUser) -> str:
    async with await connect(url) as connection:
        return (await start_session(connection, user)).token


async def read_sql(
    url: str, statement: str, params: tuple[object, ...] = ()
) -> list[dict[str, object]]:
    """Run one read or write on the test database and return its rows. Awaitable."""
    async with await connect(url) as connection:
        cursor = await connection.execute(statement.encode(), params)
        return await cursor.fetchall() if cursor.description else []


def run_sql(url: str, statement: str, params: tuple[object, ...] = ()) -> list[dict[str, object]]:
    """``read_sql`` for sync tests: async tests must await ``read_sql`` instead."""
    return asyncio.run(read_sql(url, statement, params))


def new_course(instructor: TestClient, name: str = "SOEN 490") -> dict[str, str]:
    response = instructor.post("/api/courses", json={"name": name})
    assert response.status_code == 201
    body: dict[str, str] = response.json()
    return body


def new_team(instructor: TestClient, course_id: str, name: str) -> str:
    response = instructor.post(f"/api/courses/{course_id}/teams", json={"name": name})
    assert response.status_code == 201
    return str(response.json()["id"])


class CourseWithStudent(NamedTuple):
    instructor: TestClient
    course: dict[str, str]
    student: TestClient


def course_with_student(sign_in: SignIn) -> CourseWithStudent:
    """An instructor of a course, with one student (``alice``) already joined."""
    instructor = sign_in("prof", instructor=True)
    course = new_course(instructor)
    student = sign_in("alice")
    joined = student.post("/api/courses/join", json={"join_code": course["join_code"]})
    assert joined.status_code == 200
    return CourseWithStudent(instructor, course, student)


def make_ta(instructor: TestClient, course_id: str, login: str) -> None:
    roster = instructor.post(
        f"/api/courses/{course_id}/roster",
        json={"entries": [{"github_login": login, "role": "ta"}]},
    )
    assert roster.json()["applied"] == 1


def member_id(client: TestClient, course_id: str, login: str) -> str:
    response = client.get(f"/api/courses/{course_id}/members")
    return str(next(m["user_id"] for m in response.json() if m["github_login"] == login))


class TwoTeams(NamedTuple):
    instructor: TestClient
    course_id: str
    pixel: str
    alpha: str


def course_with_two_teams(sign_in: SignIn) -> TwoTeams:
    """Alice is in Pixel and Bob is in Alpha, both students of the same course."""
    instructor = sign_in("prof", instructor=True)
    course = new_course(instructor)
    pixel, alpha = (new_team(instructor, course["id"], name) for name in ("Pixel", "Alpha"))
    for login, team in (("alice", pixel), ("bob", alpha)):
        sign_in(login).post("/api/courses/join", json={"join_code": course["join_code"]})
        added = instructor.post(
            f"/api/courses/{course['id']}/teams/{team}/members", json={"github_login": login}
        )
        assert added.status_code == 200
    return TwoTeams(instructor, course["id"], pixel, alpha)
