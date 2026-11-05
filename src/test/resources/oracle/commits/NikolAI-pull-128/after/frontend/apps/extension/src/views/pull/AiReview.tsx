// AI contribution: 50%
/** The PR's AI review (US-3) and every review run, failed ones included (US-11.3). */

import type { ReviewRun } from "@nikolai/api-client";
import { formatLatency, timeAgo } from "../../shared/format";
import { Markdown } from "../../shared/markdown";
import { isInProgress, latestReview, runStatusView } from "../../shared/status";
import { Card, Pill, safeGitHubUrl } from "../ui";

/**
 * Whether the comment `runs[index]` posted is still on the PR. A run that finishes after it (a
 * new review, or the "couldn't review" note) replaces NikolAI's comment, so only a run with
 * nothing newer but queued or running runs still has its comment.
 */
function commentIsLive(runs: ReviewRun[], index: number): boolean {
  return runs.slice(0, index).every(isInProgress);
}

/** A link to the PR comment that posted a run, when it's still there and on github.com. */
function CommentLink({ runs, index, label }: { runs: ReviewRun[]; index: number; label: string }) {
  const url = runs[index]?.comment_url ?? null;
  const href = url === null || !commentIsLive(runs, index) ? null : safeGitHubUrl(url);
  return href === null ? null : <a href={href}>{label}</a>;
}

function RunHistory({ runs }: { runs: ReviewRun[] }) {
  if (runs.length === 0) return null;
  return (
    <Card title="Review runs">
      <ul className="card-body small plain-list" aria-label="Review runs">
        {runs.map((run, index) => {
          const status = runStatusView(run.status);
          return (
            <li key={run.id} className="inline">
              <Pill tone={status.tone}>{status.label}</Pill>
              <span className="muted">{timeAgo(run.created_at)}</span>
              {run.error_message !== null && (
                <span className="muted">Reason: {run.error_message}</span>
              )}
              {run.review !== null && (
                <span className="muted">
                  {run.review.model} · {formatLatency(run.review.latency_ms)}
                </span>
              )}
              <span className="spacer" />
              <CommentLink runs={runs} index={index} label="Comment" />
            </li>
          );
        })}
      </ul>
    </Card>
  );
}

/** What the newest run says, for a PR with no review to show. */
function missingReview(newest: ReviewRun | undefined): string {
  if (newest === undefined) return "NikolAI hasn't reviewed this pull request yet.";
  if (isInProgress(newest)) {
    return "NikolAI is reviewing this pull request. Its review appears here when it's done.";
  }
  if (newest.status === "cancelled") return "NikolAI's latest review was cancelled.";
  const reason = newest.error_message === null ? "" : ` (${newest.error_message})`;
  return `NikolAI couldn't review this pull request: its latest attempt failed${reason}.`;
}

/** A note when a newer run than the review shown exists, or null when the review is current. */
function staleNote(newest: ReviewRun, shown: ReviewRun): string | null {
  if (newest.id === shown.id) return null;
  const from = `This is the review of an earlier push, from ${timeAgo(shown.created_at)}.`;
  if (isInProgress(newest)) return `NikolAI is reviewing the latest push. ${from}`;
  if (newest.status === "cancelled") return `The latest review was cancelled. ${from}`;
  return `The latest review attempt failed. ${from}`;
}

/** The newest review NikolAI posted on the PR, then the history of every run. */
export function AiReview({ runs }: { runs: ReviewRun[] }) {
  const latest = latestReview(runs);
  const newest = runs[0];
  const index = latest === undefined ? -1 : runs.indexOf(latest);
  const note = latest === undefined || newest === undefined ? null : staleNote(newest, latest);
  return (
    <>
      <Card
        title="NikolAI review"
        actions={
          latest === undefined ? undefined : (
            <CommentLink runs={runs} index={index} label="View the comment on GitHub" />
          )
        }
        footer="AI-generated advisory feedback on the title and description, not a grade."
      >
        {note !== null && (
          <div className="card-body small" role="note" style={{ color: "var(--warn-fg)" }}>
            {note}
          </div>
        )}
        {latest?.review == null ? (
          <div className="card-body muted">{missingReview(newest)}</div>
        ) : (
          <div className="card-body">
            <Markdown source={latest.review.content} />
          </div>
        )}
      </Card>
      <RunHistory runs={runs} />
    </>
  );
}
