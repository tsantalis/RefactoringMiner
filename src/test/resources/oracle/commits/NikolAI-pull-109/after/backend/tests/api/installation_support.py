"""Signed installation deliveries for the US-10.1 tests."""

# AI contribution: 50%

from uuid import uuid4

from fastapi.testclient import TestClient

from tests.api.courses_support import run_sql
from tests.fakes import signed

APP: dict[str, object] = {"id": 77}
ALICE: dict[str, object] = {"id": 1001, "login": "alice"}  # A Pixel student installs the App.
PIXEL_APP: dict[str, object] = {"id": 501, "full_name": "Pixel-Org/App"}
PIXEL_DOCS: dict[str, object] = {"id": 502, "full_name": "Pixel-Org/Docs"}


def deliver(client: TestClient, event: str, payload: dict[str, object]) -> int:
    body, signature = signed(payload)
    response = client.post(
        "/webhook",
        content=body,
        headers={
            "X-GitHub-Event": event,
            "X-GitHub-Delivery": str(uuid4()),
            "X-Hub-Signature-256": signature,
            "Content-Type": "application/json",
        },
    )
    code: int = response.status_code
    return code


def install(
    client: TestClient,
    action: str,
    *repos: dict[str, object],
    sender: dict[str, object] = ALICE,
) -> int:
    payload: dict[str, object] = {
        "action": action,
        "installation": APP,
        "sender": sender,
        "repositories": list(repos),
    }
    return deliver(client, "installation", payload)


def change_repositories(
    client: TestClient,
    action: str,
    added: list[dict[str, object]] | None = None,
    removed: list[dict[str, object]] | None = None,
) -> int:
    payload: dict[str, object] = {
        "action": action,
        "installation": APP,
        "sender": ALICE,
        "repositories_added": added or [],
        "repositories_removed": removed or [],
    }
    return deliver(client, "installation_repositories", payload)


def registered(url: str) -> list[dict[str, object]]:
    return run_sql(
        url,
        """SELECT r.full_name, r.archived_at IS NULL AS active, r.installed_by_login,
                  t.name AS team, t.archived_at IS NULL AS team_active, t.course_id
           FROM repositories r JOIN teams t ON t.id = r.team_id
           WHERE r.github_repository_id >= 500 ORDER BY r.github_repository_id""",
    )
