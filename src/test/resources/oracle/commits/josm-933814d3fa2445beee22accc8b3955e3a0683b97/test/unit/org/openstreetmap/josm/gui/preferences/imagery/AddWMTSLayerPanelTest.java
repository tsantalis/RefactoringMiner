// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AddWMTSLayerPanel} class.
 */
class AddWMTSLayerPanelTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

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
