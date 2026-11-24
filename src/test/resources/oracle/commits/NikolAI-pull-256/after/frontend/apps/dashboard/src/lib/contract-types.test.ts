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
  JoinRequest,
  PlanningPage,
  RequirementText,
  RosterResult,
  TeamDetail,
  TeamSummary,
  Infer,
} from "@nikolai/api-client";
import { describe, expectTypeOf, it } from "vitest";

import {
  type DashboardClient,
  courseSchema,
  currentUserSchema,
  joinCodeSchema,
  memberSchema,
  planningSchema,
  requirementTextSchema,
  rosterResultSchema,
  teamDetailSchema,
  teamSchema,
} from "./api";

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

  it("POST /api/courses/join: the body the client sends is the server's JoinRequest", () => {
    expectTypeOf<Parameters<DashboardClient["joinCourse"]>[0]>().toEqualTypeOf<
      JoinRequest["join_code"]
    >();
  });

  it("POST /api/courses/{id}/join-code: the new code is what the check accepts, and back", () => {
    expectTypeOf<JoinCode>().toExtend<Infer<typeof joinCodeSchema>>();
    expectTypeOf<Infer<typeof joinCodeSchema>>().toExtend<JoinCode>();
  });

  it("GET/PUT requirement texts: the server's response is what the check accepts, and back", () => {
    expectTypeOf<RequirementText>().toExtend<Infer<typeof requirementTextSchema>>();
    expectTypeOf<Infer<typeof requirementTextSchema>>().toExtend<RequirementText>();
  });

  it("GET/POST team detail: the members and repositories shape the check accepts, and back", () => {
    expectTypeOf<TeamDetail>().toExtend<Infer<typeof teamDetailSchema>>();
    expectTypeOf<Infer<typeof teamDetailSchema>>().toExtend<TeamDetail>();
  });

  it("POST /api/courses/{id}/roster: the per-row report the check accepts, and back", () => {
    expectTypeOf<RosterResult>().toExtend<Infer<typeof rosterResultSchema>>();
    expectTypeOf<Infer<typeof rosterResultSchema>>().toExtend<RosterResult>();
  });

  it("GET /api/teams/{id}/planning: the planning page the check accepts, and back", () => {
    expectTypeOf<PlanningPage>().toExtend<Infer<typeof planningSchema>>();
    expectTypeOf<Infer<typeof planningSchema>>().toExtend<PlanningPage>();
  });
});
