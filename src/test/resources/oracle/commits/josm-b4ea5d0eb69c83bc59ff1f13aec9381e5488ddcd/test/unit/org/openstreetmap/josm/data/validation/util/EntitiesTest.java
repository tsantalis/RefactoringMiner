// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.util;

import net.trajano.commons.testing.UtilityClassTestUtil;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for class {@link Entities}.
 */
class EntitiesTest {
    /**
     * Tests that {@code Entities} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(Entities.class);
    }
}
