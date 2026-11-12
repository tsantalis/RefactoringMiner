"""GitHub App webhook endpoint (US-2).

Checks that the delivery really comes from GitHub, then hands pull-request events to the
review service (US-3), completed RefactoringMiner Action runs to the refactoring report
(US-8) and completed Semgrep runs to the static findings (US-15), as background tasks:
GitHub waits only about 10 seconds for a reply, and a model review takes 4 to 18 seconds.
Installation events register and archive the repositories the App is installed on
(US-10.1); ``issues`` events check a user story's format and clarity (US-12.1).
"""

# AI contribution: 50%

import asyncio
import json
import logging
from collections.abc import Awaitable, Callable
from functools import partial
from typing import Annotated, cast
from uuid import UUID

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, Request, status
from psycopg import Error as DatabaseError
from pydantic import BaseModel, ValidationError

from app.core.config import Settings, get_settings
from app.db import connect
from app.db.repositories.issue_events import SavedIssueEvent, save_issues_event
from app.db.repositories.pr_events import (
    RepositoryNotRegisteredError,
    SavedPREvent,
    save_pr_event,
)
from app.db.repositories.registrations import active_repository_id
from app.github.payloads import InstallationEvent, IssuesEvent, PullRequestEvent, WorkflowRunEvent
from app.github.signatures import SIGNATURE_HEADER, verify_signature
from app.providers.base import LLMProvider
from app.services.installations import INSTALLATION_EVENTS, apply_installation_event
from app.services.pr_review import handle_pull_request_event, wants_review
from app.services.refactoring_comments import handle_refactoring_run, wants_refactoring_report
from app.services.static_findings import handle_static_analysis_run, wants_static_findings
from app.services.story_check import (
    handle_issues_event,
    is_user_story,
    story_label,
    wants_story_check,
)

logger = logging.getLogger(__name__)

router = APIRouter(tags=["github"])

# A background job reading a completed workflow run's results.
type WorkflowRunJob = Callable[[WorkflowRunEvent, Settings], Awaitable[None]]


def get_llm_provider(request: Request) -> LLMProvider | None:
    """The provider created at startup (see ``app.main``), or None if reviews are disabled."""
    return cast("LLMProvider | None", getattr(request.app.state, "llm_provider", None))


async def _verified_body(request: Request, settings: Settings) -> bytes:
    """The delivery's raw body, once its signature proves it comes from GitHub."""
    secret = settings.github_webhook_secret
    if secret is None or not secret.get_secret_value():
        logger.error("Refused a GitHub webhook: WEBHOOK_SECRET is not set")
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, "Webhook secret not configured")
    body = await request.body()
    if not verify_signature(secret.get_secret_value(), body, request.headers.get(SIGNATURE_HEADER)):
        logger.warning("Rejected a GitHub webhook with a missing or invalid signature")
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Invalid webhook signature")
    return body


def _database_url(settings: Settings) -> str:
    database_url = settings.database_url
    if database_url is None or not database_url.get_secret_value():
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, "Database not configured")
    return database_url.get_secret_value()


async def _apply_installation(
    body: bytes, event_name: str, delivery: str, settings: Settings
) -> dict[str, bool]:
    """Register or archive the repositories an installation delivery names."""
    try:
        event = InstallationEvent.model_validate_json(body)
    except ValidationError:
        logger.warning("Rejected an unreadable %s payload (delivery %s)", event_name, delivery)
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST, f"Unexpected {event_name} payload"
        ) from None
    try:
        async with asyncio.timeout(5), await connect(_database_url(settings)) as connection:
            await apply_installation_event(connection, event_name, event)
    except DatabaseError, TimeoutError:
        # Only the type: psycopg's message can echo the connection string. GitHub doesn't retry
        # a failed delivery by itself, and the transaction left nothing applied.
        logger.error(
            "Could not apply %s delivery %s; nothing was saved. Redeliver it from the GitHub "
            "App's Advanced settings once the database is back",
            event_name,
            delivery,
        )
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE, "Could not save installation change"
        ) from None
    return {"ok": True}


