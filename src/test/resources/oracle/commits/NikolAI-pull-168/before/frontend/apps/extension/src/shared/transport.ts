// AI contribution: 50%
/**
 * How the GitHub page's NikolAI views reach the server. A content script on github.com can't call
 * another origin (CORS), so it messages the background service worker, which holds the host
 * permission and forwards the request. The worker only forwards to the server set in the options,
 * and only under `/api/`.
 */

import { joinBase, REQUEST_TIMEOUT_MS, SERVER_REQUEST, type FetchLike } from "./api/http";
import type { Settings } from "./settings";

export interface ProxyRequest {
  type: "nikolai:request";
  /** Path and query under the server, e.g. `/api/repos/a/b/team`. */
  path: string;
  method: "GET" | "POST";
  body?: string;
}

export type ProxyReply = { ok: true; status: number; body: string } | { ok: false; error: string };

const OUTSIDE_API = "NikolAI only forwards requests to its own API.";

export function isProxyRequest(message: unknown): message is ProxyRequest {
  if (typeof message !== "object" || message === null) return false;
  const value = message as Record<string, unknown>;
  return (
    value.type === "nikolai:request" &&
    typeof value.path === "string" &&
    (value.method === "GET" || value.method === "POST") &&
    (value.body === undefined || typeof value.body === "string")
  );
}

function apiUrl(baseUrl: string, path: string): string | null {
  if (!path.startsWith("/api/") || path.includes("..") || path.includes("\\")) return null;
  const base = new URL(joinBase(baseUrl));
  const url = new URL(`${base.pathname.replace(/\/$/, "")}${path}`, base.origin);
  // Check again after parsing: encoded dots (`%2e%2e`) only resolve here.
  const apiRoot = `${base.pathname.replace(/\/$/, "")}/api/`;
  return url.origin === base.origin && url.pathname.startsWith(apiRoot) ? url.toString() : null;
}

/** Background side: forward one checked request to the configured server. */
export async function forwardRequest(
  request: ProxyRequest,
  settings: Settings,
  fetchImpl: FetchLike,
): Promise<ProxyReply> {
  if (settings.apiBaseUrl === "")
    return { ok: false, error: "No NikolAI server is set in the options." };
  const url = apiUrl(settings.apiBaseUrl, request.path);
  if (url === null) return { ok: false, error: OUTSIDE_API };
  const init: RequestInit = {
    ...SERVER_REQUEST,
    method: request.method,
    signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
  };
  if (request.body !== undefined) init.body = request.body;
  try {
    const response = await fetchImpl(url, init);
    return { ok: true, status: response.status, body: await response.text() };
  } catch {
    // The error text can name hosts and ports; the view only needs to know it failed.
    return { ok: false, error: "The NikolAI server didn't answer." };
  }
}

/** Content side: a fetch that goes through the background worker. Only the path is sent. */
export function messageFetch(send: (message: ProxyRequest) => Promise<ProxyReply>): FetchLike {
  return async (input, init) => {
    const url = new URL(input);
    const request: ProxyRequest = {
      type: "nikolai:request",
      path: `${url.pathname}${url.search}`,
      method: init?.method === "POST" ? "POST" : "GET",
    };
    if (typeof init?.body === "string") request.body = init.body;
    const reply = await untilAborted(send(request), init?.signal ?? undefined);
    if (!reply.ok) throw new TypeError(reply.error);
    const empty = reply.status === 204 || reply.body === "";
    return new Response(empty ? null : reply.body, { status: reply.status });
  };
}

/** Settle with `promise`, or reject as soon as `signal` aborts (the worker can't be cancelled). */
function untilAborted<T>(promise: Promise<T>, signal: AbortSignal | undefined): Promise<T> {
  if (!signal) return promise;
  return new Promise<T>((resolve, reject) => {
    const abort = (): void => {
      reject(new DOMException("The request was aborted.", "AbortError"));
    };
    if (signal.aborted) abort();
    signal.addEventListener("abort", abort, { once: true });
    promise.then(resolve, reject).finally(() => {
      signal.removeEventListener("abort", abort);
    });
  });
}
