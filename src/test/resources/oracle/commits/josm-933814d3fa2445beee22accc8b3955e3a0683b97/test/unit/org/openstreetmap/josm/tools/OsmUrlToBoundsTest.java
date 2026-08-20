// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import org.junit.Assert;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
  * Unit tests of {@link OsmUrlToBounds} class.
*/
class OsmUrlToBoundsTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test for {@link OsmUrlToBounds#positionToBounds}.
     */
    @Test
    void testPositionToBounds() {
        Assert.assertEquals(new Bounds(51.7167359, 8.7573485, 51.720724, 8.7659315),
                OsmUrlToBounds.positionToBounds(51.71873, 8.76164, 17));
        Assert.assertEquals(new Bounds(40.8609329, -75.7523458, 40.8633671, -75.7480542),
                OsmUrlToBounds.positionToBounds(40.86215, -75.75020, 18));
    }

    /**
     * data for {@link #testParse}
     */
    private static final ParseTestItem[] parseTestData = {
        new ParseTestItem("https://www.openstreetmap.org", null),
        new ParseTestItem("geo:12.34,56.78?z=9",
                OsmUrlToBounds.positionToBounds(12.34, 56.78, 9)),
        new ParseTestItem("https://www.openstreetmap.org/?bbox=-0.489,51.28,0.236,51.686",
                new Bounds(51.28, -0.489, 51.686, 0.236)),
        new ParseTestItem("https://www.openstreetmap.org/?minlon=-0.489&minlat=51.28&maxlon=0.236&maxlat=51.686",
                new Bounds(51.28, -0.489, 51.686, 0.236)),
        new ParseTestItem("https://www.openstreetmap.org/?maxlat=51.686&maxlon=0.236&minlat=51.28&minlon=-0.489",
                new Bounds(51.28, -0.489, 51.686, 0.236)),
        new ParseTestItem("https://www.openstreetmap.org/?zoom=17&lat=51.71873&lon=8.76164",
                OsmUrlToBounds.positionToBounds(51.71873, 8.76164, 17)),
        new ParseTestItem("https://www.openstreetmap.org/?lon=8.76164&lat=51.71873&zoom=17&foo",
                OsmUrlToBounds.positionToBounds(51.71873, 8.76164, 17)),
        new ParseTestItem("https://www.openstreetmap.org/?mlon=8.76164&mlat=51.71873",
                OsmUrlToBounds.positionToBounds(51.71873, 8.76164, 18)),
        new ParseTestItem("http://osm.org/go/euulwp",
                OsmUrlToBounds.positionToBounds(51.48262023925781, -0.29937744140625, 8)),
        new ParseTestItem("https://www.openstreetmap.org/#map=17/51.71873/8.76164",
                OsmUrlToBounds.positionToBounds(51.71873, 8.76164, 17)),
        new ParseTestItem("https://www.openstreetmap.org/#map=17/51.71873/8.76164&layers=CN",
                OsmUrlToBounds.positionToBounds(51.71873, 8.76164, 17)),
        new ParseTestItem("https%3A%2F%2Fwww.openstreetmap.org%2F%23map%3D16%2F51.71873%2F8.76164",
                OsmUrlToBounds.positionToBounds(51.71873, 8.76164, 16)),
        new ParseTestItem("https%3A%2F%2Fwww.openstreetmap.org%2F%23map%3D16%2F51.71873%2F8.76164%26layers%3DCN",
                OsmUrlToBounds.positionToBounds(51.71873, 8.76164, 16)),
        new ParseTestItem("https://www.openstreetmap.org/?note=26325#map=18/40.86215/-75.75020",
                OsmUrlToBounds.positionToBounds(40.86215, -75.75020, 18)),
        new ParseTestItem("https://www.openstreetmap.org/?note=26325#map=18/40.86215/-75.75020&layers=N",
                OsmUrlToBounds.positionToBounds(40.86215, -75.75020, 18)),
        new ParseTestItem("https://www.openstreetmap.org/?mlat=51.5&mlon=-0.01#map=10/51.4831/-0.1270",
                OsmUrlToBounds.positionToBounds(51.4831, -0.1270, 10)),
        new ParseTestItem("https://www.openstreetmap.org/?mlat=51.5&mlon=-0.01#map=10/51.4831/-0.3509&layers=T",
                OsmUrlToBounds.positionToBounds(51.4831, -0.3509, 10)),
        new ParseTestItem("https://www.openstreetmap.org/#map", null),
        new ParseTestItem("https://www.openstreetmap.org/#map=foo", null),
        new ParseTestItem("https://www.openstreetmap.org/#map=fooz/foolat/foolon", null),
        new ParseTestItem("http://tyrasd.github.io/latest-changes/#13/51.6891/10.2312",
                OsmUrlToBounds.positionToBounds(51.6891, 10.2312, 13))
    };

    private static class ParseTestItem {
        public String url;
        public Bounds bounds;

        ParseTestItem(String url, Bounds bounds) {
            this.url = url;
            this.bounds = bounds;
        }
    }

    /**
     * Test URL parsing
     */
    @Test
    void testParse() {
        for (ParseTestItem item : parseTestData) {
            Bounds bounds = null;
            try {
                bounds = OsmUrlToBounds.parse(item.url);
            } catch (IllegalArgumentException e) {
                // Ignore. check if bounds is null after
                Logging.trace(e);
            }
            Assert.assertEquals(item.url, item.bounds, bounds);
        }
    }

    /**
     * Test for {@link OsmUrlToBounds#getZoom}.
     */
    @Test
    void testGetZoom() {
        Assert.assertEquals(4, OsmUrlToBounds.getZoom(OsmUrlToBounds.positionToBounds(0, 0, 4)));
        Assert.assertEquals(10, OsmUrlToBounds.getZoom(OsmUrlToBounds.positionToBounds(5, 5, 10)));
        Assert.assertEquals(18, OsmUrlToBounds.getZoom(OsmUrlToBounds.positionToBounds(40, 20, 18)));
    }
}
