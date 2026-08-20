// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bugreport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit test of {@link DebugTextDisplay} class.
 */
@BasicPreferences
class DebugTextDisplayTest {
    /**
     * Test {@link DebugTextDisplay#getCodeText}
     */
    @Test
    void testGetCodeText() {
        assertEquals("test", new DebugTextDisplay("test").getCodeText());
    }

    /**
     * Test {@link DebugTextDisplay#copyToClipboard}
     */
    @Test
    void testCopyToClipboard() {
        new DebugTextDisplay("copy").copyToClipboard();
        assertEquals(String.format("{{{%ncopy%n}}}"), ClipboardUtils.getClipboardStringContent());
    }
}
