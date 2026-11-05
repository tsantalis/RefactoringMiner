// AI contribution: 50%
/** The PR tab against a fake NikolAI server, through the real HTTP clients (US-11.3, US-9). */

import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { Finding, PullRequestFeedback, ReviewRun } from "../../shared/api/contract";
import type { PullInsights, PullRefactorings } from "../../shared/api/views-contract";
import type { GitHubPage } from "../../shared/page";
import { clientsFor, DEFAULT_SETTINGS } from "../../shared/settings";
import { App } from "../App";

const PULL: GitHubPage = { kind: "pull", owner: "team-alpha", repo: "sandbox", number: 42 };
const API = "https://nikolai.example.ca/api/repos/team-alpha/sandbox/pulls/42";
const COMMENT_URL = "https://github.com/team-alpha/sandbox/pull/42#issuecomment-1001";

function run(overrides: Partial<ReviewRun>): ReviewRun {
  return {
    id: "run-1",
    status: "completed",
    tool_name: "stub-agent",
    tool_version: "1",
    created_at: "2026-11-03T18:00:00Z",
    started_at: "2026-11-03T18:00:00Z",
    finished_at: "2026-11-03T18:00:04Z",
    error_message: null,
    retry_of_run_id: null,
    review: null,
    comment_url: null,
    ...overrides,
  };
}

const REVIEWED = run({
  id: "run-2",
  review: {
    content: "### Description\n\nSay **how** you tested `Invoice.calc`.",
    model: "cohere/north-mini-code:free",
    provider: "Cohere",
    latency_ms: 4100,
    usage: null,
    created_at: "2026-11-03T18:00:04Z",
  },
  comment_url: COMMENT_URL,
});
const FAILED = run({ status: "failed", error_message: "RateLimited" });

const FINDING: Finding = {
  id: "finding-1",
  name: "calc() does too much",
  description: null,
  severity: "medium",
  category: "maintainability",
  locations: [{ file_path: "src/Invoice.java", side: "child", start_line: 12, end_line: 40 }],
};

function stored(runs: ReviewRun[], findings: Finding[] = []): PullRequestFeedback {
  return {
    repository: "team-alpha/sandbox",
    number: 42,
    title: "Extract the invoice total",
    author_login: "student1",
    state: "open",
    merged_at: null,
    runs,
    findings,
  };
}

const json = (body: unknown, status = 200): Response =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

const NOT_ANALYSED: PullRefactorings = { commit: null, since_commit: null, items: [] };

/** A student's insights with the one finding already fixed, so the checklist is all done. */
const ALL_FIXED: PullInsights = {
  viewer: { login: "student1", role: "student" },
  checklist: [
    {
      finding_id: FINDING.id,
      status: "fixed",
      fixed_in: {
        commit: "abc1234",
        location: { file_path: "src/Invoice.java", start_line: 12, end_line: 40 },
      },
    },
  ],
  withheld_count: 0,
  your_review: null,
  pushes: [],
  marking: null,
};

/** A TA's insights, with the marking only course staff see. */
const TA_MARKING: PullInsights = {
  ...ALL_FIXED,
  viewer: { login: "ta1", role: "ta" },
  checklist: [],
  marking: {
    checks: [{ rule: "Links its story", met: true, detail: "Closes #72" }],
    contributors: [],
    ai_contribution: null,
    confirmed_refactorings: 0,
  },
};

/** A fake server with the PR and its refactorings, and its insights when they are given. */
function showStored(
  feedback: PullRequestFeedback,
  section: string | null = "ai-review",
  refactorings: PullRefactorings = NOT_ANALYSED,
  insights: PullInsights | Promise<PullInsights> | null = null,
) {
  const server = (url: string): Promise<Response> => {
    if (url === API) return Promise.resolve(json(feedback));
    if (url === `${API}/refactorings`) return Promise.resolve(json(refactorings));
    if (url === `${API}/insights` && insights !== null) return Promise.resolve(insights).then(json);
    return Promise.resolve(json({ detail: "Not Found" }, 404));
  };
  const clients = clientsFor(
    { ...DEFAULT_SETTINGS, apiBaseUrl: "https://nikolai.example.ca" },
    server,
  );
  const { rerender } = render(<App page={PULL} section={section} clients={clients} />);
  /** Follow a link to another view of the same PR, as the content script does. */
  return (next: string | null): void => {
    rerender(<App page={PULL} section={next} clients={clients} />);
  };
}

