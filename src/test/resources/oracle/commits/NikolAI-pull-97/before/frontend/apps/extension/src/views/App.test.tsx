// AI contribution: 50%
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { ApiFailure, ApiResult } from "../shared/api/http";
import type { SampleScenario } from "../shared/api/sample";
import type { CourseRole } from "../shared/api/views-contract";
import type { GitHubPage } from "../shared/page";
import { clientsFor, DEFAULT_SETTINGS } from "../shared/settings";
import { App } from "./App";

const REPO: GitHubPage = { kind: "repo", owner: "team-alpha", repo: "sandbox" };
const PULL: GitHubPage = { kind: "pull", owner: "team-alpha", repo: "sandbox", number: 42 };

function show(
  page: GitHubPage,
  role: CourseRole = "student",
  section: string | null = null,
  scenario: SampleScenario = "reviewed",
) {
  const clients = clientsFor({ ...DEFAULT_SETTINGS, previewRole: role, sampleScenario: scenario });
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
    expect(await screen.findByText(/member-c has no commits this iteration/)).toBeDefined();
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
  it("reviews a story on an issue", async () => {
    show({ kind: "issue", owner: "team-alpha", repo: "sandbox", number: 72 });
    expect(await screen.findByText("7 / 10")).toBeDefined();
    expect(screen.getByText(/the NikolAI tab appears on it/)).toBeDefined();
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

describe("states", () => {
  it("tells a student from another team they have no access", async () => {
    showWith(REPO, { kind: "forbidden" });
    expect(await screen.findByText("You don't have access")).toBeDefined();
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

  it("explains a repository that isn't set up", async () => {
    show(PULL, "student", null, "not-registered");
    expect(await screen.findByText("NikolAI isn't set up here")).toBeDefined();
  });

  it("says when NikolAI has no record of the PR", async () => {
    show(PULL, "student", null, "not-found");
    expect(await screen.findByText("Nothing here yet")).toBeDefined();
  });
});
