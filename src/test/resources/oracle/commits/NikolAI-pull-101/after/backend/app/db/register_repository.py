"""Register an active team/repository before enabling persisted webhook reviews.

Run ``python -m app.db.register_repository --help``. Included in the backend image;
no GitHub credentials or development dependencies are needed. A repository registered
automatically by an installation webhook (US-10.1) has no team yet; this command assigns
it to one.
"""

# AI contribution: 50%

import argparse
import asyncio
import re
from getpass import getpass
from uuid import UUID

from psycopg import AsyncConnection
from psycopg import Error as DatabaseError
from psycopg.rows import DictRow

from app.core.config import ConfigurationError, Settings
from app.db import connect


def _validate(team_name: str, github_repository_id: int, full_name: str) -> None:
    """Refuse input that cannot name a team or a GitHub repository."""
    if not team_name.strip():
        raise ValueError("Team name must not be blank")
    if github_repository_id <= 0:
        raise ValueError("GitHub repository ID must be positive")
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", full_name):
        raise ValueError("Repository name must be owner/repository")


async def _claim_existing(
    connection: AsyncConnection[DictRow], existing: DictRow, *, team_id: UUID, full_name: str
) -> UUID:
    """Refuse a conflicting row, or assign a team to an installation-registered one."""
    if (
        existing["archived_at"] is not None
        or existing["full_name"] != full_name
        or (existing["team_id"] is not None and existing["team_id"] != team_id)
    ):
        raise ValueError("Repository exists with a different team/name or is archived")
    if existing["team_id"] is None:
        # An installation webhook (US-10.1) registered this repository first.
        await connection.execute(
            "UPDATE repositories SET team_id = %s, updated_at = now() WHERE id = %s",
            (team_id, existing["id"]),
        )
    return UUID(str(existing["id"]))


async def _insert_repository(
    connection: AsyncConnection[DictRow],
    *,
    team_id: UUID,
    github_repository_id: int,
    full_name: str,
) -> UUID:
    """Create the repository row and return its ID."""
    cursor = await connection.execute(
        """INSERT INTO repositories (team_id, github_repository_id, full_name)
           VALUES (%s, %s, %s) RETURNING id""",
        (team_id, github_repository_id, full_name),
    )
    row = await cursor.fetchone()
    if row is None:
        raise RuntimeError("Repository registration returned no ID")
    return UUID(str(row["id"]))


async def register_repository(
    connection: AsyncConnection[DictRow],
    *,
    team_id: UUID,
    team_name: str,
    github_repository_id: int,
    full_name: str,
) -> UUID:
    """Create missing rows atomically; identical repeated registrations are safe.

    Require an explicit team UUID so repeated executions cannot create duplicate
    teams. Refuse archived rows, renames and reassignment rather than altering an
    existing team's ownership or historical identity as a side effect of setup.
    """
    _validate(team_name, github_repository_id, full_name)

    async with connection.transaction():
        # Serialize registrations even when two callers choose different teams.
        await connection.execute(
            "SELECT pg_advisory_xact_lock(hashtextextended(%s, 0))",
            (f"register-repository:{github_repository_id}",),
        )
        await connection.execute(
            "INSERT INTO teams (id, name) VALUES (%s, %s) ON CONFLICT (id) DO NOTHING",
            (team_id, team_name),
        )
        cursor = await connection.execute(
            "SELECT name, archived_at FROM teams WHERE id = %s FOR UPDATE", (team_id,)
        )
        team = await cursor.fetchone()
        if team is None or team["archived_at"] is not None or team["name"] != team_name:
            raise ValueError("Team must be active and its name must match the supplied UUID")

        cursor = await connection.execute(
            """SELECT id, team_id, full_name, archived_at FROM repositories
               WHERE github_repository_id = %s FOR UPDATE""",
            (github_repository_id,),
        )
        existing = await cursor.fetchone()
        if existing is not None:
            return await _claim_existing(connection, existing, team_id=team_id, full_name=full_name)
        return await _insert_repository(
            connection,
            team_id=team_id,
            github_repository_id=github_repository_id,
            full_name=full_name,
        )


async def _run(args: argparse.Namespace) -> None:
    if args.prompt_database_url:
        url = getpass("Database URL for the target backend (hidden): ").strip()
    else:
        secret = Settings().database_url
        url = secret.get_secret_value().strip() if secret else ""
    if not url:
        raise ConfigurationError("Set DATABASE_URL or use --prompt-database-url")
    async with await connect(url) as connection:
        repository_id = await register_repository(
            connection,
            team_id=args.team_id,
            team_name=args.team_name,
            github_repository_id=args.github_repository_id,
            full_name=args.repository,
        )
    print(f"Registered repository {repository_id} under team {args.team_id}.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--team-id", type=UUID, required=True)
    parser.add_argument("--team-name", required=True)
    parser.add_argument("--github-repository-id", type=int, required=True)
    parser.add_argument("--repository", required=True, help="Exact GitHub owner/repository name")
    parser.add_argument("--prompt-database-url", action="store_true", help="Enter URL privately")
    args = parser.parse_args()
    try:
        asyncio.run(_run(args))
    except DatabaseError:
        # Connection strings and SQL exceptions can contain credentials.
        parser.exit(1, "Registration failed: check database connectivity and apply the schema.\n")
    except ConfigurationError, ValueError:
        parser.exit(
            1,
            "Registration failed: check DATABASE_URL, repository ID/name and team UUID/name; "
            "existing rows must be active and match.\n",
        )


if __name__ == "__main__":
    main()
