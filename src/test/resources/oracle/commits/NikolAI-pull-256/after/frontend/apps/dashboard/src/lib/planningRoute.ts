// AI contribution: 50%
/**
 * Which team's planning page the dashboard shows, if any, and where its back button goes
 * (US-19.1, #241). Students land on their own team; staff open one through `?team=`.
 */

import { isDashboardRole, type CourseSummary, type CurrentUser } from "@nikolai/api-client";
import type { DashboardLocation } from "./dashboardUrl";

/** The planning page to show: its team, and its back button (none for a student at home). */
export interface PlanningRoute {
  teamId: string;
  back: { label: string; onBack: () => void } | null;
}

/** Whether the user sees the staff dashboard: an instructor, or staff or stakeholder in a course. */
export function isStaffUser(user: CurrentUser): boolean {
  return user.is_instructor || user.courses.some((course) => isDashboardRole(course.role));
}

/** The team a student lands on: the first team in any of their student courses, or null. */
export function studentTeamId(courses: readonly CourseSummary[]): string | null {
  return (
    courses.filter((course) => course.role === "student").flatMap((course) => course.teams)[0]
      ?.id ?? null
  );
}

/** Staff return where they came from; a student who followed a link returns to their team. */
function backLabel(staff: boolean, inCourse: boolean): string {
  if (!staff) return "← Back to your team";
  return inCourse ? "← Back to the course" : "← Back to your courses";
}

/**
 * The planning page for `location`: the `?team=` it names, else a pure student's own team.
 * Null when neither applies, so the dashboard shows its other pages.
 */
export function planningRoute(
  user: CurrentUser,
  location: DashboardLocation,
  inCourse: boolean,
  closeTeam: () => void,
): PlanningRoute | null {
  const staff = isStaffUser(user);
  const ownTeam = staff ? null : studentTeamId(user.courses);
  const teamId = location.team ?? ownTeam;
  if (teamId === null) return null;
  const hasBack = staff || teamId !== ownTeam;
  return {
    teamId,
    back: hasBack ? { label: backLabel(staff, inCourse), onBack: closeTeam } : null,
  };
}
