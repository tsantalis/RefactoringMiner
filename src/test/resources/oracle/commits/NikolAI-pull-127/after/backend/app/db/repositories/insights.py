"""Read what the PR tab's insights and the review comment (US-9) need from stored review runs.

Access is the caller's job: the API runs ``readable_repository`` first. Callers own their
connections.
"""

# AI contribution: 50%

from typing import Final, cast
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db.models.checklist import StoredChecklistSource
from app.db.models.findings import FIXED_IN
from app.db.models.pull_insights import StoredChecklist, StoredCodeReview, StoredFix, read_fix
from app.db.repositories.feedback import (
    CHECKLIST_OUTPUT,
    CODE_REVIEW_RAN,
    FINDING_FIXED,
    FINDING_ORDER,
)

# A stored withheld count above this (or below 0) isn't a count of the model's findings.
MAX_WITHHELD: Final = 10_000
# The checklist review's stored ``code_review.withheld_count``, as ``w.value``.
WITHHELD_VALUE: Final = """CROSS JOIN LATERAL (
                SELECT checklist_output.metadata #> '{code_review,withheld_count}' AS value) w"""
# ``w.value`` as a count, or NULL when it isn't a count of the model's findings. Nested so the
# cast only sees numbers: AND doesn't short-circuit in SQL.
WITHHELD_COUNT: Final = """CASE WHEN jsonb_typeof(w.value) = 'number' THEN
                       CASE WHEN (w.value #>> '{}')::numeric BETWEEN 0 AND %(max)s
                            THEN floor((w.value #>> '{}')::numeric)::int END
                   END"""


async def checklist_review(
    connection: AsyncConnection[DictRow], pull_request_id: UUID
) -> StoredChecklist | None:
    """The finding IDs, their statuses, their fixes and the withheld count of the PR's checklist
    review (``CHECKLIST_OUTPUT``).

    An item is ``fixed`` when a push review recorded its fix (``FINDING_FIXED``). Otherwise its
    status is the latest ``user_actions`` row, by ``created_at``, for any finding of the PR with
    the same key: a push review carries each item into the next review under its key
    (``app.services.fix_tracking``), so an answer stays with the item. Answers are from anyone:
    the checklist belongs to the team's PR, and only the team's students can answer it
    (``can_answer_for_team``), so the latest answer by any of them is the team's. Without one
    it is ``open``. One statement for every finding.
    """
    cursor = await connection.execute(
        # Only module constants are interpolated; values are bound parameters.
        f"""WITH {CHECKLIST_OUTPUT}
            SELECT coalesce(array_agg(f.id ORDER BY {FINDING_ORDER})
                            FILTER (WHERE f.id IS NOT NULL), '{{}}') AS finding_ids,
                   coalesce(array_agg(CASE WHEN {FINDING_FIXED} THEN 'fixed'
                                           ELSE coalesce(answer.action_type, 'open') END
                                      ORDER BY {FINDING_ORDER})
                            FILTER (WHERE f.id IS NOT NULL), '{{}}') AS statuses,
                   coalesce(array_agg(f.details -> '{FIXED_IN}' ORDER BY {FINDING_ORDER})
                            FILTER (WHERE f.id IS NOT NULL), '{{}}') AS fixes,
                   {WITHHELD_COUNT} AS withheld_count
            FROM checklist_output
            {WITHHELD_VALUE}
            LEFT JOIN findings f ON f.analysis_output_id = checklist_output.id
            LEFT JOIN LATERAL (
                SELECT u.action_type FROM findings same
                JOIN analysis_outputs so ON so.id = same.analysis_output_id
                JOIN analysis_runs sa ON sa.id = so.analysis_run_id
                JOIN user_actions u ON u.finding_id = same.id
                WHERE same.finding_key = f.finding_key
                  AND sa.pull_request_id = %(pull_request)s
                  AND u.action_type IN ('acknowledged', 'disputed')
                ORDER BY u.created_at DESC, u.id DESC LIMIT 1) answer ON true
            GROUP BY w.value""",  # noqa: S608
        {"pull_request": pull_request_id, "up_to_run": None, "max": MAX_WITHHELD},
    )
    row = await cursor.fetchone()
    return StoredChecklist.model_validate(row) if row is not None else None


