// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link FullyAutomaticPropertiesPanel} class.
 */
class FullyAutomaticPropertiesPanelTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link FullyAutomaticPropertiesPanel#FullyAutomaticPropertiesPanel}.
     */
    @Test
    void testFullyAutomaticPropertiesPanel() {
        assertTrue(new FullyAutomaticPropertiesPanel().getComponentCount() > 0);
    }
}
