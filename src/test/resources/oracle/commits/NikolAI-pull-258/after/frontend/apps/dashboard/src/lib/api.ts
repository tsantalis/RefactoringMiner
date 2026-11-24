// AI contribution: 50%
/** Calls the NikolAI API from the dashboard. Only `fetch` is replaceable, so tests mock it. */

import {
  array,
  boolean,
  courseRole,
  grantRole,
  encodePath,
  joinBase,
  literal,
  nullable,
  number,
  object,
  requestJson,
  string,
  type ApiResult,
  type BrainstormList,
  type Check,
  type CourseMember,
  type CourseRole,
  type CourseSummary,
  type CurrentUser,
  type FetchLike,
  type JoinCode,
  type PlanningPage,
  type RequirementText,
  type RosterResult,
  type TeamDetail,
  type TeamAccessGrant,
  type TeamSummary,
} from "@nikolai/api-client";

export { REQUEST_FAILED, UNREADABLE, UNREACHABLE } from "@nikolai/api-client";
export type { ApiResult, FetchLike };

/** The two requirement texts a course stores, and the `${kind}-requirements` suffix they share. */
export type RequirementKind = "course" | "project";

export class DashboardClient {
  private readonly baseUrl: string;

  constructor(
    baseUrl: string,
    private readonly fetchImpl: FetchLike = (input, init) => fetch(input, init),
  ) {
    this.baseUrl = joinBase(baseUrl);
  }

  private coursePath(courseId: string, ...rest: string[]): string {
    return [`${this.baseUrl}/api/courses`, courseId, ...rest]
      .map((part, index) => (index === 0 ? part : encodePath(part)))
      .join("/");
  }

  getCurrentUser(signal?: AbortSignal): Promise<ApiResult<CurrentUser>> {
    return requestJson(this.fetchImpl, `${this.baseUrl}/api/me`, currentUserCheck, { signal });
  }

  getCourse(courseId: string, signal?: AbortSignal): Promise<ApiResult<CourseSummary>> {
    return requestJson(this.fetchImpl, this.coursePath(courseId), courseCheck, { signal });
  }

  createCourse(name: string): Promise<ApiResult<CourseSummary>> {
    return requestJson(this.fetchImpl, `${this.baseUrl}/api/courses`, courseCheck, {
      method: "POST",
      body: { name },
    });
  }

  /** Sends the code unchanged: the server normalises it (`normalize_join_code`) and 404s a bad one. */
  joinCourse(joinCode: string): Promise<ApiResult<CourseSummary>> {
    return requestJson(this.fetchImpl, `${this.baseUrl}/api/courses/join`, courseCheck, {
      method: "POST",
      body: { join_code: joinCode },
    });
  }

  rotateJoinCode(courseId: string): Promise<ApiResult<JoinCode>> {
    return requestJson(this.fetchImpl, this.coursePath(courseId, "join-code"), joinCodeCheck, {
      method: "POST",
    });
  }

  createTeam(courseId: string, name: string): Promise<ApiResult<TeamSummary>> {
    return requestJson(this.fetchImpl, this.coursePath(courseId, "teams"), teamCheck, {
      method: "POST",
      body: { name },
    });
  }

  listMembers(courseId: string, signal?: AbortSignal): Promise<ApiResult<CourseMember[]>> {
    return requestJson(this.fetchImpl, this.coursePath(courseId, "members"), array(memberCheck), {
      signal,
    });
  }

  promoteToTa(courseId: string, userId: string): Promise<ApiResult<CourseMember>> {
    return requestJson(
      this.fetchImpl,
      this.coursePath(courseId, "members", userId, "promote"),
      memberCheck,
      { method: "POST" },
    );
  }

  getTeam(courseId: string, teamId: string, signal?: AbortSignal): Promise<ApiResult<TeamDetail>> {
    return requestJson(
      this.fetchImpl,
      this.coursePath(courseId, "teams", teamId),
      teamDetailCheck,
      { signal },
    );
  }

