// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link BasicUploadSettingsPanel} class.
 */
@BasicPreferences
class BasicUploadSettingsPanelTest {
    /**
     * Test of {@link BasicUploadSettingsPanel#BasicUploadSettingsPanel}.
     */
    @Test
    void testBasicUploadSettingsPanel() {
        assertNotNull(new BasicUploadSettingsPanel(new ChangesetCommentModel(), new ChangesetCommentModel(), new ChangesetReviewModel()));
    }
}
