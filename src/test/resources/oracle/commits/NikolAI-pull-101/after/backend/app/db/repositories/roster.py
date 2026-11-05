"""Import a course roster and claim its rows on first sign-in (ADR 0001 decision 8).

A roster row names a GitHub login, a course role and (for students) a team. Rows wait
until that login signs in, then become course and team memberships. A bad row is reported
with its position and never aborts the rest of the import.
"""

# AI contribution: 50%

import re
from collections.abc import Sequence
from dataclasses import dataclass
from typing import Final
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db.models.courses import RosterImportResult, RosterRowError
from app.db.repositories.courses import COURSE_ROLES, lock_course

MAX_ROSTER_ROWS: Final = 1000
_GITHUB_LOGIN = re.compile(r"^[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?$")
_TEAM_NAME_LIMIT: Final = 100
_STUDENT: Final = "student"


@dataclass(frozen=True, slots=True)
class RosterRow:
    """One row as the instructor sent it, before validation."""

    github_login: str
    role: str
    team: str | None


def _row_problem(login: str, role: str, team: str | None) -> str | None:
    """Why this row cannot be imported, or None when it is valid."""
    if not _GITHUB_LOGIN.fullmatch(login):
        return "The GitHub login is not valid."
    if role not in COURSE_ROLES:
        return "The role must be instructor, ta or student."
    if team is not None and not 1 <= len(team) <= _TEAM_NAME_LIMIT:
        return f"The team name must be 1 to {_TEAM_NAME_LIMIT} characters."
    if role == _STUDENT and team is None:
        return "A student needs a team."
    return None


async def import_roster(
    connection: AsyncConnection[DictRow],
    *,
    course_id: UUID,
    rows: Sequence[RosterRow],
) -> RosterImportResult:
    """Upsert valid rows, create missing teams by name, and report refused rows."""
    if len(rows) > MAX_ROSTER_ROWS:
        raise ValueError(f"A roster can hold at most {MAX_ROSTER_ROWS} rows.")
    errors: list[RosterRowError] = []
    imported = 0
    seen: set[str] = set()
    async with connection.transaction():
        await lock_course(connection, course_id)
        for position, row in enumerate(rows, start=1):
            login = row.github_login.strip()
            team = row.team.strip() if row.team else None
            problem = _row_problem(login, row.role, team)
            if problem is not None:
                errors.append(RosterRowError(row=position, error=problem))
                continue
            if login.lower() in seen:
                errors.append(
                    RosterRowError(row=position, error="This login already appears in the roster.")
                )
                continue
            seen.add(login.lower())
            team_id = (
                await _find_or_create_team(connection, course_id=course_id, name=team)
                if team is not None
                else None
            )
            claimed_by = await _upsert_entry(
                connection,
                course_id=course_id,
                login=login,
                role=row.role,
                team_id=team_id,
            )
            if claimed_by is not None:
                await _sync_membership(
                    connection,
                    course_id=course_id,
                    user_id=claimed_by,
                    role=row.role,
                    team_id=team_id,
                )
            imported += 1
    return RosterImportResult(imported=imported, errors=errors)


async def claim_roster_entries(
    connection: AsyncConnection[DictRow], *, user_id: UUID, github_login: str
) -> int:
    """Claim every pending row for this login and apply its course role and team."""
    async with connection.transaction():
        cursor = await connection.execute(
            """SELECT id, course_id, role, team_id FROM course_roster_entries
               WHERE claimed_by_user_id IS NULL AND lower(github_login) = lower(%s)
               FOR UPDATE""",
            (github_login,),
        )
        entries = await cursor.fetchall()
        for entry in entries:
            await _sync_membership(
                connection,
                course_id=UUID(str(entry["course_id"])),
                user_id=user_id,
                role=str(entry["role"]),
                team_id=None if entry["team_id"] is None else UUID(str(entry["team_id"])),
            )
            await connection.execute(
                """UPDATE course_roster_entries
                   SET claimed_by_user_id = %s, claimed_at = now(), updated_at = now()
                   WHERE id = %s""",
                (user_id, entry["id"]),
            )
    return len(entries)


async def _upsert_entry(
    connection: AsyncConnection[DictRow],
    *,
    course_id: UUID,
    login: str,
    role: str,
    team_id: UUID | None,
) -> UUID | None:
    """Insert or update the pending row and return who already claimed it, if anyone."""
    cursor = await connection.execute(
        """SELECT id, claimed_by_user_id FROM course_roster_entries
           WHERE course_id = %s AND lower(github_login) = lower(%s) FOR UPDATE""",
        (course_id, login),
    )
    existing = await cursor.fetchone()
    if existing is None:
        await connection.execute(
            """INSERT INTO course_roster_entries (course_id, github_login, role, team_id)
               VALUES (%s, %s, %s, %s)""",
            (course_id, login, role, team_id),
        )
        return None
    await connection.execute(
        """UPDATE course_roster_entries
           SET role = %s, team_id = %s, updated_at = now()
           WHERE id = %s""",
        (role, team_id, existing["id"]),
    )
    claimed_by = existing["claimed_by_user_id"]
    return None if claimed_by is None else UUID(str(claimed_by))


async def _find_or_create_team(
    connection: AsyncConnection[DictRow], *, course_id: UUID, name: str
) -> UUID:
    """A team named in the roster is created on first mention, then reused."""
    cursor = await connection.execute(
        """SELECT id FROM teams
           WHERE course_id = %s AND lower(name) = lower(%s) AND archived_at IS NULL
           FOR UPDATE""",
        (course_id, name),
    )
    row = await cursor.fetchone()
    if row is not None:
        return UUID(str(row["id"]))
    cursor = await connection.execute(
        "INSERT INTO teams (course_id, name) VALUES (%s, %s) RETURNING id",
        (course_id, name),
    )
    created = await cursor.fetchone()
    if created is None:
        raise RuntimeError("Team insert returned no row")
    return UUID(str(created["id"]))


async def _sync_membership(
    connection: AsyncConnection[DictRow],
    *,
    course_id: UUID,
    user_id: UUID,
    role: str,
    team_id: UUID | None,
) -> None:
    """Give the user this course role and team, without demoting an instructor."""
    cursor = await connection.execute(
        """SELECT id, role FROM course_memberships
           WHERE course_id = %s AND user_id = %s AND left_at IS NULL FOR UPDATE""",
        (course_id, user_id),
    )
    existing = await cursor.fetchone()
    if existing is None:
        await connection.execute(
            """INSERT INTO course_memberships (course_id, user_id, role)
               VALUES (%s, %s, %s)""",
            (course_id, user_id, role),
        )
    elif existing["role"] != role and existing["role"] != "instructor":
        await connection.execute(
            "UPDATE course_memberships SET role = %s WHERE id = %s",
            (role, existing["id"]),
        )
    if team_id is None:
        return
    await connection.execute(
        """UPDATE team_memberships m
           SET left_at = GREATEST(now(), m.joined_at + interval '1 microsecond')
           FROM teams t
           WHERE m.team_id = t.id AND m.user_id = %s AND t.course_id = %s
             AND m.left_at IS NULL AND m.team_id <> %s""",
        (user_id, course_id, team_id),
    )
    cursor = await connection.execute(
        """SELECT id FROM team_memberships
           WHERE team_id = %s AND user_id = %s AND left_at IS NULL FOR UPDATE""",
        (team_id, user_id),
    )
    if await cursor.fetchone() is None:
        await connection.execute(
            "INSERT INTO team_memberships (team_id, user_id, role) VALUES (%s, %s, 'member')",
            (team_id, user_id),
        )
