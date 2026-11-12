// AI contribution: 50%
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { App } from "./App";
import {
  DashboardClient,
  UNREADABLE,
  UNREACHABLE,
  REQUEST_FAILED,
  type FetchLike,
} from "./lib/api";
import { COURSE_NOT_LINKED } from "./lib/roster-notice";
import { COURSE, USER, json, type Call } from "./test/fixtures";

const SIGN_IN_URL = "https://api.test/auth/github/login?return_to=https%3A%2F%2Fapp.test";
const SERVER_ERROR = "NikolAI had a problem. Try again in a moment.";

function show(...responses: (Response | Error)[]): { client: DashboardClient; calls: Call[] } {
  const calls: Call[] = [];
  let index = 0;
  const fetchImpl: FetchLike = (url, init) => {
    calls.push({ url, init });
    const response = responses[index] ?? responses.at(-1);
    if (response === undefined) throw new Error("No fetch response configured for this test.");
    index += 1;
    return response instanceof Error ? Promise.reject(response) : Promise.resolve(response);
  };
  const client = new DashboardClient("https://api.test", fetchImpl);
  render(<App client={client} signInUrl={SIGN_IN_URL} />);
  return { client, calls };
}

function typeCourseName(name: string): void {
  fireEvent.change(screen.getByLabelText("Course name"), { target: { value: name } });
  fireEvent.click(screen.getByRole("button", { name: "Create course" }));
}

