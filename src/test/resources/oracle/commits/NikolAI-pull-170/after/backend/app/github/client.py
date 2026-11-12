"""Sign in to GitHub as the NikolAI GitHub App (US-2), with githubkit.

A GitHub App signs a short-lived JWT with its private key, then exchanges it for a token of
one installation (the repository or organization that installed the App). githubkit does
both on the first request, so building a client never touches the network.
"""

# AI contribution: 50%

from githubkit import GitHub
from githubkit.auth import AppAuthStrategy, AppInstallationAuthStrategy
from pydantic import BaseModel

from app.core.config import ConfigurationError, Settings


class _InstallationId(BaseModel):
    """The one field read from GitHub's answer: githubkit's schema can lag behind GitHub's."""

    id: int


def _app_client(settings: Settings) -> GitHub[AppAuthStrategy]:
    """A client that acts as the App itself.

    Raises:
        ConfigurationError: if the App ID or the private key is missing.
    """
    if not settings.github_app_id:
        raise ConfigurationError("APP_ID is not set: add the GitHub App's ID to backend/.env")
    return GitHub(AppAuthStrategy(settings.github_app_id, settings.read_github_private_key()))


def installation_client(
    settings: Settings, installation_id: int
) -> GitHub[AppInstallationAuthStrategy]:
    """A client that acts as the App's installation ``installation_id``.

    Raises:
        ConfigurationError: if the App ID or the private key is missing.
    """
    app_client = _app_client(settings)
    return app_client.with_auth(app_client.auth.as_installation(installation_id))


async def repository_client(
    settings: Settings, owner: str, repo: str
) -> GitHub[AppInstallationAuthStrategy]:
    """A client for the installation on ``owner/repo``, for work no webhook started.

    NikolAI doesn't store installation IDs: webhooks carry theirs, and this asks GitHub.

    Raises:
        ConfigurationError: if the App ID or the private key is missing.
        githubkit.exception.GitHubException: the App isn't installed there, or GitHub didn't answer.
        pydantic.ValidationError: GitHub's answer has no installation ID.
    """
    app_client = _app_client(settings)
    response = await app_client.rest.apps.async_get_repo_installation(owner, repo)
    installation = _InstallationId.model_validate(response.json())
    return app_client.with_auth(app_client.auth.as_installation(installation.id))
