"""GitHub App webhook endpoint (US-2).

Checks that the delivery really comes from GitHub, then hands pull-request events to the
review service (US-3), completed RefactoringMiner Action runs to the refactoring report (US-8)
and completed Semgrep runs to the static findings (US-15), as background tasks: GitHub waits
only about 10 seconds for a reply, and a model review takes 4 to 18 seconds. Installation
events register and archive the repositories the App is installed on (US-10.1).
"""

# AI contribution: 50%

import asyncio
import json
import logging
from collections.abc import Awaitable, Callable
from typing import Annotated, cast
from uuid import UUID

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, Request, status
from psycopg import Error as DatabaseError
from pydantic import ValidationError

from app.core.config import Settings, get_settings
from app.db import connect
from app.db.repositories.pr_events import (
    RepositoryNotRegisteredError,
    SavedPREvent,
    save_pr_event,
)
from app.db.repositories.registrations import active_repository_id
from app.github.payloads import InstallationEvent, PullRequestEvent, WorkflowRunEvent
from app.github.signatures import SIGNATURE_HEADER, verify_signature
from app.providers.base import LLMProvider
from app.services.installations import INSTALLATION_EVENTS, apply_installation_event
from app.services.pr_review import handle_pull_request_event, wants_review
from app.services.refactoring_comments import handle_refactoring_run, wants_refactoring_report
from app.services.static_findings import handle_static_analysis_run, wants_static_findings

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


def _parse_pull_request(body: bytes, delivery: str) -> tuple[PullRequestEvent, UUID]:
    try:
        event = PullRequestEvent.model_validate_json(body)
    except ValidationError:
        logger.warning("Rejected an unreadable pull_request payload (delivery %s)", delivery)
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST, "Unexpected pull_request payload"
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
        return await _receive_workflow_run(body, delivery, settings, background_tasks)
    if event_name != "pull_request":
        # Includes "ping", which GitHub sends when the webhook is set up.
        logger.info("Ignored a GitHub %r event (delivery %s)", event_name, delivery)
        return {"ok": True}

    event, delivery_id = _parse_pull_request(body, delivery)
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


async def _receive_workflow_run(
    body: bytes, delivery: str, settings: Settings, background_tasks: BackgroundTasks
) -> dict[str, bool]:
    """Schedule the job that reads a completed run of a team's workflow."""
    try:
        event = WorkflowRunEvent.model_validate_json(body)
    except ValidationError:
        logger.warning("Rejected an unreadable workflow_run payload (delivery %s)", delivery)
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST, "Unexpected workflow_run payload"
        ) from None
    job = _workflow_run_job(event)
    if job is None:
        return {"ok": True}
    output, handle = job
    try:
        async with asyncio.timeout(5), await connect(_database_url(settings)) as connection:
            registered = await active_repository_id(connection, event.repository.id) is not None
    except DatabaseError, TimeoutError:
        # Database exceptions may contain connection credentials.
        logger.error("Could not check the registration for delivery %s", delivery)
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE, "Could not check the repository"
        ) from None
    if not registered:
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


def _workflow_run_job(event: WorkflowRunEvent) -> tuple[str, WorkflowRunJob] | None:
    """What the run's job outputs (named in the logs) and the job, or None to ignore the run."""
    if wants_refactoring_report(event):
        return "refactoring report", handle_refactoring_run
    if wants_static_findings(event):
        return "static findings", handle_static_analysis_run
    return None
