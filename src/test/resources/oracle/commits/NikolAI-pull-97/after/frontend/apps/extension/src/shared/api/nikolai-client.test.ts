// AI contribution: 50%
import { describe, expect, it } from "vitest";
import { HttpNikolaiClient } from "./nikolai-client";
import { sampleTeam } from "./samples/team";
import { SAMPLE_REFACTORINGS, samplePullInsights } from "./samples/views";

const json = (status: number, body: unknown): Response =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

interface Call {
  url: string;
  init: RequestInit | undefined;
}

function clientReturning(response: Response): { client: HttpNikolaiClient; calls: Call[] } {
  const calls: Call[] = [];
  const client = new HttpNikolaiClient("https://nikolai.example.ca/", (url, init) => {
    calls.push({ url, init });
    return Promise.resolve(response);
  });
  return { client, calls };
}

const REPO = { owner: "team-alpha", repo: "sandbox" };

describe("HttpNikolaiClient", () => {
  it("reads a team's overview from its repository", async () => {
    const team = sampleTeam("student");
    const { client, calls } = clientReturning(json(200, team));
    expect(await client.getTeam(REPO)).toEqual({ kind: "ok", data: team });
    expect(calls[0]?.url).toBe("https://nikolai.example.ca/api/repos/team-alpha/sandbox/team");
  });

  it("rejects an overview with an unknown course role", async () => {
    const team = { ...sampleTeam("student"), viewer: { login: "x", role: "admin" } };
    const { client } = clientReturning(json(200, team));
    expect(await client.getTeam(REPO)).toEqual({
      kind: "unreachable",
      reason: "The NikolAI server sent a response we can't read.",
    });
  });

  it("rejects staff-only data sent to a student, so a server bug can't leak marks", async () => {
    const unreadable = {
      kind: "unreachable",
      reason: "The NikolAI server sent a response we can't read.",
    };
    const studentWithStaff = { ...sampleTeam("ta"), viewer: { login: "x", role: "student" } };
    expect(await clientReturning(json(200, studentWithStaff)).client.getTeam(REPO)).toEqual(
      unreadable,
    );
    const pull = { ...samplePullInsights("ta"), viewer: { login: "x", role: "student" } };
    const ref = { ...REPO, number: 1 };
    expect(await clientReturning(json(200, pull)).client.getPullInsights(ref)).toEqual(unreadable);
    const ta = samplePullInsights("ta");
    expect(await clientReturning(json(200, ta)).client.getPullInsights(ref)).toEqual({
      kind: "ok",
      data: ta,
    });
  });

  it("reads refactorings, with or without a location, and rejects an unknown change", async () => {
    const ref = { ...REPO, number: 7 };
    const [first, ...rest] = SAMPLE_REFACTORINGS.items;
    if (!first) throw new Error("no sample refactoring");
    const unlocated = {
      ...SAMPLE_REFACTORINGS,
      items: [{ ...first, file_path: null, start_line: null }, ...rest],
    };
    expect(await clientReturning(json(200, unlocated)).client.getRefactorings(ref)).toEqual({
      kind: "ok",
      data: unlocated,
    });
    const odd = { ...SAMPLE_REFACTORINGS, items: [{ ...first, change: "moved" }] };
    expect(await clientReturning(json(200, odd)).client.getRefactorings(ref)).toEqual({
      kind: "unreachable",
      reason: "The NikolAI server sent a response we can't read.",
    });
  });

  it("reports another team's repository as no access", async () => {
    const { client } = clientReturning(json(403, { detail: "Forbidden" }));
    expect(await client.getTeam(REPO)).toEqual({ kind: "forbidden" });
  });

  it("builds each view's endpoint", async () => {
    const { client, calls } = clientReturning(json(404, {}));
    await client.getPullInsights({ ...REPO, number: 7 });
    await client.getRefactorings({ ...REPO, number: 7 });
    await client.getStoryReview({ ...REPO, number: 8 });
    await client.getPlanning({ owner: "soen 490", number: 2 });
    await client.getCi(REPO);
    expect(calls.map((call) => call.url)).toEqual([
      "https://nikolai.example.ca/api/repos/team-alpha/sandbox/pulls/7/insights",
      "https://nikolai.example.ca/api/repos/team-alpha/sandbox/pulls/7/refactorings",
      "https://nikolai.example.ca/api/repos/team-alpha/sandbox/issues/8/story-review",
      "https://nikolai.example.ca/api/projects/soen%20490/2/planning",
      "https://nikolai.example.ca/api/repos/team-alpha/sandbox/ci",
    ]);
  });

  it("posts a student's dispute with its reason", async () => {
    const { client, calls } = clientReturning(new Response(null, { status: 204 }));
    const result = await client.actOnFinding("f-1", "dispute", "Only GET endpoints exist so far.");
    expect(result).toEqual({ kind: "ok", data: null });
    expect(calls[0]?.url).toBe("https://nikolai.example.ca/api/findings/f-1/actions");
    expect(calls[0]?.init?.method).toBe("POST");
    expect(JSON.parse(calls[0]?.init?.body as string)).toEqual({
      action: "dispute",
      reason: "Only GET endpoints exist so far.",
    });
  });
});
