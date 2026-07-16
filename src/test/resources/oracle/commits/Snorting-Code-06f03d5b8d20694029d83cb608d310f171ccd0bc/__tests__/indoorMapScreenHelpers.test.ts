import { parseOutdoorStrategyParam } from "../utils/indoorMapScreenHelpers";

describe("parseOutdoorStrategyParam", () => {
  let warnSpy: jest.SpyInstance;

  beforeEach(() => {
    warnSpy = jest.spyOn(console, "warn").mockImplementation(() => {});
  });

  afterEach(() => {
    warnSpy.mockRestore();
  });

  it("returns undefined for empty or non-string params", () => {
    expect(parseOutdoorStrategyParam(undefined)).toBeUndefined();
    expect(parseOutdoorStrategyParam("")).toBeUndefined();
    expect(parseOutdoorStrategyParam("   ")).toBeUndefined();
  });

  it("returns parsed strategy for valid JSON", () => {
    const raw = JSON.stringify({ mode: "walking", label: "Walking", icon: "walk" });

    expect(parseOutdoorStrategyParam(raw)).toEqual({
      mode: "walking",
      label: "Walking",
      icon: "walk",
    });
  });

  it("returns undefined and warns for invalid JSON", () => {
    const result = parseOutdoorStrategyParam("{not-json}");

    expect(result).toBeUndefined();
    expect(warnSpy).toHaveBeenCalledWith(
      "IndoorMapScreen: invalid outdoorStrategy param",
      expect.any(SyntaxError),
    );
  });
});
