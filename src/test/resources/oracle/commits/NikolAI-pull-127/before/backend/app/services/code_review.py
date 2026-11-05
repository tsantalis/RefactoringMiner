"""Run the code-review agent on a PR's diff and keep only findings that check out (US-9).

The model's findings are untrusted output. NikolAI keeps a finding only when it points at
lines inside the diff the agent was shown, and only when the model is confident enough;
low-confidence findings are withheld and counted, so the checklist can say how many were
left out. Authorization and storage stay in application code.

A large PR is reviewed in batches (see ``app.services.code_review_diff``). Each batch's findings
are checked against the diff that batch showed, then all of them are merged into one checklist:
grouped by issue, ranked and capped as if they came from one call. The batches run at the
same time, each taking one of the process's model-call slots. When one batch fails, the others
are cancelled and the code review fails as a whole: a checklist missing a batch's files would
drop that code's open findings, and fix tracking (#64) would read them as fixed. The review is
then saved as ``not_run``, so the previous checklist stands.
"""

# AI contribution: 50%

import asyncio
import hashlib
import logging
from dataclasses import dataclass
from typing import Final

from app.agents.base import AgentContext, AgentResult, PullRequestRef
from app.agents.code_review import (
    MAX_FINDINGS,
    CodeReviewAgent,
    ProposedFinding,
    parse_findings,
)
from app.db.models.findings import CodeFinding
from app.providers.base import LLMProvider
from app.services.code_review_diff import ReviewableDiff, not_reviewed_count, plan_review
from app.services.model_slots import model_call_slots
from app.services.pull_request_details import PullRequestDetails

logger = logging.getLogger(__name__)

# Findings the model is less sure of than this are withheld from the checklist and counted.
MIN_CONFIDENCE: Final = 0.5

_SEVERITY_ORDER: Final = ("critical", "high", "medium", "low", "info")
# The ``not_run`` reason of an edit that left the code as it was (a new title or description).
CODE_UNCHANGED: Final = "code_unchanged"
# The ``not_run`` reason when GitHub didn't send the PR's changed files.
FILES_UNAVAILABLE: Final = "files_unavailable"


@dataclass(frozen=True, slots=True)
class CodeReview:
    """The outcome of one code review, ready to be saved with the review output."""

    # One per batch; empty when the PR had no reviewable change, so the model wasn't called.
    results: tuple[AgentResult, ...]
    findings: list[CodeFinding]
    withheld_count: int
    # Findings left out for another reason: invalid fields, outside the diff, or over the cap.
    dropped: dict[str, int]
    # Changed files that weren't reviewed, with the reason.
    skipped_files: dict[str, str]
    unfetched_files: int

    @property
    def not_reviewed_count(self) -> int:
        """Every changed file that wasn't reviewed, named or not."""
        return not_reviewed_count(self.skipped_files, self.unfetched_files)

    def metadata(self) -> dict[str, object]:
        """What's stored beside the findings, in the review output's ``metadata``."""
        return {
            **_agent_metadata(),
            "results": [result.model_dump(mode="json") for result in self.results],
            "batches": len(self.results),
            "withheld_count": self.withheld_count,
            "min_confidence": MIN_CONFIDENCE,
            "dropped": self.dropped,
            "skipped_files": self.skipped_files,
            "unfetched_files": self.unfetched_files,
            "not_reviewed_count": self.not_reviewed_count,
        }


@dataclass(frozen=True, slots=True)
class _BatchReview:
    """One batch's reply: the findings inside its diff, and how many weren't."""

    result: AgentResult
    inside: list[ProposedFinding]
    invalid_count: int
    outside_count: int


def not_run_metadata(reason: str) -> dict[str, object]:
    """The review output's ``metadata`` when the code review didn't run with this review.

    ``reason`` is ``CODE_UNCHANGED``, ``FILES_UNAVAILABLE`` or the type of the model error
    that stopped it. The title and description review is still saved and published, and the
    read API keeps showing the findings of the latest review whose code review did run.
    """
    return {**_agent_metadata(), "not_run": reason}


def _agent_metadata() -> dict[str, object]:
    return {"agent": CodeReviewAgent.name, "prompt_version": CodeReviewAgent.prompt_version}


