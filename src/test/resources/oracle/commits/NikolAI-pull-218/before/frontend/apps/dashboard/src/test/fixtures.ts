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

export const TEAM = { id: "33333333-3333-3333-3333-333333333333", name: "Pixel" };

export const ALICE_MEMBER = {
  user_id: "44444444-4444-4444-4444-444444444444",
  github_login: "alice",
  display_name: "Alice",
  role: "student",
};

export const STAKEHOLDER_MEMBER = {
  user_id: "99999999-9999-9999-9999-999999999999",
  github_login: "product-owner",
  display_name: "Product Owner",
  role: "stakeholder",
};

export const PROF_MEMBER = {
  user_id: "55555555-5555-5555-5555-555555555555",
  github_login: "prof",
  display_name: "Prof",
  role: "instructor",
};

export const MEMBERS = [ALICE_MEMBER, PROF_MEMBER];

/** `GET /api/courses/{course_id}/teams/{team_id}` for `TEAM`, with one linked repository. */
export const TEAM_DETAIL = {
  id: TEAM.id,
  name: TEAM.name,
  course_id: COURSE.id,
  members: [
    {
      user_id: ALICE_MEMBER.user_id,
      github_login: ALICE_MEMBER.github_login,
      display_name: ALICE_MEMBER.display_name,
      role: "member",
    },
  ],
  repositories: ["pixel-org/app"],
};

/** The grants list for an instructor opening the course page. */
export const GRANTS: unknown[] = [];

export const json = (status: number, body: unknown): Response =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

export interface Call {
  url: string;
  init: RequestInit | undefined;
}
