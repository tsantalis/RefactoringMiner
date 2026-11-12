// AI contribution: 50%
/** WCAG AA contrast for the colour token pairs the team panel and PR tab draw with. */

import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

// Vitest doesn't process CSS, so `?raw` comes back empty: read the real files instead.
// Vitest runs each workspace package from its own directory. The tokens and the base rules moved
// to @nikolai/ui, so the palette and focus rules are read from there.
const tokens = readFileSync(resolve(process.cwd(), "../../packages/ui/src/tokens.css"), "utf8");
const styles = readFileSync(resolve(process.cwd(), "src/views/styles.css"), "utf8");
const focusStyles = readFileSync(resolve(process.cwd(), "src/views/focus.css"), "utf8");

type Palette = Record<string, string>;

/** The custom properties declared in one block of tokens.css: `:host,` for the themes. */
function palette(selector: string): Palette {
  const found: Palette = {};
  for (const match of block(tokens, selector).matchAll(/--([\w-]+):\s*(#[0-9a-fA-F]{6})/g)) {
    const name = match[1];
    const value = match[2];
    if (name !== undefined && value !== undefined) found[name] = value;
  }
  return found;
}

/** WCAG 2.x relative luminance, from the specification's formula (not the app's code). */
function luminance(hex: string): number {
  const digits = hex.slice(1);
  const channels = [0, 2, 4].map((offset) => {
    const channel = parseInt(digits.slice(offset, offset + 2), 16) / 255;
    return channel <= 0.03928 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4;
  });
  const [red = 0, green = 0, blue = 0] = channels;
  return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
}

function contrast(one: string, other: string): number {
  const [lighter = 0, darker = 0] = [luminance(one), luminance(other)].sort((a, b) => b - a);
  return (lighter + 0.05) / (darker + 0.05);
}

/** The body of the first block of `selector` in `text`, up to its closing brace. */
function block(text: string, selector: string): string {
  const start = text.indexOf(selector);
  if (start < 0) throw new Error(`no ${selector} block`);
  return text.slice(start + selector.length, text.indexOf("}", start));
}

function color(colors: Palette, token: string): string {
  const value = colors[token];
  if (value === undefined) throw new Error(`no --${token} in this theme`);
  return value;
}

/** A colour pair the views draw, and the WCAG ratio it has to reach. */
interface Pair {
  label: string;
  foreground: string;
  background: string;
  minimum: number;
}

/** WCAG AA minimums: normal-size text, and non-text pairs such as focus rings and the bar. */
const AA_TEXT = 4.5;
const AA_GRAPHIC = 3;

/** Every pair of text and background (or foreground) colours the views use. */
const PAIRS: Pair[] = [
  { label: "body text", foreground: "fg", background: "bg", minimum: AA_TEXT },
  { label: "body text on a card head", foreground: "fg", background: "subtle", minimum: AA_TEXT },
  { label: "text on a flagged row", foreground: "fg", background: "warn-bg", minimum: AA_TEXT },
  { label: "text on a hovered button", foreground: "fg", background: "hover", minimum: AA_TEXT },
  { label: "muted text", foreground: "muted", background: "bg", minimum: AA_TEXT },
  {
    label: "muted text on a card head",
    foreground: "muted",
    background: "subtle",
    minimum: AA_TEXT,
  },
  { label: "links", foreground: "accent", background: "bg", minimum: AA_TEXT },
  { label: "links on a card head", foreground: "accent", background: "subtle", minimum: AA_TEXT },
  { label: "the staff bar", foreground: "on-mark", background: "mark", minimum: AA_TEXT },
  {
    label: "the primary button and the fixed tick",
    foreground: "on-emphasis",
    background: "ok-emphasis",
    minimum: AA_TEXT,
  },
  { label: "success status", foreground: "ok-fg", background: "ok-bg", minimum: AA_TEXT },
  { label: "attention status", foreground: "warn-fg", background: "warn-bg", minimum: AA_TEXT },
  { label: "danger status", foreground: "bad-fg", background: "bad-bg", minimum: AA_TEXT },
  { label: "info status", foreground: "info-fg", background: "info-bg", minimum: AA_TEXT },
  { label: "neutral status", foreground: "gray-fg", background: "gray-bg", minimum: AA_TEXT },
  { label: "error text on a card", foreground: "bad-fg", background: "bg", minimum: AA_TEXT },
  { label: "fixed-in text on a card", foreground: "ok-fg", background: "bg", minimum: AA_TEXT },
  { label: "warning text on a card", foreground: "warn-fg", background: "bg", minimum: AA_TEXT },
  // Pairs the views draw the earlier list missed: a section counter, the story card's before and
  // after text, and the progress bar's fill, which is a graphic.
  { label: "a section counter", foreground: "fg", background: "track", minimum: AA_TEXT },
  { label: "story text before a change", foreground: "fg", background: "bad-bg", minimum: AA_TEXT },
  { label: "story text after a change", foreground: "fg", background: "ok-bg", minimum: AA_TEXT },
  {
    label: "the progress bar on its track",
    foreground: "ok-bar",
    background: "track",
    minimum: AA_GRAPHIC,
  },
  // The focus ring identifies a control, so WCAG asks for 3:1, not 4.5:1.
  { label: "focus ring on a page", foreground: "accent", background: "bg", minimum: AA_GRAPHIC },
  {
    label: "focus ring on a card head",
    foreground: "accent",
    background: "subtle",
    minimum: AA_GRAPHIC,
  },
  {
    label: "focus ring on a hovered row",
    foreground: "accent",
    background: "hover",
    minimum: AA_GRAPHIC,
  },
  {
    label: "focus ring on a track",
    foreground: "accent",
    background: "track",
    minimum: AA_GRAPHIC,
  },
];

const THEMES: [string, Palette][] = [
  ["light", palette(":host,")],
  ["dark", palette(':host([data-theme="dark"]),')],
];

describe.each(THEMES)("%s theme", (_name, colors) => {
  it.each(PAIRS)("$label meets its WCAG ratio", ({ foreground, background, minimum }) => {
    expect(contrast(color(colors, foreground), color(colors, background))).toBeGreaterThanOrEqual(
      minimum,
    );
  });
});

describe("colour usage", () => {
  it("draws the primary button's label in white on its green fill", () => {
    expect(block(tokens, ".button.primary {")).toMatch(/color:\s*var\(--on-emphasis\)/);
  });

  it("draws the fixed tick in white on its green fill", () => {
    expect(block(styles, ".check.done {")).toMatch(/color:\s*var\(--on-emphasis\)/);
  });

  it("shows a focus ring on every keyboard-focusable control", () => {
    for (const selector of ["a", "button", "input", "select", "textarea"]) {
      // Anchored, or "a:focus-visible" also matches the end of "textarea:focus-visible".
      expect(tokens).toMatch(new RegExp(`(^|[\\s,])${selector}:focus-visible`));
    }
    expect(tokens).toMatch(/outline:\s*2px solid var\(--accent\)/);
  });

  it("keeps the section picker's focus ring inside its clipped container", () => {
    expect(focusStyles).toMatch(/\.segmented button:focus-visible\s*\{/);
    expect(focusStyles).toMatch(/outline-offset:\s*-2px/);
  });
});
