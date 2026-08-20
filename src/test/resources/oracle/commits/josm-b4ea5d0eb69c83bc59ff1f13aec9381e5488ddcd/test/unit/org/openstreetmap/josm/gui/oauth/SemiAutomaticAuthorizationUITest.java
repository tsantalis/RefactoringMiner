// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link SemiAutomaticAuthorizationUI} class.
 */
@BasicPreferences
class SemiAutomaticAuthorizationUITest {
    /**
     * Unit test of {@link SemiAutomaticAuthorizationUI#SemiAutomaticAuthorizationUI}.
     */
    @Test
    void testSemiAutomaticAuthorizationUI() {
        assertNotNull(new SemiAutomaticAuthorizationUI("", MainApplication.worker));
    }
}
