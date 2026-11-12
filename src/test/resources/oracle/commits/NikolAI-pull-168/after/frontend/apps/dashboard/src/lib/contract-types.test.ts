// AI contribution: 50%
/**
 * The dashboard's runtime checks against the backend's generated types. These are type-level
 * assertions: `pnpm typecheck` fails when a response the dashboard reads is no longer what the
 * check accepts (a field renamed, removed, or of another type). Regenerate with `pnpm generate:api`
 * after a backend change, then fix whichever side is wrong.
 */

import type { CourseSummary, CurrentUser, Infer } from "@nikolai/api-client";
import { describe, expectTypeOf, it } from "vitest";

import { courseSchema, currentUserSchema } from "./api";

describe("dashboard responses match the backend's generated types", () => {
  it("GET /api/me: the server's response is what the check accepts, and back", () => {
    expectTypeOf<CurrentUser>().toExtend<Infer<typeof currentUserSchema>>();
    expectTypeOf<Infer<typeof currentUserSchema>>().toExtend<CurrentUser>();
  });

  it("GET /api/courses: the server's course is what the check accepts, and back", () => {
    expectTypeOf<CourseSummary>().toExtend<Infer<typeof courseSchema>>();
    expectTypeOf<Infer<typeof courseSchema>>().toExtend<CourseSummary>();
  });
});
