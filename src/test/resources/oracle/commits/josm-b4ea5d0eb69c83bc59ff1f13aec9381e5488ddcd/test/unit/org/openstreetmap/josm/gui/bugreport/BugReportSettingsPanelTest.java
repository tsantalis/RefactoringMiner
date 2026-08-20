// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bugreport;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.openstreetmap.josm.tools.bugreport.BugReport;

import org.junit.jupiter.api.Test;

/**
 * Tests the {@link BugReportSettingsPanel} class.
 */
class BugReportSettingsPanelTest {
    /**
     * Test {@link BugReportSettingsPanel}
     */
    @Test
    void testBugReportSettingsPanel() {
        assertNotNull(new BugReportSettingsPanel(new BugReport(BugReport.intercept(new Exception()))));
    }
}
