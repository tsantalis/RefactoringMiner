"""What a RefactoringMiner run saves, and how two pushes compare (US-8)."""

# AI contribution: 50%

from pathlib import Path

import pytest

from app.db.models.refactorings import StoredRefactoring
from app.services.refactoring_report import CodeLocation, Refactoring, parse_refactorings
from app.services.refactorings import compare, to_stored

SAMPLE = Path(__file__).parents[1] / "fixtures/refactoringminer/refactorings.json"


def stored(kind: str, description: str) -> StoredRefactoring:
    return StoredRefactoring(
        type=kind,
        description=description,
        file_path=None,
        start_line=None,
        end_line=None,
        side="child",
    )


def test_each_refactoring_keeps_its_head_location_and_a_clean_description() -> None:
    first, extracted, _ = to_stored(parse_refactorings(SAMPLE.read_bytes())).refactorings

    assert first.description == (
        "public test_reachable_when_the_port_accepts(listening_port) renamed to public "
        "test_reachable_when_the_database_answers(database_url) in class "
        "db.test_health.__module__"
    )
    assert (first.file_path, first.start_line, first.end_line, first.side) == (
        "backend/tests/db/test_health.py",
        12,
        16,
        "child",
    )
    # RefactoringMiner often gives no location: the refactoring is kept without one.
    assert (extracted.file_path, extracted.start_line) == (None, None)


def test_a_refactoring_only_in_the_base_points_at_the_parent_side() -> None:
    moved = Refactoring(type="Move Class", before=(CodeLocation(file_path="a/b.py", start_line=3),))

    (saved,) = to_stored([moved]).refactorings

    assert (saved.file_path, saved.start_line, saved.side) == ("a/b.py", 3, "parent")


@pytest.mark.parametrize(
    "path", ["/etc/passwd", "../outside.py", "src/../../x.py", "bad\nname.py", "x" * 1_001]
)
def test_an_unsafe_path_is_dropped(path: str) -> None:
    risky = Refactoring(type="Rename Class", after=(CodeLocation(file_path=path, start_line=0),))

    (saved,) = to_stored([risky]).refactorings

    assert (saved.file_path, saved.start_line) == (None, None)


def test_without_a_previous_push_every_refactoring_is_new() -> None:
    items = compare([stored("Extract Method", "a"), stored("Rename Method", "b")], None)

    assert [item.change for item in items] == ["new", "new"]


def test_two_pushes_mark_unchanged_new_and_undone() -> None:
    previous = [stored("Extract Method", "a"), stored("Move Class", "c")]
    latest = [stored("Extract Method", "a"), stored("Rename Method", "b")]

    items = compare(latest, previous)

    assert [(item.type, item.change) for item in items] == [
        ("Extract Method", "unchanged"),
        ("Rename Method", "new"),
        ("Move Class", "undone"),
    ]
