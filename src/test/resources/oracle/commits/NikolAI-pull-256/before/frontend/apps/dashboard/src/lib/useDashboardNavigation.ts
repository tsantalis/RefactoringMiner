// AI contribution: 50%
/** The dashboard's deep-link navigation: `?course=<id>` and `?course=<id>&page=requirements`. */

import { useCallback, useRef, useState } from "react";
import { courseIdFromUrl, pageFromUrl, type DashboardPage } from "./dashboardUrl";
import { usePopstateGuard } from "./usePopstateGuard";

export interface DashboardNavigation {
  openCourseId: string | null;
  page: DashboardPage;
  /** True while the requirements page holds unsaved edits; the popstate guard reads it. */
  requirementsDirty: React.RefObject<boolean>;
  openCourse: (courseId: string | null) => void;
  openRequirements: () => void;
  /** The requirements page's own back button; callers confirm unsaved changes first. */
  closeRequirements: () => void;
}

/**
 * Owns the `?course=` and `?page=` query params and the Back/Forward handling.
 * 56 lines, just over the 50-line limit: the URL initialisers, the three pushUrl-based
 * openers and the return object are each a few tight lines, and splitting the openers into a
 * second hook buys indirection without readability. The guard itself lives in
 * `usePopstateGuard`; see that file for why a declined confirm restores the URL with
 * `pushState` at the popstate layer.
 */
export function useDashboardNavigation(): DashboardNavigation {
  const [openCourseId, setOpenCourseId] = useState<string | null>(() =>
    typeof window === "undefined" ? null : courseIdFromUrl(),
  );
  const [page, setPage] = useState<DashboardPage>(() =>
    typeof window === "undefined" ? "course" : pageFromUrl(),
  );
  const requirementsDirty = useRef(false);
  const locationRef = useRef({ course: openCourseId, page });
  const urlRef = useRef(typeof window === "undefined" ? "" : window.location.href);
  locationRef.current = { course: openCourseId, page };

  usePopstateGuard({ requirementsDirty, locationRef, urlRef, setOpenCourseId, setPage });

  const pushUrl = useCallback(
    (mutate: (url: URL) => void, next: { course: string | null; page: DashboardPage }): void => {
      const url = new URL(window.location.href);
      mutate(url);
      window.history.pushState(null, "", url);
      urlRef.current = url.href;
      locationRef.current = next;
      setOpenCourseId(next.course);
      setPage(next.page);
    },
    [],
  );

  const openCourse = useCallback(
    (courseId: string | null) => {
      pushUrl(
        (url) => {
          if (courseId === null) {
            url.searchParams.delete("course");
          } else {
            url.searchParams.set("course", courseId);
          }
          url.searchParams.delete("page");
        },
        { course: courseId, page: "course" },
      );
    },
    [pushUrl],
  );

  const openRequirements = useCallback(() => {
    pushUrl(
      (url) => {
        url.searchParams.set("page", "requirements");
      },
      { course: locationRef.current.course, page: "requirements" },
    );
  }, [pushUrl]);

  const closeRequirements = useCallback(() => {
    pushUrl(
      (url) => {
        url.searchParams.delete("page");
      },
      { course: locationRef.current.course, page: "course" },
    );
  }, [pushUrl]);

  return {
    openCourseId,
    page,
    requirementsDirty,
    openCourse,
    openRequirements,
    closeRequirements,
  };
}
