import { MaterialCommunityIcons } from "@expo/vector-icons";
import { Image as ExpoImage } from "expo-image";
import { useLocalSearchParams, useRouter } from "expo-router";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Pressable,
  ScrollView,
  Text,
  TextInput,
  useWindowDimensions,
  View,
} from "react-native";
import { IndoorPOIFilter } from "../components/IndoorPOIFilter";
import { IndoorPOIOverlay } from "../components/IndoorPOIOverlay";
import {
  IndoorDirectionsPanel,
  IndoorRouteOverlay,
} from "../components/IndoorRouteOverlay";
import { BUILDINGS } from "../constants/buildings";
import { type POICategoryId } from "../constants/indoorPOI";
import { colors, spacing } from "../constants/theme";
import { styles } from "../styles/IndoorMapScreen.styles";
import {
  isDestinationLegOrigin,
  pickClosestEntryExitNodeId,
} from "../utils/destinationIndoorLeg";
import {
  getNormalizedBuildingPlan,
  type IndoorRoomRecord,
} from "../utils/indoorBuildingPlan";
import { selectBestIndoorExit } from "../utils/indoorExit";
import {
  getFloorContentBounds,
  getFloorImageDimensions,
  getFloorStageLayout,
  trimParam,
  type FloorViewport
} from "../utils/indoorMapScreenHelpers";
import {
  getIndoorNavigationRoute,
  getIndoorNavigationRouteFromNode,
  getIndoorNavigationRouteToNode,
  NavigationRoute,
  type NavigationResult,
} from "../utils/indoorNavigation";
import { getIndoorPOIs } from "../utils/indoorPOI";
import { findIndoorRoomMatch } from "../utils/indoorRoomSearch";
import { getAvailableFloors, getBuildingPlanAsset, getFloorImageMetadata } from "../utils/mapAssets";
import { parseFloors } from "../utils/routeParams";
import {
  serializeTransitionPayload,
  type IndoorToOutdoorTransitionPayload,
} from "../utils/routeTransition";
const MARKER_SIZE = 28;
const DEFAULT_VIEWPORT_HEIGHT = 420;
const DEFAULT_AVAILABLE_FLOORS = [1] as const;
function useFloorSync(availableFloors: number[], selectedFloor: number, setSelectedFloor: (f: number) => void) {
  useEffect(() => {
    if (availableFloors.length > 0 && !availableFloors.includes(selectedFloor)) {
      setSelectedFloor(availableFloors[0] || 1);
    }
  }, [availableFloors, selectedFloor, setSelectedFloor]);
}

function useInitialRoomQuery(
  initialRoomQuery: string,
  availableFloors: number[],
  setSearchQuery: (q: string) => void,
  performRoomSearch: (q: string, floor: number) => void,
) {
  useEffect(() => {
    if (!initialRoomQuery) return;
    setSearchQuery(initialRoomQuery);
    performRoomSearch(initialRoomQuery, availableFloors[0] || 1);
  }, [availableFloors, initialRoomQuery, performRoomSearch, setSearchQuery]);
}

