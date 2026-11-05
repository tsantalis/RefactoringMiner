// AI contribution: 50%
/**
 * Runtime check of `GET /api/repos/{owner}/{repo}/pulls/{number}` before the panel trusts it.
 * TypeScript types vanish at runtime, so a changed or broken response would otherwise crash the
 * panel half-way through. Built from the `schema.ts` checks, so its type (`Infer`) can be compared
 * with the generated `PullRequestFeedback` in `contract-types.test.ts`.
 */

import type { PullRequestFeedback } from "@nikolai/api-client";

import { array, literal, nullable, number, object, string, type Check } from "./schema";
import { severity } from "./views-contract";

const usage = object({ prompt_tokens: number, completion_tokens: number, total_tokens: number });

const review = object({
  content: string,
  model: string,
  provider: nullable(string),
  latency_ms: number,
  usage: nullable(usage),
  created_at: string,
});

const run = object({
  id: string,
  status: literal("queued", "running", "completed", "failed", "cancelled"),
  tool_name: string,
  tool_version: nullable(string),
  created_at: string,
  started_at: nullable(string),
  finished_at: nullable(string),
  error_message: nullable(string),
  retry_of_run_id: nullable(string),
  review: nullable(review),
  comment_url: nullable(string),
});

const finding = object({
  id: string,
  name: string,
  description: nullable(string),
  severity: nullable(severity),
  category: nullable(string),
  locations: array(
    object({
      file_path: string,
      side: literal("parent", "child"),
      start_line: nullable(number),
      end_line: nullable(number),
    }),
  ),
});

export const pullRequestFeedback = object({
  repository: string,
  number,
  title: string,
  author_login: string,
  state: literal("open", "closed"),
  merged_at: nullable(string),
  runs: array(run),
  findings: array(finding),
});

/** The same check, typed as the generated response the views use. */
export const isPullRequestFeedback: Check<PullRequestFeedback> = pullRequestFeedback;
