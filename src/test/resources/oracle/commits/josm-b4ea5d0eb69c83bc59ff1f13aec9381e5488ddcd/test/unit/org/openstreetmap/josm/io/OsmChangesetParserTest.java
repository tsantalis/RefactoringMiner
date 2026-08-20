// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link OsmChangesetParser} class.
 */
@BasicPreferences
class OsmChangesetParserTest {

    private static final String BEGIN =
        "<osm version=\"0.6\" generator=\"OpenStreetMap server\" copyright=\"OpenStreetMap and contributors\" " +
                "attribution=\"http://www.openstreetmap.org/copyright\" license=\"http://opendatacommons.org/licenses/odbl/1-0/\">" +
            "<changeset id=\"36749147\" user=\"kesler\" uid=\"13908\" created_at=\"2016-01-22T21:55:37Z\" "+
                "closed_at=\"2016-01-22T21:56:39Z\"  open=\"false\" min_lat=\"36.6649211\" min_lon=\"55.377015\" max_lat=\"38.1490357\" " +
                "max_lon=\"60.3766983\" comments_count=\"2\" changes_count=\"9\">" +
            "<tag k=\"created_by\" v=\"JOSM/1.5 (9329 en)\"/>" +
            "<tag k=\"comment\" v=\"Fixing errors in North Khorasan\"/>";

    private static final String DISCUSSION =
        "<discussion>" +
        "<comment date=\"2016-09-13T13:28:20Z\" uid=\"1733149\" user=\"Jean Passepartout\">" +
            "<text>" +
                "Hi keeler, Thank you for contributing to OpenStreetMap. " +
                "I noticed you added this way: http://www.openstreetmap.org/way/363580576, " +
                "but it is a duplicate of another way, with a different name. "+
                "Could you review and fix this? Please let me know if you need any help. " +
                "Thank you and Happy Mapping! Jean Passepartout" +
            "</text>" +
        "</comment>" +
        "<comment date=\"2016-09-17T21:58:57Z\" uid=\"13908\" user=\"kesler\">" +
            "<text>" +
                "Hi Jean, Thanks for your attempts to fix Iran map errors and please excuse me for delayed reply. "+
                "I fixed my mistake and developed Esfarayen town map more in changeset https://www.openstreetmap.org/changeset/42234849. " +
                "Please feel free and don't hesitate to tell me every big or even minor mis-mapped area in Iran. Thaks." +
            "</text>" +
        "</comment>" +
        "</discussion>";

    private static final String END =
        "</changeset>" +
        "</osm>";

    private static List<Changeset> parse(String cs) throws IllegalDataException {
        return OsmChangesetParser.parse(new ByteArrayInputStream(cs.getBytes(StandardCharsets.UTF_8)), NullProgressMonitor.INSTANCE);
    }

    /**
     * Parse single changeset - without discussion
     * @throws IllegalDataException in case of error
     */
    @Test
    void testParseWithoutDiscussion() throws IllegalDataException {
        // http://api.openstreetmap.org/api/0.6/changeset/36749147
        Changeset cs = parse(BEGIN + END).iterator().next();
        assertEquals(2, cs.getCommentsCount());
        assertEquals(9, cs.getChangesCount());
        assertTrue(cs.getDiscussion().isEmpty());
    }

    /**
     * Parse single changeset - with discussion
     * @throws IllegalDataException in case of error
     */
    @Test
    void testParseWithDiscussion() throws IllegalDataException {
        // http://api.openstreetmap.org/api/0.6/changeset/36749147?include_discussion=true
        Changeset cs = parse(BEGIN + DISCUSSION + END).iterator().next();
        assertEquals(2, cs.getCommentsCount());
        assertEquals(9, cs.getChangesCount());
        assertEquals(2, cs.getDiscussion().size());
    }
}
