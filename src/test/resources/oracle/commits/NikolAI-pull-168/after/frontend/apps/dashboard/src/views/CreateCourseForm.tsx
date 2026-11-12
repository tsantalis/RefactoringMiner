// AI contribution: 50%
/** The instructor's form for opening a new course. */

import type { CourseSummary } from "@nikolai/api-client";
import { Card } from "@nikolai/ui";
import type { DashboardClient } from "../lib/api";
import { COURSE_NAME_MAX, useCreateCourse } from "../lib/useCreateCourse";

export function CreateCourseForm({
  client,
  onCreated,
  onSignedOut,
}: {
  client: DashboardClient;
  onCreated: (course: CourseSummary) => void;
  onSignedOut: () => void;
}) {
  const { name, setName, error, busy, submit } = useCreateCourse(client, onCreated, onSignedOut);

  return (
    <Card title={<h2>Create a course</h2>}>
      <div className="card-body">
        <form className="create-form" onSubmit={(event) => void submit(event)}>
          <label htmlFor="course-name">Course name</label>
          <input
            id="course-name"
            type="text"
            value={name}
            onChange={(event) => {
              setName(event.target.value);
            }}
            aria-invalid={error !== null}
            aria-describedby={error === null ? undefined : "course-name-error"}
            maxLength={COURSE_NAME_MAX}
            required
          />
          <button className="button primary" type="submit" disabled={busy}>
            {busy ? "Creating…" : "Create course"}
          </button>
        </form>
        {error !== null && (
          <p className="error" id="course-name-error" role="alert">
            {error}
          </p>
        )}
      </div>
    </Card>
  );
}
