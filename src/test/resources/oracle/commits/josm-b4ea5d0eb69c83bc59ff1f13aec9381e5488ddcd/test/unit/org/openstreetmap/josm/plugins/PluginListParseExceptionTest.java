// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link PluginListParseException} class.
 */
class PluginListParseExceptionTest {
    /**
     * Unit test of {@link PluginListParseException#PluginListParseException}.
     */
    @Test
    void testPluginListParseException() {
        NullPointerException npe = new NullPointerException();
        PluginListParseException ex = new PluginListParseException(npe);
        assertEquals(npe, ex.getCause());
        ex = new PluginListParseException("bar", npe);
        assertEquals("bar", ex.getMessage());
        assertEquals(npe, ex.getCause());
    }
}
