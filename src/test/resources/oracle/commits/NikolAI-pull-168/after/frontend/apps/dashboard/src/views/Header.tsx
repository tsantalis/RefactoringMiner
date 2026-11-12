// AI contribution: 50%
/** The signed-in user's name and the button that ends their session. */

import { useState } from "react";
import type { CurrentUser } from "@nikolai/api-client";
import type { DashboardClient } from "../lib/api";

export function Header({
  user,
  client,
  onSignedOut,
}: {
  user: CurrentUser;
  client: DashboardClient;
  onSignedOut: () => void;
}) {
  const [error, setError] = useState<string | null>(null);
  async function signOut(): Promise<void> {
    const result = await client.signOut();
    if (result.kind === "ok" || result.kind === "signed-out") {
      onSignedOut();
      return;
    }
    setError("Couldn't sign out. Try again.");
  }
  return (
    <header className="topbar">
      <div>
        <h1 className="title">NikolAI dashboard</h1>
        <p className="muted">
          Signed in as <strong>{user.github_login}</strong>
        </p>
      </div>
      <button className="button" onClick={() => void signOut()}>
        Sign out
      </button>
      {error !== null && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
    </header>
  );
}
