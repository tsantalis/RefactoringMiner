// AI contribution: 50%
/** One shape for "load something, show loading, keep the retry loop minimal". */

import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type Dispatch,
  type SetStateAction,
} from "react";
import type { ApiResult } from "./api";

export type Loaded<T> = { kind: "loading" } | Exclude<ApiResult<T>, { kind: "signed-out" }>;

/**
 * Load something from the API on mount, with the same abort and retry rules as the rest of the
 * dashboard. A 401 routes to the sign-in screen instead of a retry loop. `setLoaded` lets the
 * caller patch the loaded data in place (e.g. after a promote), without refiring the fetch.
 */
export function useLoad<T>(
  load: (signal: AbortSignal) => Promise<ApiResult<T>>,
  onSignedOut: () => void,
): {
  state: Loaded<T>;
  reload: () => void;
  setLoaded: Dispatch<SetStateAction<Loaded<T>>>;
} {
  const [state, setLoaded] = useState<Loaded<T>>({ kind: "loading" });
  const loadRef = useRef(load);
  const signedOutRef = useRef(onSignedOut);
  const currentRef = useRef<AbortController | null>(null);
  loadRef.current = load;
  signedOutRef.current = onSignedOut;

  const reload = useCallback(() => {
    currentRef.current?.abort();
    setLoaded({ kind: "loading" });
    const controller = new AbortController();
    currentRef.current = controller;
    loadRef.current(controller.signal).then(
      (result) => {
        if (controller.signal.aborted) return;
        if (result.kind === "signed-out") {
          signedOutRef.current();
          return;
        }
        setLoaded(result);
      },
      (cause: unknown) => {
        if (controller.signal.aborted) return;
        console.error("NikolAI: the dashboard failed to load data.", cause);
        setLoaded({ kind: "unreachable", reason: "Something went wrong loading NikolAI." });
      },
    );
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);
  useEffect(
    () => () => {
      currentRef.current?.abort();
    },
    [],
  );

  return { state, reload, setLoaded };
}
