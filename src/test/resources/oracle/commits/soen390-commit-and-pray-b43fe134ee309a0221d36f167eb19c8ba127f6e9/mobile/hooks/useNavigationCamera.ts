import { useRef, useState, useEffect, useCallback } from "react";
import MapView, { Region } from "react-native-maps";
import * as Location from "expo-location";
import { DirectionsState, Coordinates } from "./useDirections";

const ANIMATION_DURATION = 600;

interface UseNavigationCameraParams {
  directionsState: DirectionsState;
  location: Location.LocationObject | null;
  selectedCampus: { initialRegion: Region };
  onRouteReady: (result: any) => void;
  checkProgress: (userLocation: Coordinates) => void;
}

export function useNavigationCamera({
  directionsState,
  location,
  selectedCampus,
  onRouteReady,
  checkProgress,
}: UseNavigationCameraParams) {
  const mapRef = useRef<MapView>(null);
  const previousDirectionsActiveRef = useRef(false);
  const [shouldFitRoute, setShouldFitRoute] = useState(directionsState.isActive);

  useEffect(() => {
    if (directionsState.isActive && !previousDirectionsActiveRef.current) {
      setShouldFitRoute(true);
    }
    if (!directionsState.isActive) {
      setShouldFitRoute(false);
    }
    previousDirectionsActiveRef.current = directionsState.isActive;
  }, [directionsState.isActive]);

  // Zoom to selected campus
  useEffect(() => {
    mapRef.current?.animateToRegion(selectedCampus.initialRegion, ANIMATION_DURATION);
  }, [selectedCampus]);

  // Map centers around user's location during navigation
  useEffect(() => {
    if (directionsState.isActive && location) {
      mapRef.current?.animateToRegion({
        latitude: location.coords.latitude,
        longitude: location.coords.longitude,
        latitudeDelta: 0.005,
        longitudeDelta: 0.005,
      }, ANIMATION_DURATION);
    }
  }, [directionsState.isActive, location]);

  // GPS-based step progression
  useEffect(() => {
    if (directionsState.isActive && location) {
      checkProgress({
        latitude: location.coords.latitude,
        longitude: location.coords.longitude,
      });
    }
  }, [location, directionsState.isActive, checkProgress]);

  const handleRouteReady = useCallback((result: any) => {
    onRouteReady(result);
    if (!directionsState.isActive && result?.coordinates?.length) {
      // Preview mode: zoom out to show the full route
      mapRef.current?.fitToCoordinates(result.coordinates, {
        edgePadding: { top: 80, right: 50, bottom: 80, left: 50 },
        animated: true,
      });
    } else if (shouldFitRoute && result?.coordinates?.length) {
      mapRef.current?.fitToCoordinates(result.coordinates, {
        edgePadding: { top: 160, right: 50, bottom: 220, left: 50 },
        animated: true,
      });
      setShouldFitRoute(false);
    }
  }, [onRouteReady, directionsState.isActive, shouldFitRoute]);

  return { mapRef, handleRouteReady };
}
