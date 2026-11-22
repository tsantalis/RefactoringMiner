// AI contribution: 50%
/** Everything the members section needs from its three hooks, gathered in one place. */

import { useCallback, useState, type Dispatch, type SetStateAction } from "react";
import type { CourseMember } from "@nikolai/api-client";
import type { DashboardClient } from "./api";
import { useLoad, type Loaded } from "./useLoad";
import { useMemberActions, type MemberActions } from "./useMemberActions";
import { useTeamAccess, type TeamAccessControls } from "./useTeamAccess";

export interface MembersContext {
  state: Loaded<CourseMember[]>;
  reload: () => void;
  setLoaded: Dispatch<SetStateAction<Loaded<CourseMember[]>>>;
  error: string | null;
  setError: (message: string | null) => void;
  announcement: string | null;
  actions: MemberActions;
  access: TeamAccessControls;
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
  const actions = useMemberActions(
    client,
    courseId,
    onSignedOut,
    (update) => {
      setLoaded(update);
    },
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
  return { state, reload, setLoaded, error, setError, announcement, actions, access };
}
