// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link AddWMTSLayerPanel} class.
 */
@BasicPreferences
class AddWMTSLayerPanelTest {
    /**
     * Unit test of {@link AddWMTSLayerPanel}.
     */
    @Test
    void testAddWMTSLayerPanel() {
        AddWMTSLayerPanel panel = new AddWMTSLayerPanel();
        assertFalse(panel.isImageryValid());
    }

    /**
     * Unit test of {@link AddWMTSLayerPanel#getImageryInfo}.
     */
    @Test
    void testGetImageryInfo() {
        assertThrows(IllegalArgumentException.class, () -> new AddWMTSLayerPanel().getImageryInfo());
    }
}
