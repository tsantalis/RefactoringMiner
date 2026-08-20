// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests for class {@link PreferencesUtils}.
 */
class PreferencesUtilsTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

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
