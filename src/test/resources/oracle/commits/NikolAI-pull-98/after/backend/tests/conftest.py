"""Pytest configuration shared by the whole suite."""

# AI contribution: 50%

import asyncio
import os
import socket
import socketserver
import sys
import threading
from collections.abc import AsyncIterator, Iterator
from typing import Final
from uuid import uuid4

import pytest
from psycopg import sql
from psycopg.conninfo import make_conninfo

from app.db import connect
from app.db.migrate import apply_migrations

# Settings that tests must never pick up from the developer's machine or deployment.
_BACKEND_ENV_PREFIXES: Final = ("OPENROUTER_", "GITHUB_", "REVIEW_")
_BACKEND_ENV_NAMES: Final = frozenset(
    {
        "APP_ID",
        "WEBHOOK_SECRET",
        "PR_LOG_DIR",
        "DATABASE_URL",
        "NIKOLAI_ENV",
        "SESSION_SECRET",
        "ALLOWED_ORIGINS",
    }
)

if sys.platform == "win32":

    @pytest.fixture(scope="session")
    def event_loop_policy() -> asyncio.AbstractEventLoopPolicy:
        return asyncio.WindowsSelectorEventLoopPolicy()


@pytest.fixture(autouse=True)
def _isolate_backend_environment(monkeypatch: pytest.MonkeyPatch) -> None:
    """Keep tests independent of OPENROUTER_*, GITHUB_* and GitHub App variables in the shell."""
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


@pytest.fixture
async def empty_database_url() -> AsyncIterator[str]:
    """A disposable, genuinely empty database."""
    admin_url = os.environ.get("TEST_DATABASE_URL")
    if not admin_url:
        pytest.skip("Set TEST_DATABASE_URL to a disposable PostgreSQL server with CREATEDB")
    name = f"nikolai_test_{uuid4().hex}"
    async with await connect(admin_url) as admin:
        await admin.execute(sql.SQL("CREATE DATABASE {}").format(sql.Identifier(name)))
        try:
            yield make_conninfo(admin_url, dbname=name)
        finally:
            await admin.execute(sql.SQL("DROP DATABASE {}").format(sql.Identifier(name)))


@pytest.fixture
async def database_url(empty_database_url: str) -> str:
    """Apply the real schema in a disposable database shared by persistence/query tests."""
    async with await connect(empty_database_url) as connection:
        await apply_migrations(connection)
        await connection.execute(
            """WITH team AS (INSERT INTO teams (name) VALUES ('Test team') RETURNING id)
                   INSERT INTO repositories (team_id, github_repository_id, full_name)
                   SELECT id, 100, 'octocat/demo' FROM team"""
        )
    return empty_database_url
