"""Apply every numbered file in ``migrations/`` that this database has not had yet.

This is an explicit deployment command, not an automatic server-startup migration, and it is
safe to rerun. Run ``python -m app.db.migrate --help`` for database selection options.
"""

# AI contribution: 50%

import argparse
import asyncio
from getpass import getpass
from pathlib import Path
from typing import Final

from psycopg import AsyncConnection
from psycopg import Error as DatabaseError
from psycopg.rows import DictRow

from app.core.config import ConfigurationError, Settings
from app.db import connect

Connection = AsyncConnection[DictRow]

MIGRATIONS_DIR: Final = Path(__file__).resolve().parents[2] / "migrations"
# Databases set up by init_schema have this file's tables but no record of it.
INITIAL_MIGRATION: Final = "0001_initial_schema.sql"


async def _record_initial_schema(connection: Connection) -> None:
    """Mark 0001 as applied when ``init_schema`` created the tables before this runner existed."""
    await connection.execute(
        """INSERT INTO schema_migrations (filename)
           SELECT %s WHERE to_regclass('public.users') IS NOT NULL
             AND NOT EXISTS (SELECT 1 FROM schema_migrations)""",
        (INITIAL_MIGRATION,),
    )


def _read_migrations(directory: Path) -> dict[str, str]:
    """Each numbered file's SQL by name, in order, read before the transaction starts."""
    paths = sorted(directory.glob("[0-9]*.sql"))
    return {path.name: path.read_text(encoding="utf-8") for path in paths}


async def apply_migrations(
    connection: Connection, *, directory: Path = MIGRATIONS_DIR
) -> list[str]:
    """Apply the pending files in name order and return their names.

    The whole run is one transaction: if any file fails, the database stays as it was.
    """
    migrations = _read_migrations(directory)
    applied: list[str] = []
    async with connection.transaction():
        await connection.execute("SET LOCAL lock_timeout = '5s'")
        await connection.execute("SET LOCAL statement_timeout = '60s'")
        await connection.execute("SET LOCAL search_path TO public")
        await connection.execute(
            "SELECT pg_advisory_xact_lock(hashtextextended('nikolai:migrations', 0))"
        )
        await connection.execute(
            """CREATE TABLE IF NOT EXISTS schema_migrations (
                   filename text PRIMARY KEY,
                   applied_at timestamp with time zone DEFAULT now() NOT NULL
               )"""
        )
        await _record_initial_schema(connection)
        cursor = await connection.execute("SELECT filename FROM schema_migrations")
        done = {row["filename"] for row in await cursor.fetchall()}
        for name, migration in migrations.items():
            if name in done:
                continue
            await connection.execute(migration)
            await connection.execute(
                "INSERT INTO schema_migrations (filename) VALUES (%s)", (name,)
            )
            applied.append(name)
    return applied


async def _run(*, prompt_database_url: bool) -> None:
    if prompt_database_url:
        url = getpass("Database URL to migrate (hidden): ").strip()
    else:
        secret = Settings().database_url
        url = secret.get_secret_value().strip() if secret else ""
    if not url:
        raise ConfigurationError("Set DATABASE_URL or use --prompt-database-url")
    async with await connect(url) as connection:
        applied = await apply_migrations(connection)
    for name in applied:
        print(f"Applied {name}")
    if not applied:
        print("Database is up to date.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prompt-database-url", action="store_true", help="Enter URL privately")
    args = parser.parse_args()
    try:
        asyncio.run(_run(prompt_database_url=args.prompt_database_url))
    except DatabaseError:
        # Connection strings and SQL exceptions can contain credentials.
        parser.exit(1, "Migration failed: nothing was changed. Check connectivity and the SQL.\n")
    except OSError:
        parser.exit(1, "Cannot read the migrations folder from this installation.\n")
    except ConfigurationError, ValueError:
        parser.exit(1, "Migrating requires a valid DATABASE_URL or --prompt-database-url.\n")


if __name__ == "__main__":
    main()
