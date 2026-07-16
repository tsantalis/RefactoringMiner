import React, {useState, useRef, useEffect, useMemo} from 'react';
import {View, Text, Pressable, StyleSheet, Animated} from 'react-native';
import {
    getDirections,
    TransportMode,
    DirectionsResponse,
    DirectionsRequest,
    Coordinate,
    Provider,
    TimeFilterMode,
} from '@/services/maps/directions-api-adapter';
import {TimePickerModal} from './TimePickerModal';
import {validateCampusRoute} from '@/services/maps/route-validator';
import {formatISOToTime, getCurrentTimeISO} from '@/utils/timeFormatter';
import {useShuttleSchedule} from '@/hooks/use-shuttle-schedule';
import {useShuttleRouting} from '@/hooks/use-shuttle-routing';
import {ShuttleScheduleModal} from '@/components/ui/shuttle-schedule-modal';
import {ShuttleScheduleSection} from '@/components/ui/shuttle-schedule-section';

type TransportModeLabel = 'Drive' | 'Walk' | 'Transit' | 'Shuttle';

interface NavigationBottomProps {
    origin: Coordinate;
    destination: Coordinate;
    onDirectionsChange?: (directions: DirectionsResponse) => void;
    onStartPress?: () => void;
    onModeChange?: (mode: TransportModeLabel) => void;
    onTimeFilterChange?: (timeFilter: string, timeFilterMode: TimeFilterMode) => void;
    initialMode?: TransportModeLabel;
}

const MODES: TransportModeLabel[] = ['Drive', 'Walk', 'Transit', 'Shuttle'];

const MODE_META: Record<TransportModeLabel, {label: string; indicatorColor: string}> = {
    Drive: {label: 'Drive', indicatorColor: '#e5a712'},
    Walk: {label: 'Walk', indicatorColor: '#e5a712'},
    Transit: {label: 'Transit', indicatorColor: '#e5a712'},
    Shuttle: {label: 'Shuttle', indicatorColor: '#f4b742'},
};

const mapUiModeToTransportMode = (mode: TransportModeLabel) => {
    switch (mode) {
        case 'Drive':
            return TransportMode.DRIVING;
        case 'Walk':
            return TransportMode.WALKING;
        case 'Transit':
            return TransportMode.TRANSIT;
        case 'Shuttle':
            return TransportMode.TRANSIT;
        default:
            return TransportMode.WALKING;
    }
};

const mapUiModeToProvider = (mode: TransportModeLabel) => {
    return mode === 'Transit' ? Provider.GOOGLE_MAPS : Provider.MAPBOX;
};

const formatDistance = (meters?: number) => {
    if (meters == null) return '—';
    return meters >= 1000 ? `${(meters / 1000).toFixed(1)} km` : `${Math.round(meters)} m`;
};

const formatDuration = (seconds?: number) => {
    if (seconds == null) return '— min';
    const totalMinutes = Math.round(seconds / 60);
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    if (hours) {
        const paddedMinutes = String(minutes).padStart(2, '0');
        return `${hours}:${paddedMinutes} hr`;
    }
    return `${minutes} min`;
};

const formatDepartureTime = (date: Date) =>
    date.toLocaleTimeString(undefined, {hour: 'numeric', minute: '2-digit'});

/**
 * Formats a relative-minutes value for the "in X min" label next to each departure.
 * Handles negative values (custom filter in the past relative to wall clock) gracefully.
 */
const formatMinutesUntil = (minutes: number) => {
    if (minutes <= 0) return 'Now';
    if (minutes < 60) return `in ${minutes} min`;
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return m > 0 ? `in ${h} hr ${m} min` : `in ${h} hr`;
};

// In arrive-by mode, don't suggest shuttle options that would arrive far too early.
// If the best shuttle would get you there way before the requested time, we prefer
// showing the Transit fallback instead of misleading "available" shuttle rows.
const MAX_ARRIVE_EARLY_BUFFER_MINUTES = 60;

/**
 * When a custom timeFilter is active (more than 60 s away from real now), display
 * labels relative to the filter anchor so they are meaningful to the user.
 * When the filter is essentially "now", use the wall-clock-relative minutesUntil
 * so the labels reflect real time remaining.
 */
const pickDisplayMinutes = (
    item: {minutesUntil: number; minutesFromFilter: number},
    isCustomFilter: boolean,
): number => (isCustomFilter ? item.minutesFromFilter : item.minutesUntil);