async def comment_checklist_source(
    connection: AsyncConnection[DictRow], pull_request_id: UUID, run_id: UUID, *, unchanged: str
) -> StoredChecklistSource:
    """What the comment of review run ``run_id`` lists, read in one statement.

    That is the PR's checklist review as it stood when the run was created (``CHECKLIST_OUTPUT``
    with ``up_to_run``), its withheld count, and whether its code review had nothing to send
    the model. ``failed_since`` says that a code review after it, up to this run, didn't run for
    a reason other than ``unchanged`` (what an edited title records), so the code moved on
    without a new checklist; with no checklist review, any such failure counts.
    """
    cursor = await connection.execute(
        # Only module constants are interpolated; values are bound parameters.
        f"""WITH {CHECKLIST_OUTPUT}
            SELECT checklist_output.id AS output_id,
                   {WITHHELD_COUNT} AS withheld_count,
                   coalesce(jsonb_typeof(checklist_output.metadata #> '{{code_review,result}}')
                            = 'null', false) AS nothing_reviewed,
                   EXISTS (
                       SELECT 1 FROM analysis_runs a
                       JOIN analysis_outputs o
                         ON o.analysis_run_id = a.id AND o.output_key = 'review'
                        AND o.output_type = 'agent_result'
                       WHERE a.pull_request_id = %(pull_request)s
                         AND a.analysis_type = 'llm_review' AND a.status = 'completed'
                         AND o.metadata #>> '{{code_review,not_run}}' <> %(unchanged)s
                         AND (a.created_at, a.id) <= (SELECT r.created_at, r.id
                                                      FROM analysis_runs r
                                                      WHERE r.id = %(up_to_run)s)
                         AND (checklist_output.id IS NULL
                              OR (a.created_at, a.id)
                                 > (checklist_output.run_created_at, checklist_output.run_id))
                   ) AS failed_since
            FROM (SELECT 1) AS one
            LEFT JOIN checklist_output ON true
            {WITHHELD_VALUE}""",  # noqa: S608
        {
            "pull_request": pull_request_id,
            "up_to_run": run_id,
            "unchanged": unchanged,
            "max": MAX_WITHHELD,
        },
    )
    row = await cursor.fetchone()
    if row is None:
        raise RuntimeError("The checklist source query returned no row")
    return StoredChecklistSource.model_validate(row)


async def list_code_reviews(
    connection: AsyncConnection[DictRow], pull_request_id: UUID
) -> list[StoredCodeReview]:
    """The PR's completed reviews whose code review ran, oldest first, with the keys of their
    findings and of those fixed.

    The head commit is the run's ``child_commit_sha``; runs saved before that column was filled
    for reviews fall back to the head SHA in their triggering webhook payload.
    """
    cursor = await connection.execute(
        # Only a module constant is interpolated; the PR is a bound parameter.
        f"""SELECT coalesce(a.child_commit_sha,
                            e.payload #>> '{{pull_request,head,sha}}') AS head_sha,
                   coalesce(array_agg(f.finding_key ORDER BY f.finding_key)
                            FILTER (WHERE f.id IS NOT NULL), '{{}}') AS finding_keys,
                   coalesce(array_agg(f.finding_key ORDER BY f.finding_key)
                            FILTER (WHERE {FINDING_FIXED}), '{{}}') AS fixed_keys
            FROM analysis_runs a
            JOIN analysis_outputs o
              ON o.analysis_run_id = a.id AND o.output_key = 'review'
             AND o.output_type = 'agent_result'
            LEFT JOIN webhook_events e ON e.id = a.trigger_event_id
            LEFT JOIN findings f ON f.analysis_output_id = o.id
            WHERE a.pull_request_id = %s AND a.analysis_type = 'llm_review'
              AND a.status = 'completed' AND {CODE_REVIEW_RAN}
            GROUP BY a.id, e.id
            ORDER BY a.created_at, a.id""",  # noqa: S608
        (pull_request_id,),
    )
    return [StoredCodeReview.model_validate(row) for row in await cursor.fetchall()]


async def list_output_fixes(
    connection: AsyncConnection[DictRow], output_id: UUID
) -> dict[UUID, StoredFix]:
    """The fixes of review output ``output_id``'s fixed findings, by finding id."""
    cursor = await connection.execute(
        # Only module constants are interpolated; the output is a bound parameter.
        f"""SELECT f.id, f.details -> '{FIXED_IN}' AS fix FROM findings f
            WHERE f.analysis_output_id = %s AND {FINDING_FIXED}""",  # noqa: S608
        (output_id,),
    )
    fixes = {cast(UUID, row["id"]): read_fix(row["fix"]) for row in await cursor.fetchall()}
    return {finding_id: fix for finding_id, fix in fixes.items() if fix is not None}
