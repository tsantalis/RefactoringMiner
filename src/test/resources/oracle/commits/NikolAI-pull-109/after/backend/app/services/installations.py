"""Keep NikolAI's repositories in step with where the GitHub App is installed (US-10.1)."""

# AI contribution: 50%

import logging
from typing import Final, NamedTuple

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db.repositories.repository_registry import (
    archive_repositories,
    register_installed_repository,
)
from app.github.payloads import InstallationEvent, InstalledRepository

logger = logging.getLogger(__name__)

INSTALLATION_EVENTS: Final = frozenset({"installation", "installation_repositories"})
# "installation" actions that give the App access, and those that take it away. Other actions
# (such as new_permissions_accepted) change nothing NikolAI stores.
_GRANTING: Final = frozenset({"created", "unsuspend"})
_REVOKING: Final = frozenset({"deleted", "suspend"})


class InstallationChange(NamedTuple):
    registered: int
    archived: int
    skipped: int


def _changed_repositories(
    event_name: str, event: InstallationEvent
) -> tuple[list[InstalledRepository], list[InstalledRepository]]:
    """The repositories this delivery adds and removes."""
    if event_name == "installation_repositories":
        return event.repositories_added, event.repositories_removed
    if event.action in _GRANTING:
        return event.repositories, []
    if event.action in _REVOKING:
        return [], event.repositories
    return [], []


async def apply_installation_event(
    connection: AsyncConnection[DictRow], event_name: str, event: InstallationEvent
) -> InstallationChange:
    """Register added repositories and archive removed ones. Redelivering changes nothing."""
    added, removed = _changed_repositories(event_name, event)
    skipped = 0
    for repository in added:
        try:
            await register_installed_repository(connection, repository.id, repository.full_name)
        except ValueError:
            skipped += 1
            logger.warning("Skipped installed repository %s: unexpected name", repository.id)
    archived = await archive_repositories(connection, [r.id for r in removed]) if removed else 0
    change = InstallationChange(len(added) - skipped, archived, skipped)
    logger.info(
        "Installation %s %s %s: %d registered, %d archived, %d skipped",
        event.installation.id,
        event_name,
        event.action,
        *change,
    )
    return change
