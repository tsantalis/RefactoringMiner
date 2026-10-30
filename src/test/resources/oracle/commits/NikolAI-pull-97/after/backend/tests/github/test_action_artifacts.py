"""Tests for reading refactorings.json out of the RefactoringMiner Action's artifacts (US-8)."""

# AI contribution: 50% or more AI-generated

import io
import tarfile
import zipfile

import pytest

from app.github import action_artifacts
from app.github.action_artifacts import ArtifactError, read_refactorings_json

REPORT = b'{"refactorings": []}'


def zipped(files: dict[str, bytes]) -> io.BytesIO:
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as archive:
        for name, content in files.items():
            archive.writestr(name, content)
    buffer.seek(0)
    return buffer


def tarred(files: dict[str, bytes]) -> bytes:
    buffer = io.BytesIO()
    with tarfile.open(fileobj=buffer, mode="w") as archive:
        for name, content in files.items():
            info = tarfile.TarInfo(name)
            info.size = len(content)
            archive.addfile(info, io.BytesIO(content))
    return buffer.getvalue()


def test_reads_the_report_of_a_private_repository_artifact() -> None:
    # refactoring-diff: the exported web view, with refactorings.json at its root.
    artifact = zipped({"list/index.html": b"<html>", "refactorings.json": REPORT})

    assert read_refactorings_json(artifact, pr_number=7) == REPORT


def test_reads_this_pull_requests_report_from_the_pages_site() -> None:
    # github-pages: one artifact.tar holding every open PR's view.
    site = tarred(
        {
            "./index.html": b"<html>",
            "./refactorings/pr-70/refactorings.json": b'{"refactorings": [{"type": "x"}]}',
            "./refactorings/pr-7/refactorings.json": REPORT,
        }
    )

    assert read_refactorings_json(zipped({"artifact.tar": site}), pr_number=7) == REPORT


@pytest.mark.parametrize(
    "files",
    [
        {"unrelated.txt": b"hi"},
        # A closed PR's run removes its folder from the site.
        {"artifact.tar": tarred({"./refactorings/pr-8/refactorings.json": REPORT})},
    ],
)
def test_an_artifact_without_this_pull_requests_report_has_none(files: dict[str, bytes]) -> None:
    assert read_refactorings_json(zipped(files), pr_number=7) is None


def test_an_oversized_report_is_refused(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(action_artifacts, "MAX_REPORT_BYTES", 10)

    with pytest.raises(ArtifactError):
        read_refactorings_json(zipped({"refactorings.json": REPORT}), pr_number=7)


def test_a_corrupt_artifact_is_an_artifact_error() -> None:
    with pytest.raises(ArtifactError):
        read_refactorings_json(io.BytesIO(b"not a zip"), pr_number=7)
