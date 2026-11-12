// AI contribution: 50%
import { describe, expect, it } from "vitest";
import { followSystemTheme } from "./theme";

function mediaDouble(matches: boolean): {
  media: Parameters<typeof followSystemTheme>[0];
  notify: (next: boolean) => void;
} {
  let listener: ((event: { matches: boolean }) => void) | undefined;
  return {
    media: {
      matches,
      addEventListener: (_type, next) => {
        listener = next;
      },
    },
    notify: (next) => {
      listener?.({ matches: next });
    },
  };
}

describe("followSystemTheme", () => {
  it("starts on the system preference and follows changes", () => {
    const page = mediaDouble(true);
    const root = { dataset: {} as DOMStringMap };

    followSystemTheme(page.media, root);
    expect(root.dataset.theme).toBe("dark");

    page.notify(false);
    expect(root.dataset.theme).toBe("light");
  });

  it("starts light when the system prefers light", () => {
    const page = mediaDouble(false);
    const root = { dataset: {} as DOMStringMap };

    followSystemTheme(page.media, root);

    expect(root.dataset.theme).toBe("light");
  });
});
