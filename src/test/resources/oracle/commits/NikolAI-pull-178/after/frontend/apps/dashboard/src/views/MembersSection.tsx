// AI contribution: 50%
/** The course's people: a read-only list for TAs, with promote-to-TA for instructors. */

import { useCallback, useEffect, useRef, useState, type RefObject } from "react";
import { ROLE_LABELS, type CourseMember, type CourseSummary } from "@nikolai/api-client";
import { Card } from "@nikolai/ui";
import type { DashboardClient } from "../lib/api";
import { actionErrorMessage } from "../lib/actionErrors";
import { useLoad, type Loaded } from "../lib/useLoad";

export function MembersSection({
  client,
  course,
  canPromote,
  onSignedOut,
}: {
  client: DashboardClient;
  course: CourseSummary;
  canPromote: boolean;
  onSignedOut: () => void;
}) {
  const load = useCallback(
    (signal: AbortSignal) => client.listMembers(course.id, signal),
    [client, course.id],
  );
  const { state, reload, setLoaded } = useLoad<CourseMember[]>(load, onSignedOut);
  const [error, setError] = useState<string | null>(null);
  const [promotingUserId, setPromotingUserId] = useState<string | null>(null);
  const [confirming, setConfirming] = useState<CourseMember | null>(null);
  const [announcement, setAnnouncement] = useState<string | null>(null);
  const rowButtons = useRef(new Map<string, HTMLButtonElement>());
  const rowHeadings = useRef(new Map<string, HTMLHeadingElement>());
  const keepButton = useRef<HTMLButtonElement>(null);

  const prevConfirming = useRef<CourseMember | null>(null);
  useEffect(() => {
    if (confirming !== null) {
      prevConfirming.current = confirming;
      // The confirmation just opened; give it the focus (WCAG dialog guidance).
      keepButton.current?.focus();
      return;
    }
    if (prevConfirming.current === null) return;
    // The confirmation just closed; hand focus back to the button that opened it. After a
    // successful promote that button is gone (the member is a TA), so land on the row itself.
    const userId = prevConfirming.current.user_id;
    prevConfirming.current = null;
    const target = rowButtons.current.get(userId) ?? rowHeadings.current.get(userId);
    target?.focus();
  }, [confirming]);

  async function promote(userId: string): Promise<void> {
    setPromotingUserId(userId);
    setError(null);
    const result = await client.promoteToTa(course.id, userId);
    setPromotingUserId(null);
    setConfirming(null);
    if (result.kind === "signed-out") {
      onSignedOut();
      return;
    }
    if (result.kind === "ok") {
      setLoaded((current) =>
        current.kind === "ok"
          ? {
              ...current,
              data: current.data.map((member) =>
                member.user_id === userId ? result.data : member,
              ),
            }
          : current,
      );
      setAnnouncement(`${result.data.github_login} is now a TA.`);
      return;
    }
    setError(
      actionErrorMessage(result, {
        forbidden: "Only instructors can promote a member to TA.",
        notFound: "That person isn't in this course any more.",
        conflict: "They're already a TA, or an instructor.",
      }),
    );
  }

  const lockedByConfirm = confirming !== null;

  return (
    <Card title={<h2>Members</h2>}>
      <MembersBody
        state={state}
        promotingUserId={promotingUserId}
        lockedByConfirm={lockedByConfirm}
        onAskPromote={setConfirming}
        rowButtons={rowButtons}
        rowHeadings={rowHeadings}
        canPromote={canPromote}
        onRetry={() => {
          reload();
        }}
      />
      {confirming !== null && (
        <div className="card-body">
          <PromoteConfirm
            member={confirming}
            busy={promotingUserId === confirming.user_id}
            keepRef={keepButton}
            onConfirm={() => {
              void promote(confirming.user_id);
            }}
            onCancel={() => {
              setConfirming(null);
            }}
          />
        </div>
      )}
      {error !== null && (
        <p className="error card-body" role="alert">
          {error}
        </p>
      )}
      {announcement !== null && (
        <p className="muted card-body" role="status">
          {announcement}
        </p>
      )}
    </Card>
  );
}

