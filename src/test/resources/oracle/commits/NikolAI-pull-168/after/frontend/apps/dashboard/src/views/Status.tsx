// AI contribution: 50%
/** A full-page state: a title, an optional detail and an optional action. */

import type { ReactNode } from "react";
import { StateMessage } from "@nikolai/ui";

export function Status({
  title,
  detail,
  action,
  live = "alert",
}: {
  title: string;
  detail?: string;
  action?: ReactNode;
  live?: "status" | "alert";
}) {
  return (
    <main className="page">
      <StateMessage title={title} detail={detail} action={action} live={live} />
    </main>
  );
}
