// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link PropertiesMerger} class.
 */
@BasicPreferences
class PropertiesMergerTest {
    /**
     * Unit test of {@link PropertiesMerger#PropertiesMerger}.
     */
    @Test
    void testPropertiesMerger() {
        PropertiesMerger merger = new PropertiesMerger();
        assertNotNull(TestUtils.getComponentByName(merger, "button.keepmycoordinates"));
        assertNotNull(TestUtils.getComponentByName(merger, "button.keeptheircoordinates"));
        assertNotNull(TestUtils.getComponentByName(merger, "button.undecidecoordinates"));
        assertNotNull(TestUtils.getComponentByName(merger, "button.keepmydeletedstate"));
        assertNotNull(TestUtils.getComponentByName(merger, "button.keeptheirdeletedstate"));
        assertNotNull(TestUtils.getComponentByName(merger, "button.undecidedeletedstate"));
    }
}
