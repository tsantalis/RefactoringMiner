"""The rules of a sign-in: session tokens and the OAuth state value (ADR 0001).

A session token is 32 random bytes held by the browser; the database keeps only its SHA-256.
The state value protects the callback from forged sign-ins. It is random, carries its own
expiry and an HMAC, and travels in a cookie so the callback can match it to this browser.
"""

# AI contribution: 50%

import hashlib
import hmac
import secrets
from datetime import timedelta
from time import time
from typing import Final

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db.repositories.users import create_session
from app.github.oauth import GitHubUser

SESSION_COOKIE: Final = "nikolai_session"
SESSION_LIFETIME: Final = timedelta(days=14)
STATE_COOKIE: Final = "nikolai_oauth_state"
STATE_LIFETIME: Final = timedelta(minutes=10)


def hash_token(token: str) -> str:
    """The only form of a session token that is stored."""
    return hashlib.sha256(token.encode()).hexdigest()


async def start_session(connection: AsyncConnection[DictRow], user: GitHubUser) -> str:
    """Record the user, open a session and return its token for the cookie."""
    token = secrets.token_urlsafe(32)
    await create_session(
        connection,
        github_user_id=user.id,
        github_login=user.login,
        display_name=user.name,
        token_hash=hash_token(token),
        lifetime=SESSION_LIFETIME,
    )
    return token


def _sign(secret: str, value: str) -> str:
    return hmac.new(secret.encode(), value.encode(), hashlib.sha256).hexdigest()


def new_state(secret: str) -> str:
    """A fresh ``nonce.expiry.signature`` value."""
    value = f"{secrets.token_urlsafe(32)}.{int(time() + STATE_LIFETIME.total_seconds())}"
    return f"{value}.{_sign(secret, value)}"


def state_is_valid(secret: str, cookie: str | None, state: str | None) -> bool:
    """True if the callback's state is the cookie's, signed by us and unexpired."""
    if cookie is None or state is None:
        return False
    if not hmac.compare_digest(cookie.encode(), state.encode()):
        return False
    value, _, signature = state.rpartition(".")
    if not hmac.compare_digest(_sign(secret, value).encode(), signature.encode()):
        return False
    return int(value.rpartition(".")[2]) > time()
