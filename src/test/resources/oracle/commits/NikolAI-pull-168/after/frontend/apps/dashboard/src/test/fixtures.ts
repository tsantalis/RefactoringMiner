// AI contribution: 50%
/** Shared API fixtures for the dashboard tests. */

export const USER = {
  id: "11111111-1111-1111-1111-111111111111",
  github_user_id: 42,
  github_login: "octocat",
  display_name: "Mona",
  is_instructor: true,
  courses: [],
};

export const COURSE = {
  id: "22222222-2222-2222-2222-222222222222",
  name: "SOEN 490",
  role: "instructor",
  join_code: "ABCD2345",
  teams: [],
};

export const json = (status: number, body: unknown): Response =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

export interface Call {
  url: string;
  init: RequestInit | undefined;
}
