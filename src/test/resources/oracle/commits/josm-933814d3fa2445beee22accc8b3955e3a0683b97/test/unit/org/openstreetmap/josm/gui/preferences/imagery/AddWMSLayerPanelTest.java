// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AddWMSLayerPanel} class.
 */
class AddWMSLayerPanelTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link AddWMSLayerPanel}.
     */
    @Test
    void testAddWMSLayerPanel() {
        AddWMSLayerPanel panel = new AddWMSLayerPanel();
        assertFalse(panel.isImageryValid());
    }
}
