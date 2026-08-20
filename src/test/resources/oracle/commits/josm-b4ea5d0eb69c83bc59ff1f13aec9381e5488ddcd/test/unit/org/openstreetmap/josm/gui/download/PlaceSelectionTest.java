// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link PlaceSelection} class.
 */
@BasicPreferences
class PlaceSelectionTest {
    /**
     * Test for {@link PlaceSelection#PlaceSelection}.
     */
    @Test
    void testBookmarkSelection() {
        PlaceSelection sel = new PlaceSelection();
        sel.addGui(null);
        sel.setDownloadArea(null);
        sel.setDownloadArea(new Bounds(0, 0, 1, 1));
    }
}
