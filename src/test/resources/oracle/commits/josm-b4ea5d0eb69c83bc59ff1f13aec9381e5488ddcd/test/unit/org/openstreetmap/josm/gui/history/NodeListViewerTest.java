// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link NodeListViewer} class.
 */
@BasicPreferences
class NodeListViewerTest {
    /**
     * Test for {@link NodeListViewer#NodeListViewer} - {@code null} handling.
     */
    @Test
    void testNodeListViewerNull() {
        assertThrows(IllegalArgumentException.class, () -> new NodeListViewer(null));
    }

    /**
     * Test for {@link NodeListViewer#NodeListViewer} - nominal case.
     */
    @Test
    void testNodeListViewerNominal() {
        assertNotNull(new NodeListViewer(new HistoryBrowserModel()));
    }
}
