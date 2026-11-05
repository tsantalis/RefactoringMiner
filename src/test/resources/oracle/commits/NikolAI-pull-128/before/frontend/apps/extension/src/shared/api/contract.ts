// AI contribution: 50%
/**
 * TEMPORARY: the response the extension expects from the US-5 read-only API (task 5.6).
 *
 * The conventions say API types must be generated into `@nikolai/api-client` from the backend's
 * OpenAPI schema. That endpoint doesn't exist yet, so this file is the proposed contract for it,
 * built from the US-4 tables (`pull_requests`, `analysis_runs`, `analysis_outputs`, `findings`,
 * `finding_locations`, `github_publications`). Once 5.6 ships, run `pnpm generate:api` and replace
 * these imports with the generated types, then delete this file.
 *
 * Endpoint: GET /api/repos/{owner}/{repo}/pulls?limit=50&offset=0
 *   200 → PullRequestSummary[], newest updated first (limit 1 to 100)
 *   401, 403 and 404 (repository_not_registered) as below
 *
 * Endpoint: GET /api/repos/{owner}/{repo}/pulls/{number}
 *   200 → PullRequestFeedback
 *   401 → not signed in (US-11)
 *   403 → signed in but not on this team (US-10)
 *   404 → {"detail": {"code": "repository_not_registered" | "pull_request_not_found"}}
 */

export type RunStatus = "queued" | "running" | "completed" | "failed" | "cancelled";

export type Severity = "info" | "low" | "medium" | "high" | "critical";

/** Same fields as the backend's `TokenUsage` (app/providers/base.py). */
export interface TokenUsage {
  prompt_tokens: number;
  completion_tokens: number;
  total_tokens: number;
}

/** A saved agent result: `analysis_outputs.text_content` plus fields of its `json_content`. */
export interface ReviewOutput {
  content: string;
  model: string;
  provider: string | null;
  latency_ms: number;
  usage: TokenUsage | null;
  created_at: string;
}

/** One `analysis_runs` row of type `llm_review`, with its output once it completed. */
export interface ReviewRun {
  id: string;
  status: RunStatus;
  tool_name: string;
  tool_version: string | null;
  created_at: string;
  started_at: string | null;
  finished_at: string | null;
  /** A safe error category (e.g. "RateLimited"), never a raw provider error. */
  error_message: string | null;
  retry_of_run_id: string | null;
  review: ReviewOutput | null;
  /** `github_publications.url` of the comment that posted this review, when there is one. */
  comment_url: string | null;
}

export interface FindingLocation {
  file_path: string;
  side: "parent" | "child";
  start_line: number | null;
  end_line: number | null;
}

export interface Finding {
  id: string;
  name: string;
  description: string | null;
  severity: Severity | null;
  category: string | null;
  locations: FindingLocation[];
}

export interface PullRequestFeedback {
  repository: string;
  number: number;
  title: string;
  author_login: string;
  state: "open" | "closed";
  merged_at: string | null;
  /** Every review run for the PR, newest first, including failed ones. */
  runs: ReviewRun[];
  /** Line-level findings of the latest completed review (empty until the US-9 agent). */
  findings: Finding[];
}

/** One stored PR in the repository's list. Timestamps are GitHub's. */
export interface PullRequestSummary {
  number: number;
  title: string;
  author_login: string;
  state: "open" | "closed";
  merged_at: string | null;
  created_at: string;
  updated_at: string;
}

/** The `detail.code` of each error the server names, so a view can say what went wrong. */
export const ERROR_CODES = {
  404: ["repository_not_registered", "pull_request_not_found", "finding_not_found"],
  403: ["origin_not_allowed", "not_a_team_student"],
  409: ["finding_superseded"],
} as const;

export type NotFoundCode = (typeof ERROR_CODES)[404][number];
export type ForbiddenCode = (typeof ERROR_CODES)[403][number];
export type ConflictCode = (typeof ERROR_CODES)[409][number];
