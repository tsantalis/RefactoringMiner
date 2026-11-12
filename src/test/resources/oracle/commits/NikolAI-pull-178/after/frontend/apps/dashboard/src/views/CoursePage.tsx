// AI contribution: 50%
/** One course: its join code, its teams and its members. TAs see it read-only. */

import { useCallback, useEffect, useRef } from "react";
import { ROLE_LABELS, type CourseSummary } from "@nikolai/api-client";
import { Pill } from "@nikolai/ui";
import type { DashboardClient } from "../lib/api";
import { useLoad } from "../lib/useLoad";
import { JoinCodeSection } from "./JoinCodeSection";
import { MembersSection } from "./MembersSection";
import { TeamsSection } from "./TeamsSection";

export function CoursePage({
  client,
  course,
  onBack,
  onCourseChange,
  onSignedOut,
}: {
  client: DashboardClient;
  course: CourseSummary;
  onBack: () => void;
  onCourseChange: (course: CourseSummary) => void;
  onSignedOut: () => void;
}) {
  const load = useCallback(
    (signal: AbortSignal) => client.getCourse(course.id, signal),
    [client, course.id],
  );
  const { state, reload, setLoaded } = useLoad(load, onSignedOut);
  const fresh = state.kind === "ok" ? state.data : null;
  const headingRef = useRef<HTMLHeadingElement>(null);
  const syncedRef = useRef(false);

  useEffect(() => {
    if (syncedRef.current || fresh === null) return;
    syncedRef.current = true;
    onCourseChange(fresh);
    // Focus the heading only on the first load; the page is a new context each time it opens.
    headingRef.current?.focus();
  }, [fresh, onCourseChange]);

  if (state.kind === "loading" && fresh === null) {
    return (
      <div className="course-page">
        <p className="muted card-body">Loading {course.name}…</p>
      </div>
    );
  }
  if (state.kind !== "ok" && fresh === null) {
    return (
      <div className="card-body">
        <p className="error" role="alert">
          {state.kind === "unreachable" ? state.reason : "Something went wrong loading the course."}
        </p>
        <button className="button" onClick={reload}>
          Try again
        </button>
      </div>
    );
  }

  const view = fresh ?? course;
  const applyChange = (next: CourseSummary): void => {
    setLoaded({ kind: "ok", data: next });
    onCourseChange(next);
  };
  const isInstructor = view.role === "instructor";
  return (
    <div className="course-page">
      <div className="course-page-head">
        <button className="button small" onClick={onBack}>
          ← Back to your courses
        </button>
        <h2 ref={headingRef} tabIndex={-1} aria-label={`Course: ${view.name}`}>
          {view.name}
        </h2>
        <Pill tone="info">{ROLE_LABELS[view.role]}</Pill>
      </div>
      {isInstructor && (
        <JoinCodeSection
          client={client}
          course={view}
          onCourseChange={applyChange}
          onSignedOut={onSignedOut}
        />
      )}
      <TeamsSection
        client={client}
        course={view}
        canCreate={isInstructor}
        onCourseChange={applyChange}
        onSignedOut={onSignedOut}
      />
      <MembersSection
        client={client}
        course={view}
        canPromote={isInstructor}
        onSignedOut={onSignedOut}
      />
    </div>
  );
}
