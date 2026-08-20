// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests for class {@link AddTagsDialog}.
 */
@BasicPreferences
class AddTagsDialogTest {
    /**
     * Unit test of {@link AddTagsDialog#parseUrlTagsToKeyValues}
     */
    @Test
    void testParseUrlTagsToKeyValues() {
        Map<String, String> strings = AddTagsDialog.parseUrlTagsToKeyValues("wikipedia:de=Residenzschloss Dresden|name:en=Dresden Castle");
        assertEquals(2, strings.size());
        assertEquals("Residenzschloss Dresden", strings.get("wikipedia:de"));
        assertEquals("Dresden Castle", strings.get("name:en"));
    }
}
