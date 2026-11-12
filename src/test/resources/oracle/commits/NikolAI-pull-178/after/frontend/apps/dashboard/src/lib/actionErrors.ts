// AI contribution: 50%
/** Turns a failed write into one clean sentence, never a stack trace. */

import type { ApiResult } from "./api";

export interface ActionMessages {
  /** The caller lacks the role for this action; the server said 403. */
  forbidden: string;
  /** The target no longer exists; the server said 404. */
  notFound?: string;
  /** The server said 409; use the resource's own rule when there is one. */
  conflict?: string;
}

export function actionErrorMessage(
  result: Exclude<ApiResult<unknown>, { kind: "ok" } | { kind: "signed-out" }>,
  messages: ActionMessages,
): string {
  switch (result.kind) {
    case "forbidden":
      return messages.forbidden;
    case "not-found":
      return messages.notFound ?? "That course no longer exists.";
    case "not-registered":
      return "NikolAI isn't set up for that yet.";
    case "conflict":
      return messages.conflict ?? "That conflicts with something that already exists.";
    case "unreachable":
      return result.reason;
  }
}
