// AI contribution: 50%
/** What a student sees before they're on a team: the join form for the code their instructor
 *  shared. Once on a team, the dashboard opens their team's planning page instead. */

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
        title="You're not on a team yet"
        detail="Join your course with the code your instructor shared. Once your instructor puts you on a team, your team's planning page opens here."
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
  // A course that already has them on a team opens its planning page, so only this case shows.
  const { name } = joined.course;
  const joinedLine = joined.alreadyMember ? `You're already in ${name}.` : `You joined ${name}.`;
  return (
    <p>{joinedLine} Your instructor will put you on a team, and its planning page opens here.</p>
  );
}
