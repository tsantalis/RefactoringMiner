// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link PluginException} class.
 */
class PluginExceptionTest {
    /**
     * Unit test of {@link PluginException#PluginException}.
     */
    @Test
    void testPluginDownloadException() {
        PluginException ex = new PluginException("foo");
        assertEquals("foo", ex.getMessage());
        NullPointerException npe = new NullPointerException();
        ex = new PluginException("bar", npe);
        assertEquals("An error occurred in plugin bar", ex.getMessage());
        assertEquals(npe, ex.getCause());
        ex = new PluginException(null, "foobar", npe);
        assertEquals("An error occurred in plugin foobar", ex.getMessage());
        assertEquals(npe, ex.getCause());
    }
}
