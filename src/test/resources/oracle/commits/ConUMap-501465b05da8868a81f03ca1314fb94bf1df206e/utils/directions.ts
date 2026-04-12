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

interface RawRoutesResponse {
  routes?: RawRoute[];
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

// Helpers

function parseDurationSeconds(d: string | undefined): number {
  if (!d) return 0;
  return Math.round(Number.parseFloat(d.replace("s", ""))) || 0;
}

function formatDuration(seconds: number): string {
  const mins = Math.round(seconds / 60);

  if (mins < 60) {
    return `${mins} min${mins === 1 ? "" : "s"}`;
  }

  const hrs = Math.floor(mins / 60);
  const rem = mins % 60;
  const hrsText = `${hrs} hr${hrs === 1 ? "" : "s"}`;
  const minsText = `${rem} min${rem === 1 ? "" : "s"}`;

  return rem > 0 ? `${hrsText} ${minsText}` : hrsText;
}

function formatDistance(meters: number): string {
  if (meters < 1000) return `${meters} m`;
  return `${(meters / 1000).toFixed(1)} km`;
}

function normalizeRoute(route: RawRoute): NormalizedRoute {
  const legs = (route.legs ?? []).map((leg): NormalizedLeg => {
    const durationSeconds = parseDurationSeconds(leg.duration);

    const steps = (leg.steps ?? []).map((step): NormalizedStep => {
      const stepDurationSeconds = parseDurationSeconds(
        step.staticDuration ?? step.duration,
      );
      const transitDetails = step.transitDetails;

      return {
        distance: {
          text: formatDistance(step.distanceMeters ?? 0),
          value: step.distanceMeters ?? 0,
        },
        duration: {
          text: formatDuration(stepDurationSeconds),
          value: stepDurationSeconds,
        },
        html_instructions: step.navigationInstruction?.instructions ?? "",
        maneuver: step.navigationInstruction?.maneuver ?? "",
        polyline: { points: step.polyline?.encodedPolyline ?? "" },
        travel_mode: step.travelMode ?? "WALKING",
        transit_details: transitDetails
          ? {
              line: {
                name: transitDetails.transitLine?.name,
                short_name: transitDetails.transitLine?.nameShort,
                vehicle_type: transitDetails.transitLine?.vehicle?.type,
              },
              departure_stop: {
                name: transitDetails.stopDetails?.departureStop?.name,
                location: transitDetails.stopDetails?.departureStop?.location?.latLng
                  ? {
                      lat: transitDetails.stopDetails.departureStop.location.latLng
                        .latitude,
                      lng: transitDetails.stopDetails.departureStop.location.latLng
                        .longitude,
                    }
                  : undefined,
              },
              arrival_stop: {
                name: transitDetails.stopDetails?.arrivalStop?.name,
                location: transitDetails.stopDetails?.arrivalStop?.location?.latLng
                  ? {
                      lat: transitDetails.stopDetails.arrivalStop.location.latLng
                        .latitude,
                      lng: transitDetails.stopDetails.arrivalStop.location.latLng
                        .longitude,
                    }
                  : undefined,
              },
            }
          : undefined,
      };
    });

    const firstTransitStep = leg.steps?.find(
      (step) => step.transitDetails?.localizedValues,
    );
    const lastTransitStep = [...(leg.steps ?? [])]
      .reverse()
      .find((step) => step.transitDetails?.localizedValues);

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
        text: formatDuration(durationSeconds),
        value: durationSeconds,
      },
      departure_time: departureText ? { text: departureText } : undefined,
      arrival_time: arrivalText ? { text: arrivalText } : undefined,
      steps,
    };
  });

  return {
    summary: route.description ?? "",
    overview_polyline: { points: route.polyline?.encodedPolyline ?? "" },
    legs,
  };
}

// Strategy Pattern
type GoogleTravelMode = "WALK" | "TRANSIT" | "DRIVE" | "BICYCLE";

interface RouteStrategy {
  fetch(origin: Coordinate, destination: Coordinate): Promise<NormalizedRoute[] | null>;
}

abstract class GoogleRoutesStrategy implements RouteStrategy {
  protected abstract readonly travelMode: GoogleTravelMode;

  protected buildRequestBody(origin: Coordinate, destination: Coordinate) {
    return {
      origin: {
        location: {
          latLng: {
            latitude: origin.latitude,
            longitude: origin.longitude,
          },
        },
      },
      destination: {
        location: {
          latLng: {
            latitude: destination.latitude,
            longitude: destination.longitude,
          },
        },
      },
      travelMode: this.travelMode,
      computeAlternativeRoutes: true,
    };
  }

  async fetch(
    origin: Coordinate,
    destination: Coordinate,
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
        body: JSON.stringify(this.buildRequestBody(origin, destination)),
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        console.warn(`Routes API HTTP error: ${response.status}`);
        return null;
      }

      const data: RawRoutesResponse = await response.json();

      if (!data.routes || data.routes.length === 0) {
        return [];
      }

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
}

class WalkingRouteStrategy extends GoogleRoutesStrategy {
  protected readonly travelMode: GoogleTravelMode = "WALK";
}

class TransitRouteStrategy extends GoogleRoutesStrategy {
  protected readonly travelMode: GoogleTravelMode = "TRANSIT";
}

class DrivingRouteStrategy extends GoogleRoutesStrategy {
  protected readonly travelMode: GoogleTravelMode = "DRIVE";
}

class BicyclingRouteStrategy extends GoogleRoutesStrategy {
  protected readonly travelMode: GoogleTravelMode = "BICYCLE";
}

class ShuttleRouteStrategy implements RouteStrategy {
  async fetch(
    _origin: Coordinate,
    _destination: Coordinate,
  ): Promise<NormalizedRoute[] | null> {
    // Shuttle handled separately via Concordia shuttle schedule.
    return [];
  }
}

// Strategy Context / Factory
class RouteStrategyFactory {
  private static readonly strategies: Record<TransportationMode, RouteStrategy> = {
    walking: new WalkingRouteStrategy(),
    transit: new TransitRouteStrategy(),
    driving: new DrivingRouteStrategy(),
    bicycling: new BicyclingRouteStrategy(),
    shuttle: new ShuttleRouteStrategy(),
  };

  static getStrategy(mode: TransportationMode): RouteStrategy {
    return this.strategies[mode];
  }
}

// Public API

export async function fetchDirections(
  origin: Coordinate,
  destination: Coordinate,
  mode: TransportationMode,
): Promise<NormalizedRoute[] | null> {
  const strategy = RouteStrategyFactory.getStrategy(mode);
  return strategy.fetch(origin, destination);
}

export async function fetchAllDirections(
  origin: Coordinate,
  destination: Coordinate,
): Promise<Record<TransportationMode, NormalizedRoute[] | null>> {
  const modes: TransportationMode[] = [
    "walking",
    "transit",
    "driving",
    "bicycling",
    "shuttle",
  ];

  const results = await Promise.all(
    modes.map(async (mode) => {
      const routes = await fetchDirections(origin, destination, mode);
      return [mode, routes] as const;
    }),
  );

  return Object.fromEntries(results) as Record<
    TransportationMode,
    NormalizedRoute[] | null
  >;
}
