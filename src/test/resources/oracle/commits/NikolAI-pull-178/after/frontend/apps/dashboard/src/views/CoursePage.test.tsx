// AI contribution: 50%
/** The course page: join code, teams and members, per role, with only `fetch` mocked. */

import { fireEvent, render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../App";
import { DashboardClient, type FetchLike } from "../lib/api";
import { ALICE_MEMBER, COURSE, MEMBERS, TEAM, USER, json, type Call } from "../test/fixtures";

const SIGN_IN_URL = "https://api.test/auth/github/login?return_to=https%3A%2F%2Fapp.test";
const SERVER_ERROR = "NikolAI had a problem. Try again in a moment.";
const COURSE_ID = COURSE.id;
const TEAM_ID = "88888888-8888-8888-8888-888888888888";
const COURSE_WITH_TEAM = { ...COURSE, teams: [TEAM] };
const TA_COURSE = { ...COURSE_WITH_TEAM, role: "ta", join_code: null };
const TA_USER = { ...USER, is_instructor: false, courses: [TA_COURSE] };
const STUDENT_COURSE = { ...COURSE_WITH_TEAM, role: "student", join_code: null };
const STUDENT_USER = { ...USER, is_instructor: false, courses: [STUDENT_COURSE] };
const INSTRUCTOR_WITH_TEAM = { ...USER, courses: [COURSE_WITH_TEAM] };
const PROMOTED = { ...ALICE_MEMBER, role: "ta" };

function show(...responses: (Response | Error)[]): { calls: Call[] } {
  const calls: Call[] = [];
  let index = 0;
  const fetchImpl: FetchLike = (url, init) => {
    calls.push({ url, init });
    const response = responses[index] ?? responses.at(-1);
    if (response === undefined) throw new Error("No fetch response configured for this test.");
    index += 1;
    return response instanceof Error ? Promise.reject(response) : Promise.resolve(response);
  };
  render(
    <App client={new DashboardClient("https://api.test", fetchImpl)} signInUrl={SIGN_IN_URL} />,
  );
  return { calls };
}

async function openCoursePage(
  user: unknown,
  options: { members?: unknown; course?: unknown; apiPath?: string } = {},
): Promise<Call[]> {
  const members = options.members ?? MEMBERS;
  const fresh = options.course ?? COURSE_WITH_TEAM;
  if (options.apiPath !== undefined) {
    window.history.replaceState(null, "", options.apiPath);
  }
  const { calls } = show(json(200, user), json(200, fresh), json(200, members));
  if (options.apiPath === undefined) {
    fireEvent.click(await screen.findByRole("button", { name: /SOEN 490$/ }));
  }
  await screen.findByRole("heading", { name: "Course: SOEN 490" });
  return calls;
}

async function openCoursePageWithStack(
  user: unknown,
  stack: (Response | Error)[],
): Promise<Call[]> {
  const { calls } = show(
    json(200, user),
    json(200, COURSE_WITH_TEAM),
    json(200, MEMBERS),
    ...stack,
  );
  fireEvent.click(await screen.findByRole("button", { name: /SOEN 490$/ }));
  await screen.findByRole("heading", { name: "Course: SOEN 490" });
  return calls;
}

beforeEach(() => {
  window.history.replaceState(null, "", "/");
});

describe("course page", () => {
  it("shows an instructor the join code, teams and members", async () => {
    await openCoursePage(INSTRUCTOR_WITH_TEAM);

    expect(screen.getByLabelText("Join code").textContent).toBe("ABCD2345");
    expect(screen.getByRole("button", { name: "Copy" })).toBeDefined();
    expect(screen.getByRole("button", { name: "Rotate code…" })).toBeDefined();
    expect(screen.getByRole("button", { name: "Create team" })).toBeDefined();
    expect(screen.getByRole("heading", { name: "Pixel" })).toBeDefined();
    expect(await screen.findAllByRole("button", { name: "Promote alice to TA" })).toHaveLength(1);
    expect(screen.getByText("prof")).toBeDefined();
  });

  it("shows a TA the page read-only", async () => {
    await openCoursePage(TA_USER, { course: TA_COURSE });

    expect(screen.queryByLabelText("Join code")).toBeNull();
    expect(screen.queryByRole("button", { name: "Rotate code…" })).toBeNull();
    expect(screen.queryByRole("button", { name: "Create team" })).toBeNull();
    expect(screen.queryByRole("button", { name: "Promote alice to TA" })).toBeNull();
    expect(screen.getByRole("heading", { name: "Pixel" })).toBeDefined();
    expect(await screen.findByText("alice")).toBeDefined();

    fireEvent.click(screen.getByRole("button", { name: "← Back to your courses" }));

    expect(await screen.findByRole("button", { name: "Open SOEN 490" })).toBeDefined();
  });

  it("shows the freshly fetched teams, not the /api/me snapshot", async () => {
    const TWO_TEAMS = { ...COURSE_WITH_TEAM, teams: [TEAM, { id: TEAM_ID, name: "Alpha" }] };
    await openCoursePage(INSTRUCTOR_WITH_TEAM, { course: TWO_TEAMS });

    expect(screen.getByRole("heading", { name: "Pixel" })).toBeDefined();
    expect(screen.getByRole("heading", { name: "Alpha" })).toBeDefined();
  });

  it("opens from a ?course= deep link and returns to the list on Back", async () => {
    window.history.replaceState(null, "", `/?course=${COURSE_ID}`);
    show(json(200, INSTRUCTOR_WITH_TEAM), json(200, COURSE_WITH_TEAM), json(200, MEMBERS));

    expect(await screen.findByRole("heading", { name: "Course: SOEN 490" })).toBeDefined();

    fireEvent.click(screen.getByRole("button", { name: "← Back to your courses" }));

    expect(await screen.findByRole("heading", { name: "Your courses" })).toBeDefined();
    expect(window.location.search).toBe("");
  });

  it("ignores a ?course= deep link for a non-staff course", async () => {
    window.history.replaceState(null, "", `/?course=${COURSE_ID}`);
    show(json(200, STUDENT_USER));

    expect(
      await screen.findByRole("heading", { name: "This dashboard is for course staff" }),
    ).toBeDefined();
    expect(screen.queryByRole("heading", { name: "Course: SOEN 490" })).toBeNull();
  });

  it("does not offer Manage on a student's only course", async () => {
    show(json(200, STUDENT_USER));

    expect(
      await screen.findByRole("heading", { name: "This dashboard is for course staff" }),
    ).toBeDefined();
    expect(screen.queryByRole("button", { name: /SOEN 490$/ })).toBeNull();
  });

  it("goes back to the course list, and the overview button takes focus", async () => {
    show(
      json(200, INSTRUCTOR_WITH_TEAM),
      json(200, COURSE_WITH_TEAM),
      json(200, MEMBERS),
      json(200, COURSE_WITH_TEAM),
      json(200, MEMBERS),
    );
    fireEvent.click(await screen.findByRole("button", { name: "Manage SOEN 490" }));
    await screen.findByRole("heading", { name: "Course: SOEN 490" });

    fireEvent.click(screen.getByRole("button", { name: "← Back to your courses" }));

    const button = await screen.findByRole("button", { name: "Manage SOEN 490" });
    expect(document.activeElement).toBe(button);
  });

  it("puts focus on the course heading when the page opens", async () => {
    await openCoursePage(INSTRUCTOR_WITH_TEAM);

    expect(document.activeElement).toBe(screen.getByRole("heading", { name: "Course: SOEN 490" }));
  });

  it("copies the join code to the clipboard and announces it", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(window.navigator, "clipboard", {
      value: { writeText },
      configurable: true,
    });
    await openCoursePage(INSTRUCTOR_WITH_TEAM);

    fireEvent.click(screen.getByRole("button", { name: "Copy" }));

    expect(writeText).toHaveBeenCalledWith("ABCD2345");
    expect(await screen.findByRole("status")).toBeDefined();
    expect(screen.getByRole("status").textContent).toContain("Join code copied.");
  });

  it("offers a clean message when the browser refuses the clipboard", async () => {
    const writeText = vi.fn().mockRejectedValue(new Error("denied"));
    Object.defineProperty(window.navigator, "clipboard", {
      value: { writeText },
      configurable: true,
    });
    await openCoursePage(INSTRUCTOR_WITH_TEAM);

    fireEvent.click(screen.getByRole("button", { name: "Copy" }));

    expect((await screen.findByRole("alert")).textContent).toContain(
      "Copying isn't allowed here. The code is selected",
    );
    expect(screen.getByLabelText("Join code").textContent).toBe("ABCD2345");
  });

  it("keeps the code when the rotate is cancelled, and rotates on confirm", async () => {
    const NEW_CODE = "NEWCODE1";
    const { calls } = show(
      json(200, INSTRUCTOR_WITH_TEAM),
      json(200, COURSE_WITH_TEAM),
      json(200, MEMBERS),
      json(200, { join_code: NEW_CODE }),
    );
    fireEvent.click(await screen.findByRole("button", { name: "Manage SOEN 490" }));
    await screen.findByRole("heading", { name: "Course: SOEN 490" });

    fireEvent.click(screen.getByRole("button", { name: "Rotate code…" }));
    fireEvent.click(screen.getByRole("button", { name: "Keep current code" }));
    expect(screen.getByLabelText("Join code").textContent).toBe("ABCD2345");
    expect(calls.filter((call) => call.init?.method === "POST")).toHaveLength(0);
    expect(document.activeElement).toBe(screen.getByRole("button", { name: "Rotate code…" }));

    fireEvent.click(screen.getByRole("button", { name: "Rotate code…" }));
    expect(document.activeElement).toBe(screen.getByRole("button", { name: "Keep current code" }));
    fireEvent.keyDown(screen.getByRole("group", { name: "Rotate the join code" }), {
      key: "Escape",
    });
    expect(screen.queryByRole("button", { name: "Keep current code" })).toBeNull();
    expect(document.activeElement).toBe(screen.getByRole("button", { name: "Rotate code…" }));
    expect(calls.filter((call) => call.init?.method === "POST")).toHaveLength(0);

    fireEvent.click(screen.getByRole("button", { name: "Rotate code…" }));
    fireEvent.click(screen.getByRole("button", { name: "Rotate code" }));

    expect(await screen.findByLabelText("Join code")).toHaveProperty("textContent", NEW_CODE);
    const rotate = calls.find((call) => call.url.endsWith(`/courses/${COURSE_ID}/join-code`));
    expect(rotate?.init?.method).toBe("POST");
    expect(await screen.findByRole("status")).toBeDefined();
  });

  it("shows a clean error and keeps the old code when the rotate fails", async () => {
    await openCoursePageWithStack(INSTRUCTOR_WITH_TEAM, [json(500, {})]);

    fireEvent.click(await screen.findByRole("button", { name: "Rotate code…" }));
    fireEvent.click(screen.getByRole("button", { name: "Rotate code" }));

    expect((await screen.findByRole("alert")).textContent).toContain(SERVER_ERROR);
    expect(screen.getByLabelText("Join code").textContent).toBe("ABCD2345");
  });

  it("sends the user to sign in when the session ends on a write", async () => {
    await openCoursePageWithStack(INSTRUCTOR_WITH_TEAM, [json(401, {})]);

    fireEvent.click(await screen.findByRole("button", { name: "Rotate code…" }));
    fireEvent.click(screen.getByRole("button", { name: "Rotate code" }));

    expect(await screen.findByRole("link", { name: "Sign in with GitHub" })).toBeDefined();
  });

  it("creates a team and announces it", async () => {
    const { calls } = show(
      json(200, INSTRUCTOR_WITH_TEAM),
      json(200, COURSE_WITH_TEAM),
      json(200, MEMBERS),
      json(201, { id: "66666666-6666-6666-6666-666666666666", name: "Alpha" }),
    );
    fireEvent.click(await screen.findByRole("button", { name: "Manage SOEN 490" }));
    await screen.findByRole("heading", { name: "Course: SOEN 490" });

    fireEvent.change(screen.getByLabelText("New team name"), { target: { value: "Alpha" } });
    fireEvent.click(screen.getByRole("button", { name: "Create team" }));

    expect(await screen.findByRole("heading", { name: "Alpha" })).toBeDefined();
    expect((await screen.findByRole("status")).textContent).toContain("Team Alpha created.");
    const create = calls.find((call) => call.url.endsWith(`/courses/${COURSE_ID}/teams`));
    expect(create?.init?.body).toBe(JSON.stringify({ name: "Alpha" }));
  });

  it("refuses a blank team name without calling the server", async () => {
    const { calls } = show(
      json(200, INSTRUCTOR_WITH_TEAM),
      json(200, COURSE_WITH_TEAM),
      json(200, MEMBERS),
    );
    fireEvent.click(await screen.findByRole("button", { name: "Manage SOEN 490" }));
    await screen.findByRole("heading", { name: "Course: SOEN 490" });

    fireEvent.change(screen.getByLabelText("New team name"), { target: { value: "   " } });
    fireEvent.click(screen.getByRole("button", { name: "Create team" }));

    expect((await screen.findByRole("alert")).textContent).toContain("Enter a team name.");
    expect(calls.filter((call) => call.init?.method === "POST")).toHaveLength(0);
  });

  it("keeps the server's alphabetical order when a team is created", async () => {
    await openCoursePageWithStack(INSTRUCTOR_WITH_TEAM, [
      json(201, { id: "77777777-7777-7777-7777-777777777777", name: "Alpha" }),
    ]);

    fireEvent.change(screen.getByLabelText("New team name"), { target: { value: "Alpha" } });
    fireEvent.click(screen.getByRole("button", { name: "Create team" }));

    await screen.findByRole("heading", { name: "Alpha" });
    const teamsHeading = screen.getByRole("heading", { name: "Teams" });
    const teamsCard = teamsHeading.closest("section");
    if (teamsCard === null) throw new Error("Teams card not found");
    const names = within(teamsCard)
      .getAllByRole("heading", { level: 3 })
      .map((heading) => heading.textContent);
    expect(names).toEqual(["Alpha", "Pixel"]);
  });

  it("shows a clean error when the team name is taken", async () => {
    await openCoursePageWithStack(INSTRUCTOR_WITH_TEAM, [json(409, {})]);

    fireEvent.change(screen.getByLabelText("New team name"), { target: { value: "Pixel" } });
    fireEvent.click(screen.getByRole("button", { name: "Create team" }));

    expect((await screen.findByRole("alert")).textContent).toContain(
      "This course already has a team with that name.",
    );
  });

  it("confirms before promoting a member to TA", async () => {
    const { calls } = show(
      json(200, INSTRUCTOR_WITH_TEAM),
      json(200, COURSE_WITH_TEAM),
      json(200, MEMBERS),
      json(200, PROMOTED),
    );
    fireEvent.click(await screen.findByRole("button", { name: "Manage SOEN 490" }));
    await screen.findByRole("heading", { name: "Course: SOEN 490" });

    fireEvent.click(await screen.findByRole("button", { name: "Promote alice to TA" }));

    expect(screen.getByRole("group", { name: "Promote alice" })).toBeDefined();
    expect(calls.filter((call) => call.url.includes("/promote"))).toHaveLength(0);

    fireEvent.click(screen.getByRole("button", { name: "Make TA" }));

    const promote = calls.find((call) => call.url.includes("/promote"));
    expect(promote?.url).toContain(`/members/${ALICE_MEMBER.user_id}/promote`);
    expect(promote?.init?.method).toBe("POST");
    expect((await screen.findByRole("status")).textContent).toContain("alice is now a TA.");
  });

  it("shows a clean error when the promote fails after confirmation", async () => {
    await openCoursePageWithStack(INSTRUCTOR_WITH_TEAM, [json(500, {})]);

    fireEvent.click(await screen.findByRole("button", { name: "Promote alice to TA" }));
    fireEvent.click(screen.getByRole("button", { name: "Make TA" }));

    expect((await screen.findByRole("alert")).textContent).toContain(SERVER_ERROR);
    expect(screen.getByRole("button", { name: "Promote alice to TA" })).toBeDefined();
  });

  it("says so when the member left the course before being promoted", async () => {
    await openCoursePageWithStack(INSTRUCTOR_WITH_TEAM, [json(404, {})]);

    fireEvent.click(await screen.findByRole("button", { name: "Promote alice to TA" }));
    fireEvent.click(screen.getByRole("button", { name: "Make TA" }));

    expect((await screen.findByRole("alert")).textContent).toContain(
      "That person isn't in this course any more.",
    );
  });

  it("says so when the member is already a TA", async () => {
    await openCoursePageWithStack(INSTRUCTOR_WITH_TEAM, [json(409, {})]);

    fireEvent.click(await screen.findByRole("button", { name: "Promote alice to TA" }));
    fireEvent.click(screen.getByRole("button", { name: "Make TA" }));

    expect((await screen.findByRole("alert")).textContent).toContain(
      "They're already a TA, or an instructor.",
    );
  });

  it("moves focus into the promote confirmation and back on cancel", async () => {
    await openCoursePageWithStack(INSTRUCTOR_WITH_TEAM, [json(200, PROMOTED)]);

    const promote = await screen.findByRole("button", { name: "Promote alice to TA" });
    fireEvent.click(promote);

    expect(document.activeElement).toBe(screen.getByRole("button", { name: "Keep as student" }));
    expect(promote).toHaveProperty("disabled", true);

    fireEvent.click(screen.getByRole("button", { name: "Keep as student" }));

    expect(document.activeElement).toBe(promote);
    expect(promote).toHaveProperty("disabled", false);
  });

  it("closes the promote confirmation with Escape and returns focus", async () => {
    await openCoursePage(INSTRUCTOR_WITH_TEAM);

    const promote = await screen.findByRole("button", { name: "Promote alice to TA" });
    fireEvent.click(promote);
    fireEvent.keyDown(screen.getByRole("group", { name: "Promote alice" }), { key: "Escape" });

    expect(screen.queryByRole("group", { name: "Promote alice" })).toBeNull();
    expect(document.activeElement).toBe(promote);
  });

  it("returns focus to the member's row after a successful promote", async () => {
    await openCoursePageWithStack(INSTRUCTOR_WITH_TEAM, [json(200, PROMOTED)]);

    fireEvent.click(await screen.findByRole("button", { name: "Promote alice to TA" }));
    fireEvent.click(screen.getByRole("button", { name: "Make TA" }));

    expect((await screen.findByRole("status")).textContent).toContain("alice is now a TA.");
    expect(document.activeElement).toBe(screen.getByRole("heading", { name: "alice" }));
  });

  it("offers a retry when the members list fails to load", async () => {
    show(
      json(200, INSTRUCTOR_WITH_TEAM),
      json(200, COURSE_WITH_TEAM),
      json(500, {}),
      json(200, MEMBERS),
    );
    fireEvent.click(await screen.findByRole("button", { name: "Manage SOEN 490" }));

    expect((await screen.findByRole("alert")).textContent).toContain(SERVER_ERROR);
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));

    expect(await screen.findByText("alice")).toBeDefined();
  });

  it("goes back to sign in when the session ends mid-page", async () => {
    show(json(200, INSTRUCTOR_WITH_TEAM), json(401, {}));
    fireEvent.click(await screen.findByRole("button", { name: "Manage SOEN 490" }));

    expect(await screen.findByRole("link", { name: "Sign in with GitHub" })).toBeDefined();
  });
});
