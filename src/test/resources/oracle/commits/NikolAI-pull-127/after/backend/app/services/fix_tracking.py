"""Tick the checklist items a new push fixed, with evidence, and add its new issues (US-9.4).

When a student pushes to a PR that already has a checklist, NikolAI reviews the push instead of
the whole PR again:

1. the push is every commit from the head the checklist was made for to the new head
   (``app.services.push_diff``); the fix-check agent sees the open items and each commit's diff;
2. an item is ticked only when the agent cites a commit of the push, and a file and lines
   inside that commit's diff. The citation is untrusted model output, checked here in code;
3. new issues must point inside a commit's diff too, and are moved to the head's line numbers
   (``PushPlan.to_head``) so the checklist links to the code as it is now;
4. every item of the previous checklist is carried into the new review: the same key, a link to
   the finding it started as, and its fix if it has one, so a student's acknowledgement or
   dispute stays with it (the PR insights follow an item's status by its key). A fixed issue
   the push brings back (the same key) reopens its item.

The new review's checklist lists the open items first, in their old order, then the new ones,
then the fixed ones. A push that doesn't build on the checklist's head (a force push) can't be
followed commit by commit, so it gets a full code review instead (``None`` here).
"""

# AI contribution: 50%

import dataclasses
import html
import logging
from collections.abc import Awaitable, Callable
from dataclasses import dataclass
from functools import partial
from typing import Final
from uuid import UUID

from githubkit import GitHub
from githubkit.auth import AppInstallationAuthStrategy
from githubkit.exception import GitHubException

from app.agents.base import AgentContext, AgentResult, PullRequestRef
from app.agents.fix_check import (
    FixCheckAgent,
    ParsedPushReply,
    ProposedPushFinding,
    parse_push_reply,
)
from app.agents.untrusted import neutralize_tags
from app.db import connect
from app.db.models.findings import CodeFinding
from app.db.models.pull_insights import StoredFix
from app.db.repositories.checklist_items import PreviousChecklist, StoredItem, previous_checklist
from app.github.payloads import PullRequestEvent
from app.providers.base import LLMProvider
from app.services.code_review import (
    CODE_UNCHANGED,
    FILES_UNAVAILABLE,
    MIN_CONFIDENCE,
    finding_key,
    select_findings,
)
from app.services.model_slots import model_call_slots
from app.services.push_diff import Push, PushPlan, fetch_push, plan_push

logger = logging.getLogger(__name__)

# Pushes are what ``synchronize`` events bring.
PUSH_ACTION: Final = "synchronize"


@dataclass(frozen=True, slots=True)
class PushReview:
    """The outcome of a push review, ready to be saved like a code review."""

    # None when the push had no reviewable change, so the model wasn't called.
    result: AgentResult | None
    findings: list[CodeFinding]
    withheld_count: int
    dropped: dict[str, int]
    skipped_files: dict[str, str]
    push: Push

    def metadata(self) -> dict[str, object]:
        """What's stored beside the findings, in the review output's ``metadata``."""
        results = [] if self.result is None else [self.result.model_dump(mode="json")]
        return {
            "agent": FixCheckAgent.name,
            "prompt_version": FixCheckAgent.prompt_version,
            "results": results,
            "batches": len(results),
            "withheld_count": self.withheld_count,
            "min_confidence": MIN_CONFIDENCE,
            "dropped": self.dropped,
            "skipped_files": self.skipped_files,
            "unfetched_files": 0,
            "not_reviewed_count": len(self.skipped_files),
            "push": {
                "base": self.push.base,
                "head": self.push.head,
                "commits": [commit.sha for commit in self.push.commits],
                "unshown_commits": self.push.unshown_commits,
            },
        }


# Runs ``review_push`` for one saved review, as ``push_lookup`` binds it.
type PushLookup = Callable[[], Awaitable[PushReview | str | None]]


def push_lookup(
    github: GitHub[AppInstallationAuthStrategy],
    persisted_run: tuple[str, UUID] | None,
    event: PullRequestEvent,
    pull_request: PullRequestRef,
    provider: LLMProvider,
) -> PushLookup | None:
    """The push review of a saved review; an unsaved one has no checklist to follow."""
    if persisted_run is None:
        return None
    return partial(review_push, github, *persisted_run, event, pull_request, provider)


