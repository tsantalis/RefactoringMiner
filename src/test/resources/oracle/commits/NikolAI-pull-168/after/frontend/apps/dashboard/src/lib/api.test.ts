// AI contribution: 50%
import { TIMED_OUT, requestJson, string } from "@nikolai/api-client";
import { describe, expect, it, vi } from "vitest";
import { DashboardClient, UNREADABLE, UNREACHABLE, type FetchLike } from "./api";
import { COURSE, USER, json, type Call } from "../test/fixtures";

function clientReturning(response: Response | Error): { client: DashboardClient; calls: Call[] } {
  const calls: Call[] = [];
  const fetchImpl: FetchLike = (url, init) => {
    calls.push({ url, init });
    return response instanceof Error ? Promise.reject(response) : Promise.resolve(response);
  };
  return { client: new DashboardClient("https://api.test/", fetchImpl), calls };
}

describe("DashboardClient", () => {
  it("reads the signed-in user with the session cookie", async () => {
    const { client, calls } = clientReturning(json(200, USER));

    expect(await client.getCurrentUser()).toEqual({ kind: "ok", data: USER });
    expect(calls[0]?.url).toBe("https://api.test/api/me");
    expect(calls[0]?.init?.method).toBe("GET");
    expect(calls[0]?.init?.credentials).toBe("include");
    expect(calls[0]?.init?.headers).toEqual({ Accept: "application/json" });
  });

  it("reports a signed-out user", async () => {
    expect(await clientReturning(json(401, {})).client.getCurrentUser()).toEqual({
      kind: "signed-out",
    });
  });

  it("reports a forbidden user", async () => {
    expect(await clientReturning(json(403, {})).client.getCurrentUser()).toEqual({
      kind: "forbidden",
    });
  });

  it("rejects a user response with a missing field, and warns developers", async () => {
    const warn = vi.spyOn(console, "warn").mockImplementation(() => undefined);
    const { client } = clientReturning(json(200, { ...USER, courses: undefined }));

    expect(await client.getCurrentUser()).toEqual({ kind: "unreachable", reason: UNREADABLE });
    expect(String(warn.mock.calls[0]?.[0])).toContain("/api/me");
    warn.mockRestore();
  });

  it("reports a server that doesn't answer", async () => {
    expect(await clientReturning(new TypeError("offline")).client.getCurrentUser()).toEqual({
      kind: "unreachable",
      reason: UNREACHABLE,
    });
  });

  it("reports a server error without showing its body, and logs only the path and status", async () => {
    const warn = vi.spyOn(console, "warn").mockImplementation(() => undefined);
    const { client } = clientReturning(json(500, { detail: "secret stack trace" }));

    expect(await client.getCurrentUser()).toEqual({
      kind: "unreachable",
      reason: "NikolAI had a problem. Try again in a moment.",
    });
    expect(String(warn.mock.calls[0]?.[0])).toContain("/api/me");
    expect(String(warn.mock.calls[0]?.[0])).toContain("500");
    expect(String(warn.mock.calls[0]?.[0])).not.toContain("secret");
    warn.mockRestore();
  });

  it("gives up on a server that never answers", async () => {
    const hanging: FetchLike = (_url, init) =>
      new Promise((_resolve, reject) => {
        init?.signal?.addEventListener("abort", () => {
          reject(new Error("aborted"));
        });
      });

    expect(
      await requestJson(hanging, "https://api.test/api/me", string, { timeoutMs: 20 }),
    ).toEqual({ kind: "unreachable", reason: TIMED_OUT });
  });

  it("builds the sign-in URL and encodes the dashboard origin", () => {
    const { client } = clientReturning(json(200, USER));

    expect(client.signInUrl("https://app.test")).toBe(
      "https://api.test/auth/github/login?return_to=https%3A%2F%2Fapp.test",
    );
  });

  it("creates a course with a JSON body", async () => {
    const { client, calls } = clientReturning(json(201, COURSE));

    expect(await client.createCourse("SOEN 490")).toEqual({ kind: "ok", data: COURSE });
    expect(calls[0]?.url).toBe("https://api.test/api/courses");
    expect(calls[0]?.init?.method).toBe("POST");
    expect(calls[0]?.init?.body).toBe(JSON.stringify({ name: "SOEN 490" }));
    expect(calls[0]?.init?.headers).toEqual({
      Accept: "application/json",
      "Content-Type": "application/json",
    });
  });

  it("reports a rejected course name", async () => {
    expect(await clientReturning(json(422, { detail: [] })).client.createCourse("")).toEqual({
      kind: "unreachable",
      reason: "Check what you entered and try again.",
    });
  });

  it("signs out with a POST and accepts the empty answer", async () => {
    const { client, calls } = clientReturning(new Response(null, { status: 204 }));

    expect(await client.signOut()).toEqual({ kind: "ok", data: null });
    expect(calls[0]?.url).toBe("https://api.test/auth/logout");
    expect(calls[0]?.init?.method).toBe("POST");
  });

  it("doesn't accept a sign-out whose 200 body isn't JSON", async () => {
    const { client } = clientReturning(new Response("not json", { status: 200 }));

    expect(await client.signOut()).toEqual({ kind: "unreachable", reason: UNREADABLE });
  });
});
