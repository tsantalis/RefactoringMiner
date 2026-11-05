// AI contribution: 50%
/**
 * PROPOSED: the responses behind the NikolAI tabs (UI revamp, #100). Like `contract.ts`, these
 * endpoints don't exist yet; this file is the shape the extension asks the backend for, and the
 * runtime check that rejects anything else. When the backend ships them, generate the types into
 * `@nikolai/api-client` and keep these checks.
 *
 *   GET  /api/repos/{owner}/{repo}/team                     → TeamOverview      (US-11, US-10)
 *   GET  /api/repos/{owner}/{repo}/pulls/{n}/insights       → PullInsights      (US-8, US-9)
 *   GET  /api/repos/{owner}/{repo}/issues/{n}/story-review  → StoryReview
 *   GET  /api/projects/{owner}/{n}/planning                 → Planning
 *   GET  /api/repos/{owner}/{repo}/ci                       → CiAssistant
 *   POST /api/findings/{id}/actions  {action, reason}       → 204               (US-10.4)
 *
 * Every response names the viewer's course role. The server decides it (`can_read_team`, ADR
 * 0001); the extension only uses it to pick the student or staff layout. Staff-only fields are
 * null for students, so hiding them in the browser is never the access check.
 */

import {
  array,
  boolean,
  literal,
  nullable,
  number,
  object,
  refine,
  string,
  type Check,
  type Infer,
} from "./schema";

export const courseRole = literal("student", "ta", "instructor");
/** TAs and instructors see the staff layout of every NikolAI view. */
export function isStaffRole(role: CourseRole): boolean {
  return role === "ta" || role === "instructor";
}

export const severity = literal("info", "low", "medium", "high", "critical");

const viewer = object({ login: string, role: courseRole });

/** Reject a response that hands a student a staff-only field: the server must send it as null. */
function staffOnly<T extends { viewer: { role: CourseRole } }>(
  check: Check<T>,
  staffField: (value: T) => unknown,
): Check<T> {
  return refine(check, (value) => isStaffRole(value.viewer.role) || staffField(value) === null);
}
const progress = object({ name: string, due_on: string, tasks_done: number, tasks_total: number });

const teamPull = object({
  number,
  title: string,
  author: string,
  updated_at: string,
  draft: boolean,
  checklist_open: number,
  checklist_total: number,
  refactorings: number,
  review_status: literal("none", "queued", "running", "completed", "failed"),
});

const member = object({
  login: string,
  commits: number,
  pulls: number,
  reviews: number,
  substantive_reviews: number,
  fixes_with_evidence: number,
  last_commit_at: nullable(string),
});

const rubricRow = object({
  category: string,
  suggested_level: literal("poor", "ok", "expected", "exceptional"),
  evidence: string,
  mark: nullable(string),
});

const teamStaff = object({
  marking: object({
    iteration: string,
    state: literal("draft", "published"),
    rubric: array(rubricRow),
  }),
  attention: array(
    object({
      kind: literal("untested", "open-threads", "story", "ai-use", "inactive"),
      text: string,
      pull_number: nullable(number),
    }),
  ),
  disputes: array(
    object({
      finding_id: string,
      login: string,
      finding_name: string,
      pull_number: number,
      reason: string,
    }),
  ),
  stories_done: number,
  stories_planned: number,
  features_tested: number,
  features_total: number,
  ai_declared_percent: number,
});

export const teamOverview = staffOnly(
  object({
    viewer,
    team: object({ name: string, course: string }),
    iteration: nullable(progress),
    next_deliverables: array(progress),
    stats: object({
      open_checklist_items: number,
      refactorings: number,
      refactorings_this_week: number,
      reviewed_pulls: number,
      merged_pulls: number,
    }),
    pulls: array(teamPull),
    members: array(member),
    badges: object({
      earned: array(object({ id: string, name: string })),
      next: nullable(object({ name: string, progress: number, goal: number })),
    }),
    staff: nullable(teamStaff),
  }),
  (team) => team.staff,
);

const location = object({
  file_path: string,
  start_line: nullable(number),
  end_line: nullable(number),
});

export const pullInsights = staffOnly(
  object({
    viewer,
    checklist: array(
      object({
        finding_id: string,
        status: literal("open", "fixed", "acknowledged", "disputed"),
        fixed_in: nullable(object({ commit: string, location })),
      }),
    ),
    withheld_count: number,
    refactorings: object({
      since_commit: nullable(string),
      items: array(
        object({
          type: string,
          description: string,
          change: literal("new", "undone", "unchanged"),
          file_path: string,
          start_line: nullable(number),
          side: literal("parent", "child"),
        }),
      ),
    }),
    your_review: nullable(
      object({
        substantive: boolean,
        technical_comments: number,
        missed: array(
          object({ name: string, severity: nullable(severity), file_path: nullable(string) }),
        ),
      }),
    ),
    pushes: array(object({ commit: string, message: string, fixed: number, added: number })),
    marking: nullable(
      object({
        checks: array(object({ rule: string, met: boolean, detail: string })),
        contributors: array(
          object({ login: string, commits: number, additions: number, deletions: number }),
        ),
        ai_contribution: nullable(string),
        confirmed_refactorings: number,
      }),
    ),
  }),
  (pull) => pull.marking,
);

export const storyReview = object({
  viewer,
  score: number,
  max_score: number,
  checks: array(object({ text: string, passed: boolean })),
  current_text: nullable(string),
  suggested_text: nullable(string),
  split: nullable(string),
  requirement: nullable(string),
  linked_pulls: array(number),
  signed_off: boolean,
});

export const planning = object({
  viewer,
  iteration: object({ name: string, starts_on: string, ends_on: string }),
  members: array(
    object({ login: string, planned_hours: number, capacity_hours: number, items: array(string) }),
  ),
  backlog: array(
    object({
      issue_number: number,
      title: string,
      effort: string,
      priority: literal("high", "medium", "low"),
      reason: string,
    }),
  ),
  burndown: object({
    total: number,
    days: number,
    points: array(object({ day: number, remaining: number })),
  }),
});

export const ciAssistant = object({
  viewer,
  detected: array(string),
  pipelines: array(object({ name: string, exists: boolean })),
  suggestions: array(object({ title: string, detail: string })),
  saving_minutes: nullable(number),
  coverage: array(object({ file_path: string, percent: number })),
  failed_run: nullable(
    object({ workflow: string, run_number: number, url: string, draft_title: string }),
  ),
});

export type CourseRole = Infer<typeof courseRole>;
export type Viewer = Infer<typeof viewer>;
export type TeamOverview = Infer<typeof teamOverview>;
export type TeamStaff = Infer<typeof teamStaff>;
export type PullInsights = Infer<typeof pullInsights>;
export type StoryReview = Infer<typeof storyReview>;
export type Planning = Infer<typeof planning>;
export type CiAssistant = Infer<typeof ciAssistant>;
export type FindingAction = "acknowledge" | "dispute";
