"""The lifecycle of one user story check (US-12.1) in ``analysis_runs``/``analysis_outputs``.

The webhook route already persisted the delivery and its issue
(``app.db.repositories.issue_events``). A check claims that delivery and opens a
``story_review`` run before the model call, so the run is visible while it works;
finishing is serialized per issue (``story_ownership``), so the newest event's
verdict decides the comment and an older late finisher is cancelled as superseded
instead of overwriting it. Each check is one run with its output (history); the
newest completed run is the story review the extension reads (US-12.2).

The check's permission was already approved by the webhook route; an event for an
unknown repository never reaches here.
"""

# AI contribution: 50%

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from dataclasses import dataclass
from typing import Final
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow
from psycopg.types.json import Jsonb

from app.agents.story_review import StoryCheckOutcome, StoryReviewAgent
from app.db import connect

TOOL_NAME: str = StoryReviewAgent.name
TOOL_VERSION: str = StoryReviewAgent.prompt_version
OUTPUT_KEY: str = "story_review"
OUTPUT_TYPE: str = "story_review"
SCHEMA_VERSION: str = "1"
# A check that finishes after the story it read was edited is cancelled with this reason.
SUPERSEDED: Final = "Superseded"


@dataclass(frozen=True)
class StartedStoryCheck:
    """The run opened for an event, and the issue it belongs to (the lock's key)."""

    run_id: UUID
    issue_id: UUID


async def start_story_check(url: str, event_id: UUID) -> StartedStoryCheck | None:
    """Claim the delivery and open its check run; None when there is nothing to check.

    The flip from ``received`` to ``processing`` is part of the claim, so an event
    that was already claimed or handled never runs a second time. The run row is
    committed before the model call, so a user can see the check working.
    """
    async with await connect(url) as connection, connection.transaction():
        cursor = await connection.execute(
            """UPDATE webhook_events SET status = 'processing', processed_at = NULL
               WHERE id = %s AND status = 'received' RETURNING id""",
            (event_id,),
        )
        event = await cursor.fetchone()
        if event is None:
            return None
        cursor = await connection.execute(
            """INSERT INTO analysis_runs
               (repository_id, team_id_at_run, issue_id, trigger_event_id, analysis_type,
                tool_name, tool_version, status, started_at)
               SELECT e.repository_id, r.team_id, e.issue_id, e.id, 'story_review',
                      %s, %s, 'running', now()
               FROM webhook_events e JOIN repositories r ON r.id = e.repository_id
               WHERE e.id = %s RETURNING id, issue_id""",
            (TOOL_NAME, TOOL_VERSION, event_id),
        )
        run = await cursor.fetchone()
        if run is None:  # pragma: no cover - the UPDATE above proved the event is there.
            raise RuntimeError("Story-review run insert returned no ID")
        return StartedStoryCheck(run_id=UUID(str(run["id"])), issue_id=UUID(str(run["issue_id"])))


@asynccontextmanager
async def story_ownership(url: str, issue_id: UUID) -> AsyncIterator[None]:
    """Hold the issue's session lock until the check finishes and its comment is posted.

    Every worker finishing a check of the same issue waits here, so the posting
    decision and the comment edit cannot interleave between them. PostgreSQL
    releases the lock when this session dies, so a crashed worker can't keep the
    others out; no transaction is held while a model call runs. Requires direct
    PostgreSQL or session pooling, as ``app.db.repositories.review_runs`` does.
    """
    async with await connect(url) as connection:
        await connection.execute(
            "SELECT pg_advisory_lock(hashtextextended(%s, 0))", (f"story:{issue_id}",)
        )
        try:
            yield
        finally:
            # Released on close of this session even if the unlock is skipped,
            # and the finally keeps the scope honest when the body raises.
            await connection.execute(
                "SELECT pg_advisory_unlock(hashtextextended(%s, 0))", (f"story:{issue_id}",)
            )


