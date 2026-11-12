// AI contribution: 50%
/** The course's join code: copy it, or rotate it after a confirmation. */

import { useEffect, useRef } from "react";
import type { CourseSummary } from "@nikolai/api-client";
import { Card } from "@nikolai/ui";
import type { DashboardClient } from "../lib/api";
import { useJoinCode } from "../lib/useJoinCode";

export function JoinCodeSection({
  client,
  course,
  onCourseChange,
  onSignedOut,
}: {
  client: DashboardClient;
  course: CourseSummary;
  onCourseChange: (course: CourseSummary) => void;
  onSignedOut: () => void;
}) {
  const {
    code,
    codeRef,
    copied,
    copy,
    confirming,
    openConfirm,
    cancelConfirm,
    rotate,
    busy,
    error,
    announcement,
  } = useJoinCode(client, course, onCourseChange, onSignedOut);
  const keepRef = useRef<HTMLButtonElement>(null);
  const rotateRef = useRef<HTMLButtonElement>(null);
  const mounted = useRef(false);

  useEffect(() => {
    if (!mounted.current) {
      mounted.current = true;
      return;
    }
    if (confirming) {
      keepRef.current?.focus();
    } else {
      rotateRef.current?.focus();
    }
  }, [confirming]);

  if (code === null) return null;

  return (
    <Card title={<h2>Join code</h2>}>
      <div className="card-body">
        <p className="join-code-row">
          <code className="join-code" aria-label="Join code" ref={codeRef}>
            {code}
          </code>
          <button
            className="button"
            onClick={() => {
              void copy();
            }}
          >
            {copied ? "Copied!" : "Copy"}
          </button>
        </p>
        <p className="muted">Students join the course with this code.</p>
        {confirming ? (
          <div
            className="confirm-box"
            role="group"
            aria-label="Rotate the join code"
            onKeyDown={(event) => {
              if (event.key === "Escape") cancelConfirm();
            }}
          >
            <p>
              Rotate the join code? The old code stops working at once; members who already joined
              stay in the course.
            </p>
            <div className="confirm-actions">
              <button className="button danger" disabled={busy} onClick={rotate} ref={rotateRef}>
                {busy ? "Rotating…" : "Rotate code"}
              </button>
              <button className="button" disabled={busy} onClick={cancelConfirm} ref={keepRef}>
                Keep current code
              </button>
            </div>
          </div>
        ) : (
          <button className="button" onClick={openConfirm} ref={rotateRef}>
            Rotate code…
          </button>
        )}
        {error !== null && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
        {announcement !== null && (
          <p className="muted" role="status">
            {announcement}
          </p>
        )}
      </div>
    </Card>
  );
}
