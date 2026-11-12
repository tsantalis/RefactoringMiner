"""NikolAI backend: the FastAPI app behind the GitHub App (US-2) and its PR reviews (US-3).

Run it from ``backend/`` (the Docker image runs the same app)::

    uv run uvicorn app.main:app --port 8000

Importing this module reads the settings for the CORS allow-list. Secrets are used only when
the server starts (the LLM provider) or when a webhook arrives (the GitHub App credentials).
"""

# AI contribution: 50%

import asyncio
import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager, suppress

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import (
    auth,
    backlog_coverage,
    courses,
    findings,
    health,
    pulls,
    teams,
    webhooks,
)
from app.core.config import Settings, get_settings
from app.core.logging_config import configure_logging
from app.providers.base import LLMProvider
from app.providers.errors import LLMConfigurationError
from app.providers.factory import create_provider
from app.services.review_worker import run_review_worker

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    """Create one LLM provider for the whole server, and close it on shutdown."""
    configure_logging()
    settings = get_settings()
    settings.require_production_ready()
    provider: LLMProvider | None
    try:
        provider = create_provider(settings)
    except LLMConfigurationError as error:
        # The server still receives webhooks and logs PRs; it just doesn't review them.
        logger.error("PR reviews are disabled: %s", error)
        provider = None
    app.state.llm_provider = provider
    worker = (
        asyncio.create_task(run_review_worker(settings, provider))
        if provider is not None and settings.database_url
        else None
    )
    try:
        yield
    finally:
        if worker is not None:
            worker.cancel()
            with suppress(asyncio.CancelledError):
                await worker
        if provider is not None:
            await provider.aclose()


def create_app(settings: Settings | None = None) -> FastAPI:
    """Build the app. Tests pass settings so the CORS allow-list is the test's."""
    configured = settings or get_settings()
    application = FastAPI(title="NikolAI backend", lifespan=lifespan)
    application.add_middleware(
        CORSMiddleware,
        allow_origins=configured.allowed_origins,
        allow_credentials=True,
        allow_methods=["GET", "POST", "PUT"],
        allow_headers=["Accept", "Content-Type"],
    )
    application.include_router(health.router)
    application.include_router(webhooks.router)
    application.include_router(auth.router)
    application.include_router(courses.router)
    application.include_router(pulls.router)
    application.include_router(findings.router)
    application.include_router(teams.router)
    application.include_router(backlog_coverage.router)

    @application.get("/")
    async def root() -> dict[str, str]:
        """Health check, kept from US-2."""
        return {"message": "NikolAI backend running"}

    return application


app = create_app()
