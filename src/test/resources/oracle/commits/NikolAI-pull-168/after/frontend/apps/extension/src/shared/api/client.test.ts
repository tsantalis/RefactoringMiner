// AI contribution: 50%
import { describe, expect, it, vi } from "vitest";
import { PR_REF } from "../../test/fakes";
import { HttpFeedbackClient } from "./client";
import type { PullRequestFeedback } from "@nikolai/api-client";
import { sampleFeedback } from "./sample";
import { isPullRequestFeedback } from "./validate";

const json = (status: number, body: unknown): Response =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

function stored(): PullRequestFeedback {
  const result = sampleFeedback(PR_REF, "reviewed", new Date("2026-10-20T12:00:00Z"));
  if (result.kind !== "ok") throw new Error("sample should be ok");
  return result.feedback;
}

function clientReturning(response: Response | Error): {
  client: HttpFeedbackClient;
  urls: string[];
} {
  const urls: string[] = [];
  const client = new HttpFeedbackClient("https://nikolai.example.ca/", (url) => {
    urls.push(url);
    return response instanceof Error ? Promise.reject(response) : Promise.resolve(response);
  });
  return { client, urls };
}

describe("HttpFeedbackClient", () => {
  it("reads a stored PR and its feedback", async () => {
    const { client, urls } = clientReturning(json(200, stored()));
    const result = await client.getFeedback(PR_REF);
    expect(urls).toEqual(["https://nikolai.example.ca/api/repos/team-alpha/sandbox/pulls/42"]);
    expect(result).toEqual({ kind: "ok", feedback: stored() });
  });

  it("encodes owner and repo in the URL", () => {
    const { client } = clientReturning(json(200, {}));
    expect(client.feedbackUrl({ owner: "a b", repo: "c/d", number: 1 })).toBe(
      "https://nikolai.example.ca/api/repos/a%20b/c%2Fd/pulls/1",
    );
  });

  it("reports an unknown PR", async () => {
    const { client } = clientReturning(json(404, { detail: { code: "pull_request_not_found" } }));
    expect(await client.getFeedback(PR_REF)).toEqual({
      kind: "not-found",
      code: "pull_request_not_found",
    });
  });

  it("reports a repository NikolAI isn't set up for", async () => {
    const { client } = clientReturning(
      json(404, { detail: { code: "repository_not_registered" } }),
    );
    expect(await client.getFeedback(PR_REF)).toEqual({ kind: "not-registered" });
  });

  it("treats a plain 404 as an unknown PR", async () => {
    const { client } = clientReturning(json(404, { detail: "Not Found" }));
    expect(await client.getFeedback(PR_REF)).toEqual({ kind: "not-found" });
  });

  it.each([
    [401, "signed-out"],
    [403, "forbidden"],
  ])("maps HTTP %i to %s", async (status, kind) => {
    const { client } = clientReturning(json(status, { detail: "nope" }));
    expect((await client.getFeedback(PR_REF)).kind).toBe(kind);
  });

  it("reports a server error as unreachable", async () => {
    const { client } = clientReturning(json(500, {}));
    expect(await client.getFeedback(PR_REF)).toEqual({
      kind: "unreachable",
      reason: "NikolAI had a problem. Try again in a moment.",
    });
  });

  it("reports a network failure as unreachable", async () => {
    const { client } = clientReturning(new TypeError("Failed to fetch"));
    expect((await client.getFeedback(PR_REF)).kind).toBe("unreachable");
  });

  it("rejects a response that doesn't match the contract, and warns developers", async () => {
    const warn = vi.spyOn(console, "warn").mockImplementation(() => undefined);
    const { client } = clientReturning(json(200, { repository: "x", runs: "not a list" }));
    expect((await client.getFeedback(PR_REF)).kind).toBe("unreachable");
    expect(warn).toHaveBeenCalledOnce();
    expect(String(warn.mock.calls[0]?.[0])).not.toContain("not a list");
    warn.mockRestore();
  });

  it("gives up on a server that never answers", async () => {
    // A fake server that accepts the request and then stays silent until it's aborted.
    const hanging = (_url: string, init?: RequestInit): Promise<Response> =>
      new Promise((_resolve, reject) => {
        init?.signal?.addEventListener("abort", () => {
          reject(new Error("aborted"));
        });
      });
    const client = new HttpFeedbackClient("https://nikolai.example.ca", hanging, 20);
    expect(await client.getFeedback(PR_REF)).toEqual({
      kind: "unreachable",
      reason: "The NikolAI server took too long to answer.",
    });
    // With a caller signal too, the timeout still wins while the caller hasn't cancelled.
    expect((await client.getFeedback(PR_REF, new AbortController().signal)).kind).toBe(
      "unreachable",
    );
  });

  it("lets an aborted request reject so the panel can ignore it", async () => {
    const controller = new AbortController();
    controller.abort();
    const { client } = clientReturning(new Error("aborted"));
    await expect(client.getFeedback(PR_REF, controller.signal)).rejects.toThrow("aborted");
  });
});

describe("isPullRequestFeedback", () => {
  it("accepts every sample scenario that has feedback", () => {
    for (const scenario of ["reviewed", "running", "failed", "no-findings"] as const) {
      const result = sampleFeedback(PR_REF, scenario);
      expect(result.kind === "ok" && isPullRequestFeedback(result.feedback)).toBe(true);
    }
  });

  it("rejects unknown statuses and severities", () => {
    const feedback = stored();
    expect(
      isPullRequestFeedback({ ...feedback, runs: [{ ...feedback.runs[0], status: "done" }] }),
    ).toBe(false);
    expect(
      isPullRequestFeedback({
        ...feedback,
        findings: [{ ...feedback.findings[0], severity: "HIGH" }],
      }),
    ).toBe(false);
    expect(isPullRequestFeedback(null)).toBe(false);
  });
});
