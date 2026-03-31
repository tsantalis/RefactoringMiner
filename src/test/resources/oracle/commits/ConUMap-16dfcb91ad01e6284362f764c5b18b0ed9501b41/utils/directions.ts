import { Coordinate } from "@/types/mapTypes";
import { TransportationMode } from "@/types/buildingTypes";

const ROUTES_BASE_URL = "https://routes.googleapis.com/directions/v2:computeRoutes";

// ---------------------------------------------------------------------------
// Raw Google Routes API v2 shapes
// ---------------------------------------------------------------------------

interface RawLatLng {
  latitude: number;
  longitude: number;
}

interface RawLocation {
  latLng?: RawLatLng;
}

interface RawStop {
  name?: string;
  location?: RawLocation;
}

interface RawStopDetails {
  departureStop?: RawStop;
  arrivalStop?: RawStop;
}

interface RawLocalizedTimeText {
  text?: string;
}

interface RawLocalizedTime {
  time?: RawLocalizedTimeText;
}

interface RawLocalizedValues {
  departureTime?: RawLocalizedTime;
  arrivalTime?: RawLocalizedTime;
}

interface RawVehicle {
  type?: string;
}

interface RawTransitLine {
  name?: string;
  nameShort?: string;
  vehicle?: RawVehicle;
}

interface RawTransitDetails {
  stopDetails?: RawStopDetails;
  localizedValues?: RawLocalizedValues;
  transitLine?: RawTransitLine;
}

interface RawNavigationInstruction {
  instructions?: string;
  maneuver?: string;
}

interface RawPolyline {
  encodedPolyline?: string;
}

interface RawStep {
  distanceMeters?: number;
  staticDuration?: string;
  duration?: string;
  polyline?: RawPolyline;
  navigationInstruction?: RawNavigationInstruction;
  travelMode?: string;
  transitDetails?: RawTransitDetails;
}

interface RawLeg {
  distanceMeters?: number;
  duration?: string;
  steps?: RawStep[];
}

interface RawRoute {
  description?: string;
  polyline?: RawPolyline;
  legs?: RawLeg[];
}

// ---------------------------------------------------------------------------
// Normalized (app-internal) route shapes
// ---------------------------------------------------------------------------

interface NormalizedTextValue {
  text: string;
  value: number;
}

interface NormalizedLatLng {
  lat: number;
  lng: number;
}

interface NormalizedTransitLine {
  name?: string;
  short_name?: string;
  vehicle_type?: string;
}

interface NormalizedTransitStop {
  name?: string;
  location?: NormalizedLatLng;
}

interface NormalizedTransitDetails {
  line: NormalizedTransitLine;
  departure_stop: NormalizedTransitStop;
  arrival_stop: NormalizedTransitStop;
}

export interface NormalizedStep {
  distance: NormalizedTextValue;
  duration: NormalizedTextValue;
  html_instructions: string;
  maneuver: string;
  polyline: { points: string };
  travel_mode: string;
  transit_details?: NormalizedTransitDetails;
}

export interface NormalizedLeg {
  distance: NormalizedTextValue;
  duration: NormalizedTextValue;
  departure_time?: { text: string };
  arrival_time?: { text: string };
  steps: NormalizedStep[];
}

export interface NormalizedRoute {
  summary: string;
  overview_polyline: { points: string };
  legs: NormalizedLeg[];
}

const MODE_MAP: Record<Exclude<TransportationMode, "shuttle">, string> = {
  walking: "WALK",
  transit: "TRANSIT",
  driving: "DRIVE",
  bicycling: "BICYCLE",
};

const FIELD_MASK = [
  "routes.description",
  "routes.polyline.encodedPolyline",
  "routes.legs.distanceMeters",
  "routes.legs.duration",
  "routes.legs.steps.distanceMeters",
  "routes.legs.steps.staticDuration",
  "routes.legs.steps.polyline.encodedPolyline",
  "routes.legs.steps.navigationInstruction",
  "routes.legs.steps.travelMode",
  "routes.legs.steps.transitDetails.stopDetails",
  "routes.legs.steps.transitDetails.localizedValues",
  "routes.legs.steps.transitDetails.transitLine.name",
  "routes.legs.steps.transitDetails.transitLine.nameShort",
  "routes.legs.steps.transitDetails.transitLine.vehicle.type",
  "routes.legs.steps.transitDetails.stopDetails.departureStop.name",
  "routes.legs.steps.transitDetails.stopDetails.departureStop.location",
  "routes.legs.steps.transitDetails.stopDetails.arrivalStop.name",
  "routes.legs.steps.transitDetails.stopDetails.arrivalStop.location",
].join(",");

/** Parses a duration string from the API (e.g. "120s") into seconds as a number. */
function parseDurationSeconds(d: string | undefined): number {
  if (!d) return 0;
  return Math.round(Number.parseFloat(d.replace("s", ""))) || 0;
}

/** Format seconds into a human-readable string ("2 mins", "1 hr 5 mins"). */
function formatDuration(seconds: number): string {
  const mins = Math.round(seconds / 60);
  if (mins < 60) {
    return `${mins} min${mins === 1 ? "" : "s"}`;
  }

  const hrs = Math.floor(mins / 60);
  const rem = mins % 60;
  const hrsText = `${hrs} hr` + (hrs === 1 ? "" : "s");
  const remSuffix = rem === 1 ? "" : "s";
  const remText = rem > 0 ? `${hrsText} ${rem} min${remSuffix}` : hrsText;
  return remText;
}

/** Format distance in meters into a human-readable string ("850 m", "1.2 km"). */
function formatDistance(meters: number): string {
  if (meters < 1000) return `${meters} m`;
  return `${(meters / 1000).toFixed(1)} km`;
}

