// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link ChangesetCacheTableColumnModel} class.
 */
class ChangesetCacheTableColumnModelTest {
    /**
     * Unit test of {@link ChangesetCacheTableColumnModel}.
     */
    @Test
    void testChangesetCacheTableColumnModel() {
        assertNotNull(new ChangesetCacheTableColumnModel());
    }
}