async def review_code(
    details: PullRequestDetails, pull_request: PullRequestRef, provider: LLMProvider
) -> CodeReview:
    """Review the PR's changed code, a batch per model call, and return the findings worth showing.

    Raises:
        LLMError: a batch's model call failed, or its reply isn't the expected JSON shape.
    """
    plan = plan_review(details)
    try:
        async with asyncio.TaskGroup() as group:
            tasks = [
                group.create_task(_review_batch(batch, pull_request, provider))
                for batch in plan.batches
            ]
    except ExceptionGroup as failed:
        # The first failure cancelled the other batches, so it's the one that matters; raised
        # alone, an LLMError is recorded as ``not_run`` like a one-call review's.
        raise failed.exceptions[0] from None
    batches = [task.result() for task in tasks]
    review = _merge(batches, plan.skipped_files, plan.unfetched_files)
    logger.info(
        "Code review of %s#%d in %d batches: %d findings kept, %d withheld, %d files not "
        "reviewed, dropped %s",
        pull_request.repository,
        pull_request.number,
        len(batches),
        len(review.findings),
        review.withheld_count,
        review.not_reviewed_count,
        review.dropped,
    )
    return review


def _merge(batches: list[_BatchReview], skipped: dict[str, str], unfetched: int) -> CodeReview:
    """One checklist from every batch: grouped by issue, ranked, then capped."""
    inside = [finding for batch in batches for finding in batch.inside]
    issues = _group(sorted(inside, key=_importance))
    confident = [
        (key, group) for key, group in issues.items() if group[0].confidence >= MIN_CONFIDENCE
    ]
    return CodeReview(
        results=tuple(batch.result for batch in batches),
        findings=[
            _code_finding(key, group, rank)
            for rank, (key, group) in enumerate(confident[:MAX_FINDINGS])
        ],
        withheld_count=len(issues) - len(confident),
        dropped={
            "invalid": sum(batch.invalid_count for batch in batches),
            "outside_diff": sum(batch.outside_count for batch in batches),
            "duplicate": len(inside) - sum(len(_lines(group)) for group in issues.values()),
            "over_limit": max(0, len(confident) - MAX_FINDINGS),
        },
        skipped_files=skipped,
        unfetched_files=unfetched,
    )


async def _review_batch(
    batch: ReviewableDiff, pull_request: PullRequestRef, provider: LLMProvider
) -> _BatchReview:
    """One model call, keeping the findings that point inside the diff it showed."""
    context = AgentContext(prompt=batch.prompt, pull_request=pull_request)
    async with model_call_slots():
        result = await CodeReviewAgent(provider).run(context)
    parsed = parse_findings(result.content)
    inside = [f for f in parsed.findings if batch.contains(f.file_path, f.start_line, f.end_line)]
    logger.info(
        "Code review batch of %s#%d with %s in %.0f ms: %d of %d findings inside its diff",
        pull_request.repository,
        pull_request.number,
        result.model,
        result.latency_ms,
        len(inside),
        len(parsed.findings),
    )
    return _BatchReview(result, inside, parsed.invalid_count, len(parsed.findings) - len(inside))


def _importance(finding: ProposedFinding) -> tuple[int, float]:
    return _SEVERITY_ORDER.index(finding.severity), -finding.confidence


def _group(findings: list[ProposedFinding]) -> dict[str, list[ProposedFinding]]:
    """Group the findings that name the same issue in the same file, keeping their order."""
    issues: dict[str, list[ProposedFinding]] = {}
    for finding in findings:
        issues.setdefault(_finding_key(finding), []).append(finding)
    return issues


def _lines(group: list[ProposedFinding]) -> tuple[tuple[int, int], ...]:
    """The distinct line ranges an issue was reported at: an exact repeat counts once."""
    return tuple(sorted({(finding.start_line, finding.end_line) for finding in group}))


def _code_finding(key: str, group: list[ProposedFinding], rank: int) -> CodeFinding:
    """One finding per issue, told by its most important report, at every place reported."""
    first = group[0]
    return CodeFinding(
        key=key,
        name=first.name,
        description=first.description,
        severity=first.severity,
        category=first.category,
        file_path=first.file_path,
        lines=_lines(group),
        confidence=first.confidence,
        rank=rank,
    )


def _finding_key(finding: ProposedFinding) -> str:
    """Category, path and name only: lines move between pushes, the issue doesn't."""
    name = " ".join(finding.name.casefold().split())
    identity = "\n".join((finding.category, finding.file_path, name))
    return hashlib.sha256(identity.encode()).hexdigest()[:16]