def _parse_event[Event: BaseModel](
    model: type[Event], event_name: str, body: bytes, delivery: str
) -> tuple[Event, UUID]:
    """The typed event of a verified delivery body, and its GitHub delivery ID as a UUID.

    Raises:
        HTTPException: the payload doesn't match ``model``, or the delivery ID is
            missing or not a UUID (400). GitHub numbers every delivery, so a
            check without one cannot be deduplicated or traced.
    """
    try:
        event = model.model_validate_json(body)
    except ValidationError:
        logger.warning("Rejected an unreadable %s payload (delivery %s)", event_name, delivery)
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST, f"Unexpected {event_name} payload"
        ) from None
    try:
        return event, UUID(delivery)
    except ValueError:
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST, "Missing or invalid GitHub delivery ID"
        ) from None


async def _save_pull_request(
    body: bytes,
    event: PullRequestEvent,
    delivery_id: UUID,
    review_requested: bool,
    settings: Settings,
) -> SavedPREvent | None:
    """Persist the delivery, or None when its repository isn't registered."""
    try:
        # Persist before acknowledging or scheduling work. Bound lock/network waits
        # so a database problem doesn't consume GitHub's whole response window.
        async with asyncio.timeout(5), await connect(_database_url(settings)) as connection:
            return await save_pr_event(
                connection, delivery_id, json.loads(body), ignored=not review_requested
            )
    except RepositoryNotRegisteredError:
        logger.warning(
            "Skipped PR delivery %s: repository ID %s has no active registration (or was "
            "installed but not yet linked to a course team by an instructor); link it or run "
            "python -m app.db.register_repository, then redeliver the event",
            delivery_id,
            event.repository.id,
        )
        return None
    except DatabaseError, TimeoutError, ValueError:
        # Database exceptions may contain connection credentials or payload contents.
        logger.error("Could not persist PR delivery %s", delivery_id)
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE, "Could not save webhook event"
        ) from None


@router.post("/webhook", status_code=status.HTTP_202_ACCEPTED)
async def receive_github_webhook(
    request: Request,
    background_tasks: BackgroundTasks,
    settings: Annotated[Settings, Depends(get_settings)],
    provider: Annotated[LLMProvider | None, Depends(get_llm_provider)],
) -> dict[str, bool]:
    """Receive one webhook delivery from the GitHub App."""
    body = await _verified_body(request, settings)
    event_name = request.headers.get("X-GitHub-Event", "")
    delivery = request.headers.get("X-GitHub-Delivery", "-")
    if event_name in INSTALLATION_EVENTS:
        return await _apply_installation(body, event_name, delivery, settings)
    if event_name == "workflow_run":
        return await _receive_workflow_run(body, delivery, settings, provider, background_tasks)
    if event_name == "issues":
        return await _receive_issues(body, delivery, settings, background_tasks, provider)
    if event_name != "pull_request":
        # Includes "ping", which GitHub sends when the webhook is set up.
        logger.info("Ignored a GitHub %r event (delivery %s)", event_name, delivery)
        return {"ok": True}

    event, delivery_id = _parse_event(PullRequestEvent, "pull_request", body, delivery)
    review_requested = wants_review(event)
    saved = await _save_pull_request(body, event, delivery_id, review_requested, settings)
    if saved is None:
        return {"ok": True, "ignored": True}
    if not saved.created:
        logger.info("Already received PR delivery %s", delivery_id)
        return {"ok": True}

    label = f"{event.repository.full_name}#{event.pull_request.number}"
    if not review_requested:
        logger.info("No review for %s (action %r, delivery %s)", label, event.action, delivery)
        return {"ok": True}
    background_tasks.add_task(handle_pull_request_event, event, provider, settings, saved.event_id)
    logger.info("Scheduled a review of %s (delivery %s)", label, delivery)
    return {"ok": True}


async def _registered_repository(
    settings: Settings, github_repository_id: int, delivery: str
) -> bool:
    """Whether NikolAI serves the repository (US-10.1). 503 when the answer can't be read.

    A repository of an installation an instructor hasn't linked to a course team is not
    served, so nothing runs on it (ADR 0001).
    """
    try:
        async with asyncio.timeout(5), await connect(_database_url(settings)) as connection:
            return await active_repository_id(connection, github_repository_id) is not None
    except DatabaseError, TimeoutError:
        # Database exceptions may contain connection credentials.
        logger.error("Could not check the registration for delivery %s", delivery)
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE, "Could not check the repository"
        ) from None