  /** `GET /api/teams/{team_id}/planning`: the team, and whether the viewer may plan for it. */
  getPlanning(teamId: string, signal?: AbortSignal): Promise<ApiResult<PlanningPage>> {
    return requestJson(
      this.fetchImpl,
      `${this.baseUrl}/api/teams/${encodePath(teamId)}/planning`,
      planningCheck,
      { signal },
    );
  }

  getBrainstorm(teamId: string, signal?: AbortSignal): Promise<ApiResult<BrainstormList>> {
    return requestJson(
      this.fetchImpl,
      `${this.baseUrl}/api/teams/${encodePath(teamId)}/planning/brainstorm`,
      brainstormCheck,
      { signal },
    );
  }

  /** Rows go as the instructor pasted them: the server report is what the page shows. */
  importRoster(courseId: string, entries: RosterEntry[]): Promise<ApiResult<RosterResult>> {
    return requestJson(this.fetchImpl, this.coursePath(courseId, "roster"), rosterResultCheck, {
      method: "POST",
      body: { entries },
    });
  }

  linkRepository(
    courseId: string,
    teamId: string,
    repository: string,
  ): Promise<ApiResult<TeamDetail>> {
    return requestJson(
      this.fetchImpl,
      this.coursePath(courseId, "teams", teamId, "repositories"),
      teamDetailCheck,
      { method: "POST", body: { repository } },
    );
  }

  /** Move a member onto a team: it becomes their only team in this course (#214). */
  assignTeam(
    courseId: string,
    teamId: string,
    githubLogin: string,
  ): Promise<ApiResult<TeamDetail>> {
    return requestJson(
      this.fetchImpl,
      this.coursePath(courseId, "teams", teamId, "members"),
      teamDetailCheck,
      { method: "POST", body: { github_login: githubLogin } },
    );
  }

  makeStakeholder(courseId: string, userId: string): Promise<ApiResult<CourseMember>> {
    return requestJson(
      this.fetchImpl,
      this.coursePath(courseId, "members", userId, "stakeholder"),
      memberCheck,
      { method: "POST" },
    );
  }

  listGrants(courseId: string, signal?: AbortSignal): Promise<ApiResult<TeamAccessGrant[]>> {
    return requestJson(this.fetchImpl, this.coursePath(courseId, "grants"), array(grantCheck), {
      signal,
    });
  }

  grantTeamAccess(
    courseId: string,
    teamId: string,
    userId: string,
  ): Promise<ApiResult<TeamAccessGrant>> {
    return requestJson(
      this.fetchImpl,
      this.coursePath(courseId, "teams", teamId, "grants"),
      grantCheck,
      { method: "POST", body: { user_id: userId } },
    );
  }

  revokeTeamAccess(courseId: string, teamId: string, userId: string): Promise<ApiResult<null>> {
    return requestJson(
      this.fetchImpl,
      this.coursePath(courseId, "teams", teamId, "grants", userId),
      isNull,
      { method: "DELETE" },
    );
  }

  getRequirements(
    courseId: string,
    kind: RequirementKind,
    signal?: AbortSignal,
  ): Promise<ApiResult<RequirementText>> {
    return requestJson(
      this.fetchImpl,
      this.coursePath(courseId, `${kind}-requirements`),
      requirementTextCheck,
      { signal },
    );
  }

  replaceRequirements(
    courseId: string,
    kind: RequirementKind,
    text: string,
  ): Promise<ApiResult<RequirementText>> {
    return requestJson(
      this.fetchImpl,
      this.coursePath(courseId, `${kind}-requirements`),
      requirementTextCheck,
      { method: "PUT", body: { text } },
    );
  }

  signInUrl(origin: string): string {
    return `${this.baseUrl}/auth/github/login?return_to=${encodeURIComponent(origin)}`;
  }

  signOut(): Promise<ApiResult<null>> {
    return requestJson(this.fetchImpl, `${this.baseUrl}/auth/logout`, isNull, { method: "POST" });
  }
}

/** Same limit as the backend's `MAX_REQUIREMENT_TEXT_LENGTH` in `backend/app/db/models/courses.py`; UX only, the API decides. */
export const REQUIREMENT_TEXT_MAX = 100_000;

