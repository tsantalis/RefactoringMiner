// AI contribution: 50%
/**
 * A team's planning page (US-19.1, spec #234): Brainstorm, Planning sessions, Availability and
 * Burndown. Students on the team plan here; staff and granted stakeholders read it with every
 * control off (`can_plan` is false). Later stories fill each section.
 */

import { useCallback, useEffect, useRef } from "react";
import { Card } from "@nikolai/ui";
import type { DashboardClient } from "../lib/api";
import { useLoad } from "../lib/useLoad";
import { LoadError } from "./LoadError";

// An unknown team is 403 too (the access check runs first), so there's no 404 message.
const MESSAGES = { forbidden: "You don't have access to this team's planning." };

/** Each section and what it says until the team has something in it. */
const SECTIONS: readonly (readonly [title: string, empty: string])[] = [
  ["Brainstorm", "No brainstorm issues yet."],
  ["Planning sessions", "No planning sessions yet."],
  ["Availability", "Nobody on the team has entered their availability yet."],
  ["Burndown", "No burndown yet."],
];

export function PlanningPage({
  client,
  teamId,
  back,
  onSignedOut,
}: {
  client: DashboardClient;
  teamId: string;
  /** The back button's label and action; students who land here have none. */
  back: { label: string; onBack: () => void } | null;
  onSignedOut: () => void;
}) {
  const load = useCallback(
    (signal: AbortSignal) => client.getPlanning(teamId, signal),
    [client, teamId],
  );
  const { state, reload } = useLoad(load, onSignedOut);
  const headingRef = useRef<HTMLHeadingElement>(null);
  const loaded = state.kind === "ok";
  useEffect(() => {
    if (loaded) headingRef.current?.focus();
  }, [loaded]);

  return (
    <div className="course-page">
      <div className="course-page-head">
        {back !== null && (
          <button className="button small" onClick={back.onBack}>
            {back.label}
          </button>
        )}
        {state.kind === "ok" && (
          <h2 ref={headingRef} tabIndex={-1}>
            {state.data.team.name} planning
          </h2>
        )}
      </div>
      {state.kind === "loading" && <p className="muted card-body">Loading the planning page…</p>}
      <LoadError state={state} messages={MESSAGES} onRetry={reload} />
      {state.kind === "ok" && <PlanningSections canPlan={state.data.can_plan} />}
    </div>
  );
}

function PlanningSections({ canPlan }: { canPlan: boolean }) {
  return (
    <>
      {!canPlan && (
        <p className="muted card-body">
          You're viewing this team's planning read-only. Only the team's students can change it.
        </p>
      )}
      {SECTIONS.map(([title, empty]) => (
        <Card key={title} title={<h3>{title}</h3>}>
          <p className="muted card-body">{empty}</p>
        </Card>
      ))}
    </>
  );
}