function buildSimpleArrivalDepartureLabel(
    durationSeconds: number,
    timeFilter: string,
    timeFilterMode: TimeFilterMode,
): string {
    const baseTime = new Date(timeFilter);
    const durationMs = durationSeconds * 1000;
    if (timeFilterMode === 'depart') {
        const arrivalTime = new Date(baseTime.getTime() + durationMs);
        return `Arrive by ${formatISOToTime(arrivalTime.toISOString())}`;
    }
    const departureTime = new Date(baseTime.getTime() - durationMs);
    return `Depart at ${formatISOToTime(departureTime.toISOString())}`;
}

function calculateTransitArrivalDepartureLabel(
    directions: DirectionsResponse,
    timeFilter: string,
    timeFilterMode: TimeFilterMode,
): string {
    const steps = directions.steps;
    if (!steps || steps.length === 0) return '';

    const firstTransitIndex = steps.findIndex(step => step.transitDetails);
    if (firstTransitIndex === -1) {
        return buildSimpleArrivalDepartureLabel(directions.durationSeconds, timeFilter, timeFilterMode);
    }

    let lastTransitIndex = firstTransitIndex;
    for (let i = steps.length - 1; i >= 0; i--) {
        if (steps[i].transitDetails) {
            lastTransitIndex = i;
            break;
        }
    }

    if (timeFilterMode === 'depart') {
        const departureTimeStr = steps[firstTransitIndex].transitDetails?.departureTime;
        if (!departureTimeStr) return '';

        const firstTransitTime = new Date(departureTimeStr);
        let initialWalkingDuration = 0;
        for (let i = 0; i < firstTransitIndex; i++) {
            initialWalkingDuration += steps[i].duration;
        }

        const durationWithoutInitialWalking = directions.durationSeconds - initialWalkingDuration;
        const arrivalTime = new Date(firstTransitTime.getTime() + durationWithoutInitialWalking * 1000);
        return `Arrive by ${formatISOToTime(arrivalTime.toISOString())}`;
    }

    const arrivalTimeStr = steps[lastTransitIndex].transitDetails?.arrivalTime;
    if (!arrivalTimeStr) return '';

    const lastTransitTime = new Date(arrivalTimeStr);
    let finalWalkingDuration = 0;
    for (let i = steps.length - 1; i > lastTransitIndex; i--) {
        finalWalkingDuration += steps[i].duration;
    }

    const durationWithoutFinalWalking = directions.durationSeconds - finalWalkingDuration;
    const departureTime = new Date(lastTransitTime.getTime() - durationWithoutFinalWalking * 1000);
    return `Depart at ${formatISOToTime(departureTime.toISOString())}`;
}

type ShuttleLegMetrics = {
    totalDistanceMeters: number;
    walkMinutes: number;
    shuttleMinutes: number;
    walkFromMinutes: number;
};

type ShuttleMetrics = {
    totalDurationSeconds: number;
    totalDistanceMeters: number;
    arrivalLabel: string;
    walkMinutes: number;
    shuttleMinutes: number;
    walkFromMinutes: number;
    totalTripMinutes: number;
};

type ShuttleScheduleContextLike = {
    schedule?: unknown;
    isNextServiceDay?: boolean;
} | null;

function computeDisplayState({
    selectedMode,
    isSameCampus,
    shuttleScheduleContext,
    reachableDeparturesLength,
    shuttleMetrics,
    directions,
    arriveLeaveDetails,
    showLastShuttleWarning,
}: {
    selectedMode: TransportModeLabel;
    isSameCampus: boolean;
    shuttleScheduleContext: ShuttleScheduleContextLike;
    reachableDeparturesLength: number;
    shuttleMetrics: ShuttleMetrics | null;
    directions: DirectionsResponse | null;
    arriveLeaveDetails: string;
    showLastShuttleWarning: boolean;
}) {
    const isShuttleMode = selectedMode === 'Shuttle';

    const hideMetricsRow =
        isShuttleMode &&
        (isSameCampus ||
            !shuttleScheduleContext?.schedule ||
            !!shuttleScheduleContext?.isNextServiceDay ||
            reachableDeparturesLength === 0);

    const showInlineShuttleMetrics =
        isShuttleMode &&
        hideMetricsRow &&
        !!shuttleMetrics &&
        reachableDeparturesLength > 0 &&
        !shuttleScheduleContext?.isNextServiceDay;

    const activeDurationSeconds =
        isShuttleMode && shuttleMetrics
            ? shuttleMetrics.totalDurationSeconds
            : directions?.durationSeconds;
    const activeDistanceMeters =
        isShuttleMode && shuttleMetrics
            ? shuttleMetrics.totalDistanceMeters
            : directions?.distanceMeters;
    const activeArrivalTime = isShuttleMode && shuttleMetrics ? shuttleMetrics.arrivalLabel : arriveLeaveDetails;
    const displayedArrivalText = isShuttleMode ? activeArrivalTime : arriveLeaveDetails;
    const showShuttleLastWarningInline = isShuttleMode && showLastShuttleWarning;

    return {
        hideMetricsRow,
        showInlineShuttleMetrics,
        activeDurationSeconds,
        activeDistanceMeters,
        displayedArrivalText,
        showShuttleLastWarningInline,
    };
}

