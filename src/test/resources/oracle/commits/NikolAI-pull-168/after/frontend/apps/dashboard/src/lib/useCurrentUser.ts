// AI contribution: 50%
/** Loads the signed-in user, and keeps the page's copy of their courses current. */

import { useCallback, useEffect, useState } from "react";
import type { CourseSummary, CurrentUser } from "@nikolai/api-client";
import type { ApiResult, DashboardClient } from "./api";

export type Loaded = { kind: "loading" } | ApiResult<CurrentUser>;

/**
 * The dashboard's data: `state` is the signed-in user (or the screen to show instead), `reload`
 * retries a failed load, `addCourse` appends a created course, and `markSignedOut` swaps to the
 * sign-in screen after the session ends.
 */
export function useCurrentUser(client: DashboardClient): {
  state: Loaded;
  reload: (signal?: AbortSignal) => void;
  addCourse: (course: CourseSummary) => void;
  markSignedOut: () => void;
} {
  const [state, setState] = useState<Loaded>({ kind: "loading" });

  const reload = useCallback(
    (signal?: AbortSignal) => {
      setState({ kind: "loading" });
      client.getCurrentUser(signal).then(setState, (error: unknown) => {
        if (signal?.aborted) return;
        console.error("NikolAI: the dashboard failed to load.", error);
        setState({ kind: "unreachable", reason: "Something went wrong loading NikolAI." });
      });
    },
    [client],
  );

  useEffect(() => {
    const controller = new AbortController();
    reload(controller.signal);
    return () => {
      controller.abort();
    };
  }, [reload]);

  const addCourse = useCallback((course: CourseSummary) => {
    setState((current) =>
      current.kind === "ok"
        ? { ...current, data: { ...current.data, courses: [...current.data.courses, course] } }
        : current,
    );
  }, []);

  const markSignedOut = useCallback(() => {
    setState({ kind: "signed-out" });
  }, []);

  return { state, reload, addCourse, markSignedOut };
}
