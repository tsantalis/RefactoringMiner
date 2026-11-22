// AI contribution: 50%
/** Everything the members section needs from its four hooks, gathered in one place. */

import { useCallback, useState, type Dispatch, type SetStateAction } from "react";
import type { CourseMember } from "@nikolai/api-client";
import type { DashboardClient } from "./api";
import { useLoad, type Loaded } from "./useLoad";
import { useMemberActions, type MemberActions, type SetMembersLoaded } from "./useMemberActions";
import { useTeamAccess, type TeamAccessControls } from "./useTeamAccess";
import { useTeamAssignment, type TeamAssignment } from "./useTeamAssignment";

export interface MembersContext {
  state: Loaded<CourseMember[]>;
  reload: () => void;
  setLoaded: Dispatch<SetStateAction<Loaded<CourseMember[]>>>;
  error: string | null;
  setError: (message: string | null) => void;
  announcement: string | null;
  actions: MemberActions;
  access: TeamAccessControls;
  teamMoves: TeamAssignment;
}

export function useMembersContext(
  client: DashboardClient,
  courseId: string,
  canManage: boolean,
  onSignedOut: () => void,
): MembersContext {
  const load = useCallback(
    (signal: AbortSignal) => client.listMembers(courseId, signal),
    [client, courseId],
  );
  const { state, reload, setLoaded } = useLoad<CourseMember[]>(load, onSignedOut);
  const [error, setError] = useState<string | null>(null);
  const [announcement, setAnnouncement] = useState<string | null>(null);
  const setMembers: SetMembersLoaded = (update) => {
    setLoaded(update);
  };
  const actions = useMemberActions(
    client,
    courseId,
    onSignedOut,
    setMembers,
    setAnnouncement,
    setError,
  );
  const access = useTeamAccess(
    client,
    courseId,
    canManage,
    onSignedOut,
    reload,
    setAnnouncement,
    setError,
  );
  const teamMoves = useTeamAssignment({
    client,
    courseId,
    onSignedOut,
    setMembersLoaded: setMembers,
    setAnnouncement,
    setError,
  });
  return { state, reload, setLoaded, error, setError, announcement, actions, access, teamMoves };
}
