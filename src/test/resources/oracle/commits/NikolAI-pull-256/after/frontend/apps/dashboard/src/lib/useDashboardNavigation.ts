// AI contribution: 50%
/** The dashboard's deep-link navigation: `?course=`, `&page=requirements` and `?team=`. */

import { useCallback, useRef, useState } from "react";
import { locationFromUrl, writeLocation, type DashboardLocation } from "./dashboardUrl";
import { usePopstateGuard } from "./usePopstateGuard";

const HOME: DashboardLocation = { course: null, page: "course", team: null };

export interface DashboardNavigation {
  location: DashboardLocation;
  /** True while the requirements page holds unsaved edits; the popstate guard reads it. */
  requirementsDirty: React.RefObject<boolean>;
  openCourse: (courseId: string | null) => void;
  openRequirements: () => void;
  /** The requirements page's own back button; callers confirm unsaved changes first. */
  closeRequirements: () => void;
  /**
   * Open a team's planning page from the open course (or none), or close it with null. With
   * `replace`, it takes the current history entry's place, so Back doesn't return to it.
   */
  openTeam: (teamId: string | null, replace?: boolean) => void;
}

/**
 * Owns the dashboard's query params and the Back/Forward handling. Every opener pushes one
 * history entry through `go`, or replaces it when asked. The guard itself lives in
 * `usePopstateGuard`; see that file for why a declined confirm restores the URL with `pushState` at the popstate layer.
 */
export function useDashboardNavigation(): DashboardNavigation {
  const [location, setLocation] = useState<DashboardLocation>(() =>
    typeof window === "undefined" ? HOME : locationFromUrl(),
  );
  const requirementsDirty = useRef(false);
  const locationRef = useRef(location);
  const urlRef = useRef(typeof window === "undefined" ? "" : window.location.href);
  locationRef.current = location;

  usePopstateGuard({ requirementsDirty, locationRef, urlRef, setLocation });

  const go = useCallback((next: DashboardLocation, replace = false): void => {
    const url = new URL(window.location.href);
    writeLocation(url, next);
    if (replace) {
      window.history.replaceState(null, "", url);
    } else {
      window.history.pushState(null, "", url);
    }
    urlRef.current = url.href;
    locationRef.current = next;
    setLocation(next);
  }, []);

  const openCourse = useCallback(
    (courseId: string | null) => {
      go({ course: courseId, page: "course", team: null });
    },
    [go],
  );
  const openRequirements = useCallback(() => {
    go({ ...locationRef.current, page: "requirements", team: null });
  }, [go]);
  const closeRequirements = useCallback(() => {
    go({ ...locationRef.current, page: "course" });
  }, [go]);
  const openTeam = useCallback(
    (teamId: string | null, replace = false) => {
      go({ course: locationRef.current.course, page: "course", team: teamId }, replace);
    },
    [go],
  );

  return { location, requirementsDirty, openCourse, openRequirements, closeRequirements, openTeam };
}
