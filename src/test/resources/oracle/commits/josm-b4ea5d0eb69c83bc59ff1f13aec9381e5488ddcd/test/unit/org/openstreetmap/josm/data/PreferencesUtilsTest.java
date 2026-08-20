// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import net.trajano.commons.testing.UtilityClassTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for class {@link PreferencesUtils}.
 */
@BasicPreferences
class PreferencesUtilsTest {
    /**
     * Setup test.
     */
    @BeforeEach
    public void setUp() {
        PreferencesUtils.resetLog();
    }

    /**
     * Test method for {@link PreferencesUtils#log}.
     */
    @Test
    void testLog() {
        assertEquals("", PreferencesUtils.getLog());
        PreferencesUtils.log("test");
        assertEquals("test\n", PreferencesUtils.getLog());
        PreferencesUtils.log("%d\n", 100);
        assertEquals("test\n100\n", PreferencesUtils.getLog());
        PreferencesUtils.log("test");
        assertEquals("test\n100\ntest\n", PreferencesUtils.getLog());
    }

    /**
     * Tests that {@code PreferencesUtils} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(PreferencesUtils.class);
    }
}
