// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ManualAuthorizationUI} class.
 */
@BasicPreferences
class ManualAuthorizationUITest {
    /**
     * Unit test of {@link ManualAuthorizationUI#ManualAuthorizationUI}.
     */
    @Test
    void testManualAuthorizationUI() {
        assertNotNull(new ManualAuthorizationUI("", MainApplication.worker));
    }
}
