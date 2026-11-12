"""Sign-in with GitHub, logout and ``GET /api/me`` (US-11.1, ADR 0001)."""

# AI contribution: 50%

import logging
from typing import Annotated, Final, Literal, NamedTuple
from urllib.parse import quote, urlsplit
from uuid import UUID

import httpx
from fastapi import APIRouter, Depends, HTTPException, Request, Response, status
from fastapi.responses import HTMLResponse, RedirectResponse

from app.api.dependencies import current_user, database, require_allowed_origin
from app.api.sign_in_page import SIGN_IN_PATH, sign_in_problem_page, signed_in_page
from app.core.config import Settings, get_settings
from app.core.logging_config import log_safe_exception
from app.db.models.courses import Me
from app.db.models.users import User
from app.db.repositories.courses import list_user_courses
from app.github.oauth import (
    GitHubOAuthError,
    GitHubUser,
    authorize_url,
    fetch_github_user,
    safe_error_code,
)
from app.services.courses import claim_roster_at_sign_in
from app.services.sessions import (
    SESSION_COOKIE,
    SESSION_LIFETIME,
    STATE_COOKIE,
    STATE_LIFETIME,
    OpenedSession,
    end_session,
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
# The callback redirects here (post/redirect/get), so reloading the tab never replays the code.
SIGNED_IN_PATH: Final = "/auth/signed-in"
# The query the callback adds when the roster claim failed, so the page can say so.
NOT_LINKED: Final = "not-linked"
# Remembers where a dashboard sign-in should land after GitHub, only for ALLOWED_ORIGINS.
RETURN_TO_COOKIE: Final = "nikolai_oauth_return_to"
NOT_CONFIGURED: Final = "GitHub sign-in is not configured"
STATE_EXPIRED: Final = "Sign-in expired. Please try again."
GITHUB_FAILED: Final = "GitHub sign-in failed. Try again."
RETURN_TO_NOT_ALLOWED: Final = "That sign-in link isn't allowed."


class OAuthSettings(NamedTuple):
    client_id: str
    client_secret: str
    session_secret: str


def _oauth_settings(settings: Settings) -> OAuthSettings | None:
    """The sign-in settings, or None while any of them is missing."""
    client_id = settings.github_client_id or ""
    client_secret = settings.github_client_secret
    session_secret = settings.session_secret
    if not (client_id and client_secret and session_secret):
        logger.error("Refused a sign-in: the GitHub OAuth settings are incomplete")
        return None
    return OAuthSettings(
        client_id, client_secret.get_secret_value(), session_secret.get_secret_value()
    )


def get_github_transport() -> httpx.AsyncBaseTransport | None:
    """The real network (None), unless a test overrides this dependency."""
    return None


def _allowed_return_to(settings: Settings, value: str | None) -> str | None:
    """The origin to land on after GitHub, when it is exactly one of ALLOWED_ORIGINS.

    Only the origin is kept: a dashboard can ask to come back to itself, never to a path or
    another site. The callback checks the cookie again, so editing it cannot add an origin.
    Only web origins are redirect targets: the same list also holds the extension's
    ``chrome-extension://`` origin for credentialed writes, and a redirect is a different trust
    decision from a write origin.
    """
    if value is None:
        return None
    origin = value.strip().rstrip("/")
    if urlsplit(origin).scheme not in {"http", "https"}:
        return None
    return origin if origin in settings.allowed_origins else None


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


def _forget_return_to(response: Response) -> Response:
    """Drop a stale dashboard return target, so a later sign-in starts clean."""
    _clear_cookie(response, RETURN_TO_COOKIE, STATE_SAMESITE)
    return response


def _retry_path(settings: Settings, request: Request) -> str:
    """Where "Try again" starts over, keeping the dashboard that asked for this sign-in."""
    origin = _allowed_return_to(settings, request.cookies.get(RETURN_TO_COOKIE))
    return SIGN_IN_PATH if origin is None else f"{SIGN_IN_PATH}?return_to={quote(origin, safe='')}"


def _problem_page(message: str, status_code: int, sign_in_path: str) -> HTMLResponse:
    """A failure page that also forgets the return target it used, so nothing is left behind."""
    page = sign_in_problem_page(message, status_code, sign_in_path)
    _clear_cookie(page, RETURN_TO_COOKIE, STATE_SAMESITE)
    return page


@router.get(SIGN_IN_PATH)
async def github_login(
    settings: Annotated[Settings, Depends(get_settings)], return_to: str | None = None
) -> Response:
    """Send the browser to GitHub to approve the sign-in, and remember where to come back to."""
    oauth = _oauth_settings(settings)
    if oauth is None:
        return _forget_return_to(
            sign_in_problem_page(NOT_CONFIGURED, status.HTTP_503_SERVICE_UNAVAILABLE)
        )
    target = _allowed_return_to(settings, return_to)
    if return_to is not None and target is None:
        logger.warning("Refused a sign-in return_to outside ALLOWED_ORIGINS: %r", return_to)
        return _forget_return_to(
            sign_in_problem_page(RETURN_TO_NOT_ALLOWED, status.HTTP_400_BAD_REQUEST)
        )
    state = new_state(oauth.session_secret)
    response = RedirectResponse(authorize_url(oauth.client_id, state))
    _set_cookie(
        response,
        STATE_COOKIE,
        state,
        max_age=int(STATE_LIFETIME.total_seconds()),
        samesite=STATE_SAMESITE,
    )
    if target is not None:
        _set_cookie(
            response,
            RETURN_TO_COOKIE,
            target,
            max_age=int(STATE_LIFETIME.total_seconds()),
            samesite=STATE_SAMESITE,
        )
    else:
        # A previous dashboard sign-in may have left a target behind; this one has none.
        _clear_cookie(response, RETURN_TO_COOKIE, STATE_SAMESITE)
    return response


async def _claim_roster(settings: Settings, user_id: UUID, github_login: str) -> bool:
    """Best effort: a roster problem must never stop someone signing in. True if it worked.

    The claim runs in its own transaction after the session is saved, so a failure leaves the
    roster rows pending and the next sign-in tries again.
    """
    try:
        async with database(settings) as connection:
            await claim_roster_at_sign_in(connection, user_id, github_login)
    except (HTTPException, RuntimeError) as error:
        logger.error(
            "Roster claim failed for GitHub user %s; sign-in continues: %s",
            github_login,
            type(error).__name__,
        )
        return False
    return True


def _no_code_page(error: str | None, sign_in_path: str) -> HTMLResponse:
    """GitHub came back without a code: the user declined, or the App is misconfigured."""
    if error == "access_denied":
        logger.warning("GitHub sign-in was cancelled at GitHub")
        return _problem_page("Sign-in was cancelled.", status.HTTP_400_BAD_REQUEST, sign_in_path)
    # Anything else (redirect_uri_mismatch, application_suspended, ...) is for an operator to fix.
    logger.error("GitHub sent the sign-in back without a code: %s", safe_error_code(error))
    return _problem_page(GITHUB_FAILED, status.HTTP_502_BAD_GATEWAY, sign_in_path)


async def _open_session(
    settings: Settings, user: GitHubUser, sign_in_path: str
) -> OpenedSession | HTMLResponse:
    """Save the user and a new session, or the page that says why that failed."""
    try:
        async with database(settings) as connection:
            return await start_session(connection, user)
    except HTTPException as error:
        return _problem_page(str(error.detail), error.status_code, sign_in_path)
    except RuntimeError as error:
        log_safe_exception(
            logger, "Saving the session failed for GitHub user %s", user.login, error=error
        )
        return _problem_page(
            "Sign-in failed. Try again.",
            status.HTTP_500_INTERNAL_SERVER_ERROR,
            sign_in_path,
        )


def _signed_in_target(settings: Settings, request: Request, linked: bool) -> str:
    """Where to land after sign-in: the dashboard that asked, or the signed-in page."""
    return_to = _allowed_return_to(settings, request.cookies.get(RETURN_TO_COOKIE))
    if return_to is not None:
        return return_to if linked else f"{return_to}/?course={NOT_LINKED}"
    if linked:
        return SIGNED_IN_PATH
    return f"{SIGNED_IN_PATH}?course={NOT_LINKED}"


@router.get("/auth/github/callback")
async def github_callback(
    request: Request,
    settings: Annotated[Settings, Depends(get_settings)],
    transport: Annotated[httpx.AsyncBaseTransport | None, Depends(get_github_transport)],
    code: str | None = None,
    state: str | None = None,
    error: str | None = None,
) -> Response:
    """Finish sign-in: check ``state``, identify the user, open a session, go to the page."""
    # The callback is the last page of a browser tab the extension opened, or the last stop of the
    # dashboard's top-level sign-in redirect, so every answer it can reach is a page the person
    # can read, with a link that starts the sign-in over from wherever they came from.
    retry_path = _retry_path(settings, request)
    oauth = _oauth_settings(settings)
    if oauth is None:
        return _problem_page(NOT_CONFIGURED, status.HTTP_503_SERVICE_UNAVAILABLE, retry_path)
    if not state_is_valid(oauth.session_secret, request.cookies.get(STATE_COOKIE), state):
        logger.warning("Rejected a sign-in callback with a missing or invalid state")
        return _problem_page(STATE_EXPIRED, status.HTTP_400_BAD_REQUEST, retry_path)
    if not code:
        return _no_code_page(error, retry_path)
    try:
        user = await fetch_github_user(
            client_id=oauth.client_id,
            client_secret=oauth.client_secret,
            code=code,
            transport=transport,
        )
    except GitHubOAuthError as oauth_error:
        logger.warning("GitHub sign-in failed: %s", oauth_error)
        return _problem_page(GITHUB_FAILED, status.HTTP_502_BAD_GATEWAY, retry_path)
    session = await _open_session(settings, user, retry_path)
    if isinstance(session, HTMLResponse):
        return session
    linked = await _claim_roster(settings, session.user_id, user.login)
    logger.info("Signed in GitHub user %s", user.login)
    target = _signed_in_target(settings, request, linked)
    response = RedirectResponse(target, status_code=status.HTTP_303_SEE_OTHER)
    _set_cookie(
        response,
        SESSION_COOKIE,
        session.token,
        max_age=int(SESSION_LIFETIME.total_seconds()),
        samesite=SESSION_SAMESITE,
    )
    _clear_cookie(response, STATE_COOKIE, STATE_SAMESITE)
    _clear_cookie(response, RETURN_TO_COOKIE, STATE_SAMESITE)
    return response


@router.get(SIGNED_IN_PATH)
async def signed_in(
    request: Request,
    settings: Annotated[Settings, Depends(get_settings)],
    course: str | None = None,
) -> Response:
    """Who this browser is signed in as. Safe to reload, unlike the callback."""
    try:
        user = await current_user(request, settings)
    except HTTPException as error:
        message = "You're not signed in." if error.status_code == 401 else str(error.detail)
        return sign_in_problem_page(message, error.status_code)
    return signed_in_page(user.github_login, course_linked=course != NOT_LINKED)


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
            await end_session(connection, token)
    response = Response(status_code=status.HTTP_204_NO_CONTENT)
    _clear_cookie(response, SESSION_COOKIE, SESSION_SAMESITE)
    return response


@router.get("/api/me")
async def me(
    user: Annotated[User, Depends(current_user)],
    settings: Annotated[Settings, Depends(get_settings)],
) -> Me:
    """The signed-in user, with their courses, course roles and the teams they can see."""
    async with database(settings) as connection:
        courses = await list_user_courses(connection, user.id)
    return Me(**user.model_dump(), courses=courses)