async def review_push(
    github: GitHub[AppInstallationAuthStrategy],
    database_url: str,
    run_id: UUID,
    event: PullRequestEvent,
    pull_request: PullRequestRef,
    provider: LLMProvider,
) -> PushReview | str | None:
    """Review a push against the PR's checklist: a ``PushReview``, the ``not_run`` reason when
    the checklist stands as it is, or None when the PR needs a full code review instead.

    Raises:
        LLMError: the model call failed, or its reply isn't the expected JSON shape.
    """
    if event.action != PUSH_ACTION:
        return None
    if event.pull_request.head is None:
        # Without the new head there is no push to follow: review the PR in full instead.
        logger.warning(
            "Push to %s#%d without the PR's head; reviewing it in full",
            pull_request.repository,
            pull_request.number,
        )
        return None
    head = event.pull_request.head.sha
    async with await connect(database_url) as connection:
        previous = await previous_checklist(connection, run_id)
    if previous is None or previous.head_sha is None:
        return None
    if previous.head_sha == head:
        return CODE_UNCHANGED
    owner, repo = event.repository.owner.login, event.repository.name
    try:
        # From the checklist's head, not the event's ``before``: a late or retried event's
        # ``before`` may not be the head the checklist was made for.
        push = await fetch_push(github, owner, repo, previous.head_sha, head)
    except GitHubException as error:
        logger.error(
            "Could not read the push to %s#%d: %s",
            pull_request.repository,
            pull_request.number,
            type(error).__name__,
        )
        return FILES_UNAVAILABLE
    if push is None:
        return None
    if not push.commits:
        return CODE_UNCHANGED
    return await _review(plan_push(push), previous, event, pull_request, provider)


async def _review(
    plan: PushPlan,
    previous: PreviousChecklist,
    event: PullRequestEvent,
    pull_request: PullRequestRef,
    provider: LLMProvider,
) -> PushReview:
    """Ask the agent about the push, then keep only what checks out."""
    open_items = [item for item in previous.items if item.finding.fixed_in is None]
    result, reply = None, ParsedPushReply(fixes=[], findings=[], invalid_count=0)
    if any(plan.changed_lines.values()):
        context = AgentContext(prompt=_prompt(plan, open_items, event), pull_request=pull_request)
        async with model_call_slots():
            result = await FixCheckAgent(provider).run(context)
        reply = parse_push_reply(result.content)
    fixes = _checked_fixes(plan, open_items, reply)
    new, dropped = _new_findings(plan, previous, reply)
    dropped |= {"invalid": reply.invalid_count, "rejected_fixes": len(reply.fixes) - len(fixes)}
    logger.info(
        "Push review of %s#%d: %d of %d open items fixed, %d new findings, %d reopened, dropped %s",
        pull_request.repository,
        pull_request.number,
        len(fixes),
        len(open_items),
        len(new.findings),
        len(new.reopened),
        dropped,
    )
    return PushReview(
        result=result,
        findings=_checklist(previous.items, fixes, new),
        withheld_count=previous.withheld_count + new.withheld_count,
        dropped=dropped,
        skipped_files=plan.skipped_files,
        push=plan.push,
    )


def _checked_fixes(
    plan: PushPlan, open_items: list[StoredItem], reply: ParsedPushReply
) -> dict[UUID, StoredFix]:
    """The cited fixes that check out, by item: a known open item, a commit of the push, and
    lines inside that commit's diff. The first valid citation of an item wins."""
    fixes: dict[UUID, StoredFix] = {}
    for fix in reply.fixes:
        sha = plan.resolve(fix.commit)
        if fix.item > len(open_items) or sha is None:
            continue
        item = open_items[fix.item - 1]
        cited = StoredFix(
            commit=sha, file_path=fix.file_path, start_line=fix.start_line, end_line=fix.end_line
        )
        if item.id not in fixes and plan.contains(cited):
            fixes[item.id] = cited
    return fixes


@dataclass(frozen=True, slots=True)
class _NewFindings:
    findings: list[CodeFinding]
    # Issues a push fixed before that came back, by key: their items reopen at these reports.
    reopened: dict[str, CodeFinding]
    withheld_count: int


