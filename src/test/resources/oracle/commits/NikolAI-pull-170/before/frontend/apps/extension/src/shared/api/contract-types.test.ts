// AI contribution: 50%
/**
 * The runtime checks against the backend's generated types (`@nikolai/api-client`). These are
 * type-level assertions: `pnpm typecheck` fails when a shipped endpoint's generated response is
 * no longer what the extension's check accepts (a field renamed, removed, or of another type).
 * Regenerate with `pnpm generate:api` after a backend change, then fix whichever side is wrong.
 */

import type {
  FindingActionRequest,
  PullInsights as ServerPullInsights,
  PullRequestFeedback as ServerPullRequestFeedback,
  PullRequestRefactorings as ServerPullRequestRefactorings,
  TeamOverview as ServerTeamOverview,
} from "@nikolai/api-client";
import { describe, expectTypeOf, it } from "vitest";

import type { Infer } from "./schema";
import type { pullRequestFeedback } from "./validate";
import type { FindingAction, PullInsights, PullRefactorings, TeamOverview } from "./views-contract";

type PullRequestFeedback = Infer<typeof pullRequestFeedback>;

describe("shipped endpoints match the backend's generated types", () => {
  it("GET .../pulls/{n}: the server's response is what the check accepts, and back", () => {
    expectTypeOf<ServerPullRequestFeedback>().toExtend<PullRequestFeedback>();
    expectTypeOf<PullRequestFeedback>().toExtend<ServerPullRequestFeedback>();
  });

  it("GET .../team: the server's response is what the check accepts, and back", () => {
    expectTypeOf<ServerTeamOverview>().toExtend<TeamOverview>();
    expectTypeOf<TeamOverview>().toExtend<ServerTeamOverview>();
  });

  it("GET .../pulls/{n}/refactorings: the server's response is what the check accepts, and back", () => {
    expectTypeOf<ServerPullRequestRefactorings>().toExtend<PullRefactorings>();
    expectTypeOf<PullRefactorings>().toExtend<ServerPullRequestRefactorings>();
  });

  it("GET .../pulls/{n}/insights: the server's response is what the check accepts", () => {
    // One way only: `your_review` and `marking` are always null on the server until their
    // stories ship, while the check already accepts their proposed shape.
    expectTypeOf<ServerPullInsights>().toExtend<PullInsights>();
  });

  it("POST /api/findings/{id}/actions: the actions the extension sends are the server's", () => {
    expectTypeOf<FindingAction>().toEqualTypeOf<FindingActionRequest["action"]>();
  });

  it("rejects a server response whose field changed type", () => {
    type Pull = ServerTeamOverview["pulls"][number];
    type Drifted = Omit<ServerTeamOverview, "pulls"> & {
      pulls: (Omit<Pull, "draft"> & { draft: string })[];
    };
    expectTypeOf<Drifted>().not.toExtend<TeamOverview>();
  });
});
