// AI contribution: 50%
/** How a review's state is shown: a word, a tone, and the toolbar badge (US-5). */

import type { FeedbackResult } from "./api/client";
import type { ReviewRun, RunStatus, Severity } from "./api/contract";

export type Tone = "success" | "attention" | "danger" | "neutral";

export interface StatusView {
  label: string;
  tone: Tone;
}

const RUN_STATUS: Record<RunStatus, StatusView> = {
  completed: { label: "Reviewed", tone: "success" },
  running: { label: "Reviewing", tone: "attention" },
  queued: { label: "Queued", tone: "neutral" },
  failed: { label: "Review failed", tone: "danger" },
  cancelled: { label: "Cancelled", tone: "neutral" },
};

export function runStatusView(status: RunStatus): StatusView {
  return RUN_STATUS[status];
}

export function isInProgress(run: ReviewRun | undefined): boolean {
  return run?.status === "queued" || run?.status === "running";
}

/** The newest run that has a review to read, if any. */
export function latestReview(runs: readonly ReviewRun[]): ReviewRun | undefined {
  return runs.find((run) => run.status === "completed" && run.review !== null);
}

/** The PR's overall status in one view: what the toolbar badge and header button show. */
export function summaryView(result: FeedbackResult): StatusView | null {
  switch (result.kind) {
    case "ok": {
      const latest = result.feedback.runs[0];
      return latest ? runStatusView(latest.status) : { label: "Not reviewed", tone: "neutral" };
    }
    case "not-found":
      return { label: "Not reviewed", tone: "neutral" };
    case "conflict":
      return { label: "Out of date", tone: "neutral" };
    case "signed-out":
      return { label: "Signed out", tone: "neutral" };
    case "forbidden":
      return { label: "No access", tone: "neutral" };
    case "unreachable":
      return { label: "Can't connect", tone: "danger" };
    case "not-registered":
      return null;
  }
}

export interface BadgeView {
  text: string;
  color: string;
  title: string;
}

const BADGE_COLORS: Record<Tone, string> = {
  success: "#1A7F37",
  attention: "#9A6700",
  danger: "#CF222E",
  neutral: "#59636E",
};

// Neutral states (not reviewed, signed out, no access) deliberately show no badge text, so PRs
// NikolAI has nothing to say about stay quiet; the tooltip still names the state.
const BADGE_TEXT: Record<Tone, string> = {
  success: "OK",
  attention: "...",
  danger: "!",
  neutral: "",
};

/** The toolbar badge for a PR, or null for no badge (not a NikolAI repo). */
export function badgeFor(result: FeedbackResult): BadgeView | null {
  const view = summaryView(result);
  if (view === null) return null;
  return {
    text: BADGE_TEXT[view.tone],
    color: BADGE_COLORS[view.tone],
    title: `NikolAI: ${view.label}`,
  };
}

export const SEVERITY_ORDER: readonly (Severity | null)[] = [
  "critical",
  "high",
  "medium",
  "low",
  "info",
  null,
];

export function severityRank(severity: Severity | null): number {
  return SEVERITY_ORDER.indexOf(severity);
}

export function severityLabel(severity: Severity | null): string {
  if (severity === null) return "Unrated";
  return severity.charAt(0).toUpperCase() + severity.slice(1);
}

export function severityTone(severity: Severity | null): Tone {
  switch (severity) {
    case "critical":
    case "high":
      return "danger";
    case "medium":
      return "attention";
    default:
      return "neutral";
  }
}
