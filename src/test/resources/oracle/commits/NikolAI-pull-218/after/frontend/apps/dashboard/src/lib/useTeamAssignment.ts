// AI contribution: 50%
/** Moving a member onto a team from the members list (#214): ask, confirm, then move. */

import { useEffect, useRef, useState, type RefObject } from "react";
import type { CourseMember, TeamSummary } from "@nikolai/api-client";
import { actionErrorMessage } from "./actionErrors";
import { patchMember, type SetMembersLoaded } from "./useMemberActions";
import type { DashboardClient } from "./api";

export interface TeamMove {
  member: CourseMember;
  team: TeamSummary;
}

export interface TeamAssignment {
  /** The move waiting for confirmation, if any. */
  pending: TeamMove | null;
  busy: boolean;
  keepButton: RefObject<HTMLButtonElement | null>;
  pickers: RefObject<Map<string, HTMLSelectElement>>;
  ask: (member: CourseMember, team: TeamSummary) => void;
  cancel: () => void;
  confirm: () => Promise<void>;
}

/** What a move needs from the members section: the API, the list and its messages. */
export interface MoveContext {
  client: DashboardClient;
  courseId: string;
  onSignedOut: () => void;
  setMembersLoaded: SetMembersLoaded;
  setAnnouncement: (message: string | null) => void;
  setError: (message: string | null) => void;
}

const ERRORS = {
  forbidden: "Only instructors can put members on a team.",
  notFound: "That person or team isn't in this course any more.",
  conflict: "That person can't be put on a team.",
};

/** Focus the safe choice when the confirmation opens, and the member's picker when it closes. */
function useMoveFocus(
  pending: TeamMove | null,
  keepButton: RefObject<HTMLButtonElement | null>,
  pickers: RefObject<Map<string, HTMLSelectElement>>,
): void {
  const previous = useRef<TeamMove | null>(null);
  useEffect(() => {
    if (pending !== null) {
      previous.current = pending;
      keepButton.current?.focus();
      return;
    }
    if (previous.current === null) return;
    pickers.current.get(previous.current.member.user_id)?.focus();
    previous.current = null;
  }, [pending, keepButton, pickers]);
}

/** Send one confirmed move and report it; the list updates only when the server agrees. */
async function sendMove(context: MoveContext, { member, team }: TeamMove): Promise<void> {
  context.setError(null);
  context.setAnnouncement(null);
  const result = await context.client.assignTeam(context.courseId, team.id, member.github_login);
  if (result.kind === "signed-out") {
    context.onSignedOut();
    return;
  }
  if (result.kind !== "ok") {
    context.setError(actionErrorMessage(result, ERRORS));
    return;
  }
  const moved = { id: result.data.id, name: result.data.name };
  patchMember(context.setMembersLoaded, member.user_id, (m) => ({ ...m, team: moved }));
  context.setAnnouncement(`${member.github_login} is now on ${moved.name}.`);
}

export function useTeamAssignment(context: MoveContext): TeamAssignment {
  const [pending, setPending] = useState<TeamMove | null>(null);
  const [busy, setBusy] = useState(false);
  const keepButton = useRef<HTMLButtonElement>(null);
  const pickers = useRef(new Map<string, HTMLSelectElement>());
  useMoveFocus(pending, keepButton, pickers);

  return {
    pending,
    busy,
    keepButton,
    pickers,
    ask: (member, team) => {
      setPending({ member, team });
    },
    cancel: () => {
      setPending(null);
    },
    confirm: async () => {
      if (pending === null) return;
      setBusy(true);
      await sendMove(context, pending);
      setBusy(false);
      setPending(null);
    },
  };
}
