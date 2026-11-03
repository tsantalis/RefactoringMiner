"""Repositories the GitHub App is installed on, and linking them to course teams (US-10.1).

An installed repository starts in a team of its own, named after it and in no course, because an
installation event doesn't say which student team it belongs to. An instructor then links it to
a team of their course. Uninstalling archives it; its stored PRs and reviews stay readable.
"""

# AI contribution: 50%

import re
from enum import StrEnum
from typing import Final
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.errors import UniqueViolation
from psycopg.rows import DictRow

from app.db.models.courses import REPOSITORY_NAME_PATTERN
from app.db.repositories.feedback import find_repository

FULL_NAME: Final = re.compile(REPOSITORY_NAME_PATTERN)


class LinkRefusal(StrEnum):
    NOT_REGISTERED = "repository_not_registered"
    UNINSTALLED = "repository_uninstalled"
    OTHER_COURSE = "repository_in_another_course"


class RepositoryLinkError(ValueError):
    def __init__(self, refusal: LinkRefusal) -> None:
        super().__init__(refusal.value)
        self.refusal = refusal


async def register_installed_repository(
    connection: AsyncConnection[DictRow], github_repository_id: int, full_name: str
) -> None:
    """Register the repository, or bring back and rename its existing row. Safe to repeat.

    Raises:
        ValueError: if ``full_name`` isn't ``owner/name``.
    """
    if not FULL_NAME.fullmatch(full_name):
        raise ValueError("Repository name must be owner/repository")
    async with connection.transaction():
        # Same lock as ``app.db.register_repository``, so the CLI and webhooks can't race.
        await connection.execute(
            "SELECT pg_advisory_xact_lock(hashtextextended(%s, 0))",
            (f"register-repository:{github_repository_id}",),
        )
        cursor = await connection.execute(
            """UPDATE repositories SET full_name = %s, archived_at = NULL, updated_at = now()
               WHERE github_repository_id = %s RETURNING id""",
            (full_name, github_repository_id),
        )
        if await cursor.fetchone() is not None:
            return
        team_id = await _new_team(connection, full_name, github_repository_id)
        await connection.execute(
            """INSERT INTO repositories (team_id, github_repository_id, full_name)
               VALUES (%s, %s, %s)""",
            (team_id, github_repository_id, full_name),
        )


async def _new_team(
    connection: AsyncConnection[DictRow], full_name: str, github_repository_id: int
) -> UUID:
    """A course-less team named after the repository (with its GitHub ID if that name is taken)."""
    for name in (full_name, f"{full_name} ({github_repository_id})"):
        try:
            async with connection.transaction():
                cursor = await connection.execute(
                    "INSERT INTO teams (name) VALUES (%s) RETURNING id", (name,)
                )
                row = await cursor.fetchone()
        except UniqueViolation:
            continue
        if row is None:
            raise RuntimeError("Team insert returned no ID")
        return UUID(str(row["id"]))
    raise RuntimeError("No free team name for an installed repository")


async def archive_repositories(
    connection: AsyncConnection[DictRow], github_repository_ids: list[int]
) -> int:
    """Archive the repositories the App was removed from. Returns how many were active."""
    cursor = await connection.execute(
        """UPDATE repositories SET archived_at = now(), updated_at = now()
           WHERE github_repository_id = ANY(%s) AND archived_at IS NULL""",
        (github_repository_ids,),
    )
    return cursor.rowcount


async def link_repository(
    connection: AsyncConnection[DictRow], course_id: UUID, team_id: UUID, full_name: str
) -> None:
    """Move a registered repository into ``team_id`` of ``course_id``.

    Only a repository in no course, or already in this course, can move, so one instructor can't
    take another course's repository. The course-less team it leaves is archived once it has no
    active repository or member left.

    Raises:
        RepositoryLinkError: when the repository can't be linked, with the reason.
    """
    found = await find_repository(connection, full_name)
    if found is None:
        raise RepositoryLinkError(LinkRefusal.NOT_REGISTERED)
    async with connection.transaction():
        cursor = await connection.execute(
            """SELECT r.team_id, r.archived_at, t.course_id FROM repositories r
               JOIN teams t ON t.id = r.team_id WHERE r.id = %s FOR UPDATE OF r""",
            (found.id,),
        )
        row = await cursor.fetchone()
        if row is None:
            raise RepositoryLinkError(LinkRefusal.NOT_REGISTERED)
        if row["archived_at"] is not None:
            raise RepositoryLinkError(LinkRefusal.UNINSTALLED)
        if row["course_id"] is not None and row["course_id"] != course_id:
            raise RepositoryLinkError(LinkRefusal.OTHER_COURSE)
        await connection.execute(
            "UPDATE repositories SET team_id = %s, updated_at = now() WHERE id = %s",
            (team_id, found.id),
        )
        await connection.execute(
            """UPDATE teams SET archived_at = now()
               WHERE id = %(old)s AND id <> %(new)s AND course_id IS NULL
                 AND archived_at IS NULL
                 AND NOT EXISTS (SELECT 1 FROM repositories
                                 WHERE team_id = %(old)s AND archived_at IS NULL)
                 AND NOT EXISTS (SELECT 1 FROM team_memberships
                                 WHERE team_id = %(old)s AND left_at IS NULL)""",
            {"old": row["team_id"], "new": team_id},
        )