function useNavAutoTrigger(
  buildingName: string | undefined,
  navOrigin: string | undefined,
  navDest: string | undefined,
  handleNavigate: () => void,
) {
  useEffect(() => {
    if (buildingName && trimParam(navOrigin) && trimParam(navDest)) {
      handleNavigate();
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps
}

export default function IndoorMapScreen() {
  const router = useRouter();
  const {
    buildingName,
    floors,
    roomQuery,
    navOrigin,
    navDest,
    outdoorDestBuilding,
    outdoorStrategy,
    outdoorAccessibleOnly,
    destinationRoomQuery,
    accessibleOnly: accessibleOnlyParam,
  } = useLocalSearchParams<{
    buildingName: string;
    floors: string;
    roomQuery?: string;
    navOrigin?: string;
    navDest?: string;
    outdoorDestBuilding?: string;
    outdoorStrategy?: string;
    outdoorAccessibleOnly?: string;
    destinationRoomQuery?: string;
    accessibleOnly?: string;
  }>();
  // Accessibility mode state
  const [accessibleOnly, setAccessibleOnly] = useState(
    accessibleOnlyParam === "true",
  );
  const { width: windowWidth, height: windowHeight } = useWindowDimensions();
  const availableFloors = useMemo(() => {
    const parsed = parseFloors(floors);
    if (Array.isArray(parsed) && parsed.length > 0) return parsed;

    // Defensive fallback: some navigation flows may omit/garble `floors`.
    // Prefer using the building's real floors when available.
    if (typeof buildingName === "string" && buildingName.trim()) {
      const fallback = getAvailableFloors(buildingName);
      if (Array.isArray(fallback) && fallback.length > 0) return fallback;
    }
    return [...DEFAULT_AVAILABLE_FLOORS];
  }, [buildingName, floors]);
  const [selectedFloor, setSelectedFloor] = useState(availableFloors[0] || 1);
  const [searchQuery, setSearchQuery] = useState("");
  const [searchError, setSearchError] = useState<string | null>(null);
  const [selectedRoom, setSelectedRoom] = useState<IndoorRoomRecord | null>(
    null,
  );
  const [mapViewport, setMapViewport] = useState<FloorViewport>({
    width: 0,
    height: 0,
  });

  const [navOriginQuery, setNavOriginQuery] = useState(
    typeof navOrigin === "string" ? navOrigin.trim() : "",
  );
  const [navDestQuery, setNavDestQuery] = useState(
    typeof navDest === "string" ? navDest.trim() : "",
  );

  const destinationRoomQueryText =
    typeof destinationRoomQuery === "string" ? destinationRoomQuery.trim() : "";
  const trimmedOutdoorDestBuilding = trimParam(outdoorDestBuilding);
  const outdoorDestBuildingCode = trimmedOutdoorDestBuilding.toUpperCase();

  useEffect(() => {
    if (!availableFloors.length) return;

    // If current floor is no longer valid, reset it
    if (!availableFloors.includes(selectedFloor)) {
      setSelectedFloor(availableFloors[0]);
    }
  }, [availableFloors, selectedFloor]);

  // Cross-building origin leg UX: show the final destination room in the "To" input,
  // even though we route to an exit based on `outdoorDestBuilding`.
  useEffect(() => {
    const isCrossBuildingOriginLeg = Boolean(trimmedOutdoorDestBuilding);
    if (!isCrossBuildingOriginLeg) return;
    if (!destinationRoomQueryText) return;
    setNavDestQuery(destinationRoomQueryText);
  }, [destinationRoomQueryText, trimmedOutdoorDestBuilding]);
  const [navError, setNavError] = useState<string | null>(null);
  const [activeRoute, setActiveRoute] = useState<NavigationRoute | null>(null);
  const [pendingExitOutdoor, setPendingExitOutdoor] = useState<{ latitude: number; longitude: number } | null>(null);

  // Safety: if the user switches buildings, never reuse a previous building's selected exit
  // coordinate when transitioning outside.
  useEffect(() => {
    setPendingExitOutdoor(null);
  }, [buildingName]);
  const [activePOICategories, setActivePOICategories] = useState<Set<POICategoryId>>(new Set());

  const initialRoomQuery = trimParam(roomQuery);
  const mapKey = `${buildingName}-${selectedFloor}`;
  const floorImageMetadata = getFloorImageMetadata(
    buildingName || "",
    selectedFloor,
  );
  const floorImageAsset = floorImageMetadata?.source;
  const normalizedBuildingPlan = useMemo(
    () => (buildingName ? getNormalizedBuildingPlan(buildingName) : null),
    [buildingName],
  );

  const allPOIs = buildingName ? getIndoorPOIs(buildingName) : [];

  const handlePOIToggle = useCallback((categoryId: POICategoryId) => {
    setActivePOICategories((prev) => {
      const next = new Set(prev);
      if (next.has(categoryId)) {
        next.delete(categoryId);
      } else {
        next.add(categoryId);
      }
      return next;
    });
  }, []);

  useEffect(() => {
    setSearchQuery("");
    setSearchError(null);
    setSelectedRoom(null);
  }, [buildingName]);

  useFloorSync(availableFloors, selectedFloor, setSelectedFloor);


  const currentFloorRooms = useMemo(
    () => normalizedBuildingPlan?.roomsByFloor[selectedFloor] ?? [],
    [normalizedBuildingPlan, selectedFloor],
  );
  const coordinateScale = floorImageMetadata?.coordinateScale ?? 1;
  const scaledCurrentFloorRooms = useMemo(
    () =>
      currentFloorRooms.map((room) => ({
        ...room,
        x: room.x * coordinateScale,
        y: room.y * coordinateScale,
      })),
    [coordinateScale, currentFloorRooms],
  );

  const floorImageDimensions = useMemo(
    () => getFloorImageDimensions(floorImageMetadata, scaledCurrentFloorRooms),
    [floorImageMetadata, scaledCurrentFloorRooms],
  );
  const floorBounds = useMemo(
    () =>
      floorImageMetadata?.showFullImage
        ? { minX: 0, minY: 0, maxX: floorImageDimensions.width, maxY: floorImageDimensions.height }
        : getFloorContentBounds(floorImageDimensions, scaledCurrentFloorRooms),
    [floorImageDimensions, floorImageMetadata?.showFullImage, scaledCurrentFloorRooms],
  );

  const effectiveViewport = useMemo<FloorViewport>(
    () => ({
      width: mapViewport.width || Math.max(windowWidth, 320),
      height: mapViewport.height || Math.max(windowHeight * 0.44, DEFAULT_VIEWPORT_HEIGHT),
    }),
    [mapViewport, windowHeight, windowWidth],
  );

  const floorStageLayout = useMemo(
    () =>
      getFloorStageLayout(effectiveViewport, floorImageDimensions, floorBounds),
    [effectiveViewport, floorBounds, floorImageDimensions],
  );

  const showFloorImageMap = floorImageAsset != null;
  const showNoMapMessage = !showFloorImageMap;

  const selectedRoomOnCurrentFloor = useMemo(() => {
    if (selectedRoom?.floor !== selectedFloor) return null;
    return {
      ...selectedRoom,
      x: selectedRoom.x * coordinateScale,
      y: selectedRoom.y * coordinateScale,
    };
  }, [coordinateScale, selectedFloor, selectedRoom]);

  const selectedRoomMarkerPosition = useMemo(() => {
    if (!selectedRoomOnCurrentFloor) return null;
    return {
      left:
        floorStageLayout.frameLeft +
        (selectedRoomOnCurrentFloor.x - floorBounds.minX) *
        floorStageLayout.scale -
        MARKER_SIZE / 2,
      top:
        floorStageLayout.frameTop +
        (selectedRoomOnCurrentFloor.y - floorBounds.minY) *
        floorStageLayout.scale -
        MARKER_SIZE / 2,
    };
  }, [
    floorBounds.minX,
    floorBounds.minY,
    floorStageLayout,
    selectedRoomOnCurrentFloor,
  ]);

  const performRoomSearch = useCallback(
    (rawQuery: string, currentFloor: number) => {
      const trimmedQuery = rawQuery.trim();

      if (!normalizedBuildingPlan) {
        setSelectedRoom(null);
        setSearchError(`Room search is not available for ${buildingName}.`);
        return;
      }

      const match = findIndoorRoomMatch(normalizedBuildingPlan, trimmedQuery, {
        currentFloor,
      });

      if (!match) {
        setSelectedRoom(null);
        setSearchError(
          `Room "${trimmedQuery}" was not found in ${buildingName}.`,
        );
        return;
      }

      setSelectedRoom(match.room);
      setSearchQuery(match.room.label);
      setSearchError(null);

      if (match.floor !== currentFloor) {
        setSelectedFloor(match.floor);
      }
    },
    [buildingName, normalizedBuildingPlan],
  );

  useInitialRoomQuery(initialRoomQuery, availableFloors, setSearchQuery, performRoomSearch);

  const failNavigation = useCallback((message: string) => {
    setNavError(message);
    setActiveRoute(null);
  }, []);

  const applyNavigationResult = useCallback(
    (result: NavigationResult) => {
      if (result.success) {
        setActiveRoute(result.route);
        setSelectedFloor(result.route.origin.floor);
      } else {
        failNavigation(result.message);
      }
    },
    [failNavigation],
  );

  const routeDestinationIndoorLegFromEntrance = useCallback((): boolean => {
    if (!buildingName) return true;
    if (!isDestinationLegOrigin(navOriginQuery)) return false;

    try {
      const destQuery = navDestQuery.trim();
      if (!destQuery) {
        failNavigation("Enter a destination room to continue indoors.");
        return true;
      }

      const plan = getNormalizedBuildingPlan(buildingName);
      const asset = getBuildingPlanAsset(buildingName);
      if (!plan || !asset) {
        failNavigation(`No building plan found for "${buildingName}".`);
        return true;
      }

      const destMatch = findIndoorRoomMatch(plan, destQuery);
      if (!destMatch) {
        failNavigation(`Room "${destQuery}" was not found in ${buildingName}.`);
        return true;
      }

      type EntryExitNode = {
        id: string;
        type: string;
        x?: number;
        y?: number;
      };

      const entryNodes: EntryExitNode[] = (asset.nodes ?? []).filter(
        (n: EntryExitNode) => n.type === "building_entry_exit",
      );
      if (entryNodes.length === 0) {
        failNavigation(`No building entrances were found for ${buildingName}.`);
        return true;
      }

      const entryNodeId =
        pickClosestEntryExitNodeId({
          entryNodes,
          destinationRoom: destMatch.room,
        }) ?? entryNodes[0]?.id;
      if (!entryNodeId) {
        failNavigation(`No usable entrance node was found for ${buildingName}.`);
        return true;
      }

      const result = getIndoorNavigationRouteFromNode(
        buildingName,
        entryNodeId,
        destQuery,
        { accessibleOnly },
      );

      applyNavigationResult(result);
      return true;
    } catch {
      failNavigation("Unable to compute indoor directions from the entrance.");
      return true;
    }
  }, [
    accessibleOnly,
    applyNavigationResult,
    buildingName,
    failNavigation,
    navDestQuery,
    navOriginQuery,
  ]);

  const routeToBestExitForCrossBuildingOrigin = useCallback((): boolean => {
    if (!buildingName) return true;

    const isCrossBuildingSignal =
      Boolean(outdoorDestBuildingCode) &&
      outdoorDestBuildingCode !== buildingName.trim().toUpperCase();
    const isCrossBuildingOriginLeg = Boolean(trimmedOutdoorDestBuilding);

    if (!(isCrossBuildingOriginLeg && isCrossBuildingSignal)) return false;

    try {
      const plan = getNormalizedBuildingPlan(buildingName);
      if (!plan) {
        failNavigation(`No building plan found for "${buildingName}".`);
        return true;
      }

      const originMatch = findIndoorRoomMatch(plan, navOriginQuery);
      if (!originMatch) {
        failNavigation(
          `Could not find room matching "${navOriginQuery}" in ${buildingName}.`,
        );
        return true;
      }

      const exitPick = selectBestIndoorExit(
        buildingName,
        {
          roomOrNodeId: originMatch.room.id,
          x: originMatch.room.x,
          y: originMatch.room.y,
          floor: originMatch.room.floor,
        },
        { accessibleOnly },
      );

      if (!exitPick.success) {
        failNavigation(exitPick.message);
        return true;
      }

      // Always prefer an explicit per-exit outdoor coordinate.
      // If it's missing, fall back to the building centroid later when continuing outside.
      setPendingExitOutdoor(exitPick.exit.outdoorLatLng ?? null);

      const result = getIndoorNavigationRouteToNode(
        buildingName,
        navOriginQuery,
        exitPick.exit.nodeId,
        { accessibleOnly },
      );

      applyNavigationResult(result as any);
      return true;
    } catch {
      failNavigation("Unable to compute an indoor route to an exit.");
      return true;
    }
  }, [
    accessibleOnly,
    applyNavigationResult,
    buildingName,
    failNavigation,
    navOriginQuery,
    outdoorDestBuildingCode,
    trimmedOutdoorDestBuilding,
  ]);

  const handleNavigate = useCallback(() => {
    if (!buildingName) return;
    setNavError(null);
    setPendingExitOutdoor(null);

    // Destination-building leg: when arriving from the Campus Map "Continue indoors" step,
    // we may not know which entrance the user used. In that case, CampusMapScreen passes
    // navOrigin="ENTRANCE" and navDest=<room query>. We route from the closest
    // building_entry_exit node to the destination room.
    if (routeDestinationIndoorLegFromEntrance()) return;

    // IndoorMapScreen is building-local only. If the destination looks like a different
    // building/campus code, tell the user to start cross-building navigation from the Campus Map.
    const typedDest = navDestQuery.trim().toUpperCase();
    const isCampusCode = typedDest === "SGW" || typedDest === "LOYOLA";
    const isDifferentBuildingCode = BUILDINGS.some(
      (b) => b.name.trim().toUpperCase() === typedDest,
    );
    // If IndoorMapScreen was opened as the *origin-building* leg of a cross-building trip,
    // CampusMapScreen will pass `outdoorDestBuilding`. In that case we *do* allow typing a
    // different building code as navDest because it means "route to an exit and continue outside".
    const isCrossBuildingOriginLeg = Boolean(trimmedOutdoorDestBuilding);
    if (
      !isCrossBuildingOriginLeg &&
      (isCampusCode || isDifferentBuildingCode) &&
      typedDest !== buildingName.trim().toUpperCase()
    ) {
      setNavError(
        "Cross-building directions start from the Campus Map. Open the Campus Map and use Directions to navigate between buildings.",
      );
      setActiveRoute(null);
      return;
    }

    // Cross-building origin leg: if navDest is a building/campus code (ex: "CC") we
    // interpret it as "route to the best exit".
    if (routeToBestExitForCrossBuildingOrigin()) return;

    const result = getIndoorNavigationRoute(
      buildingName,
      navOriginQuery,
      navDestQuery,
      { accessibleOnly },
    );

    applyNavigationResult(result);
  }, [
    accessibleOnly,
    applyNavigationResult,
    buildingName,
    navDestQuery,
    navOriginQuery,
    routeDestinationIndoorLegFromEntrance,
    routeToBestExitForCrossBuildingOrigin,
    trimmedOutdoorDestBuilding,
  ]);

  const handleContinueOutside = useCallback(() => {
    if (!buildingName) return;
    const destCode = outdoorDestBuildingCode;
    if (!destCode) return;

    // Choose a sane outdoor starting point. Prefer the selected exit's outdoorLatLng.
    // If it's missing, fall back to the building centroid (never (0,0)).
    const originCode = buildingName.trim().toUpperCase();
    const originBuilding = BUILDINGS.find(
      (b) => b.name.trim().toUpperCase() === originCode,
    );

    // Guardrail: never start the outdoor leg from a coordinate that is clearly not near
    // the current building (stale state, bad exit metadata, etc.).
    const isLikelyNearOriginBuilding = (candidate: { latitude: number; longitude: number }) => {
      const origin = originBuilding?.coordinates;
      if (!origin) return true; // can’t validate; accept
      const dLat = candidate.latitude - origin.latitude;
      const dLng = candidate.longitude - origin.longitude;
      // Rough distance check in degrees. For Concordia SGW buildings this should be very small.
      const distSq = dLat * dLat + dLng * dLng;
      // ~0.003 degrees is on the order of a few hundred meters; different campuses will be far larger.
      return distSq < 0.003 * 0.003;
    };

    const candidateExitOutdoor = pendingExitOutdoor;
    const effectiveExitOutdoor =
      candidateExitOutdoor && isLikelyNearOriginBuilding(candidateExitOutdoor)
        ? candidateExitOutdoor
        : originBuilding?.coordinates ?? null;
    if (!effectiveExitOutdoor) {
      setNavError(
        "Couldn't determine an outdoor start point for this building exit. Please try a different exit.",
      );
      return;
    }

    const payload: IndoorToOutdoorTransitionPayload = {
      mode: "indoor_to_outdoor",
      originBuildingCode: originCode,
      // These are not used by CampusMapScreen for the outdoor leg start override.
      // Provide safe placeholders (exitOutdoor is the important piece for startOverride).
      exitNodeId: "",
      exitIndoor: {
        buildingCode: originCode,
        floor: 1,
        x: 0,
        y: 0,
      },
      destinationBuildingCode: destCode,
      strategy: (() => {
        if (typeof outdoorStrategy !== "string" || !outdoorStrategy) return undefined;
        try {
          return JSON.parse(outdoorStrategy);
        } catch (e) {
          // If the strategy param is malformed, ignore it and continue with defaults.
          // Sonar: don't swallow exceptions silently.
          console.warn("IndoorMapScreen: invalid outdoorStrategy param", e);
          return undefined;
        }
      })(),
      accessibleOnly:
        outdoorAccessibleOnly === "true" || accessibleOnly === true,
      exitOutdoor: effectiveExitOutdoor,
    };

    router.push({
      pathname: "/CampusMapScreen",
      params: {
        transition: serializeTransitionPayload(payload),
        ...(destinationRoomQueryText ? { destinationRoomQuery: destinationRoomQueryText } : {}),
      },
    });
  }, [accessibleOnly, buildingName, destinationRoomQueryText, outdoorAccessibleOnly, outdoorDestBuildingCode, outdoorStrategy, pendingExitOutdoor, router]);

  useNavAutoTrigger(buildingName, navOrigin, navDest, handleNavigate);


  return (
    <View style={styles.container}>
      <View style={styles.searchPanel}>
        <View style={styles.titleRow}>
          <Text style={styles.buildingTitle}>{buildingName} Building</Text>
          <Pressable
            testID="indoor-accessible-mode-toggle"
            accessibilityRole="switch"
            accessibilityState={{ checked: accessibleOnly }}
            accessibilityLabel="Toggle accessible route"
            onPress={() => setAccessibleOnly((prev) => !prev)}
            style={[
              styles.accessibleToggle,
              accessibleOnly && styles.accessibleToggleActive,
            ]}
          >
            <MaterialCommunityIcons
              name="wheelchair-accessibility"
              size={18}
              color={accessibleOnly ? colors.white : colors.primary}
            />
            <Text
              style={[
                styles.accessibleToggleText,
                accessibleOnly && styles.accessibleToggleTextActive,
              ]}
            >
              Accessible
            </Text>
          </Pressable>
        </View>

        <View style={styles.searchRow}>
          <TextInput
            style={styles.searchInput}
            placeholder="From (H-110)"
            placeholderTextColor={colors.gray500}
            value={navOriginQuery}
            onChangeText={setNavOriginQuery}
            returnKeyType="next"
          />
          <TextInput
            style={styles.searchInput}
            placeholder="To (H-920)"
            placeholderTextColor={colors.gray500}
            value={navDestQuery}
            onChangeText={setNavDestQuery}
            returnKeyType="go"
            onSubmitEditing={handleNavigate}
          />
          <Pressable style={styles.searchButton} onPress={handleNavigate}>
            <Text style={styles.searchButtonText}>Go</Text>
          </Pressable>
        </View>



        {navError && (
          <View style={styles.errorBanner}>
            <Text style={styles.errorText}>{navError}</Text>
          </View>
        )}

        {Boolean(activeRoute) && Boolean(trimmedOutdoorDestBuilding) && (
          <View style={{ marginTop: spacing.sm }}>
            {destinationRoomQueryText ? (
              <Text style={{ color: colors.gray700, marginBottom: spacing.xs }}>
                Destination: {destinationRoomQueryText}
              </Text>
            ) : null}
            <Pressable
              accessibilityRole="button"
              onPress={handleContinueOutside}
              style={[styles.searchButton, { alignSelf: "flex-start" }]}
              testID="continue-outside"
            >
              <Text style={styles.searchButtonText}>Continue outside</Text>
            </Pressable>
          </View>
        )}

        {selectedRoom && !activeRoute && (
          <View style={styles.selectedRoomBanner} testID="selected-room-banner">
            <Text style={styles.selectedRoomText}>
              Showing {selectedRoom.label} on floor {selectedRoom.floor}
            </Text>
          </View>
        )}

        {searchError && (
          <View style={styles.errorBanner} testID="room-search-error">
            <Text style={styles.errorText}>{searchError}</Text>
          </View>
        )}
      </View>

      <View style={styles.floorSelectorWrapper}>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.floorSelector}
        >
          {availableFloors.map((floor: number) => (
            <Pressable
              key={floor}
              testID={`floor-button-${floor}`}
              onPress={() => setSelectedFloor(floor)}
              style={[
                styles.floorButton,
                selectedFloor === floor && styles.floorButtonActive,
              ]}
              accessibilityRole="button"
              accessibilityState={{ selected: selectedFloor === floor }}
            >
              <Text
                style={[
                  styles.floorButtonText,
                  selectedFloor === floor && styles.floorButtonTextActive,
                ]}
              >
                {floor}
              </Text>
            </Pressable>
          ))}
        </ScrollView>
      </View>

      <IndoorPOIFilter
        activeCategories={activePOICategories}
        onToggle={handlePOIToggle}
      />

      <View
        testID="indoor-map-container"
        style={styles.mapContainer}
        onLayout={(event) => {
          const { width, height } = event.nativeEvent.layout;
          setMapViewport({ width, height });
        }}
      >
        {showFloorImageMap ? (
          <View style={styles.mapViewport} testID="indoor-floor-stage">
            <View
              style={[
                styles.floorFrame,
                {
                  left: floorStageLayout.frameLeft,
                  top: floorStageLayout.frameTop,
                  width: floorStageLayout.frameWidth,
                  height: floorStageLayout.frameHeight,
                },
              ]}
            >
              <ExpoImage
                testID="indoor-floor-image"
                source={floorImageAsset}
                style={{
                  position: "absolute",
                  left: floorStageLayout.imageLeft,
                  top: floorStageLayout.imageTop,
                  width: floorStageLayout.imageWidth,
                  height: floorStageLayout.imageHeight,
                }}
                contentFit="fill"
              />
            </View>

            {activeRoute && (
              <IndoorRouteOverlay
                route={activeRoute}
                floor={selectedFloor}
                coordinateScale={coordinateScale}
                stageLayout={floorStageLayout}
                floorBounds={floorBounds}
                accessibleOnly={accessibleOnly}
              />
            )}

            {activePOICategories.size > 0 && (
              <IndoorPOIOverlay
                pois={allPOIs}
                floor={selectedFloor}
                coordinateScale={coordinateScale}
                stageLayout={floorStageLayout}
                floorBounds={floorBounds}
                activeCategories={activePOICategories}
              />
            )}

            {selectedRoomMarkerPosition && !activeRoute && (
              <View
                testID="selected-room-marker"
                style={[
                  styles.roomMarker,
                  {
                    left: selectedRoomMarkerPosition.left,
                    top: selectedRoomMarkerPosition.top,
                  },
                ]}
              >
                <View style={styles.roomMarkerPulse} />
                <View style={styles.roomMarkerInner} />
              </View>
            )}
          </View>
        ) : (
          showNoMapMessage && (
            <View style={styles.emptyState}>
              <Text>No map available for {mapKey}</Text>
            </View>
          )
        )}
      </View>

      {activeRoute && (
        <IndoorDirectionsPanel
          route={activeRoute}
          onClose={() => {
            setActiveRoute(null);
          }}
        />
      )}


    </View>
  );
}
