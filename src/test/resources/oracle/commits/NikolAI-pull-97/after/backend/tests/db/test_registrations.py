"""Real SQL tests for which repositories NikolAI serves (US-8). Need TEST_DATABASE_URL."""

# AI contribution: 50% or more AI-generated

from app.db import connect
from app.db.repositories.registrations import active_repository_id


async def test_a_repository_registered_to_an_active_team_is_served(database_url: str) -> None:
    async with await connect(database_url) as connection:
        assert await active_repository_id(connection, 100) is not None
        assert await active_repository_id(connection, 999) is None


async def test_a_repository_of_an_archived_team_is_not_served(database_url: str) -> None:
    async with await connect(database_url) as connection:
        await connection.execute("UPDATE teams SET archived_at = now()")

        assert await active_repository_id(connection, 100) is None
