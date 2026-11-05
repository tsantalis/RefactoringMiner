// AI contribution: 50%
/** One way to call the NikolAI server and turn its answer into a state every view can show. */

import type { Check } from "./schema";

/** The `detail.code` of each error the server names, so a view can say what went wrong. */
export const ERROR_CODES = {
  404: ["repository_not_registered", "pull_request_not_found", "finding_not_found"],
  403: ["origin_not_allowed", "not_a_team_student"],
  409: ["finding_superseded"],
} as const;

export type NotFoundCode = (typeof ERROR_CODES)[404][number];
export type ForbiddenCode = (typeof ERROR_CODES)[403][number];
export type ConflictCode = (typeof ERROR_CODES)[409][number];

/** Every outcome a NikolAI view has a screen for. `code` is the server's `detail.code`, if any. */
export type ApiResult<T> =
  | { kind: "ok"; data: T }
  | { kind: "not-registered" }
  | { kind: "not-found"; code?: NotFoundCode }
  | { kind: "signed-out" }
  | { kind: "forbidden"; code?: ForbiddenCode }
  | { kind: "conflict"; code?: ConflictCode }
  | { kind: "unreachable"; reason: string };

export type ApiFailure = Exclude<ApiResult<never>, { kind: "ok" }>;

export type FetchLike = (input: string, init?: RequestInit) => Promise<Response>;

export const REQUEST_TIMEOUT_MS = 10_000;

export const UNREADABLE = "The NikolAI server sent a response we can't read.";

/** Headers and cookie mode for every call to the NikolAI server (ADR 0001's session cookie). */
export const SERVER_REQUEST: Pick<RequestInit, "headers" | "credentials"> = {
  headers: { Accept: "application/json", "Content-Type": "application/json" },
  credentials: "include",
};

export interface RequestOptions {
  signal?: AbortSignal | undefined;
  timeoutMs?: number;
  method?: "GET" | "POST";
  body?: unknown;
}

function requestInit(options: RequestOptions, signal: AbortSignal): RequestInit {
  const init: RequestInit = { ...SERVER_REQUEST, method: options.method ?? "GET", signal };
  if (options.body !== undefined) init.body = JSON.stringify(options.body);
  return init;
}

/** Fetch `url` and check its JSON with `check`. A 204 counts as `null`. */
export async function requestJson<T>(
  fetchImpl: FetchLike,
  url: string,
  check: Check<T>,
  options: RequestOptions = {},
): Promise<ApiResult<T>> {
  const timeout = AbortSignal.timeout(options.timeoutMs ?? REQUEST_TIMEOUT_MS);
  const signal = options.signal ? AbortSignal.any([options.signal, timeout]) : timeout;
  let response: Response;
  try {
    response = await fetchImpl(url, requestInit(options, signal));
  } catch (error) {
    // The caller cancelled (the user moved to another page): let them ignore it.
    if (options.signal?.aborted) throw error;
    return {
      kind: "unreachable",
      reason: timeout.aborted
        ? "The NikolAI server took too long to answer."
        : "The NikolAI server didn't answer.",
    };
  }
  return readResponse(response, check);
}

async function readResponse<T>(response: Response, check: Check<T>): Promise<ApiResult<T>> {
  switch (response.status) {
    case 200:
    case 204: {
      const body: unknown =
        response.status === 204 ? null : await response.json().catch(() => null);
      if (check(body)) return { kind: "ok", data: body };
      // Most likely the API and the contract disagree. Say so for developers, without logging
      // the body (it holds student work).
      console.warn("NikolAI: the server's response doesn't match the expected API contract.");
      return { kind: "unreachable", reason: UNREADABLE };
    }
    case 401:
      return { kind: "signed-out" };
    case 403:
      return withCode({ kind: "forbidden" }, await errorCode(response, ERROR_CODES[403]));
    case 404: {
      const code = await errorCode(response, ERROR_CODES[404]);
      return code === "repository_not_registered"
        ? { kind: "not-registered" }
        : withCode({ kind: "not-found" }, code);
    }
    case 409:
      return withCode({ kind: "conflict" }, await errorCode(response, ERROR_CODES[409]));
    default:
      return {
        kind: "unreachable",
        reason: `The NikolAI server answered with an error (${response.status}).`,
      };
  }
}

/** The body's `detail.code` when it is one of `known`, else null (a plain FastAPI error). */
async function errorCode<C extends string>(
  response: Response,
  known: readonly C[],
): Promise<C | null> {
  const body: unknown = await response.json().catch(() => null);
  if (typeof body !== "object" || body === null || !("detail" in body)) return null;
  const { detail } = body;
  if (typeof detail !== "object" || detail === null || !("code" in detail)) return null;
  return known.find((code) => code === detail.code) ?? null;
}

function withCode<R extends object, C extends string>(
  result: R,
  code: C | null,
): R | (R & { code: C }) {
  return code === null ? result : { ...result, code };
}

export function joinBase(baseUrl: string): string {
  return baseUrl.replace(/\/+$/, "");
}

export const encodePath = (...parts: string[]): string => parts.map(encodeURIComponent).join("/");
