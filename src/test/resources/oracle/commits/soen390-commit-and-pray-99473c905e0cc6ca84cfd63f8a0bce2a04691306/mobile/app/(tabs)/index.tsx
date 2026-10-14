import React, { useMemo, useRef, useEffect, useState } from "react";
import MapView, { Marker, Polygon, Region } from "react-native-maps";
import { View, Text } from "react-native";
import { useTheme } from "../../context/ThemeContext";
import { CAMPUSES, DEFAULT_CAMPUS, findCampusForCoordinate } from "../../constants/campusLocations";
import { BUILDING_POLYGON_COLORS } from "../../constants/mapColors";
import { useLocationPermissions } from "../../hooks/useLocationPermissions";
import { useWatchLocation } from "../../hooks/useWatchLocation";
import { useUserBuilding } from "../../hooks/useUserBuilding";
import { getInteriorPoint } from "../../utils/geometry";
import sgwBuildingsData from "../../data/buildings/sgw.json";
import loyolaBuildingsData from "../../data/buildings/loyola.json";
import CampusToggle from "../../components/campusToggle";
import BuildingModal from "../../components/buildingModal";
import { useDirections } from "../../hooks/useDirections";
import MapViewDirections from "react-native-maps-directions";
import SearchBar, { BuildingChoice } from "../../components/searchBar";
import NavigationSteps from "../../components/NavigationSteps";
import { styles, HIGHLIGHT_COLOR, STROKE_COLOR } from "@/styles/index.styles";

const LABEL_ZOOM_THRESHOLD = 0.015;
const ANCHOR_OFFSET = { x: 0.5, y: 0.5 };
const ANIMATION_DURATION = 600;

