// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link PluginDownloadException} class.
 */
class PluginDownloadExceptionTest {
    /**
     * Unit test of {@link PluginDownloadException#PluginDownloadException}.
     */
    @Test
    void testPluginDownloadException() {
        PluginDownloadException ex = new PluginDownloadException("foo");
        assertEquals("foo", ex.getMessage());
        NullPointerException npe = new NullPointerException();
        ex = new PluginDownloadException(npe);
        assertEquals(npe, ex.getCause());
        ex = new PluginDownloadException("bar", npe);
        assertEquals("bar", ex.getMessage());
        assertEquals(npe, ex.getCause());
    }
}
