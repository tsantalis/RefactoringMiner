// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bugreport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit test of {@link DebugTextDisplay} class.
 */
class DebugTextDisplayTest {
    /**
     * Setup test
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

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
