// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ProxyPreference} class.
 */
@BasicPreferences
class ProxyPreferenceTest {
    /**
     * Unit test of {@link ProxyPreference#ProxyPreference}.
     */
    @Test
    void testProxyPreference() {
        assertNotNull(new ProxyPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ProxyPreference#addGui}.
     */
    @Test
    void testAddGui() {
        assertDoesNotThrow(() -> PreferencesTestUtils.doTestPreferenceSettingAddGui(new ProxyPreference.Factory(), null));
    }
}
