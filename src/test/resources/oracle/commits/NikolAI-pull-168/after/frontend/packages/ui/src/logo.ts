// AI contribution: 50%
/** The NikolAI mark (the "stencil N"): one rounded square and three strokes, on a 48-unit grid. */

export const LOGO_VIEWBOX = "0 0 48 48";
export const LOGO_GROUND = { width: 48, height: 48, rx: 11 } as const;
export const LOGO_STROKES = [
  { kind: "rect", x: 12, y: 12, width: 6, height: 24, rx: 1 },
  { kind: "rect", x: 30, y: 12, width: 6, height: 24, rx: 1 },
  { kind: "path", d: "M20 12h4.5l3.5 24h-4.5z" },
] as const;
