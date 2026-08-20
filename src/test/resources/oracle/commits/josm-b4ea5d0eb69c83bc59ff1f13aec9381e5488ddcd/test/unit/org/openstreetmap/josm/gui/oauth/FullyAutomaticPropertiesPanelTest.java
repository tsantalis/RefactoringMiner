// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link FullyAutomaticPropertiesPanel} class.
 */
@BasicPreferences
class FullyAutomaticPropertiesPanelTest {
    /**
     * Unit test of {@link FullyAutomaticPropertiesPanel#FullyAutomaticPropertiesPanel}.
     */
    @Test
    void testFullyAutomaticPropertiesPanel() {
        assertTrue(new FullyAutomaticPropertiesPanel().getComponentCount() > 0);
    }
}
