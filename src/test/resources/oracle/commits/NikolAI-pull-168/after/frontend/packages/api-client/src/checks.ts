// AI contribution: 50%
/**
 * Small runtime checks for API responses. Each check is also the response's TypeScript type
 * (`Infer<typeof check>`), so the type and its validation can't drift apart.
 */

export type Check<T> = (value: unknown) => value is T;
export type Infer<C> = C extends Check<infer T> ? T : never;

export const string: Check<string> = (value): value is string => typeof value === "string";

export const number: Check<number> = (value): value is number =>
  typeof value === "number" && Number.isFinite(value);

export const boolean: Check<boolean> = (value): value is boolean => typeof value === "boolean";

export function literal<const T extends readonly string[]>(...values: T): Check<T[number]> {
  const allowed = new Set<unknown>(values);
  return (value): value is T[number] => allowed.has(value);
}

export function nullable<T>(check: Check<T>): Check<T | null> {
  return (value): value is T | null => value === null || check(value);
}

export function array<T>(check: Check<T>): Check<T[]> {
  return (value): value is T[] => Array.isArray(value) && value.every(check);
}

type Shape = Record<string, Check<unknown>>;

/** An object with at least these fields; extra fields the server adds later are allowed. */
export function object<S extends Shape>(shape: S): Check<{ [K in keyof S]: Infer<S[K]> }> {
  const entries = Object.entries(shape);
  return (value): value is { [K in keyof S]: Infer<S[K]> } => {
    if (typeof value !== "object" || value === null || Array.isArray(value)) return false;
    const record = value as Record<string, unknown>;
    return entries.every(([key, check]) => check(record[key]));
  };
}

/** A value that passes `check` and also satisfies a rule across its fields. */
export function refine<T>(check: Check<T>, rule: (value: T) => boolean): Check<T> {
  return (value): value is T => check(value) && rule(value);
}