describe("dashboard", () => {
  it("asks a signed-out visitor to sign in with GitHub", async () => {
    show(json(401, {}));

    const link = await screen.findByRole("link", { name: "Sign in with GitHub" });
    expect(link.getAttribute("href")).toBe(SIGN_IN_URL);
  });

  it("lets an instructor create their first course", async () => {
    show(json(200, USER));

    expect(await screen.findByRole("heading", { name: "Create a course" })).toBeDefined();
    expect(screen.getByText("You don't have any courses yet.")).toBeDefined();
  });

  it("shows an instructor their courses and the create form", async () => {
    show(json(200, { ...USER, courses: [COURSE] }));

    expect(await screen.findByRole("heading", { name: "SOEN 490" })).toBeDefined();
    expect(screen.getByText("Instructor · 0 teams")).toBeDefined();
    expect(screen.getByRole("heading", { name: "Create a course" })).toBeDefined();
  });

  it("tells a student the dashboard is for course staff", async () => {
    show(json(200, { ...USER, is_instructor: false, courses: [{ ...COURSE, role: "student" }] }));

    expect(
      await screen.findByRole("heading", { name: "This dashboard is for course staff" }),
    ).toBeDefined();
    expect(screen.getByText(/Use the NikolAI extension/)).toBeDefined();
    expect(screen.getByText(/how to install it/)).toBeDefined();
    expect(screen.queryByRole("link", { name: "NikolAI extension" })).toBeNull();
    expect(screen.queryByRole("heading", { name: "Create a course" })).toBeNull();
  });

  it("tells a brand-new student with no courses the same thing", async () => {
    show(json(200, { ...USER, is_instructor: false, courses: [] }));

    expect(
      await screen.findByRole("heading", { name: "This dashboard is for course staff" }),
    ).toBeDefined();
    expect(screen.queryByRole("heading", { name: "Your courses" })).toBeNull();
  });

  it("shows the notice the API sends when the roster claim failed", async () => {
    const client = new DashboardClient("https://api.test", () => Promise.resolve(json(200, USER)));
    render(<App client={client} signInUrl={SIGN_IN_URL} notice={COURSE_NOT_LINKED} />);

    expect((await screen.findByRole("alert")).textContent).toBe(COURSE_NOT_LINKED);
  });

  it("shows a TA their courses without the create form", async () => {
    show(json(200, { ...USER, is_instructor: false, courses: [{ ...COURSE, role: "ta" }] }));

    expect(await screen.findByRole("heading", { name: "SOEN 490" })).toBeDefined();
    expect(screen.getByText("TA · 0 teams")).toBeDefined();
    expect(screen.queryByRole("heading", { name: "Create a course" })).toBeNull();
  });

  it("adds a created course to the list", async () => {
    const { calls } = show(json(200, USER), json(201, COURSE));
    await screen.findByRole("heading", { name: "Create a course" });

    typeCourseName("SOEN 490");

    expect(await screen.findByRole("heading", { name: "SOEN 490" })).toBeDefined();
    expect(calls[1]?.init?.body).toBe(JSON.stringify({ name: "SOEN 490" }));
  });

  it("keeps the form and explains a rejected course", async () => {
    show(json(200, USER), json(403, {}));
    await screen.findByRole("heading", { name: "Create a course" });

    typeCourseName("SOEN 490");

    expect((await screen.findByRole("alert")).textContent).toContain(
      "Only instructors can create courses.",
    );
    expect(screen.getByRole("heading", { name: "Create a course" })).toBeDefined();
  });

  it("refuses a blank course name without calling the server", async () => {
    const { calls } = show(json(200, USER));
    await screen.findByRole("heading", { name: "Create a course" });

    typeCourseName("   ");

    expect((await screen.findByRole("alert")).textContent).toContain("Enter a course name.");
    expect(calls).toHaveLength(1);
  });

  it("shows a clean error for an unreadable response", async () => {
    show(json(200, { id: "not-a-user" }));

    expect(await screen.findByRole("heading", { name: "Can't reach NikolAI" })).toBeDefined();
    expect(screen.getByText(UNREADABLE)).toBeDefined();
    expect(screen.getByRole("button", { name: "Try again" })).toBeDefined();
  });

  it.each([500, 503])("offers a retry when /api/me answers %i", async (status) => {
    show(json(status, {}));

    expect(await screen.findByText(SERVER_ERROR)).toBeDefined();
    expect(screen.getByRole("main")).toBeDefined();
    expect(screen.getByRole("button", { name: "Try again" })).toBeDefined();
  });

  it("offers a retry when something in front of the API blocks /api/me", async () => {
    show(json(403, {}));

    expect(await screen.findByText(REQUEST_FAILED)).toBeDefined();
    expect(screen.getByRole("button", { name: "Try again" })).toBeDefined();
  });

  it("reports a network failure and recovers when the user retries", async () => {
    show(new TypeError("offline"), json(200, USER));

    expect(await screen.findByText(UNREACHABLE)).toBeDefined();
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));

    expect(await screen.findByRole("heading", { name: "Create a course" })).toBeDefined();
  });

  it("returns to the sign-in screen when the session expires during a create", async () => {
    show(json(200, USER), json(401, {}));
    await screen.findByRole("heading", { name: "Create a course" });

    typeCourseName("SOEN 490");

    expect(await screen.findByRole("link", { name: "Sign in with GitHub" })).toBeDefined();
  });

  it("keeps the typed name when a create fails in the server", async () => {
    show(json(200, USER), json(500, {}));
    await screen.findByRole("heading", { name: "Create a course" });

    typeCourseName("SOEN 490");

    expect((await screen.findByRole("alert")).textContent).toBe(SERVER_ERROR);
    expect(screen.getByLabelText<HTMLInputElement>("Course name").value).toBe("SOEN 490");
    expect(screen.getByRole("heading", { name: "Create a course" })).toBeDefined();
  });

  it("returns to the sign-in screen after signing out", async () => {
    show(json(200, USER), new Response(null, { status: 204 }));
    await screen.findByRole("heading", { name: "Create a course" });

    fireEvent.click(screen.getByRole("button", { name: "Sign out" }));

    expect(await screen.findByRole("link", { name: "Sign in with GitHub" })).toBeDefined();
  });

  it("says when signing out fails", async () => {
    show(json(200, USER), json(500, {}));
    await screen.findByRole("heading", { name: "Create a course" });

    fireEvent.click(screen.getByRole("button", { name: "Sign out" }));

    expect((await screen.findByRole("alert")).textContent).toContain("Couldn't sign out.");
  });
});
