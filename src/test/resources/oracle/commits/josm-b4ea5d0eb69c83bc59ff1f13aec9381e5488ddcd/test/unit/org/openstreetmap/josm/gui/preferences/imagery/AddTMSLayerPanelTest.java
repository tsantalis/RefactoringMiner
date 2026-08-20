// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link AddTMSLayerPanel} class.
 */
@BasicPreferences
class AddTMSLayerPanelTest {
    /**
     * Unit test of {@link AddTMSLayerPanel}.
     */
    @Test
    void testAddTMSLayerPanel() {
        AddTMSLayerPanel panel = new AddTMSLayerPanel();
        assertEquals("", panel.getImageryInfo().getUrl());
        assertFalse(panel.isImageryValid());
    }
}
