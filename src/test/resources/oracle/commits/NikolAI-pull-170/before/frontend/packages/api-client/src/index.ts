// AI contribution: 50%
/**
 * Everything the NikolAI front ends share: the backend's response types, generated from its
 * OpenAPI schema (`src/schema.d.ts`, `pnpm generate:api`), the runtime checks that accept those
 * responses, the HTTP client, and the course roles. Import these names rather than
 * `components["schemas"][...]`, so a renamed backend model breaks here, in one place.
 */

import type { components } from "./schema";

export * from "./checks";
export * from "./http";
export * from "./roles";

type Schemas = components["schemas"];

/** `GET /api/me`: the signed-in user, with every course they belong to. */
export type CurrentUser = Schemas["Me"];
export type CourseSummary = Schemas["CourseSummary"];

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
