// AI contribution: 50%
/** Made-up data for the PR, issue, Projects and Actions views, matching `sample.ts`'s findings. */

import type {
  CiAssistant,
  CourseRole,
  Planning,
  PullInsights,
  StoryReview,
} from "../views-contract";
import { daysFromNow, sampleViewer } from "./team";

const SAMPLE_MARKING: NonNullable<PullInsights["marking"]> = {
  checks: [
    { rule: "Links its story", met: true, detail: "Closes #57, part of US-12" },
    { rule: "Adds tests for the change", met: true, detail: "Main path and a failure case" },
    { rule: "Declares AI use", met: true, detail: "PR body and every changed source file" },
    { rule: "Commit messages reference the issue", met: true, detail: "5 of 5 commits" },
    {
      rule: "Has a substantive teammate review",
      met: true,
      detail: "The reviewer names what they checked",
    },
    {
      rule: "Answers requested changes before merge",
      met: false,
      detail: "2 review threads have no reply yet",
    },
  ],
  contributors: [
    { login: "member-a", commits: 5, additions: 412, deletions: 37 },
    { login: "member-b", commits: 1, additions: 12, deletions: 4 },
  ],
  ai_contribution: "50%",
  confirmed_refactorings: 2,
};

export function samplePullInsights(role: CourseRole): PullInsights {
  return {
    viewer: sampleViewer(role),
    checklist: [
      { finding_id: "f-1", status: "open", fixed_in: null },
      {
        finding_id: "f-2",
        status: "fixed",
        fixed_in: {
          commit: "4e1c2a9",
          location: { file_path: "src/auth/login_form.py", start_line: 22, end_line: 26 },
        },
      },
      { finding_id: "f-3", status: "acknowledged", fixed_in: null },
    ],
    withheld_count: 2,
    refactorings: SAMPLE_REFACTORINGS,
    your_review: {
      substantive: true,
      technical_comments: 3,
      missed: [
        {
          name: "Password is logged in plain text",
          severity: "high",
          file_path: "src/auth/session.py",
        },
      ],
    },
    pushes: [
      { commit: "4e1c2a9", message: "Validate the email before use", fixed: 1, added: 0 },
      { commit: "9c03d1e", message: "Extract the login checks", fixed: 0, added: 1 },
      { commit: "b71a0f4", message: "Add login form validation", fixed: 0, added: 2 },
    ],
    marking: role === "student" ? null : SAMPLE_MARKING,
  };
}

function refactoring(
  type: string,
  description: string,
  change: "new" | "undone" | "unchanged",
  file_path: string,
  start_line: number,
): PullInsights["refactorings"]["items"][number] {
  return {
    type,
    description,
    change,
    file_path,
    start_line,
    side: change === "undone" ? "parent" : "child",
  };
}

const SAMPLE_REFACTORINGS: PullInsights["refactorings"] = {
  since_commit: "9c03d1e",
  items: [
    refactoring(
      "Extract Method",
      "validate_email() from submit()",
      "new",
      "src/auth/login_form.py",
      24,
    ),
    refactoring("Rename Variable", "pw to password", "new", "src/auth/session.py", 58),
    refactoring("Move Class", "Session to auth/session.py", "unchanged", "src/auth/session.py", 1),
    refactoring("Inline Variable", "result in login()", "undone", "src/auth/login_form.py", 40),
  ],
};

export function sampleStoryReview(role: CourseRole): StoryReview {
  return {
    viewer: sampleViewer(role),
    score: 7,
    max_score: 10,
    checks: [
      { text: 'Uses "As a …, I want …, so that …"', passed: true },
      { text: "Has points, priority, risk, a label and a milestone", passed: true },
      {
        text: "The first criterion is a condition, not something a tester can check",
        passed: false,
      },
      { text: '"in one place" can be read two ways', passed: false },
    ],
    current_text: "When the GitHub App is installed successfuly.",
    suggested_text:
      "After the course's GitHub App is installed on the team's repository, the NikolAI tab appears on it.",
    split:
      'Split into "Sign in with GitHub" and "See my team\'s progress", under the Team progress epic.',
    requirement: "A panel for each team that shows pending deliverables and progress",
    linked_pulls: [98, 101],
    signed_off: false,
  };
}

export function samplePlanning(role: CourseRole, now: Date = new Date()): Planning {
  const remaining = [22, 21, 18, 16, 13, 9, 8];
  return {
    viewer: sampleViewer(role),
    iteration: {
      name: "Iteration 4 (Release 1)",
      starts_on: daysFromNow(now, 2),
      ends_on: daysFromNow(now, 16),
    },
    members: [
      { login: "member-a", planned_hours: 10, capacity_hours: 12, items: ["US-9 checklist"] },
      { login: "member-b", planned_hours: 14, capacity_hours: 12, items: ["US-10 roster", "10.1"] },
      { login: "member-c", planned_hours: 6, capacity_hours: 10, items: ["US-11 panel"] },
    ],
    backlog: [
      {
        issue_number: 66,
        title: "US-10: Register courses, teams and who can see what",
        effort: "5 pts",
        priority: "high",
        reason: "Blocks US-11 and the read API",
      },
      {
        issue_number: 60,
        title: "US-9: A checklist that updates as students fix issues",
        effort: "8 pts",
        priority: "high",
        reason: "High risk, start early",
      },
      {
        issue_number: 72,
        title: "US-11: Sign in and see my team's progress",
        effort: "5 pts",
        priority: "medium",
        reason: "Waits for US-10's access rules",
      },
    ],
    burndown: {
      total: 22,
      days: 14,
      points: remaining.map((left, index) => ({ day: index * 2, remaining: left })),
    },
  };
}

export function sampleCi(role: CourseRole): CiAssistant {
  return {
    viewer: sampleViewer(role),
    detected: ["Python 3.13", "pytest", "TypeScript", "Vitest", "Dockerfile"],
    pipelines: [
      { name: "Unit tests", exists: true },
      { name: "Lint and type check", exists: true },
      { name: "Docker image build", exists: true },
      { name: "Sonar static analysis", exists: false },
      { name: "End-to-end tests", exists: false },
    ],
    suggestions: [
      {
        title: "Dependencies install twice",
        detail: "ci.yml and deploy.yml both run uv sync on the same push",
      },
      {
        title: "The pnpm store isn't cached",
        detail: "The frontend job installs from scratch on every run",
      },
      { title: "Tests run on docs-only changes", detail: "Add a paths filter for docs/**" },
    ],
    saving_minutes: 3,
    coverage: [
      { file_path: "app/api/routes/auth.py", percent: 92 },
      { file_path: "app/services/pr_review.py", percent: 81 },
      { file_path: "app/github/action_artifacts.py", percent: 64 },
      { file_path: "app/retry_publication.py", percent: 38 },
    ],
    failed_run: {
      workflow: "Backend CI",
      run_number: 812,
      url: "https://github.com/team-alpha/sandbox/actions",
      draft_title: "test_callback_rejects_wrong_state fails after the sessions refactor",
    },
  };
}
