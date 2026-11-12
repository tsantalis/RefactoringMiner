// AI contribution: 50%
/** The course's teams: the list everyone staff sees, and the instructor's create form. */

import { useState, type SyntheticEvent } from "react";
import type { CourseSummary, TeamSummary } from "@nikolai/api-client";
import { Card } from "@nikolai/ui";
import { NAME_MAX, type DashboardClient } from "../lib/api";
import { useCreateTeam } from "../lib/useCreateTeam";

export function TeamsSection({
  client,
  course,
  canCreate,
  onCourseChange,
  onSignedOut,
}: {
  client: DashboardClient;
  course: CourseSummary;
  canCreate: boolean;
  onCourseChange: (course: CourseSummary) => void;
  onSignedOut: () => void;
}) {
  const [announcement, setAnnouncement] = useState<string | null>(null);

  return (
    <Card title={<h2>Teams</h2>}>
      {course.teams.length === 0 ? (
        <p className="muted card-body">No teams yet.</p>
      ) : (
        <ul className="rows">
          {course.teams.map((team) => (
            <li className="item" key={team.id}>
              <h3>{team.name}</h3>
            </li>
          ))}
        </ul>
      )}
      {canCreate && (
        <div className="card-body">
          <TeamForm
            client={client}
            courseId={course.id}
            onCreated={(team) => {
              onCourseChange(withTeam(course, team));
              setAnnouncement(`Team ${team.name} created.`);
            }}
            onSignedOut={onSignedOut}
          />
        </div>
      )}
      {announcement !== null && (
        <p className="muted card-body" role="status">
          {announcement}
        </p>
      )}
    </Card>
  );
}

/** The list shows teams in the server's lower(name) order; insert the new one the same way.
 *  The comparison is approximate (JS, not Postgres' collation); the page refetches the course,
 *  which puts exact server order in place on the next mount. */
function withTeam(course: CourseSummary, team: TeamSummary): CourseSummary {
  const teams = [...course.teams, team].sort((a, b) =>
    a.name.toLowerCase().localeCompare(b.name.toLowerCase()),
  );
  return { ...course, teams };
}

function TeamForm({
  client,
  onCreated,
  onSignedOut,
  courseId,
}: {
  client: DashboardClient;
  onCreated: (team: TeamSummary) => void;
  onSignedOut: () => void;
  courseId: string;
}) {
  const [name, setName] = useState("");
  const { busy, error, setError, create } = useCreateTeam(
    client,
    courseId,
    (team) => {
      setName("");
      onCreated(team);
    },
    onSignedOut,
  );

  async function submit(event: SyntheticEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    const trimmed = name.trim();
    if (trimmed === "") {
      setError("Enter a team name.");
      return;
    }
    await create(trimmed);
  }

  return (
    <>
      <form className="create-form" onSubmit={(event) => void submit(event)}>
        <label htmlFor="team-name">New team name</label>
        <input
          id="team-name"
          type="text"
          value={name}
          onChange={(event) => {
            setName(event.target.value);
          }}
          aria-invalid={error !== null}
          aria-describedby={error === null ? undefined : "team-name-error"}
          maxLength={NAME_MAX}
          required
        />
        <button className="button primary" type="submit" disabled={busy}>
          {busy ? "Creating…" : "Create team"}
        </button>
      </form>
      {error !== null && (
        <p className="error" id="team-name-error" role="alert">
          {error}
        </p>
      )}
    </>
  );
}
