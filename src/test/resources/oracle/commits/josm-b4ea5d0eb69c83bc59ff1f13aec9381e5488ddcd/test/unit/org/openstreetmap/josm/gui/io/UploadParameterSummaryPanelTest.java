// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link UploadParameterSummaryPanel} class.
 */
@BasicPreferences
class UploadParameterSummaryPanelTest {
    /**
     * Test of {@link UploadParameterSummaryPanel#UploadParameterSummaryPanel}.
     */
    @Test
    void testUploadParameterSummaryPanel() {
        assertNotNull(new UploadParameterSummaryPanel());
    }
}
