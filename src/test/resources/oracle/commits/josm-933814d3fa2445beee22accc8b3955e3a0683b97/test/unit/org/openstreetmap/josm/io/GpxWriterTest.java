// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.TestUtils.assertEqualsNewline;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxData.XMLNamespace;
import org.openstreetmap.josm.data.gpx.GpxExtensionCollection;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests the {@link GpxWriter}.
 */
public class GpxWriterTest {

    /**
     * Setup rule
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static void testSingleWaypoint(Consumer<WayPoint> consumer, String atts) throws IOException {
        GpxData gpx = new GpxData();
        WayPoint waypoint = new WayPoint(LatLon.ZERO);
        waypoint.setInstant(Instant.parse("2019-06-08T08:23:12.300Z"));
        consumer.accept(waypoint);
        gpx.addWaypoint(waypoint);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GpxWriter writer = new GpxWriter(baos)) {
            writer.write(gpx);
        }
        assertEqualsNewline("<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<gpx version=\"1.1\" creator=\"JOSM GPX export\" xmlns=\"http://www.topografix.com/GPX/1/1\"\n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n" +
                "  <metadata>\n" +
                "    <bounds minlat=\"0.0\" minlon=\"0.0\" maxlat=\"0.0\" maxlon=\"0.0\"/>\n" +
                "  </metadata>\n" +
                "  <wpt lat=\"0.0\" lon=\"0.0\">\n" +
                atts +
                "  </wpt>\n" +
                "</gpx>", baos.toString());
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/16550">#16550</a>
     * @throws IOException never
     */
    @Test
    void testTicket16550() throws IOException {
        // Checks that time stored as date is correctly written into XML timestamp
        testSingleWaypoint(
                w -> w.put(GpxConstants.PT_TIME, LocalDate.of(2018, Month.AUGUST, 2).atStartOfDay(ZoneOffset.UTC).toInstant()),
                "    <time>2018-08-02T00:00:00Z</time>\n");
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/16725">#16725</a>
     * @throws IOException never
     */
    @Test
    void testTicket16725() throws IOException {
        // Checks that sat, hdop, pdop, vdop are correctly exported
        testSingleWaypoint(
                w -> {
                    w.put(GpxConstants.PT_SAT, 16);
                    w.put(GpxConstants.PT_HDOP, 0.7);
                    w.put(GpxConstants.PT_VDOP, 0.9);
                    w.put(GpxConstants.PT_PDOP, 1.2);
                },
                "    <time>2019-06-08T08:23:12.300Z</time>\n" +
                "    <sat>16</sat>\n" +
                "    <hdop>0.7</hdop>\n" +
                "    <vdop>0.9</vdop>\n" +
                "    <pdop>1.2</pdop>\n");
    }

    /**
     * Tests if extensions are written correctly.
     * @throws IOException in case of I/O error
     */
    @Test
    void testExtensions() throws IOException {
        GpxData data = new GpxData();
        // only namespace, no location printed
        data.getNamespaces().add(new XMLNamespace("test", "http://example.com/testURI"));
        // printed
        data.getNamespaces().add(new XMLNamespace("knownprefix", "http://example.com/URI", "http://example.com/location.xsd"));
        // NOT printed
        data.getNamespaces().add(new XMLNamespace("notpresent", "http://example.com/notpresent", "http://example.com/notpresent.xsd"));

        GpxExtensionCollection exts = data.getExtensions();
        data.fromServer = true; //printed
        data.getLayerPrefs().put("foo", "bar"); //printed depending on writer config
        exts.add("knownprefix", "foo", "bar"); //printed
        exts.add("unknownprefix", "foo", "bar"); //NOT printed

        WayPoint wpt = new WayPoint(LatLon.ZERO);
        wpt.getExtensions().add("test", "foo", "extension of a waypoint"); //printed

        GpxTrackSegment seg = new GpxTrackSegment(Arrays.asList(wpt));
        seg.getExtensions().add("test", "foo", "extension of a segment"); //printed

        GpxTrack trk = new GpxTrack(Arrays.asList(seg), new HashMap<>());

        trk.setColor(Color.RED); //printed depending on writer configuration

        data.addTrack(trk);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GpxWriter writer = new GpxWriter(baos);

        // CHECKSTYLE.OFF: LineLength

        writer.write(data);
        assertEqualsNewline("<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<gpx version=\"1.1\" creator=\"JOSM GPX export\" xmlns=\"http://www.topografix.com/GPX/1/1\"\n" +
                "    xmlns:knownprefix=\"http://example.com/URI\"\n" +
                "    xmlns:josm=\"http://josm.openstreetmap.de/gpx-extensions-1.1\"\n" +
                "    xmlns:gpxd=\"http://josm.openstreetmap.de/gpx-drawing-extensions-1.0\"\n" +
                "    xmlns:test=\"http://example.com/testURI\"\n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://example.com/URI http://example.com/location.xsd http://josm.openstreetmap.de/gpx-extensions-1.1 http://josm.openstreetmap.de/gpx-extensions-1.1.xsd http://josm.openstreetmap.de/gpx-drawing-extensions-1.0 http://josm.openstreetmap.de/gpx-drawing-extensions-1.0.xsd\">\n" +
                "  <metadata>\n" +
                "    <bounds minlat=\"0.0\" minlon=\"0.0\" maxlat=\"0.0\" maxlon=\"0.0\"/>\n" +
                "    <extensions>\n" +
                "      <knownprefix:foo>bar</knownprefix:foo>\n" +
                "      <josm:from-server>true</josm:from-server>\n" +
                "      <josm:layerPreferences>\n" +
                "        <josm:entry key=\"foo\" value=\"bar\"/>\n" +
                "      </josm:layerPreferences>\n" +
                "    </extensions>\n" +
                "  </metadata>\n" +
                "  <trk>\n" +
                "    <extensions>\n" +
                "      <gpxd:color>#FF0000</gpxd:color>\n" +
                "    </extensions>\n" +
                "    <trkseg>\n" +
                "      <extensions>\n" +
                "        <test:foo>extension of a segment</test:foo>\n" +
                "      </extensions>\n" +
                "      <trkpt lat=\"0.0\" lon=\"0.0\">\n" +
                "        <extensions>\n" +
                "          <test:foo>extension of a waypoint</test:foo>\n" +
                "        </extensions>\n" +
                "      </trkpt>\n" +
                "    </trkseg>\n" +
                "  </trk>\n" +
                "</gpx>", baos.toString());

        baos.reset();
        writer.write(data, GpxConstants.ColorFormat.GPXX, true);
        assertEqualsNewline("<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<gpx version=\"1.1\" creator=\"JOSM GPX export\" xmlns=\"http://www.topografix.com/GPX/1/1\"\n" +
                "    xmlns:knownprefix=\"http://example.com/URI\"\n" +
                "    xmlns:josm=\"http://josm.openstreetmap.de/gpx-extensions-1.1\"\n" +
                "    xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\"\n" +
                "    xmlns:test=\"http://example.com/testURI\"\n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://example.com/URI http://example.com/location.xsd http://josm.openstreetmap.de/gpx-extensions-1.1 http://josm.openstreetmap.de/gpx-extensions-1.1.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www.garmin.com/xmlschemas/GpxExtensionsv3.xsd\">\n" +
                "  <metadata>\n" +
                "    <bounds minlat=\"0.0\" minlon=\"0.0\" maxlat=\"0.0\" maxlon=\"0.0\"/>\n" +
                "    <extensions>\n" +
                "      <knownprefix:foo>bar</knownprefix:foo>\n" +
                "      <josm:from-server>true</josm:from-server>\n" +
                "      <josm:layerPreferences>\n" +
                "        <josm:entry key=\"foo\" value=\"bar\"/>\n" +
                "      </josm:layerPreferences>\n" +
                "    </extensions>\n" +
                "  </metadata>\n" +
                "  <trk>\n" +
                "    <extensions>\n" +
                "      <gpxx:TrackExtension>\n" +
                "        <gpxx:DisplayColor>Red</gpxx:DisplayColor>\n" +
                "      </gpxx:TrackExtension>\n" +
                "    </extensions>\n" +
                "    <trkseg>\n" +
                "      <extensions>\n" +
                "        <test:foo>extension of a segment</test:foo>\n" +
                "      </extensions>\n" +
                "      <trkpt lat=\"0.0\" lon=\"0.0\">\n" +
                "        <extensions>\n" +
                "          <test:foo>extension of a waypoint</test:foo>\n" +
                "        </extensions>\n" +
                "      </trkpt>\n" +
                "    </trkseg>\n" +
                "  </trk>\n" +
                "</gpx>", baos.toString());

        baos.reset();
        writer.write(data, null, false);
        assertEqualsNewline("<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<gpx version=\"1.1\" creator=\"JOSM GPX export\" xmlns=\"http://www.topografix.com/GPX/1/1\"\n" +
                "    xmlns:knownprefix=\"http://example.com/URI\"\n" +
                "    xmlns:josm=\"http://josm.openstreetmap.de/gpx-extensions-1.1\"\n" +
                "    xmlns:test=\"http://example.com/testURI\"\n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://example.com/URI http://example.com/location.xsd http://josm.openstreetmap.de/gpx-extensions-1.1 http://josm.openstreetmap.de/gpx-extensions-1.1.xsd\">\n" +
                "  <metadata>\n" +
                "    <bounds minlat=\"0.0\" minlon=\"0.0\" maxlat=\"0.0\" maxlon=\"0.0\"/>\n" +
                "    <extensions>\n" +
                "      <knownprefix:foo>bar</knownprefix:foo>\n" +
                "      <josm:from-server>true</josm:from-server>\n" +
                "    </extensions>\n" +
                "  </metadata>\n" +
                "  <trk>\n" +
                "    <trkseg>\n" +
                "      <extensions>\n" +
                "        <test:foo>extension of a segment</test:foo>\n" +
                "      </extensions>\n" +
                "      <trkpt lat=\"0.0\" lon=\"0.0\">\n" +
                "        <extensions>\n" +
                "          <test:foo>extension of a waypoint</test:foo>\n" +
                "        </extensions>\n" +
                "      </trkpt>\n" +
                "    </trkseg>\n" +
                "  </trk>\n" +
                "</gpx>", baos.toString());

        baos.reset();
        writer.write(data, GpxConstants.ColorFormat.GPXX, true);
        // checked again to make sure that extensions are shown again after
        // being hidden, even if they don't actually have to be converted
        // (GPXD -> convertColor() -> GPXX -> hide() -> null -> show() -> GPXX)
        assertEqualsNewline("<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<gpx version=\"1.1\" creator=\"JOSM GPX export\" xmlns=\"http://www.topografix.com/GPX/1/1\"\n" +
                "    xmlns:knownprefix=\"http://example.com/URI\"\n" +
                "    xmlns:josm=\"http://josm.openstreetmap.de/gpx-extensions-1.1\"\n" +
                "    xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\"\n" +
                "    xmlns:test=\"http://example.com/testURI\"\n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://example.com/URI http://example.com/location.xsd http://josm.openstreetmap.de/gpx-extensions-1.1 http://josm.openstreetmap.de/gpx-extensions-1.1.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www.garmin.com/xmlschemas/GpxExtensionsv3.xsd\">\n" +
                "  <metadata>\n" +
                "    <bounds minlat=\"0.0\" minlon=\"0.0\" maxlat=\"0.0\" maxlon=\"0.0\"/>\n" +
                "    <extensions>\n" +
                "      <knownprefix:foo>bar</knownprefix:foo>\n" +
                "      <josm:from-server>true</josm:from-server>\n" +
                "      <josm:layerPreferences>\n" +
                "        <josm:entry key=\"foo\" value=\"bar\"/>\n" +
                "      </josm:layerPreferences>\n" +
                "    </extensions>\n" +
                "  </metadata>\n" +
                "  <trk>\n" +
                "    <extensions>\n" +
                "      <gpxx:TrackExtension>\n" +
                "        <gpxx:DisplayColor>Red</gpxx:DisplayColor>\n" +
                "      </gpxx:TrackExtension>\n" +
                "    </extensions>\n" +
                "    <trkseg>\n" +
                "      <extensions>\n" +
                "        <test:foo>extension of a segment</test:foo>\n" +
                "      </extensions>\n" +
                "      <trkpt lat=\"0.0\" lon=\"0.0\">\n" +
                "        <extensions>\n" +
                "          <test:foo>extension of a waypoint</test:foo>\n" +
                "        </extensions>\n" +
                "      </trkpt>\n" +
                "    </trkseg>\n" +
                "  </trk>\n" +
                "</gpx>", baos.toString());

        // CHECKSTYLE.ON: LineLength

        writer.close();
    }
}
