// AI contribution: 50%
/** What a student sees: the dashboard is for staff, their work happens in the extension, and
 *  the join form for the code their instructor shared. */

import type { CourseSummary } from "@nikolai/api-client";
import { Card, StateMessage } from "@nikolai/ui";
import { JOIN_CODE_MAX, type DashboardClient } from "../lib/api";
import { useJoinCourse, type Joined } from "../lib/useJoinCourse";

/** What the join form needs: the client, the student's courses before joining, and the page's hooks. */
interface JoinProps {
  client: DashboardClient;
  courses: readonly CourseSummary[];
  onJoined: (joined: Joined) => void;
  onSignedOut: () => void;
}

export function StudentMessage(props: JoinProps) {
  return (
    <div className="student-page">
      <StateMessage
        live="status"
        title="This dashboard is for course staff"
        detail="Use the NikolAI extension on your team's repository on GitHub. Your instructor can tell you how to install it."
      />
      <JoinCourseForm {...props} />
    </div>
  );
}

function JoinCourseForm({ client, courses, onJoined, onSignedOut }: JoinProps) {
  const { code, setCode, joined, error, busy, submit } = useJoinCourse(
    client,
    courses,
    onJoined,
    onSignedOut,
  );

  return (
    <Card title={<h2>Join a course</h2>}>
      <div className="card-body">
        <form className="create-form" onSubmit={(event) => void submit(event)}>
          <label htmlFor="join-code">Join code</label>
          <input
            id="join-code"
            className="mono"
            type="text"
            value={code}
            onChange={(event) => {
              setCode(event.target.value);
            }}
            aria-invalid={error !== null}
            aria-describedby={error === null ? "join-code-help" : "join-code-error"}
            maxLength={JOIN_CODE_MAX}
            autoComplete="off"
            autoCapitalize="characters"
            spellCheck={false}
          />
          <button className="button primary" type="submit" disabled={busy}>
            {busy ? "Joining…" : "Join course"}
          </button>
        </form>
        <p className="muted" id="join-code-help">
          Paste the code your instructor shared.
        </p>
        {error !== null && (
          <p className="error" id="join-code-error" role="alert">
            {error}
          </p>
        )}
        <div role="status">{joined !== null && <JoinedMessage joined={joined} />}</div>
      </div>
    </Card>
  );
}

function JoinedMessage({ joined }: { joined: Joined }) {
  const { name, teams } = joined.course;
  const joinedLine = joined.alreadyMember ? `You're already in ${name}.` : `You joined ${name}.`;
  const team = teams[0];
  const nextStep =
    team === undefined
      ? "Your instructor will put you on a team. Then open your team's repository on GitHub and use the NikolAI extension there."
      : `You're on team ${team.name}. Open its repository on GitHub and use the NikolAI extension there.`;
  return (
    <p>
      {joinedLine} {nextStep}
    </p>
  );
}
