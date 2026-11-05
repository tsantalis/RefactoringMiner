"""Register and archive repositories from GitHub App installation webhooks (US-10.1).

The webhook calls these functions after verifying GitHub's signature. A repository
registered here belongs to an installation but not yet to a team: an instructor assigns
it through ``app.db.register_repository`` or the roster import. Uninstalling the App (or
removing one repository from it) archives those repositories so PR deliveries stop being
reviewed.
"""

# AI contribution: 50%

import re
from collections.abc import Iterable
from dataclasses import dataclass
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow

_FULL_NAME = re.compile(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")


@dataclass(frozen=True, slots=True)
class InstallationRepository:
    """One repository as GitHub reports it in an installation delivery."""

    github_repository_id: int
    full_name: str


def _validated_repositories(
    repositories: Iterable[InstallationRepository],
) -> list[InstallationRepository]:
    checked: list[InstallationRepository] = []
    for repository in repositories:
        if repository.github_repository_id <= 0:
            raise ValueError("GitHub repository ID must be positive")
        if not _FULL_NAME.fullmatch(repository.full_name):
            raise ValueError("Repository name must be owner/repository")
        checked.append(repository)
    return checked


async def register_installation_repositories(
    connection: AsyncConnection[DictRow],
    *,
    installation_id: int,
    repositories: Iterable[InstallationRepository],
) -> list[UUID]:
    """Create or refresh the repositories an installation can act on.

    Repeated deliveries are safe. An existing team assignment and all historical rows are
    kept; only the installation ID, the repository name and the archived flag are refreshed.
    """
    if installation_id <= 0:
        raise ValueError("GitHub installation ID must be positive")
    checked = _validated_repositories(repositories)
    if not checked:
        return []
    async with connection.transaction():
        await connection.execute(
            "SELECT pg_advisory_xact_lock(hashtextextended(%s, 0))",
            (f"installation:{installation_id}",),
        )
        repository_ids: list[UUID] = []
        for repository in checked:
            cursor = await connection.execute(
                """INSERT INTO repositories
                       (team_id, github_installation_id, github_repository_id, full_name)
                   VALUES (NULL, %s, %s, %s)
                   ON CONFLICT (github_repository_id) DO UPDATE
                   SET github_installation_id = EXCLUDED.github_installation_id,
                       full_name = EXCLUDED.full_name,
                       archived_at = NULL,
                       updated_at = now()
                   RETURNING id""",
                (installation_id, repository.github_repository_id, repository.full_name),
            )
            row = await cursor.fetchone()
            if row is None:
                raise RuntimeError("Repository registration returned no ID")
            repository_ids.append(UUID(str(row["id"])))
        return repository_ids


async def archive_installation_repositories(
    connection: AsyncConnection[DictRow],
    *,
    installation_id: int,
    github_repository_ids: Iterable[int] | None = None,
) -> int:
    """Archive repositories removed from an installation, or all of them on uninstall.

    Pass ``github_repository_ids=None`` when the whole installation is deleted. Returns the
    number of rows newly archived.
    """
    if installation_id <= 0:
        raise ValueError("GitHub installation ID must be positive")
    repository_ids = None if github_repository_ids is None else list(github_repository_ids)
    if repository_ids is not None and any(repository_id <= 0 for repository_id in repository_ids):
        raise ValueError("GitHub repository IDs must be positive")
    async with connection.transaction():
        await connection.execute(
            "SELECT pg_advisory_xact_lock(hashtextextended(%s, 0))",
            (f"installation:{installation_id}",),
        )
        if repository_ids is None:
            cursor = await connection.execute(
                """UPDATE repositories
                   SET archived_at = now(), updated_at = now()
                   WHERE github_installation_id = %s AND archived_at IS NULL""",
                (installation_id,),
            )
        elif repository_ids:
            cursor = await connection.execute(
                """UPDATE repositories
                   SET archived_at = now(), updated_at = now()
                   WHERE github_installation_id = %s AND github_repository_id = ANY(%s)
                     AND archived_at IS NULL""",
                (installation_id, repository_ids),
            )
        else:
            return 0
        return cursor.rowcount
