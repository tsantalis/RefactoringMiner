"""GitHub App webhook endpoint (US-2, US-10.1).

Checks that the delivery really comes from GitHub, then:
- registers or archives repositories for installation events (US-10.1);
- hands pull-request events to the review service (US-3) as a background task, because
  GitHub waits only about 10 seconds for a reply and a model review takes 4 to 18 seconds.
"""

# AI contribution: 50%

import asyncio
import json
import logging
from typing import Annotated, cast
from uuid import UUID

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, Request, status
from psycopg import Error as DatabaseError
from pydantic import ValidationError

from app.core.config import Settings, get_settings
from app.db import connect
from app.db.repositories.installations import (
    InstallationRepository,
    archive_installation_repositories,
    register_installation_repositories,
)
from app.db.repositories.pr_events import (
    RepositoryNotRegisteredError,
    SavedPREvent,
    save_pr_event,
)
from app.github.payloads import (
    InstallationEvent,
    InstallationRepositoriesEvent,
    PullRequestEvent,
    RepositorySummary,
)
from app.github.signatures import SIGNATURE_HEADER, verify_signature
from app.providers.base import LLMProvider
from app.services.pr_review import handle_pull_request_event, wants_review

logger = logging.getLogger(__name__)

router = APIRouter(tags=["github"])

# Deliveries that tell us which repositories the App can now act on.
INSTALLATION_EVENTS = frozenset({"installation", "installation_repositories"})
# GitHub waits about 10 seconds; keep database waits inside that window.
_INSTALLATION_TIMEOUT_SECONDS = 8


def get_llm_provider(request: Request) -> LLMProvider | None:
    """The provider created at startup (see ``app.main``), or None if reviews are disabled."""
    return cast("LLMProvider | None", getattr(request.app.state, "llm_provider", None))


def _require_database(settings: Settings) -> str:
    """Return the configured database URL, or refuse the delivery with HTTP 503."""
    database_url = settings.database_url
    if database_url is None or not database_url.get_secret_value():
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, "Database not configured")
    return database_url.get_secret_value()


def _installation_repositories(
    repositories: list[RepositorySummary],
) -> list[InstallationRepository]:
    """The identifier and name of each repository in a delivery."""
    return [
        InstallationRepository(repository.id, repository.full_name) for repository in repositories
    ]


async def _apply_installation_event(
    event: InstallationEvent, delivery: str, database_url: str
) -> None:
    """Register an installation's repositories, or archive them when it is uninstalled."""
    installation_id = event.installation.id
    if event.action == "deleted":
        await _archive_installation(database_url, installation_id)
    elif event.action in {"created", "new_permissions_accepted"}:
        await _register_installation(
            database_url, installation_id, _installation_repositories(event.repositories)
        )
    else:
        logger.info("Ignored installation action %r (delivery %s)", event.action, delivery)


async def _apply_installation_repositories_event(
    event: InstallationRepositoriesEvent, delivery: str, database_url: str
) -> None:
    """Register the repositories added to an installation, or archive the removed ones."""
    installation_id = event.installation.id
    if event.action == "added":
        await _register_installation(
            database_url, installation_id, _installation_repositories(event.repositories_added)
        )
    elif event.action == "removed":
        await _archive_installation_repositories(
            database_url,
            installation_id,
            [repository.id for repository in event.repositories_removed],
        )
    else:
        logger.info(
            "Ignored installation_repositories action %r (delivery %s)", event.action, delivery
        )


async def _handle_installation_delivery(
    event_name: str, body: bytes, delivery: str, database_url: str
) -> dict[str, bool]:
    """Register repositories when the App is installed, archive them when it is removed."""
    try:
        if event_name == "installation":
            await _apply_installation_event(
                InstallationEvent.model_validate_json(body), delivery, database_url
            )
        else:
            await _apply_installation_repositories_event(
                InstallationRepositoriesEvent.model_validate_json(body), delivery, database_url
            )
    except ValidationError:
        logger.warning("Rejected an unreadable %s payload (delivery %s)", event_name, delivery)
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST, f"Unexpected {event_name} payload"
        ) from None
    except ValueError:
        logger.warning("Rejected an invalid %s payload (delivery %s)", event_name, delivery)
        raise HTTPException(status.HTTP_400_BAD_REQUEST, f"Invalid {event_name} payload") from None
    except DatabaseError, TimeoutError:
        # Database exceptions may contain connection credentials or payload contents.
        logger.error("Could not process %s delivery %s", event_name, delivery)
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE, "Could not save installation event"
        ) from None
    return {"ok": True}


