// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bugreport;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.bugreport.BugReport;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests the {@link BugReportSettingsPanel} class.
 */
class BugReportSettingsPanelTest {

    /**
     * Setup test
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test {@link BugReportSettingsPanel}
     */
    @Test
    void testBugReportSettingsPanel() {
        assertNotNull(new BugReportSettingsPanel(new BugReport(BugReport.intercept(new Exception()))));
    }
}
