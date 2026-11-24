// AI contribution: 50%
/** The planning page's Brainstorm section (US-20.2, #245), with only `fetch` mocked. */

import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { DashboardClient, type FetchLike } from "../lib/api";
import { PLANNING, TEAM, json } from "../test/fixtures";
import { PlanningPage } from "./PlanningPage";

const REPOSITORY = "Pixel-Org/App";
const READ_ONLY = { ...PLANNING, can_plan: false };

function idea(number: number, title: string) {
  return { number, title, url: `https://github.com/${REPOSITORY}/issues/${number}` };
}

const ALL_URL = `https://github.com/${REPOSITORY}/issues?q=label-filter`;

function listed(issues: ReturnType<typeof idea>[], more = false) {
  return { full_name: REPOSITORY, status: "listed", issues, more, all_url: ALL_URL };
}

/** The API's answer: the label it names and each repository's list. */
function answer(...repositories: object[]) {
  return json(200, { label: "brainstorm", repositories });
}

/** Opens Pixel's planning page with `page` as the page and `brainstorm` as its list. */
function open(brainstorm: Response, page: object = PLANNING) {
  const fetchImpl: FetchLike = (url) => {
    const path = url.slice("https://api.test/api/".length);
    if (path === `teams/${TEAM.id}/planning`) return Promise.resolve(json(200, page));
    if (path === `teams/${TEAM.id}/planning/brainstorm`) return Promise.resolve(brainstorm);
    return Promise.resolve(json(404, { detail: "Not Found" }));
  };
  render(
    <PlanningPage
      client={new DashboardClient("https://api.test", fetchImpl)}
      teamId={TEAM.id}
      back={null}
      onSignedOut={() => {}}
    />,
  );
}

async function brainstormCard(): Promise<HTMLElement> {
  const heading = await screen.findByRole("heading", { name: "Brainstorm" });
  const card = heading.closest("section");
  if (!(card instanceof HTMLElement)) throw new Error("The Brainstorm heading has no card");
  return card;
}

describe("the Brainstorm section", () => {
  it("lists each open brainstorm issue with a link to it on GitHub", async () => {
    open(answer(listed([idea(21, "Dark mode"), idea(25, "Export to CSV")])));

    const card = await brainstormCard();
    const link = await within(card).findByRole("link", { name: "#21 Dark mode" });

    expect(link.getAttribute("href")).toBe(`https://github.com/${REPOSITORY}/issues/21`);
    expect(within(card).getByRole("link", { name: "#25 Export to CSV" })).toBeDefined();
  });

  it("explains the label the API names when a repository has none", async () => {
    open(json(200, { label: "idea", repositories: [listed([])] }));

    const card = await brainstormCard();
    const label = await within(card).findByText("idea", { selector: "code" });

    expect(label.parentElement?.textContent).toMatch(/^No open brainstorm issues yet\. Label any/);
    expect(within(card).queryByRole("link")).toBeNull();
  });

  it("shows staff the same list, read-only", async () => {
    open(answer(listed([idea(21, "Dark mode")])), READ_ONLY);

    const card = await brainstormCard();

    expect(await within(card).findByRole("link", { name: "#21 Dark mode" })).toBeDefined();
    expect(screen.getByText(/viewing this team's planning read-only/)).toBeDefined();
    expect(within(card).queryByRole("button")).toBeNull();
  });

  it("says when GitHub couldn't be read for a repository and still lists the others", async () => {
    const gone = { ...listed([]), full_name: "Pixel-Org/Gone", status: "failed" };
    open(answer(listed([idea(21, "Dark mode")]), gone));

    const card = await brainstormCard();

    expect(await within(card).findByRole("alert")).toHaveProperty(
      "textContent",
      "NikolAI couldn't read the brainstorm issues of Pixel-Org/Gone from GitHub. Try again later.",
    );
    expect(within(card).getByRole("link", { name: "#21 Dark mode" })).toBeDefined();
  });

  it("says so when the team has no repository linked", async () => {
    open(answer());

    const card = await brainstormCard();

    expect(await within(card).findByText(/no repository linked yet/)).toBeDefined();
  });

  it("links to the API's GitHub list when there are more than were listed", async () => {
    open(answer(listed([idea(21, "Dark mode")], true)));

    const card = await brainstormCard();
    const all = await within(card).findByRole("link", { name: /See all open brainstorm issues/ });

    expect(all.getAttribute("href")).toBe(ALL_URL);
  });
});
