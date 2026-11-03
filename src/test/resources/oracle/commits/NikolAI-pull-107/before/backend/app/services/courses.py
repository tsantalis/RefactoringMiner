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
from app.db.repositories.courses import create_course, find_team, rotate_join_code, users_with_login
from app.db.repositories.roster import claim_roster_entries, save_roster_entry, user_for_github_id

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


def parse_roster_row(raw: dict[str, object]) -> RosterRow:
    """Check one roster row. Raises ``ValueError`` with a reason the instructor can act on."""
    login = raw.get("github_login")
    login = login.strip() if isinstance(login, str) else None
    if not login or len(login) > GITHUB_LOGIN_MAX_LENGTH or not _LOGIN.fullmatch(login):
        raise ValueError("github_login must be a GitHub username")
    role = raw.get("role", "student")
    if role not in COURSE_ROLES:
        raise ValueError("role must be instructor, ta or student")
    team = raw.get("team")
    if team is not None and (not isinstance(team, str) or not team.strip()):
        raise ValueError("team must be a team name")
    return RosterRow(login, cast(CourseRole, role), team.strip() if team else None)


async def _check_roster_row(
    connection: AsyncConnection[DictRow],
    course_id: UUID,
    raw: dict[str, object],
    earlier_logins: list[str],
) -> tuple[RosterRow, UUID | None]:
    """The row and its team's ID. Raises ``ValueError`` with the reason it can't be imported."""
    row = parse_roster_row(raw)
    if row.github_login.lower() in (login.lower() for login in earlier_logins):
        raise ValueError("this login is already on an earlier row")
    if row.team is None:
        return row, None
    team_id = await find_team(connection, course_id, name=row.team)
    if team_id is None:
        raise ValueError(f"no team named {row.team!r} in this course")
    return row, team_id


async def import_roster(
    connection: AsyncConnection[DictRow],
    course_id: UUID,
    rows: list[dict[str, object]],
    imported_by: UUID,
) -> RosterResult:
    """Save every valid row and report the rest by row number; a bad row never stops the import.

    Rows for people who already signed in are applied at once, so the roster also promotes an
    existing student to TA or moves them into a team.
    """
    errors: list[RosterRowError] = []
    saved: list[str] = []
    for number, raw in enumerate(rows, start=1):
        try:
            row, team_id = await _check_roster_row(connection, course_id, raw, saved)
        except ValueError as error:
            login = raw.get("github_login")
            errors.append(
                RosterRowError(
                    row=number,
                    github_login=login if isinstance(login, str) else "",
                    reason=str(error),
                )
            )
            continue
        await save_roster_entry(
            connection,
            course_id=course_id,
            github_login=row.github_login,
            role=row.role,
            team_id=team_id,
            imported_by=imported_by,
        )
        saved.append(row.github_login)
    applied = 0
    for login in saved:
        users = await users_with_login(connection, login)
        if len(users) == 1:  # Two rows means a rename in flight: wait for their sign-in.
            applied += await claim_roster_entries(
                connection, user_id=users[0], github_login=login, course_id=course_id
            )
    logger.info(
        "Roster import for course %s: %d saved, %d applied, %d rejected",
        course_id,
        len(saved),
        applied,
        len(errors),
    )
    return RosterResult(imported=len(saved), applied=applied, errors=errors)


async def claim_roster_at_sign_in(connection: AsyncConnection[DictRow], github_user_id: int) -> int:
    """Give a user who just signed in every course role and team their login was rostered for."""
    user = await user_for_github_id(connection, github_user_id)
    if user is None:
        return 0
    user_id, login = user
    claimed = await claim_roster_entries(connection, user_id=user_id, github_login=login)
    if claimed:
        logger.info("Claimed %d roster entries for GitHub user %s", claimed, login)
    return claimed
