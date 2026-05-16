import { getAvailableRooms } from "../api/indoorDirectionsApi";
import { buildEventIndoorTarget } from "../utils/eventIndoorNavigation";
import { getAvailableFloors } from "../utils/buildingIndoorMaps";
import { findBuildingFromLocationText } from "../utils/eventLocationBuildingMatcher";

jest.mock("../api/indoorDirectionsApi", () => ({
  getAvailableRooms: jest.fn(),
}));

jest.mock("../utils/buildingIndoorMaps", () => ({
  getAvailableFloors: jest.fn(),
}));

jest.mock("../utils/eventLocationBuildingMatcher", () => ({
  findBuildingFromLocationText: jest.fn(),
}));

describe("eventIndoorNavigation", () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });

  it("returns null when no building matches the event location", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue(null);

    const target = await buildEventIndoorTarget({
      locationText: "Unknown place",
      detailsText: "Classroom: H-937",
    });

    expect(target).toBeNull();
    expect(getAvailableRooms).not.toHaveBeenCalled();
  });

  it("marks unsupported floor and skips room resolution", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue({ id: "H" });
    (getAvailableFloors as jest.Mock).mockReturnValue(["1", "2", "8"]);
    (getAvailableRooms as jest.Mock).mockResolvedValue([]);

    const target = await buildEventIndoorTarget({
      locationText: "Hall Building",
      detailsText: "SGW\nHall\nClassroom: H-937\nThu, 10:00 - 11:15",
    });

    expect(target).toEqual({
      buildingId: "H",
      floor: "9",
      startFloor: null,
      floorSupported: false,
      destinationRoom: "H-937",
      startRoom: null,
    });
    expect(getAvailableRooms).toHaveBeenCalledTimes(3);
  });

  it("resolves classroom and selects a likely indoor start room", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue({ id: "H" });
    (getAvailableFloors as jest.Mock).mockReturnValue(["1", "8", "9"]);
    (getAvailableRooms as jest.Mock).mockImplementation(
      async (_buildingId: string, floor: string) => {
        if (floor === "1") {
          return ["H1-Maisonneuve-Entry", "Hall-Elevator-Main"];
        }
        if (floor === "9") {
          return ["H9-937", "Hall-Elevator-Main", "H9-Emergency-Exit-975"];
        }
        return [];
      },
    );

    const target = await buildEventIndoorTarget({
      locationText: "H-937",
      detailsText: "SGW\nHall\nClassroom: H-937\nThu, 10:00 - 11:15",
    });

    expect(target).toEqual({
      buildingId: "H",
      floor: "9",
      startFloor: "9",
      floorSupported: true,
      destinationRoom: "H9-937",
      startRoom: "Hall-Elevator-Main",
    });
  });

  it("matches calendar format with building name and Rm token", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue({ id: "MB" });
    (getAvailableFloors as jest.Mock).mockReturnValue(["1", "S2"]);
    (getAvailableRooms as jest.Mock).mockImplementation(
      async (_buildingId: string, floor: string) => {
        if (floor === "S2") {
          return ["MB-S2-330", "MB-Elevator-Main"];
        }
        return ["MB-1-101"];
      },
    );

    const target = await buildEventIndoorTarget({
      locationText: "John Molson School of Business Rm S2.330",
      detailsText: "John Molson School of Business\nRm S2.330",
    });

    expect(target).toEqual({
      buildingId: "MB",
      floor: "S2",
      startFloor: "S2",
      floorSupported: true,
      destinationRoom: "MB-S2-330",
      startRoom: "MB-Elevator-Main",
    });
  });

  it("prefers elevator as start room for MB basement floors", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue({ id: "MB" });
    (getAvailableFloors as jest.Mock).mockReturnValue(["1", "S2"]);
    (getAvailableRooms as jest.Mock).mockImplementation(
      async (_buildingId: string, floor: string) => {
        if (floor === "S2") {
          return ["MBS2-Entrance-Exit", "MB-Elevator-Main", "MB-S2-330"];
        }
        return ["MB-1-101"];
      },
    );

    const target = await buildEventIndoorTarget({
      locationText: "John Molson School of Business Rm S2.330",
      detailsText: "John Molson School of Business\nRm S2.330",
    });

    expect(target).toEqual({
      buildingId: "MB",
      floor: "S2",
      startFloor: "S2",
      floorSupported: true,
      destinationRoom: "MB-S2-330",
      startRoom: "MB-Elevator-Main",
    });
  });

  it("returns unsupported target when building has no mapped floors", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue({ id: "H" });
    (getAvailableFloors as jest.Mock).mockReturnValue([]);

    const target = await buildEventIndoorTarget({
      locationText: "Hall Building",
      detailsText: "Classroom: H-937",
    });

    expect(target).toEqual({
      buildingId: "H",
      floor: null,
      startFloor: null,
      floorSupported: false,
      destinationRoom: null,
      startRoom: null,
    });
    expect(getAvailableRooms).not.toHaveBeenCalled();
  });

  it("uses explicit floor from details when no room match is available", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue({ id: "H" });
    (getAvailableFloors as jest.Mock).mockReturnValue(["1", "2"]);
    (getAvailableRooms as jest.Mock).mockImplementation(
      async (_buildingId: string, floor: string) => {
        if (floor === "2") {
          return ["H2-201"];
        }
        return ["H1-101"];
      },
    );

    const target = await buildEventIndoorTarget({
      locationText: "Hall Building",
      detailsText: "Floor: 2\nTopic: Course planning session",
    });

    expect(target).toEqual({
      buildingId: "H",
      floor: "2",
      startFloor: "2",
      floorSupported: true,
      destinationRoom: null,
      startRoom: null,
    });
  });

  it("continues matching when one floor room fetch fails", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue({ id: "H" });
    (getAvailableFloors as jest.Mock).mockReturnValue(["1", "9"]);
    (getAvailableRooms as jest.Mock).mockImplementation(
      async (_buildingId: string, floor: string) => {
        if (floor === "1") {
          throw new Error("backend temp failure");
        }
        return ["H9-937", "Hall-Elevator-Main"];
      },
    );

    const target = await buildEventIndoorTarget({
      locationText: "Hall Building",
      detailsText: "Classroom: H9-937",
    });

    expect(target).toEqual({
      buildingId: "H",
      floor: "9",
      startFloor: "9",
      floorSupported: true,
      destinationRoom: "H9-937",
      startRoom: "Hall-Elevator-Main",
    });
  });

  it("handles very long input safely and still resolves early room tokens", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue({ id: "H" });
    (getAvailableFloors as jest.Mock).mockReturnValue(["9"]);
    (getAvailableRooms as jest.Mock).mockResolvedValue([
      "H9-937",
      "Hall-Elevator-Main",
    ]);

    const longTail = "X".repeat(8000);
    const target = await buildEventIndoorTarget({
      locationText: "Hall Building",
      detailsText: `Classroom: H9-937\n${longTail}`,
    });

    expect(target).toEqual({
      buildingId: "H",
      floor: "9",
      startFloor: "9",
      floorSupported: true,
      destinationRoom: "H9-937",
      startRoom: "Hall-Elevator-Main",
    });
  });

  it("returns unsupported when no floor can be inferred from details or room tokens", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue({ id: "H" });
    (getAvailableFloors as jest.Mock).mockReturnValue(["1", "2"]);
    (getAvailableRooms as jest.Mock).mockResolvedValue(["H1-101", "H2-201"]);

    const target = await buildEventIndoorTarget({
      locationText: "Hall Building",
      detailsText: "Classroom: LAB",
    });

    expect(target).toEqual({
      buildingId: "H",
      floor: null,
      startFloor: null,
      floorSupported: false,
      destinationRoom: "LAB",
      startRoom: null,
    });
  });

  it("infers floor from numeric room token for MB when floor is not explicit", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue({ id: "MB" });
    (getAvailableFloors as jest.Mock).mockReturnValue(["1", "3"]);
    (getAvailableRooms as jest.Mock).mockImplementation(
      async (_buildingId: string, floor: string) => {
        if (floor === "3") {
          return ["MB3-330", "MB-Elevator-Main"];
        }
        return ["MB1-101"];
      },
    );

    const target = await buildEventIndoorTarget({
      locationText: "John Molson School of Business",
      detailsText: "Rm 330",
    });

    expect(target).toEqual({
      buildingId: "MB",
      floor: "3",
      startFloor: "3",
      floorSupported: true,
      destinationRoom: "MB3-330",
      startRoom: "MB-Elevator-Main",
    });
  });

  it("prefers MB main elevator when multiple elevators are available on basement floor", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue({ id: "MB" });
    (getAvailableFloors as jest.Mock).mockReturnValue(["S2"]);
    (getAvailableRooms as jest.Mock).mockResolvedValue([
      "MB-Elevator-Side",
      "MB-Elevator-Main",
      "MB-S2-330",
    ]);

    const target = await buildEventIndoorTarget({
      locationText: "John Molson School of Business Rm S2.330",
      detailsText: "Rm S2.330",
    });

    expect(target).toEqual({
      buildingId: "MB",
      floor: "S2",
      startFloor: "S2",
      floorSupported: true,
      destinationRoom: "MB-S2-330",
      startRoom: "MB-Elevator-Main",
    });
  });

  it("uses lexical tie-break when MB basement elevators share same priority", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue({ id: "MB" });
    (getAvailableFloors as jest.Mock).mockReturnValue(["S2"]);
    (getAvailableRooms as jest.Mock).mockResolvedValue([
      "MB-Elevator-Main-B",
      "MB-Elevator-Main-A",
      "MB-S2-330",
    ]);

    const target = await buildEventIndoorTarget({
      locationText: "John Molson School of Business Rm S2.330",
      detailsText: "Rm S2.330",
    });

    expect(target).toEqual({
      buildingId: "MB",
      floor: "S2",
      startFloor: "S2",
      floorSupported: true,
      destinationRoom: "MB-S2-330",
      startRoom: "MB-Elevator-Main-A",
    });
  });

  it("falls back to the merged location/details lookup for building matching", async () => {
    (findBuildingFromLocationText as jest.Mock)
      .mockReturnValueOnce(null)
      .mockReturnValueOnce(null)
      .mockReturnValueOnce({ id: "H" });
    (getAvailableFloors as jest.Mock).mockReturnValue(["9"]);
    (getAvailableRooms as jest.Mock).mockResolvedValue([
      "H9-937",
      "Hall-Elevator-Main",
    ]);

    const target = await buildEventIndoorTarget({
      locationText: "Hall",
      detailsText: "Classroom: H9-937",
    });

    expect(target).toEqual({
      buildingId: "H",
      floor: "9",
      startFloor: "9",
      floorSupported: true,
      destinationRoom: "H9-937",
      startRoom: "Hall-Elevator-Main",
    });
    expect(findBuildingFromLocationText).toHaveBeenCalledTimes(3);
    expect(findBuildingFromLocationText).toHaveBeenNthCalledWith(
      3,
      "Hall Classroom: H9-937",
    );
  });

  it("returns null start room when destination is the only room on the floor", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue({ id: "H" });
    (getAvailableFloors as jest.Mock).mockReturnValue(["9"]);
    (getAvailableRooms as jest.Mock).mockResolvedValue(["H9-937"]);

    const target = await buildEventIndoorTarget({
      locationText: "Hall Building",
      detailsText: "Classroom: H9-937",
    });

    expect(target).toEqual({
      buildingId: "H",
      floor: "9",
      startFloor: "9",
      floorSupported: true,
      destinationRoom: "H9-937",
      startRoom: null,
    });
  });

  it("handles non-array room payloads from the API gracefully", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue({ id: "H" });
    (getAvailableFloors as jest.Mock).mockReturnValue(["9"]);
    (getAvailableRooms as jest.Mock).mockResolvedValue("unexpected");

    const target = await buildEventIndoorTarget({
      locationText: "Hall Building",
      detailsText: "Classroom: H9-937",
    });

    expect(target).toEqual({
      buildingId: "H",
      floor: "9",
      startFloor: "9",
      floorSupported: true,
      destinationRoom: "H9-937",
      startRoom: null,
    });
  });

  it("returns unsupported with null destination when only an unsupported explicit floor is provided", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue({ id: "H" });
    (getAvailableFloors as jest.Mock).mockReturnValue(["1", "2"]);
    (getAvailableRooms as jest.Mock).mockResolvedValue([]);

    const target = await buildEventIndoorTarget({
      locationText: "Hall Building",
      detailsText: "Floor: 9",
    });

    expect(target).toEqual({
      buildingId: "H",
      floor: "9",
      startFloor: null,
      floorSupported: false,
      destinationRoom: null,
      startRoom: null,
    });
  });

  it("falls back to generic start-room scoring when MB basement has no elevators", async () => {
    (findBuildingFromLocationText as jest.Mock).mockReturnValue({ id: "MB" });
    (getAvailableFloors as jest.Mock).mockReturnValue(["S2"]);
    (getAvailableRooms as jest.Mock).mockResolvedValue([
      "MB-S2-330",
      "MB-S2-Exit",
    ]);

    const target = await buildEventIndoorTarget({
      locationText: "John Molson School of Business Rm S2.330",
      detailsText: "Rm S2.330",
    });

    expect(target).toEqual({
      buildingId: "MB",
      floor: "S2",
      startFloor: "S2",
      floorSupported: true,
      destinationRoom: "MB-S2-330",
      startRoom: "MB-S2-Exit",
    });
  });
});
