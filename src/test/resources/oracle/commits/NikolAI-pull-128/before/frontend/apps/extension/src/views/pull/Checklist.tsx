// AI contribution: 50%
/** The PR's checklist (US-9): each finding, whether it's fixed, and the student's response. */

import { useRef, useState } from "react";
import type { Finding } from "../../shared/api/contract";
import type { ApiFailure } from "../../shared/api/http";
import type { NikolaiClient } from "../../shared/api/nikolai-client";
import {
  isStaffRole,
  type FindingAction,
  type PullInsights,
} from "../../shared/api/views-contract";
import type { PullRequestRef } from "../../shared/github-url";
import { severityLabel, severityRank, severityTone } from "../../shared/status";
import { Bar, Card, Pill } from "../ui";
import { LineLink } from "./LineLink";

type Status = PullInsights["checklist"][number]["status"];
type Entry = PullInsights["checklist"][number];

export interface ChecklistItem {
  finding: Finding;
  status: Status;
  fixedIn: Entry["fixed_in"];
}

/** One item per finding, open first then by severity. Findings without a status are open. */
export function checklistItems(
  findings: Finding[],
  insights: PullInsights | null,
): ChecklistItem[] {
  const entries = new Map(insights?.checklist.map((entry) => [entry.finding_id, entry]));
  const items = findings.map((finding) => {
    const entry = entries.get(finding.id);
    return { finding, status: entry?.status ?? "open", fixedIn: entry?.fixed_in ?? null };
  });
  const done = (item: ChecklistItem): number => (item.status === "fixed" ? 1 : 0);
  return items.sort(
    (a, b) =>
      done(a) - done(b) || severityRank(a.finding.severity) - severityRank(b.finding.severity),
  );
}

const STATUS_LABELS: Partial<Record<Status, string>> = {
  acknowledged: "Acknowledged",
  disputed: "Disputed",
};

type Act = (action: FindingAction, reason: string) => void;

function DisputeForm({
  saving,
  onSend,
  onCancel,
}: {
  saving: boolean;
  onSend: (reason: string) => void;
  onCancel: () => void;
}) {
  const [reason, setReason] = useState("");
  return (
    <form
      className="stack"
      onSubmit={(event) => {
        event.preventDefault();
        onSend(reason.trim());
      }}
    >
      <label className="small strong">
        Why is this item wrong?
        <textarea
          value={reason}
          onChange={(event) => {
            setReason(event.target.value);
          }}
          required
          maxLength={1000}
        />
      </label>
      <div className="inline">
        <button className="button small" type="submit" disabled={saving || reason.trim() === ""}>
          Send dispute
        </button>
        <button className="button small" type="button" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  );
}

function Responses({
  item,
  act,
  saving,
  canAnswer,
}: {
  item: ChecklistItem;
  act: Act;
  saving: boolean;
  canAnswer: boolean;
}) {
  const [disputing, setDisputing] = useState(false);
  const label = STATUS_LABELS[item.status];
  if (label !== undefined) return <Pill tone="neutral">{label}</Pill>;
  // Course staff read the team's answers but can't answer for a student (the server refuses).
  if (!canAnswer) return null;
  if (disputing) {
    return (
      <DisputeForm
        saving={saving}
        onSend={(reason) => {
          act("dispute", reason);
        }}
        onCancel={() => {
          setDisputing(false);
        }}
      />
    );
  }
  return (
    <div className="inline no-wrap">
      <button
        className="button small"
        type="button"
        disabled={saving}
        onClick={() => {
          act("acknowledge", "");
        }}
      >
        Acknowledge
      </button>
      <button
        className="button small"
        type="button"
        disabled={saving}
        onClick={() => {
          setDisputing(true);
        }}
      >
        Dispute
      </button>
    </div>
  );
}

function ChecklistRow({
  pull,
  item,
  act,
  saving,
  canAnswer,
}: {
  pull: PullRequestRef;
  item: ChecklistItem;
  act: Act;
  saving: boolean;
  canAnswer: boolean;
}) {
  const fixed = item.status === "fixed";
  const location = item.finding.locations[0];
  return (
    <div className="item">
      <span
        className={fixed ? "check done" : "check"}
        role="img"
        aria-label={fixed ? "Fixed" : "Open"}
      >
        {fixed ? "✓" : ""}
      </span>
      <div className="stack grow" style={{ gap: 4 }}>
        <div className="inline">
          <span className={fixed ? "strong struck" : "strong"}>{item.finding.name}</span>
          <Pill tone={severityTone(item.finding.severity)}>
            {severityLabel(item.finding.severity)}
          </Pill>
          {item.finding.category !== null && <Pill tone="neutral">{item.finding.category}</Pill>}
        </div>
        {location !== undefined && (
          <LineLink
            pull={pull}
            path={location.file_path}
            start={location.start_line}
            end={location.end_line}
            side={location.side}
          />
        )}
        {item.fixedIn !== null && (
          <span className="small" style={{ color: "var(--ok-fg)" }}>
            Fixed in {item.fixedIn.commit}: {item.fixedIn.location.file_path}
          </span>
        )}
      </div>
      {!fixed && <Responses item={item} act={act} saving={saving} canAnswer={canAnswer} />}
    </div>
  );
}