async def _receive_issues(
    body: bytes,
    delivery: str,
    settings: Settings,
    background_tasks: BackgroundTasks,
    provider: LLMProvider | None,
) -> dict[str, bool]:
    """Save the issues delivery and schedule one user story's check (US-12.1).

    Every issues delivery for a registered repository is saved, once per delivery ID:
    housekeeping and non-story ones as ``ignored``, still with the issue they carry,
    so a redelivery is answered idempotently and the runs' history stays traceable.
    A redelivered delivery for a story is scheduled again and the background job's
    run claim keeps that safe: an already processed or in-flight delivery is a no-op,
    one saved just before a crash still gets its check. Only ``user story`` issues
    opened, or edited through their title or body, are checked (ADR 0001's
    acceptance for read access marks what NikolAI serves).
    """
    event, delivery_id = _parse_event(IssuesEvent, "issues", body, delivery)
    checked = wants_story_check(event) and is_user_story(event)
    saved = await _save_issues(body, event, delivery_id, checked, settings)
    if saved is None:
        return {"ok": True, "ignored": True}
    label = story_label(event)
    if not checked:
        if saved.created:
            logger.info(
                "No story check for %s (action %r, delivery %s)", label, event.action, delivery
            )
        else:
            logger.info("Already received issues delivery %s", delivery_id)
        return {"ok": True}
    if not saved.created:
        # A redelivery: schedule the check again. start_story_check claims deliveries
        # that are still ``received`` only, so a delivery saved just before a crash
        # (never processed) still gets its check; one already processed is a no-op —
        # the run claim in the job is what keeps this idempotent.
        logger.info("Issues delivery %s was saved before; checking it now", delivery_id)
    background_tasks.add_task(handle_issues_event, event, provider, settings, saved.event_id)
    logger.info("Scheduled the story check of %s (delivery %s)", label, delivery)
    return {"ok": True}


async def _save_issues(
    body: bytes,
    event: IssuesEvent,
    delivery_id: UUID,
    checked: bool,
    settings: Settings,
) -> SavedIssueEvent | None:
    """Persist the issues delivery, or None when its repository isn't registered."""
    try:
        # Persist before acknowledging or scheduling work. Bound lock/network waits
        # so a database problem doesn't consume GitHub's whole response window.
        async with asyncio.timeout(5), await connect(_database_url(settings)) as connection:
            return await save_issues_event(
                connection, delivery_id, json.loads(body), ignored=not checked
            )
    except RepositoryNotRegisteredError:
        logger.warning(
            "Skipped issues delivery %s: repository ID %s has no active registration (or "
            "was installed but not yet linked to a course team by an instructor); link it "
            "or run python -m app.db.register_repository, then redeliver the event",
            delivery_id,
            event.repository.id,
        )
        return None
    except DatabaseError, TimeoutError, ValueError:
        # Database exceptions may contain connection credentials or payload contents.
        logger.error("Could not persist issues delivery %s", delivery_id)
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE, "Could not save webhook event"
        ) from None


async def _receive_workflow_run(
    body: bytes,
    delivery: str,
    settings: Settings,
    provider: LLMProvider | None,
    background_tasks: BackgroundTasks,
) -> dict[str, bool]:
    """Schedule the job that reads a completed run of a team's workflow."""
    try:
        event = WorkflowRunEvent.model_validate_json(body)
    except ValidationError:
        logger.warning("Rejected an unreadable workflow_run payload (delivery %s)", delivery)
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST, "Unexpected workflow_run payload"
        ) from None
    job = _workflow_run_job(event, provider)
    if job is None:
        return {"ok": True}
    output, handle = job
    if not await _registered_repository(settings, event.repository.id, delivery):
        # The workflows also run on repositories that aren't teams', such as NikolAI's own.
        logger.info(
            "No %s for %s: not registered to an active team (delivery %s)",
            output,
            event.repository.full_name,
            delivery,
        )
        return {"ok": True, "ignored": True}
    background_tasks.add_task(handle, event, settings)
    logger.info(
        "Scheduled the %s of run %d on %s (delivery %s)",
        output,
        event.workflow_run.id,
        event.repository.full_name,
        delivery,
    )
    return {"ok": True}


def _workflow_run_job(
    event: WorkflowRunEvent, provider: LLMProvider | None
) -> tuple[str, WorkflowRunJob] | None:
    """What the run's job outputs (named in the logs) and the job, or None to ignore the run.

    Both jobs get the model: the refactoring report's for its design-change agent (US-16), and
    the static findings' to explain findings that join the checklist of a review that ran before
    they came (#161).
    """
    if wants_refactoring_report(event):
        return "refactoring report", partial(handle_refactoring_run, provider=provider)
    if wants_static_findings(event):
        return "static findings", partial(handle_static_analysis_run, provider=provider)
    return None
