// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.importers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.openstreetmap.josm.data.coor.LatLon;

import org.junit.jupiter.api.Test;

/**
 * Test {@link OsmLinkPaster}
 * @author Michael Zangl
 */
class OsmLinkPasterTest {
    /**
     * Test of {@link OsmLinkPaster#parseLatLon(String)}
     */
    @Test
    void testParseLatLon() {
        assertEquals(new LatLon(51.71873, 8.76164),
                OsmLinkPaster.parseLatLon("https://www.openstreetmap.org/#map=17/51.71873/8.76164"));
        assertNull(OsmLinkPaster.parseLatLon("http://www.openstreetmap.org/"));
        assertNull(OsmLinkPaster.parseLatLon("foo-bar"));
    }
}
