// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link ChangesetDiscussionPanel} class.
 */
@BasicPreferences
class ChangesetDiscussionPanelTest {
    /**
     * Unit test of {@link ChangesetDiscussionPanel#ChangesetDiscussionPanel}.
     */
    @Test
    void testChangesetDiscussionPanel() {
        assertNotNull(new ChangesetDiscussionPanel());
    }
}