export default function Index() {
  const { colorScheme } = useTheme();
  const isDark = colorScheme === "dark";

  const defaultCampus = CAMPUSES[DEFAULT_CAMPUS];
  const [campusKey, setCampusKey] = useState<string>(DEFAULT_CAMPUS);

  const campusBuildingsData = useMemo(() => {
    return campusKey === "SGW" ? sgwBuildingsData.features : loyolaBuildingsData.features;
  }, [campusKey]);

  const permissionState = useLocationPermissions();
  const { location } = useWatchLocation({ enabled: permissionState.granted });
  const userBuilding = useUserBuilding(location);

  const currentCampus = useMemo(() => {
    if (!location) return undefined;
    return findCampusForCoordinate(location.coords.latitude, location.coords.longitude);
  }, [location]);

  const {
    state: directionsState,
    apiKey,
    startDirections,
    previewDirections,
    startDirectionsToBuilding,
    onRouteReady,
    endDirections,
    nextStep,
    prevStep,
    checkProgress,
  } = useDirections();

  const [showLabels, setShowLabels] = useState(
    defaultCampus.initialRegion.latitudeDelta <= LABEL_ZOOM_THRESHOLD
  );

  const [selectedBuilding, setSelectedBuilding] = useState<string | null>(null);
  const [selectedBuildingData, setSelectedBuildingData] = useState<any>(null);

  const [startChoice, setStartChoice] = useState<BuildingChoice | null>(null);
  const [destChoice, setDestChoice] = useState<BuildingChoice | null>(null);

  const handleEndDirections = () => {
    endDirections();
    setStartChoice(null);
    setDestChoice(null);
  };

  const handleStartRoute = () => {
    if (!destChoice || !location) return;

    startDirections({ latitude: location.coords.latitude, longitude: location.coords.longitude }, destChoice.coordinate);
  };

  const handlePreviewRoute = () => {
    if (!destChoice || !startChoice) return;

    previewDirections(startChoice?.coordinate, destChoice?.coordinate);
  }

  const handleRegionChange = (region: Region) => {
    setShowLabels(region.latitudeDelta <= LABEL_ZOOM_THRESHOLD);
  };

  const handleBuildingSelect = (buildingId: string, buildingData: any) => {
    setSelectedBuilding(buildingId);
    setSelectedBuildingData(buildingData);
  };

  const handleCloseModal = () => {
    setSelectedBuilding(null);
    setSelectedBuildingData(null);
  };

  const selectedCampus = useMemo(() => {
    return CAMPUSES[campusKey] ?? CAMPUSES[DEFAULT_CAMPUS];
  }, [campusKey]);

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

  const buildingPolygons = useMemo(() => {
    return campusBuildingsData.map((building: any) => {
      const isSelected = selectedBuilding === building.id;
      const isUserInside = userBuilding?.id === building.id;

      return (
        <React.Fragment key={building.id}>
          <Polygon
            coordinates={building.geometry.coordinates[0].map(([longitude, latitude]: [number, number]) => ({
              latitude,
              longitude,
            }))}
            fillColor={isSelected || isUserInside ? HIGHLIGHT_COLOR : BUILDING_POLYGON_COLORS.fillColor}
            strokeColor={isSelected || isUserInside ? STROKE_COLOR : BUILDING_POLYGON_COLORS.strokeColor}
            strokeWidth={BUILDING_POLYGON_COLORS.strokeWidth}
            tappable
            onPress={() => handleBuildingSelect(building.id, building)}
          />
        </React.Fragment>
      );
    });
  }, [campusBuildingsData, selectedBuilding, userBuilding]);

  const buildingLabels = useMemo(() => {
    return campusBuildingsData
      .filter((b: any) => (b.properties as { code?: string })?.code)
      .map((building: any) => {
        const centroid = getInteriorPoint(building.geometry.coordinates[0]);
        const code = (building.properties as { code: string }).code;

        return (
          <React.Fragment key={building.id}>
            <Polygon
              testID={`building-${building.id}`}
              coordinates={building.geometry.coordinates[0].map(
                ([longitude, latitude]) => ({
                  latitude,
                  longitude,
                })
              )}
              fillColor={
                selectedBuilding === building.id
                  ? HIGHLIGHT_COLOR
                  : BUILDING_POLYGON_COLORS.fillColor
              }
              strokeColor={
                selectedBuilding === building.id
                  ? STROKE_COLOR
                  : BUILDING_POLYGON_COLORS.strokeColor
              }
              strokeWidth={BUILDING_POLYGON_COLORS.strokeWidth}
              tappable
              onPress={() => handleBuildingSelect(building.id, building)}
            />
            <Marker key={`label-${building.id}`} coordinate={centroid} anchor={ANCHOR_OFFSET} tracksViewChanges={false}>
              <View style={styles.labelContainer}>
                <Text style={styles.buildingLabel}>{code}</Text>
              </View>
            </Marker>
          </React.Fragment>
        );
      });
  }, [campusBuildingsData]);

  const buildingChoices: BuildingChoice[] = useMemo(() => {
    const toChoices = (features: any[], campus: "SGW" | "Loyola") =>
      features.map((b: any) => ({
        id: b.id,
        name: b.properties?.name ?? b.properties?.code ?? "Unknown building",
        code: b.properties?.code,
        coordinate: getInteriorPoint(b.geometry.coordinates[0]),
        campus,
      }));

    return [...toChoices(sgwBuildingsData.features, "SGW"), ...toChoices(loyolaBuildingsData.features, "Loyola")];
  }, []);

  return (
    <View style={styles.container}>
      <MapView
        ref={mapRef}
        style={styles.map}
        initialRegion={selectedCampus.initialRegion}
        userInterfaceStyle={isDark ? "dark" : "light"}
        showsUserLocation
        onRegionChangeComplete={handleRegionChange}
      >
        {buildingPolygons}
        {showLabels && buildingLabels}

        {directionsState.origin && directionsState.destination && (
          <MapViewDirections
            key={`${campusKey}-${directionsState.origin?.latitude ?? "x"}-${directionsState.destination?.latitude ?? "y"}`}
            origin={directionsState.origin}
            destination={directionsState.destination}
            apikey={apiKey}
            strokeWidth={5}
            strokeColor="#0A84FF"
            onReady={(result) => {
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
            }}
            onError={(error) => console.error("[Index] MapViewDirections ERROR:", error)}
          />
        )}
      </MapView>



      {!directionsState.isActive && (
        <SearchBar
          buildings={buildingChoices}
          start={startChoice}
          destination={destChoice}
          onChangeStart={setStartChoice}
          onChangeDestination={setDestChoice}
          routeActive={directionsState.isActive}
          previewActive={!directionsState.isActive && !!directionsState.origin}
          onEndRoute={handleEndDirections}
          onStartRoute={handleStartRoute}
          onPreviewRoute={handlePreviewRoute}
          onExitPreview={handleEndDirections}
        />
      )}


      <View style={styles.overlay}>
        <Text style={styles.overlayTitle}>Current Location</Text>
        <Text style={styles.overlayValue}>
          {permissionState.granted ? currentCampus?.campus.name ?? "Outside campus boundaries" : "Location permission required"}
        </Text>

        {userBuilding && <Text style={styles.overlayBuilding}>📍 Inside: {userBuilding.name}</Text>}
      </View>

      {!directionsState.isActive && (
        <CampusToggle selectedCampus={campusKey} onCampusChange={setCampusKey} />
      )}

      {directionsState.isActive && directionsState.steps.length > 0 && (
        <NavigationSteps
          steps={directionsState.steps}
          currentStepIndex={directionsState.currentStepIndex}
          totalDistance={directionsState.routeInfo.distanceText ?? ""}
          totalDuration={directionsState.routeInfo.durationText ?? ""}
          isOffRoute={directionsState.isOffRoute}
          onEndNavigation={handleEndDirections}
          onNextStep={nextStep}
          onPrevStep={prevStep}
        />
      )}

      <BuildingModal
        visible={!!selectedBuilding}
        building={selectedBuildingData}
        onClose={handleCloseModal}
        location={location}
        onGetDirections={startDirectionsToBuilding}
      />
    </View>
  );
}