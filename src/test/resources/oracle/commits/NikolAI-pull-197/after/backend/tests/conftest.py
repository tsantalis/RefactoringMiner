"""Pytest configuration shared by the whole suite."""

# AI contribution: 50%

import asyncio
import os
import socket
import socketserver
import sys
import threading
from collections.abc import AsyncIterator, Coroutine, Iterator
from typing import Final
from uuid import uuid4

import pytest
from psycopg import sql
from psycopg.conninfo import make_conninfo

from app.db import connect
from app.db.migrate import apply_migrations

# Settings that tests must never pick up from the developer's machine or deployment.
_BACKEND_ENV_PREFIXES: Final = ("OPENROUTER_", "GROQ_", "GITHUB_", "REVIEW_")
_BACKEND_ENV_NAMES: Final = frozenset(
    {
        "APP_ID",
        "WEBHOOK_SECRET",
        "PR_LOG_DIR",
        "DATABASE_URL",
        "NIKOLAI_ENV",
        "SESSION_SECRET",
        "ALLOWED_ORIGINS",
        "LLM_HOSTS",
    }
)

if sys.platform == "win32":

    @pytest.fixture(scope="session")
    def event_loop_policy() -> asyncio.AbstractEventLoopPolicy:
        return asyncio.WindowsSelectorEventLoopPolicy()


@pytest.fixture(autouse=True)
def _isolate_backend_environment(monkeypatch: pytest.MonkeyPatch) -> None:
    """Keep tests independent of OPENROUTER_*, GROQ_*, GITHUB_* and GitHub App variables in the
    shell."""
    for name in list(os.environ):
        upper = name.upper()
        if upper.startswith(_BACKEND_ENV_PREFIXES) or upper in _BACKEND_ENV_NAMES:
            monkeypatch.delenv(name)


class _SilentHandler(socketserver.BaseRequestHandler):
    def handle(self) -> None:
        """Accept and close without speaking PostgreSQL, like a wrong service on the port."""


@pytest.fixture
def listening_port() -> Iterator[int]:
    """A local TCP port that accepts connections but is not PostgreSQL (readiness: "down")."""
    server = socketserver.TCPServer(("127.0.0.1", 0), _SilentHandler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        yield server.server_address[1]
    finally:
        server.shutdown()
        server.server_close()
        thread.join(timeout=5)


@pytest.fixture
def closed_port() -> int:
    """A local port that was bound and released, so connections are refused (readiness: "down")."""
    sock = socket.socket()
    sock.bind(("127.0.0.1", 0))
    port: int = sock.getsockname()[1]
    sock.close()
    return port


def _admin_url() -> str:
    admin_url = os.environ.get("TEST_DATABASE_URL")
    if not admin_url:
        pytest.skip("Set TEST_DATABASE_URL to a disposable PostgreSQL server with CREATEDB")
    return admin_url


async def _create_database(admin_url: str, name: str, *, template: str | None = None) -> None:
    query = sql.SQL("CREATE DATABASE {}").format(sql.Identifier(name))
    if template is not None:
        query = sql.SQL("{} TEMPLATE {}").format(query, sql.Identifier(template))
    async with await connect(admin_url) as admin:
        await admin.execute(query)


async def _drop_database(admin_url: str, name: str) -> None:
    async with await connect(admin_url) as admin:
        await admin.execute(sql.SQL("DROP DATABASE {}").format(sql.Identifier(name)))


async def _build_template(admin_url: str, name: str) -> None:
    """Create ``name`` with the real schema and the shared test team and repository."""
    await _create_database(admin_url, name)
    async with await connect(make_conninfo(admin_url, dbname=name)) as connection:
        await apply_migrations(connection)
        await connection.execute(
            """WITH team AS (INSERT INTO teams (name) VALUES ('Test team') RETURNING id)
                   INSERT INTO repositories (team_id, github_repository_id, full_name)
                   SELECT id, 100, 'octocat/demo' FROM team"""
        )


def _run(coroutine: Coroutine[object, object, None]) -> None:
    # psycopg's async connections need a selector loop (Windows defaults to the proactor).
    asyncio.run(coroutine, loop_factory=asyncio.SelectorEventLoop)


@pytest.fixture(scope="session")
def _migrated_template() -> Iterator[str]:
    """A database migrated and seeded once per test process, copied by ``database_url``.

    Replaying every migration for each test was most of the suite's time. Each process (one
    per pytest-xdist worker) builds its own template, so parallel workers never share one.
    """
    admin_url = _admin_url()
    name = f"nikolai_template_{uuid4().hex}"
    _run(_build_template(admin_url, name))
    try:
        yield name
    finally:
        _run(_drop_database(admin_url, name))


@pytest.fixture
async def empty_database_url() -> AsyncIterator[str]:
    """A disposable, genuinely empty database."""
    admin_url = _admin_url()
    name = f"nikolai_test_{uuid4().hex}"
    await _create_database(admin_url, name)
    try:
        yield make_conninfo(admin_url, dbname=name)
    finally:
        await _drop_database(admin_url, name)


@pytest.fixture
async def database_url(_migrated_template: str) -> AsyncIterator[str]:
    """A disposable database with the real schema, copied from the migrated template.

    PostgreSQL copies the template's files, so each test still gets its own clean database
    without replaying the migrations.
    """
    admin_url = _admin_url()
    name = f"nikolai_test_{uuid4().hex}"
    await _create_database(admin_url, name, template=_migrated_template)
    try:
        yield make_conninfo(admin_url, dbname=name)
    finally:
        await _drop_database(admin_url, name)
