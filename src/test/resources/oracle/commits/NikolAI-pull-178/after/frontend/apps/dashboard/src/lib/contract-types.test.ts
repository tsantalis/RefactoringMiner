// AI contribution: 50%
/**
 * The dashboard's runtime checks against the backend's generated types. These are type-level
 * assertions: `pnpm typecheck` fails when a response the dashboard reads is no longer what the
 * check accepts (a field renamed, removed, or of another type). Regenerate with `pnpm generate:api`
 * after a backend change, then fix whichever side is wrong.
 */

import type {
  CourseMember,
  CourseSummary,
  CurrentUser,
  JoinCode,
  TeamSummary,
  Infer,
} from "@nikolai/api-client";
import { describe, expectTypeOf, it } from "vitest";

import { courseSchema, currentUserSchema, joinCodeSchema, memberSchema, teamSchema } from "./api";

describe("dashboard responses match the backend's generated types", () => {
  it("GET /api/me: the server's response is what the check accepts, and back", () => {
    expectTypeOf<CurrentUser>().toExtend<Infer<typeof currentUserSchema>>();
    expectTypeOf<Infer<typeof currentUserSchema>>().toExtend<CurrentUser>();
  });

  it("GET /api/courses: the server's course is what the check accepts, and back", () => {
    expectTypeOf<CourseSummary>().toExtend<Infer<typeof courseSchema>>();
    expectTypeOf<Infer<typeof courseSchema>>().toExtend<CourseSummary>();
  });

  it("GET /api/courses/{id}/members: the member row is what the check accepts, and back", () => {
    expectTypeOf<CourseMember>().toExtend<Infer<typeof memberSchema>>();
    expectTypeOf<Infer<typeof memberSchema>>().toExtend<CourseMember>();
  });

  it("POST /api/courses/{id}/teams: the created team is what the check accepts, and back", () => {
    expectTypeOf<TeamSummary>().toExtend<Infer<typeof teamSchema>>();
    expectTypeOf<Infer<typeof teamSchema>>().toExtend<TeamSummary>();
  });

  it("POST /api/courses/{id}/join-code: the new code is what the check accepts, and back", () => {
    expectTypeOf<JoinCode>().toExtend<Infer<typeof joinCodeSchema>>();
    expectTypeOf<Infer<typeof joinCodeSchema>>().toExtend<JoinCode>();
  });
});