const CHANGED = "This item changed. Reload the checklist.";

/** What to say when a save fails, from the server's code. Only a failure that retrying might
 * fix asks to try again. Course staff can read the checklist but not answer it. */
function saveError(failure: ApiFailure): string {
  switch (failure.kind) {
    case "signed-out":
      return "Sign in to NikolAI to answer the checklist.";
    case "forbidden":
      if (failure.code === "not_a_team_student") {
        return "Only a student on this team can answer its checklist.";
      }
      if (failure.code === "origin_not_allowed") {
        return "Your NikolAI server isn't set up to accept answers from this extension. Let your instructor know.";
      }
      break;
    case "conflict":
      return CHANGED;
    case "not-found":
      if (failure.code === "finding_not_found") return CHANGED;
      break;
  }
  return "NikolAI couldn't save that. Try again in a moment.";
}

/** Save a student's response to an item, and show it right away once the server accepts it.
 * An item's buttons are disabled while its save is in flight, so a double click sends one. */
function useFindingActions(client: NikolaiClient) {
  const [overrides, setOverrides] = useState<Record<string, Status>>({});
  const [error, setError] = useState<string | null>(null);
  const inFlight = useRef(new Set<string>());
  const [saving, setSaving] = useState<ReadonlySet<string>>(new Set());
  const track = (id: string, busy: boolean) => {
    if (busy) inFlight.current.add(id);
    else inFlight.current.delete(id);
    setSaving(new Set(inFlight.current));
  };
  const act =
    (id: string): Act =>
    (action, reason) => {
      if (inFlight.current.has(id)) return;
      setError(null);
      track(id, true);
      void client.actOnFinding(id, action, reason).then((result) => {
        track(id, false);
        if (result.kind !== "ok") {
          setError(saveError(result));
          return;
        }
        setOverrides((old) => ({
          ...old,
          [id]: action === "dispute" ? "disputed" : "acknowledged",
        }));
      });
    };
  return { overrides, error, act, saving };
}

/** The footer naming how many low-confidence findings the review left out, if any. */
function withheldNote(count: number): string | undefined {
  if (count <= 0) return undefined;
  return count === 1
    ? "1 low-confidence finding was left out."
    : `${count} low-confidence findings were left out.`;
}

export function Checklist({
  pull,
  items: listed,
  reviewed,
  insights,
  client,
}: {
  pull: PullRequestRef;
  /** From `checklistItems`. */
  items: ChecklistItem[];
  /** False until a review has completed, so an empty list isn't read as "nothing to fix". */
  reviewed: boolean;
  insights: PullInsights | null;
  client: NikolaiClient;
}) {
  const { overrides, error, act, saving } = useFindingActions(client);
  const items = listed.map((item) => ({
    ...item,
    status: overrides[item.finding.id] ?? item.status,
  }));
  const fixed = items.filter((item) => item.status === "fixed").length;
  const withheld = withheldNote(insights?.withheld_count ?? 0);
  // Only the team's students answer; without insights the role is unknown, so the server decides.
  const canAnswer = insights === null || !isStaffRole(insights.viewer.role);
  const title = (
    <>
      <span>Review checklist</span>
      <Pill tone="success">
        {fixed} of {items.length} fixed
      </Pill>
    </>
  );
  return (
    <Card title={title} footer={withheld}>
      {items.length === 0 ? (
        <div className="card-body muted">
          {reviewed
            ? "NikolAI found nothing to fix in this pull request."
            : "NikolAI hasn't reviewed the code in this pull request yet."}
        </div>
      ) : (
        <div className="card-body">
          <Bar value={fixed} max={items.length} label={`${fixed} of ${items.length} items fixed`} />
        </div>
      )}
      {error !== null && (
        <div className="card-body" role="alert" style={{ color: "var(--bad-fg)" }}>
          {error}
        </div>
      )}
      {items.map((item) => (
        <ChecklistRow
          key={item.finding.id}
          pull={pull}
          item={item}
          act={act(item.finding.id)}
          saving={saving.has(item.finding.id)}
          canAnswer={canAnswer}
        />
      ))}
    </Card>
  );
}
