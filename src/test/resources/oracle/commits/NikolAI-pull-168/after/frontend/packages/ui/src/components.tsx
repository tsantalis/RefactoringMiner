// AI contribution: 50%
/** The building blocks every NikolAI front end draws with. */

import type { ReactNode } from "react";
import { LOGO_GROUND, LOGO_STROKES, LOGO_VIEWBOX } from "./logo";

export type PillTone = "success" | "attention" | "danger" | "info" | "neutral";

export function Pill({ tone, children }: { tone: PillTone; children: ReactNode }) {
  return <span className={`pill tone-${tone}`}>{children}</span>;
}

/** The NikolAI mark. It follows the text color, so it fits light and dark themes. */
export function Logo({ size = 16 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox={LOGO_VIEWBOX} aria-hidden="true">
      <rect {...LOGO_GROUND} fill="var(--mark)" />
      {LOGO_STROKES.map((stroke) =>
        stroke.kind === "rect" ? (
          <rect
            key={stroke.x}
            x={stroke.x}
            y={stroke.y}
            width={stroke.width}
            height={stroke.height}
            rx={stroke.rx}
            fill="var(--on-mark)"
          />
        ) : (
          <path key="diagonal" d={stroke.d} fill="var(--on-mark)" />
        ),
      )}
    </svg>
  );
}

export function Card({
  title,
  actions,
  footer,
  children,
}: {
  title: ReactNode;
  actions?: ReactNode | undefined;
  footer?: ReactNode | undefined;
  children: ReactNode;
}) {
  return (
    <section className="card">
      <div className="card-head">
        {title}
        {actions !== undefined && (
          <>
            <span className="spacer" />
            {actions}
          </>
        )}
      </div>
      {children}
      {footer !== undefined && <div className="card-foot">{footer}</div>}
    </section>
  );
}

/**
 * A page's whole state: the mark, a title, one line of detail and an optional action. Loading is
 * announced politely (`status`); everything else is an alert, so it interrupts a screen reader.
 */
export function StateMessage({
  title,
  detail,
  action,
  live = "alert",
}: {
  title: string;
  detail?: string | undefined;
  action?: ReactNode | undefined;
  live?: "status" | "alert";
}) {
  return (
    <div className="state" role={live}>
      <Logo size={32} />
      <h2>{title}</h2>
      {detail !== undefined && detail !== "" && <p className="muted">{detail}</p>}
      {action}
    </div>
  );
}
