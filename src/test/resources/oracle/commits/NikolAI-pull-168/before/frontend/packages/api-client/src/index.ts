// AI contribution: 50%
/**
 * The NikolAI backend's response types, generated from its OpenAPI schema (`src/schema.d.ts`,
 * `pnpm generate:api`). Import these names rather than `components["schemas"][...]`, so a
 * renamed backend model breaks here, in one place. Add a name when a view first needs it.
 *
 * These are compile-time types only: a view still checks each response at runtime before it
 * trusts it (`apps/extension/src/shared/api/schema.ts`).
 */

import type { components } from "./schema";

type Schemas = components["schemas"];

/** `GET /api/repos/{owner}/{repo}/pulls/{number}`: the PR, its review runs and findings. */
export type PullRequestFeedback = Schemas["PullRequestFeedback"];
export type ReviewRun = Schemas["ReviewRun"];
export type Finding = Schemas["Finding"];
export type RunStatus = ReviewRun["status"];
export type Severity = NonNullable<Finding["severity"]>;

/** `GET /api/repos/{owner}/{repo}/pulls/{number}/refactorings`. */
export type PullRequestRefactorings = Schemas["PullRequestRefactorings"];
/** `GET /api/repos/{owner}/{repo}/pulls/{number}/insights`. */
export type PullInsights = Schemas["PullInsights"];
/** `GET /api/repos/{owner}/{repo}/team`. */
export type TeamOverview = Schemas["TeamOverview"];
/** `POST /api/findings/{finding_id}/actions`: the request body. */
export type FindingActionRequest = Schemas["FindingActionRequest"];
