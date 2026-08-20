// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AddTMSLayerPanel} class.
 */
class AddTMSLayerPanelTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

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
