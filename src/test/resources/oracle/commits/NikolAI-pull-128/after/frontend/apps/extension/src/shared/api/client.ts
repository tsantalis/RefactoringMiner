// AI contribution: 50%
/** Read a PR's stored NikolAI feedback from the backend (US-5). */

import type { PullRequestRef } from "../github-url";
import type { PullRequestFeedback } from "@nikolai/api-client";
import {
  encodePath,
  joinBase,
  requestJson,
  REQUEST_TIMEOUT_MS,
  type ApiFailure,
  type FetchLike,
} from "./http";
import { isPullRequestFeedback } from "./validate";

/** Every outcome the PR view has a screen for. */
export type FeedbackResult = { kind: "ok"; feedback: PullRequestFeedback } | ApiFailure;

export interface FeedbackClient {
  /** True when the data is made up (no NikolAI server configured yet). */
  readonly isSample: boolean;
  getFeedback(ref: PullRequestRef, signal?: AbortSignal): Promise<FeedbackResult>;
}

export class HttpFeedbackClient implements FeedbackClient {
  readonly isSample = false;
  private readonly baseUrl: string;

  constructor(
    baseUrl: string,
    private readonly fetchImpl: FetchLike = (input, init) => fetch(input, init),
    /** A server that accepts the connection but never answers mustn't leave the view loading. */
    private readonly timeoutMs = REQUEST_TIMEOUT_MS,
  ) {
    this.baseUrl = joinBase(baseUrl);
  }

  feedbackUrl(ref: PullRequestRef): string {
    return `${this.baseUrl}/api/repos/${encodePath(ref.owner, ref.repo)}/pulls/${ref.number}`;
  }

  async getFeedback(ref: PullRequestRef, signal?: AbortSignal): Promise<FeedbackResult> {
    const result = await requestJson(this.fetchImpl, this.feedbackUrl(ref), isPullRequestFeedback, {
      signal,
      timeoutMs: this.timeoutMs,
    });
    return result.kind === "ok" ? { kind: "ok", feedback: result.data } : result;
  }
}
