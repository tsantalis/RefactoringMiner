// AI contribution: 50%
/** The NikolAI tab on a repository: the team's dashboard, in its student or staff layout. */

import type { NikolaiClient } from "../../shared/api/nikolai-client";
import type { TeamOverview } from "../../shared/api/views-contract";
import { daysUntil, shortDate } from "../../shared/format";
import { routeHash, type RepoRef } from "../../shared/page";
import { isStaffRole, ROLE_LABELS } from "../../shared/api/views-contract";
import { StaffBar } from "../StaffBar";
import { StateMessage } from "../StateMessage";
import { Pill, SampleNote } from "../ui";
import { useResource } from "../useResource";
import { BacklogCoverageView } from "./BacklogCoverageView";
import { StaffOverview } from "./StaffOverview";
import { StudentOverview } from "./StudentOverview";

/** The `#nikolai/...` section of the backlog coverage check. */
const BACKLOG_COVERAGE = "backlog-coverage";

function RepoMenu({
  repo,
  team,
  section,
}: {
  repo: RepoRef;
  team: TeamOverview;
  section: string | null;
}) {
  const base = `https://github.com/${repo.owner}/${repo.repo}`;
  const links: [string, string][] = [
    ["Pull requests", `${base}/pulls`],
    ["Stories", `${base}/issues`],
    ["Sprint planning", `${base}/projects`],
    ["CI/CD and tests", `${base}/actions${routeHash(null)}`],
  ];
  const reach = isStaffRole(team.viewer.role)
    ? "every team in this course"
    : `${team.team.name} only`;
  return (
    <nav className="menu" aria-label="NikolAI">
      <div className="muted small">{team.team.course}</div>
      <div className="strong">{team.team.name}</div>
      <a
        className="nav-link"
        href={routeHash(null)}
        aria-current={section === BACKLOG_COVERAGE ? undefined : "page"}
      >
        Overview
      </a>
      <a
        className="nav-link"
        href={routeHash(BACKLOG_COVERAGE)}
        aria-current={section === BACKLOG_COVERAGE ? "page" : undefined}
      >
        Backlog coverage
      </a>
      {links.map(([label, href]) => (
        <a key={label} className="nav-link" href={href}>
          {label}
        </a>
      ))}
      <p className="muted small">
        Signed in as <strong>{team.viewer.login}</strong> · {ROLE_LABELS[team.viewer.role]}. You see{" "}
        {reach}.
      </p>
    </nav>
  );
}

function DueLabel({ iteration }: { iteration: TeamOverview["iteration"] }) {
  if (iteration === null) return null;
  const days = daysUntil(iteration.due_on);
  const left = days < 0 ? "past due" : days === 0 ? "due today" : `${days} days left`;
  return (
    <Pill tone={days <= 2 ? "attention" : "neutral"}>
      {iteration.name} due {shortDate(iteration.due_on)} · {left}
    </Pill>
  );
}

export function RepoView({
  repo,
  section,
  client,
}: {
  repo: RepoRef;
  section: string | null;
  client: NikolaiClient;
}) {
  const result = useResource(
    (signal) => client.getTeam(repo, signal),
    `${repo.owner}/${repo.repo}`,
  );
  if (result.kind !== "ok") return <StateMessage state={result} subject="this team's work" />;
  const team = result.data;
  const staff = isStaffRole(team.viewer.role) ? team.staff : null;
  return (
    <>
      {staff !== null && <StaffBar viewer={team.viewer} course={team.team.course} />}
      <div className="page">
        <div className="row">
          <RepoMenu repo={repo} team={team} section={section} />
          <main className="grow">
            <div className="inline">
              <h1 className="title">{team.team.name}</h1>
              <DueLabel iteration={team.iteration} />
            </div>
            {section === BACKLOG_COVERAGE ? (
              <BacklogCoverageView key={`${repo.owner}/${repo.repo}`} repo={repo} client={client} />
            ) : staff === null ? (
              <StudentOverview repo={repo} team={team} />
            ) : (
              <StaffOverview repo={repo} team={team} staff={staff} />
            )}
            <SampleNote isSample={client.isSample} />
          </main>
        </div>
      </div>
    </>
  );
}
