// AI contribution: 50%
/** The create-course form's state: what was typed, whether it is sending, and what went wrong. */

import { useState, type SyntheticEvent } from "react";
import type { CourseSummary } from "@nikolai/api-client";
import { NAME_MAX, type DashboardClient } from "./api";
import { useAction } from "./useAction";

export const COURSE_NAME_MAX = NAME_MAX;

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
  const { busy, error, setError, run } = useAction<CourseSummary>(
    {
      forbidden: "Only instructors can create courses.",
      conflict: "That course already exists.",
    },
    (created) => {
      setName("");
      onCreated(created);
    },
    onSignedOut,
  );

  async function submit(event: SyntheticEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    const trimmed = name.trim();
    if (trimmed === "") {
      setError("Enter a course name.");
      return;
    }
    await run(() => client.createCourse(trimmed));
  }

  return { name, setName, error, busy, submit };
}
