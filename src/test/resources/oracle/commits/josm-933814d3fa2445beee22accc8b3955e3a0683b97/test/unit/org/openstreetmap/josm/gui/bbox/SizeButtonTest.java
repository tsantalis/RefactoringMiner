// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.bbox.SizeButton.AccessibleSizeButton;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link SizeButton} class.
 */
class SizeButtonTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

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
