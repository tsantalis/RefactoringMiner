// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;


import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link ServerAccessPreference} class.
 */
@BasicPreferences
class ServerAccessPreferenceTest {
    /**
     * Unit test of {@link ServerAccessPreference#ServerAccessPreference}.
     */
    @Test
    void testServerAccessPreference() {
        assertNotNull(new ServerAccessPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ServerAccessPreference#addGui}.
     */
    @Test
    void testAddGui() {
        assertDoesNotThrow(() -> PreferencesTestUtils.doTestPreferenceSettingAddGui(new ServerAccessPreference.Factory(), null));
    }
}
