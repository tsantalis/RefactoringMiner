// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests the {@link GpxReader}.
 */
public class GpxReaderTest {

    /**
     * Setup rule
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Parses a GPX file and returns the parsed data
     * @param filename the GPX file to parse
     * @return the parsed GPX data
     * @throws IOException if an error occurs during reading.
     * @throws SAXException if a SAX error occurs
     */
    public static GpxData parseGpxData(String filename) throws IOException, SAXException {
        final GpxData result;
        try (FileInputStream in = new FileInputStream(new File(filename))) {
            GpxReader reader = new GpxReader(in);
            assertTrue(reader.parse(false));
            result = reader.getGpxData();
        }
        return result;
    }

    /**
     * Tests the {@code munich.gpx} test file.
     * @throws Exception if something goes wrong
     */
    @Test
    void testMunich() throws Exception {
        final GpxData result = parseGpxData("nodist/data/munich.gpx");
        assertEquals(2762, result.getTracks().size());
        assertEquals(0, result.getRoutes().size());
        assertEquals(903, result.getWaypoints().size());

        final WayPoint tenthWayPoint = new ArrayList<>(result.getWaypoints()).get(10);
        assertEquals("128970", tenthWayPoint.get(GpxData.GPX_NAME));
        assertEquals(new LatLon(48.183956146240234, 11.43463134765625), tenthWayPoint.getCoor());
    }

    /**
     * Tests if layer preferences can be read
     * @throws Exception if track can't be parsed
     */
    @Test
    void testLayerPrefs() throws Exception {
        final GpxData data = parseGpxData(TestUtils.getTestDataRoot() + "tracks/tracks-layerprefs.gpx");
        Map<String, String> e = new HashMap<>();
        e.put("colormode.velocity.tune", "10");
        e.put("lines.arrows.min-distance", "20");
        e.put("colormode", "1");
        e.put("lines", "1");
        e.put("lines.arrows", "true");
        e.put("colormode.dynamic-range", "true");
        e.put("colors", "0");
        assertEquals(data.getLayerPrefs(), e);
    }

    /**
     * Tests invalid data.
     * @throws Exception always SAXException
     */
    @Test
    void testException() throws Exception {
        assertThrows(SAXException.class,
                () -> new GpxReader(new ByteArrayInputStream("--foo--bar--".getBytes(StandardCharsets.UTF_8))).parse(true));
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/15634">#15634</a>
     * @throws IOException if an error occurs during reading
     * @throws SAXException if any XML error occurs
     */
    @Test
    void testTicket15634() throws IOException, SAXException {
        assertEquals(new Bounds(53.7229357, -7.9135019, 53.9301103, -7.59656),
                GpxReaderTest.parseGpxData(TestUtils.getRegressionDataFile(15634, "drumlish.gpx")).getMetaBounds());
    }
}
