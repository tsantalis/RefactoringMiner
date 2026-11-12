// AI contribution: 50%
/** Reads the one-time notice the API puts on the dashboard URL after a failed roster claim. */

/** Same wording as `COURSE_NOT_LINKED` in `backend/app/api/sign_in_page.py`. */
export const COURSE_NOT_LINKED =
  "We couldn't link your course yet. Sign in again later or ask your instructor.";

const COURSE_PARAM = "course";
const NOT_LINKED = "not-linked";

type ReadableLocation = Pick<Location, "href">;
type ReplaceableHistory = Pick<History, "state" | "replaceState">;

/**
 * Read `?course=not-linked` once and take it out of the address bar, so a reload doesn't repeat
 * the notice. Returns the notice to show, or `null` when the API didn't send one.
 */
export function takeRosterNotice(
  location: ReadableLocation,
  history: ReplaceableHistory,
): string | null {
  const url = new URL(location.href);
  if (url.searchParams.get(COURSE_PARAM) !== NOT_LINKED) return null;
  url.searchParams.delete(COURSE_PARAM);
  history.replaceState(history.state, "", `${url.pathname}${url.search}${url.hash}`);
  return COURSE_NOT_LINKED;
}
