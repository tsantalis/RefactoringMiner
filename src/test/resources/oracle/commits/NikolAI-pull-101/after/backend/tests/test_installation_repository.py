"""Installation webhooks register repositories automatically (US-10.1)."""

# AI contribution: 50%

from uuid import uuid4

import httpx
import pytest
from pydantic import SecretStr

from app.core.config import Settings, get_settings
from app.db import connect
from app.db.repositories.installations import (
    InstallationRepository,
    archive_installation_repositories,
    register_installation_repositories,
)
from app.main import app
from tests.fakes import (
    INSTALLATION_ID,
    WEBHOOK_SECRET,
    installation_payload,
    installation_repositories_payload,
    repository_summary,
    signed,
)


def _settings(database_url: str) -> Settings:
    return Settings(_env_file=None).model_copy(
        update={
            "database_url": SecretStr(database_url),
            "github_webhook_secret": SecretStr(WEBHOOK_SECRET),
        }
    )


async def _post(payload: dict[str, object], event_name: str) -> httpx.Response:
    body, signature = signed(payload)
    headers = {
        "X-GitHub-Event": event_name,
        "X-GitHub-Delivery": str(uuid4()),
        "X-Hub-Signature-256": signature,
    }
    async with httpx.AsyncClient(
        transport=httpx.ASGITransport(app=app), base_url="http://test"
    ) as client:
        return await client.post("/webhook", content=body, headers=headers)


async def test_installation_registers_repositories_without_a_team(database_url: str) -> None:
    async with await connect(database_url) as connection:
        await connection.execute("DELETE FROM repositories")
        ids = await register_installation_repositories(
            connection,
            installation_id=INSTALLATION_ID,
            repositories=[InstallationRepository(500, "octocat/new")],
        )
        row = await (
            await connection.execute(
                """SELECT team_id, github_installation_id, full_name, archived_at
                   FROM repositories WHERE id = %s""",
                (ids[0],),
            )
        ).fetchone()
        assert row == {
            "team_id": None,
            "github_installation_id": INSTALLATION_ID,
            "full_name": "octocat/new",
            "archived_at": None,
        }


async def test_repeated_registration_keeps_the_team_assignment(database_url: str) -> None:
    async with await connect(database_url) as connection:
        original = await (
            await connection.execute(
                "SELECT team_id FROM repositories WHERE github_repository_id = 100"
            )
        ).fetchone()
        for _ in range(2):
            await register_installation_repositories(
                connection,
                installation_id=INSTALLATION_ID,
                repositories=[InstallationRepository(100, "octocat/demo")],
            )
        row = await (
            await connection.execute(
                """SELECT team_id, github_installation_id FROM repositories
                   WHERE github_repository_id = 100"""
            )
        ).fetchone()
        assert original is not None
        assert row == {"team_id": original["team_id"], "github_installation_id": INSTALLATION_ID}


async def test_removed_repository_is_archived(database_url: str) -> None:
    async with await connect(database_url) as connection:
        await register_installation_repositories(
            connection,
            installation_id=INSTALLATION_ID,
            repositories=[InstallationRepository(100, "octocat/demo")],
        )
        archived = await archive_installation_repositories(
            connection, installation_id=INSTALLATION_ID, github_repository_ids=[100]
        )
        assert archived == 1
        row = await (
            await connection.execute(
                "SELECT archived_at FROM repositories WHERE github_repository_id = 100"
            )
        ).fetchone()
        assert row is not None
        assert row["archived_at"] is not None


async def test_uninstall_archives_only_its_repositories(database_url: str) -> None:
    async with await connect(database_url) as connection:
        await register_installation_repositories(
            connection,
            installation_id=INSTALLATION_ID,
            repositories=[InstallationRepository(100, "octocat/demo")],
        )
        await register_installation_repositories(
            connection,
            installation_id=99,
            repositories=[InstallationRepository(600, "other/repo")],
        )
        archived = await archive_installation_repositories(
            connection, installation_id=INSTALLATION_ID
        )
        rows = await (
            await connection.execute(
                "SELECT full_name, archived_at FROM repositories ORDER BY full_name"
            )
        ).fetchall()
        assert archived == 1
        assert [(row["full_name"], row["archived_at"] is not None) for row in rows] == [
            ("octocat/demo", True),
            ("other/repo", False),
        ]


