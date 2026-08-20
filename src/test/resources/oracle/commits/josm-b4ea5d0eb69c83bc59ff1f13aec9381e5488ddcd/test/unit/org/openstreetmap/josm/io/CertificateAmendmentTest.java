// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import net.trajano.commons.testing.UtilityClassTestUtil;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link CertificateAmendment} class.
 */
@BasicPreferences
class CertificateAmendmentTest {
    /**
     * Tests that {@code CertificateAmendment} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(CertificateAmendment.class);
    }
}
