// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.io.CredentialDialog.CredentialPanel;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link CredentialDialog} class.
 */
@BasicPreferences
class CredentialDialogTest {
    /**
     * Test of {@link CredentialDialog.CredentialPanel} class.
     */
    @Test
    void testCredentialPanel() {
        CredentialPanel cp = new CredentialPanel(null);
        cp.build();

        cp.init(null, null);
        assertEquals("", cp.getUserName());
        assertArrayEquals("".toCharArray(), cp.getPassword());
        assertFalse(cp.isSaveCredentials());

        cp.init("user", "password");
        assertEquals("user", cp.getUserName());
        assertArrayEquals("password".toCharArray(), cp.getPassword());
        assertTrue(cp.isSaveCredentials());

        cp.updateWarningLabel(null);
        cp.updateWarningLabel("http://something_insecure");
        cp.updateWarningLabel("https://something_secure");

        cp.startUserInput();
    }
}
