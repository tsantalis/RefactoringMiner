// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.importers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test {@link OsmLinkPaster}
 * @author Michael Zangl
 */
class OsmLinkPasterTest {
    /**
     * No dependencies
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

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
