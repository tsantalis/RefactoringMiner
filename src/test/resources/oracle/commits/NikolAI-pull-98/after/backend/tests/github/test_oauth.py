"""GitHub's OAuth code exchange, with GitHub faked by ``httpx.MockTransport``."""

# AI contribution: 50%

import httpx
import pytest

from app.github.oauth import GitHubOAuthError, fetch_github_user


async def _sign_in_with_token_reply(body: object) -> None:
    def github(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json=body)

    await fetch_github_user(
        client_id="client-id",
        client_secret="client-secret",
        code="abc",
        transport=httpx.MockTransport(github),
    )


@pytest.mark.parametrize("code", ["bad_verification_code", "incorrect_client_credentials"])
async def test_a_refused_code_keeps_githubs_reason(code: str) -> None:
    with pytest.raises(GitHubOAuthError, match=rf"no user token: {code}$"):
        await _sign_in_with_token_reply({"error": code})


@pytest.mark.parametrize("code", ["<script>", "line\nbreak", 42, None, "a" * 65])
async def test_an_unexpected_error_value_is_not_repeated(code: object) -> None:
    with pytest.raises(GitHubOAuthError, match=r"no user token: no error code$"):
        await _sign_in_with_token_reply({"error": code})
