"""Join codes and roster rows (ADR 0001, decisions 5 and 8)."""

# AI contribution: 50%

import pytest
from psycopg.errors import UniqueViolation

from app.services import courses
from app.services.courses import (
    JOIN_CODE_ALPHABET,
    JoinCodeExhaustedError,
    new_join_code,
    normalize_join_code,
    parse_roster_row,
    replace_join_code,
)


def test_a_join_code_has_eight_unambiguous_characters() -> None:
    codes = {new_join_code() for _ in range(200)}

    assert all(len(code) == 8 and set(code) <= set(JOIN_CODE_ALPHABET) for code in codes)
    assert not set("0O1IL") & set(JOIN_CODE_ALPHABET)
    assert len(codes) > 190


def test_a_typed_code_ignores_case_spaces_and_dashes() -> None:
    assert normalize_join_code(" abcd-2345\t") == "ABCD2345"


def test_a_roster_row_defaults_to_a_student_without_a_team() -> None:
    row = parse_roster_row({"github_login": " Octo-Cat "})

    assert (row.github_login, row.role, row.team) == ("Octo-Cat", "student", None)


@pytest.mark.parametrize(
    ("raw", "reason"),
    [
        ({}, "github_login"),
        ({"github_login": "-bad"}, "github_login"),
        ({"github_login": "a" * 40}, "github_login"),
        ({"github_login": "ok", "role": "admin"}, "role"),
        ({"github_login": "ok", "team": "  "}, "team"),
        ({"github_login": "ok", "team": 3}, "team"),
    ],
)
def test_a_bad_roster_row_says_which_field_is_wrong(raw: dict[str, object], reason: str) -> None:
    with pytest.raises(ValueError, match=reason):
        parse_roster_row(raw)


async def test_a_used_join_code_is_retried(monkeypatch: pytest.MonkeyPatch) -> None:
    tried: list[str] = []
    codes = iter(["TAKEN234", "FRESH234"])
    monkeypatch.setattr(courses, "new_join_code", lambda: next(codes))

    async def rotate(connection: object, course_id: object, code: str) -> None:
        tried.append(code)
        if code == "TAKEN234":
            raise UniqueViolation("duplicate join code")

    monkeypatch.setattr(courses, "rotate_join_code", rotate)

    assert await replace_join_code(None, None) == "FRESH234"  # type: ignore[arg-type]
    assert tried == ["TAKEN234", "FRESH234"]


async def test_join_code_generation_gives_up_after_repeated_collisions(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    async def always_taken(connection: object, course_id: object, code: str) -> None:
        raise UniqueViolation("duplicate join code")

    monkeypatch.setattr(courses, "rotate_join_code", always_taken)

    with pytest.raises(JoinCodeExhaustedError):
        await replace_join_code(None, None)  # type: ignore[arg-type]
