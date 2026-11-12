"""One short database step of a background job, such as the Action run jobs (US-8, US-15).

A job opens a connection per step, rather than one for the whole job, so no connection is held
while GitHub is called and an artifact downloads.
"""

# AI contribution: 50%

import asyncio
from collections.abc import Awaitable, Callable
from typing import Final

from psycopg import AsyncConnection
from psycopg import Error as DatabaseError
from psycopg.rows import DictRow

from app.core.config import Settings
from app.db import connect

DATABASE_TIMEOUT_SECONDS: Final = 5


class DatabaseStepError(Exception):
    """The database step failed. The message is only the error's type."""


def database_url(settings: Settings) -> str:
    """The configured database URL, or "" when there is none."""
    url = settings.database_url.get_secret_value() if settings.database_url else ""
    return url.strip()


async def in_database[T](
    url: str, operation: Callable[[AsyncConnection[DictRow]], Awaitable[T]]
) -> T:
    """Run ``operation`` on its own connection, within ``DATABASE_TIMEOUT_SECONDS``.

    Raises:
        DatabaseStepError: if the database can't be reached or the step fails.
    """
    try:
        async with asyncio.timeout(DATABASE_TIMEOUT_SECONDS), await connect(url) as connection:
            return await operation(connection)
    except (DatabaseError, OSError, TimeoutError) as error:
        # Only the type: psycopg's message can echo the whole URL, password included.
        raise DatabaseStepError(type(error).__name__) from None
