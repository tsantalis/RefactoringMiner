// AI contribution: 50%
/** The NikolAI mark (the "stencil N"): one rounded square and three strokes, on a 48-unit grid. */

export const LOGO_VIEWBOX = "0 0 48 48";
export const LOGO_GROUND = { width: 48, height: 48, rx: 11 } as const;
export const LOGO_STROKES = [
  { kind: "rect", x: 12, y: 12, width: 6, height: 24, rx: 1 },
  { kind: "rect", x: 30, y: 12, width: 6, height: 24, rx: 1 },
  { kind: "path", d: "M20 12h4.5l3.5 24h-4.5z" },
] as const;

const SVG_NS = "http://www.w3.org/2000/svg";

/**
 * The mark as DOM nodes, for the tab drawn into GitHub's own markup (outside React). It follows
 * the text color, so it matches GitHub's light and dark themes.
 */
export function logoElement(
  doc: Document,
  size: number,
  /** The strokes' color; by default the page's background, so they look cut out. */
  cut = "var(--bgColor-default, var(--color-canvas-default, #ffffff))",
): SVGSVGElement {
  const svg = doc.createElementNS(SVG_NS, "svg");
  svg.setAttribute("viewBox", LOGO_VIEWBOX);
  svg.setAttribute("width", String(size));
  svg.setAttribute("height", String(size));
  svg.setAttribute("aria-hidden", "true");
  const ground = doc.createElementNS(SVG_NS, "rect");
  for (const [key, value] of Object.entries(LOGO_GROUND)) ground.setAttribute(key, String(value));
  ground.setAttribute("fill", "currentColor");
  svg.append(ground);
  for (const { kind, ...attributes } of LOGO_STROKES) {
    const node = doc.createElementNS(SVG_NS, kind);
    for (const [key, value] of Object.entries(attributes)) node.setAttribute(key, String(value));
    node.setAttribute("fill", cut);
    svg.append(node);
  }
  return svg;
}
