// AI contribution: 50%
/** The create-course form's state: what was typed, whether it is sending, and what went wrong. */

import { useState, type SyntheticEvent } from "react";
import type { CourseSummary } from "@nikolai/api-client";
import type { ApiResult, DashboardClient } from "./api";

/** Same limit as the backend's `Name` in `models/courses.py`; keep the two in step. */
export const COURSE_NAME_MAX = 200;

export function useCreateCourse(
  client: DashboardClient,
  onCreated: (course: CourseSummary) => void,
  onSignedOut: () => void,
): {
  name: string;
  setName: (name: string) => void;
  error: string | null;
  busy: boolean;
  submit: (event: SyntheticEvent<HTMLFormElement>) => Promise<void>;
} {
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(event: SyntheticEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    const trimmed = name.trim();
    if (trimmed === "") {
      setError("Enter a course name.");
      return;
    }
    setBusy(true);
    setError(null);
    const result = await client.createCourse(trimmed);
    setBusy(false);
    if (result.kind === "ok") {
      setName("");
      onCreated(result.data);
      return;
    }
    if (result.kind === "signed-out") {
      onSignedOut();
      return;
    }
    setError(createErrorMessage(result));
  }

  return { name, setName, error, busy, submit };
}

function createErrorMessage(
  result: Exclude<ApiResult<CourseSummary>, { kind: "ok" } | { kind: "signed-out" }>,
): string {
  switch (result.kind) {
    case "forbidden":
      return "Only instructors can create courses.";
    case "unreachable":
      return result.reason;
    case "not-found":
      return "That course no longer exists.";
    case "not-registered":
      return "NikolAI isn't set up for that course.";
    case "conflict":
      return "That course already exists.";
  }
}
