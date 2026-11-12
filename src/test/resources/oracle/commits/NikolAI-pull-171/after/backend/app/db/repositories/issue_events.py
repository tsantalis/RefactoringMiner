"""Persist validated ``issues`` deliveries; story policy and the check itself live elsewhere."""

# AI contribution: 50%

from dataclasses import dataclass
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow
from psycopg.types.json import Jsonb

from app.db.repositories.pr_events import RepositoryNotRegisteredError
from app.db.repositories.registrations import active_repository_id
from app.db.repositories.users import upsert_github_user
from app.github.payloads import IssuesEvent


@dataclass(frozen=True)
class SavedIssueEvent:
    event_id: UUID
    created: bool


async def save_issues_event(
    connection: AsyncConnection[DictRow],
    delivery_id: UUID,
    payload: dict[str, object],
    *,
    ignored: bool = False,
) -> SavedIssueEvent:
    """Save an issues delivery and its user/issue references atomically, once per delivery UUID.

    Call after verifying the webhook signature. The caller owns the connection.
    Preserve the original JSON, including fields outside our typed payload model.
    This function neither starts a check nor marks an event processed.
    The caller supplies the story policy: ignored deliveries are finalized in the
    same insert, still with the issue they carry.
    """
    event = IssuesEvent.model_validate(payload)
    async with connection.transaction():
        # Serialize retries before touching snapshots. The unique constraint remains
        # the database's final protection against duplicate delivery IDs.
        await connection.execute(
            "SELECT pg_advisory_xact_lock(hashtextextended(%s, 0))",
            (f"issue-event:{delivery_id}",),
        )
        cursor = await connection.execute(
            "SELECT id FROM webhook_events WHERE delivery_id = %s", (delivery_id,)
        )
        existing = await cursor.fetchone()
        if existing is not None:
            return SavedIssueEvent(event_id=existing["id"], created=False)

        repository_id = await active_repository_id(connection, event.repository.id, lock=True)
        if repository_id is None:
            raise RepositoryNotRegisteredError(
                f"Repository {event.repository.id} must be registered to an active team"
            )

        issue_id = await _upsert_issue(connection, event, repository_id)
        sender_id = await upsert_github_user(connection, event.sender.id, event.sender.login)

        cursor = await connection.execute(
            """INSERT INTO webhook_events
               (delivery_id, repository_id, issue_id, actor_user_id,
                event_type, event_action, payload, status, processed_at)
               VALUES (%s, %s, %s, %s, 'issues', %s, %s, %s,
                       CASE WHEN %s THEN now() ELSE NULL END) RETURNING id""",
            (
                delivery_id,
                repository_id,
                issue_id,
                sender_id,
                event.action,
                Jsonb(payload),
                "ignored" if ignored else "received",
                ignored,
            ),
        )
        saved = await cursor.fetchone()
        if saved is None:
            raise RuntimeError("Event insert returned no ID")
        return SavedIssueEvent(event_id=saved["id"], created=True)


async def _upsert_issue(
    connection: AsyncConnection[DictRow], event: IssuesEvent, repository_id: UUID
) -> UUID:
    """The issue's row, created or synced from the event; returns its id."""
    issue = event.issue
    author_id = await upsert_github_user(connection, issue.user.id, issue.user.login)
    await connection.execute(
        """INSERT INTO issues
           (repository_id, github_issue_id, issue_number, author_user_id, title, state,
            github_created_at, github_updated_at)
           VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
           ON CONFLICT (github_issue_id) DO UPDATE
           SET title = EXCLUDED.title, state = EXCLUDED.state,
               author_user_id = EXCLUDED.author_user_id,
               github_updated_at = EXCLUDED.github_updated_at, last_synced_at = now()
           WHERE issues.repository_id = EXCLUDED.repository_id
             AND issues.issue_number = EXCLUDED.issue_number
             AND issues.github_updated_at <= EXCLUDED.github_updated_at""",
        (
            repository_id,
            issue.id,
            issue.number,
            author_id,
            issue.title,
            issue.state,
            issue.created_at,
            issue.updated_at,
        ),
    )
    cursor = await connection.execute(
        """SELECT id FROM issues
           WHERE github_issue_id = %s AND repository_id = %s""",
        (issue.id, repository_id),
    )
    saved = await cursor.fetchone()
    if saved is None:
        raise ValueError("GitHub issue ID belongs to a different repository or issue number")
    return UUID(str(saved["id"]))
