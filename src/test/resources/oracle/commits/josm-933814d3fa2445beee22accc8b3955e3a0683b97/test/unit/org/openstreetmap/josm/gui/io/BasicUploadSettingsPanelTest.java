// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link BasicUploadSettingsPanel} class.
 */
class BasicUploadSettingsPanelTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test of {@link BasicUploadSettingsPanel#BasicUploadSettingsPanel}.
     */
    @Test
    void testBasicUploadSettingsPanel() {
        assertNotNull(new BasicUploadSettingsPanel(new ChangesetCommentModel(), new ChangesetCommentModel(), new ChangesetReviewModel()));
    }
}
