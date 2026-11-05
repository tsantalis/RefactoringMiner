// AI contribution: 50%
/** Read the data behind each NikolAI tab from the backend (UI revamp, #100). */

import type { PullRequestRef } from "../github-url";
import type { RepoRef } from "../page";
import { encodePath, joinBase, requestJson, type ApiResult, type FetchLike } from "./http";
import { literal, nullable, type Check } from "./schema";
import {
  ciAssistant,
  planning,
  pullInsights,
  pullRefactorings,
  storyReview,
  teamOverview,
  type CiAssistant,
  type FindingAction,
  type Planning,
  type PullInsights,
  type PullRefactorings,
  type StoryReview,
  type TeamOverview,
} from "./views-contract";

export interface ProjectRef {
  owner: string;
  number: number;
}

/** An issue is addressed like a pull request: owner, repo and number. */
export type IssueRef = PullRequestRef;

export interface NikolaiClient {
  readonly isSample: boolean;
  getTeam(repo: RepoRef, signal?: AbortSignal): Promise<ApiResult<TeamOverview>>;
  getPullInsights(ref: PullRequestRef, signal?: AbortSignal): Promise<ApiResult<PullInsights>>;
  getRefactorings(ref: PullRequestRef, signal?: AbortSignal): Promise<ApiResult<PullRefactorings>>;
  getStoryReview(ref: IssueRef, signal?: AbortSignal): Promise<ApiResult<StoryReview>>;
  getPlanning(ref: ProjectRef, signal?: AbortSignal): Promise<ApiResult<Planning>>;
  getCi(repo: RepoRef, signal?: AbortSignal): Promise<ApiResult<CiAssistant>>;
  /** Acknowledge or dispute a checklist item (US-10.4). */
  actOnFinding(findingId: string, action: FindingAction, reason: string): Promise<ApiResult<null>>;
}

const noContent = nullable(literal());

export class HttpNikolaiClient implements NikolaiClient {
  readonly isSample = false;
  private readonly baseUrl: string;

  constructor(
    baseUrl: string,
    private readonly fetchImpl: FetchLike = (input, init) => fetch(input, init),
  ) {
    this.baseUrl = joinBase(baseUrl);
  }

  private get<T>(path: string, check: Check<T>, signal?: AbortSignal): Promise<ApiResult<T>> {
    return requestJson(this.fetchImpl, `${this.baseUrl}/api/${path}`, check, { signal });
  }

  private repoPath(repo: RepoRef): string {
    return `repos/${encodePath(repo.owner, repo.repo)}`;
  }

  getTeam(repo: RepoRef, signal?: AbortSignal): Promise<ApiResult<TeamOverview>> {
    return this.get(`${this.repoPath(repo)}/team`, teamOverview, signal);
  }

  getPullInsights(ref: PullRequestRef, signal?: AbortSignal): Promise<ApiResult<PullInsights>> {
    return this.get(`${this.repoPath(ref)}/pulls/${ref.number}/insights`, pullInsights, signal);
  }

  getRefactorings(ref: PullRequestRef, signal?: AbortSignal): Promise<ApiResult<PullRefactorings>> {
    const path = `${this.repoPath(ref)}/pulls/${ref.number}/refactorings`;
    return this.get(path, pullRefactorings, signal);
  }

  getStoryReview(ref: IssueRef, signal?: AbortSignal): Promise<ApiResult<StoryReview>> {
    return this.get(`${this.repoPath(ref)}/issues/${ref.number}/story-review`, storyReview, signal);
  }

  getPlanning(ref: ProjectRef, signal?: AbortSignal): Promise<ApiResult<Planning>> {
    return this.get(`projects/${encodePath(ref.owner)}/${ref.number}/planning`, planning, signal);
  }

  getCi(repo: RepoRef, signal?: AbortSignal): Promise<ApiResult<CiAssistant>> {
    return this.get(`${this.repoPath(repo)}/ci`, ciAssistant, signal);
  }

  actOnFinding(findingId: string, action: FindingAction, reason: string): Promise<ApiResult<null>> {
    const url = `${this.baseUrl}/api/findings/${encodePath(findingId)}/actions`;
    return requestJson(this.fetchImpl, url, noContent, {
      method: "POST",
      body: { action, reason },
    });
  }
}
