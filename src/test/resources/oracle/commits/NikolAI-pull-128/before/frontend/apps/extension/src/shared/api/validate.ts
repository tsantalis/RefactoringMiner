/**
 * Runtime check of the backend's response before the panel trusts it. TypeScript types vanish at
 * runtime, so a changed or broken response would otherwise crash the panel half-way through.
 */

import type { PullRequestFeedback } from "./contract";

type Json = Record<string, unknown>;

const RUN_STATUSES = new Set(["queued", "running", "completed", "failed", "cancelled"]);
const SEVERITIES = new Set(["info", "low", "medium", "high", "critical"]);

function isObject(value: unknown): value is Json {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

const isString = (value: unknown): value is string => typeof value === "string";
const isNumber = (value: unknown): value is number =>
  typeof value === "number" && Number.isFinite(value);
const orNull =
  <T>(check: (value: unknown) => value is T) =>
  (value: unknown): value is T | null =>
    value === null || check(value);

function isUsage(value: unknown): boolean {
  return (
    isObject(value) &&
    isNumber(value.prompt_tokens) &&
    isNumber(value.completion_tokens) &&
    isNumber(value.total_tokens)
  );
}

function isReview(value: unknown): boolean {
  return (
    isObject(value) &&
    isString(value.content) &&
    isString(value.model) &&
    orNull(isString)(value.provider) &&
    isNumber(value.latency_ms) &&
    (value.usage === null || isUsage(value.usage)) &&
    isString(value.created_at)
  );
}

function isRun(value: unknown): boolean {
  return (
    isObject(value) &&
    isString(value.id) &&
    isString(value.status) &&
    RUN_STATUSES.has(value.status) &&
    isString(value.tool_name) &&
    orNull(isString)(value.tool_version) &&
    isString(value.created_at) &&
    orNull(isString)(value.started_at) &&
    orNull(isString)(value.finished_at) &&
    orNull(isString)(value.error_message) &&
    orNull(isString)(value.retry_of_run_id) &&
    (value.review === null || isReview(value.review)) &&
    orNull(isString)(value.comment_url)
  );
}

function isLocation(value: unknown): boolean {
  return (
    isObject(value) &&
    isString(value.file_path) &&
    (value.side === "parent" || value.side === "child") &&
    orNull(isNumber)(value.start_line) &&
    orNull(isNumber)(value.end_line)
  );
}

function isFinding(value: unknown): boolean {
  return (
    isObject(value) &&
    isString(value.id) &&
    isString(value.name) &&
    orNull(isString)(value.description) &&
    (value.severity === null || (isString(value.severity) && SEVERITIES.has(value.severity))) &&
    orNull(isString)(value.category) &&
    Array.isArray(value.locations) &&
    value.locations.every(isLocation)
  );
}

export function isPullRequestFeedback(value: unknown): value is PullRequestFeedback {
  return (
    isObject(value) &&
    isString(value.repository) &&
    isNumber(value.number) &&
    isString(value.title) &&
    isString(value.author_login) &&
    (value.state === "open" || value.state === "closed") &&
    orNull(isString)(value.merged_at) &&
    Array.isArray(value.runs) &&
    value.runs.every(isRun) &&
    Array.isArray(value.findings) &&
    value.findings.every(isFinding)
  );
}
