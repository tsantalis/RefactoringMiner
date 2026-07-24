package com.conveyal.r5.labeling;

import com.conveyal.osmlib.Way;
import com.conveyal.r5.streets.EdgeStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TypeOfEdgeLabelerTest {

    public static final EnumSet<EdgeStore.EdgeFlag> PLATFORM = EnumSet
        .of(EdgeStore.EdgeFlag.PLATFORM,
            EdgeStore.EdgeFlag.ALLOWS_PEDESTRIAN,
            EdgeStore.EdgeFlag.ALLOWS_WHEELCHAIR,
            EdgeStore.EdgeFlag.LINKABLE);

    static TypeOfEdgeLabeler typeOfEdgeLabeler;
    static TraversalPermissionLabeler traversalPermissionLabeler;

    @BeforeAll
    public static void setUpClass() {
        typeOfEdgeLabeler = new TypeOfEdgeLabeler();
        traversalPermissionLabeler = new TestPermissionsLabeler();
    }

    public static Stream<Arguments> data() {
        EnumSet<EdgeStore.EdgeFlag> CAR = EnumSet.copyOf(TraversalPermissionLabelerTest.CAR);
        CAR.add(EdgeStore.EdgeFlag.LINKABLE);

        EnumSet<EdgeStore.EdgeFlag> CAR_CYCLEWAY = EnumSet.copyOf(TraversalPermissionLabelerTest.BICYCLE_AND_CAR);
        CAR_CYCLEWAY.add(EdgeStore.EdgeFlag.BIKE_PATH);
        CAR_CYCLEWAY.add(EdgeStore.EdgeFlag.LINKABLE);

        EnumSet<EdgeStore.EdgeFlag> PEDESTRIAN = EnumSet.copyOf(TraversalPermissionLabelerTest.PEDESTRIAN);
        PEDESTRIAN.add(EdgeStore.EdgeFlag.LINKABLE);

        EnumSet<EdgeStore.EdgeFlag> PEDESTRIAN_BIKE_CYCLEWAY = EnumSet.copyOf(TraversalPermissionLabelerTest.PEDESTRIAN_AND_BICYCLE);
        PEDESTRIAN_BIKE_CYCLEWAY.add(EdgeStore.EdgeFlag.BIKE_PATH);
        PEDESTRIAN_BIKE_CYCLEWAY.add(EdgeStore.EdgeFlag.LINKABLE);

        return Stream.of(
            Arguments.of(
                "sidewalk",
                "highway=cycleway;foot=designated;oneway=yes",
                EnumSet.of(EdgeStore.EdgeFlag.SIDEWALK,
                    EdgeStore.EdgeFlag.BIKE_PATH,
                    EdgeStore.EdgeFlag.ALLOWS_BIKE,
                    EdgeStore.EdgeFlag.ALLOWS_PEDESTRIAN,
                    EdgeStore.EdgeFlag.ALLOWS_WHEELCHAIR,
                    EdgeStore.EdgeFlag.LINKABLE),
                EnumSet.of(EdgeStore.EdgeFlag.SIDEWALK,
                    EdgeStore.EdgeFlag.ALLOWS_PEDESTRIAN,
                    EdgeStore.EdgeFlag.ALLOWS_WHEELCHAIR,
                    EdgeStore.EdgeFlag.LINKABLE)
            ),
            Arguments.of(
                "sidewalk",
                "highway=footway;footway=sidewalk",
                EnumSet.of(EdgeStore.EdgeFlag.SIDEWALK,
                    EdgeStore.EdgeFlag.ALLOWS_PEDESTRIAN,
                    EdgeStore.EdgeFlag.ALLOWS_WHEELCHAIR,
                    EdgeStore.EdgeFlag.LINKABLE),
                EnumSet.of(EdgeStore.EdgeFlag.SIDEWALK,
                    EdgeStore.EdgeFlag.ALLOWS_PEDESTRIAN,
                    EdgeStore.EdgeFlag.ALLOWS_WHEELCHAIR,
                    EdgeStore.EdgeFlag.LINKABLE)
            ),
            Arguments.of("platform", "highway=platform", PLATFORM, PLATFORM),
            Arguments.of("platform", "public_transport=platform", PLATFORM, PLATFORM),
            Arguments.of("platform", "railway=platform", PLATFORM, PLATFORM),
            Arguments.of("monodirectional cycleway", "highway=nobikenoped;cycleway=lane", CAR_CYCLEWAY, CAR_CYCLEWAY),
            Arguments.of("monodirectional cycleway", "highway=nobikenoped;cycleway=track", CAR_CYCLEWAY, CAR_CYCLEWAY),
            Arguments.of("directional cycleway", "highway=nobikenoped;cycleway:right=track", CAR_CYCLEWAY, CAR),
            Arguments.of("directional cycleway", "highway=nobikenoped;cycleway:left=track", CAR, CAR_CYCLEWAY),
            Arguments.of("directional cycleway", "highway=nobikenoped;cycleway:right=lane", CAR_CYCLEWAY, CAR),
            Arguments.of("directional cycleway", "highway=nobikenoped;cycleway:left=lane", CAR, CAR_CYCLEWAY),
            Arguments.of("directional cycleway", "highway=cycleway;oneway=true", PEDESTRIAN_BIKE_CYCLEWAY, PEDESTRIAN),
            Arguments.of("directional cycleway", "highway=cycleway", PEDESTRIAN_BIKE_CYCLEWAY, PEDESTRIAN_BIKE_CYCLEWAY),
            Arguments.of(
                "oneway streets with opposite lane cycleway",
                "highway=residential;cycleway=opposite_lane;oneway=yes",
                EnumSet.of(EdgeStore.EdgeFlag.ALLOWS_BIKE,
                    EdgeStore.EdgeFlag.ALLOWS_CAR,
                    EdgeStore.EdgeFlag.ALLOWS_PEDESTRIAN,
                    EdgeStore.EdgeFlag.ALLOWS_WHEELCHAIR,
                    EdgeStore.EdgeFlag.LINKABLE),
                PEDESTRIAN_BIKE_CYCLEWAY
            ),
            Arguments.of(
                "oneway reversed street with opposite lane cycleway",
                "highway=residential;cycleway=opposite_lane;oneway=-1",
                PEDESTRIAN_BIKE_CYCLEWAY,
                EnumSet.of(EdgeStore.EdgeFlag.ALLOWS_BIKE,
                    EdgeStore.EdgeFlag.ALLOWS_CAR,
                    EdgeStore.EdgeFlag.ALLOWS_PEDESTRIAN,
                    EdgeStore.EdgeFlag.ALLOWS_WHEELCHAIR,
                    EdgeStore.EdgeFlag.LINKABLE)
            )
        );


    }

    @ParameterizedTest(name="{0} with tags:{1}")
    @MethodSource("data")
    public void testParam(
            String name,
            String tags,
            EnumSet<EdgeStore.EdgeFlag> expectedForwardFlags,
            EnumSet<EdgeStore.EdgeFlag> expectedBackwardFlags
    ) throws Exception {
        Way osmWay = TraversalPermissionLabelerTest.makeOSMWayFromTags(tags);
        RoadPermission roadPermission = traversalPermissionLabeler.getPermissions(osmWay);
        typeOfEdgeLabeler.label(osmWay, roadPermission.forward, roadPermission.backward);
        assertEquals(expectedForwardFlags, roadPermission.forward, "Tags: " + tags);
        assertEquals(expectedBackwardFlags, roadPermission.backward, "Tags: " + tags);
    }



}