async def _register_installation(
    database_url: str, installation_id: int, repositories: list[InstallationRepository]
) -> None:
    async with (
        asyncio.timeout(_INSTALLATION_TIMEOUT_SECONDS),
        await connect(database_url) as connection,
    ):
        saved = await register_installation_repositories(
            connection, installation_id=installation_id, repositories=repositories
        )
    logger.info("Registered %d repositories for installation %s", len(saved), installation_id)


async def _archive_installation(database_url: str, installation_id: int) -> None:
    async with (
        asyncio.timeout(_INSTALLATION_TIMEOUT_SECONDS),
        await connect(database_url) as connection,
    ):
        archived = await archive_installation_repositories(
            connection, installation_id=installation_id
        )
    logger.info(
        "Archived %d repositories of uninstalled installation %s", archived, installation_id
    )


async def _archive_installation_repositories(
    database_url: str, installation_id: int, repository_ids: list[int]
) -> None:
    async with (
        asyncio.timeout(_INSTALLATION_TIMEOUT_SECONDS),
        await connect(database_url) as connection,
    ):
        archived = await archive_installation_repositories(
            connection, installation_id=installation_id, github_repository_ids=repository_ids
        )
    logger.info("Archived %d repositories removed from installation %s", archived, installation_id)


def _parse_pull_request(body: bytes, delivery: str) -> tuple[PullRequestEvent, UUID]:
    """The typed event and its delivery ID, or HTTP 400 when either is unusable."""
    try:
        event = PullRequestEvent.model_validate_json(body)
    except ValidationError:
        logger.warning("Rejected an unreadable pull_request payload (delivery %s)", delivery)
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST, "Unexpected pull_request payload"
        ) from None
    try:
        delivery_id = UUID(delivery)
    except ValueError:
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST, "Missing or invalid GitHub delivery ID"
        ) from None
    return event, delivery_id


async def _persist_pull_request(
    body: bytes,
    event: PullRequestEvent,
    delivery_id: UUID,
    *,
    review_requested: bool,
    database_url: str,
) -> SavedPREvent:
    """Save the delivery before anything is acknowledged or scheduled."""
    try:
        # Bound lock/network waits so a database problem doesn't consume GitHub's
        # whole response window.
        async with asyncio.timeout(5), await connect(database_url) as connection:
            return await save_pr_event(
                connection, delivery_id, json.loads(body), ignored=not review_requested
            )
    except RepositoryNotRegisteredError:
        raise
    except DatabaseError, TimeoutError, ValueError:
        # Database exceptions may contain connection credentials or payload contents.
        logger.error("Could not persist PR delivery %s", delivery_id)
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE, "Could not save webhook event"
        ) from None


async def _handle_pull_request_delivery(
    body: bytes,
    delivery: str,
    database_url: str,
    background_tasks: BackgroundTasks,
    provider: LLMProvider | None,
    settings: Settings,
) -> dict[str, bool]:
    """Persist a PR delivery and schedule a review when the action asks for one."""
    event, delivery_id = _parse_pull_request(body, delivery)
    review_requested = wants_review(event)
    try:
        saved = await _persist_pull_request(
            body, event, delivery_id, review_requested=review_requested, database_url=database_url
        )
    except RepositoryNotRegisteredError:
        logger.warning(
            "Skipped PR delivery %s: repository ID %s has no active registration; "
            "run python -m app.db.register_repository, then redeliver the event",
            delivery_id,
            event.repository.id,
        )
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


@router.post("/webhook", status_code=status.HTTP_202_ACCEPTED)
async def receive_github_webhook(
    request: Request,
    background_tasks: BackgroundTasks,
    settings: Annotated[Settings, Depends(get_settings)],
    provider: Annotated[LLMProvider | None, Depends(get_llm_provider)],
) -> dict[str, bool]:
    """Receive one webhook delivery from the GitHub App."""
    secret = settings.github_webhook_secret
    if secret is None or not secret.get_secret_value():
        logger.error("Refused a GitHub webhook: WEBHOOK_SECRET is not set")
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, "Webhook secret not configured")
    body = await request.body()
    if not verify_signature(secret.get_secret_value(), body, request.headers.get(SIGNATURE_HEADER)):
        logger.warning("Rejected a GitHub webhook with a missing or invalid signature")
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Invalid webhook signature")

    event_name = request.headers.get("X-GitHub-Event", "")
    delivery = request.headers.get("X-GitHub-Delivery", "-")
    database_url = _require_database(settings)
    if event_name in INSTALLATION_EVENTS:
        return await _handle_installation_delivery(event_name, body, delivery, database_url)
    if event_name != "pull_request":
        # Includes "ping", which GitHub sends when the webhook is set up.
        logger.info("Ignored a GitHub %r event (delivery %s)", event_name, delivery)
        return {"ok": True}
    return await _handle_pull_request_delivery(
        body, delivery, database_url, background_tasks, provider, settings
    )
