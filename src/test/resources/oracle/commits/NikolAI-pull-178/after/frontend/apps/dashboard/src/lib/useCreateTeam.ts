// AI contribution: 50%
/** The create-team submit cycle: busy, clean error, sign-out on 401, the new team out. */

import type { TeamSummary } from "@nikolai/api-client";
import type { DashboardClient } from "./api";
import { useAction } from "./useAction";

export function useCreateTeam(
  client: DashboardClient,
  courseId: string,
  onCreated: (team: TeamSummary) => void,
  onSignedOut: () => void,
): {
  busy: boolean;
  error: string | null;
  setError: (error: string | null) => void;
  create: (name: string) => Promise<void>;
} {
  const { busy, error, setError, run } = useAction<TeamSummary>(
    {
      forbidden: "Only instructors can create teams.",
      conflict: "This course already has a team with that name.",
    },
    onCreated,
    onSignedOut,
  );

  return { busy, error, setError, create: (name) => run(() => client.createTeam(courseId, name)) };
}
