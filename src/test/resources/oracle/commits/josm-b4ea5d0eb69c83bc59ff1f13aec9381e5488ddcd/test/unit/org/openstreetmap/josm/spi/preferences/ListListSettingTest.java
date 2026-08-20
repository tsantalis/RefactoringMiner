// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Test {@link ListListSetting}.
 */
// This is a preference test
@BasicPreferences
class ListListSettingTest {
    /**
     * Unit test of methods {@link ListListSetting#equals} and {@link ListListSetting#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(ListListSetting.class).usingGetClass()
            .withIgnoredFields("isNew", "time")
            .verify();
    }
}
