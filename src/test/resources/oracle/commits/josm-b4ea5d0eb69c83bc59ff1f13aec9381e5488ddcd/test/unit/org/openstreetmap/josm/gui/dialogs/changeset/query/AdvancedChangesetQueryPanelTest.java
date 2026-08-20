// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link AdvancedChangesetQueryPanel} class.
 */
@BasicPreferences
class AdvancedChangesetQueryPanelTest {
    /**
     * Unit test of {@link AdvancedChangesetQueryPanel#AdvancedChangesetQueryPanel}.
     */
    @Test
    void testAdvancedChangesetQueryPanel() {
        assertNotNull(new AdvancedChangesetQueryPanel());
    }
}