export function NavigationBottom({
    origin,
    destination,
    onDirectionsChange,
    onStartPress,
    onModeChange,
    onTimeFilterChange,
    initialMode = 'Drive',
}: NavigationBottomProps) {
    const [selectedMode, setSelectedMode] = useState<TransportModeLabel>(initialMode);
    const [directions, setDirections] = useState<DirectionsResponse | null>(null);
    const [showScheduleModal, setShowScheduleModal] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [timeFilter, setTimeFilter] = useState(getCurrentTimeISO());
    const [timePickerVisible, setTimePickerVisible] = useState(false);
    const [timeFilterMode, setTimeFilterMode] = useState<TimeFilterMode>('depart');
    const slideAnim = useRef(new Animated.Value(MODES.indexOf(initialMode))).current;
    const [arriveLeaveDetails, setArriveLeaveDetails] = useState<string>('');

    // True when the user has picked a custom time meaningfully different from right now.
    const isCustomFilter = useMemo(
        () => Math.abs(new Date(timeFilter).getTime() - Date.now()) > 60_000,
        [timeFilter],
    );

    useEffect(() => {
        Animated.spring(slideAnim, {
            toValue: MODES.indexOf(selectedMode),
            useNativeDriver: false,
            speed: 12,
            bounciness: 8,
        }).start();
    }, [selectedMode, slideAnim]);

    useEffect(() => {
        let active = true;
        let timeoutId: ReturnType<typeof setTimeout>;

        const fetchDirections = async () => {
            setIsLoading(true);
            setDirections(null);
            setShowScheduleModal(false);

            if (selectedMode === 'Shuttle') {
                setIsLoading(false);
                return;
            }

            try {
                const request: DirectionsRequest = {
                    origin,
                    destination,
                    transportMode: mapUiModeToTransportMode(selectedMode),
                    provider: mapUiModeToProvider(selectedMode),
                    timeFilterMode,
                    timeFilter,
                };
                const directionsResponse = await getDirections(request);
                if (!active) return;
                setDirections(directionsResponse);
                onDirectionsChange?.(directionsResponse);
            } catch (err) {
                if (!active) return;
                setDirections(null);
                console.warn('Failed to load directions', err);
            } finally {
                if (active) setIsLoading(false);
            }
        };

        timeoutId = setTimeout(() => {
            fetchDirections();
        }, 1000);

        return () => {
            active = false;
            clearTimeout(timeoutId);
        };
    }, [
        origin.longitude,
        origin.latitude,
        destination.longitude,
        destination.latitude,
        selectedMode,
        timeFilter,
        timeFilterMode,
        onDirectionsChange,
    ]);

    // Calculate arrival/departure details for non-shuttle modes.
    useEffect(() => {
        if (!directions) {
            setArriveLeaveDetails('');
            return;
        }

        try {
            let details = '';
            if (selectedMode === 'Transit') {
                details = calculateTransitArrivalDepartureLabel(directions, timeFilter, timeFilterMode);
            } else if (selectedMode === 'Walk' || selectedMode === 'Drive') {
                details = buildSimpleArrivalDepartureLabel(directions.durationSeconds, timeFilter, timeFilterMode);
            }
            setArriveLeaveDetails(details);
        } catch (err) {
            console.warn('Failed to calculate arrival/departure time', err);
            setArriveLeaveDetails('');
        }
    }, [directions, timeFilter, timeFilterMode, selectedMode]);

    // Detect whether both endpoints sit on the same campus.
    const isSameCampus = useMemo((): boolean => {
        const result = validateCampusRoute({
            origin: {type: 'coordinate', longitude: origin.longitude, latitude: origin.latitude},
            destination: {type: 'coordinate', longitude: destination.longitude, latitude: destination.latitude},
        });
        if (!result.valid) return true;
        return !result.route.isInterCampus;
    }, [origin.longitude, origin.latitude, destination.longitude, destination.latitude]);

    const handleModeChange = (mode: TransportModeLabel) => {
        setSelectedMode(mode);
        onModeChange?.(mode);
    };

    const handleTimeFilterChange = (time: string, mode: TimeFilterMode) => {
        setTimeFilter(time);
        setTimeFilterMode(mode);
        setTimePickerVisible(false);
        onTimeFilterChange?.(time, mode);
    };

    const indicatorWidth = slideAnim.interpolate({
        inputRange: [0, 3],
        outputRange: ['25%', '25%'],
    });

    const indicatorLeft = slideAnim.interpolate({
        inputRange: [0, 1, 2, 3],
        outputRange: ['0%', '25%', '50%', '75%'],
    });

    const shuttleScheduleContext = useShuttleSchedule({
        enabled: selectedMode === 'Shuttle',
        origin,
        destination,
        timeFilter,
        timeFilterMode,
    });

    const shuttleRouting = useShuttleRouting({
        enabled: selectedMode === 'Shuttle' && !isSameCampus,
        origin,
        destination,
        timeFilter,
        timeFilterMode,
    });

    // ── Leg-based routing metrics (distance / walk segments) ──────────────────
    // Computed independently of reachableDepartures to avoid a circular dep.
    const shuttleLegMetrics = useMemo<ShuttleLegMetrics | null>(() => {
        if (selectedMode !== 'Shuttle') return null;
        const {walkToStop, shuttleLeg, walkFromStop} = shuttleRouting;
        if (!walkToStop || !shuttleLeg || !walkFromStop) return null;

        const totalDistanceMeters =
            walkToStop.distanceMeters + shuttleLeg.distanceMeters + walkFromStop.distanceMeters;
        const walkMinutes = Math.ceil(walkToStop.durationSeconds / 60);
        const shuttleMinutes = Math.ceil(shuttleLeg.durationSeconds / 60);
        const walkFromMinutes = Math.ceil(walkFromStop.durationSeconds / 60);

        return {totalDistanceMeters, walkMinutes, shuttleMinutes, walkFromMinutes};
    }, [selectedMode, shuttleRouting]);

    // ── Departures filtered to only those reachable given the walk-to-stop time ──
    // Uses minutesFromFilter (relative to timeFilter) so the filter respects the
    // selected time. Falls back to a 5-min walk estimate while routing loads.
    const reachableDepartures = useMemo(() => {
        if (!shuttleScheduleContext?.departures) return [];

        // Next-service-day departures are never filtered by walk time —
        // the user just sees the full upcoming list.
        if (shuttleScheduleContext.isNextServiceDay) {
            return shuttleScheduleContext.departures;
        }

        // Use actual walk minutes once routing resolves; fall back to 5 min estimate.
        const walkMinutes = shuttleLegMetrics?.walkMinutes ?? 5;

        if (timeFilterMode === 'arrive') {
            // Valid departure: shuttleDep + shuttleRide + walkFrom <= arriveBy
            // i.e. minutesFromFilter <= -(shuttleMinutes + walkFromMinutes)
            const postBoardMinutes =
                (shuttleLegMetrics?.shuttleMinutes ?? 20) + (shuttleLegMetrics?.walkFromMinutes ?? 5);
            const earliestAcceptableDepartureFromFilter =
                -(postBoardMinutes + MAX_ARRIVE_EARLY_BUFFER_MINUTES);
            return shuttleScheduleContext.departures.filter(
                (item) =>
                    item.minutesFromFilter <= -postBoardMinutes &&
                    item.minutesFromFilter >= earliestAcceptableDepartureFromFilter,
            );
        }

        // Depart mode: departure must be at least walkMinutes away from filter anchor.
        return shuttleScheduleContext.departures.filter(
            (item) => item.minutesFromFilter >= walkMinutes,
        );
    }, [shuttleScheduleContext, shuttleLegMetrics, timeFilterMode]);

    const showLastShuttleWarning = useMemo(() => {
        if (selectedMode !== 'Shuttle') return false;
        if (!shuttleScheduleContext?.departureTimes?.length) return false;
        if (shuttleScheduleContext.isNextServiceDay) return false;
        if (reachableDepartures.length === 0) return false;

        const suggestedDeparture =
            timeFilterMode === 'arrive'
                ? reachableDepartures[reachableDepartures.length - 1]
                : reachableDepartures[0];
        const lastScheduledDepartureTime =
            shuttleScheduleContext.departureTimes[shuttleScheduleContext.departureTimes.length - 1];

        return suggestedDeparture.time === lastScheduledDepartureTime;
    }, [selectedMode, shuttleScheduleContext, reachableDepartures, timeFilterMode]);

    // ── Full shuttle metrics including arrival label ───────────────────────────
    // Built AFTER reachableDepartures so the arrival label reflects the actual
    // next catchable shuttle rather than raw chained leg durations.
    //
    // Returns null when the shuttle isn't running today — showing "18 min /
    // Arrive by 9:31 AM" anchored to Monday's first departure at 7 AM Saturday
    // is technically correct math but deeply misleading. In that state the card
    // shows only the Transit redirect + the next-day schedule list.
    const shuttleMetrics = useMemo<ShuttleMetrics | null>(() => {
        if (selectedMode !== 'Shuttle' || !shuttleLegMetrics) return null;
        if (shuttleScheduleContext?.isNextServiceDay) return null;
        const {totalDistanceMeters, walkMinutes, shuttleMinutes, walkFromMinutes} = shuttleLegMetrics;
        const totalTripMinutes = walkMinutes + shuttleMinutes + walkFromMinutes;
        const totalDurationSeconds = totalTripMinutes * 60;

        let arrivalLabel = '';

        if (timeFilterMode === 'depart') {
            if (reachableDepartures.length > 0) {
                // Base arrival on the first actually-catchable shuttle departure.
                const nextDep = reachableDepartures[0];
                const arrivalMs =
                    nextDep.departureDate.getTime() +
                    (shuttleMinutes + walkFromMinutes) * 60_000;
                arrivalLabel = `Arrive by ${new Date(arrivalMs).toLocaleTimeString(undefined, {
                    hour: 'numeric',
                    minute: '2-digit',
                })}`;
            }
        } else if (reachableDepartures.length > 0) {
            // Arrive mode: report when the user needs to leave to board the latest valid shuttle.
            const lastDep = reachableDepartures[reachableDepartures.length - 1];
            const departMs = lastDep.departureDate.getTime() - walkMinutes * 60_000;
            arrivalLabel = `Depart at ${new Date(departMs).toLocaleTimeString(undefined, {
                hour: 'numeric',
                minute: '2-digit',
            })}`;
        }

        return {totalDurationSeconds, totalDistanceMeters, arrivalLabel, walkMinutes, shuttleMinutes, walkFromMinutes, totalTripMinutes};
    }, [selectedMode, shuttleLegMetrics, reachableDepartures, timeFilterMode]);

    const {
        hideMetricsRow,
        showInlineShuttleMetrics,
        activeDurationSeconds,
        activeDistanceMeters,
        displayedArrivalText,
        showShuttleLastWarningInline,
    } = useMemo(
        () =>
            computeDisplayState({
                selectedMode,
                isSameCampus,
                shuttleScheduleContext,
                reachableDeparturesLength: reachableDepartures.length,
                shuttleMetrics,
                directions,
                arriveLeaveDetails,
                showLastShuttleWarning,
            }),
        [
            selectedMode,
            isSameCampus,
            shuttleScheduleContext,
            reachableDepartures.length,
            shuttleMetrics,
            directions,
            arriveLeaveDetails,
            showLastShuttleWarning,
        ],
    );

    const durationText = formatDuration(activeDurationSeconds);
    const distanceText = formatDistance(activeDistanceMeters);
    const durationParts = useMemo(() => durationText.split(' '), [durationText]);
    const durationValue = durationParts[0] ?? durationText;
    const durationUnit = durationParts[1] ?? 'min';

    const displayDepartureTime = formatISOToTime(timeFilter);
    const timeModeLabel = timeFilterMode === 'depart' ? 'Depart at' : 'Arrive by';

    const formatTimeLabel = (time: string, baseDate: Date) => {
        const [hoursStr, minutesStr] = time.split(':');
        const hours = Number(hoursStr);
        const minutes = Number(minutesStr);
        const date = new Date(baseDate.getFullYear(), baseDate.getMonth(), baseDate.getDate(), hours, minutes, 0, 0);
        return formatDepartureTime(date);
    };

    return (
        <>
            <View style={styles.navCard}>
                <View style={styles.navHeaderRow}>
                    <Text style={styles.navHeaderText}>{selectedMode}</Text>
                    <Pressable
                        style={styles.departAtButton}
                        onPress={() => setTimePickerVisible(true)}
                    >
                        <Text style={styles.departAtButtonText}>{timeModeLabel}: {displayDepartureTime}</Text>
                    </Pressable>
                </View>

                <View style={styles.modeBarContainer}>
                    <Animated.View
                        style={[
                            styles.modeIndicator,
                            {
                                width: indicatorWidth,
                                left: indicatorLeft,
                                backgroundColor: MODE_META[selectedMode].indicatorColor,
                            },
                        ]}
                    />
                    {MODES.map((mode, index) => (
                        <Pressable
                            key={mode}
                            onPress={() => handleModeChange(mode)}
                            style={[styles.modeOption, index !== MODES.length - 1 && styles.modeBorder]}
                        >
                            <Text
                                style={[
                                    styles.modeOptionText,
                                    selectedMode === mode && styles.modeOptionTextActive,
                                ]}
                            >
                                {MODE_META[mode].label}
                            </Text>
                        </Pressable>
                    ))}
                </View>

                {!hideMetricsRow && (
                    <View style={styles.metricsRow}>
                        <View style={[styles.metricCell, styles.durationCell]}>
                            <Text style={styles.durationValue}>{durationValue}</Text>
                            <Text style={styles.durationUnit}>{durationUnit}</Text>
                        </View>

                        <View style={[styles.metricCell, styles.middleCell]}>
                            <Text style={styles.arrivalText} numberOfLines={1} ellipsizeMode="tail">
                                {displayedArrivalText}
                            </Text>
                            <View style={styles.distanceRow}>
                                <Text style={styles.distanceText}>{distanceText}</Text>
                                {showShuttleLastWarningInline ? (
                                    <Text style={styles.lastShuttleInlineWarning}>Last shuttle for the day</Text>
                                ) : null}
                            </View>
                        </View>

                        <View style={[styles.metricCell, styles.startCell]}>
                            <Pressable style={styles.startButton} onPress={onStartPress}>
                                <Text style={styles.startButtonText}>Start</Text>
                            </Pressable>
                        </View>
                    </View>
                )}

                {selectedMode === 'Shuttle' && (
                    <ShuttleScheduleSection
                        directionLabel={shuttleScheduleContext?.directionLabel ?? 'Shuttle'}
                        validPeriod={shuttleScheduleContext?.schedule?.validPeriod}
                        hasSchedule={!!shuttleScheduleContext?.schedule}
                        showNextServiceLabel={!!shuttleScheduleContext?.showNextServiceLabel}
                        nextServiceLabel={
                            shuttleScheduleContext?.showNextServiceLabel
                                ? shuttleScheduleContext?.serviceDate.toLocaleDateString(undefined, {weekday: 'long'})
                                : undefined
                        }
                        departures={
                            reachableDepartures.slice(0, 3).map((item) => ({
                                key: `${shuttleScheduleContext?.directionLabel}-${item.time}`,
                                timeLabel: formatDepartureTime(item.departureDate),
                                // Use filter-relative minutes when a custom time is selected so
                                // labels like "in 15 min" make sense relative to that chosen time.
                                etaLabel: shuttleScheduleContext?.isNextServiceDay
                                    ? `on ${shuttleScheduleContext.serviceDate.toLocaleDateString(undefined, {weekday: 'long'})}`
                                    : formatMinutesUntil(pickDisplayMinutes(item, isCustomFilter)),
                            }))
                        }
                        showSeeMoreButton={
                            !!shuttleScheduleContext?.showSeeMoreButton ||
                            reachableDepartures.length > 3
                        }
                        onOpenModal={() => setShowScheduleModal(true)}
                        onFallbackPress={() => handleModeChange('Transit')}
                        showSameCampusRedirect={isSameCampus}
                        onSameCampusRedirect={() => handleModeChange('Walk')}
                        noTopSpacing={hideMetricsRow}
                        inlineMetrics={
                            showInlineShuttleMetrics && shuttleMetrics
                                ? {
                                      durationText: formatDuration(shuttleMetrics.totalDurationSeconds),
                                      distanceText: formatDistance(shuttleMetrics.totalDistanceMeters),
                                      arrivalLabel: shuttleMetrics.arrivalLabel,
                                  }
                                : null
                        }
                    />
                )}

                <ShuttleScheduleModal
                    visible={showScheduleModal}
                    directionLabel={shuttleScheduleContext?.directionLabel ?? 'Shuttle'}
                    serviceDateLabel={
                        shuttleScheduleContext?.schedule
                            ? shuttleScheduleContext.serviceDate.toLocaleDateString(undefined, {
                                  weekday: 'long',
                                  month: 'short',
                                  day: 'numeric',
                              })
                            : undefined
                    }
                    times={
                        shuttleScheduleContext?.departureTimes?.map((time) =>
                            formatTimeLabel(time, shuttleScheduleContext.serviceDate),
                        ) ?? []
                    }
                    onClose={() => setShowScheduleModal(false)}
                />
            </View>

            <TimePickerModal
                visible={timePickerVisible}
                initialTime={timeFilter}
                initialMode={timeFilterMode}
                onConfirm={handleTimeFilterChange}
                onCancel={() => setTimePickerVisible(false)}
            />
        </>
    );
}

