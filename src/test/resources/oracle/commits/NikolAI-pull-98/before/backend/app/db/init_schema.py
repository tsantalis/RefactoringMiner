"""Apply 0001_initial_schema.sql once to an empty public schema.

This is an explicit setup command, not an automatic server-startup migration.
Run ``python -m app.db.init_schema --help`` for database selection options.
"""

import argparse
import asyncio
from getpass import getpass
from pathlib import Path

from psycopg import AsyncConnection
from psycopg import Error as DatabaseError
from psycopg.rows import DictRow

from app.core.config import ConfigurationError, Settings
from app.db import connect

MIGRATION = Path(__file__).resolve().parents[2] / "migrations/0001_initial_schema.sql"


class SchemaNotEmptyError(ValueError):
    """Initial setup must not be run over existing application tables or data."""


async def initialize_schema(connection: AsyncConnection[DictRow]) -> None:
    """Apply the initial SQL atomically, refusing an initialized or partial schema."""
    migration = MIGRATION.read_text(encoding="utf-8")
    async with connection.transaction():
        await connection.execute("SET LOCAL lock_timeout = '5s'")
        await connection.execute("SET LOCAL statement_timeout = '60s'")
        await connection.execute("SET LOCAL search_path TO public")
        await connection.execute(
            "SELECT pg_advisory_xact_lock(hashtextextended('nikolai:initial-schema', 0))"
        )
        cursor = await connection.execute(
            """SELECT EXISTS (
                   SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                   WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p', 'v', 'm', 'S', 'f')
               ) AS populated"""
        )
        row = await cursor.fetchone()
        if row is None or row["populated"]:
            raise SchemaNotEmptyError("The public schema already contains database objects")
        await connection.execute(migration)


async def _run(*, prompt_database_url: bool) -> None:
    if prompt_database_url:
        url = getpass("Database URL to initialize (hidden): ").strip()
    else:
        secret = Settings().database_url
        url = secret.get_secret_value().strip() if secret else ""
    if not url:
        raise ConfigurationError("Set DATABASE_URL or use --prompt-database-url")
    async with await connect(url) as connection:
        await initialize_schema(connection)
    print("Applied 0001_initial_schema.sql successfully.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prompt-database-url", action="store_true", help="Enter URL privately")
    args = parser.parse_args()
    try:
        asyncio.run(_run(prompt_database_url=args.prompt_database_url))
    except SchemaNotEmptyError:
        parser.exit(1, "Refused: public schema is not empty. No changes made; do not rerun 0001.\n")
    except DatabaseError:
        parser.exit(1, "Schema setup failed: check connectivity, SQL and database permissions.\n")
    except OSError:
        parser.exit(1, "Cannot read migrations/0001_initial_schema.sql from this installation.\n")
    except ConfigurationError, ValueError:
        parser.exit(1, "Schema setup requires a valid DATABASE_URL or --prompt-database-url.\n")


if __name__ == "__main__":
    main()
