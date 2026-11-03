"""Course rules: join codes, the roster import and the roster claim at sign-in (ADR 0001)."""

# AI contribution: 50%

import logging
import re
import secrets
from collections.abc import Awaitable, Callable
from typing import Final, NamedTuple, cast, get_args
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.errors import UniqueViolation
from psycopg.rows import DictRow

from app.db.models.courses import (
    GITHUB_LOGIN_MAX_LENGTH,
    GITHUB_LOGIN_PATTERN,
    CourseRole,
    RosterResult,
    RosterRowError,
)
from app.db.repositories.courses import (
    create_course,
    rotate_join_code,
    team_ids_by_name,
    users_by_logins,
)
from app.db.repositories.roster import RosterEntry, claim_roster_entries, save_roster_entries

logger = logging.getLogger(__name__)

# Decision 5: 8 characters with no 0/O/1/I/L, so a code read aloud or off a slide is unambiguous.
JOIN_CODE_ALPHABET: Final = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
JOIN_CODE_LENGTH: Final = 8
JOIN_CODE_ATTEMPTS: Final = 5
COURSE_ROLES: Final = frozenset(get_args(CourseRole))
_LOGIN: Final = re.compile(GITHUB_LOGIN_PATTERN)


class JoinCodeExhaustedError(RuntimeError):
    """No unused join code was found in ``JOIN_CODE_ATTEMPTS`` tries."""


class RosterRow(NamedTuple):
    github_login: str
    role: CourseRole
    team: str | None


def new_join_code() -> str:
    return "".join(secrets.choice(JOIN_CODE_ALPHABET) for _ in range(JOIN_CODE_LENGTH))


def normalize_join_code(typed: str) -> str:
    """What a student typed, without case, spaces or dashes ("abcd-efgh" works)."""
    return re.sub(r"[\s-]", "", typed).upper()


async def _with_new_join_code(save: Callable[[str], Awaitable[None]]) -> str:
    """Try fresh codes until one is unused among active courses; return the saved code."""
    for _ in range(JOIN_CODE_ATTEMPTS):
        code = new_join_code()
        try:
            await save(code)
        except UniqueViolation:
            continue
        return code
    raise JoinCodeExhaustedError("Could not find an unused join code")


async def open_course(connection: AsyncConnection[DictRow], name: str, creator_id: UUID) -> UUID:
    """Create a course with a fresh join code; its creator becomes its instructor."""
    created: list[UUID] = []

    async def save(code: str) -> None:
        created.append(
            await create_course(connection, name=name, join_code=code, creator_id=creator_id)
        )

    await _with_new_join_code(save)
    return created[0]


async def replace_join_code(connection: AsyncConnection[DictRow], course_id: UUID) -> str:
    async def save(code: str) -> None:
        await rotate_join_code(connection, course_id, code)

    return await _with_new_join_code(save)


def parse_roster_row(raw: object) -> RosterRow:
    """Check one roster row. Raises ``ValueError`` with a reason the instructor can act on."""
    if not isinstance(raw, dict):
        raise ValueError("row must be an object")
    login = raw.get("github_login")
    login = login.strip() if isinstance(login, str) else None
    if not login or len(login) > GITHUB_LOGIN_MAX_LENGTH or not _LOGIN.fullmatch(login):
        raise ValueError("github_login must be a GitHub username")
    role = raw.get("role", "student")
    if not isinstance(role, str) or role not in COURSE_ROLES:
        raise ValueError("role must be instructor, ta or student")
    team = raw.get("team")
    if team is not None and (not isinstance(team, str) or not team.strip()):
        raise ValueError("team must be a team name")
    return RosterRow(login, cast(CourseRole, role), team.strip() if team else None)


def _check_roster_row(raw: object, teams: dict[str, UUID], seen: set[str]) -> RosterEntry:
    """The row ready to save. Raises ``ValueError`` with the reason it can't be imported."""
    row = parse_roster_row(raw)
    if row.github_login.lower() in seen:
        raise ValueError("this login is already on an earlier row")
    if row.team is None:
        return RosterEntry(row.github_login, row.role, None)
    team_id = teams.get(row.team.lower())
    if team_id is None:
        raise ValueError(f"no team named {row.team!r} in this course")
    return RosterEntry(row.github_login, row.role, team_id)


def _row_error(number: int, raw: object, error: ValueError) -> RosterRowError:
    login = raw.get("github_login") if isinstance(raw, dict) else None
    return RosterRowError(
        row=number, github_login=login if isinstance(login, str) else "", reason=str(error)
    )


async def import_roster(
    connection: AsyncConnection[DictRow],
    course_id: UUID,
    rows: list[object],
    imported_by: UUID,
) -> RosterResult:
    """Save every valid row and report the rest by row number; a bad row never stops the import.

    Rows for people who already signed in are applied at once, so the roster also promotes an
    existing student to TA or moves them to another team. The import is one transaction, so a
    failure part way (a timeout on a huge roster, say) saves nothing rather than half of it.
    """
    teams = await team_ids_by_name(connection, course_id)
    errors: list[RosterRowError] = []
    entries: list[RosterEntry] = []
    seen: set[str] = set()
    for number, raw in enumerate(rows, start=1):
        try:
            entry = _check_roster_row(raw, teams, seen)
        except ValueError as error:
            errors.append(_row_error(number, raw, error))
            continue
        seen.add(entry.github_login.lower())
        entries.append(entry)
    async with connection.transaction():
        await save_roster_entries(
            connection, course_id=course_id, entries=entries, imported_by=imported_by
        )
        applied = await _apply_to_signed_in_users(connection, course_id, entries)
    logger.info(
        "Roster import for course %s: %d saved, %d applied, %d rejected",
        course_id,
        len(entries),
        applied,
        len(errors),
    )
    return RosterResult(imported=len(entries), applied=applied, errors=errors)


async def _apply_to_signed_in_users(
    connection: AsyncConnection[DictRow], course_id: UUID, entries: list[RosterEntry]
) -> int:
    users = await users_by_logins(connection, [entry.github_login for entry in entries])
    applied = 0
    for entry in entries:
        matches = users.get(entry.github_login.lower(), [])
        if len(matches) == 1:  # Two users means a rename in flight: wait for their sign-in.
            applied += await claim_roster_entries(
                connection, user_id=matches[0], github_login=entry.github_login, course_id=course_id
            )
    return applied


async def claim_roster_at_sign_in(
    connection: AsyncConnection[DictRow], user_id: UUID, github_login: str
) -> int:
    """Give a user who just signed in every course role and team their login was rostered for."""
    claimed = await claim_roster_entries(connection, user_id=user_id, github_login=github_login)
    if claimed:
        logger.info("Claimed %d roster entries for GitHub user %s", claimed, github_login)
    return claimed
