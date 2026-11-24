// AI contribution: 50%
/** The requirements page, per role, with only `fetch` mocked. */

import { fireEvent, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { COURSE, GRANTS, MEMBERS, USER, json, type Call } from "../test/fixtures";
import {
  SERVER_ERROR,
  COURSE_WITH_TEAM,
  INSTRUCTOR,
  TA_COURSE,
  TA_USER,
  STAKEHOLDER_COURSE,
  STAKEHOLDER_USER,
  show,
  type FetchInterceptor,
} from "../test/dashboard";

const COURSE_TEXT = "Course rules: every story needs acceptance criteria.";
const PROJECT_TEXT = "The product must remind residents to recycle.";

async function openRequirementsPage(
  user: unknown,
  options: {
    course?: unknown;
    courseText?: string;
    projectText?: string;
    courseResponse?: Response | Error;
    stack?: (Response | Error)[];
    intercept?: FetchInterceptor;
  } = {},
): Promise<Call[]> {
  const fresh = options.course ?? COURSE_WITH_TEAM;
  const role = (fresh as { role: string }).role;
  const { calls } = show(
    json(200, user),
    json(200, fresh),
    ...(role === "stakeholder" ? [] : [json(200, MEMBERS)]),
    ...(role === "instructor" ? [json(200, GRANTS)] : []),
    options.courseResponse ?? json(200, { text: options.courseText ?? COURSE_TEXT }),
    json(200, { text: options.projectText ?? PROJECT_TEXT }),
    ...(options.stack ?? []),
    ...(options.intercept === undefined ? [] : [options.intercept]),
  );
  fireEvent.click(await screen.findByRole("button", { name: /SOEN 490$/ }));
  await screen.findByRole("heading", { name: "Course: SOEN 490" });
  fireEvent.click(screen.getByRole("button", { name: "Requirements" }));
  await screen.findByRole("heading", { name: "Requirements" });
  return calls;
}

function textareaValue(label: string): string {
  return screen.getByLabelText<HTMLTextAreaElement>(label).value;
}

function saveButton(name: string): HTMLButtonElement {
  return screen.getByRole<HTMLButtonElement>("button", { name });
}

beforeEach(() => {
  window.history.replaceState(null, "", "/");
});

describe("requirements page", () => {
  it("shows an instructor both texts in editors", async () => {
    const calls = await openRequirementsPage(INSTRUCTOR);

    expect(textareaValue("Course requirements")).toBe(COURSE_TEXT);
    expect(textareaValue("Project requirements")).toBe(PROJECT_TEXT);
    expect(saveButton("Save course requirements").disabled).toBe(true);
    const paths = calls.map((call) => call.url);
    expect(paths.some((url) => url.endsWith("/course-requirements"))).toBe(true);
    expect(paths.some((url) => url.endsWith("/project-requirements"))).toBe(true);
  });

  it("shows a stakeholder the same editors without the staff-only course surfaces", async () => {
    await openRequirementsPage(STAKEHOLDER_USER, { course: STAKEHOLDER_COURSE });

    expect(textareaValue("Course requirements")).toBe(COURSE_TEXT);
    expect(textareaValue("Project requirements")).toBe(PROJECT_TEXT);
    expect(screen.queryByLabelText("Join code")).toBeNull();
    expect(screen.queryByRole("heading", { name: "Members" })).toBeNull();
  });

  it("shows a TA both texts read-only", async () => {
    await openRequirementsPage(TA_USER, { course: TA_COURSE });

    expect(screen.queryByLabelText("Course requirements")).toBeNull();
    expect(screen.queryByLabelText("Project requirements")).toBeNull();
    expect(screen.queryByRole("button", { name: /Save / })).toBeNull();
    expect(screen.getByText(COURSE_TEXT)).toBeDefined();
    expect(screen.getByText(PROJECT_TEXT)).toBeDefined();
  });

  it("shows a TA an empty state for each text that isn't set", async () => {
    await openRequirementsPage(TA_USER, { course: TA_COURSE, courseText: "", projectText: "" });

    expect(screen.getByText("No course requirements have been set yet.")).toBeDefined();
    expect(screen.getByText("No project requirements have been set yet.")).toBeDefined();
  });

  it("saves the course requirements through the requirement endpoints", async () => {
    const calls = await openRequirementsPage(INSTRUCTOR, {
      stack: [json(200, { text: "Rewritten rules" })],
    });

    fireEvent.change(screen.getByLabelText("Course requirements"), {
      target: { value: "Rewritten rules" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save course requirements" }));

    expect(await screen.findByText("Course requirements saved.")).toBeDefined();
    const put = calls.find((call) => call.init?.method === "PUT");
    expect(put?.url).toContain("/course-requirements");
    expect(put?.init?.body).toBe(JSON.stringify({ text: "Rewritten rules" }));
    expect(textareaValue("Project requirements")).toBe(PROJECT_TEXT);
    expect(saveButton("Save course requirements").disabled).toBe(true);
  });

  it("saves the project requirements through the requirement endpoints", async () => {
    const calls = await openRequirementsPage(INSTRUCTOR, {
      stack: [json(200, { text: "Rewritten PRD" })],
    });

    fireEvent.change(screen.getByLabelText("Project requirements"), {
      target: { value: "Rewritten PRD" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save project requirements" }));

    expect(await screen.findByText("Project requirements saved.")).toBeDefined();
    const put = calls.find((call) => call.init?.method === "PUT");
    expect(put?.url).toContain("/project-requirements");
    expect(put?.init?.body).toBe(JSON.stringify({ text: "Rewritten PRD" }));
  });

  it("keeps the draft and shows a clean message when the save is refused", async () => {
    const calls = await openRequirementsPage(INSTRUCTOR, { stack: [json(403, {})] });

    fireEvent.change(screen.getByLabelText("Course requirements"), {
      target: { value: "My new rules" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save course requirements" }));

    expect((await screen.findByRole("alert")).textContent).toContain(
      "Only an instructor or a stakeholder",
    );
    expect(textareaValue("Course requirements")).toBe("My new rules");
    expect(calls.some((call) => call.init?.method === "PUT")).toBe(true);
  });

  it("keeps the draft and shows a clean message when the server fails", async () => {
    await openRequirementsPage(INSTRUCTOR, { stack: [json(500, { detail: "stack" })] });

    fireEvent.change(screen.getByLabelText("Project requirements"), {
      target: { value: "My new PRD" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save project requirements" }));

    expect((await screen.findByRole("alert")).textContent).toBe(SERVER_ERROR);
    expect(textareaValue("Project requirements")).toBe("My new PRD");
  });

  it("blocks an over-long text without calling the server", async () => {
    const calls = await openRequirementsPage(INSTRUCTOR);
    const callsBeforeTyping = calls.length;

    fireEvent.change(screen.getByLabelText("Course requirements"), {
      target: { value: "x".repeat(100_001) },
    });

    expect(screen.getByText("100,001 / 100,000 characters")).toBeDefined();
    expect(saveButton("Save course requirements").disabled).toBe(true);
    expect(calls).toHaveLength(callsBeforeTyping);
  });

  it("shows a read-failure message without a pointless retry when the user can't read the text", async () => {
    const courseResponse = json(403, {});
    await openRequirementsPage(INSTRUCTOR, { courseResponse });

    expect((await screen.findByRole("alert")).textContent).toBe(
      "You can't read this course's requirements.",
    );
    expect(screen.queryByRole("button", { name: "Try again" })).toBeNull();
  });

  it("shows a not-found message when the course is gone", async () => {
    const courseResponse = json(404, {});
    await openRequirementsPage(INSTRUCTOR, { courseResponse });

    expect((await screen.findByRole("alert")).textContent).toBe("That course no longer exists.");
    expect(screen.queryByRole("button", { name: "Try again" })).toBeNull();
  });

  it("offers a retry when a text fails to load, and recovers", async () => {
    const calls = await openRequirementsPage(INSTRUCTOR, {
      courseResponse: json(500, {}),
      stack: [json(200, { text: "Recovered rules" })],
    });

    expect((await screen.findByRole("alert")).textContent).toBe(SERVER_ERROR);
    expect(textareaValue("Project requirements")).toBe(PROJECT_TEXT);

    fireEvent.click(screen.getByRole("button", { name: "Try again" }));

    expect((await screen.findByLabelText<HTMLTextAreaElement>("Course requirements")).value).toBe(
      "Recovered rules",
    );
    expect(calls.filter((call) => call.url.endsWith("/course-requirements"))).toHaveLength(2);
  });

  it("blocks closing the tab with unsaved changes, and stops blocking after a save", async () => {
    await openRequirementsPage(INSTRUCTOR, { stack: [json(200, { text: "Saved rules" })] });

    fireEvent.change(screen.getByLabelText("Course requirements"), {
      target: { value: "Unsaved rules" },
    });
    const dirtyAttempt = new Event("beforeunload", { cancelable: true });
    window.dispatchEvent(dirtyAttempt);
    expect(dirtyAttempt.defaultPrevented).toBe(true);

    fireEvent.click(screen.getByRole("button", { name: "Save course requirements" }));
    await screen.findByText("Course requirements saved.");
    const savedAttempt = new Event("beforeunload", { cancelable: true });
    window.dispatchEvent(savedAttempt);
    expect(savedAttempt.defaultPrevented).toBe(false);
  });

  it("keeps the draft when the back button's discard is declined", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(false);
    await openRequirementsPage(INSTRUCTOR);

    fireEvent.change(screen.getByLabelText("Course requirements"), {
      target: { value: "Unsaved rules" },
    });
    fireEvent.click(screen.getByRole("button", { name: /Back to SOEN 490/ }));

    expect(confirmSpy).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("heading", { name: "Requirements" })).toBeDefined();
    expect(textareaValue("Course requirements")).toBe("Unsaved rules");
  });

  it("leaves the page when the back button's discard is confirmed", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    // The course page remounts on the way back and fetches the course and the lists again.
    await openRequirementsPage(INSTRUCTOR, {
      stack: [json(200, COURSE_WITH_TEAM), json(200, MEMBERS), json(200, GRANTS)],
    });

    fireEvent.change(screen.getByLabelText("Project requirements"), {
      target: { value: "Unsaved PRD" },
    });
    fireEvent.click(screen.getByRole("button", { name: /Back to SOEN 490/ }));

    expect(confirmSpy).toHaveBeenCalledTimes(1);
    expect(await screen.findByRole("heading", { name: "Course: SOEN 490" })).toBeDefined();
  });

  it("keeps what was typed while a save was in flight", async () => {
    let resolvePut: (response: Response) => void = () => {
      throw new Error("PUT not started");
    };
    const putPromise = new Promise<Response>((resolve) => {
      resolvePut = resolve;
    });
    let putInit: RequestInit | undefined;
    await openRequirementsPage(INSTRUCTOR, {
      intercept: (url, init) => {
        if (init?.method === "PUT") {
          putInit = init;
          return putPromise;
        }
        return undefined;
      },
    });

    fireEvent.change(screen.getByLabelText("Course requirements"), {
      target: { value: "Rewritten rules" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save course requirements" }));
    // Type while the save is in flight.
    fireEvent.change(screen.getByLabelText("Course requirements"), {
      target: { value: "Rewritten rules and more" },
    });
    resolvePut(json(200, { text: "Rewritten rules" }));

    expect(await screen.findByText("Course requirements saved.")).toBeDefined();
    expect(JSON.parse(typeof putInit?.body === "string" ? putInit.body : "")).toEqual({
      text: "Rewritten rules",
    });
    expect(textareaValue("Course requirements")).toBe("Rewritten rules and more");
  });

  it("keeps the editor and the draft when the save finds the session ended", async () => {
    await openRequirementsPage(INSTRUCTOR, { stack: [json(401, {})] });

    fireEvent.change(screen.getByLabelText("Course requirements"), {
      target: { value: "Unsaved rules" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save course requirements" }));

    expect((await screen.findByRole("alert")).textContent).toBe(
      "Your session ended. Copy your text, then sign in again.",
    );
    expect(screen.queryByRole("link", { name: "Sign in with GitHub" })).toBeNull();
    expect(textareaValue("Course requirements")).toBe("Unsaved rules");
  });

  it("asks before signing out with unsaved changes, and keeps the draft when declined", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(false);
    await openRequirementsPage(INSTRUCTOR, {
      stack: [new Response(null, { status: 204 })],
    });

    fireEvent.change(screen.getByLabelText("Course requirements"), {
      target: { value: "Unsaved rules" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Sign out" }));

    expect(confirmSpy).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("link", { name: "Sign in with GitHub" })).toBeNull();
    expect(textareaValue("Course requirements")).toBe("Unsaved rules");
  });

  it("asks before signing out with unsaved changes, and leaves when confirmed", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    await openRequirementsPage(INSTRUCTOR, {
      stack: [new Response(null, { status: 204 })],
    });

    fireEvent.change(screen.getByLabelText("Course requirements"), {
      target: { value: "Unsaved rules" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Sign out" }));

    expect(confirmSpy).toHaveBeenCalledTimes(1);
    expect(await screen.findByRole("link", { name: "Sign in with GitHub" })).toBeDefined();
  });

  it("counts code points after trimming, like the server", async () => {
    await openRequirementsPage(INSTRUCTOR);

    fireEvent.change(screen.getByLabelText("Course requirements"), {
      target: { value: "😀😀" },
    });
    expect(screen.getByText("2 / 100,000 characters")).toBeDefined();

    fireEvent.change(screen.getByLabelText("Course requirements"), {
      target: { value: "hi   " },
    });
    expect(screen.getByText("2 / 100,000 characters")).toBeDefined();
  });

  it("does not ask about leaving when there is nothing unsaved", async () => {
    const confirmSpy = vi.spyOn(window, "confirm");
    await openRequirementsPage(INSTRUCTOR, {
      stack: [json(200, COURSE_WITH_TEAM), json(200, MEMBERS), json(200, GRANTS)],
    });

    fireEvent.click(screen.getByRole("button", { name: /Back to SOEN 490/ }));

    expect(confirmSpy).not.toHaveBeenCalled();
    expect(await screen.findByRole("heading", { name: "Course: SOEN 490" })).toBeDefined();
  });

  it("keeps the edits when the browser's back button would leave the page", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(false);
    await openRequirementsPage(INSTRUCTOR);

    fireEvent.change(screen.getByLabelText("Course requirements"), {
      target: { value: "Unsaved rules" },
    });
    window.history.replaceState(null, "", `/?course=${COURSE.id}`);
    fireEvent(window, new Event("popstate"));

    expect(confirmSpy).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("heading", { name: "Requirements" })).toBeDefined();
    expect(window.location.search).toContain("page=requirements");
    expect(textareaValue("Course requirements")).toBe("Unsaved rules");
  });

  it("keeps the unsaved-changes guard armed when Back returns to a requirements entry", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(false);
    await openRequirementsPage(INSTRUCTOR);

    fireEvent.change(screen.getByLabelText("Course requirements"), {
      target: { value: "Unsaved rules" },
    });
    // First Back: the browser moves to the course page; the declined guard restores the URL.
    window.history.replaceState(null, "", `/?course=${COURSE.id}`);
    fireEvent(window, new Event("popstate"));
    expect(confirmSpy).toHaveBeenCalledTimes(1);

    // Second Back: the entry the guard restored still points at the requirements page.
    window.history.replaceState(null, "", `/?course=${COURSE.id}&page=requirements`);
    fireEvent(window, new Event("popstate"));
    expect(confirmSpy).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("heading", { name: "Requirements" })).toBeDefined();

    // Third Back: leaving again asks again — the guard was not disarmed in between.
    window.history.replaceState(null, "", `/?course=${COURSE.id}`);
    fireEvent(window, new Event("popstate"));
    expect(confirmSpy).toHaveBeenCalledTimes(2);
    expect(screen.getByRole("heading", { name: "Requirements" })).toBeDefined();
  });

  it("ignores a requirements deep link for a student-role course", async () => {
    const studentCourse = { ...COURSE_WITH_TEAM, role: "student", join_code: null };
    const studentUser = { ...USER, is_instructor: false, courses: [studentCourse] };
    window.history.replaceState(null, "", `/?course=${COURSE.id}&page=requirements`);
    show(json(200, studentUser));

    expect(
      await screen.findByRole("heading", { name: "This dashboard is for course staff" }),
    ).toBeDefined();
    expect(screen.queryByRole("heading", { name: "Requirements" })).toBeNull();
  });

  it("discards the edits when the browser's back button is confirmed", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    await openRequirementsPage(INSTRUCTOR, {
      stack: [json(200, COURSE_WITH_TEAM), json(200, MEMBERS), json(200, GRANTS)],
    });

    fireEvent.change(screen.getByLabelText("Course requirements"), {
      target: { value: "Unsaved rules" },
    });
    window.history.replaceState(null, "", `/?course=${COURSE.id}`);
    fireEvent(window, new Event("popstate"));

    expect(confirmSpy).toHaveBeenCalledTimes(1);
    expect(await screen.findByRole("heading", { name: "Course: SOEN 490" })).toBeDefined();
  });
});
