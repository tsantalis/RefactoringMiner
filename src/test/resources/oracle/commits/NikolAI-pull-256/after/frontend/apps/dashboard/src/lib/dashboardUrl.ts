// AI contribution: 50%
/**
 * The dashboard's URL state: `?course=<id>`, `?course=<id>&page=requirements`, and a team's
 * planning page at `?team=<id>` (staff arriving from a course keep its `course` too).
 */

export type DashboardPage = "course" | "requirements";

/** Where the dashboard is: the open course, its page, and the open planning page's team. */
export interface DashboardLocation {
  course: string | null;
  page: DashboardPage;
  team: string | null;
}

/** The location the current URL names; anything missing or unknown is the home page. */
export function locationFromUrl(): DashboardLocation {
  const params = new URLSearchParams(window.location.search);
  return {
    course: params.get("course"),
    page: params.get("page") === "requirements" ? "requirements" : "course",
    team: params.get("team"),
  };
}

/** Write `location` into `url`'s query, leaving any other parameter alone. */
export function writeLocation(url: URL, location: DashboardLocation): void {
  const entries: [string, string | null][] = [
    ["course", location.course],
    ["page", location.page === "requirements" ? "requirements" : null],
    ["team", location.team],
  ];
  for (const [name, value] of entries) {
    if (value === null) {
      url.searchParams.delete(name);
    } else {
      url.searchParams.set(name, value);
    }
  }
}
