// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link PropertiesDialog} class.
 */
class PropertiesDialogTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static String createSearchSetting(List<OsmPrimitive> sel, boolean sameType) {
        return PropertiesDialog.createSearchSetting("foo", sel, sameType).text;
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/12504">#12504</a>.
     */
    @Test
    void testTicket12504() {
        List<OsmPrimitive> sel = new ArrayList<>();
        // 160 objects with foo=bar, 400 objects without foo
        for (int i = 0; i < 160+400; i++) {
            Node n = new Node(LatLon.ZERO);
            if (i < 160) {
                n.put("foo", "bar");
            }
            sel.add(n);
        }
        assertEquals("(\"foo\"=\"bar\")", createSearchSetting(sel, false));

        Node n = new Node(LatLon.ZERO);
        n.put("foo", "baz");
        sel.add(0, n);

        assertEquals("(\"foo\"=\"baz\") OR (\"foo\"=\"bar\")", createSearchSetting(sel, false));

        sel.remove(0);

        Way w = new Way();
        w.put("foo", "bar");
        sel.add(0, w);

        assertEquals("(type:way \"foo\"=\"bar\") OR (type:node \"foo\"=\"bar\")", createSearchSetting(sel, true));
    }
}
