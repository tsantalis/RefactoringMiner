"""The checklist a saved review's PR comment lists (US-9).

It is the PR's checklist (``CHECKLIST_OUTPUT``) as it stood when the review ran: the review's
own findings, or, when its code review didn't run, those of the latest review before it whose
code review did, with the fixes a push review recorded. So the comment lists what the NikolAI
tab showed for that review.
"""

# AI contribution: 50%

from uuid import UUID

from app.db import connect
from app.db.models.checklist import Checklist, CommentChecklist
from app.db.repositories.feedback import list_output_findings
from app.db.repositories.insights import comment_checklist_source, list_output_fixes
from app.db.repositories.publications import find_published_review
from app.services.code_review import CODE_UNCHANGED


async def load_comment_checklist(url: str, publication_id: UUID) -> CommentChecklist:
    """The checklist the comment of publication ``publication_id`` lists.

    The checklist review is read once and its findings by its output, so the count and the
    findings always come from the same review.

    Raises:
        ValueError: if there is no such publication.
    """
    async with await connect(url) as connection:
        published = await find_published_review(connection, publication_id)
        if published is None:
            raise ValueError("Saved review publication not found")
        source = await comment_checklist_source(
            connection, published.pull_request_id, published.run_id, unchanged=CODE_UNCHANGED
        )
        if source.output_id is None:
            return CommentChecklist(checklist=None, failed_since=source.failed_since)
        findings = await list_output_findings(connection, source.output_id)
        fixes = await list_output_fixes(connection, source.output_id)
    checklist = Checklist(
        findings=findings,
        # A missing or nonsensical stored count reads as none withheld, as in the PR insights.
        withheld_count=source.withheld_count or 0,
        nothing_reviewed=source.nothing_reviewed,
        fixes=fixes,
    )
    return CommentChecklist(checklist=checklist, failed_since=source.failed_since)
