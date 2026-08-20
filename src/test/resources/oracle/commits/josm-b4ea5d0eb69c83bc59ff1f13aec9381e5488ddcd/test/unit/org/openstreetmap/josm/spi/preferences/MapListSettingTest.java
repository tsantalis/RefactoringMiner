// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Test {@link MapListSetting}.
 */
// This is a preference test
@BasicPreferences
class MapListSettingTest {
    /**
     * Unit test of methods {@link MapListSetting#equals} and {@link MapListSetting#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(MapListSetting.class).usingGetClass()
            .withIgnoredFields("isNew", "time")
            .verify();
    }
}
