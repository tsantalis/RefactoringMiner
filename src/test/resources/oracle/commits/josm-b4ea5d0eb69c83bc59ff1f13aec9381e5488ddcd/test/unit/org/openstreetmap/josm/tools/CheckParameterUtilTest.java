// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import net.trajano.commons.testing.UtilityClassTestUtil;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link CheckParameterUtil} class.
 */
class CheckParameterUtilTest {
    /**
     * Tests that {@code CheckParameterUtil} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(CheckParameterUtil.class);
    }
}
