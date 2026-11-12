// AI contribution: 50%
/** The backlog coverage check: which project requirements no user story covers (US-13.2). */

import { useState } from "react";
import type { ApiFailure } from "../../shared/api/http";
import type { NikolaiClient } from "../../shared/api/nikolai-client";
import type { BacklogCoverage } from "../../shared/api/views-contract";
import { shortDate } from "../../shared/format";
import type { RepoRef } from "../../shared/page";
import { StateMessage } from "../StateMessage";
import { Card, Pill, safeGitHubUrl } from "../ui";
import { useResource } from "../useResource";

type Requirement = BacklogCoverage["requirements"][number];
type Story = Requirement["stories"][number];

const SUBJECT = "this team's backlog";

function StoryLink({ story }: { story: Story }) {
  const label = `#${story.number} ${story.title}`;
  const url = safeGitHubUrl(story.url);
  return url === null ? <span>{label}</span> : <a href={url}>{label}</a>;
}

function Uncovered({ requirements }: { requirements: Requirement[] }) {
  if (requirements.length === 0) {
    return <p className="muted">Every requirement has at least one story.</p>;
  }
  return (
    <section className="stack" aria-label="No story covers these">
      <div className="inline">
        <h2 className="strong">No story covers these</h2>
        <Pill tone="attention">{requirements.length}</Pill>
      </div>
      <ul>
        {requirements.map((item) => (
          <li key={item.requirement}>{item.requirement}</li>
        ))}
      </ul>
    </section>
  );
}

function Covered({ requirements }: { requirements: Requirement[] }) {
  if (requirements.length === 0) return null;
  return (
    <section className="stack" aria-label="Covered by your stories">
      <h2 className="strong">Covered by your stories</h2>
      {requirements.map((item) => (
        <div key={item.requirement} className="stack" style={{ gap: 4 }}>
          <span>{item.requirement}</span>
          <ul className="small">
            {item.stories.map((story) => (
              <li key={story.number}>
                <StoryLink story={story} />
              </li>
            ))}
          </ul>
        </div>
      ))}
    </section>
  );
}

function Result({ coverage }: { coverage: BacklogCoverage }) {
  switch (coverage.status) {
    case "not_run":
      return <p className="muted">Check your user stories against the project requirements.</p>;
    case "no_requirements":
      return (
        <p className="muted">
          Your course has no project requirements yet, so there is nothing to check your backlog
          against. This is not a pass.
        </p>
      );
    case "failed":
      return (
        <div role="alert">
          <p className="strong">NikolAI couldn't check this backlog</p>
          <p className="muted">{coverage.reason}</p>
        </div>
      );
    case "completed":
      return (
        <>
          <Uncovered requirements={coverage.requirements.filter((r) => r.stories.length === 0)} />
          <Covered requirements={coverage.requirements.filter((r) => r.stories.length > 0)} />
        </>
      );
  }
}

export function BacklogCoverageView({ repo, client }: { repo: RepoRef; client: NikolaiClient }) {
  const loaded = useResource(
    (signal) => client.getBacklogCoverage(repo, signal),
    `${repo.owner}/${repo.repo}`,
  );
  const [latest, setLatest] = useState<BacklogCoverage | null>(null);
  const [running, setRunning] = useState(false);
  const [refused, setRefused] = useState<ApiFailure | null>(null);

  if (loaded.kind !== "ok") return <StateMessage state={loaded} subject={SUBJECT} />;
  const coverage = latest ?? loaded.data;

  const run = async (): Promise<void> => {
    setRunning(true);
    setRefused(null);
    const result = await client.runBacklogCoverage(repo);
    setRunning(false);
    if (result.kind === "ok") setLatest(result.data);
    else setRefused(result);
  };

  return (
    <Card
      title="Backlog coverage"
      actions={
        <button
          type="button"
          className="button primary"
          disabled={running}
          onClick={() => void run()}
        >
          {running ? "Checking…" : "Run the check"}
        </button>
      }
      footer={
        coverage.checked_at === null
          ? undefined
          : `Last checked ${shortDate(coverage.checked_at.slice(0, 10))}`
      }
    >
      <div className="card-body">
        {refused !== null && <StateMessage state={refused} subject={SUBJECT} />}
        <Result coverage={coverage} />
      </div>
    </Card>
  );
}
