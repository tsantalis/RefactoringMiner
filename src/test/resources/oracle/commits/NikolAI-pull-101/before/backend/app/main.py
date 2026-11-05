"""NikolAI backend: the FastAPI app behind the GitHub App (US-2) and its PR reviews (US-3).

Run it from ``backend/`` (the Docker image runs the same app)::

    uv run uvicorn app.main:app --port 8000

Importing this module reads no settings, secrets or files. They are read when the server
starts (the LLM provider) or when a webhook arrives (the GitHub App credentials).
"""

import asyncio
import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager, suppress

from fastapi import FastAPI

from app.api.routes import health, webhooks
from app.core.config import get_settings
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


app = FastAPI(title="NikolAI backend", lifespan=lifespan)
app.include_router(health.router)
app.include_router(webhooks.router)


@app.get("/")
async def root() -> dict[str, str]:
    """Health check, kept from US-2."""
    return {"message": "NikolAI backend running"}
