// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Collectors;

import net.trajano.commons.testing.UtilityClassTestUtil;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link StreamUtils} class.
 */
class StreamUtilsTest {
    /**
     * Tests that {@code StreamUtils} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(StreamUtils.class);
    }

    /**
     * Tests {@link StreamUtils#reversedStream(java.util.List)}
     */
    @Test
    void testReverseStream() {
        assertEquals("baz/bar/foo",
                StreamUtils.reversedStream(Arrays.asList("foo", "bar", "baz")).collect(Collectors.joining("/")));
    }
}
