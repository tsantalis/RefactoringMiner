// AI contribution: 50%
/** Made-up team data for demos, used until a NikolAI server is set in the options. */

import type { CourseRole, TeamOverview, TeamStaff } from "../views-contract";

export const hoursAgo = (now: Date, hours: number): string =>
  new Date(now.getTime() - hours * 3_600_000).toISOString();

export const daysFromNow = (now: Date, days: number): string =>
  new Date(now.getTime() + days * 86_400_000).toISOString().slice(0, 10);

export function sampleViewer(role: CourseRole): TeamOverview["viewer"] {
  return { login: role === "student" ? "member-a" : "sample-ta", role };
}

type RubricRow = NonNullable<TeamStaff["marking"]>["rubric"][number];

const rubricRow = (
  category: string,
  suggested_level: "ok" | "expected",
  evidence: string,
): RubricRow => ({
  category,
  suggested_level,
  evidence,
  mark: null,
});

const SAMPLE_RUBRIC: RubricRow[] = [
  rubricRow(
    "Features and volume of work",
    "expected",
    "6 substantive features, 78% of planned stories",
  ),
  rubricRow("Automated testing", "expected", "5 of 6 features tested, 4 with an edge case"),
  rubricRow("Code structure", "ok", "One file is 490 lines; 2 functions are over 50 lines"),
  rubricRow("Validation, errors and logging", "expected", "8 of 9 checks met"),
  rubricRow("Security", "expected", "Access checks run before every team read"),
  rubricRow("Code review quality", "expected", "75% substantive review rate"),
  rubricRow("Traceability", "ok", "2 stories have no customer sign-off"),
  rubricRow("Responsible AI use", "expected", "94% of source files declare AI use"),
];

function sampleStaff(): TeamStaff {
  return {
    marking: { iteration: "Iteration 3", state: "draft", rubric: SAMPLE_RUBRIC },
    attention: [
      { kind: "untested", text: "adds 640 lines with no tests", pull_number: 101 },
      { kind: "open-threads", text: "has 4 unanswered review comments", pull_number: 98 },
      { kind: "story", text: "US-11 has no customer sign-off", pull_number: null },
      { kind: "ai-use", text: "3 source files have no AI declaration", pull_number: null },
    ],
    disputes: [
      {
        finding_id: "f-1",
        login: "member-a",
        finding_name: "Write endpoints don't require the Origin check by default",
        pull_number: 98,
        reason: "Only GET endpoints exist so far. The next PR adds the check to every write.",
      },
    ],
    stories_done: 3,
    stories_planned: 4,
    features_tested: 5,
    features_total: 6,
    ai_declared_percent: 94,
  };
}

export function sampleTeam(role: CourseRole, now: Date = new Date()): TeamOverview {
  return {
    viewer: sampleViewer(role),
    team: {
      id: "00000000-0000-4000-8000-000000000001",
      name: "Team Pixel",
      course: "SOEN 490 · Fall 2026",
    },
    iteration: {
      name: "Iteration 3",
      due_on: daysFromNow(now, 2),
      tasks_done: 14,
      tasks_total: 22,
    },
    next_deliverables: [
      {
        name: "Iteration 4 (Release 1)",
        due_on: daysFromNow(now, 16),
        tasks_done: 0,
        tasks_total: 0,
      },
    ],
    stats: {
      open_checklist_items: 9,
      refactorings: 17,
      refactorings_this_week: 4,
      reviewed_pulls: 6,
      merged_pulls: 8,
    },
    pulls: samplePulls(now),
    members: sampleMembers(now),
    badges: {
      earned: [
        { id: "refactoring-rookie", name: "Refactoring Rookie" },
        { id: "first-fix", name: "First Fix" },
      ],
      next: { name: "Clean Reviewer", progress: 3, goal: 5 },
    },
    staff: role === "student" ? null : sampleStaff(),
  };
}

function pull(
  number: number,
  title: string,
  author: string,
  updated_at: string,
  checklist_open: number,
  checklist_total: number,
  refactorings: number,
  review_status: TeamOverview["pulls"][number]["review_status"],
): TeamOverview["pulls"][number] {
  return {
    number,
    title,
    author,
    updated_at,
    draft: false,
    checklist_open,
    checklist_total,
    refactorings,
    review_status,
  };
}

function member(
  login: string,
  commits: number,
  pulls: number,
  reviews: number,
  substantive_reviews: number,
  fixes_with_evidence: number,
  last_commit_at: string | null,
): TeamOverview["members"][number] {
  return {
    login,
    commits,
    pulls,
    reviews,
    substantive_reviews,
    fixes_with_evidence,
    last_commit_at,
  };
}

function samplePulls(now: Date): TeamOverview["pulls"] {
  return [
    pull(98, "Add sign-in with GitHub", "member-a", hoursAgo(now, 2), 4, 9, 11, "completed"),
    pull(101, "Build the team panel", "member-b", hoursAgo(now, 1), 3, 5, 2, "running"),
    pull(103, "Build the PR detail view", "member-c", hoursAgo(now, 5), 2, 2, 0, "completed"),
    {
      ...pull(
        104,
        "Link the panel from PR comments",
        "member-a",
        hoursAgo(now, 9),
        0,
        0,
        0,
        "none",
      ),
      draft: true,
    },
  ];
}

function sampleMembers(now: Date): TeamOverview["members"] {
  return [
    member("member-a", 12, 3, 4, 4, 5, hoursAgo(now, 2)),
    member("member-b", 8, 2, 5, 3, 3, hoursAgo(now, 20)),
    member("member-c", 0, 0, 1, 0, 0, null),
  ];
}
