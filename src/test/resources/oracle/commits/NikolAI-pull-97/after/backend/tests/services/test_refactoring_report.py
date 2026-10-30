"""Tests for reading RefactoringMiner's refactorings.json and rendering NikolAI's report (US-8)."""

# AI contribution: 50% or more AI-generated

from pathlib import Path

import pytest

from app.github.comments import MAX_COMMENT_CHARS
from app.services.refactoring_report import (
    REPORT_MARKER,
    Refactoring,
    RefactoringReportError,
    parse_refactorings,
    render_report,
)

SAMPLE = Path(__file__).parents[1] / "fixtures/refactoringminer/refactorings.json"


def test_parses_the_refactorings_of_a_real_export() -> None:
    refactorings = parse_refactorings(SAMPLE.read_bytes())

    assert [r.type for r in refactorings] == ["Rename Method", "Extract Method", "Rename Method"]
    assert refactorings[1].markup.startswith("**Extract Method** [private _neutralize_tags(text)]")


@pytest.mark.parametrize("raw", [b"not json", b'{"refactorings": "nope"}', b'[{"type": 1}]'])
def test_unreadable_output_is_a_report_error(raw: bytes) -> None:
    with pytest.raises(RefactoringReportError):
        parse_refactorings(raw)


PR_URL = "https://github.com/octocat/demo/pull/7"
SHA = "0123456789abcdef0123456789abcdef01234567"


def link(anchor: str) -> str:
    return f"{PR_URL}/changes?diff=split#{anchor}"


def test_the_report_groups_refactorings_by_type_most_frequent_first() -> None:
    refactorings = [
        Refactoring(type="Extract Method", markup=f"**Extract Method** [b()]({link('R2')}) from x"),
        Refactoring(type="Rename Method", markup=f"**Rename Method** [a()]({link('L1')}) to c"),
        Refactoring(type="Rename Method", markup=f"**Rename Method** [d()]({link('L3')}) to e"),
    ]

    assert render_report(refactorings, PR_URL, SHA) == (
        f"{REPORT_MARKER}\n### NikolAI refactoring report\n\n"
        "Found 3 refactorings: 2 Rename Method, 1 Extract Method\n\n"
        "#### Rename Method (2)\n\n"
        f"- [a()]({link('L1')}) to c\n"
        f"- [d()]({link('L3')}) to e\n\n"
        "#### Extract Method (1)\n\n"
        f"- [b()]({link('R2')}) from x\n\n"
        "<sub>Detected by RefactoringMiner on commit 0123456 · "
        "each link opens the lines in Files changed</sub>"
    )


def item_line(refactoring: Refactoring) -> str:
    """The rendered bullet of a one-refactoring report."""
    report = render_report([refactoring], PR_URL, SHA)
    return next(line for line in report.splitlines() if line.startswith("- "))


def test_python_names_keep_their_underscores_but_code_and_urls_are_untouched() -> None:
    markup = f"**Rename Method** [__init__(self)]({link('L1_x')}) to run_all in `pkg.__module__`"

    assert item_line(Refactoring(type="Rename Method", markup=markup)) == (
        f"- [\\_\\_init\\_\\_(self)]({link('L1_x')}) to run\\_all in `pkg.__module__`"
    )


def test_links_outside_this_pull_request_lose_their_url() -> None:
    markup = "**Move Class** [Evil](https://example.com/phish) moved to [Ok](https://github.com/octocat/demo/pull/70/x)"

    assert item_line(Refactoring(type="Move Class", markup=markup)) == "- Evil moved to Ok"


def test_html_and_line_breaks_cannot_escape_the_bullet() -> None:
    markup = "**Rename Class** <img src=x> A\n\n# heading <!-- nikolai:refactorings -->"

    assert item_line(Refactoring(type="Rename Class", markup=markup)) == (
        "- &lt;img src=x&gt; A # heading &lt;!-- nikolai:refactorings --&gt;"
    )


def test_without_markup_the_plain_description_is_used() -> None:
    refactoring = Refactoring(type="Rename Variable", description="Rename Variable\tmy_var to x")

    assert item_line(refactoring) == "- my\\_var to x"


def test_a_change_without_refactorings_says_so() -> None:
    report = render_report([], PR_URL, SHA)

    assert report.startswith(f"{REPORT_MARKER}\n### NikolAI refactoring report\n\n")
    assert "No refactorings detected in this pull request." in report
    assert "####" not in report


def test_a_huge_report_fits_in_one_github_comment_and_keeps_the_full_count() -> None:
    long_name = "x" * 400
    refactorings = [
        Refactoring(type="Rename Variable", markup=f"[{long_name}{n}]({link(f'L{n}')}) to y")
        for n in range(1_000)
    ]

    report = render_report(refactorings, PR_URL, SHA)

    assert len(report) <= MAX_COMMENT_CHARS
    assert "Found 1000 refactorings: 1000 Rename Variable" in report
    listed = report.count("\n- ")
    assert 0 < listed < 1_000
    assert f"{1_000 - listed} more refactorings are not listed" in report