function PromoteConfirm({
  member,
  busy,
  keepRef,
  onConfirm,
  onCancel,
}: {
  member: CourseMember;
  busy: boolean;
  keepRef: RefObject<HTMLButtonElement | null>;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <div
      className="confirm-box"
      role="group"
      aria-label={`Promote ${member.github_login}`}
      onKeyDown={(event) => {
        if (event.key === "Escape") onCancel();
      }}
    >
      <p>
        Make {member.github_login} a TA? They will see every section of this course, read-only; only
        instructors change the join code and the teams.
      </p>
      <div className="confirm-actions">
        <button className="button" disabled={busy} onClick={onConfirm}>
          {busy ? "Promoting…" : "Make TA"}
        </button>
        <button className="button" disabled={busy} onClick={onCancel} ref={keepRef}>
          Keep as student
        </button>
      </div>
    </div>
  );
}

function MembersBody({
  state,
  canPromote,
  promotingUserId,
  lockedByConfirm,
  onAskPromote,
  rowButtons,
  rowHeadings,
  onRetry,
}: {
  state: Loaded<CourseMember[]>;
  canPromote: boolean;
  promotingUserId: string | null;
  lockedByConfirm: boolean;
  onAskPromote: (member: CourseMember) => void;
  rowButtons: RefObject<Map<string, HTMLButtonElement>>;
  rowHeadings: RefObject<Map<string, HTMLHeadingElement>>;
  onRetry: () => void;
}) {
  if (state.kind === "loading") return <p className="muted card-body">Loading members…</p>;
  if (state.kind === "ok") {
    if (state.data.length === 0) {
      return <p className="muted card-body">Nobody has joined this course yet.</p>;
    }
    return (
      <ul className="rows">
        {state.data.map((member) => (
          <MemberRow
            key={member.user_id}
            member={member}
            canPromote={canPromote}
            promoting={promotingUserId === member.user_id}
            lockedByConfirm={lockedByConfirm}
            onAskPromote={onAskPromote}
            rowButtons={rowButtons}
            rowHeadings={rowHeadings}
          />
        ))}
      </ul>
    );
  }
  return (
    <div className="card-body">
      <p className="error" role="alert">
        {actionErrorMessage(state, { forbidden: "Only course staff can see the members." })}
      </p>
      {state.kind === "unreachable" && (
        <button className="button" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}

function MemberRow({
  member,
  canPromote,
  promoting,
  lockedByConfirm,
  onAskPromote,
  rowButtons,
  rowHeadings,
}: {
  member: CourseMember;
  canPromote: boolean;
  promoting: boolean;
  lockedByConfirm: boolean;
  onAskPromote: (member: CourseMember) => void;
  rowButtons: RefObject<Map<string, HTMLButtonElement>>;
  rowHeadings: RefObject<Map<string, HTMLHeadingElement>>;
}) {
  return (
    <li className="item">
      <div>
        <h3
          tabIndex={-1}
          ref={(element) => {
            if (element) rowHeadings.current.set(member.user_id, element);
            else rowHeadings.current.delete(member.user_id);
          }}
        >
          {member.github_login}
        </h3>
        {member.display_name !== null && <p className="muted">{member.display_name}</p>}
      </div>
      <div className="member-actions">
        <span className="pill tone-neutral">{ROLE_LABELS[member.role]}</span>
        {canPromote && member.role === "student" && (
          <button
            className="button small"
            disabled={promoting || lockedByConfirm}
            aria-label={`Promote ${member.github_login} to TA`}
            ref={(element) => {
              if (element) rowButtons.current.set(member.user_id, element);
              else rowButtons.current.delete(member.user_id);
            }}
            onClick={() => {
              onAskPromote(member);
            }}
          >
            {promoting ? "Promoting…" : "Promote to TA"}
          </button>
        )}
      </div>
    </li>
  );
}
