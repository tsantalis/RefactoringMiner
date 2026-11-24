// AI contribution: 50%
import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { ApiFailure, ApiResult } from "../shared/api/http";
import type { SampleScenario } from "../shared/api/sample";
import type { CourseRole, TeamOverview } from "../shared/api/views-contract";
import type { GitHubPage } from "../shared/page";
import { clientsFor, DEFAULT_SETTINGS } from "../shared/settings";
import { App } from "./App";

const REPO: GitHubPage = { kind: "repo", owner: "team-alpha", repo: "sandbox" };
const PULL: GitHubPage = { kind: "pull", owner: "team-alpha", repo: "sandbox", number: 42 };
const ISSUE: GitHubPage = { kind: "issue", owner: "team-alpha", repo: "sandbox", number: 12 };

function show(
  page: GitHubPage,
  role: CourseRole = "student",
  section: string | null = null,
  scenario: SampleScenario = "reviewed",
  adjust: (clients: ReturnType<typeof clientsFor>) => void = () => undefined,
) {
  const clients = clientsFor({ ...DEFAULT_SETTINGS, previewRole: role, sampleScenario: scenario });
  adjust(clients);
  render(<App page={page} section={section} clients={clients} />);
  return clients;
}

describe("repository tab", () => {
  it("shows a student their team's dashboard without staff data", async () => {
    show(REPO);
    expect(await screen.findByRole("heading", { name: "Team Pixel" })).toBeDefined();
    expect(screen.getByText("Build the team panel")).toBeDefined();
    expect(screen.queryByText(/Staff view/)).toBeNull();
    expect(screen.queryByText("Iteration 3 marking")).toBeNull();
  });

  it("shows a TA the staff view with marking and disputed feedback", async () => {
    show(REPO, "ta");
    expect(await screen.findByText(/Staff view/)).toBeDefined();
    expect(screen.getByText("Iteration 3 marking")).toBeDefined();
    expect(screen.getByText("Automated testing")).toBeDefined();
    expect(screen.getByText(/Write endpoints don't require the Origin check/)).toBeDefined();
  });

  it("warns about a member with no commits", async () => {
    show(REPO);
    expect(await screen.findByText(/member-c has no commits in a pull request yet/)).toBeDefined();
  });
});

/** A `/team` answer as the server sends it before checklists, iterations or marking exist. */
function serverTeam(role: CourseRole, pulls: TeamOverview["pulls"]): TeamOverview {
  return {
    viewer: { login: role === "student" ? "alice" : "prof", role },
    team: { id: "33333333-3333-3333-3333-333333333333", name: "Pixel", course: null },
    iteration: null,
    next_deliverables: [],
    stats: {
      open_checklist_items: null,
      refactorings: pulls.reduce((sum, pull) => sum + pull.refactorings, 0),
      refactorings_this_week: 0,
      reviewed_pulls: 0,
      merged_pulls: 0,
    },
    pulls,
    members:
      pulls.length === 0
        ? []
        : [
            {
              login: "alice",
              commits: 3,
              pulls: 1,
              reviews: null,
              substantive_reviews: null,
              fixes_with_evidence: null,
              last_commit_at: null,
            },
          ],
    badges: { earned: [], next: null },
    staff:
      role === "student"
        ? null
        : {
            marking: null,
            attention: [],
            disputes: [],
            stories_done: null,
            stories_planned: null,
            features_tested: null,
            features_total: null,
            ai_declared_percent: null,
          },
  };
}

const OPEN_PULL: TeamOverview["pulls"][number] = {
  number: 7,
  title: "Serve the team panel",
  author: "alice",
  updated_at: "2026-11-03T12:00:00Z",
  draft: false,
  checklist_open: null,
  checklist_total: null,
  refactorings: 3,
  review_status: "completed",
};

function showFromServer(team: TeamOverview): string[] {
  const urls: string[] = [];
  const server = (url: string) => {
    urls.push(url);
    return Promise.resolve(Response.json(team));
  };
  const clients = clientsFor(
    { ...DEFAULT_SETTINGS, apiBaseUrl: "https://nikolai.example.ca" },
    server,
  );
  render(<App page={REPO} section={null} clients={clients} />);
  return urls;
}

describe("repository tab, from the server", () => {
  it("shows a student their team's open PRs and members", async () => {
    const urls = showFromServer(serverTeam("student", [OPEN_PULL]));
    expect(await screen.findByRole("heading", { name: "Pixel" })).toBeDefined();
    expect(urls).toEqual(["https://nikolai.example.ca/api/repos/team-alpha/sandbox/team"]);
    expect(screen.getByRole("link", { name: "Serve the team panel" })).toBeDefined();
    // An unreviewed PR has no checklist and peer reviews aren't stored: never zeros.
    expect(screen.getByText("None yet")).toBeDefined();
    expect(screen.getAllByText("n/a")).toHaveLength(1);
    expect(screen.queryByText("Open checklist items")).toBeNull();
    expect(screen.queryByText(/Staff view/)).toBeNull();
    expect(screen.queryByText(/has no commits/)).toBeNull();
  });

  it("shows each reviewed PR's open checklist items and the team's open total", async () => {
    const reviewed = { ...OPEN_PULL, checklist_open: 2, checklist_total: 3 };
    const clean = {
      ...OPEN_PULL,
      number: 6,
      title: "Fix the tile",
      checklist_open: 0,
      checklist_total: 0,
    };
    const unreviewed = { ...OPEN_PULL, number: 5, title: "Draft the docs" };
    const team = serverTeam("student", [reviewed, clean, unreviewed]);
    showFromServer({ ...team, stats: { ...team.stats, open_checklist_items: 2 } });
    expect(await screen.findByText("2 of 3 open")).toBeDefined();
    expect(screen.getByText("No items")).toBeDefined();
    expect(screen.getByText("None yet")).toBeDefined();
    expect(screen.getByText("Open checklist items")).toBeDefined();
  });

  it("shows staff the team's open checklist items once a PR has a checklist", async () => {
    const team = serverTeam("instructor", [
      { ...OPEN_PULL, checklist_open: 4, checklist_total: 4 },
    ]);
    showFromServer({ ...team, stats: { ...team.stats, open_checklist_items: 4 } });
    const label = await screen.findByText("Open checklist items");
    const tile = label.closest<HTMLElement>(".tile");
    expect(tile).not.toBeNull();
    expect(within(tile as HTMLElement).getByText("4")).toBeDefined();
  });

  it("shows staff their view with no marking card before marking starts", async () => {
    showFromServer(serverTeam("instructor", [OPEN_PULL]));
    expect(await screen.findByText(/Staff view/)).toBeDefined();
    expect(screen.queryByText(/marking$/)).toBeNull();
    // Stories, tests, AI declarations and peer reviews aren't read yet: "n/a", never "0 of 0" or 0%.
    expect(screen.queryByText("0%")).toBeNull();
    expect(screen.queryByText("0 of 0")).toBeNull();
    expect(screen.getByText("No student has disputed feedback.")).toBeDefined();
  });

  it("shows the empty states of a team with no PRs or members", async () => {
    showFromServer(serverTeam("student", []));
    expect(await screen.findByText("No open pull requests yet.")).toBeDefined();
    expect(screen.getByText("No members on this team yet.")).toBeDefined();
  });
});

describe("pull request tab", () => {
  it("lists open and fixed checklist items with the fixing commit", async () => {
    show(PULL);
    expect(await screen.findByRole("button", { name: "Checklist 2" })).toBeDefined();
    expect(await screen.findByText("Password is logged in plain text")).toBeDefined();
    expect(screen.getByText(/Fixed in 4e1c2a9/)).toBeDefined();
    expect(screen.queryByRole("button", { name: "Marking" })).toBeNull();
  });

  it("opens the refactorings view from the route", async () => {
    show(PULL, "student", "refactorings");
    expect(await screen.findByText("Extract Method")).toBeDefined();
    expect(screen.getByText(/validate_email\(\) from submit\(\)/)).toBeDefined();
  });

  it("shows the refactorings even when the other insights are missing", async () => {
    show(PULL, "student", "refactorings", "reviewed", (c) => {
      vi.spyOn(c.nikolai, "getPullInsights").mockResolvedValue({ kind: "not-found" });
    });
    expect(await screen.findByRole("button", { name: /Refactorings/ })).toBeDefined();
    expect(await screen.findByText("Extract Method")).toBeDefined();
  });

  it("says when RefactoringMiner hasn't analysed the pull request yet", async () => {
    show(PULL, "student", "refactorings", "reviewed", (c) => {
      vi.spyOn(c.nikolai, "getRefactorings").mockResolvedValue({
        kind: "ok",
        data: {
          commit: null,
          since_commit: null,
          items: [],
          ai_check: null,
          ai_note: null,
          design_changes: null,
        },
      });
    });
    expect(await screen.findByText(/hasn't analysed this pull request yet/)).toBeDefined();
  });

  it("lists a refactoring that has no location without a file link", async () => {
    show(PULL, "student", "refactorings", "reviewed", (c) => {
      vi.spyOn(c.nikolai, "getRefactorings").mockResolvedValue({
        kind: "ok",
        data: {
          commit: "bbb2222",
          since_commit: null,
          items: [
            {
              type: "Extract Method",
              description: "check() extracted from login()",
              change: "new",
              file_path: null,
              start_line: null,
              side: "child",
              source: "RefactoringMiner",
            },
          ],
          ai_check: null,
          ai_note: null,
          design_changes: null,
        },
      });
    });
    expect(await screen.findByText("check() extracted from login()")).toBeDefined();
    expect(screen.queryByRole("link", { name: /\.py/ })).toBeNull();
  });

  it("gives staff a marking view with the course rules", async () => {
    show(PULL, "instructor");
    fireEvent.click(await screen.findByRole("button", { name: "Marking" }));
    expect(await screen.findByText("Answers requested changes before merge")).toBeDefined();
  });

  it("sends a student's acknowledgement of an item", async () => {
    const clients = show(PULL);
    const act = vi.spyOn(clients.nikolai, "actOnFinding");
    const [acknowledge] = await screen.findAllByRole("button", { name: "Acknowledge" });
    if (!acknowledge) throw new Error("no Acknowledge button");
    fireEvent.click(acknowledge);
    expect(act).toHaveBeenCalledWith("f-1", "acknowledge", "");
    expect(await screen.findByText("Acknowledged")).toBeDefined();
  });
});

describe("other pages", () => {
  it("doesn't show made-up story data on an issue", async () => {
    show({ kind: "issue", owner: "team-alpha", repo: "sandbox", number: 72 });
    await waitFor(() => {
      expect(screen.queryByText("Loading NikolAI…")).toBeNull();
    });
    expect(screen.queryByText("7 / 10")).toBeNull();
    expect(screen.queryByText(/the NikolAI tab appears on it/)).toBeNull();
  });

  it("proposes a plan on a Projects board and flags an over-capacity member", async () => {
    show({ kind: "project", owner: "team-alpha", number: 2 });
    expect(await screen.findByText(/Proposed plan for Iteration 4/)).toBeDefined();
    expect(screen.getByText(/member-b is over capacity/)).toBeDefined();
  });

  it("shows the CI/CD assistant on the Actions page", async () => {
    show({ kind: "actions", owner: "team-alpha", repo: "sandbox" });
    expect(await screen.findByRole("heading", { name: "CI/CD assistant" })).toBeDefined();
    expect(screen.getByText("Sonar static analysis")).toBeDefined();
  });
});

function showWith(page: GitHubPage, failure: ApiFailure) {
  const clients = clientsFor(DEFAULT_SETTINGS);
  const fail = <T,>(): Promise<ApiResult<T>> => Promise.resolve(failure);
  Object.assign(clients.nikolai, { getTeam: fail, getStoryReview: fail, getCi: fail });
  render(<App page={page} section={null} clients={clients} />);
}

/** A fake server that numbers its answers from 1 and counts every request it gets. */
function countingServer(answer: (count: number) => Response) {
  let count = 0;
  return {
    server: () => {
      count += 1;
      return Promise.resolve(answer(count));
    },
    answers: () => count,
  };
}

function renderWithServer(server: () => Promise<Response>) {
  const clients = clientsFor(
    { ...DEFAULT_SETTINGS, apiBaseUrl: "https://nikolai.example.ca" },
    server,
  );
  render(<App page={REPO} section={null} clients={clients} />);
}

/** Let pending requests settle and React apply their results. */
async function settle(): Promise<void> {
  await act(() => new Promise((resolve) => setTimeout(resolve, 0)));
}

describe("states", () => {
  it("tells a student from another team they have no access", async () => {
    showWith(REPO, { kind: "forbidden" });
    expect(await screen.findByText("You don't have access")).toBeDefined();
    expect(
      screen.getByText(/If you're on this team, join your course with the code/),
    ).toBeDefined();
    expect(screen.getByText(/Ask your instructor to put you on the team/)).toBeDefined();
    // Sample data has no server, so there's no dashboard to link to.
    expect(screen.queryByRole("link", { name: /dashboard/ })).toBeNull();
  });

  it("links a student without access to the dashboard's join form", async () => {
    const clients = clientsFor(
      { ...DEFAULT_SETTINGS, apiBaseUrl: "https://api.nikolai.live" },
      () => Promise.resolve(new Response(JSON.stringify({ detail: "Forbidden" }), { status: 403 })),
    );
    render(<App page={REPO} section={null} clients={clients} />);
    const link = await screen.findByRole("link", { name: "Open the NikolAI dashboard" });
    expect(link.getAttribute("href")).toBe("https://app.nikolai.live");
    expect(link.getAttribute("target")).toBe("_blank");
    expect(link.getAttribute("rel")).toBe("noopener noreferrer");
  });

  it("asks to reload when what the view showed has changed", async () => {
    showWith(REPO, { kind: "conflict" });
    expect(await screen.findByText("This changed")).toBeDefined();
  });

  it("explains an unreachable server without technical details", async () => {
    showWith(
      { kind: "actions", owner: "a", repo: "b" },
      { kind: "unreachable", reason: "The NikolAI server didn't answer." },
    );
    expect(await screen.findByText("Can't reach NikolAI")).toBeDefined();
    expect(screen.getByText("The NikolAI server didn't answer.")).toBeDefined();
  });

  it("shows nothing on an issue that isn't a user story", async () => {
    showWith({ kind: "issue", owner: "a", repo: "b", number: 3 }, { kind: "not-found" });
    await waitFor(() => {
      expect(screen.queryByText("Loading NikolAI…")).toBeNull();
    });
    expect(screen.queryByText(/story review/)).toBeNull();
  });

  it("explains on an issue that the repository isn't set up, instead of showing nothing", async () => {
    show({ kind: "issue", owner: "a", repo: "b", number: 3 }, "student", null, "not-registered");
    expect(await screen.findByText("NikolAI isn't set up here")).toBeDefined();
  });

  it("asks a signed-out user to sign in", async () => {
    show(REPO, "student", null, "signed-out");
    expect(await screen.findByText("Sign in to NikolAI")).toBeDefined();
  });

  it("opens the server's GitHub sign-in in a new tab for a signed-out user", async () => {
    const signedOut = () => Promise.resolve(new Response(null, { status: 401 }));
    const clients = clientsFor(
      { ...DEFAULT_SETTINGS, apiBaseUrl: "https://nikolai.example.ca/" },
      signedOut,
    );
    render(<App page={PULL} section={null} clients={clients} />);
    const link = await screen.findByRole("link", { name: "Sign in with GitHub" });
    expect(link.getAttribute("href")).toBe("https://nikolai.example.ca/auth/github/login");
    expect(link.getAttribute("target")).toBe("_blank");
    expect(link.getAttribute("rel")).toBe("noopener noreferrer");
  });

  it("loads the view again when the tab regains focus after signing in", async () => {
    const counter = countingServer((answer) =>
      answer === 1
        ? new Response(null, { status: 401 })
        : Response.json(serverTeam("student", [OPEN_PULL])),
    );
    renderWithServer(counter.server);
    expect(await screen.findByText("Sign in to NikolAI")).toBeDefined();

    fireEvent.focus(window);

    expect(await screen.findByRole("heading", { name: "Pixel" })).toBeDefined();
    expect(counter.answers()).toBe(2);
  });

  it("asks again each time the tab comes back still signed out", async () => {
    // jsdom keeps the page hidden, so the visibility check needs a visible document.
    vi.spyOn(document, "visibilityState", "get").mockReturnValue("visible");
    const counter = countingServer(() => new Response(null, { status: 401 }));
    renderWithServer(counter.server);
    expect(await screen.findByText("Sign in to NikolAI")).toBeDefined();

    fireEvent(document, new Event("visibilitychange"));
    await waitFor(() => {
      expect(counter.answers()).toBe(2);
    });
    await settle();
    fireEvent(document, new Event("visibilitychange"));

    await waitFor(() => {
      expect(counter.answers()).toBe(3);
    });
    expect(await screen.findByText("Sign in to NikolAI")).toBeDefined();
  });

  it("sends one request when focus and visibilitychange arrive together", async () => {
    vi.spyOn(document, "visibilityState", "get").mockReturnValue("visible");
    const counter = countingServer((answer) =>
      answer === 1
        ? new Response(null, { status: 401 })
        : Response.json(serverTeam("student", [OPEN_PULL])),
    );
    renderWithServer(counter.server);
    expect(await screen.findByText("Sign in to NikolAI")).toBeDefined();

    fireEvent.focus(window);
    fireEvent(document, new Event("visibilitychange"));

    expect(await screen.findByRole("heading", { name: "Pixel" })).toBeDefined();
    await settle();
    expect(counter.answers()).toBe(2);
  });

  it("does not load a view again on focus once it has loaded", async () => {
    const counter = countingServer(() => Response.json(serverTeam("student", [OPEN_PULL])));
    renderWithServer(counter.server);
    expect(await screen.findByRole("heading", { name: "Pixel" })).toBeDefined();

    fireEvent.focus(window);
    await settle();

    expect(counter.answers()).toBe(1);
  });

  it("has no sign-in link on sample data, where there is no server to sign in to", async () => {
    show(PULL, "student", null, "signed-out");
    expect(await screen.findByText("Sign in to NikolAI")).toBeDefined();
    expect(screen.queryByRole("link", { name: "Sign in with GitHub" })).toBeNull();
  });

  it("explains a repository that isn't set up", async () => {
    show(PULL, "student", null, "not-registered");
    expect(await screen.findByText("NikolAI isn't set up here")).toBeDefined();
  });

  it("says when NikolAI has no record of the PR", async () => {
    show(PULL, "student", null, "not-found");
    expect(await screen.findByText("Nothing here yet")).toBeDefined();
  });

  it("opens the story review when the issue comment's link is followed", async () => {
    const clients = clientsFor({ ...DEFAULT_SETTINGS, previewRole: "student" });
    vi.spyOn(clients.nikolai, "getStoryReviewStatus").mockResolvedValue({
      kind: "ok",
      data: {
        viewer: { login: "alice", role: "student" },
        status: "completed",
        reason: null,
        checked_at: "2026-11-09T12:00:00Z",
      },
    });
    vi.spyOn(clients.nikolai, "getStoryReview").mockResolvedValue({
      kind: "ok",
      data: {
        viewer: { login: "alice", role: "student" },
        score: 2,
        max_score: 3,
        format: "default",
        requirements_truncated: false,
        checks: [{ text: "The story sentence is present", passed: true }],
        current_text: null,
        suggested_text: null,
        split: null,
        requirement: null,
        linked_pulls: [],
        signed_off: false,
        checked_at: "2026-11-09T12:00:00Z",
      },
    });
    render(<App page={ISSUE} section="story-review" clients={clients} />);

    const heading = await screen.findByRole("heading", { name: "NikolAI story review" });
    await waitFor(() => {
      expect(document.activeElement).toBe(heading);
    });
  });
});
