// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.tags;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link TagMerger} class.
 */
@BasicPreferences
class TagMergerTest {
    /**
     * Unit test of {@link TagMerger#TagMerger}.
     */
    @Test
    void testTagMerger() {
        assertNotNull(new TagMerger());
    }
}
