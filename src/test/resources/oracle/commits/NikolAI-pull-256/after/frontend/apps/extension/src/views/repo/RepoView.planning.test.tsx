// AI contribution: 50%
/** The repository tab's "Open planning" link to the team's planning page (US-19.1, #241). */

import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { sampleTeam } from "../../shared/api/samples/team";
import type { TeamOverview } from "../../shared/api/views-contract";
import type { GitHubPage } from "../../shared/page";
import { clientsFor, DEFAULT_SETTINGS } from "../../shared/settings";
import { App } from "../App";

const REPO: GitHubPage = { kind: "repo", owner: "team-alpha", repo: "sandbox" };

/** The repository tab, with `team` answered by a server at `apiBaseUrl`. */
function showFromServer(team: TeamOverview, apiBaseUrl: string): void {
  const clients = clientsFor({ ...DEFAULT_SETTINGS, apiBaseUrl }, () =>
    Promise.resolve(Response.json(team)),
  );
  render(<App page={REPO} section={null} clients={clients} />);
}

describe("repository tab's planning link", () => {
  it("links to the team's planning page on the dashboard, in a new tab", async () => {
    const team = sampleTeam("student");
    showFromServer(team, "https://api.nikolai.live");

    const link = await screen.findByRole("link", { name: "Open planning" });
    expect(link.getAttribute("href")).toBe(`https://app.nikolai.live/?team=${team.team.id}`);
    expect(link.getAttribute("target")).toBe("_blank");
    expect(link.getAttribute("rel")).toBe("noopener noreferrer");
  });

  it("has no planning link when the dashboard's address can't be told", async () => {
    const team = sampleTeam("ta");
    showFromServer(team, "https://nikolai.example.ca");

    expect(await screen.findByRole("heading", { name: team.team.name })).toBeDefined();
    expect(screen.queryByRole("link", { name: "Open planning" })).toBeNull();
  });
});
