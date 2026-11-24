// AI contribution: 50%
/** The requirements page's unsaved-changes guard for the browser's Back and Forward. */

import { useEffect, type RefObject } from "react";
import { UNSAVED_CHANGES_MESSAGE } from "./useUnsavedChanges";
import { locationFromUrl, type DashboardLocation } from "./dashboardUrl";

interface PopstateGuard {
  requirementsDirty: RefObject<boolean>;
  locationRef: RefObject<DashboardLocation>;
  urlRef: RefObject<string>;
  setLocation: (location: DashboardLocation) => void;
}

/**
 * The Back/Forward half of the unsaved-changes guard. Declining the confirm restores the
 * requirements URL on the current history entry with `pushState`: replacing would drop the
 * course entry from history, and a popstate handler can't cancel the navigation otherwise.
 */
export function usePopstateGuard({
  requirementsDirty,
  locationRef,
  urlRef,
  setLocation,
}: PopstateGuard): void {
  useEffect(() => {
    const onPop = (): void => {
      const next = locationFromUrl();
      const current = locationRef.current;
      const leavingRequirements =
        next.page !== "requirements" || next.course !== current.course || next.team !== null;
      if (requirementsDirty.current && leavingRequirements) {
        // The browser already moved; put the requirements URL back and keep the edits.
        if (!window.confirm(UNSAVED_CHANGES_MESSAGE)) {
          window.history.pushState(null, "", urlRef.current);
          return;
        }
      }
      // When this leaves the requirements page, its unmount cleanup clears requirementsDirty.
      urlRef.current = window.location.href;
      locationRef.current = next;
      setLocation(next);
    };
    window.addEventListener("popstate", onPop);
    return () => {
      window.removeEventListener("popstate", onPop);
    };
  }, [requirementsDirty, locationRef, urlRef, setLocation]);
}
