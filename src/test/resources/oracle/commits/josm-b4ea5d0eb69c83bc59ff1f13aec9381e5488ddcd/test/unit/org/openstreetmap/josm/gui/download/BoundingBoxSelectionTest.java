// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link BoundingBoxSelection} class.
 */
@BasicPreferences
class BoundingBoxSelectionTest {
    /**
     * Test for {@link BoundingBoxSelection#BoundingBoxSelection}.
     */
    @Test
    void testBoundingBoxSelection() {
        BoundingBoxSelection sel = new BoundingBoxSelection();
        sel.addGui(null);
        sel.setDownloadArea(null);
        sel.setDownloadArea(new Bounds(0, 0, 1, 1));
    }
}
