// AI contribution: 50%
/** Building blocks shared by every NikolAI view. */

import type { ReactNode } from "react";
import type { Tone } from "../shared/status";

export { Card, Logo, Pill, type PillTone } from "@nikolai/ui";

export function Counter({ children }: { children: ReactNode }) {
  return <span className="counter">{children}</span>;
}

/** A progress bar. `label` is read out by screen readers in place of the bar. */
export function Bar({
  value,
  max,
  tone = "success",
  label,
}: {
  value: number;
  max: number;
  tone?: Tone | "info";
  label: string;
}) {
  const percent = max <= 0 ? 0 : Math.min(100, Math.round((value / max) * 100));
  return (
    <div
      className={`bar ${tone}`}
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={max}
      aria-valuenow={value}
    >
      <span style={{ width: `${percent}%` }} />
    </div>
  );
}

export function Tile({
  label,
  value,
  note,
}: {
  label: string;
  value: ReactNode;
  note?: ReactNode;
}) {
  return (
    <div className="tile">
      <span className="muted small strong">{label}</span>
      <span className="tile-value">{value}</span>
      {note !== undefined && <span className="muted small">{note}</span>}
    </div>
  );
}

/** Shown on every view while the extension reads made-up data. */
export function SampleNote({ isSample }: { isSample: boolean }) {
  if (!isSample) return null;
  return (
    <p className="sample-note">
      Sample data. Set your NikolAI server in the extension's options to see your team's.
    </p>
  );
}

/** Only links to github.com leave the view; anything else from the server isn't trusted. */
export function safeGitHubUrl(url: string): string | null {
  try {
    const parsed = new URL(url);
    return parsed.protocol === "https:" && parsed.hostname === "github.com"
      ? parsed.toString()
      : null;
  } catch {
    return null;
  }
}