/**
 * Converts a single Routes API v2 route object into the Directions-API shape
 * expected by the app's popup and map components.
 */
function normalizeRoute(r: RawRoute): NormalizedRoute {
  const legs = (r.legs ?? []).map((leg: RawLeg): NormalizedLeg => {
    const durSecs = parseDurationSeconds(leg.duration);

    const steps = (leg.steps ?? []).map((step: RawStep): NormalizedStep => {
      const stepDurSecs = parseDurationSeconds(step.staticDuration ?? step.duration);
      const td = step.transitDetails;

      return {
        distance: {
          text: formatDistance(step.distanceMeters ?? 0),
          value: step.distanceMeters ?? 0,
        },
        duration: {
          text: formatDuration(stepDurSecs),
          value: stepDurSecs,
        },
        html_instructions: step.navigationInstruction?.instructions ?? "",
        maneuver: step.navigationInstruction?.maneuver ?? "",
        polyline: { points: step.polyline?.encodedPolyline ?? "" },
        travel_mode: step.travelMode ?? "WALKING",
        transit_details: td
          ? {
              line: {
                name: td.transitLine?.name,
                short_name: td.transitLine?.nameShort,
                vehicle_type: td.transitLine?.vehicle?.type,
              },
              departure_stop: {
                name: td.stopDetails?.departureStop?.name,
                location: td.stopDetails?.departureStop?.location?.latLng
                  ? {
                      lat: td.stopDetails.departureStop.location.latLng.latitude,
                      lng: td.stopDetails.departureStop.location.latLng.longitude,
                    }
                  : undefined,
              },
              arrival_stop: {
                name: td.stopDetails?.arrivalStop?.name,
                location: td.stopDetails?.arrivalStop?.location?.latLng
                  ? {
                      lat: td.stopDetails.arrivalStop.location.latLng.latitude,
                      lng: td.stopDetails.arrivalStop.location.latLng.longitude,
                    }
                  : undefined,
              },
            }
          : undefined,
      };
    });

    const firstTransitStep = leg.steps?.find(
      (s: RawStep) => s.transitDetails?.localizedValues,
    );
    const lastTransitStep = [...(leg.steps ?? [])]
      .reverse()
      .find((s: RawStep) => s.transitDetails?.localizedValues);

    const departureText =
      firstTransitStep?.transitDetails?.localizedValues?.departureTime?.time?.text;
    const arrivalText =
      lastTransitStep?.transitDetails?.localizedValues?.arrivalTime?.time?.text;

    return {
      distance: {
        text: formatDistance(leg.distanceMeters ?? 0),
        value: leg.distanceMeters ?? 0,
      },
      duration: {
        text: formatDuration(durSecs),
        value: durSecs,
      },
      departure_time: departureText ? { text: departureText } : undefined,
      arrival_time: arrivalText ? { text: arrivalText } : undefined,
      steps,
    };
  });

  return {
    summary: r.description ?? "",
    overview_polyline: { points: r.polyline?.encodedPolyline ?? "" },
    legs,
  };
}

/**
 * Fetches directions from the Google Routes API v2 for a single transport mode.
 * Requires EXPO_PUBLIC_GOOGLE_API_KEY to be set in the environment.
 *
 * Returns the routes normalised to the Directions-API shape the app expects.
 * @returns Array of route objects on success, empty array for zero results, null on error.
 */
export async function fetchDirections(
  origin: Coordinate,
  destination: Coordinate,
  mode: Exclude<TransportationMode, "shuttle">,
): Promise<NormalizedRoute[] | null> {
  const apiKey = process.env.EXPO_PUBLIC_GOOGLE_API_KEY;
  if (!apiKey) {
    console.warn(
      "EXPO_PUBLIC_GOOGLE_API_KEY is not set – directions will not be available.",
    );
    return null;
  }

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 10_000);

  try {
    const response = await fetch(ROUTES_BASE_URL, {
      method: "POST",
      signal: controller.signal,
      headers: {
        "Content-Type": "application/json",
        "X-Goog-Api-Key": apiKey,
        "X-Goog-FieldMask": FIELD_MASK,
      },
      body: JSON.stringify({
        origin: {
          location: {
            latLng: { latitude: origin.latitude, longitude: origin.longitude },
          },
        },
        destination: {
          location: {
            latLng: { latitude: destination.latitude, longitude: destination.longitude },
          },
        },
        travelMode: MODE_MAP[mode],
        computeAlternativeRoutes: true,
      }),
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      console.warn(`Routes API HTTP error: ${response.status}`);
      return null;
    }

    const data = await response.json();

    if (!data.routes || data.routes.length === 0) return [];
    return data.routes.map(normalizeRoute);
  } catch (error) {
    clearTimeout(timeoutId);
    if (error instanceof Error && error.name === "AbortError") {
      console.warn("fetchDirections timed out after 10 s");
    } else {
      console.error("Failed to fetch directions:", error);
    }
    return null;
  }
}

/**
 * Fetches directions for all supported transport modes concurrently.
 * Shuttle is always returned as an empty array (no API route available;
 * handled separately via the Concordia shuttle schedule).
 */
export async function fetchAllDirections(
  origin: Coordinate,
  destination: Coordinate,
): Promise<Record<TransportationMode, NormalizedRoute[] | null>> {
  const modes: Exclude<TransportationMode, "shuttle">[] = [
    "walking",
    "transit",
    "driving",
    "bicycling",
  ];

  const results = await Promise.all(
    modes.map((mode) => fetchDirections(origin, destination, mode)),
  );

  return {
    walking: results[0],
    transit: results[1],
    driving: results[2],
    bicycling: results[3],
    shuttle: [],
  };
}
