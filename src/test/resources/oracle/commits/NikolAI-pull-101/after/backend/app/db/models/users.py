"""Read model for a signed-in user, returned by ``GET /api/me``."""

# AI contribution: 50%

from uuid import UUID

from pydantic import BaseModel


class User(BaseModel):
    id: UUID
    github_user_id: int
    github_login: str
    display_name: str | None
    is_instructor: bool
