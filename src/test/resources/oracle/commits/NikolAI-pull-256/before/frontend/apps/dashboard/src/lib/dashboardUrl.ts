// AI contribution: 50%
/** The dashboard's URL state: `?course=<id>` and `?course=<id>&page=requirements`. */

export type DashboardPage = "course" | "requirements";

/** The `?course=` deep link, or null when the URL carries none. */
export function courseIdFromUrl(): string | null {
  return new URLSearchParams(window.location.search).get("course");
}

/** The `?page=` view of the course page; `requirements` only opens the requirements page. */
export function pageFromUrl(): DashboardPage {
  return new URLSearchParams(window.location.search).get("page") === "requirements"
    ? "requirements"
    : "course";
}
