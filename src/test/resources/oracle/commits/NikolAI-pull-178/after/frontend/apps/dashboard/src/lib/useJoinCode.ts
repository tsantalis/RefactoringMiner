// AI contribution: 50%
/** The join-code card's state: copy, a confirmation flow for rotate, and what to say. */

import { useEffect, useRef, useState, type RefObject } from "react";
import type { CourseSummary, JoinCode } from "@nikolai/api-client";
import type { DashboardClient } from "./api";
import { useAction } from "./useAction";

/** Clipboard access can vanish in embedded browsers; degrade loud but clear. */
export function useJoinCode(
  client: DashboardClient,
  course: CourseSummary,
  onCourseChange: (course: CourseSummary) => void,
  onSignedOut: () => void,
): {
  code: string | null;
  codeRef: RefObject<HTMLElement | null>;
  copied: boolean;
  copy: () => Promise<void>;
  confirming: boolean;
  openConfirm: () => void;
  cancelConfirm: () => void;
  rotate: () => void;
  busy: boolean;
  error: string | null;
  announcement: string | null;
} {
  const [confirming, setConfirming] = useState(false);
  const [copied, setCopied] = useState(false);
  const [announcement, setAnnouncement] = useState<string | null>(null);
  const codeRef = useRef<HTMLElement | null>(null);
  const code = course.join_code;

  const { busy, error, setError, run } = useAction<JoinCode>(
    {
      forbidden: "Only instructors can rotate the join code.",
    },
    (result) => {
      onCourseChange({ ...course, join_code: result.join_code });
      setConfirming(false);
      setAnnouncement(`New join code: ${result.join_code}. The old code no longer works.`);
    },
    onSignedOut,
  );

  useEffect(() => {
    if (!copied) return undefined;
    const timer = setTimeout(() => {
      setCopied(false);
    }, 2000);
    return () => {
      clearTimeout(timer);
    };
  }, [copied]);

  async function copy(): Promise<void> {
    if (code === null) return;
    setError(null);
    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
      setAnnouncement("Join code copied.");
    } catch (cause) {
      console.warn("NikolAI: the join code could not be copied.", cause);
      selectCode(codeRef.current);
      setError("Copying isn't allowed here. The code is selected: press Ctrl+C (Cmd+C on a Mac).");
    }
  }

  return {
    code,
    codeRef,
    copied,
    copy,
    confirming,
    openConfirm: () => {
      setError(null);
      setAnnouncement(null);
      setConfirming(true);
    },
    cancelConfirm: () => {
      setConfirming(false);
    },
    rotate: () => {
      void run(() => client.rotateJoinCode(course.id));
    },
    busy,
    error,
    announcement,
  };
}

function selectCode(element: HTMLElement | null): void {
  if (element === null) return;
  const range = document.createRange();
  range.selectNodeContents(element);
  const selection = window.getSelection();
  selection?.removeAllRanges();
  selection?.addRange(range);
}
