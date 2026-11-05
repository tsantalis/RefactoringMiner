"""A stored fix is read back only when its lines make sense; a damaged one means not fixed."""

# AI contribution: 50%

import pytest

from app.db.models.pull_insights import StoredFix, read_fix

FIX = {"commit": "2" * 40, "file_path": "a.py", "start_line": 3, "end_line": 4}


def test_a_well_formed_fix_is_read_back() -> None:
    assert read_fix(FIX) == StoredFix.model_validate(FIX)


@pytest.mark.parametrize(
    "damage",
    [
        {"start_line": 0},  # lines start at 1
        {"end_line": 0},
        {"start_line": 5},  # ends before it starts
        {"commit": None},
    ],
)
def test_a_damaged_fix_reads_as_not_fixed(damage: dict[str, object]) -> None:
    assert read_fix(FIX | damage) is None
