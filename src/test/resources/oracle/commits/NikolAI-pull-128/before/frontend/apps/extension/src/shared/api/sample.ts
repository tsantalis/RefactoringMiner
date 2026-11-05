/**
 * Made-up feedback for demos and development, used until a NikolAI server is set in the options.
 * Each scenario matches one of the panel's states, so every screen can be shown without a backend.
 */

import type { PullRequestRef } from "../github-url";
import type { FeedbackClient, FeedbackResult } from "./client";
import type { Finding, PullRequestFeedback, ReviewRun } from "./contract";

export const SAMPLE_SCENARIOS = [
  "reviewed",
  "running",
  "failed",
  "no-findings",
  "not-found",
  "not-registered",
  "signed-out",
  "unreachable",
] as const;

export type SampleScenario = (typeof SAMPLE_SCENARIOS)[number];

export const SAMPLE_SCENARIO_LABELS: Record<SampleScenario, string> = {
  reviewed: "Reviewed, with findings on the code",
  running: "Review in progress",
  failed: "Latest review failed",
  "no-findings": "Reviewed, no line-level findings (today's agent)",
  "not-found": "NikolAI has no record of the PR",
  "not-registered": "Repository not set up",
  "signed-out": "Signed out",
  unreachable: "Server unreachable",
};

const SAMPLE_REVIEW = `### Title

The title says what the PR does, but "Add validation" is broad. Name the form, for example
**"Validate the login form before submitting"**.

### Description

- It explains *what* changed, but not *why* the old behaviour was a problem.
- There's no note on how you tested it. Add the steps, or the tests you ran:

\`\`\`bash
pnpm test -- login-form
\`\`\`

### Suggestions

1. Link the issue this PR closes, e.g. \`Fixes #57\`.
2. Mention the edge cases you covered (empty email, wrong password).`;

const SAMPLE_FINDINGS: Finding[] = [
  {
    id: "f-1",
    name: "Password is logged in plain text",
    description: "The login handler logs the request body, which includes the password.",
    severity: "high",
    category: "security",
    locations: [{ file_path: "src/auth/session.py", side: "child", start_line: 58, end_line: 60 }],
  },
  {
    id: "f-2",
    name: "Input isn't validated before use",
    description: "The email is used before checking that it isn't empty.",
    severity: "medium",
    category: "reliability",
    locations: [
      { file_path: "src/auth/login_form.py", side: "child", start_line: 24, end_line: 24 },
    ],
  },
  {
    id: "f-3",
    name: "Duplicated validation logic",
    description: "The same checks appear in the sign-up form; consider sharing them.",
    severity: "low",
    category: "maintainability",
    locations: [
      { file_path: "src/auth/login_form.py", side: "child", start_line: 31, end_line: 38 },
    ],
  },
];

function minutesAgo(now: Date, minutes: number): string {
  return new Date(now.getTime() - minutes * 60_000).toISOString();
}

function completedRun(now: Date, ref: PullRequestRef): ReviewRun {
  return {
    id: "run-2",
    status: "completed",
    tool_name: "stub-agent",
    tool_version: "1",
    created_at: minutesAgo(now, 6),
    started_at: minutesAgo(now, 6),
    finished_at: minutesAgo(now, 5),
    error_message: null,
    retry_of_run_id: "run-1",
    review: {
      content: SAMPLE_REVIEW,
      model: "cohere/north-mini-code:free",
      provider: "OpenRouter",
      latency_ms: 3240,
      usage: { prompt_tokens: 412, completion_tokens: 236, total_tokens: 648 },
      created_at: minutesAgo(now, 5),
    },
    // Sample data has no real bot comment, so the anchor lands at the top of the PR's Conversation.
    comment_url: `https://github.com/${ref.owner}/${ref.repo}/pull/${ref.number}#issuecomment-sample`,
  };
}

function failedRun(now: Date, id: string, minutes: number): ReviewRun {
  return {
    id,
    status: "failed",
    tool_name: "stub-agent",
    tool_version: "1",
    created_at: minutesAgo(now, minutes + 1),
    started_at: minutesAgo(now, minutes + 1),
    finished_at: minutesAgo(now, minutes),
    error_message: "RateLimited",
    retry_of_run_id: null,
    review: null,
    comment_url: null,
  };
}

export function sampleFeedback(
  ref: PullRequestRef,
  scenario: SampleScenario,
  now: Date = new Date(),
): FeedbackResult {
  const base: Omit<PullRequestFeedback, "runs" | "findings"> = {
    repository: `${ref.owner}/${ref.repo}`,
    number: ref.number,
    title: "Add login form validation",
    author_login: "sample-student",
    state: "open",
    merged_at: null,
  };
  switch (scenario) {
    case "reviewed":
      return {
        kind: "ok",
        feedback: {
          ...base,
          runs: [completedRun(now, ref), failedRun(now, "run-1", 8)],
          findings: SAMPLE_FINDINGS,
        },
      };
    case "no-findings":
      return {
        kind: "ok",
        feedback: { ...base, runs: [completedRun(now, ref)], findings: [] },
      };
    case "running":
      return {
        kind: "ok",
        feedback: {
          ...base,
          runs: [
            {
              ...failedRun(now, "run-3", 0),
              status: "running",
              finished_at: null,
              error_message: null,
            },
            completedRun(now, ref),
          ],
          findings: [],
        },
      };
    case "failed":
      return {
        kind: "ok",
        feedback: { ...base, runs: [failedRun(now, "run-1", 2)], findings: [] },
      };
    case "not-found":
      return { kind: "not-found" };
    case "not-registered":
      return { kind: "not-registered" };
    case "signed-out":
      return { kind: "signed-out" };
    case "unreachable":
      return { kind: "unreachable", reason: "The NikolAI server didn't answer." };
  }
}

export class SampleFeedbackClient implements FeedbackClient {
  readonly isSample = true;

  constructor(private readonly scenario: SampleScenario) {}

  getFeedback(ref: PullRequestRef): Promise<FeedbackResult> {
    return Promise.resolve(sampleFeedback(ref, this.scenario));
  }
}
