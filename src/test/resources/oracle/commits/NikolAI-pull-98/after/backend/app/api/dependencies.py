"""What authenticated endpoints share: the database, ``current_user`` and the Origin check."""

# AI contribution: 50%

import asyncio
import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from typing import Annotated, Final

from fastapi import Depends, HTTPException, Request, status
from psycopg import AsyncConnection
from psycopg import Error as DatabaseError
from psycopg.rows import DictRow

from app.core.config import Settings, get_settings
from app.db import connect
from app.db.models.users import User
from app.db.repositories.users import find_session_user
from app.services.sessions import SESSION_COOKIE, hash_token

logger = logging.getLogger(__name__)

DATABASE_TIMEOUT_SECONDS: Final = 5


@asynccontextmanager
async def database(settings: Settings) -> AsyncIterator[AsyncConnection[DictRow]]:
    """One connection for a request's work; any database failure becomes a clean 503."""
    url = settings.database_url.get_secret_value() if settings.database_url else ""
    if not url.strip():
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, "Database not configured")
    try:
        async with asyncio.timeout(DATABASE_TIMEOUT_SECONDS), await connect(url) as connection:
            yield connection
    except (DatabaseError, OSError, TimeoutError) as error:
        # Only the type: psycopg's message can echo the whole URL, password included.
        logger.error("Database request failed: %s", type(error).__name__)
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, "Database unavailable") from None


async def current_user(
    request: Request, settings: Annotated[Settings, Depends(get_settings)]
) -> User:
    """The signed-in user, or 401 with no valid session.

    Reads need only this. Writes must also depend on ``require_allowed_origin`` (ADR 0001,
    decision 3).
    """
    token = request.cookies.get(SESSION_COOKIE)
    if not token:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Sign in required")
    async with database(settings) as connection:
        user = await find_session_user(connection, hash_token(token))
    if user is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Sign in required")
    return user


def require_allowed_origin(
    request: Request, settings: Annotated[Settings, Depends(get_settings)]
) -> None:
    """Guard cookie-authenticated writes.

    The session cookie is ``SameSite=None``, so browsers attach it to requests from any site;
    the ``Origin`` header, which pages cannot forge, is what stops cross-site writes.
    """
    origin = request.headers.get("Origin")
    if origin not in settings.allowed_origins:
        logger.warning("Refused a cookie-authenticated write from origin %r", origin)
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Origin not allowed")
