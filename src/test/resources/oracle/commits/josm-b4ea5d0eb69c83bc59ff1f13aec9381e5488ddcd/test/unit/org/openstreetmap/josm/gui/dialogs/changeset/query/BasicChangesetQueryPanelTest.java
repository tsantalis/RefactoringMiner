// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link BasicChangesetQueryPanel} class.
 */
@BasicPreferences
class BasicChangesetQueryPanelTest {
    /**
     * Unit test of {@link BasicChangesetQueryPanel#BasicChangesetQueryPanel}.
     */
    @Test
    void testBasicChangesetQueryPanel() {
        assertNotNull(new BasicChangesetQueryPanel());
    }
}
