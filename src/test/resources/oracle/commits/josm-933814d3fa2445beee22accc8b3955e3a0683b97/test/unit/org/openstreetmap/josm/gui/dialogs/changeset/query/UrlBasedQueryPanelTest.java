// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link UrlBasedQueryPanel} class.
 */
class UrlBasedQueryPanelTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link UrlBasedQueryPanel#UrlBasedQueryPanel}.
     */
    @Test
    void testUrlBasedQueryPanel() {
        assertNotNull(new UrlBasedQueryPanel());
    }

    /**
     * Checks that examples displayed in panel are correct.
     */
    @Test
    void testExamplesAreCorrect() {
        for (String example : UrlBasedQueryPanel.getExamples()) {
            assertTrue(UrlBasedQueryPanel.isValidChangesetQueryUrl(example), example);
        }
    }
}
