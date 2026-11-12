// AI contribution: 50%
/** One shape for "send an action somewhere: busy, clean error, sign-out or apply". */

import { useCallback, useState } from "react";
import type { ApiResult } from "./api";
import { actionErrorMessage, type ActionMessages } from "./actionErrors";

/**
 * The busy/error flow every write on the dashboard uses: set busy, clear the old error, run the
 * action; on success, apply it; on a 401, hand the app to the sign-in screen; otherwise show
 * the caller's message for what went wrong.
 */
export function useAction<T>(
  messages: ActionMessages,
  onOk: (data: T) => void,
  onSignedOut: () => void,
): {
  busy: boolean;
  error: string | null;
  setError: (error: string | null) => void;
  run: (action: () => Promise<ApiResult<T>>) => Promise<void>;
} {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const run = useCallback(
    async (action: () => Promise<ApiResult<T>>) => {
      setBusy(true);
      setError(null);
      const result = await action();
      setBusy(false);
      if (result.kind === "signed-out") {
        onSignedOut();
        return;
      }
      if (result.kind === "ok") {
        onOk(result.data);
        return;
      }
      setError(actionErrorMessage(result, messages));
    },
    [messages, onOk, onSignedOut],
  );

  return { busy, error, setError, run };
}
