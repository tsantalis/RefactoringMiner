// AI contribution: 50%
/** What a view shows instead of its data: loading, signed out, no access, and so on. */

import { createContext, useContext } from "react";
import { StateMessage as StatePanel } from "@nikolai/ui";
import type { Loaded } from "./useResource";

type NotOk = Exclude<Loaded<never>, { kind: "ok" }>;

/** The server's sign-in page, or null on sample data. `App` provides it to every view. */
export const SignInUrlContext = createContext<string | null>(null);

function messageFor(
  state: NotOk,
  subject: string,
  canSignIn: boolean,
): { title: string; detail: string } {
  switch (state.kind) {
    case "loading":
      return { title: "Loading NikolAI…", detail: "" };
    case "signed-out":
      return {
        title: "Sign in to NikolAI",
        detail: canSignIn
          ? "Sign in with GitHub in a new tab; this page loads again when you come back."
          : "Sign in with GitHub on your NikolAI server, then come back to this page.",
      };
    case "forbidden":
      return {
        title: "You don't have access",
        detail: `NikolAI only shows ${subject} to its team and the course's TAs and instructors.`,
      };
    case "not-registered":
      return {
        title: "NikolAI isn't set up here",
        detail: "Ask your instructor to install the course's GitHub App on this repository.",
      };
    case "not-found":
      return { title: "Nothing here yet", detail: `NikolAI has no record of ${subject} yet.` };
    case "conflict":
      return { title: "This changed", detail: "Reload this page to see NikolAI's latest." };
    case "unreachable":
      return { title: "Can't reach NikolAI", detail: state.reason };
  }
}

export function StateMessage({ state, subject }: { state: NotOk; subject: string }) {
  const signInUrl = useContext(SignInUrlContext);
  const { title, detail } = messageFor(state, subject, signInUrl !== null);
  return (
    <StatePanel
      live={state.kind === "loading" ? "status" : "alert"}
      title={title}
      detail={detail === "" ? undefined : detail}
      action={
        state.kind === "signed-out" && signInUrl !== null ? (
          // A new tab: GitHub's OAuth pages refuse to load in a frame, and this page keeps its place.
          <a className="button primary" href={signInUrl} target="_blank" rel="noopener noreferrer">
            Sign in with GitHub
          </a>
        ) : undefined
      }
    />
  );
}
