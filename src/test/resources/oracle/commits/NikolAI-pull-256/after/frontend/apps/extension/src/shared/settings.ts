// AI contribution: 50%
/** The extension's settings, kept in chrome.storage.sync so they follow the user's browser. */

import { HttpFeedbackClient, type FeedbackClient } from "./api/client";
import { joinBase, type FetchLike } from "./api/http";
import { HttpNikolaiClient, type NikolaiClient } from "./api/nikolai-client";
import { SAMPLE_SCENARIOS, SampleFeedbackClient, type SampleScenario } from "./api/sample";
import { SampleNikolaiClient } from "./api/samples/client";
import { courseRole, type CourseRole } from "./api/views-contract";

export interface Settings {
  /** The NikolAI server, e.g. "https://nikolai.example.ca". Empty means sample data. */
  apiBaseUrl: string;
  sampleScenario: SampleScenario;
  /** Which layout the sample data shows. With a real server, the server decides the role. */
  previewRole: CourseRole;
}

export const DEFAULT_SETTINGS: Settings = {
  apiBaseUrl: "",
  sampleScenario: "reviewed",
  previewRole: "student",
};

/** Keep only well-formed values, so a bad or old stored value falls back to the default. */
export function normalizeSettings(stored: Record<string, unknown>): Settings {
  const scenario = SAMPLE_SCENARIOS.find((name) => name === stored.sampleScenario);
  return {
    apiBaseUrl:
      typeof stored.apiBaseUrl === "string"
        ? stored.apiBaseUrl.trim()
        : DEFAULT_SETTINGS.apiBaseUrl,
    sampleScenario: scenario ?? DEFAULT_SETTINGS.sampleScenario,
    previewRole: courseRole(stored.previewRole) ? stored.previewRole : DEFAULT_SETTINGS.previewRole,
  };
}

export async function loadSettings(): Promise<Settings> {
  const stored = await chrome.storage.sync.get(Object.keys(DEFAULT_SETTINGS));
  return normalizeSettings(stored);
}

export async function saveSettings(settings: Settings): Promise<void> {
  await chrome.storage.sync.set(settings);
}

/** Call `listener` with the new settings whenever they change in any extension page. */
export function onSettingsChanged(listener: (settings: Settings) => void): () => void {
  const handler = (_changes: unknown, area: string): void => {
    if (area === "sync") void loadSettings().then(listener);
  };
  chrome.storage.onChanged.addListener(handler);
  return () => {
    chrome.storage.onChanged.removeListener(handler);
  };
}

export interface Clients {
  feedback: FeedbackClient;
  nikolai: NikolaiClient;
  /** Where a signed-out user starts GitHub sign-in, or null on sample data (no server). */
  signInUrl: string | null;
  /** The course dashboard, where a student joins with the course code, or null if unknown. */
  dashboardUrl: string | null;
}

/** Sample clients until a server is set; otherwise clients that call it through `fetchImpl`. */
export function clientsFor(settings: Settings, fetchImpl?: FetchLike): Clients {
  if (settings.apiBaseUrl === "") {
    return {
      feedback: new SampleFeedbackClient(settings.sampleScenario),
      nikolai: new SampleNikolaiClient(settings.sampleScenario, settings.previewRole),
      signInUrl: null,
      dashboardUrl: null,
    };
  }
  return {
    feedback: new HttpFeedbackClient(settings.apiBaseUrl, fetchImpl),
    nikolai: new HttpNikolaiClient(settings.apiBaseUrl, fetchImpl),
    signInUrl: signInUrl(settings.apiBaseUrl),
    dashboardUrl: dashboardUrl(settings.apiBaseUrl),
  };
}

/**
 * The server's GitHub sign-in page (ADR 0001), or null when `apiBaseUrl` isn't an address the
 * options page would accept. It becomes a link on github.com, so only http(s) gets through.
 */
export function signInUrl(apiBaseUrl: string): string | null {
  if (apiBaseUrl.trim() === "" || checkServerUrl(apiBaseUrl) !== null) return null;
  return `${joinBase(apiBaseUrl.trim())}/auth/github/login`;
}

const LOCAL_HOSTS = new Set(["localhost", "127.0.0.1"]);

/** Where the dashboard's dev server runs (`frontend/apps/dashboard/vite.config.ts`). */
const LOCAL_DASHBOARD = "http://localhost:5173";

/**
 * The dashboard that goes with the server at `apiBaseUrl`, or null when it can't be told. The
 * deployment serves the API at `api.<domain>` and the dashboard at `app.<domain>` (production:
 * api.nikolai.live and app.nikolai.live); a local server pairs with the dashboard's dev server.
 */
export function dashboardUrl(apiBaseUrl: string): string | null {
  if (apiBaseUrl.trim() === "" || checkServerUrl(apiBaseUrl) !== null) return null;
  const url = new URL(apiBaseUrl.trim());
  if (LOCAL_HOSTS.has(url.hostname)) return LOCAL_DASHBOARD;
  if (url.protocol !== "https:" || !url.hostname.startsWith("api.")) return null;
  return `https://app.${url.hostname.slice("api.".length)}`;
}

/** The team's planning page on the dashboard at `dashboard` (US-19.1): `?team=<id>`. */
export function planningUrl(dashboard: string, teamId: string): string {
  const url = new URL(dashboard);
  url.searchParams.set("team", teamId);
  return url.href;
}

/** Why `value` can't be the server address, or null when it can (empty means sample data). */
export function checkServerUrl(value: string): string | null {
  if (value.trim() === "") return null;
  let url: URL;
  try {
    url = new URL(value.trim());
  } catch {
    return "Enter the full address, e.g. https://nikolai.example.ca.";
  }
  // Paths are joined onto the address as text, so a query or fragment would swallow them. The
  // raw text is checked because a bare "?" or "#" leaves url.search and url.hash empty.
  if (/[?#]/.test(value)) return "Enter the server's address without a ? or # part.";
  if (url.protocol === "https:") return null;
  if (url.protocol === "http:" && LOCAL_HOSTS.has(url.hostname)) return null;
  return "Use an https:// address. Only localhost may use http://.";
}

/** Origin pattern to ask host permission for, so the extension may call the server. */
export function originPattern(apiBaseUrl: string): string | null {
  try {
    const url = new URL(apiBaseUrl);
    if (url.protocol !== "https:" && url.protocol !== "http:") return null;
    return `${url.protocol}//${url.hostname}/*`;
  } catch {
    return null;
  }
}
