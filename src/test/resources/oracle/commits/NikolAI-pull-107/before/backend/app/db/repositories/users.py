"""Signed-in users, their sessions and the instructor flag (ADR 0001)."""

# AI contribution: 50%

from datetime import timedelta

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db.models.users import User


class InstructorGrantError(ValueError):
    """The login does not name exactly one known user. The message is safe to print."""


async def save_user_and_session(
    connection: AsyncConnection[DictRow],
    *,
    github_user_id: int,
    github_login: str,
    display_name: str | None,
    token_hash: str,
    lifetime: timedelta,
) -> None:
    """Upsert the user by GitHub ID and open a session, both or neither."""
    async with connection.transaction():
        cursor = await connection.execute(
            """INSERT INTO users (github_user_id, github_login, display_name)
               VALUES (%s, %s, %s)
               ON CONFLICT (github_user_id) DO UPDATE
               SET github_login = EXCLUDED.github_login,
                   display_name = EXCLUDED.display_name, updated_at = now()
               RETURNING id""",
            (github_user_id, github_login, display_name),
        )
        row = await cursor.fetchone()
        if row is None:
            raise RuntimeError("User insert returned no ID")
        await connection.execute(
            """INSERT INTO user_sessions (user_id, token_hash, expires_at)
               VALUES (%s, %s, now() + %s)""",
            (row["id"], token_hash, lifetime),
        )


async def find_session_user(connection: AsyncConnection[DictRow], token_hash: str) -> User | None:
    """The user of an unexpired, unrevoked session, or None."""
    cursor = await connection.execute(
        """SELECT u.id, u.github_user_id, u.github_login, u.display_name, u.is_instructor
           FROM user_sessions s JOIN users u ON u.id = s.user_id
           WHERE s.token_hash = %s AND s.revoked_at IS NULL AND s.expires_at > now()""",
        (token_hash,),
    )
    row = await cursor.fetchone()
    return User.model_validate(row) if row is not None else None


async def revoke_session(connection: AsyncConnection[DictRow], token_hash: str) -> None:
    """End a session. An unknown or already revoked one changes nothing."""
    await connection.execute(
        """UPDATE user_sessions SET revoked_at = now()
           WHERE token_hash = %s AND revoked_at IS NULL""",
        (token_hash,),
    )


async def grant_instructor(connection: AsyncConnection[DictRow], github_login: str) -> None:
    """Make the user with this login (any case) an instructor. Repeating it is harmless.

    Logins are not unique in ``users``: someone can rename and another account can take the old
    name, leaving two rows until the first signs in again. Refuse rather than guess, because
    this grants a privilege.
    """
    async with connection.transaction():
        cursor = await connection.execute(
            "SELECT id FROM users WHERE lower(github_login) = lower(%s) FOR UPDATE",
            (github_login,),
        )
        rows = await cursor.fetchall()
        if not rows:
            raise InstructorGrantError(
                f"No user with GitHub login {github_login!r}. Signing in once creates one."
            )
        if len(rows) > 1:
            raise InstructorGrantError(
                f"Several users have GitHub login {github_login!r}. Ask them to sign in again."
            )
        await connection.execute(
            "UPDATE users SET is_instructor = true, updated_at = now() WHERE id = %s",
            (rows[0]["id"],),
        )
