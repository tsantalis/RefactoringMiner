// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Test {@link StringSetting}.
 */
// This is a preference test
@BasicPreferences
class StringSettingTest {
    /**
     * Unit test of methods {@link StringSetting#equals} and {@link StringSetting#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(StringSetting.class).usingGetClass()
            .withIgnoredFields("isNew", "time")
            .verify();
    }
}
