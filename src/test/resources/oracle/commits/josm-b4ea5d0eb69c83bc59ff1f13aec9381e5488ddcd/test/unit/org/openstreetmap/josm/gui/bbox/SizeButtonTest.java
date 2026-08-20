// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.bbox.SizeButton.AccessibleSizeButton;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link SizeButton} class.
 */
@BasicPreferences
class SizeButtonTest {
    /**
     * Unit test of {@link SizeButton#SizeButton}.
     */
    @Test
    void testSizeButton() {
        SizeButton sb = new SizeButton(new SlippyMapBBoxChooser());
        sb.paint(TestUtils.newGraphics());
        AccessibleSizeButton asb = (AccessibleSizeButton) sb.getAccessibleContext();
        assertEquals(1, asb.getAccessibleActionCount());
        assertEquals("toggle", asb.getAccessibleActionDescription(0));
        assertFalse(sb.isEnlarged());
        assertTrue(asb.doAccessibleAction(0));
        sb.paint(TestUtils.newGraphics());
        assertTrue(sb.isEnlarged());
    }
}