async def test_installation_webhook_registers_repository(database_url: str) -> None:
    app.dependency_overrides[get_settings] = lambda: _settings(database_url)
    try:
        async with await connect(database_url) as connection:
            await connection.execute("DELETE FROM repositories")
        response = await _post(installation_payload(), "installation")
        assert response.status_code == 202
        assert response.json() == {"ok": True}
        async with await connect(database_url) as connection:
            row = await (
                await connection.execute(
                    """SELECT team_id, github_installation_id, full_name FROM repositories
                       WHERE github_repository_id = 100"""
                )
            ).fetchone()
            assert row == {
                "team_id": None,
                "github_installation_id": INSTALLATION_ID,
                "full_name": "octocat/demo",
            }
    finally:
        app.dependency_overrides.clear()


async def test_installation_webhook_archives_removed_repository(database_url: str) -> None:
    app.dependency_overrides[get_settings] = lambda: _settings(database_url)
    try:
        assert (await _post(installation_payload(), "installation")).status_code == 202
        response = await _post(
            installation_repositories_payload("removed"), "installation_repositories"
        )
        assert response.status_code == 202
        async with await connect(database_url) as connection:
            row = await (
                await connection.execute(
                    "SELECT archived_at FROM repositories WHERE github_repository_id = 100"
                )
            ).fetchone()
            assert row is not None
            assert row["archived_at"] is not None
    finally:
        app.dependency_overrides.clear()


async def test_uninstall_webhook_archives_every_repository(database_url: str) -> None:
    app.dependency_overrides[get_settings] = lambda: _settings(database_url)
    try:
        assert (await _post(installation_payload(), "installation")).status_code == 202
        response = await _post(installation_payload("deleted", repositories=[]), "installation")
        assert response.status_code == 202
        async with await connect(database_url) as connection:
            row = await (
                await connection.execute(
                    "SELECT archived_at FROM repositories WHERE github_repository_id = 100"
                )
            ).fetchone()
            assert row is not None
            assert row["archived_at"] is not None
    finally:
        app.dependency_overrides.clear()


async def test_installation_webhook_requires_a_database() -> None:
    settings = Settings(_env_file=None).model_copy(
        update={"github_webhook_secret": SecretStr(WEBHOOK_SECRET)}
    )
    app.dependency_overrides[get_settings] = lambda: settings
    try:
        response = await _post(installation_payload(), "installation")
        assert response.status_code == 503
        assert response.json() == {"detail": "Database not configured"}
    finally:
        app.dependency_overrides.clear()


async def test_installation_webhook_rejects_an_invalid_repository(database_url: str) -> None:
    app.dependency_overrides[get_settings] = lambda: _settings(database_url)
    try:
        response = await _post(
            installation_payload(repositories=[repository_summary(0, "octocat/demo")]),
            "installation",
        )
        assert response.status_code == 400
    finally:
        app.dependency_overrides.clear()


async def test_installation_webhook_rejects_a_malformed_payload(database_url: str) -> None:
    app.dependency_overrides[get_settings] = lambda: _settings(database_url)
    try:
        response = await _post({"action": "created", "installation": {"id": 1}}, "installation")
        assert response.status_code == 400
    finally:
        app.dependency_overrides.clear()


@pytest.mark.parametrize(
    ("action", "event_name"),
    [("suspend", "installation"), ("unknown", "installation_repositories")],
)
async def test_unhandled_actions_are_acknowledged(
    database_url: str, action: str, event_name: str
) -> None:
    app.dependency_overrides[get_settings] = lambda: _settings(database_url)
    try:
        payload = (
            installation_payload(action)
            if event_name == "installation"
            else installation_repositories_payload(action)
        )
        response = await _post(payload, event_name)
        assert response.status_code == 202
        assert response.json() == {"ok": True}
    finally:
        app.dependency_overrides.clear()
