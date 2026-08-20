// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ChangesetManagementPanel} class.
 */
@BasicPreferences
class ChangesetManagementPanelTest {
    /**
     * Test of {@link ChangesetManagementPanel#ChangesetManagementPanel}.
     */
    @Test
    void testChangesetManagementPanel() {
        assertNotNull(new ChangesetManagementPanel(new ChangesetCommentModel()));
    }
}
