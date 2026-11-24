// AI contribution: 50%
/** The student page's join form, through the real App with only `fetch` mocked. */

import { fireEvent, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import { UNREACHABLE } from "../lib/api";
import { SIGN_IN_URL, show } from "../test/dashboard";
import { COURSE, PLANNING, TEAM, USER, json } from "../test/fixtures";

const STUDENT = { ...USER, is_instructor: false, courses: [] };
const JOINED = { ...COURSE, role: "student", join_code: null };

function join(code: string): void {
  fireEvent.change(screen.getByLabelText("Join code"), { target: { value: code } });
  fireEvent.click(screen.getByRole("button", { name: "Join course" }));
}

beforeEach(() => {
  window.history.replaceState(null, "", "/");
});

describe("joining a course with its code", () => {
  it("joins, names the course, says what happens next and clears the code", async () => {
    const { calls } = show(json(200, STUDENT), json(200, JOINED));
    await screen.findByLabelText("Join code");

    join("abcd-2345");

    const status = await screen.findByText(/You joined SOEN 490\./);
    expect(status.closest("[role=status]")).not.toBeNull();
    expect(screen.getByText(/Your instructor will put you on a team/)).toBeDefined();
    expect(calls[1]?.url).toBe("https://api.test/api/courses/join");
    expect(calls[1]?.init?.body).toBe(JSON.stringify({ join_code: "abcd-2345" }));
    expect(screen.getByLabelText<HTMLInputElement>("Join code").value).toBe("");
  });

  it("adds the joined course to the signed-in user without reloading the page", async () => {
    const { calls } = show(json(200, STUDENT), json(200, JOINED), json(200, JOINED));
    await screen.findByLabelText("Join code");

    join("ABCD2345");
    await screen.findByText(/You joined SOEN 490\./);
    join("ABCD2345");

    expect(await screen.findByText(/You're already in SOEN 490\./)).toBeDefined();
    expect(calls.filter((call) => call.url.endsWith("/api/me"))).toHaveLength(1);
  });

  it("says a student is already in a course they join again", async () => {
    show(json(200, { ...STUDENT, courses: [JOINED] }), json(200, JOINED));
    await screen.findByLabelText("Join code");

    join("ABCD2345");

    expect(await screen.findByText(/You're already in SOEN 490\./)).toBeDefined();
    expect(screen.queryByText(/You joined/)).toBeNull();
  });

  it("opens the team's planning page when the joined course already has them on a team", async () => {
    show(json(200, STUDENT), json(200, { ...JOINED, teams: [TEAM] }), json(200, PLANNING));
    await screen.findByLabelText("Join code");

    join("ABCD2345");

    expect(await screen.findByRole("heading", { name: "Pixel planning" })).toBeDefined();
    expect(screen.queryByLabelText("Join code")).toBeNull();
  });

  it("explains a code that matches no course, and keeps it to fix", async () => {
    show(json(200, STUDENT), json(404, { detail: "No course has this join code" }));
    await screen.findByLabelText("Join code");

    join("WRONG123");

    expect((await screen.findByRole("alert")).textContent).toBe(
      "That code doesn't match a course. Check it with your instructor.",
    );
    expect(screen.queryByText(/detail/)).toBeNull();
    expect(screen.getByLabelText<HTMLInputElement>("Join code").value).toBe("WRONG123");
  });

  it("keeps the typed code when the server can't be reached", async () => {
    show(json(200, STUDENT), new TypeError("offline"));
    await screen.findByLabelText("Join code");

    join("ABCD2345");

    expect((await screen.findByRole("alert")).textContent).toBe(UNREACHABLE);
    expect(screen.getByLabelText<HTMLInputElement>("Join code").value).toBe("ABCD2345");
  });

  it("goes back to sign-in when the session has ended", async () => {
    show(json(200, STUDENT), json(401, {}));
    await screen.findByLabelText("Join code");

    join("ABCD2345");

    const link = await screen.findByRole("link", { name: "Sign in with GitHub" });
    expect(link.getAttribute("href")).toBe(SIGN_IN_URL);
  });

  it("asks for a code instead of sending an empty one", async () => {
    const { calls } = show(json(200, STUDENT));
    await screen.findByLabelText("Join code");

    join("   ");

    expect((await screen.findByRole("alert")).textContent).toBe(
      "Enter the join code your instructor shared.",
    );
    expect(calls).toHaveLength(1);
  });

  it("shows progress and sends one request however often the button is pressed", async () => {
    let answer: (response: Response) => void = () => undefined;
    const { calls } = show(json(200, STUDENT), (url) =>
      url.endsWith("/join") ? new Promise<Response>((resolve) => (answer = resolve)) : undefined,
    );
    await screen.findByLabelText("Join code");

    join("ABCD2345");
    const button = await screen.findByRole("button", { name: "Joining…" });
    expect((button as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(button);

    answer(json(200, JOINED));
    await screen.findByText(/You joined SOEN 490\./);
    await waitFor(() => {
      expect(calls.filter((call) => call.url.endsWith("/join"))).toHaveLength(1);
    });
  });
});
