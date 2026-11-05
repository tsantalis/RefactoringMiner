"""Read what the team panel shows (US-11.2) from stored PRs, runs and webhook events.

Everything is read for one repository, the one whose NikolAI tab is open, so every link the
panel builds points at it. Access is the caller's job: the API checks ``can_read_team`` first.
Callers own their connections.
"""

# AI contribution: 50%

from typing import Final
from uuid import UUID

from psycopg import AsyncConnection
from psycopg.rows import DictRow

from app.db.models.team_overview import (
    StoredChecklistCount,
    StoredPullStats,
    StoredTeam,
    StoredTeamMember,
    StoredTeamPull,
)
from app.db.repositories.feedback import CHECKLIST_OUTPUTS

# Open PRs the panel lists, newest updated first.
MAX_TEAM_PULLS: Final = 50

# ``draft`` and the commit count from the newest ``pull_request`` webhook payload of each of
# the repository's PRs: NikolAI stores no PR column for either. Only those two values go into
# the sort, not the whole payload.
_LATEST_EVENTS: Final = """
    latest_events AS (
        SELECT DISTINCT ON (w.pull_request_id) w.pull_request_id,
               w.payload #> '{pull_request,draft}' AS draft,
               w.payload #> '{pull_request,commits}' AS commits
        FROM webhook_events w
        WHERE w.repository_id = %(repo)s AND w.event_type = 'pull_request'
          AND w.pull_request_id IS NOT NULL AND w.payload IS NOT NULL
        ORDER BY w.pull_request_id, w.received_at DESC, w.id DESC)
"""


async def find_team(
    connection: AsyncConnection[DictRow], team_id: UUID, user_id: UUID
) -> StoredTeam | None:
    """The team's name, its course's name and ``user_id``'s active role in that course."""
    cursor = await connection.execute(
        """SELECT t.name, c.name AS course_name,
                  (SELECT cm.role FROM course_memberships cm
                   WHERE cm.course_id = t.course_id AND cm.user_id = %s
                     AND cm.left_at IS NULL) AS course_role
           FROM teams t LEFT JOIN courses c ON c.id = t.course_id
           WHERE t.id = %s""",
        (user_id, team_id),
    )
    row = await cursor.fetchone()
    return StoredTeam.model_validate(row) if row is not None else None


async def list_open_pulls(
    connection: AsyncConnection[DictRow], repository_id: UUID
) -> list[StoredTeamPull]:
    """The repository's open PRs with their draft flag and latest NikolAI review status.

    A cancelled review run is skipped, so the status is that of the last review that ran or is
    waiting to run.
    """
    cursor = await connection.execute(
        # Only a module constant is interpolated; values are bound parameters.
        f"WITH {_LATEST_EVENTS}"  # noqa: S608
        """SELECT p.id AS pull_request_id, p.pr_number AS number, p.title,
                  u.github_login AS author, p.github_updated_at AS updated_at,
                  COALESCE(e.draft = 'true'::jsonb, false) AS draft,
                  (SELECT a.status FROM analysis_runs a
                   WHERE a.pull_request_id = p.id AND a.analysis_type = 'llm_review'
                     AND a.status <> 'cancelled'
                   ORDER BY a.created_at DESC, a.id DESC LIMIT 1) AS review_status
           FROM pull_requests p
           JOIN users u ON u.id = p.author_user_id
           LEFT JOIN latest_events e ON e.pull_request_id = p.id
           WHERE p.repository_id = %(repo)s AND p.state = 'open'
           ORDER BY p.github_updated_at DESC, p.id DESC
           LIMIT %(limit)s""",
        {"repo": repository_id, "limit": MAX_TEAM_PULLS},
    )
    return [StoredTeamPull.model_validate(row) for row in await cursor.fetchall()]


async def checklist_counts(
    connection: AsyncConnection[DictRow], repository_id: UUID
) -> list[StoredChecklistCount]:
    """The checklist items of each open PR that has a checklist review, in one query.

    A PR's checklist is its latest checklist review's findings (``CHECKLIST_OUTPUTS``); a
    reviewed PR with no findings has 0 of 0. PRs without a checklist review aren't returned, so
    the panel shows them as not reviewed rather than as 0.

    Which items are open: only a fix closes an item. NikolAI doesn't record fixes yet, so every
    finding is open; a student acknowledging or disputing an item leaves it open too, since the
    problem is still in the code until a commit fixes it.
    """
    cursor = await connection.execute(
        # Only a module constant is interpolated; the repository is a bound parameter.
        f"WITH {CHECKLIST_OUTPUTS}"  # noqa: S608
        """SELECT c.pull_request_id, count(f.id) AS total, count(f.id) AS open
           FROM checklist_outputs c
           LEFT JOIN findings f ON f.analysis_output_id = c.id
           GROUP BY c.pull_request_id""",
        {"repo": repository_id},
    )
    return [StoredChecklistCount.model_validate(row) for row in await cursor.fetchall()]


async def pull_stats(connection: AsyncConnection[DictRow], repository_id: UUID) -> StoredPullStats:
    """How many of the repository's PRs are merged, and how many of those NikolAI reviewed."""
    cursor = await connection.execute(
        """SELECT count(*) FILTER (WHERE p.merged_at IS NOT NULL) AS merged_pulls,
                  count(*) FILTER (WHERE p.merged_at IS NOT NULL AND EXISTS (
                      SELECT 1 FROM analysis_runs a
                      WHERE a.pull_request_id = p.id AND a.analysis_type = 'llm_review'
                        AND a.status = 'completed')) AS reviewed_pulls
           FROM pull_requests p WHERE p.repository_id = %s""",
        (repository_id,),
    )
    row = await cursor.fetchone()
    if row is None:  # pragma: no cover - an aggregate without GROUP BY always returns a row.
        raise RuntimeError("The PR counts were not read")
    return StoredPullStats.model_validate(row)


async def list_members(
    connection: AsyncConnection[DictRow], team_id: UUID, repository_id: UUID
) -> list[StoredTeamMember]:
    """The team's active members, with the PRs they opened here and those PRs' commits.

    Commits are counted from each PR's latest webhook payload, so they are the commits in the
    member's own PRs, not commits they pushed elsewhere.
    """
    cursor = await connection.execute(
        # Only a module constant is interpolated; values are bound parameters.
        f"WITH {_LATEST_EVENTS}"  # noqa: S608
        """SELECT u.github_login AS login, count(p.id) AS pulls,
                  COALESCE(sum(CASE WHEN jsonb_typeof(e.commits) = 'number'
                               THEN floor((e.commits #>> '{}')::numeric)::int END), 0)
                      AS commits
           FROM team_memberships tm
           JOIN users u ON u.id = tm.user_id
           LEFT JOIN pull_requests p
             ON p.author_user_id = u.id AND p.repository_id = %(repo)s
           LEFT JOIN latest_events e ON e.pull_request_id = p.id
           WHERE tm.team_id = %(team)s AND tm.left_at IS NULL
           GROUP BY u.id, u.github_login
           ORDER BY lower(u.github_login), u.id""",
        {"repo": repository_id, "team": team_id},
    )
    return [StoredTeamMember.model_validate(row) for row in await cursor.fetchall()]
