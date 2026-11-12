// AI contribution: 50%
/** The signed-out screen: one explanation and one way in. */

import { StateMessage } from "@nikolai/ui";

export function SignIn({ signInUrl }: { signInUrl: string }) {
  return (
    <main className="page">
      <StateMessage
        live="status"
        title="NikolAI dashboard"
        detail="Sign in with GitHub to see and manage your courses."
        action={
          <a className="button primary" href={signInUrl}>
            Sign in with GitHub
          </a>
        }
      />
    </main>
  );
}
