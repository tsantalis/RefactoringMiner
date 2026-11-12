"""Sign in to GitHub as the NikolAI GitHub App (US-2), with githubkit.

A GitHub App signs a short-lived JWT with its private key, then exchanges it for a token of
one installation (the repository or organization that installed the App). githubkit does
both on the first request, so building a client never touches the network.
"""

from githubkit import GitHub
from githubkit.auth import AppAuthStrategy, AppInstallationAuthStrategy

from app.core.config import ConfigurationError, Settings


def installation_client(
    settings: Settings, installation_id: int
) -> GitHub[AppInstallationAuthStrategy]:
    """A client that acts as the App's installation ``installation_id``.

    Raises:
        ConfigurationError: if the App ID or the private key is missing.
    """
    if not settings.github_app_id:
        raise ConfigurationError("APP_ID is not set: add the GitHub App's ID to backend/.env")
    app_client = GitHub(AppAuthStrategy(settings.github_app_id, settings.read_github_private_key()))
    return app_client.with_auth(app_client.auth.as_installation(installation_id))
