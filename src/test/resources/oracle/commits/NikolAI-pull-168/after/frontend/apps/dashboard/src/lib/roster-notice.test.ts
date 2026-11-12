// AI contribution: 50%
import { describe, expect, it } from "vitest";
import { COURSE_NOT_LINKED, takeRosterNotice } from "./roster-notice";

function historyDouble(): { history: Pick<History, "state" | "replaceState">; replaced: string[] } {
  const replaced: string[] = [];
  return {
    history: {
      state: null,
      replaceState: (_state, _title, url) => {
        replaced.push(String(url));
      },
    },
    replaced,
  };
}

function locationDouble(href: string): Pick<Location, "href"> {
  return { href };
}

describe("takeRosterNotice", () => {
  it("returns the notice and takes it out of the URL", () => {
    const { history, replaced } = historyDouble();

    const notice = takeRosterNotice(
      locationDouble("https://app.test/?course=not-linked&tab=courses#top"),
      history,
    );

    expect(notice).toBe(COURSE_NOT_LINKED);
    expect(replaced).toEqual(["/?tab=courses#top"]);
  });

  it("leaves other dashboard URLs alone", () => {
    const { history, replaced } = historyDouble();

    expect(takeRosterNotice(locationDouble("https://app.test/?course=other"), history)).toBeNull();
    expect(replaced).toEqual([]);
  });
});
