// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link CredentialsAgentException} class.
 */
class CredentialsAgentExceptionTest {
    /**
     * Unit test of {@code CredentialsAgentException#CredentialsAgentException}
     */
    @Test
    void testCredentialsAgentException() {
        String msg = "test1";
        Exception cause = new Exception("test2");
        assertEquals(msg, new CredentialsAgentException(msg).getMessage());
        assertEquals(cause, new CredentialsAgentException(cause).getCause());
        CredentialsAgentException exc = new CredentialsAgentException(msg, cause);
        assertEquals(msg, exc.getMessage());
        assertEquals(cause, exc.getCause());
    }
}
