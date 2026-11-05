// AI contribution: 50%
/** The PR tab's other views: refactorings (US-8), feedback on your review, and staff marking. */

import type { PullInsights, PullRefactorings } from "../../shared/api/views-contract";
import type { PullRequestRef } from "../../shared/github-url";
import { severityLabel, severityTone } from "../../shared/status";
import { Card, Counter, Pill, type PillTone } from "../ui";
import { LineLink } from "./LineLink";

type Refactoring = PullRefactorings["items"][number];

const CHANGES: Record<Refactoring["change"], [string, PillTone]> = {
  new: ["new", "success"],
  undone: ["undone", "danger"],
  unchanged: ["unchanged", "neutral"],
};

/** Refactorings grouped by type, in the order RefactoringMiner first reported each type. */
export function groupByType(items: Refactoring[]): [string, Refactoring[]][] {
  const groups = new Map<string, Refactoring[]>();
  for (const item of items) groups.set(item.type, [...(groups.get(item.type) ?? []), item]);
  return [...groups];
}

function RefactoringGroup({
  pull,
  type,
  items,
}: {
  pull: PullRequestRef;
  type: string;
  items: Refactoring[];
}) {
  return (
    <div className="item">
      <div className="stack grow">
        <div className="inline">
          <strong>{type}</strong>
          <Counter>{items.length}</Counter>
        </div>
        {items.map((item) => {
          const [label, tone] = CHANGES[item.change];
          return (
            <div key={`${item.description}-${item.file_path}`} className="inline">
              <span>{item.description}</span>
              <Pill tone={tone}>{label}</Pill>
              {item.file_path !== null && (
                <LineLink
                  pull={pull}
                  path={item.file_path}
                  start={item.start_line}
                  end={null}
                  side={item.side}
                />
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

export function Refactorings({
  pull,
  refactorings,
}: {
  pull: PullRequestRef;
  refactorings: PullRefactorings;
}) {
  if (refactorings.commit === null) {
    return (
      <Card title="Refactoring report" footer="Detected by RefactoringMiner on every push.">
        <div className="card-body muted">
          RefactoringMiner hasn't analysed this pull request yet. Its report appears here after the
          next push.
        </div>
      </Card>
    );
  }
  const count = (change: Refactoring["change"]): number =>
    refactorings.items.filter((item) => item.change === change).length;
  const title = (
    <>
      <span>Refactoring report</span>
      <Pill tone="success">{count("new")} new</Pill>
      <Pill tone="danger">{count("undone")} undone</Pill>
      <Pill tone="neutral">{count("unchanged")} unchanged</Pill>
    </>
  );
  const since =
    refactorings.since_commit === null ? undefined : (
      <span className="muted small">since {refactorings.since_commit}</span>
    );
  return (
    <Card title={title} actions={since} footer="Detected by RefactoringMiner on every push.">
      {refactorings.items.length === 0 && (
        <div className="card-body muted">No refactorings in this pull request.</div>
      )}
      {groupByType(refactorings.items).map(([type, items]) => (
        <RefactoringGroup key={type} pull={pull} type={type} items={items} />
      ))}
    </Card>
  );
}

export function YourReview({
  pull,
  review,
}: {
  pull: PullRequestRef;
  review: PullInsights["your_review"];
}) {
  if (review === null) {
    return (
      <Card title="Your review">
        <div className="card-body muted">You haven't reviewed this pull request.</div>
      </Card>
    );
  }
  return (
    <Card
      title={
        <>
          <span>Your review on #{pull.number}</span>
          <Pill tone={review.substantive ? "success" : "attention"}>
            {review.substantive ? "Substantive" : "Not substantive yet"}
          </Pill>
        </>
      }
      actions={<span className="muted small">Only you and course staff see this.</span>}
    >
      <div className="card-body">
        <span>You left {review.technical_comments} technical comments.</span>
        <strong>Issues your review missed</strong>
        {review.missed.length === 0 && <span className="muted">None. Nice work.</span>}
        {review.missed.map((missed) => (
          <div key={missed.name} className="inline">
            <Pill tone={severityTone(missed.severity)}>{severityLabel(missed.severity)}</Pill>
            <span>{missed.name}</span>
            {missed.file_path !== null && <span className="mono muted">{missed.file_path}</span>}
          </div>
        ))}
      </div>
    </Card>
  );
}

export function Marking({ marking }: { marking: NonNullable<PullInsights["marking"]> }) {
  const met = marking.checks.filter((check) => check.met).length;
  return (
    <>
      <Card
        title={
          <>
            <span>Course rules for this PR</span>
            <Pill tone={met === marking.checks.length ? "success" : "attention"}>
              {met} of {marking.checks.length} met
            </Pill>
          </>
        }
      >
        {marking.checks.map((check) => (
          <div key={check.rule} className="item">
            <Pill tone={check.met ? "success" : "danger"}>{check.met ? "Met" : "Not met"}</Pill>
            <div className="stack" style={{ gap: 2 }}>
              <span className="strong">{check.rule}</span>
              <span className="muted small">{check.detail}</span>
            </div>
          </div>
        ))}
      </Card>
      <Card title="Who did what">
        <div className="card-body small">
          {marking.contributors.map((person) => (
            <div key={person.login} className="inline">
              <span>{person.login}</span>
              <span className="spacer" />
              <span className="muted">
                {person.commits} commits · +{person.additions} −{person.deletions}
              </span>
            </div>
          ))}
          <span className="muted">
            AI contribution declared: {marking.ai_contribution ?? "none"} ·{" "}
            {marking.confirmed_refactorings} refactorings confirmed by the author
          </span>
        </div>
      </Card>
    </>
  );
}
