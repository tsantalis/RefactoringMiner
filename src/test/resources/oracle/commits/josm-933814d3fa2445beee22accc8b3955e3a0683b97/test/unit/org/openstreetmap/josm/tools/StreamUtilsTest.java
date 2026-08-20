// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link StreamUtils} class.
 */
class StreamUtilsTest {

    /**
     * Setup rule.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

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
