// AI contribution: 50%
/** The NikolAI mark's geometry, shared with every front end, plus the DOM builder for GitHub. */

import { LOGO_GROUND, LOGO_STROKES, LOGO_VIEWBOX } from "@nikolai/ui";

export { LOGO_GROUND, LOGO_STROKES, LOGO_VIEWBOX };

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
