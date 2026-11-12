// AI contribution: 50%
import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { BacklogCoverage, TeamOverview } from "../../shared/api/views-contract";
import type { GitHubPage } from "../../shared/page";
import { clientsFor, DEFAULT_SETTINGS } from "../../shared/settings";
import { App } from "../App";

const REPO: GitHubPage = { kind: "repo", owner: "team-alpha", repo: "sandbox" };
const API = "https://nikolai.example.ca/api/repos/team-alpha/sandbox";
const STORY_URL = "https://github.com/team-alpha/sandbox/issues/12";
const viewer = { login: "alice", role: "student" } as const;

const TEAM: TeamOverview = {
  viewer,
  team: { name: "Pixel", course: "SOEN 490" },
  iteration: null,
  next_deliverables: [],
  stats: {
    open_checklist_items: null,
    refactorings: 0,
    refactorings_this_week: 0,
    reviewed_pulls: 0,
    merged_pulls: 0,
  },
  pulls: [],
  members: [],
  badges: { earned: [], next: null },
  staff: null,
};

function coverage(overrides: Partial<BacklogCoverage>): BacklogCoverage {
  return {
    viewer,
    status: "completed",
    reason: null,
    checked_at: "2026-11-08T12:00:00Z",
    requirements: [],
    ...overrides,
  };
}

const NOT_RUN = coverage({ status: "not_run", checked_at: null });
const CHECKED = coverage({
  requirements: [
    {
      requirement: "Students sign in with GitHub.",
      stories: [{ number: 12, title: "Sign in", url: STORY_URL }],
    },
    { requirement: "Instructors export the grades.", stories: [] },
  ],
});

const json = (body: unknown, status = 200): Response => Response.json(body, { status });

/** A fake NikolAI server: `GET` answers `stored`, and `POST` answers `ran`. */
function show(
  stored: BacklogCoverage,
  ran: Response | Promise<Response> = json(CHECKED),
  section: string | null = "backlog-coverage",
) {
  const posts: string[] = [];
  const server = (url: string, init?: RequestInit): Promise<Response> => {
    if (url === `${API}/team`) return Promise.resolve(json(TEAM));
    if (url === `${API}/backlog-coverage` && init?.method === "POST") {
      posts.push(url);
      return Promise.resolve(ran);
    }
    if (url === `${API}/backlog-coverage`) return Promise.resolve(json(stored));
    return Promise.resolve(json({ detail: "Not Found" }, 404));
  };
  const clients = clientsFor(
    { ...DEFAULT_SETTINGS, apiBaseUrl: "https://nikolai.example.ca" },
    server,
  );
  render(<App page={REPO} section={section} clients={clients} />);
  return posts;
}

describe("backlog coverage view", () => {
  it("shows the uncovered requirements and which stories cover the others", async () => {
    show(CHECKED);

    const uncovered = await screen.findByRole("region", { name: "No story covers these" });
    expect(within(uncovered).getByText("Instructors export the grades.")).toBeDefined();
    expect(within(uncovered).queryByText("Students sign in with GitHub.")).toBeNull();
    const covered = screen.getByRole("region", { name: "Covered by your stories" });
    expect(within(covered).getByText("Students sign in with GitHub.")).toBeDefined();
    const story = within(covered).getByRole("link", { name: "#12 Sign in" });
    expect(story.getAttribute("href")).toBe(STORY_URL);
  });

  it("says so when every requirement is covered", async () => {
    show(
      coverage({
        requirements: [
          {
            requirement: "Students sign in.",
            stories: [{ number: 12, title: "Sign in", url: STORY_URL }],
          },
        ],
      }),
    );

    expect(await screen.findByText("Every requirement has at least one story.")).toBeDefined();
  });

  it("never links a story to anywhere but GitHub", async () => {
    show(
      coverage({
        requirements: [
          {
            requirement: "Students sign in.",
            stories: [{ number: 12, title: "Sign in", url: "https://evil.test/12" }],
          },
        ],
      }),
    );

    expect(await screen.findByText("#12 Sign in")).toBeDefined();
    expect(screen.queryByRole("link", { name: "#12 Sign in" })).toBeNull();
  });

  it("invites a team that never ran the check to run it, and shows the result", async () => {
    const posts = show(NOT_RUN);

    fireEvent.click(await screen.findByRole("button", { name: "Run the check" }));

    expect(await screen.findByText("Instructors export the grades.")).toBeDefined();
    expect(posts).toEqual([`${API}/backlog-coverage`]);
  });

  it("can't be started twice while a check is running", async () => {
    let finish: (response: Response) => void = () => undefined;
    const running = new Promise<Response>((resolve) => {
      finish = resolve;
    });
    const posts = show(NOT_RUN, running);

    fireEvent.click(await screen.findByRole("button", { name: "Run the check" }));
    const busy = await screen.findByRole<HTMLButtonElement>("button", { name: "Checking…" });

    expect(busy.disabled).toBe(true);
    fireEvent.click(busy);
    expect(posts).toHaveLength(1);
    finish(json(CHECKED));
    expect(await screen.findByRole("button", { name: "Run the check" })).toBeDefined();
  });

  it("doesn't read a course with no project requirements as a pass", async () => {
    show(coverage({ status: "no_requirements" }));

    expect(await screen.findByText(/no project requirements yet/)).toBeDefined();
    expect(screen.queryByText("Every requirement has at least one story.")).toBeNull();
  });

  it("shows why NikolAI couldn't check", async () => {
    show(coverage({ status: "failed", reason: "The AI model couldn't be reached." }));

    expect(await screen.findByText("NikolAI couldn't check this backlog")).toBeDefined();
    expect(screen.getByText("The AI model couldn't be reached.")).toBeDefined();
  });

  it("keeps the last result and explains when starting a check is refused", async () => {
    show(CHECKED, json({ detail: "You don't have access" }, 403));

    fireEvent.click(await screen.findByRole("button", { name: "Run the check" }));

    expect(await screen.findByText("You don't have access")).toBeDefined();
    expect(screen.getByText("Instructors export the grades.")).toBeDefined();
  });

  it("is linked from the repository menu, and the overview stays the default", async () => {
    show(NOT_RUN, json(CHECKED), null);

    const link = await screen.findByRole("link", { name: "Backlog coverage" });
    expect(link.getAttribute("href")).toBe("#nikolai/backlog-coverage");
    expect(screen.getByRole("link", { name: "Overview" }).getAttribute("aria-current")).toBe(
      "page",
    );
    expect(screen.queryByRole("button", { name: "Run the check" })).toBeNull();
  });
});
