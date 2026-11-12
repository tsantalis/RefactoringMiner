// AI contribution: 50%
/** The team panel and the PR tab are usable with the keyboard alone. */

import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import type { CourseRole } from "../shared/api/views-contract";
import type { GitHubPage } from "../shared/page";
import { clientsFor, DEFAULT_SETTINGS, type Clients } from "../shared/settings";
import { App } from "./App";
import { PULL } from "./pull/pullTestSupport";

const REPO: GitHubPage = { kind: "repo", owner: "team-alpha", repo: "sandbox" };

function view(page: GitHubPage, role: CourseRole = "student") {
  render(
    <App
      page={page}
      section={null}
      clients={clientsFor({ ...DEFAULT_SETTINGS, previewRole: role })}
    />,
  );
  return userEvent.setup();
}

/** The text of whatever has focus after each of `count` Tabs. */
async function tabThrough(
  user: ReturnType<typeof userEvent.setup>,
  count: number,
): Promise<string[]> {
  const focused: string[] = [];
  for (let step = 0; step < count; step += 1) {
    await user.tab();
    focused.push(document.activeElement?.textContent?.trim() ?? "");
  }
  return focused;
}

/** Press Tab until `target` has focus, giving up after 20 presses. */
async function tabTo(user: ReturnType<typeof userEvent.setup>, target: HTMLElement): Promise<void> {
  for (let step = 0; step < 20 && document.activeElement !== target; step += 1) {
    await user.tab();
  }
  expect(document.activeElement).toBe(target);
}

/** The usual sample clients, except every checklist answer is refused, as the server does. */
function failingClients(): Clients {
  const clients = clientsFor({ ...DEFAULT_SETTINGS });
  return {
    ...clients,
    nikolai: new Proxy(clients.nikolai, {
      get: (target, key, receiver) =>
        key === "actOnFinding"
          ? () => Promise.resolve({ kind: "forbidden", code: "not_a_team_student" })
          : (Reflect.get(target, key, receiver) as unknown),
    }),
  };
}

describe("team panel", () => {
  it("reaches every link in reading order with Tab", async () => {
    const user = view(REPO);
    await screen.findByRole("heading", { name: "Team Pixel" });

    expect(await tabThrough(user, 10)).toEqual([
      "Overview",
      "Backlog coverage",
      "Pull requests",
      "Stories",
      "Sprint planning",
      "CI/CD and tests",
      "Add sign-in with GitHub",
      "Build the team panel",
      "Build the PR detail view",
      "Link the panel from PR comments",
    ]);
    expect(document.activeElement?.tagName).toBe("A");
  });
});

describe("pull request tab", () => {
  it("switches views with Tab and Enter without losing focus", async () => {
    const user = view(PULL);
    const checklist = await screen.findByRole("button", { name: "Checklist 2" });

    await user.tab();
    expect(document.activeElement).toBe(checklist);
    await user.tab();
    expect(document.activeElement).toBe(screen.getByRole("button", { name: "AI review" }));
    await user.tab();
    const refactorings = screen.getByRole("button", { name: /^Refactorings/ });
    expect(document.activeElement).toBe(refactorings);

    await user.keyboard("{Enter}");

    expect(await screen.findByText("Extract Method")).toBeDefined();
    expect(document.activeElement).toBe(refactorings);
  });

  it("reaches the staff controls with Tab for an instructor", async () => {
    const user = view(PULL, "instructor");
    await screen.findByRole("button", { name: "Marking" });
    expect(await tabThrough(user, 5)).toEqual([
      "Checklist2",
      "AI review",
      "Refactorings4",
      "Your review",
      "Marking",
    ]);
  });

  it("keeps focus on the item after its answer is saved", async () => {
    const user = view(PULL);
    await screen.findByRole("button", { name: "Checklist 2" });
    const [acknowledge] = await screen.findAllByRole("button", { name: "Acknowledge" });
    if (acknowledge === undefined) throw new Error("the checklist has no Acknowledge button");

    await tabTo(user, acknowledge);

    await user.keyboard("{Enter}");

    await waitFor(() => {
      expect(document.activeElement?.textContent).toBe("Acknowledged");
    });
  });

  it("moves focus into the error message when a save fails", async () => {
    render(<App page={PULL} section={null} clients={failingClients()} />);
    const user = userEvent.setup();
    await screen.findByRole("button", { name: "Checklist 2" });
    const [acknowledge] = await screen.findAllByRole("button", { name: "Acknowledge" });
    if (acknowledge === undefined) throw new Error("the checklist has no Acknowledge button");

    await tabTo(user, acknowledge);
    await user.keyboard("{Enter}");

    await waitFor(() => {
      expect(document.activeElement?.textContent).toBe(
        "Only a student on this team can answer its checklist.",
      );
    });
  });

  it("moves focus into a dispute form and back to its button", async () => {
    const user = view(PULL);
    await screen.findByRole("button", { name: "Checklist 2" });
    const [dispute] = await screen.findAllByRole("button", { name: "Dispute" });
    if (dispute === undefined) throw new Error("the checklist has no Dispute button");

    await tabTo(user, dispute);

    await user.keyboard("{Enter}");
    const reason = screen.getByRole("textbox", { name: "Why is this item wrong?" });
    expect(document.activeElement).toBe(reason);

    await user.keyboard("The line is not in the diff.");
    await user.tab();
    expect(document.activeElement).toBe(screen.getByRole("button", { name: "Send dispute" }));
    await user.tab();
    expect(document.activeElement).toBe(screen.getByRole("button", { name: "Cancel" }));

    await user.keyboard("{Enter}");

    expect(document.activeElement).toBe(screen.getAllByRole("button", { name: "Dispute" })[0]);
  });
});
