"""Repositories the GitHub App is installed on, and linking them to course teams (US-10.1).

An installed repository starts in a placeholder team: named after it, in no course and with no
members, because an installation event doesn't say which student team it belongs to. NikolAI
doesn't review it there. An instructor links it to a team of their course once the person who
installed it has joined that course. Uninstalling archives it; its stored PRs and reviews stay
readable.
"""

# AI contribution: 50%

import re
from typing import Final
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.errors import UniqueViolation
from psycopg.rows import DictRow

from app.db.models.courses import REPOSITORY_NAME_PATTERN
from app.db.repositories.courses import create_team
from app.db.repositories.feedback import ACTIVE_ROW_FIRST

FULL_NAME: Final = re.compile(REPOSITORY_NAME_PATTERN)


class RepositoryNotFoundError(LookupError):
    """No repository of that name is registered."""


class RepositoryNotLinkableError(ValueError):
    """The repository can't join this course team. The message says why, for the logs only:
    the API answers with one generic refusal so it doesn't reveal another course's repository.
    """


async def register_installed_repository(
    connection: AsyncConnection[DictRow],
    github_repository_id: int,
    full_name: str,
    installed_by: str,
) -> None:
    """Register the repository, or bring back and rename its existing row. Safe to repeat.

    A repository whose team was archived (say, a course team at the end of term) comes back in a
    fresh placeholder team, since an archived team serves nothing. One in a course-less team with
    members (a team from before courses) keeps its installer, so it stays reviewed: that team
    isn't a placeholder, so the repository could never be linked out of it.

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
            """UPDATE repositories r
               SET full_name = %s, archived_at = NULL, updated_at = now(),
                   installed_by_login = CASE
                       WHEN t.course_id IS NULL AND t.archived_at IS NULL
                            AND EXISTS (SELECT 1 FROM team_memberships m
                                        WHERE m.team_id = t.id AND m.left_at IS NULL)
                       THEN r.installed_by_login ELSE %s END
               FROM teams t WHERE t.id = r.team_id AND r.github_repository_id = %s
               RETURNING r.id, t.archived_at IS NOT NULL AS team_archived""",
            (full_name, installed_by, github_repository_id),
        )
        existing = await cursor.fetchone()
        if existing is not None and not existing["team_archived"]:
            return
        team_id = await _placeholder_team(connection, full_name, github_repository_id)
        if existing is not None:
            await connection.execute(
                "UPDATE repositories SET team_id = %s WHERE id = %s", (team_id, existing["id"])
            )
            return
        await connection.execute(
            """INSERT INTO repositories (team_id, github_repository_id, full_name,
                   installed_by_login)
               VALUES (%s, %s, %s, %s)""",
            (team_id, github_repository_id, full_name, installed_by),
        )


async def _placeholder_team(
    connection: AsyncConnection[DictRow], full_name: str, github_repository_id: int
) -> UUID:
    """A course-less team named after the repository (with its GitHub ID if that name is taken)."""
    for name in (full_name, f"{full_name} ({github_repository_id})"):
        try:
            async with connection.transaction():
                return await create_team(connection, None, name)
        except UniqueViolation:
            continue
    raise RuntimeError("No free team name for an installed repository")


async def archive_repositories(
    connection: AsyncConnection[DictRow], github_repository_ids: list[int]
) -> int:
    """Archive the repositories the App was removed from, and the placeholder teams they leave.

    Returns how many repositories were active.
    """
    cursor = await connection.execute(
        """UPDATE repositories SET archived_at = now(), updated_at = now()
           WHERE github_repository_id = ANY(%s) AND archived_at IS NULL
           RETURNING team_id""",
        (github_repository_ids,),
    )
    teams = [row["team_id"] for row in await cursor.fetchall()]
    await _archive_empty_placeholders(connection, teams)
    return len(teams)


async def _archive_empty_placeholders(
    connection: AsyncConnection[DictRow], team_ids: list[UUID]
) -> None:
    """Archive course-less teams left with no active repository and no member."""
    await connection.execute(
        """UPDATE teams t SET archived_at = now()
           WHERE t.id = ANY(%s) AND t.course_id IS NULL AND t.archived_at IS NULL
             AND NOT EXISTS (SELECT 1 FROM repositories r
                             WHERE r.team_id = t.id AND r.archived_at IS NULL)
             AND NOT EXISTS (SELECT 1 FROM team_memberships m
                             WHERE m.team_id = t.id AND m.left_at IS NULL)""",
        (team_ids,),
    )


async def link_repository(
    connection: AsyncConnection[DictRow], course_id: UUID, team_id: UUID, full_name: str
) -> None:
    """Move a registered repository into ``team_id`` of ``course_id``.

    Allowed for a repository already in this course, or one in a placeholder team whose
    installer has joined the course. Joining takes the student's own action (the join code or
    their roster row claimed at sign-in), so an instructor can't claim a repository nobody in
    their course installed. The placeholder it leaves is archived.

    Raises:
        RepositoryNotFoundError: if no repository has that name.
        RepositoryNotLinkableError: if it can't join this course team, with the reason.
    """
    async with connection.transaction():
        # Only a module constant is interpolated; the name is a bound parameter.
        cursor = await connection.execute(
            "SELECT r.id, r.team_id, r.archived_at, r.installed_by_login, t.course_id,"  # noqa: S608
            " EXISTS (SELECT 1 FROM team_memberships m"
            "         WHERE m.team_id = t.id AND m.left_at IS NULL) AS has_members"
            " FROM repositories r JOIN teams t ON t.id = r.team_id"
            " WHERE r.id = (SELECT id FROM repositories WHERE lower(full_name) = lower(%s)"
            f"              ORDER BY {ACTIVE_ROW_FIRST} LIMIT 1)"
            " FOR UPDATE OF r",
            (full_name,),
        )
        row = await cursor.fetchone()
        if row is None:
            raise RepositoryNotFoundError(full_name)
        await _check_linkable(connection, course_id, row)
        await connection.execute(
            "UPDATE repositories SET team_id = %s, updated_at = now() WHERE id = %s",
            (team_id, row["id"]),
        )
        if row["team_id"] != team_id:
            await _archive_empty_placeholders(connection, [row["team_id"]])


async def _check_linkable(
    connection: AsyncConnection[DictRow], course_id: UUID, row: DictRow
) -> None:
    if row["archived_at"] is not None:
        raise RepositoryNotLinkableError("the GitHub App was removed from it")
    if row["course_id"] == course_id:
        return  # Moving between two teams of the same course.
    if row["course_id"] is not None:
        raise RepositoryNotLinkableError("it belongs to another course")
    if row["has_members"]:
        raise RepositoryNotLinkableError("its team has members, so it isn't a placeholder")
    if row["installed_by_login"] is None:
        raise RepositoryNotLinkableError("nobody installed it through the App")
    cursor = await connection.execute(
        """SELECT EXISTS (
               SELECT 1 FROM course_memberships m JOIN users u ON u.id = m.user_id
               WHERE m.course_id = %s AND m.left_at IS NULL
                 AND lower(u.github_login) = lower(%s)) AS known""",
        (course_id, row["installed_by_login"]),
    )
    known = await cursor.fetchone()
    if not (known and known["known"]):
        raise RepositoryNotLinkableError("whoever installed it hasn't joined this course")
