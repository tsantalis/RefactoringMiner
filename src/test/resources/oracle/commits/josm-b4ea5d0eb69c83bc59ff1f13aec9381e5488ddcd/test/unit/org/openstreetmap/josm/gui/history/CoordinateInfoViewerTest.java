// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link CoordinateInfoViewer} class.
 */
@BasicPreferences
class CoordinateInfoViewerTest {
    /**
     * Test for {@link CoordinateInfoViewer#CoordinateInfoViewer} - {@code null} handling.
     */
    @Test
    void testCoordinateInfoViewerNull() {
        assertThrows(IllegalArgumentException.class, () -> new CoordinateInfoViewer(null));
    }

    /**
     * Test for {@link CoordinateInfoViewer#CoordinateInfoViewer} - nominal case.
     */
    @Test
    void testCoordinateInfoViewerNominal() {
        assertNotNull(new CoordinateInfoViewer(new HistoryBrowserModel()));
    }
}
