// AI contribution: 50%
/** Every screen the page shows before it has a signed-in user. */

import { REQUEST_FAILED } from "../lib/api";
import type { Loaded } from "../lib/useCurrentUser";
import { SignIn } from "./SignIn";
import { Status } from "./Status";

export function LoadState({
  state,
  signInUrl,
  onRetry,
}: {
  state: Exclude<Loaded, { kind: "ok" }>;
  signInUrl: string;
  onRetry: () => void;
}) {
  switch (state.kind) {
    case "loading":
      return <Status title="Loading NikolAI…" live="status" />;
    case "signed-out":
      return <SignIn signInUrl={signInUrl} />;
    case "unreachable":
      return <Retry detail={state.reason} onRetry={onRetry} />;
    default:
      // A 403 or another status from something in front of the API: it can't name a screen.
      return <Retry detail={REQUEST_FAILED} onRetry={onRetry} />;
  }
}

/** The same failure screen for every unreachable answer, with the one action that helps. */
function Retry({ detail, onRetry }: { detail: string; onRetry: () => void }) {
  return (
    <Status
      title="Can't reach NikolAI"
      detail={detail}
      action={
        <button
          className="button"
          onClick={() => {
            onRetry();
          }}
        >
          Try again
        </button>
      }
    />
  );
}
