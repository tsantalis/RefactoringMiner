"""Read Semgrep's JSON output from a team's Semgrep workflow (US-15).

The workflow runs ``semgrep scan --json`` on the PR's head commit
(https://semgrep.dev/docs/cli-reference). The output comes from a workflow the team controls, so
it is untrusted: its shape is validated and each finding's text is capped here, and its paths
and lines only count where they match the PR's own diff.
"""

# AI contribution: 50%

from typing import Annotated, Final

from pydantic import BaseModel, Field, JsonValue, StringConstraints, ValidationError

from app.db.models.static_findings import StaticFinding

MAX_RULE_ID_CHARS: Final = 300
MAX_MESSAGE_CHARS: Final = 2_000
MAX_CATEGORY_CHARS: Final = 100


class SemgrepReportError(ValueError):
    """The file isn't Semgrep's JSON output."""


class _Position(BaseModel):
    line: int = Field(ge=1)


class _Extra(BaseModel):
    message: str
    # ERROR, WARNING or INFO, or CRITICAL to LOW in newer rules.
    severity: Annotated[str, StringConstraints(pattern=r"^[A-Za-z]{1,20}$")]
    metadata: dict[str, JsonValue] = Field(default_factory=dict)


class _Result(BaseModel):
    check_id: Annotated[str, StringConstraints(min_length=1)]
    path: Annotated[str, StringConstraints(min_length=1)]
    start: _Position
    end: _Position
    extra: _Extra


class _Output(BaseModel):
    results: list[_Result]


def parse_semgrep_findings(raw: bytes) -> list[StaticFinding]:
    """The findings in Semgrep's JSON output, in Semgrep's order.

    Raises:
        SemgrepReportError: if ``raw`` isn't Semgrep's JSON output.
    """
    try:
        results = _Output.model_validate_json(raw).results
    except ValidationError as error:
        raise SemgrepReportError("Unreadable semgrep.json") from error
    return [_finding(result) for result in results]


def _finding(result: _Result) -> StaticFinding:
    category = result.extra.metadata.get("category")  # free text, set by the rule's author
    return StaticFinding(
        rule_id=result.check_id[:MAX_RULE_ID_CHARS],
        message=result.extra.message[:MAX_MESSAGE_CHARS],
        severity=result.extra.severity.upper(),
        category=category[:MAX_CATEGORY_CHARS] if isinstance(category, str) else None,
        file_path=result.path,
        start_line=result.start.line,
        end_line=result.end.line,
    )
