// AI contribution: 50%
/** Calls the NikolAI API from the dashboard. Only `fetch` is replaceable, so tests mock it. */

import {
  array,
  boolean,
  courseRole,
  joinBase,
  nullable,
  number,
  object,
  requestJson,
  string,
  type ApiResult,
  type Check,
  type CourseSummary,
  type CurrentUser,
  type FetchLike,
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

  getCurrentUser(signal?: AbortSignal): Promise<ApiResult<CurrentUser>> {
    return requestJson(this.fetchImpl, `${this.baseUrl}/api/me`, currentUserCheck, { signal });
  }

  createCourse(name: string): Promise<ApiResult<CourseSummary>> {
    return requestJson(this.fetchImpl, `${this.baseUrl}/api/courses`, courseCheck, {
      method: "POST",
      body: { name },
    });
  }

  signInUrl(origin: string): string {
    return `${this.baseUrl}/auth/github/login?return_to=${encodeURIComponent(origin)}`;
  }

  signOut(): Promise<ApiResult<null>> {
    return requestJson(this.fetchImpl, `${this.baseUrl}/auth/logout`, isNull, { method: "POST" });
  }
}

const teamSchema = object({ id: string, name: string });

export const courseSchema = object({
  id: string,
  name: string,
  role: courseRole,
  join_code: nullable(string),
  teams: array(teamSchema),
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

const isNull: Check<null> = (value): value is null => value === null;
