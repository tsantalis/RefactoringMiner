"""Sign-in with GitHub, logout and ``GET /api/me`` (US-11.1, ADR 0001)."""

# AI contribution: 50%

import logging
from typing import Annotated, Final, Literal, NamedTuple

import httpx
from fastapi import APIRouter, Depends, HTTPException, Request, Response, status
from fastapi.responses import RedirectResponse

from app.api.dependencies import current_user, database, require_allowed_origin
from app.core.config import Settings, get_settings
from app.db.models.users import User
from app.db.repositories.users import revoke_session
from app.github.oauth import GitHubOAuthError, authorize_url, fetch_github_user
from app.services.sessions import (
    SESSION_COOKIE,
    SESSION_LIFETIME,
    STATE_COOKIE,
    STATE_LIFETIME,
    hash_token,
    new_state,
    start_session,
    state_is_valid,
)

logger = logging.getLogger(__name__)

router = APIRouter(tags=["auth"])

# The extension calls the API cross-site, so the session cookie must be SameSite=None. The state
# cookie only has to survive GitHub's top-level redirect back here, which "lax" allows.
SESSION_SAMESITE: Final = "none"
STATE_SAMESITE: Final = "lax"


class OAuthSettings(NamedTuple):
    client_id: str
    client_secret: str
    session_secret: str


def _oauth_settings(settings: Settings) -> OAuthSettings:
    """The sign-in settings, or 503 while any of them is missing."""
    client_id = settings.github_client_id or ""
    client_secret = settings.github_client_secret
    session_secret = settings.session_secret
    if not (client_id and client_secret and session_secret):
        logger.error("Refused a sign-in: the GitHub OAuth settings are incomplete")
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, "GitHub sign-in is not configured")
    return OAuthSettings(
        client_id, client_secret.get_secret_value(), session_secret.get_secret_value()
    )


def get_github_transport() -> httpx.AsyncBaseTransport | None:
    """The real network (None), unless a test overrides this dependency."""
    return None


def _set_cookie(
    response: Response,
    name: str,
    value: str,
    *,
    max_age: int,
    samesite: Literal["lax", "none"],
) -> None:
    response.set_cookie(
        name, value, max_age=max_age, httponly=True, secure=True, samesite=samesite, path="/"
    )


def _clear_cookie(response: Response, name: str, samesite: Literal["lax", "none"]) -> None:
    response.delete_cookie(name, httponly=True, secure=True, samesite=samesite, path="/")


@router.get("/auth/github/login")
async def github_login(settings: Annotated[Settings, Depends(get_settings)]) -> Response:
    """Send the browser to GitHub to approve the sign-in."""
    oauth = _oauth_settings(settings)
    state = new_state(oauth.session_secret)
    response = RedirectResponse(authorize_url(oauth.client_id, state))
    _set_cookie(
        response,
        STATE_COOKIE,
        state,
        max_age=int(STATE_LIFETIME.total_seconds()),
        samesite=STATE_SAMESITE,
    )
    return response


@router.get("/auth/github/callback")
async def github_callback(
    request: Request,
    settings: Annotated[Settings, Depends(get_settings)],
    transport: Annotated[httpx.AsyncBaseTransport | None, Depends(get_github_transport)],
    code: str | None = None,
    state: str | None = None,
) -> Response:
    """Finish sign-in: check ``state``, identify the user, then open a session."""
    oauth = _oauth_settings(settings)
    if not state_is_valid(oauth.session_secret, request.cookies.get(STATE_COOKIE), state):
        logger.warning("Rejected a sign-in callback with a missing or invalid state")
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Sign-in expired. Please try again.")
    if not code:
        # GitHub comes back without a code when the user declines.
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Sign-in was cancelled.")
    try:
        user = await fetch_github_user(
            client_id=oauth.client_id,
            client_secret=oauth.client_secret,
            code=code,
            transport=transport,
        )
    except GitHubOAuthError as error:
        logger.warning("GitHub sign-in failed: %s", error)
        raise HTTPException(
            status.HTTP_502_BAD_GATEWAY, "GitHub sign-in failed. Try again."
        ) from None
    async with database(settings) as connection:
        token = await start_session(connection, user)
    logger.info("Signed in GitHub user %s", user.login)
    response = RedirectResponse("/api/me", status_code=status.HTTP_303_SEE_OTHER)
    _set_cookie(
        response,
        SESSION_COOKIE,
        token,
        max_age=int(SESSION_LIFETIME.total_seconds()),
        samesite=SESSION_SAMESITE,
    )
    _clear_cookie(response, STATE_COOKIE, STATE_SAMESITE)
    return response


@router.post(
    "/auth/logout",
    status_code=status.HTTP_204_NO_CONTENT,
    dependencies=[Depends(require_allowed_origin)],
)
async def logout(
    request: Request, settings: Annotated[Settings, Depends(get_settings)]
) -> Response:
    """Revoke this browser's session. Signing out twice is not an error."""
    token = request.cookies.get(SESSION_COOKIE)
    if token:
        async with database(settings) as connection:
            await revoke_session(connection, hash_token(token))
    response = Response(status_code=status.HTTP_204_NO_CONTENT)
    _clear_cookie(response, SESSION_COOKIE, SESSION_SAMESITE)
    return response


@router.get("/api/me")
async def me(user: Annotated[User, Depends(current_user)]) -> User:
    """The signed-in user. Courses and teams join this response in ADR 0001's PR 2."""
    return user
