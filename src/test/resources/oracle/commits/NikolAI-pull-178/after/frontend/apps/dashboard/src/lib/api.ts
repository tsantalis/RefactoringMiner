// AI contribution: 50%
/** Calls the NikolAI API from the dashboard. Only `fetch` is replaceable, so tests mock it. */

import {
  array,
  boolean,
  courseRole,
  encodePath,
  joinBase,
  nullable,
  number,
  object,
  requestJson,
  string,
  type ApiResult,
  type Check,
  type CourseMember,
  type CourseSummary,
  type CurrentUser,
  type FetchLike,
  type JoinCode,
  type TeamSummary,
} from "@nikolai/api-client";

export { REQUEST_FAILED, UNREADABLE, UNREACHABLE } from "@nikolai/api-client";
export type { ApiResult, FetchLike };

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

  signInUrl(origin: string): string {
    return `${this.baseUrl}/auth/github/login?return_to=${encodeURIComponent(origin)}`;
  }

  signOut(): Promise<ApiResult<null>> {
    return requestJson(this.fetchImpl, `${this.baseUrl}/auth/logout`, isNull, { method: "POST" });
  }
}

/** Same limit as the backend's `Name` in `models/courses.py`; UX only, keep the two in step. */
export const NAME_MAX = 200;

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
});

export const joinCodeSchema = object({ join_code: string });

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
export const joinCodeCheck: Check<JoinCode> = joinCodeSchema;

const isNull: Check<null> = (value): value is null => value === null;