describe("the AI review on the PR tab", () => {
  it("shows a stored PR's review, with a link to its comment on GitHub", async () => {
    showStored(stored([REVIEWED, FAILED]));
    expect(await screen.findByRole("heading", { name: "Description" })).toBeDefined();
    expect(screen.getByText("Invoice.calc").tagName).toBe("CODE");
    const link = screen.getByRole("link", { name: "View the comment on GitHub" });
    expect(link.getAttribute("href")).toBe(COMMENT_URL);
  });

  it("lists every run, failed ones included, newest first", async () => {
    showStored(stored([REVIEWED, FAILED]));
    const runs = await screen.findByRole("list", { name: "Review runs" });
    const items = Array.from(runs.querySelectorAll("li"), (item) => item.textContent);
    expect(items).toHaveLength(2);
    expect(items[0]).toContain("Reviewed");
    expect(items[1]).toContain("Review failed");
    expect(items[1]).toContain("RateLimited");
  });

  it("shows a PR whose reviews all failed, with no review", async () => {
    showStored(stored([FAILED, run({ id: "run-0", status: "failed", error_message: "Timeout" })]));
    const note = await screen.findByText(/NikolAI couldn't review this pull request/);
    expect(note.textContent).toContain("RateLimited");
    // Failed attempts are retried on their own, so nothing tells the student to push again.
    expect(note.textContent).not.toMatch(/push/);
    expect(screen.getByText(/Timeout/)).toBeDefined();
    expect(screen.queryByRole("link", { name: "View the comment on GitHub" })).toBeNull();
  });

  it("marks an older review as out of date when a newer run failed, without its deleted comment", async () => {
    showStored(
      stored([run({ id: "run-3", status: "failed", error_message: "Timeout" }), REVIEWED]),
    );
    expect(await screen.findByRole("heading", { name: "Description" })).toBeDefined();
    expect(screen.getByText(/latest review attempt failed/).textContent).toContain("earlier push");
    // A newer run replaces the review's comment, so its link would lead nowhere.
    expect(screen.queryByRole("link", { name: "View the comment on GitHub" })).toBeNull();
    expect(screen.queryByRole("link", { name: "Comment" })).toBeNull();
  });

  it("keeps the review's comment link while a newer review is still running", async () => {
    showStored(stored([run({ id: "run-3", status: "running", finished_at: null }), REVIEWED]));
    expect(await screen.findByText(/reviewing the latest push/)).toBeDefined();
    const link = screen.getByRole("link", { name: "View the comment on GitHub" });
    expect(link.getAttribute("href")).toBe(COMMENT_URL);
  });

  it("says when NikolAI hasn't reviewed the PR at all", async () => {
    showStored(stored([]));
    expect(
      await screen.findByText("NikolAI hasn't reviewed this pull request yet.", {
        selector: ".card .card-body",
      }),
    ).toBeDefined();
    expect(screen.queryByRole("list", { name: "Review runs" })).toBeNull();
  });

  it("can be picked from the PR tab's views", async () => {
    showStored(stored([REVIEWED]), null);
    expect(await screen.findByRole("button", { name: "AI review" })).toBeDefined();
  });
});

describe("the view the PR tab opens on", () => {
  it("opens the AI review when the URL names no view and there is nothing to fix", async () => {
    showStored(stored([REVIEWED]), null);
    expect(await screen.findByRole("heading", { name: "Description" })).toBeDefined();
    expect(screen.queryByText("Review checklist")).toBeNull();
  });

  it("opens the Checklist when the URL names no view and it has an item", async () => {
    showStored(stored([REVIEWED], [FINDING]), null);
    expect(await screen.findByText("Review checklist")).toBeDefined();
    expect(screen.getByText("calc() does too much")).toBeDefined();
  });

  it("keeps the AI review a URL names, even once the checklist has items", async () => {
    showStored(stored([REVIEWED], [FINDING]), "ai-review");
    expect(await screen.findByRole("heading", { name: "Description" })).toBeDefined();
    expect(screen.queryByText("Review checklist")).toBeNull();
  });

  it("keeps the Checklist a URL names, saying NikolAI hasn't reviewed the code yet", async () => {
    showStored(stored([]), "checklist");
    expect(await screen.findByText("Review checklist")).toBeDefined();
    expect(
      screen.getByText(/NikolAI hasn't reviewed the code in this pull request yet/),
    ).toBeDefined();
    expect(screen.queryByText(/found nothing to fix/)).toBeNull();
  });

  it("says there is nothing to fix only once a review has completed", async () => {
    showStored(stored([REVIEWED]), "checklist");
    expect(await screen.findByText(/NikolAI found nothing to fix/)).toBeDefined();
  });

  it("says the code isn't reviewed yet when the completed review lists no reviewed push", async () => {
    showStored(stored([REVIEWED]), "checklist", NOT_ANALYSED, { ...ALL_FIXED, checklist: [] });
    expect(
      await screen.findByText(/hasn't reviewed the code in this pull request yet/),
    ).toBeDefined();
  });

  it("opens the Checklist when its items are all fixed, though the counter shows none open", async () => {
    showStored(stored([REVIEWED], [FINDING]), null, NOT_ANALYSED, ALL_FIXED);
    expect(await screen.findByRole("img", { name: "Fixed" })).toBeDefined();
    expect(screen.getByText("Fixed in abc1234: src/Invoice.java#L12-L40")).toBeDefined();
    expect(screen.getByText("Review checklist")).toBeDefined();
    expect(screen.getByRole("button", { name: /^Checklist/ }).textContent).toBe("Checklist0");
  });

  it("gives a student the default view at a staff-only link", async () => {
    showStored(stored([REVIEWED]), "marking", NOT_ANALYSED, ALL_FIXED);
    expect(await screen.findByRole("heading", { name: "Description" })).toBeDefined();
    expect(screen.queryByRole("button", { name: "Marking" })).toBeNull();
    expect(screen.queryByText("Course rules for this PR")).toBeNull();
  });

  it("opens Marking for a TA at its link, waiting for the role instead of the default view", async () => {
    let answer: (insights: PullInsights) => void = () => undefined;
    const insights = new Promise<PullInsights>((resolve) => {
      answer = resolve;
    });
    showStored(stored([REVIEWED]), "marking", NOT_ANALYSED, insights);
    expect(await screen.findByRole("status")).toBeDefined();
    expect(screen.queryByRole("heading", { name: "Description" })).toBeNull();
    answer(TA_MARKING);
    expect(await screen.findByText("Course rules for this PR")).toBeDefined();
    expect(screen.getByRole("button", { name: "Marking" }).getAttribute("aria-pressed")).toBe(
      "true",
    );
  });

  it("switches views when a link names another view of the same PR", async () => {
    const follow = showStored(stored([REVIEWED], [FINDING]), null);
    expect(await screen.findByText("Review checklist")).toBeDefined();
    follow("ai-review");
    expect(await screen.findByRole("heading", { name: "Description" })).toBeDefined();
    follow(null);
    expect(await screen.findByText("Review checklist")).toBeDefined();
  });
});

describe("the refactorings on the PR tab", () => {
  it("lists a stored run's refactorings, each linking to its lines in Files changed", async () => {
    showStored(stored([REVIEWED]), "refactorings", {
      commit: "bbb2222",
      since_commit: null,
      items: [
        {
          type: "Extract Method",
          description: "total() extracted from calc()",
          change: "new",
          file_path: "src/Invoice.java",
          start_line: 12,
          side: "child",
        },
      ],
    });
    expect(await screen.findByText("total() extracted from calc()")).toBeDefined();
    const link = screen.getByRole("link", { name: /src\/Invoice\.java/ });
    await waitFor(() => {
      expect(link.getAttribute("href")).toMatch(
        /^https:\/\/github\.com\/team-alpha\/sandbox\/pull\/42\/files#diff-[0-9a-f]{64}R12$/,
      );
    });
  });
});

const SECURITY_FINDING: Finding = {
  id: "f-1",
  name: "Password is logged in plain text",
  description: "The login handler logs the request body.",
  severity: "high",
  category: "security",
  locations: [],
};

const INSIGHTS: PullInsights = {
  viewer: { login: "alice", role: "student" },
  checklist: [{ finding_id: SECURITY_FINDING.id, status: "open", fixed_in: null }],
  withheld_count: 0,
  your_review: null,
  pushes: [],
  marking: null,
};

/** Serve the PR, its insights, its refactorings and the actions endpoint, recording writes.
 * `answer` is what the actions endpoint replies (by default 204). */
function showChecklist(answer: () => Promise<Response> = () => noContent()) {
  const requests: { url: string; init: RequestInit | undefined }[] = [];
  const server = (url: string, init?: RequestInit): Promise<Response> => {
    requests.push({ url, init });
    if (url === `${API}/insights`) return Promise.resolve(json(INSIGHTS));
    if (url === `${API}/refactorings`) return Promise.resolve(json(NOT_ANALYSED));
    if (url === API)
      return Promise.resolve(json({ ...stored([REVIEWED]), findings: [SECURITY_FINDING] }));
    if (url === "https://nikolai.example.ca/api/findings/f-1/actions") return answer();
    return Promise.resolve(json({ detail: "Not Found" }, 404));
  };
  const clients = clientsFor(
    { ...DEFAULT_SETTINGS, apiBaseUrl: "https://nikolai.example.ca" },
    server,
  );
  render(<App page={PULL} section="checklist" clients={clients} />);
  return requests;
}

const noContent = () => Promise.resolve(new Response(null, { status: 204 }));
const refused = (status: number, detail: unknown) => () =>
  Promise.resolve(json({ detail }, status));

function savedActions(requests: { url: string; init: RequestInit | undefined }[]) {
  return requests.filter((request) => request.url.endsWith("/api/findings/f-1/actions"));
}

describe("responding to a checklist item", () => {
  it("acknowledges it through the real client and shows the saved status", async () => {
    const requests = showChecklist();

    fireEvent.click(await screen.findByRole("button", { name: "Acknowledge" }));

    expect(await screen.findByText("Acknowledged")).toBeDefined();
    const [request] = savedActions(requests);
    expect(request?.init?.method).toBe("POST");
    expect(request?.init?.body).toBe(JSON.stringify({ action: "acknowledge", reason: "" }));
  });

  it("sends a dispute with its reason and shows the saved status", async () => {
    const requests = showChecklist();
    fireEvent.click(await screen.findByRole("button", { name: "Dispute" }));
    fireEvent.change(screen.getByLabelText("Why is this item wrong?"), {
      target: { value: "The password is hashed before it is logged." },
    });

    fireEvent.click(screen.getByRole("button", { name: "Send dispute" }));

    expect(await screen.findByText("Disputed")).toBeDefined();
    expect(savedActions(requests)[0]?.init?.body).toBe(
      JSON.stringify({ action: "dispute", reason: "The password is hashed before it is logged." }),
    );
  });

  it("disables the item's buttons while its answer is saving, so a double click sends one", async () => {
    let finish = (): void => undefined;
    const pending = new Promise<Response>((resolve) => {
      finish = () => {
        resolve(new Response(null, { status: 204 }));
      };
    });
    const requests = showChecklist(() => pending);
    const acknowledge = await screen.findByRole<HTMLButtonElement>("button", {
      name: "Acknowledge",
    });

    fireEvent.click(acknowledge);
    fireEvent.click(acknowledge);

    expect(acknowledge.disabled).toBe(true);
    expect(screen.getByRole<HTMLButtonElement>("button", { name: "Dispute" }).disabled).toBe(true);
    finish();
    expect(await screen.findByText("Acknowledged")).toBeDefined();
    expect(savedActions(requests)).toHaveLength(1);
  });

  it.each([
    [
      "only the team's students can answer",
      refused(403, { code: "not_a_team_student" }),
      "Only a student on this team can answer its checklist.",
    ],
    [
      "the server doesn't accept the extension's origin",
      refused(403, { code: "origin_not_allowed" }),
      "Your NikolAI server isn't set up to accept answers from this extension. Let your instructor know.",
    ],
    [
      "the user must sign in",
      refused(401, "Sign in required"),
      "Sign in to NikolAI to answer the checklist.",
    ],
    [
      "a newer review replaced the item",
      refused(409, { code: "finding_superseded" }),
      "This item changed. Reload the checklist.",
    ],
    [
      "the item is gone",
      refused(404, { code: "finding_not_found" }),
      "This item changed. Reload the checklist.",
    ],
    [
      "the server can't be reached",
      () => Promise.resolve(json({}, 500)),
      "NikolAI couldn't save that. Try again in a moment.",
    ],
  ])("keeps the item open and says %s", async (_case, answer, message) => {
    showChecklist(answer);

    fireEvent.click(await screen.findByRole("button", { name: "Acknowledge" }));

    expect((await screen.findByRole("alert")).textContent).toBe(message);
    expect(screen.queryByText("Acknowledged")).toBeNull();
    expect(screen.getByRole<HTMLButtonElement>("button", { name: "Acknowledge" }).disabled).toBe(
      false,
    );
  });
});

function insightsFor(role: PullInsights["viewer"]["role"]): PullInsights {
  return {
    viewer: { login: role === "student" ? "student1" : "prof", role },
    checklist: [],
    withheld_count: 3,
    your_review: null,
    pushes: [
      { commit: "bbbbbbb", message: "Extract the invoice total", fixed: 0, added: 1 },
      { commit: "aaaaaaa", message: "Extract the invoice total", fixed: 0, added: 2 },
    ],
    marking: null,
  };
}

describe("the insights on the PR tab", () => {
  it("shows the withheld count and the reviewed pushes to a student, without the staff view", async () => {
    showStored(stored([REVIEWED]), "checklist", NOT_ANALYSED, insightsFor("student"));
    expect(await screen.findByText("3 low-confidence findings were left out.")).toBeDefined();
    expect(screen.getByText("bbbbbbb")).toBeDefined();
    expect(screen.getByText("aaaaaaa")).toBeDefined();
    expect(screen.getByText("+2 new")).toBeDefined();
    expect(screen.queryByText("Staff view")).toBeNull();
    expect(screen.queryByRole("button", { name: "Marking" })).toBeNull();
  });

  it("gives an instructor the staff bar and the Marking view's placeholder", async () => {
    showStored(stored([REVIEWED]), "marking", NOT_ANALYSED, insightsFor("instructor"));
    expect(await screen.findByText("Staff view")).toBeDefined();
    expect(screen.getByText(/You're an instructor/)).toBeDefined();
    expect(screen.getByRole("button", { name: "Marking", pressed: true })).toBeDefined();
    expect(
      screen.getByText(/doesn't check pull requests against the course rules yet/),
    ).toBeDefined();
  });

  it("shows each item's stored answer after a reload", async () => {
    const other: Finding = { ...SECURITY_FINDING, id: "f-2", name: "Invoice total ignores tax" };
    const answered: PullInsights = {
      ...insightsFor("student"),
      checklist: [
        { finding_id: SECURITY_FINDING.id, status: "acknowledged", fixed_in: null },
        { finding_id: other.id, status: "disputed", fixed_in: null },
      ],
    };
    const feedback = { ...stored([REVIEWED]), findings: [SECURITY_FINDING, other] };

    showStored(feedback, "checklist", NOT_ANALYSED, answered);

    expect(await screen.findByText("Acknowledged")).toBeDefined();
    expect(screen.getByText("Disputed")).toBeDefined();
    expect(screen.queryByRole("button", { name: "Acknowledge" })).toBeNull();
  });

  it("shows staff the team's answers without buttons to answer for a student", async () => {
    const other: Finding = { ...SECURITY_FINDING, id: "f-2", name: "Invoice total ignores tax" };
    const answered: PullInsights = {
      ...insightsFor("ta"),
      checklist: [
        { finding_id: SECURITY_FINDING.id, status: "disputed", fixed_in: null },
        { finding_id: other.id, status: "open", fixed_in: null },
      ],
    };
    const feedback = { ...stored([REVIEWED]), findings: [SECURITY_FINDING, other] };

    showStored(feedback, "checklist", NOT_ANALYSED, answered);

    expect(await screen.findByText("Disputed")).toBeDefined();
    expect(screen.getByText("Invoice total ignores tax")).toBeDefined();
    expect(screen.queryByRole("button", { name: "Acknowledge" })).toBeNull();
    expect(screen.queryByRole("button", { name: "Dispute" })).toBeNull();
  });

  it.each([0, -1])("leaves the footer out when %i findings were withheld", async (count) => {
    showStored(stored([REVIEWED]), "checklist", NOT_ANALYSED, {
      ...insightsFor("student"),
      withheld_count: count,
    });
    expect(await screen.findByText("bbbbbbb")).toBeDefined();
    expect(screen.queryByText(/low-confidence/)).toBeNull();
  });
});
