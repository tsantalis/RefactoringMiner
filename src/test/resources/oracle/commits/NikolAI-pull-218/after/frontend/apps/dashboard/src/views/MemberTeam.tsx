// AI contribution: 50%
/** A member's team on the members list: a picker for instructors, plain text for TAs (#214).
 *  Only students and TAs are on teams; stakeholders read teams through access grants. */

import type { CourseMember, TeamSummary } from "@nikolai/api-client";
import type { TeamAssignment } from "../lib/useTeamAssignment";
import { MemberConfirm } from "./MemberConfirm";

export function isOnTeams(member: CourseMember): boolean {
  return member.role === "student" || member.role === "ta";
}

export function MemberTeam({
  member,
  teams,
  canManage,
  locked,
  assignment,
}: {
  member: CourseMember;
  teams: TeamSummary[];
  canManage: boolean;
  locked: boolean;
  assignment: TeamAssignment;
}) {
  if (!isOnTeams(member)) return null;
  const current = member.team;
  if (!canManage) {
    return <p className="muted">{current === null ? "No team" : `Team: ${current.name}`}</p>;
  }
  return (
    <label className="member-team">
      Team
      <select
        aria-label={`Team for ${member.github_login}`}
        value={current?.id ?? ""}
        disabled={locked || assignment.busy}
        ref={(element) => {
          if (element) assignment.pickers.current.set(member.user_id, element);
          else assignment.pickers.current.delete(member.user_id);
        }}
        onChange={(event) => {
          const team = teams.find((candidate) => candidate.id === event.target.value);
          if (team !== undefined && team.id !== current?.id) assignment.ask(member, team);
        }}
      >
        {current === null && (
          <option value="" disabled>
            No team
          </option>
        )}
        {teams.map((team) => (
          <option key={team.id} value={team.id}>
            {team.name}
          </option>
        ))}
      </select>
    </label>
  );
}

export function TeamMoveConfirm({
  member,
  assignment,
}: {
  member: CourseMember;
  assignment: TeamAssignment;
}) {
  const pending = assignment.pending;
  if (pending === null || pending.member.user_id !== member.user_id) return null;
  const login = member.github_login;
  const target = pending.team.name;
  const current = member.team;
  const prompt = movePrompt(member, target);
  return (
    <div className="row-confirm">
      <MemberConfirm
        busy={assignment.busy}
        keepRef={assignment.keepButton}
        prompt={prompt}
        groupLabel={`Put ${login} on a team`}
        confirmLabel={`Move to ${target}`}
        busyLabel="Moving…"
        keepLabel={current === null ? "Cancel" : `Keep on ${current.name}`}
        onConfirm={() => {
          void assignment.confirm();
        }}
        onCancel={assignment.cancel}
      />
    </div>
  );
}

/** What the move means for this member. A TA reads every team anyway, so only their team changes. */
function movePrompt(member: CourseMember, target: string): string {
  const login = member.github_login;
  const current = member.team;
  const ask = current === null ? `Put ${login} on ${target}?` : `Move ${login} to ${target}?`;
  if (member.role === "ta") return `${ask} As a TA they still read every team in the course.`;
  if (current === null)
    return `${ask} They will read ${target}'s repositories and NikolAI feedback.`;
  return `${ask} They will leave ${current.name} and stop seeing its repositories and NikolAI feedback.`;
}
