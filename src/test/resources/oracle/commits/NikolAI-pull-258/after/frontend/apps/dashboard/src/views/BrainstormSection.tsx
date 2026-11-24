// AI contribution: 50%
/**
 * The planning page's Brainstorm section (US-20.2, #245): each of the team's repositories with its
 * open brainstorm issues, each linking to GitHub. It loads on its own, so a slow or failing GitHub
 * never hides the rest of the planning page. Staff read the same list: it has no controls.
 */

import { useCallback } from "react";
import type { RepositoryBrainstorm } from "@nikolai/api-client";
import { Card } from "@nikolai/ui";
import type { DashboardClient } from "../lib/api";
import { useLoad } from "../lib/useLoad";
import { LoadError } from "./LoadError";

const MESSAGES = { forbidden: "You don't have access to this team's brainstorm issues." };
const NO_REPOSITORY =
  "This team has no repository linked yet, so there are no brainstorm issues to list.";

export function BrainstormSection({
  client,
  teamId,
  onSignedOut,
}: {
  client: DashboardClient;
  teamId: string;
  onSignedOut: () => void;
}) {
  const load = useCallback(
    (signal: AbortSignal) => client.getBrainstorm(teamId, signal),
    [client, teamId],
  );
  const { state, reload } = useLoad(load, onSignedOut);

  return (
    <Card title={<h3>Brainstorm</h3>}>
      {state.kind === "loading" && <p className="muted card-body">Loading brainstorm issues…</p>}
      <LoadError state={state} messages={MESSAGES} onRetry={reload} />
      {state.kind === "ok" && state.data.repositories.length === 0 && (
        <p className="muted card-body">{NO_REPOSITORY}</p>
      )}
      {state.kind === "ok" &&
        state.data.repositories.map((repository) => (
          <RepositoryBrainstormIssues
            key={repository.full_name}
            repository={repository}
            label={state.data.label}
          />
        ))}
    </Card>
  );
}

/** `label` is the brainstorm label as the API sends it, so the page never spells it out. */
function RepositoryBrainstormIssues({
  repository,
  label,
}: {
  repository: RepositoryBrainstorm;
  label: string;
}) {
  const { full_name: fullName, status, issues, more, all_url: allUrl } = repository;
  return (
    <div className="card-body">
      <h4>{fullName}</h4>
      {status === "failed" && (
        <p className="error" role="alert">
          NikolAI couldn't read the brainstorm issues of {fullName} from GitHub. Try again later.
        </p>
      )}
      {status === "listed" && issues.length === 0 && (
        <p className="muted">
          No open brainstorm issues yet. Label any issue <code>{label}</code> on GitHub to record an
          idea: it shows up here and joins the repository&apos;s brainstorm epic.
        </p>
      )}
      {issues.length > 0 && (
        <ul>
          {issues.map((issue) => (
            <li key={issue.number}>
              <a href={issue.url} target="_blank" rel="noreferrer">
                #{issue.number} {issue.title}
              </a>
            </li>
          ))}
        </ul>
      )}
      {more && (
        <a href={allUrl} target="_blank" rel="noreferrer">
          See all open brainstorm issues on GitHub
        </a>
      )}
    </div>
  );
}
