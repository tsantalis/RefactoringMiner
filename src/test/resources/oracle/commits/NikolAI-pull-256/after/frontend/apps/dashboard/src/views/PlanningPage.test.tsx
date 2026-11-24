// AI contribution: 50%
/** The team's planning page (US-19.1): who lands on it and who reads it, with only `fetch` mocked. */

import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import { App } from "../App";
import { DashboardClient, type FetchLike } from "../lib/api";
import { COURSE, PLANNING, TEAM, USER, json, type Call } from "../test/fixtures";

const SIGN_IN_URL = "https://api.test/auth/github/login?return_to=https%3A%2F%2Fapp.test";
const STUDENT_COURSE = { ...COURSE, role: "student", join_code: null, teams: [TEAM] };
const STUDENT = { ...USER, is_instructor: false, courses: [STUDENT_COURSE] };
const INSTRUCTOR = { ...USER, courses: [{ ...COURSE, teams: [TEAM] }] };
const READ_ONLY = { ...PLANNING, can_plan: false };
const PLANNING_PATH = `teams/${TEAM.id}/planning`;
const SECTIONS = ["Brainstorm", "Planning sessions", "Availability", "Burndown"];

/** Answers each request by its path after `/api/`; anything unlisted is an empty list. */
function show(routes: Record<string, Response>): Call[] {
  const calls: Call[] = [];
  const fetchImpl: FetchLike = (url, init) => {
    calls.push({ url, init });
    const path = url.slice("https://api.test/api/".length);
    return Promise.resolve(routes[path]?.clone() ?? json(200, []));
  };
  render(
    <App client={new DashboardClient("https://api.test", fetchImpl)} signInUrl={SIGN_IN_URL} />,
  );
  return calls;
}

afterEach(() => {
  window.history.replaceState(null, "", "/");
});

describe("the planning page", () => {
  it("is where a student on a team lands, with its four sections", async () => {
    const calls = show({ me: json(200, STUDENT), [PLANNING_PATH]: json(200, PLANNING) });

    expect(await screen.findByRole("heading", { name: "Pixel planning" })).toBeDefined();
    for (const section of SECTIONS) {
      expect(screen.getByRole("heading", { name: section })).toBeDefined();
    }
    expect(calls[1]?.url).toBe(`https://api.test/api/teams/${TEAM.id}/planning`);
    expect(screen.queryByText(/read-only/)).toBeNull();
    expect(screen.queryByRole("heading", { name: "Join a course" })).toBeNull();
  });

  it("finds a student's team in a later course when the first has none", async () => {
    const noTeam = { ...STUDENT_COURSE, id: "66666666-6666-6666-6666-666666666666", teams: [] };
    show({
      me: json(200, { ...STUDENT, courses: [noTeam, STUDENT_COURSE] }),
      [PLANNING_PATH]: json(200, PLANNING),
    });

    expect(await screen.findByRole("heading", { name: "Pixel planning" })).toBeDefined();
    expect(screen.queryByRole("heading", { name: "You're not on a team yet" })).toBeNull();
  });

  it("keeps the join form for a student with no course", async () => {
    const calls = show({ me: json(200, { ...STUDENT, courses: [] }) });

    expect(await screen.findByRole("heading", { name: "Join a course" })).toBeDefined();
    expect(screen.queryByRole("heading", { name: "Pixel planning" })).toBeNull();
    expect(calls).toHaveLength(1);
  });

  it("lands a student who is also staff elsewhere on their team, with a way to their courses", async () => {
    const taCourse = { ...COURSE, id: "77777777-7777-7777-7777-777777777777", role: "ta" };
    show({
      me: json(200, { ...STUDENT, courses: [STUDENT_COURSE, taCourse] }),
      [PLANNING_PATH]: json(200, PLANNING),
    });

    expect(await screen.findByRole("heading", { name: "Pixel planning" })).toBeDefined();
    expect(window.location.search).toBe(`?team=${TEAM.id}`);

    fireEvent.click(screen.getByRole("button", { name: "← Back to your courses" }));

    expect(await screen.findByRole("heading", { name: "Your courses" })).toBeDefined();
    expect(window.location.search).toBe("");
  });

  it("keeps a mixed-role user's ?course= link instead of moving them to their team", async () => {
    const taCourse = { ...COURSE, id: "77777777-7777-7777-7777-777777777777", role: "ta" };
    window.history.replaceState(null, "", `/?course=${taCourse.id}`);
    const calls = show({
      me: json(200, { ...STUDENT, courses: [STUDENT_COURSE, taCourse] }),
      [`courses/${taCourse.id}`]: json(200, taCourse),
    });

    expect(await screen.findByRole("heading", { name: "Course: SOEN 490" })).toBeDefined();
    expect(calls.some((call) => call.url.endsWith("/planning"))).toBe(false);
  });

  it("opens read-only for staff from a ?team= link, and goes back to their courses", async () => {
    window.history.replaceState(null, "", `/?team=${TEAM.id}`);
    show({ me: json(200, INSTRUCTOR), [PLANNING_PATH]: json(200, READ_ONLY) });

    expect(await screen.findByRole("heading", { name: "Pixel planning" })).toBeDefined();
    expect(screen.getByText(/You're viewing this team's planning read-only/)).toBeDefined();

    fireEvent.click(screen.getByRole("button", { name: "← Back to your courses" }));

    expect(await screen.findByRole("heading", { name: "Your courses" })).toBeDefined();
    expect(window.location.search).toBe("");
  });

  it("opens from the course page's team list", async () => {
    show({
      me: json(200, INSTRUCTOR),
      [`courses/${COURSE.id}`]: json(200, { ...COURSE, teams: [TEAM] }),
      [PLANNING_PATH]: json(200, READ_ONLY),
    });
    fireEvent.click(await screen.findByRole("button", { name: /SOEN 490$/ }));

    fireEvent.click(await screen.findByRole("button", { name: "Open planning for Pixel" }));

    expect(await screen.findByRole("heading", { name: "Pixel planning" })).toBeDefined();
    expect(window.location.search).toBe(`?course=${COURSE.id}&team=${TEAM.id}`);
  });

  it("says so cleanly when the viewer can't read the team", async () => {
    window.history.replaceState(null, "", `/?team=${TEAM.id}`);
    show({
      me: json(200, INSTRUCTOR),
      [PLANNING_PATH]: json(403, { detail: "You don't have access to this team" }),
    });

    expect((await screen.findByRole("alert")).textContent).toBe(
      "You don't have access to this team's planning.",
    );
  });
});