async def finish_story_check(
    url: str, event_id: UUID, run_id: UUID, outcome: StoryCheckOutcome
) -> bool:
    """Store a check's outcome and finalize its delivery; True when its comment should be posted.

    Call only while holding ``story_ownership`` of the issue. A run whose story has
    since been edited (its event older than the issue's stored state) is cancelled
    as superseded and posts nothing: the newer event decides the comment.
    """
    review_absent = outcome.review is None
    # A "couldn't check" stores its stable CouldNotCheck code (the outcome's
    # validator guarantees the reason is set when the review is none).
    error_message = str(outcome.reason) if review_absent else None
    async with await connect(url) as connection, connection.transaction():
        cursor = await connection.execute(
            """SELECT ((e.payload->'issue'->>'updated_at')::timestamptz < i.github_updated_at)
                      AS stale
               FROM analysis_runs r
               JOIN webhook_events e ON e.id = r.trigger_event_id
               JOIN issues i ON i.id = r.issue_id
               WHERE r.id = %s AND r.status = 'running'
               FOR UPDATE OF r""",
            (run_id,),
        )
        run = await cursor.fetchone()
        if run is None:
            # Another finishing check of the same story already superseded this one.
            return False
        if run["stale"]:
            return await _finish_superseded(connection, event_id, run_id)
        await connection.execute(
            """INSERT INTO analysis_outputs
               (analysis_run_id, output_key, output_type, schema_version, json_content,
                metadata)
               VALUES (%s, %s, %s, %s, %s, %s)""",
            (
                run_id,
                OUTPUT_KEY,
                OUTPUT_TYPE,
                SCHEMA_VERSION,
                Jsonb(outcome.model_dump(mode="json")),
                Jsonb(_output_metadata(outcome)),
            ),
        )
        await connection.execute(
            """UPDATE analysis_runs SET status = %s, finished_at = now(), error_message = %s
               WHERE id = %s""",
            ("failed" if review_absent else "completed", error_message, run_id),
        )
        await connection.execute(
            """UPDATE webhook_events SET status = 'processed', processed_at = now()
               WHERE id = %s""",
            (event_id,),
        )
        return True


def _output_metadata(outcome: StoryCheckOutcome) -> dict[str, object]:
    """The run's reproducibility facts, kept beside the stored result like the PR reviews do."""
    metadata: dict[str, object] = {}
    if outcome.model is not None:
        metadata["model"] = outcome.model
    if outcome.provider is not None:
        metadata["provider"] = outcome.provider
    if outcome.generation_id is not None:
        metadata["generation_id"] = outcome.generation_id
    if outcome.usage is not None:
        metadata["usage"] = outcome.usage.model_dump(mode="json")
    if outcome.latency_ms is not None:
        metadata["latency_ms"] = outcome.latency_ms
    return metadata


async def _finish_superseded(
    connection: AsyncConnection[DictRow], event_id: UUID, run_id: UUID
) -> bool:
    """Finalize a check that finished stale, so it never posts over the newest verdict."""
    await connection.execute(
        """UPDATE analysis_runs SET status = 'cancelled', finished_at = now(),
           error_message = %s WHERE id = %s""",
        (SUPERSEDED, run_id),
    )
    await connection.execute(
        """UPDATE webhook_events SET status = 'processed', processed_at = now()
           WHERE id = %s""",
        (event_id,),
    )
    return False


async def fail_story_check(url: str, run_id: UUID) -> None:
    """Record a check that could not finish, when storing or posting failed in the handler.

    Only a still-``running`` run is marked, and the delivery only if its run was: a
    run that already finished keeps its result, and its delivery stays finalized.
    The failed event is marked so nothing re-claims it; the story's next opening or
    edit starts a fresh check.
    """
    async with await connect(url) as connection, connection.transaction():
        cursor = await connection.execute(
            """UPDATE analysis_runs SET status = 'failed', finished_at = now(),
               error_message = 'Interrupted' WHERE id = %s AND status = 'running'
               RETURNING trigger_event_id""",
            (run_id,),
        )
        run = await cursor.fetchone()
        if run is None:
            return
        await connection.execute(
            """UPDATE webhook_events SET status = 'failed', processed_at = now(),
               error_message = 'Interrupted' WHERE id = %s""",
            (run["trigger_event_id"],),
        )
