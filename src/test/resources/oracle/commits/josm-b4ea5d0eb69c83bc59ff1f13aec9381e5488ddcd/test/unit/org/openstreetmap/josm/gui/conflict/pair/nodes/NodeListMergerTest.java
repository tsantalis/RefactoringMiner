// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.nodes;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link NodeListMerger} class.
 */
@BasicPreferences
class NodeListMergerTest {
    /**
     * Unit test of {@link NodeListMerger#NodeListMerger}.
     */
    @Test
    void testNodeListMerger() {
        assertNotNull(new NodeListMerger());
    }
}
