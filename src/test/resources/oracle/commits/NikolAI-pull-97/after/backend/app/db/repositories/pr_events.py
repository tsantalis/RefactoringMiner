"""Persist validated PR deliveries; webhook routing and review policy live elsewhere."""

# AI contribution: 50% or more AI-generated

from dataclasses import dataclass
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow
from psycopg.types.json import Jsonb

from app.db.repositories.registrations import active_repository_id
from app.github.payloads import PullRequestEvent


class RepositoryNotRegisteredError(ValueError):
    """The payload does not belong to an active repository and team in our database."""


@dataclass(frozen=True)
class SavedPREvent:
    event_id: UUID
    created: bool


async def save_pr_event(
    connection: AsyncConnection[DictRow],
    delivery_id: UUID,
    payload: dict[str, object],
    *,
    ignored: bool = False,
) -> SavedPREvent:
    """Save an event and its user/PR references atomically, once per delivery UUID.

    Call after verifying the webhook signature. The caller owns the connection.
    Preserve the original JSON, including fields outside our typed payload model.
    This function neither starts an agent nor marks an event processed.
    The caller supplies review policy: ignored events are finalized in the same insert.
    """
    event = PullRequestEvent.model_validate(payload)
    async with connection.transaction():
        # Serialize retries before touching snapshots. The unique constraint remains
        # the database's final protection against duplicate delivery IDs.
        await connection.execute(
            "SELECT pg_advisory_xact_lock(hashtextextended(%s, 0))",
            (f"pr-event:{delivery_id}",),
        )
        cursor = await connection.execute(
            "SELECT id FROM webhook_events WHERE delivery_id = %s", (delivery_id,)
        )
        existing = await cursor.fetchone()
        if existing is not None:
            return SavedPREvent(event_id=existing["id"], created=False)

        repository_id = await active_repository_id(connection, event.repository.id, lock=True)
        if repository_id is None:
            raise RepositoryNotRegisteredError(
                f"Repository {event.repository.id} must be registered to an active team"
            )

        users: dict[int, UUID] = {}
        # Consistent lock order also handles author == sender and shared users
        # appearing in concurrent deliveries for different repositories.
        github_users = {u.id: u for u in (event.pull_request.user, event.sender)}
        for github_id, user in sorted(github_users.items()):
            cursor = await connection.execute(
                """INSERT INTO users (github_user_id, github_login) VALUES (%s, %s)
                   ON CONFLICT (github_user_id) DO UPDATE
                   SET github_login = EXCLUDED.github_login, updated_at = now()
                   RETURNING id""",
                (github_id, user.login),
            )
            row = await cursor.fetchone()
            if row is None:
                raise RuntimeError("User insert returned no ID")
            users[github_id] = row["id"]

        pr = event.pull_request
        await connection.execute(
            """INSERT INTO pull_requests
               (repository_id, github_pr_id, pr_number, author_user_id, title, state,
                merged_at, github_created_at, github_updated_at)
               VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
               ON CONFLICT (github_pr_id) DO UPDATE
               SET title = EXCLUDED.title, state = EXCLUDED.state,
                   merged_at = EXCLUDED.merged_at,
                   author_user_id = EXCLUDED.author_user_id,
                   github_updated_at = EXCLUDED.github_updated_at, last_synced_at = now()
               WHERE pull_requests.repository_id = EXCLUDED.repository_id
                 AND pull_requests.pr_number = EXCLUDED.pr_number
                 AND pull_requests.github_updated_at <= EXCLUDED.github_updated_at""",
            (
                repository_id,
                pr.id,
                pr.number,
                users[pr.user.id],
                pr.title,
                pr.state,
                pr.merged_at,
                pr.created_at,
                pr.updated_at,
            ),
        )
        cursor = await connection.execute(
            """SELECT id FROM pull_requests
               WHERE github_pr_id = %s AND repository_id = %s AND pr_number = %s""",
            (pr.id, repository_id, pr.number),
        )
        saved_pr = await cursor.fetchone()
        if saved_pr is None:
            raise ValueError("GitHub PR ID belongs to a different repository or PR number")

        cursor = await connection.execute(
            """INSERT INTO webhook_events
               (delivery_id, repository_id, pull_request_id, actor_user_id,
                event_type, event_action, payload, status, processed_at)
               VALUES (%s, %s, %s, %s, 'pull_request', %s, %s, %s,
                       CASE WHEN %s THEN now() ELSE NULL END) RETURNING id""",
            (
                delivery_id,
                repository_id,
                saved_pr["id"],
                users[event.sender.id],
                event.action,
                Jsonb(payload),
                "ignored" if ignored else "received",
                ignored,
            ),
        )
        saved = await cursor.fetchone()
        if saved is None:
            raise RuntimeError("Event insert returned no ID")
        return SavedPREvent(event_id=saved["id"], created=True)
