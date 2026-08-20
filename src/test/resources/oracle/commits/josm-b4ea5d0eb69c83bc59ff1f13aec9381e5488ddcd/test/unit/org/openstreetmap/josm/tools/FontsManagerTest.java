// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Font;
import java.awt.GraphicsEnvironment;

import net.trajano.commons.testing.UtilityClassTestUtil;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link FontsManager} class.
 */
class FontsManagerTest {
    /**
     * Test method for {@code FontsManager#initialize}
     */
    @Test
    void testFontsManager() {
        FontsManager.initialize();
        boolean found = false;
        for (Font f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
            if (f.getName().contains("Droid")) {
                System.out.println(f);
                found = true;
            }
        }
        if (!found) {
            fail("DroidSans font not found");
        }
    }

    /**
     * Tests that {@code FontsManager} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(FontsManager.class);
    }
}
