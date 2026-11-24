// AI contribution: 50%
/**
 * Students land on their team's planning page (spec #234). A pure student sees it in place of
 * the home page; someone who is a student in one course and staff in another is moved there
 * once, at sign-in, so their course list stays one Back away.
 */

import { useEffect, useRef } from "react";
import type { CurrentUser } from "@nikolai/api-client";
import type { DashboardLocation } from "./dashboardUrl";
import { isStaffUser, studentTeamId } from "./planningRoute";

/**
 * Once the user has loaded, move a staff user who is also a student on a team to that team's
 * planning page, unless the URL already names a course or a team.
 */
export function useLandOnOwnTeam(
  user: CurrentUser | null,
  location: DashboardLocation,
  openTeam: (teamId: string, replace: boolean) => void,
): void {
  const decided = useRef(false);
  useEffect(() => {
    if (decided.current || user === null) return;
    decided.current = true;
    const team = studentTeamId(user.courses);
    if (isStaffUser(user) && team !== null && location.course === null && location.team === null) {
      openTeam(team, true);
    }
  }, [user, location, openTeam]);
}
