// AI contribution: 50%
/** The NikolAI tab on a pull request: checklist, refactorings, your review, and staff marking. */

import { useState } from "react";
import type { PullRequestFeedback } from "../../shared/api/contract";
import type { ApiResult } from "../../shared/api/http";
import type { PullInsights, PullRefactorings } from "../../shared/api/views-contract";
import { formatLatency, timeAgo } from "../../shared/format";
import type { PullRequestRef } from "../../shared/github-url";
import { routeHash } from "../../shared/page";
import { isStaffRole } from "../../shared/api/views-contract";
import type { Clients } from "../../shared/settings";
import { runStatusView } from "../../shared/status";
import { StaffBar } from "../StaffBar";
import { StateMessage } from "../StateMessage";
import { Card, Counter, Pill, SampleNote } from "../ui";
import { useResource, type Loaded } from "../useResource";
import { Checklist, checklistItems } from "./Checklist";
import { Marking, Refactorings, YourReview } from "./PullSections";

type Section = "checklist" | "refactorings" | "review" | "marking";

const SECTIONS: { id: Section; label: string; staffOnly: boolean }[] = [
  { id: "checklist", label: "Checklist", staffOnly: false },
  { id: "refactorings", label: "Refactorings", staffOnly: false },
  { id: "review", label: "Your review", staffOnly: false },
  { id: "marking", label: "Marking", staffOnly: true },
];

function asSection(value: string | null, staff: boolean): Section {
  const match = SECTIONS.find((section) => section.id === value);
  return match && (staff || !match.staffOnly) ? match.id : "checklist";
}

function PushHistory({ pushes }: { pushes: PullInsights["pushes"] }) {
  if (pushes.length === 0) return null;
  return (
    <Card title="Push history">
      <div className="card-body small">
        {pushes.map((push) => (
          <div key={push.commit} className="inline">
            <span className="mono">{push.commit}</span>
            <span className="muted">{push.message}</span>
            <span className="spacer" />
            {push.fixed > 0 && <Pill tone="success">+{push.fixed} fixed</Pill>}
            {push.added > 0 && <Pill tone="attention">+{push.added} new</Pill>}
          </div>
        ))}
      </div>
    </Card>
  );
}

function AboutReview({ feedback }: { feedback: PullRequestFeedback }) {
  const latest = feedback.runs[0];
  return (
    <Card title="About this review">
      <div className="card-body small">
        {latest === undefined ? (
          <span className="muted">NikolAI hasn't reviewed this pull request yet.</span>
        ) : (
          <>
            <div className="inline">
              <Pill tone={runStatusView(latest.status).tone}>
                {runStatusView(latest.status).label}
              </Pill>
              <span className="muted">{timeAgo(latest.created_at)}</span>
            </div>
            {latest.review !== null && (
              <span className="muted">
                {latest.review.model} · {formatLatency(latest.review.latency_ms)}
              </span>
            )}
          </>
        )}
        <span className="muted">
          Code and comments in the PR are read as data. NikolAI never follows instructions found in
          them.
        </span>
      </div>
    </Card>
  );
}

interface PullData {
  pull: PullRequestRef;
  feedback: PullRequestFeedback;
  insights: PullInsights | null;
  /** Loaded on its own, so the tab works before the other insights exist (US-8). */
  refactorings: Loaded<PullRefactorings>;
  clients: Clients;
}

function SectionPicker({
  current,
  staff,
  data,
  pick,
}: {
  current: Section;
  staff: boolean;
  data: PullData;
  pick: (section: Section) => void;
}) {
  const open = checklistItems(data.feedback.findings, data.insights).filter(
    (item) => item.status !== "fixed",
  );
  const counts: Partial<Record<Section, number>> = { checklist: open.length };
  if (data.refactorings.kind === "ok") counts.refactorings = data.refactorings.data.items.length;
  return (
    <div className="segmented" role="group" aria-label="NikolAI views">
      {SECTIONS.filter((section) => staff || !section.staffOnly).map((section) => (
        <button
          key={section.id}
          type="button"
          aria-pressed={current === section.id}
          onClick={() => {
            pick(section.id);
          }}
        >
          {section.label}
          {counts[section.id] !== undefined && <Counter>{counts[section.id]}</Counter>}
        </button>
      ))}
    </div>
  );
}

function SectionBody({ current, data }: { current: Section; data: PullData }) {
  const { pull, feedback, insights, refactorings, clients } = data;
  switch (current) {
    case "checklist":
      return (
        <Checklist
          pull={pull}
          findings={feedback.findings}
          insights={insights}
          client={clients.nikolai}
        />
      );
    case "refactorings":
      return refactorings.kind === "ok" ? (
        <Refactorings pull={pull} refactorings={refactorings.data} />
      ) : (
        <StateMessage state={refactorings} subject="this pull request's refactorings" />
      );
    case "review":
      return <YourReview pull={pull} review={insights?.your_review ?? null} />;
    case "marking":
      return insights?.marking == null ? null : <Marking marking={insights.marking} />;
  }
}

async function loadFeedback(
  clients: Clients,
  pull: PullRequestRef,
  signal: AbortSignal,
): Promise<ApiResult<PullRequestFeedback>> {
  const result = await clients.feedback.getFeedback(pull, signal);
  return result.kind === "ok" ? { kind: "ok", data: result.feedback } : result;
}

export function PullView({
  pull,
  section,
  clients,
}: {
  pull: PullRequestRef;
  section: string | null;
  clients: Clients;
}) {
  const key = `${pull.owner}/${pull.repo}#${pull.number}`;
  const feedback = useResource((signal) => loadFeedback(clients, pull, signal), key);
  const extra = useResource((signal) => clients.nikolai.getPullInsights(pull, signal), key);
  const refactorings = useResource((signal) => clients.nikolai.getRefactorings(pull, signal), key);
  const [chosen, setChosen] = useState<string | null>(section);
  if (feedback.kind !== "ok") return <StateMessage state={feedback} subject="this pull request" />;
  // The checklist works from the stored findings alone, so missing insights only hide extras.
  const insights = extra.kind === "ok" ? extra.data : null;
  const data: PullData = {
    pull,
    feedback: feedback.data,
    insights,
    refactorings,
    clients,
  };
  const staff = insights !== null && isStaffRole(insights.viewer.role);
  const current = asSection(chosen, staff);
  const pick = (next: Section): void => {
    setChosen(next);
    window.history.replaceState(null, "", routeHash(next));
  };
  return (
    <>
      {staff && <StaffBar viewer={insights.viewer} course={null} />}
      <div className="page">
        <div className="row">
          <main className="grow">
            <SectionPicker current={current} staff={staff} data={data} pick={pick} />
            <SectionBody current={current} data={data} />
            <SampleNote isSample={clients.nikolai.isSample} />
          </main>
          <aside className="side">
            {insights !== null && <PushHistory pushes={insights.pushes} />}
            <AboutReview feedback={feedback.data} />
          </aside>
        </div>
      </div>
    </>
  );
}
