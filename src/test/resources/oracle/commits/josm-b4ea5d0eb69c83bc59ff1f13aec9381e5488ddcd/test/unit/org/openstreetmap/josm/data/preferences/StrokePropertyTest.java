// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.BasicStroke;

import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Test {@link StrokeProperty}
 * @author Michael Zangl
 */
// This is a preference test
@BasicPreferences
class StrokePropertyTest {
    /**
     * Test {@link StrokeProperty#get()}
     */
    @Test
    void testGetValue() {
        StrokeProperty property = new StrokeProperty("x", "1");

        Config.getPref().put("x", "11");
        BasicStroke bs = property.get();
        assertWide(bs);
        assertEquals(11, bs.getLineWidth(), 1e-10);
        assertEquals(null, bs.getDashArray());

        Config.getPref().put("x", ".5");
        bs = property.get();
        assertThin(bs);
        assertEquals(.5, bs.getLineWidth(), 1e-10);
        assertEquals(null, bs.getDashArray());

        Config.getPref().put("x", "2 1");
        bs = property.get();
        assertWide(bs);
        assertEquals(2, bs.getLineWidth(), 1e-10);
        assertArrayEquals(new float[] {1}, bs.getDashArray(), 1e-10f);

        Config.getPref().put("x", "2 0.1 1 10");
        bs = property.get();
        assertWide(bs);
        assertEquals(2, bs.getLineWidth(), 1e-10);
        assertArrayEquals(new float[] {0.1f, 1, 10}, bs.getDashArray(), 1e-10f);

        Config.getPref().put("x", "x");
        bs = property.get();
        assertThin(bs);
        assertEquals(1, bs.getLineWidth(), 1e-10);
        assertEquals(null, bs.getDashArray());

        // ignore dashes
        Config.getPref().put("x", "11 0 0 0.0001");
        bs = property.get();
        assertWide(bs);
        assertEquals(11, bs.getLineWidth(), 1e-10);
        assertEquals(null, bs.getDashArray());
    }

    /**
     * Test {@link StrokeProperty#put(BasicStroke)}
     */
    @Test
    void testPutValue() {
        StrokeProperty property = new StrokeProperty("x", new BasicStroke(12));
        BasicStroke bs = property.get();

        assertWide(bs);
        assertEquals(12, bs.getLineWidth(), 1e-10);
        assertEquals(null, bs.getDashArray());

        property.put(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[] {0.1f, 1, 10}, 0));
        bs = property.get();
        assertWide(bs);
        assertEquals(2, bs.getLineWidth(), 1e-10);
        assertArrayEquals(new float[] {0.1f, 1, 10}, bs.getDashArray(), 1e-10f);
    }

    private static void assertThin(BasicStroke bs) {
        assertBase(bs);
        assertEquals(BasicStroke.CAP_BUTT, bs.getEndCap());
        assertEquals(BasicStroke.JOIN_MITER, bs.getLineJoin());
    }

    private static void assertWide(BasicStroke bs) {
        assertBase(bs);
        assertEquals(BasicStroke.CAP_ROUND, bs.getEndCap());
        assertEquals(BasicStroke.JOIN_ROUND, bs.getLineJoin());
    }

    private static void assertBase(BasicStroke bs) {
        assertEquals(10, bs.getMiterLimit(), 1e-10);
        assertEquals(0, bs.getDashPhase(), 1e-10);
    }
}
