// AI contribution: 50%
/** The course's people: a read-only list for TAs; instructors can promote a student to TA or
 *  make them a stakeholder, and give each stakeholder read access to chosen teams. */

import {
  ROLE_LABELS,
  type CourseMember,
  type CourseSummary,
  type TeamAccessGrant,
  type TeamSummary,
} from "@nikolai/api-client";
import { Card } from "@nikolai/ui";
import type { DashboardClient } from "../lib/api";
import { type Loaded } from "../lib/useLoad";
import { useRefreshOnBump } from "../lib/useRefreshOnBump";
import { useMembersContext } from "../lib/useMembersContext";
import { MEMBER_ACTIONS, type MemberAction, type MemberActions } from "../lib/useMemberActions";
import { MemberConfirm } from "./MemberConfirm";
import { TeamAccess } from "./TeamAccess";
import { LoadError } from "./LoadError";

export function MembersSection({
  client,
  course,
  canManage,
  refreshKey,
  onSignedOut,
}: {
  client: DashboardClient;
  course: CourseSummary;
  canManage: boolean;
  /** Bumped to reload the members list after a roster import applies to someone signed in. */
  refreshKey: number;
  onSignedOut: () => void;
}) {
  const { state, reload, error, announcement, actions, access } = useMembersContext(
    client,
    course.id,
    canManage,
    onSignedOut,
  );
  useRefreshOnBump(refreshKey, reload);

  return (
    <Card title={<h2>Members</h2>}>
      <MembersBody
        state={state}
        grants={access.grants}
        teams={course.teams}
        grantsReady={access.grantsReady}
        grantBusyKey={access.grantBusyKey}
        actions={actions}
        canManage={canManage}
        onToggleGrant={(member, team, isCurrentlyGranted) =>
          void access.toggle(member, team, isCurrentlyGranted)
        }
        onRetry={reload}
      />
      {access.grantsError !== null && (
        <GrantsRetry message={access.grantsError} onRetry={access.reloadGrants} />
      )}
      {state.kind === "ok" && (
        <GrantsUnlinked
          grants={access.grants}
          members={state.data}
          teams={course.teams}
          grantBusyKey={access.grantBusyKey}
          onRevoke={(grant, team) => {
            // A revoke for somebody not on the page goes through the same path, with a stand-in
            // member row the toggle needs before the members slice exists.
            void access.toggle(
              {
                user_id: grant.user_id,
                github_login: grant.github_login,
                display_name: null,
                role: "student",
              },
              team,
              true,
            );
          }}
        />
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

function GrantsRetry({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="card-body">
      <p className="error" role="alert">
        {message}
      </p>
      <button className="button small" onClick={onRetry}>
        Retry team access
      </button>
    </div>
  );
}

function GrantsUnlinked({
  grants,
  members,
  teams,
  grantBusyKey,
  onRevoke,
}: {
  grants: TeamAccessGrant[];
  members: CourseMember[];
  teams: TeamSummary[];
  grantBusyKey: string | null;
  onRevoke: (grant: TeamAccessGrant, team: TeamSummary) => void;
}) {
  const memberIds = new Set(members.map((member) => member.user_id));
  const unlinked = grants.filter((grant) => !memberIds.has(grant.user_id));
  if (unlinked.length === 0) return null;
  const teamOf = (teamId: string): TeamSummary | undefined =>
    teams.find((team) => team.id === teamId);
  return (
    <div className="card-body">
      <h3>Access not tied to a course member</h3>
      <ul className="rows">
        {unlinked.map((grant) => {
          const team = teamOf(grant.team_id);
          if (team === undefined) return null;
          const key = `${grant.team_id}:${grant.user_id}`;
          return (
            <li className="item" key={key}>
              <div>
                <h3>{grant.github_login}</h3>
                <p className="muted">Can read {team.name}; not a member of this course.</p>
              </div>
              <button
                className="button small"
                disabled={grantBusyKey === key}
                aria-label={`Revoke ${grant.github_login}'s access to ${team.name}`}
                onClick={() => {
                  onRevoke(grant, team);
                }}
              >
                Revoke
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

function MembersBody({
  state,
  grants,
  teams,
  grantsReady,
  grantBusyKey,
  actions,
  canManage,
  onToggleGrant,
  onRetry,
}: {
  state: Loaded<CourseMember[]>;
  grants: TeamAccessGrant[];
  teams: TeamSummary[];
  grantsReady: boolean;
  grantBusyKey: string | null;
  actions: MemberActions;
  canManage: boolean;
  onToggleGrant: (member: CourseMember, team: TeamSummary, isCurrentlyGranted: boolean) => void;
  onRetry: () => void;
}) {
  if (state.kind === "loading") return <p className="muted card-body">Loading members…</p>;
  if (state.kind === "ok") {
    if (state.data.length === 0) {
      return <p className="muted card-body">Nobody has joined this course yet.</p>;
    }
    return (
      <MembersList
        members={state.data}
        grants={grants}
        teams={teams}
        grantsReady={grantsReady}
        grantBusyKey={grantBusyKey}
        actions={actions}
        canManage={canManage}
        onToggleGrant={onToggleGrant}
      />
    );
  }
  return <MembersError state={state} onRetry={onRetry} />;
}

function MembersList({
  members,
  grants,
  teams,
  grantsReady,
  grantBusyKey,
  actions,
  canManage,
  onToggleGrant,
}: {
  members: CourseMember[];
  grants: TeamAccessGrant[];
  teams: TeamSummary[];
  grantsReady: boolean;
  grantBusyKey: string | null;
  actions: MemberActions;
  canManage: boolean;
  onToggleGrant: (member: CourseMember, team: TeamSummary, isCurrentlyGranted: boolean) => void;
}) {
  return (
    <ul className="rows">
      {members.map((member) => (
        <MemberRow
          key={member.user_id}
          member={member}
          grants={grants}
          teams={teams}
          grantsReady={grantsReady}
          grantBusyKey={grantBusyKey}
          actions={actions}
          canManage={canManage}
          onToggleGrant={onToggleGrant}
        />
      ))}
    </ul>
  );
}

function MembersError({
  state,
  onRetry,
}: {
  state: Exclude<Loaded<CourseMember[]>, { kind: "ok" | "loading" }>;
  onRetry: () => void;
}) {
  return (
    <div className="card-body">
      <LoadError
        state={state}
        messages={{ forbidden: "Only course staff can see the members." }}
        onRetry={onRetry}
      />
    </div>
  );
}

function MemberRow({
  member,
  grants,
  teams,
  grantsReady,
  grantBusyKey,
  actions,
  canManage,
  onToggleGrant,
}: {
  member: CourseMember;
  grants: TeamAccessGrant[];
  teams: TeamSummary[];
  grantsReady: boolean;
  grantBusyKey: string | null;
  actions: MemberActions;
  canManage: boolean;
  onToggleGrant: (member: CourseMember, team: TeamSummary, isCurrentlyGranted: boolean) => void;
}) {
  const isStudent = member.role === "student";
  const confirming =
    actions.confirming !== null && actions.confirming.member.user_id === member.user_id
      ? actions.confirming
      : null;
  const confirmCopy = confirming !== null ? MEMBER_ACTIONS[confirming.action] : null;
  return (
    <li className="item">
      <MemberIdentity
        member={member}
        grants={grants}
        teams={teams}
        grantsReady={grantsReady}
        grantBusyKey={grantBusyKey}
        lockedByConfirm={actions.locked}
        canManage={canManage}
        actions={actions}
        onToggleGrant={onToggleGrant}
      />
      <div className="member-actions">
        <span className="pill tone-neutral">{ROLE_LABELS[member.role]}</span>
        {canManage && isStudent && <MemberActionButtons member={member} actions={actions} />}
      </div>
      {confirmCopy !== null && confirming !== null && (
        <RowConfirm copy={confirmCopy} member={member} actions={actions} />
      )}
    </li>
  );
}

function RowConfirm({
  copy,
  member,
  actions,
}: {
  copy: (typeof MEMBER_ACTIONS)[MemberAction];
  member: CourseMember;
  actions: MemberActions;
}) {
  return (
    <div className="row-confirm">
      <MemberConfirm
        busy={actions.confirmBusy}
        keepRef={actions.keepButton}
        prompt={copy.prompt(member.github_login)}
        groupLabel={copy.groupLabel(member.github_login)}
        confirmLabel={copy.confirmLabel}
        busyLabel={copy.busyLabel}
        keepLabel="Keep as student"
        onConfirm={() => {
          void actions.confirm();
        }}
        onCancel={actions.cancel}
      />
    </div>
  );
}

function MemberIdentity({
  member,
  grants,
  teams,
  grantsReady,
  grantBusyKey,
  lockedByConfirm,
  canManage,
  actions,
  onToggleGrant,
}: {
  member: CourseMember;
  grants: TeamAccessGrant[];
  teams: TeamSummary[];
  grantsReady: boolean;
  grantBusyKey: string | null;
  lockedByConfirm: boolean;
  canManage: boolean;
  actions: MemberActions;
  onToggleGrant: (member: CourseMember, team: TeamSummary, isCurrentlyGranted: boolean) => void;
}) {
  return (
    <div>
      <h3
        tabIndex={-1}
        ref={(element) => {
          if (element) actions.rowHeadings.current.set(member.user_id, element);
          else actions.rowHeadings.current.delete(member.user_id);
        }}
      >
        {member.github_login}
      </h3>
      {member.display_name !== null && <p className="muted">{member.display_name}</p>}
      {canManage && member.role === "stakeholder" && (
        <TeamAccess
          member={member}
          grants={grants}
          teams={teams}
          grantBusyKey={grantBusyKey}
          grantsReady={grantsReady}
          lockedByConfirm={lockedByConfirm}
          onToggleGrant={onToggleGrant}
        />
      )}
    </div>
  );
}

function MemberActionButtons({
  member,
  actions,
}: {
  member: CourseMember;
  actions: MemberActions;
}) {
  const busyAction =
    actions.acting !== null && actions.acting.member.user_id === member.user_id
      ? actions.acting.action
      : null;
  return (
    <>
      {(["promote", "stakeholder"] as const).map((action: MemberAction) => (
        <button
          key={action}
          className="button small"
          disabled={actions.acting !== null || actions.locked}
          aria-label={MEMBER_ACTIONS[action].ariaLabel(member.github_login)}
          ref={(element) => {
            const key = `${action}:${member.user_id}`;
            if (element) actions.actionButtons.current.set(key, element);
            else actions.actionButtons.current.delete(key);
          }}
          onClick={() => {
            actions.ask(member, action);
          }}
        >
          {MEMBER_ACTIONS[action].buttonLabel(busyAction === action)}
        </button>
      ))}
    </>
  );
}
