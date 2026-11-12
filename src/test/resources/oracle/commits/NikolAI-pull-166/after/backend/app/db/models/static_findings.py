"""Static-checker findings on the lines a PR changed, stored per PR and head commit (US-15)."""

# AI contribution: 50%

from pydantic import BaseModel, ConfigDict

from app.db.models.commit_analyses import NewCommitAnalysis


class StaticFinding(BaseModel):
    """One finding of a static checker, in the checker's own words.

    ``severity`` is the checker's level (Semgrep's ``ERROR``, ``WARNING`` or ``INFO``, or
    ``CRITICAL`` to ``LOW``) and ``category`` its rule's category, if the rule has one: the
    code review maps both to its own vocabulary. Lines are the head commit's.
    """

    model_config = ConfigDict(frozen=True)

    rule_id: str
    message: str
    severity: str
    category: str | None
    file_path: str
    start_line: int
    end_line: int


class StoredStaticFindings(BaseModel):
    """The JSON of a run's ``static_findings`` output."""

    findings: list[StaticFinding]


class NewStaticAnalysisRun(NewCommitAnalysis):
    """What the webhook job saves after Semgrep analysed a PR's head commit."""

    findings: list[StaticFinding]
