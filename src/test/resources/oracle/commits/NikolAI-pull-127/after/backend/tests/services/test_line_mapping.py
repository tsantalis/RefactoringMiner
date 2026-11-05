"""A line of a file follows the later commits of a push to its number in the PR's head (US-9.4)."""

# AI contribution: 50%

import pytest

from app.services.line_mapping import map_line

# Line 2 becomes "B", and two lines are inserted after line 5.
PATCH = "@@ -1,3 +1,3 @@\n a\n-b\n+B\n c\n@@ -5,0 +6,2 @@\n+new1\n+new2"


@pytest.mark.parametrize(
    ("line", "expected"),
    [
        (1, 1),  # unchanged context before the change
        (3, 3),  # unchanged context after it
        (4, 4),  # between the hunks
        (5, 5),  # the line the insertion follows
        (6, 8),  # pushed down by the two inserted lines
        (40, 42),
    ],
)
def test_an_unchanged_line_moves_with_the_lines_added_before_it(line: int, expected: int) -> None:
    assert map_line(PATCH, line) == expected


def test_a_changed_line_has_no_number_in_the_later_version() -> None:
    assert map_line(PATCH, 2) is None


def test_removed_lines_move_the_lines_after_them_up() -> None:
    patch = "@@ -2,3 +2 @@\n-x\n-y\n z"

    assert map_line(patch, 1) == 1
    assert map_line(patch, 3) is None
    assert map_line(patch, 4) == 2
    assert map_line(patch, 10) == 8
