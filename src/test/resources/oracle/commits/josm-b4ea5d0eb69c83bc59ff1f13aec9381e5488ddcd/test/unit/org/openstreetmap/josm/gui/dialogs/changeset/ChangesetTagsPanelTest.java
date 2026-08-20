// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link ChangesetTagsPanel} class.
 */
@BasicPreferences
class ChangesetTagsPanelTest {
    /**
     * Unit test of {@link ChangesetTagsPanel#ChangesetTagsPanel}.
     */
    @Test
    void testChangesetTagsPanel() {
        assertNotNull(new ChangesetTagsPanel());
    }
}
