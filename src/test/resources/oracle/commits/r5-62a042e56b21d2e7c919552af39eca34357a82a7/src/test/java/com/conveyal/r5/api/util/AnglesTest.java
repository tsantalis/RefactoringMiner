package com.conveyal.r5.api.util;

import com.conveyal.r5.common.DirectionUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnglesTest {

    private final double delta = 0.01;
    
    public static Stream<Arguments> data() {
        return Stream.of (
            Arguments.of( -179 ," LINESTRING(15.6455 46.557611,15.645317478134713 46.55041978026114,15.645271855225554 46.54862197528108) "),
            Arguments.of( -165 ," LINESTRING(15.6455 46.557611,15.642793190059745 46.55066372524829,15.642116595847229 46.54892689658591) "),
            Arguments.of( -150 ," LINESTRING(15.6455 46.557611,15.640270775520289 46.55138215333488,15.638963656945254 46.549824904441955) "),
            Arguments.of( -135 ," LINESTRING(15.6455 46.557611,15.638104604080235 46.55252502697691,15.636255971680814 46.55125345926329) "),
            Arguments.of( -120 ," LINESTRING(15.6455 46.557611,15.636442278176375 46.55401448502616,15.634178035314534 46.55311524458698) "),
            Arguments.of( -105 ," LINESTRING(15.6455 46.557611,15.635397101100212 46.55574904718423,15.632871484715526 46.55528342001593) "),
            Arguments.of( -90 ," LINESTRING(15.6455 46.557611,15.635040350609943 46.557610523339264,15.632425438305495 46.5576102552176) "),
            Arguments.of( -75 ," LINESTRING(15.6455 46.557611,15.6353964079014 46.55947206335472,15.632870401592394 46.55993719020118) "),
            Arguments.of( -60 ," LINESTRING(15.6455 46.557611,15.6364410775208 46.56120679998273,15.63417615929021 46.56210563823941) "),
            Arguments.of( -45 ," LINESTRING(15.6455 46.557611,15.638103217682579 46.56269649636235,15.636253805434473 46.5639677959543) "),
            Arguments.of( -30 ," LINESTRING(15.6455 46.557611,15.640269574864686 46.56383960833476,15.638961780920855 46.565396723166835) "),
            Arguments.of( -15 ," LINESTRING(15.6455 46.557611,15.642792496860903 46.56455821089129,15.642115512724018 46.56629500363218) "),
            Arguments.of( 0 ," LINESTRING(15.6455 46.557611,15.645500000000002 46.564803315018125,15.645500000000002 46.56660139377266) "),
            Arguments.of( 15 ," LINESTRING(15.6455 46.557611,15.648207503139101 46.56455821089129,15.648884487275986 46.56629500363218) "),
            Arguments.of( 30 ," LINESTRING(15.6455 46.557611,15.650730425135318 46.56383960833476,15.652038219079149 46.565396723166835) "),
            Arguments.of( 45 ," LINESTRING(15.6455 46.557611,15.652896782317425 46.56269649636235,15.654746194565531 46.5639677959543) "),
            Arguments.of( 60 ," LINESTRING(15.6455 46.557611,15.654558922479204 46.56120679998273,15.656823840709794 46.56210563823941) "),
            Arguments.of( 75 ," LINESTRING(15.6455 46.557611,15.655603592098604 46.55947206335472,15.65812959840761 46.55993719020118) "),
            Arguments.of( 90 ," LINESTRING(15.6455 46.557611,15.655959649390061 46.557610523339264,15.65857456169451 46.5576102552176) "),
            Arguments.of( 105 ," LINESTRING(15.6455 46.557611,15.655602898899792 46.55574904718423,15.658128515284478 46.55528342001593) "),
            Arguments.of( 120 ," LINESTRING(15.6455 46.557611,15.654557721823629 46.55401448502616,15.656821964685472 46.55311524458698) "),
            Arguments.of( 135 ," LINESTRING(15.6455 46.557611,15.652895395919769 46.55252502697691,15.65474402831919 46.55125345926329) "),
            Arguments.of( 150 ," LINESTRING(15.6455 46.557611,15.650729224479715 46.55138215333488,15.65203634305475 46.549824904441955) "),
            Arguments.of( 165 ," LINESTRING(15.6455 46.557611,15.648206809940259 46.55066372524829,15.648883404152777 46.54892689658591) "),
            Arguments.of( 180 ," LINESTRING(15.6455 46.557611,15.645500000000002 46.550418684981885,15.645500000000002 46.54862060622736) ")
        );
    }

    @ParameterizedTest(name="angle {0}")
    @MethodSource("data")
    public void testAngles (int angle, String wktlineString) throws ParseException {
        WKTReader wktReader = new WKTReader();
        LineString lineString = (LineString) wktReader.read(wktlineString);
        LineString reverseLine = (LineString) lineString.reverse();

        assertEquals(angle, Math.toDegrees(DirectionUtils.getFirstAngle(lineString)), delta);
        // assertEquals(angle, 180+DirectionUtils.getLastAngle(reverseLine), delta);
        assertEquals(angle, Math.toDegrees(DirectionUtils.getLastAngle(lineString)), delta);
    }

}
