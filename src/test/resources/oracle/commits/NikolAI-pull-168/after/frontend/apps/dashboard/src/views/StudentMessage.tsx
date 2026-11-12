// AI contribution: 50%
/** What a student sees: the dashboard is for staff, and their work happens in the extension. */

import { StateMessage } from "@nikolai/ui";

export function StudentMessage() {
  return (
    <StateMessage
      live="status"
      title="This dashboard is for course staff"
      detail="Use the NikolAI extension on your team's repository on GitHub. Your instructor can tell you how to install it."
    />
  );
}
