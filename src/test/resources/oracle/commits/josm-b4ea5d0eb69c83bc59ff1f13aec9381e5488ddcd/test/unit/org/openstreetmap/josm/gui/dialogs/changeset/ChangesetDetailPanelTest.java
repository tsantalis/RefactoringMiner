// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link ChangesetDetailPanel} class.
 */
@BasicPreferences
class ChangesetDetailPanelTest {
    /**
     * Unit test of {@link ChangesetDetailPanel#ChangesetDetailPanel}.
     */
    @Test
    void testChangesetDetailPanel() {
        assertNotNull(new ChangesetDetailPanel());
    }
}
