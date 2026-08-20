// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test {@link NamedColorProperty}
 * @author Michael Zangl
 */
class NamedColorPropertyTest {
    /**
     * This is a preference test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();
    private NamedColorProperty base;

    /**
     * Set up test case
     */
    @BeforeEach
    public void createTestProperty() {
        base = new NamedColorProperty("test", Color.RED);
    }

    /**
     * Test {@link NamedColorProperty#get()}
     */
    @Test
    void testGet() {
        assertEquals(Color.RED, base.get());

        UIManager.put("JOSM.clr.general.test", Color.GRAY);
        base = new NamedColorProperty("test", Color.RED);
        assertEquals(Color.GRAY, base.get());

        Config.getPref().putList("clr.general.test", Collections.singletonList("#123456"));
        assertEquals(new Color(0x123456), base.get());

        Config.getPref().putList("clr.general.test", null);
        UIManager.put("JOSM.clr.general.test", null);
        base = new NamedColorProperty("test", Color.RED);
        assertEquals(Color.RED, base.get());
    }

    /**
     * Test {@link NamedColorProperty#put}
     */
    @Test
    void testPut() {
        assertEquals(Color.RED, base.get());

        base.put(new Color(0xff00af00));
        assertEquals(new Color(0xff00af00), base.get());
        assertEquals("#00af00", Config.getPref().getList("clr.general.test").get(0).toLowerCase());

        base.put(null);
        assertEquals(Color.RED, base.get());
    }

    /**
     * Test color alpha.
     */
    @Test
    void testColorAlpha() {
        assertEquals(0x12, new NamedColorProperty("foo", new Color(0x12345678, true)).get().getAlpha());
        assertTrue(Preferences.main().putList("clr.general.bar", Arrays.asList("#34567812", "general", "", "bar")));
        assertEquals(0x12, new NamedColorProperty("bar", Color.RED).get().getAlpha());
    }

    /**
     * Test color name and alpha.
     */
    @Test
    void testColorNameAlpha() {
        assertEquals(0x12, new NamedColorProperty("foo", new Color(0x12345678, true)).get().getAlpha());
    }

    /**
     * Test {@link NamedColorProperty#getChildColor(String)}
     */
    @Test
    void testGetChildColor() {
        AbstractProperty<Color> child = base.getChildColor("test2");

        assertEquals(Color.RED, child.get());

        base.put(Color.GREEN);
        assertEquals(Color.GREEN, child.get());

        child.put(Color.YELLOW);
        assertEquals(Color.YELLOW, child.get());
        assertEquals(Color.GREEN, base.get());

        child.put(null);
        assertEquals(Color.GREEN, child.get());
    }
}