/** The length the backend counts: code points after stripping leading and trailing whitespace. */
export function requirementLength(text: string): number {
  return Array.from(text.trim()).length;
}

/** Same limit as the backend's `JoinRequest.join_code` in `backend/app/db/models/courses.py`; UX only, keep the two in step. */
export const JOIN_CODE_MAX = 40;

/** Same limit as the backend's `Name` in `backend/app/db/models/courses.py`; UX only, keep the two in step. */
export const NAME_MAX = 200;

/** Same shape and part limit as the backend's `REPOSITORY_NAME_PATTERN`; UX only, the API decides. */
export const REPOSITORY_NAME = /^[A-Za-z0-9_.-]{1,100}\/[A-Za-z0-9_.-]{1,100}$/;

/** Same limit as `RepositoryLink.repository` in `backend/app/db/models/courses.py`. */
export const REPOSITORY_MAX = 200;

/**
 * Accept what people copy from GitHub: `https://github.com/owner/name`, a `.git` suffix or a
 * trailing slash. The regex above is the final gate; this only does the trimming and stripping.
 */
export function repositoryName(raw: string): string {
  return raw
    .trim()
    .replace(/^(?:https?:\/\/)?(?:www\.)?github\.com\//i, "")
    .replace(/\.git$/i, "")
    .replace(/\/+$/, "");
}

/** One roster row as the page sends it; the server checks it and reports the bad rows. */
export interface RosterEntry {
  github_login: string;
  role?: CourseRole;
  team?: string;
}

export const teamSchema = object({ id: string, name: string });

export const courseSchema = object({
  id: string,
  name: string,
  role: courseRole,
  join_code: nullable(string),
  teams: array(teamSchema),
});

export const memberSchema = object({
  user_id: string,
  github_login: string,
  display_name: nullable(string),
  role: courseRole,
  team: nullable(teamSchema),
});

export const grantSchema = object({
  user_id: string,
  team_id: string,
  access_role: grantRole,
  github_login: string,
});

export const joinCodeSchema = object({ join_code: string });

export const requirementTextSchema = object({ text: string });

export const teamMemberSchema = object({
  user_id: string,
  github_login: string,
  display_name: nullable(string),
  role: literal("member", "lead"),
});

export const teamDetailSchema = object({
  id: string,
  name: string,
  course_id: string,
  members: array(teamMemberSchema),
  repositories: array(string),
});

export const planningSchema = object({
  team: object({ id: string, name: string, course: nullable(string) }),
  can_plan: boolean,
});

export const brainstormSchema = object({
  label: string,
  repositories: array(
    object({
      full_name: string,
      status: literal("listed", "failed"),
      issues: array(object({ number: number, title: string, url: string })),
      more: boolean,
      all_url: string,
    }),
  ),
});

export const rosterRowErrorSchema = object({
  row: number,
  github_login: string,
  reason: string,
});

export const rosterResultSchema = object({
  imported: number,
  applied: number,
  errors: array(rosterRowErrorSchema),
});

export const currentUserSchema = object({
  id: string,
  github_user_id: number,
  github_login: string,
  display_name: nullable(string),
  is_instructor: boolean,
  courses: array(courseSchema),
});

export const courseCheck: Check<CourseSummary> = courseSchema;
export const currentUserCheck: Check<CurrentUser> = currentUserSchema;
export const memberCheck: Check<CourseMember> = memberSchema;
export const teamCheck: Check<TeamSummary> = teamSchema;
export const grantCheck: Check<TeamAccessGrant> = grantSchema;
export const joinCodeCheck: Check<JoinCode> = joinCodeSchema;
export const requirementTextCheck: Check<RequirementText> = requirementTextSchema;
export const teamDetailCheck: Check<TeamDetail> = teamDetailSchema;
export const rosterResultCheck: Check<RosterResult> = rosterResultSchema;
export const planningCheck: Check<PlanningPage> = planningSchema;
export const brainstormCheck: Check<BrainstormList> = brainstormSchema;

const isNull: Check<null> = (value): value is null => value === null;