def _new_findings(
    plan: PushPlan, previous: PreviousChecklist, reply: ParsedPushReply
) -> tuple[_NewFindings, dict[str, int]]:
    """The push's new findings inside a commit's diff, at the head's lines, that the checklist
    doesn't already have open; and how many were left out, by reason.

    A finding with a fixed item's key is that issue coming back, so it reopens the item rather
    than adding a second one (a review stores each key once).
    """
    at_head = [moved for f in reply.findings if (moved := _at_head(plan, f)) is not None]
    open_keys = {item.finding.key for item in previous.items if item.finding.fixed_in is None}
    fresh = [finding for finding in at_head if finding_key(finding) not in open_keys]
    selection = select_findings(fresh)
    fixed_keys = {item.finding.key for item in previous.items} - open_keys
    new = _NewFindings(
        findings=[f for f in selection.findings if f.key not in fixed_keys],
        reopened={f.key: f for f in selection.findings if f.key in fixed_keys},
        withheld_count=selection.withheld_count,
    )
    return new, {
        "outside_diff": len(reply.findings) - len(at_head),
        "duplicate": len(at_head) - len(fresh) + selection.duplicate_count,
        "over_limit": selection.over_limit_count,
    }


def _at_head(plan: PushPlan, finding: ProposedPushFinding) -> ProposedPushFinding | None:
    """The finding at the head's line numbers, or None if it isn't inside its commit's diff or
    a later commit changed its lines."""
    sha = plan.resolve(finding.commit)
    if sha is None or not plan.contains(
        StoredFix(
            commit=sha,
            file_path=finding.file_path,
            start_line=finding.start_line,
            end_line=finding.end_line,
        )
    ):
        return None
    lines = plan.to_head(sha, finding.file_path, (finding.start_line, finding.end_line))
    if lines is None:
        return None
    return finding.model_copy(update={"start_line": lines[0], "end_line": lines[1]})


def _checklist(
    items: list[StoredItem], fixes: dict[UUID, StoredFix], new: _NewFindings
) -> list[CodeFinding]:
    """The new checklist, ranked: open items (reopened ones included) in their old order, the
    new findings, then the fixed items (fixed before this push first)."""
    carried = [_carried(item, fixes, new.reopened) for item in items]
    pairs = list(zip(items, carried, strict=True))
    still_open = [finding for _, finding in pairs if finding.fixed_in is None]
    fixed_before = [
        finding
        for item, finding in pairs
        if item.finding.fixed_in is not None and finding.fixed_in is not None
    ]
    fixed_now = [
        finding
        for item, finding in pairs
        if item.finding.fixed_in is None and finding.fixed_in is not None
    ]
    ordered = [*still_open, *new.findings, *fixed_before, *fixed_now]
    return [dataclasses.replace(finding, rank=rank) for rank, finding in enumerate(ordered)]


def _carried(
    item: StoredItem, fixes: dict[UUID, StoredFix], reopened: dict[str, CodeFinding]
) -> CodeFinding:
    """The item in the new review: linked to the finding it started as, and fixed if this or an
    earlier push fixed it, unless the issue came back, which reopens it at the new lines."""
    origin_id = item.finding.origin_id or item.id
    if (back := reopened.get(item.finding.key)) is not None:
        return dataclasses.replace(
            item.finding, origin_id=origin_id, fixed_in=None, lines=back.lines
        )
    fixed_in = item.finding.fixed_in or fixes.get(item.id)
    return dataclasses.replace(item.finding, origin_id=origin_id, fixed_in=fixed_in)


def _prompt(plan: PushPlan, open_items: list[StoredItem], event: PullRequestEvent) -> str:
    unshown = plan.push.unshown_commits
    unshown_note = (
        f" The {unshown} oldest commits of the push aren't shown: don't cite them."
        if unshown
        else ""
    )
    skipped_note = (
        f" {len(plan.skipped_files)} changed files aren't shown (binary, generated, too large, "
        "or over the size budget): don't cite or comment on them."
        if plan.skipped_files
        else ""
    )
    return (
        "A student pushed new commits to this pull request. Say which open items they fix, "
        f"and report new issues in the new commits.{unshown_note}{skipped_note}\n\n"
        "<pull_request>\n"
        f"<title>{neutralize_tags(event.pull_request.title.strip())}</title>\n"
        f"<open_items>\n{_render_items([item.finding for item in open_items])}\n</open_items>\n"
        f"<push>\n{plan.diff}\n</push>\n"
        "</pull_request>"
    )


def _render_items(items: list[CodeFinding]) -> str:
    """The open items, numbered from 1 in the order given, as the agent sees them.

    Their text is the code review's output about the student's code, so it is data too.
    """
    rendered = []
    for number, item in enumerate(items, start=1):
        lines = ", ".join(f"{first}-{last}" for first, last in item.lines)
        rendered.append(
            f'<item number="{number}" file="{html.escape(item.file_path, quote=True)}" '
            f'lines="{lines}">\n{neutralize_tags(item.name)}: '
            f"{neutralize_tags(item.description)}\n</item>"
        )
    return "\n".join(rendered) or "(no open items)"
