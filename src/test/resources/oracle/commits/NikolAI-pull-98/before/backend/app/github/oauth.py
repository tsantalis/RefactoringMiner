"""GitHub user authorization, the OAuth web flow every GitHub App has (ADR 0001).

The user token only answers "who is this?": it is used once for ``GET /user`` and never stored.
https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-a-user-access-token-for-a-github-app
"""

# AI contribution: 50%

from typing import Final
from urllib.parse import urlencode

import httpx
from pydantic import BaseModel, Field, ValidationError

AUTHORIZE_URL: Final = "https://github.com/login/oauth/authorize"
TOKEN_URL: Final = "https://github.com/login/oauth/access_token"  # noqa: S105
USER_URL: Final = "https://api.github.com/user"
TIMEOUT_SECONDS: Final = 10.0


class GitHubOAuthError(RuntimeError):
    """GitHub refused the code or answered with something unusable. Holds no token."""


class GitHubUser(BaseModel):
    """The fields of ``GET /user`` NikolAI keeps."""

    id: int = Field(gt=0)
    login: str = Field(min_length=1)
    name: str | None = None


def authorize_url(client_id: str, state: str) -> str:
    """Where to send the browser. GitHub uses the callback URL set on the App."""
    return f"{AUTHORIZE_URL}?{urlencode({'client_id': client_id, 'state': state})}"


async def _exchange_code(
    client: httpx.AsyncClient, client_id: str, client_secret: str, code: str
) -> str:
    """Swap the callback's one-time code for a user token.

    GitHub reports a bad code with status 200 and an ``error`` field, so a 200 proves nothing.
    """
    response = await client.post(
        TOKEN_URL,
        data={"client_id": client_id, "client_secret": client_secret, "code": code},
        headers={"Accept": "application/json"},
    )
    response.raise_for_status()
    token = response.json().get("access_token")
    if not isinstance(token, str) or not token:
        raise GitHubOAuthError("GitHub returned no user token")
    return token


async def fetch_github_user(
    *,
    client_id: str,
    client_secret: str,
    code: str,
    transport: httpx.AsyncBaseTransport | None = None,
) -> GitHubUser:
    """Return the GitHub user who approved ``code``; the user token goes out of scope here.

    Raises:
        GitHubOAuthError: if GitHub cannot be reached or its answer is unusable.
    """
    try:
        async with httpx.AsyncClient(timeout=TIMEOUT_SECONDS, transport=transport) as client:
            token = await _exchange_code(client, client_id, client_secret, code)
            response = await client.get(
                USER_URL,
                headers={
                    "Authorization": f"Bearer {token}",
                    "Accept": "application/vnd.github+json",
                },
            )
            response.raise_for_status()
            return GitHubUser.model_validate_json(response.content)
    except (httpx.HTTPError, ValueError, ValidationError) as error:
        # Only the type: an HTTP error's text can carry request details.
        raise GitHubOAuthError(f"GitHub answered badly: {type(error).__name__}") from None
