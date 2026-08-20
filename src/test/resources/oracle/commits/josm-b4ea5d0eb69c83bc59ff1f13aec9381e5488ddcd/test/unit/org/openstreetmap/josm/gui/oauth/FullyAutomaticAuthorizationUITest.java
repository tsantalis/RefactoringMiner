// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link FullyAutomaticAuthorizationUI} class.
 */
@BasicPreferences
class FullyAutomaticAuthorizationUITest {
    /**
     * Unit test of {@link FullyAutomaticAuthorizationUI#FullyAutomaticAuthorizationUI}.
     */
    @Test
    void testFullyAutomaticAuthorizationUI() {
        assertNotNull(new FullyAutomaticAuthorizationUI("", MainApplication.worker));
    }
}
