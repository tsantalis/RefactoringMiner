// AI contribution: 50%
/** Entry point of the dashboard page. */

import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { App } from "./App";
import { DashboardClient } from "./lib/api";
import { takeRosterNotice } from "./lib/roster-notice";
import { followSystemTheme } from "./lib/theme";
import "./styles.css";

const host = document.getElementById("root");
if (host === null) throw new Error("index.html has no #root element");

// @nikolai/ui/tokens.css has one palette per theme; the document follows the system's choice.
followSystemTheme(window.matchMedia("(prefers-color-scheme: dark)"), document.documentElement);

// The dashboard is one static page. The built files are served by nginx in production, where the
// Dockerfile's API_ORIGIN build argument feeds both this value and the CSP's connect-src.
const API_BASE_URL_DEFAULT = "https://api.nikolai.live";
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? API_BASE_URL_DEFAULT;
const client = new DashboardClient(apiBaseUrl);
const signInUrl = client.signInUrl(window.location.origin);
// The API adds `?course=not-linked` when the roster claim failed; read it once, then forget it.
const notice = takeRosterNotice(window.location, window.history);

createRoot(host).render(
  <StrictMode>
    <App client={client} signInUrl={signInUrl} notice={notice} />
  </StrictMode>,
);
