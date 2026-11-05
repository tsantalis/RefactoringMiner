"""Backend settings, read from environment variables and ``backend/.env``.

Secrets such as the OpenRouter API key, the GitHub App's webhook secret and its private key
only ever come from the environment or local files. They must never be hard-coded or
committed: ``.env`` and ``*.pem`` are gitignored, and ``.env.example`` documents them.
"""

from functools import lru_cache
from pathlib import Path
from typing import Annotated, Literal

from pydantic import AliasChoices, Field, SecretStr, ValidationInfo, field_validator
from pydantic_settings import BaseSettings, NoDecode, SettingsConfigDict

from app.providers.base import ReasoningEffort

# The backend/ folder, so `.env` is found whichever directory the app is started from.
BACKEND_DIR = Path(__file__).resolve().parents[2]


class ConfigurationError(RuntimeError):
    """A setting the backend needs is missing, for example the GitHub App's private key."""


class Settings(BaseSettings):
    """Typed backend configuration. Each field can be set by the env var of the same name."""

    model_config = SettingsConfigDict(
        env_file=BACKEND_DIR / ".env",
        env_file_encoding="utf-8",
        extra="ignore",
        # Fields with two env names (e.g. APP_ID or GITHUB_APP_ID) still accept their field
        # name in code, which also lets mypy type-check Settings(...) calls.
        validate_by_name=True,
    )

    # Compose sets DATABASE_URL when a PostgreSQL instance is attached. None means "no
    # database configured", in which case /health/ready reports "not_configured".
    database_url: SecretStr | None = None
    review_max_attempts: int = Field(default=3, ge=1, le=10)
    review_retry_delay_seconds: float = Field(default=60, ge=0)
    review_poll_seconds: float = Field(default=15, gt=0)
    review_job_timeout_seconds: float = Field(default=900, gt=0)

    openrouter_api_key: SecretStr | None = None
    openrouter_base_url: str = "https://openrouter.ai/api/v1"
    # Free ($0) coding model whose host does not train on prompts, chosen 2026-10-05: the
    # free Qwen3.8 27B was dropped, and the free Gemma 4 models share a quota that is often
    # used up. Paid alternatives, switched with OPENROUTER_MODEL: "qwen/qwen3.8-27b"
    # (stronger coder) or "anthropic/claude-haiku-4.5".
    openrouter_model: str = "cohere/north-mini-code:free"
    # Backup models, tried in this order when the main model still fails after its retries
    # (rate limit, removed, or down). Set OPENROUTER_FALLBACK_MODELS to a comma-separated
    # list, e.g. "a/model:free,b/model:free"; an empty value means no fallback. Blanks and
    # repeats are dropped, and so is the main model if it is listed. Changing the list needs
    # no code change. Xing4.0 (xingchen-agi) is free, answered with OPENROUTER_DATA_COLLECTION
    # on "deny" (host: Phala), and comes from a different vendor than the main model. It is
    # slow (~15-20 tokens/s) and sometimes returns 429, so it is a best-effort backup.
    # Check any model you add with a real PR diff, and that its host is allowed by
    # OPENROUTER_DATA_COLLECTION below, before relying on it. Rationale: wiki, LLM Platform.
    openrouter_fallback_models: Annotated[list[str], NoDecode] = [
        "xingchen-agi/xing4.0-29b-a4b:free",
    ]
    # Unset keeps the model's own default (North Mini Code thinks briefly, Claude does not
    # think unless asked, paid Qwen3.8 thinks at "xhigh"). Lower it to cut latency.
    openrouter_reasoning_effort: ReasoningEffort | None = None
    # Reasoning effort for the backup models only; the main model keeps the setting above.
    # Set OPENROUTER_FALLBACK_REASONING_EFFORT (none | minimal | low | medium | high | xhigh |
    # max) in .env or the host's variables. Backups run after the main model has already
    # failed, so "none" keeps them fast (Xing4.0: ~8 s instead of ~67 s with its default
    # thinking). Unset means backups use the same effort as the main model.
    openrouter_fallback_reasoning_effort: ReasoningEffort | None = None
    # Reasoning models can think for a while before answering, hence a generous timeout.
    openrouter_timeout_seconds: float = Field(default=120.0, gt=0)
    openrouter_max_retries: int = Field(default=2, ge=0, le=5)
    # "deny" only routes to providers that do not train on prompts (student code). Whether
    # they keep prompts is OpenRouter's separate Zero Data Retention setting.
    openrouter_data_collection: Literal["allow", "deny"] = "deny"
    # Optional attribution headers that label NikolAI's traffic on openrouter.ai.
    openrouter_app_url: str = "https://github.com/Pogut/NikolAI"
    openrouter_app_title: str = "NikolAI"

    # ---- GitHub App (US-2) --------------------------------------------------------------
    # All optional, so the app and the tests start without them. The names match Team Alpha's
    # deployment (APP_ID, WEBHOOK_SECRET); the GITHUB_-prefixed names work too.
    github_app_id: str | None = Field(
        default=None, validation_alias=AliasChoices("GITHUB_APP_ID", "APP_ID")
    )
    github_webhook_secret: SecretStr | None = Field(
        default=None, validation_alias=AliasChoices("GITHUB_WEBHOOK_SECRET", "WEBHOOK_SECRET")
    )
    # Hosts such as Railway pass the PEM key in one env var (GITHUB_PRIVATE_KEY) with literal
    # "\n" for its line breaks. Locally the key usually lives in a file instead.
    github_private_key: SecretStr | None = None
    github_private_key_path: Path = BACKEND_DIR / "private-key.pem"
    # Where the webhook keeps its Markdown log of each PR (Team Alpha's US-2 behaviour).
    pr_log_dir: Path = BACKEND_DIR / "logs"

    # Production servers set NIKOLAI_ENV=production, which makes the checks below enforce
    # that every required setting is present before startup.
    nikolai_env: Literal["development", "production"] = "development"

    @field_validator("openrouter_fallback_models", mode="before")
    @classmethod
    def _parse_fallback_models(cls, value: object, info: ValidationInfo) -> object:
        """Accept ``"a,b"`` or a list; trim, drop blanks and repeats, and drop the main model."""
        if isinstance(value, str):
            value = value.split(",")
        if not isinstance(value, (list, tuple)):
            return value
        main_model = info.data.get("openrouter_model")
        models: list[str] = []
        for item in value:
            name = str(item).strip()
            if name and name != main_model and name not in models:
                models.append(name)
        return models

    @field_validator("github_private_key", mode="before")
    @classmethod
    def _restore_key_line_breaks(cls, value: object) -> object:
        """Turn the literal ``\\n`` sequences of a one-line PEM key back into line breaks."""
        if isinstance(value, str):
            return value.replace("\\n", "\n")
        return value

    def read_github_private_key(self) -> str:
        """Return the GitHub App's private key: ``GITHUB_PRIVATE_KEY`` first, then the file.

        Raises:
            ConfigurationError: if neither holds a key.
        """
        if self.github_private_key is not None:
            key = self.github_private_key.get_secret_value()
            if key.strip():
                return key
        try:
            key = self.github_private_key_path.read_text(encoding="utf-8")
        except OSError, UnicodeError:
            raise ConfigurationError(
                "No readable GitHub App private key: set GITHUB_PRIVATE_KEY or save a "
                f"readable key as {self.github_private_key_path}"
            ) from None
        if not key.strip():
            raise ConfigurationError(
                "The GitHub App private key is empty: set GITHUB_PRIVATE_KEY or save the key "
                f"as {self.github_private_key_path}"
            )
        return key

    def missing_required_settings(self) -> list[str]:
        """Return the production settings that are missing or blank, in a stable order.

        ``GITHUB_PRIVATE_KEY`` counts as present when the env/secret is non-blank *or* the
        key file is readable and non-blank, mirroring ``read_github_private_key``'s fallback.
        """
        missing: list[str] = []

        openrouter_key = (
            self.openrouter_api_key.get_secret_value() if self.openrouter_api_key else ""
        )
        if not openrouter_key.strip():
            missing.append("OPENROUTER_API_KEY")

        database_url = self.database_url.get_secret_value() if self.database_url else ""
        if not database_url.strip():
            missing.append("DATABASE_URL")

        if not (self.github_app_id or "").strip():
            missing.append("APP_ID")

        webhook_secret = (
            self.github_webhook_secret.get_secret_value() if self.github_webhook_secret else ""
        )
        if not webhook_secret.strip():
            missing.append("WEBHOOK_SECRET")

        try:
            self.read_github_private_key()
        except ConfigurationError:
            missing.append("GITHUB_PRIVATE_KEY")

        return missing

    def require_production_ready(self) -> None:
        """Raise if production settings are missing; a no-op outside production."""
        if self.nikolai_env != "production":
            return
        missing = self.missing_required_settings()
        if missing:
            raise ConfigurationError(
                "Production startup aborted: missing or blank settings: " + ", ".join(missing)
            )


@lru_cache
def get_settings() -> Settings:
    """Return the process-wide settings (cached, and ready to use as a FastAPI dependency)."""
    return Settings()
