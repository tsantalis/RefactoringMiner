// AI contribution: 50%
/** Putting a member on a team from the members list (#214), with only `fetch` mocked. */

import { fireEvent, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import {
  COURSE_WITH_TEAM,
  INSTRUCTOR,
  SERVER_ERROR,
  TA_COURSE,
  TA_USER,
  TEAM_ID,
  show,
} from "../test/dashboard";
import {
  ALICE_MEMBER,
  GRANTS,
  PROF_MEMBER,
  STAKEHOLDER_MEMBER,
  TEAM,
  TEAM_DETAIL,
  json,
  type Call,
} from "../test/fixtures";

const WAVE = { id: "77777777-7777-7777-7777-777777777777", name: "Wave" };
const TWO_TEAMS = { ...COURSE_WITH_TEAM, teams: [TEAM, WAVE] };
const ALICE_ON_PIXEL = { ...ALICE_MEMBER, team: TEAM };

async function openMembers(
  user: unknown,
  course: unknown,
  members: unknown[],
  ...extra: Response[]
): Promise<Call[]> {
  const isInstructor = (course as { role: string }).role === "instructor";
  const { calls } = show(
    json(200, user),
    json(200, course),
    json(200, members),
    ...(isInstructor ? [json(200, GRANTS)] : []),
    ...extra,
  );
  fireEvent.click(await screen.findByRole("button", { name: /SOEN 490$/ }));
  await screen.findByRole("heading", { name: "Course: SOEN 490" });
  return calls;
}

beforeEach(() => {
  window.history.replaceState(null, "", "/");
});

describe("member teams", () => {
  it("lets an instructor put a student with no team on one, after confirming", async () => {
    const user = { ...INSTRUCTOR, courses: [TWO_TEAMS] };
    const calls = await openMembers(
      user,
      TWO_TEAMS,
      [ALICE_MEMBER, PROF_MEMBER],
      json(200, TEAM_DETAIL),
    );
    const picker = await screen.findByRole<HTMLSelectElement>("combobox", {
      name: "Team for alice",
    });
    expect(picker.value).toBe("");

    fireEvent.change(picker, { target: { value: TEAM_ID } });

    expect(screen.getByRole("group", { name: "Put alice on a team" })).toBeDefined();
    expect(calls.filter((call) => call.url.includes("/teams/"))).toHaveLength(0);
    fireEvent.click(screen.getByRole("button", { name: "Move to Pixel" }));

    expect((await screen.findByRole("status")).textContent).toContain("alice is now on Pixel.");
    const move = calls.find((call) => call.url.endsWith(`/teams/${TEAM_ID}/members`));
    expect(move?.init?.method).toBe("POST");
    expect(move?.init?.body).toBe(JSON.stringify({ github_login: "alice" }));
    expect(picker.value).toBe(TEAM_ID);
  });

  it("says a move takes the student off their current team, and keeps it on cancel", async () => {
    const user = { ...INSTRUCTOR, courses: [TWO_TEAMS] };
    const calls = await openMembers(user, TWO_TEAMS, [ALICE_ON_PIXEL, PROF_MEMBER]);
    const picker = await screen.findByRole<HTMLSelectElement>("combobox", {
      name: "Team for alice",
    });

    fireEvent.change(picker, { target: { value: WAVE.id } });

    expect(screen.getByText(/leave Pixel/)).toBeDefined();
    fireEvent.click(screen.getByRole("button", { name: "Keep on Pixel" }));
    expect(screen.queryByRole("group", { name: "Put alice on a team" })).toBeNull();
    expect(picker.value).toBe(TEAM_ID);
    expect(calls.filter((call) => call.url.includes("/teams/"))).toHaveLength(0);
  });

  it("shows a clean error when the move fails", async () => {
    const user = { ...INSTRUCTOR, courses: [TWO_TEAMS] };
    await openMembers(user, TWO_TEAMS, [ALICE_MEMBER, PROF_MEMBER], json(500, {}));

    const picker = await screen.findByRole<HTMLSelectElement>("combobox", {
      name: "Team for alice",
    });
    fireEvent.change(picker, { target: { value: TEAM_ID } });
    fireEvent.click(screen.getByRole("button", { name: "Move to Pixel" }));

    expect((await screen.findByRole("alert")).textContent).toBe(SERVER_ERROR);
    expect(picker.value).toBe("");
  });

  it("tells the instructor a TA keeps reading every team when moved", async () => {
    const user = { ...INSTRUCTOR, courses: [TWO_TEAMS] };
    const tina = { ...ALICE_ON_PIXEL, github_login: "tina", role: "ta" };
    await openMembers(user, TWO_TEAMS, [tina, PROF_MEMBER]);

    fireEvent.change(await screen.findByRole("combobox", { name: "Team for tina" }), {
      target: { value: WAVE.id },
    });

    expect(screen.getByText(/still read every team/)).toBeDefined();
    expect(screen.queryByText(/stop seeing/)).toBeNull();
  });

  it("shows a TA each member's team without a picker", async () => {
    await openMembers(TA_USER, TA_COURSE, [ALICE_ON_PIXEL, { ...PROF_MEMBER }]);

    expect(await screen.findByText("Team: Pixel")).toBeDefined();
    expect(screen.queryByRole("combobox")).toBeNull();
  });

  it("gives a stakeholder no team picker", async () => {
    await openMembers(INSTRUCTOR, COURSE_WITH_TEAM, [
      STAKEHOLDER_MEMBER,
      ALICE_MEMBER,
      PROF_MEMBER,
    ]);

    expect(await screen.findByRole("combobox", { name: "Team for alice" })).toBeDefined();
    expect(screen.queryByRole("combobox", { name: "Team for product-owner" })).toBeNull();
  });
});
