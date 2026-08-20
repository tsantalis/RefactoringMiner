// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.relation;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link RelationMemberMerger} class.
 */
@BasicPreferences
class RelationMemberMergerTest {
    /**
     * Unit test of {@link RelationMemberMerger#RelationMemberMerger}.
     */
    @Test
    void testRelationMemberMerger() {
        assertNotNull(new RelationMemberMerger());
    }
}
