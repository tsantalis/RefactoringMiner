// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Test {@link ListSetting}.
 */
// This is a preference test
@BasicPreferences
class ListSettingTest {
    /**
     * Unit test of methods {@link ListSetting#equals} and {@link ListSetting#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(ListSetting.class).usingGetClass()
            .withIgnoredFields("isNew", "time")
            .verify();
    }
}
