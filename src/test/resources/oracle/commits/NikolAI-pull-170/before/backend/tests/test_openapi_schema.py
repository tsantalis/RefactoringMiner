"""Tests for the OpenAPI dump that ``@nikolai/api-client`` is generated from."""

# AI contribution: 50%

import json
from pathlib import Path

import pytest

from app.main import app
from app.openapi_schema import main, openapi_json

READ_ENDPOINTS = [
    "/api/repos/{owner}/{repo}/pulls",
    "/api/repos/{owner}/{repo}/pulls/{number}",
    "/api/repos/{owner}/{repo}/pulls/{number}/refactorings",
    "/api/repos/{owner}/{repo}/pulls/{number}/insights",
    "/api/repos/{owner}/{repo}/team",
    "/api/findings/{finding_id}/actions",
]


def test_schema_lists_the_endpoints_the_extension_reads() -> None:
    schema = json.loads(openapi_json())

    assert set(READ_ENDPOINTS) <= set(schema["paths"])
    for name in ["PullRequestFeedback", "PullRequestRefactorings", "PullInsights", "TeamOverview"]:
        assert name in schema["components"]["schemas"]


def test_schema_names_are_unique_so_the_generated_types_are_readable() -> None:
    # FastAPI falls back to module-qualified names when two models share a class name.
    names = json.loads(openapi_json())["components"]["schemas"]

    assert not [name for name in names if name.startswith("app__")]


def test_schema_is_the_same_on_every_run(monkeypatch: pytest.MonkeyPatch) -> None:
    first = openapi_json()
    # FastAPI caches the schema after the first call: rebuild it, as a fresh process would.
    monkeypatch.setattr(app, "openapi_schema", None)

    assert openapi_json() == first


def test_main_writes_the_schema_to_a_file(tmp_path: Path) -> None:
    output = tmp_path / "openapi.json"

    assert main([str(output)]) == 0
    # Byte-for-byte: UTF-8 with "\n" line endings on every OS, Windows included.
    assert output.read_bytes() == openapi_json().encode("utf-8")


def test_main_prints_the_schema_without_a_path(capsys: pytest.CaptureFixture[str]) -> None:
    assert main([]) == 0
    assert capsys.readouterr().out == openapi_json()


def test_main_reports_a_path_it_cannot_write(
    tmp_path: Path, capsys: pytest.CaptureFixture[str]
) -> None:
    assert main([str(tmp_path / "missing" / "openapi.json")]) == 1
    assert "Can't write the OpenAPI schema" in capsys.readouterr().err
