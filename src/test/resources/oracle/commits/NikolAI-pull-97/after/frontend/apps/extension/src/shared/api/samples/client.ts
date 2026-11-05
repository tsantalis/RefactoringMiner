// AI contribution: 50%
/** The NikolAI client used until a server is set: every view shows made-up data or a state. */

import type { ApiResult } from "../http";
import type { NikolaiClient } from "../nikolai-client";
import type { SampleScenario } from "../sample";
import type { CourseRole } from "../views-contract";
import { sampleTeam } from "./team";
import {
  SAMPLE_REFACTORINGS,
  sampleCi,
  samplePlanning,
  samplePullInsights,
  sampleStoryReview,
} from "./views";

function failureFor(scenario: SampleScenario): ApiResult<never> | null {
  switch (scenario) {
    case "not-registered":
      return { kind: "not-registered" };
    case "signed-out":
      return { kind: "signed-out" };
    case "unreachable":
      return { kind: "unreachable", reason: "The NikolAI server didn't answer." };
    default:
      // "not-found" only concerns the PR feedback; the other views still have data.
      return null;
  }
}

export class SampleNikolaiClient implements NikolaiClient {
  readonly isSample = true;

  constructor(
    private readonly scenario: SampleScenario,
    private readonly role: CourseRole,
  ) {}

  private answer<T>(data: (role: CourseRole) => T): Promise<ApiResult<T>> {
    return Promise.resolve(failureFor(this.scenario) ?? { kind: "ok", data: data(this.role) });
  }

  getTeam(): Promise<ApiResult<ReturnType<typeof sampleTeam>>> {
    return this.answer((role) => sampleTeam(role));
  }

  getPullInsights(): Promise<ApiResult<ReturnType<typeof samplePullInsights>>> {
    return this.answer(samplePullInsights);
  }

  getRefactorings(): Promise<ApiResult<typeof SAMPLE_REFACTORINGS>> {
    return this.answer(() => SAMPLE_REFACTORINGS);
  }

  getStoryReview(): Promise<ApiResult<ReturnType<typeof sampleStoryReview>>> {
    return this.answer(sampleStoryReview);
  }

  getPlanning(): Promise<ApiResult<ReturnType<typeof samplePlanning>>> {
    return this.answer((role) => samplePlanning(role));
  }

  getCi(): Promise<ApiResult<ReturnType<typeof sampleCi>>> {
    return this.answer(sampleCi);
  }

  actOnFinding(): Promise<ApiResult<null>> {
    return this.answer(() => null);
  }
}
