"""The database a one-off operator command works on (``migrate``, ``register_repository``)."""

# AI contribution: 50%

from getpass import getpass

from app.core.config import ConfigurationError, Settings


def command_database_url(*, prompt: str | None) -> str:
    """The URL typed at a hidden ``prompt``, or ``DATABASE_URL`` when there is no prompt.

    Raises:
        ConfigurationError: if neither gives a URL.
    """
    if prompt is not None:
        url = getpass(prompt).strip()
    else:
        secret = Settings().database_url
        url = secret.get_secret_value().strip() if secret else ""
    if not url:
        raise ConfigurationError("Set DATABASE_URL or use --prompt-database-url")
    return url
