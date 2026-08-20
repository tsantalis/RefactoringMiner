// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.util.WindowGeometry.WindowGeometryException;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link WindowGeometry} class.
 */
class WindowGeometryTest {
    /**
     * Some of this depends on preferences.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test of {@link WindowGeometry#centerInWindow} method.
     */
    @Test
    void testCenterInWindow() {
        assertNotNull(WindowGeometry.centerInWindow(null, null));
        assertNotNull(WindowGeometry.centerInWindow(new JPanel(), null));
    }

    /**
     * Test of {@link WindowGeometry#centerOnScreen} method.
     */
    @Test
    void testCenterOnScreen() {
        Dimension dim = new Dimension(200, 100);
        assertEquals(new WindowGeometry(new Point(0, 0), dim), WindowGeometry.centerOnScreen(dim));
        assertEquals(new WindowGeometry(new Point(300, 250), dim), WindowGeometry.centerOnScreen(dim, null));

        Config.getPref().put(WindowGeometry.PREF_KEY_GUI_GEOMETRY, "x=0,y=0,width=800,height=600");
        assertEquals(new WindowGeometry(new Point(300, 250), dim), WindowGeometry.centerOnScreen(dim));
    }

    /**
     * Test of {@link WindowGeometry.WindowGeometryException} class.
     */
    @Test
    void testWindowGeometryException1() {
        Config.getPref().put("test", null);
        assertThrows(WindowGeometryException.class, () -> new WindowGeometry("test"));
    }

    /**
     * Test of {@link WindowGeometry.WindowGeometryException} class.
     */
    @Test
    void testWindowGeometryException2() {
        Config.getPref().put("test", "");
        assertThrows(WindowGeometryException.class, () -> new WindowGeometry("test"));
    }

    /**
     * Test of {@link WindowGeometry.WindowGeometryException} class.
     */
    @Test
    void testWindowGeometryException3() {
        Config.getPref().put("test", "x=not_a_number");
        assertThrows(WindowGeometryException.class, () -> new WindowGeometry("test"));
    }

    /**
     * Test of {@link WindowGeometry.WindowGeometryException} class.
     */
    @Test
    void testWindowGeometryException4() {
        Config.getPref().put("test", "wrong_pattern");
        assertThrows(WindowGeometryException.class, () -> new WindowGeometry("test"));
    }

    /**
     * Test of {@link WindowGeometry.WindowGeometryException} class.
     * @throws WindowGeometryException never
     */
    @Test
    void testWindowGeometryException5() throws WindowGeometryException {
        Config.getPref().put("test", "x=15,y=55,width=200,height=100");
        assertNotNull(new WindowGeometry("test"));
    }

    /**
     * Test of {@link WindowGeometry#isBugInMaximumWindowBounds} method.
     */
    @Test
    void testIsBugInMaximumWindowBounds() {
        assertFalse(WindowGeometry.isBugInMaximumWindowBounds(new Rectangle(10, 10)));
        assertTrue(WindowGeometry.isBugInMaximumWindowBounds(new Rectangle(10, 0)));
        assertTrue(WindowGeometry.isBugInMaximumWindowBounds(new Rectangle(0, 10)));
    }

    /**
     * Test of {@link WindowGeometry#getVirtualScreenBounds} method.
     */
    @Test
    void testGetVirtualScreenBounds() {
        assertNotNull(WindowGeometry.getVirtualScreenBounds());
    }

    /**
     * Test of {@link WindowGeometry#getMaxDimensionOnScreen} method.
     */
    @Test
    void testGetMaxDimensionOnScreen() {
        assertNotNull(WindowGeometry.getMaxDimensionOnScreen(new JLabel()));
    }

    /**
     * Test of {@link WindowGeometry#toString} method.
     */
    @Test
    void testToString() {
        assertEquals("WindowGeometry{topLeft=java.awt.Point[x=0,y=0],extent=java.awt.Dimension[width=0,height=0]}",
                new WindowGeometry(new Rectangle()).toString());
    }

    /**
     * Unit test of methods {@link WindowGeometry#equals} and {@link WindowGeometry#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(WindowGeometry.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
