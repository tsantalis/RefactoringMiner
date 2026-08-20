// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link RelationMemberConflictResolver} class.
 */
@BasicPreferences
class RelationMemberConflictResolverTest {
    /**
     * Unit test for {@link RelationMemberConflictResolver#RelationMemberConflictResolver}.
     */
    @Test
    void testRelationMemberConflictResolver() {
        assertNotNull(new RelationMemberConflictResolver(null));
    }
}
