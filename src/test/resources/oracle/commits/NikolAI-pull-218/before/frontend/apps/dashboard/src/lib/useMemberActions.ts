// AI contribution: 50%
/** The per-member actions (promote to TA, make stakeholder): one table, one hook. */

import { useEffect, useRef, useState, type RefObject } from "react";
import type { CourseMember } from "@nikolai/api-client";
import { actionErrorMessage } from "./actionErrors";
import { type Loaded } from "./useLoad";
import type { ApiResult, DashboardClient } from "./api";

export type MemberAction = "promote" | "stakeholder";

export interface PendingAction {
  member: CourseMember;
  action: MemberAction;
}

interface MemberActionDef {
  call: (
    client: DashboardClient,
    courseId: string,
    userId: string,
  ) => Promise<ApiResult<CourseMember>>;
  ariaLabel: (login: string) => string;
  buttonLabel: (busy: boolean) => string;
  confirmLabel: string;
  busyLabel: string;
  groupLabel: (login: string) => string;
  prompt: (login: string) => string;
  done: (login: string) => string;
  errors: { forbidden: string; notFound: string; conflict: string };
}

export const MEMBER_ACTIONS: Record<MemberAction, MemberActionDef> = {
  promote: {
    call: (client, courseId, userId) => client.promoteToTa(courseId, userId),
    ariaLabel: (login) => `Promote ${login} to TA`,
    buttonLabel: (busy) => (busy ? "Promoting…" : "Promote to TA"),
    confirmLabel: "Make TA",
    busyLabel: "Promoting…",
    groupLabel: (login) => `Promote ${login}`,
    prompt: (login) =>
      `Make ${login} a TA? They will see every section of this course, read-only; only ` +
      "instructors change the join code and the teams.",
    done: (login) => `${login} is now a TA.`,
    errors: {
      forbidden: "Only instructors can promote a member to TA.",
      notFound: "That person isn't in this course any more.",
      conflict: "Only a student can be promoted to TA.",
    },
  },
  stakeholder: {
    call: (client, courseId, userId) => client.makeStakeholder(courseId, userId),
    ariaLabel: (login) => `Make ${login} a stakeholder`,
    buttonLabel: (busy) => (busy ? "Making stakeholder…" : "Make stakeholder"),
    confirmLabel: "Make stakeholder",
    busyLabel: "Making stakeholder…",
    groupLabel: (login) => `Make stakeholder ${login}`,
    prompt: (login) =>
      `Make ${login} a stakeholder? They can read and edit the course and project ` +
      "requirements, and only the teams you grant them access to.",
    done: (login) => `${login} is now a stakeholder.`,
    errors: {
      forbidden: "Only instructors can make someone a stakeholder.",
      notFound: "That person isn't in this course any more.",
      conflict: "Only a student can be made a stakeholder.",
    },
  },
};

export interface MemberActions {
  acting: PendingAction | null;
  confirming: PendingAction | null;
  /** True while a confirmation is open: rows can't start another action. */
  locked: boolean;
  confirmBusy: boolean;
  actionButtons: RefObject<Map<string, HTMLButtonElement>>;
  rowHeadings: RefObject<Map<string, HTMLHeadingElement>>;
  keepButton: RefObject<HTMLButtonElement | null>;
  ask: (member: CourseMember, action: MemberAction) => void;
  cancel: () => void;
  confirm: () => Promise<void>;
}

/** Hand focus into the confirmation when it opens, and back to the opening control on close. */
function useConfirmFocus(
  confirming: PendingAction | null,
  keepButton: RefObject<HTMLButtonElement | null>,
  actionButtons: RefObject<Map<string, HTMLButtonElement>>,
  rowHeadings: RefObject<Map<string, HTMLHeadingElement>>,
): void {
  const prevConfirming = useRef<PendingAction | null>(null);
  useEffect(() => {
    if (confirming !== null) {
      prevConfirming.current = confirming;
      // The confirmation just opened; focus the safe choice in it.
      keepButton.current?.focus();
      return;
    }
    if (prevConfirming.current === null) return;
    // The confirmation just closed; hand focus back to the control that opened it. After a
    // successful change that button is gone (the member's role changed), so land on the row.
    const { member, action } = prevConfirming.current;
    prevConfirming.current = null;
    const target =
      actionButtons.current.get(`${action}:${member.user_id}`) ??
      rowHeadings.current.get(member.user_id);
    target?.focus();
  }, [confirming, keepButton, actionButtons, rowHeadings]);
}

function makeConfirmMember(
  client: DashboardClient,
  courseId: string,
  onSignedOut: () => void,
  setMembersLoaded: (update: (current: Loaded<CourseMember[]>) => Loaded<CourseMember[]>) => void,
  setActing: (pending: PendingAction | null) => void,
  setConfirming: (pending: PendingAction | null) => void,
  setAnnouncement: (message: string | null) => void,
  setError: (message: string | null) => void,
  confirming: PendingAction | null,
): () => Promise<void> {
  return async function confirm(): Promise<void> {
    if (confirming === null) return;
    const { member, action } = confirming;
    setActing({ member, action });
    setError(null);
    setAnnouncement(null);
    const result = await MEMBER_ACTIONS[action].call(client, courseId, member.user_id);
    setActing(null);
    setConfirming(null);
    if (result.kind === "signed-out") {
      onSignedOut();
      return;
    }
    if (result.kind === "ok") {
      patchMember(setMembersLoaded, member.user_id, result.data);
      setAnnouncement(MEMBER_ACTIONS[action].done(result.data.github_login));
      return;
    }
    setError(actionErrorMessage(result, MEMBER_ACTIONS[action].errors));
  };
}

export function useMemberActions(
  client: DashboardClient,
  courseId: string,
  onSignedOut: () => void,
  setMembersLoaded: (update: (current: Loaded<CourseMember[]>) => Loaded<CourseMember[]>) => void,
  setAnnouncement: (message: string | null) => void,
  setError: (message: string | null) => void,
): MemberActions {
  const [acting, setActing] = useState<PendingAction | null>(null);
  const [confirming, setConfirming] = useState<PendingAction | null>(null);
  const actionButtons = useRef(new Map<string, HTMLButtonElement>());
  const rowHeadings = useRef(new Map<string, HTMLHeadingElement>());
  const keepButton = useRef<HTMLButtonElement>(null);
  useConfirmFocus(confirming, keepButton, actionButtons, rowHeadings);
  const confirm = makeConfirmMember(
    client,
    courseId,
    onSignedOut,
    setMembersLoaded,
    setActing,
    setConfirming,
    setAnnouncement,
    setError,
    confirming,
  );

  return {
    acting,
    confirming,
    locked: confirming !== null,
    confirmBusy:
      acting !== null && confirming !== null && acting.member.user_id === confirming.member.user_id,
    actionButtons,
    rowHeadings,
    keepButton,
    ask: (member, action) => {
      setConfirming({ member, action });
    },
    cancel: () => {
      setConfirming(null);
    },
    confirm,
  };
}

function patchMember(
  setMembersLoaded: (update: (current: Loaded<CourseMember[]>) => Loaded<CourseMember[]>) => void,
  userId: string,
  member: CourseMember,
): void {
  setMembersLoaded((current) =>
    current.kind === "ok"
      ? { ...current, data: current.data.map((m) => (m.user_id === userId ? member : m)) }
      : current,
  );
}
