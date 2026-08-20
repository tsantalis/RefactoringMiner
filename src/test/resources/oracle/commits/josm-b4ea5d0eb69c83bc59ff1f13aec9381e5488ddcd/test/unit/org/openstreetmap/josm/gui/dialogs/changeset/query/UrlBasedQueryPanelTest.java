// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link UrlBasedQueryPanel} class.
 */
@BasicPreferences
class UrlBasedQueryPanelTest {
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
