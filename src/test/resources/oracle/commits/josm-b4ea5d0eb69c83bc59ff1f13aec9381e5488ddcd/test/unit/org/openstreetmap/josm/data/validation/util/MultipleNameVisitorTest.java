// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for class {@link MultipleNameVisitor}.
 */
@BasicPreferences
class MultipleNameVisitorTest {
    /**
     * Non-regression test for bug #11967.
     */
    @Test
    void testTicket11967() {
        MultipleNameVisitor visitor = new MultipleNameVisitor();
        visitor.visit(Arrays.asList(new Way(), new Way()));
        assertEquals("2 ways: \u200E0\u200E (0 nodes)\u200C, \u200E0\u200E (0 nodes)\u200C", visitor.toString());
    }

    /**
     * Non-regression test for bug #16652.
     */
    @Test
    void testTicket16652() {
        MultipleNameVisitor visitor = new MultipleNameVisitor();
        visitor.visit(Arrays.asList(
                TestUtils.newNode("name=foo"),
                TestUtils.newWay("addr:housename=Stark"),
                TestUtils.newRelation("type=route")));
        assertEquals("3 objects: foo, \u200EHouse Stark\u200E (0 nodes)\u200C, route (0, 0 members)", visitor.toString());
    }
}
