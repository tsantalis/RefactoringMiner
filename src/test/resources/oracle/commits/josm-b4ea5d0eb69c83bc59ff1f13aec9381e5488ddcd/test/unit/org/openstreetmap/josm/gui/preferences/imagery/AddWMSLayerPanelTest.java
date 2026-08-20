// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.jupiter.api.Assertions.assertFalse;


import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link AddWMSLayerPanel} class.
 */
@BasicPreferences
class AddWMSLayerPanelTest {
    /**
     * Unit test of {@link AddWMSLayerPanel}.
     */
    @Test
    void testAddWMSLayerPanel() {
        AddWMSLayerPanel panel = new AddWMSLayerPanel();
        assertFalse(panel.isImageryValid());
    }
}