const styles = StyleSheet.create({
    navCard: {
        position: 'absolute',
        left: 12,
        right: 12,
        bottom: 20,
        padding: 14,
        borderRadius: 16,
        backgroundColor: '#FFFFFF',
        shadowColor: '#000',
        shadowOpacity: 0.15,
        shadowOffset: {width: 0, height: 2},
        shadowRadius: 6,
        elevation: 6,
    },
    navHeaderRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 10,
    },
    navHeaderText: {fontSize: 16, fontWeight: '700', color: '#111827'},
    navArrival: {fontSize: 12, color: '#10B981', fontWeight: '600'},
    departAtButton: {
        backgroundColor: '#F5F3FF',
        borderRadius: 8,
        paddingVertical: 8,
        paddingHorizontal: 12,
        alignItems: 'center',
        justifyContent: 'center',
    },
    departAtButtonText: {color: '#111827', fontWeight: '600', fontSize: 12},
    modeBarContainer: {
        position: 'relative',
        flexDirection: 'row',
        backgroundColor: '#9d1e30',
        borderRadius: 12,
        overflow: 'hidden',
        marginBottom: 12,
        height: 44,
    },
    modeIndicator: {
        position: 'absolute',
        top: 0,
        bottom: 0,
        backgroundColor: '#e5a712',
        borderRadius: 12,
    },
    modeOption: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    modeBorder: {
        borderRightWidth: 1,
        borderRightColor: 'rgba(255, 255, 255, 0.2)',
    },
    modeOptionText: {fontSize: 13, fontWeight: '600', color: '#FFFFFF'},
    modeOptionTextActive: {color: '#111827'},
    metricsRow: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 12,
        marginTop: 4,
    },
    metricCell: {
        justifyContent: 'center',
    },
    durationCell: {
        flex: 1,
        alignItems: 'center',
    },
    middleCell: {
        flex: 3,
        alignItems: 'flex-start',
    },
    startCell: {
        flex: 1,
        alignItems: 'flex-end',
    },
    durationValue: {fontSize: 20, fontWeight: '700', color: '#10B981'},
    durationUnit: {fontSize: 12, color: '#10B981', fontWeight: '600'},
    arrivalText: {fontSize: 13, color: '#111827', fontWeight: '600'},
    distanceRow: {flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 4},
    distanceText: {fontSize: 12, color: '#6B7280'},
    lastShuttleInlineWarning: {fontSize: 11, fontWeight: '700', color: '#B91C1C'},
    startButton: {
        backgroundColor: '#9d1e30',
        borderRadius: 12,
        paddingVertical: 10,
        paddingHorizontal: 18,
        alignItems: 'center',
        justifyContent: 'center',
        minWidth: 90,
    },
    startButtonText: {color: '#FFFFFF', fontWeight: '700', fontSize: 14},
});
