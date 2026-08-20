// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.CustomProjection.Polarity;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CustomProjection}.
 * @author Michael Zangl
 */
// Need pref to load pref
@BasicPreferences
class CustomProjectionTest {
    /**
     * Test {@link CustomProjection#parseAngle(String, String)}
     * @throws ProjectionConfigurationException in case of error
     */
    @Test
    void testParseAngle() throws ProjectionConfigurationException {
        assertEquals(0, CustomProjection.parseAngle("0", "xxx"), 1e-10);
        assertEquals(1, CustomProjection.parseAngle("1", "xxx"), 1e-10);
        assertEquals(1.1, CustomProjection.parseAngle("1.1", "xxx"), 1e-10);

        assertEquals(1, CustomProjection.parseAngle("1d", "xxx"), 1e-10);
        assertEquals(1.1, CustomProjection.parseAngle("1.1d", "xxx"), 1e-10);

        assertEquals(1 / 60.0, CustomProjection.parseAngle("1'", "xxx"), 1e-10);
        assertEquals(1.1 / 60.0, CustomProjection.parseAngle("1.1'", "xxx"), 1e-10);

        assertEquals(1 / 3600.0, CustomProjection.parseAngle("1\"", "xxx"), 1e-10);
        assertEquals(1.1 / 3600.0, CustomProjection.parseAngle("1.1\"", "xxx"), 1e-10);

        // negate
        assertEquals(-1.1, CustomProjection.parseAngle("-1.1", "xxx"), 1e-10);
        assertEquals(1.1, CustomProjection.parseAngle("1.1N", "xxx"), 1e-10);
        assertEquals(1.1, CustomProjection.parseAngle("1.1E", "xxx"), 1e-10);
        assertEquals(-1.1, CustomProjection.parseAngle("1.1S", "xxx"), 1e-10);
        assertEquals(-1.1, CustomProjection.parseAngle("1.1W", "xxx"), 1e-10);
        assertEquals(-1.1, CustomProjection.parseAngle("-1.1N", "xxx"), 1e-10);
        assertEquals(-1.1, CustomProjection.parseAngle("-1.1E", "xxx"), 1e-10);
        assertEquals(1.1, CustomProjection.parseAngle("-1.1S", "xxx"), 1e-10);
        assertEquals(1.1, CustomProjection.parseAngle("-1.1W", "xxx"), 1e-10);

        // combine
        assertEquals(1.1 + 3 / 60.0 + 5.2 / 3600.0, CustomProjection.parseAngle("1.1d3'5.2\"", "xxx"), 1e-10);

        assertEquals(1.1, CustomProjection.parseAngle("1.1dN", "xxx"), 1e-10);
        assertEquals(-1.1, CustomProjection.parseAngle("1.1dS", "xxx"), 1e-10);
        assertEquals(1.1, CustomProjection.parseAngle("1.1dE", "xxx"), 1e-10);
        assertEquals(-1.1, CustomProjection.parseAngle("1.1dW", "xxx"), 1e-10);

        assertEquals(49.5, CustomProjection.parseAngle("49d30'N", "xxx"), 1e-10);
        assertEquals(-120.8333333333, CustomProjection.parseAngle("120.0d50'W", "xxx"), 1e-10);

        // fail
        Stream.of("", "-", "-N", "N", "1.1 ", "x", "1.1d1.1d", "1.1e", "1.1.1", ".1", "1.1d3\"5.2'").forEach(
                s -> {
                    try {
                        CustomProjection.parseAngle(s, "xxxx");
                        fail("Expected exception for " + s);
                    } catch (ProjectionConfigurationException e) {
                        // good!
                        assertTrue(e.getMessage().contains("xxx"));
                    }
                });
    }

    /**
     * Test {@link CustomProjection.Polarity}.
     */
    @Test
    void testPolarity() {
        assertEquals(LatLon.NORTH_POLE, Polarity.NORTH.getLatLon());
        assertEquals(LatLon.SOUTH_POLE, Polarity.SOUTH.getLatLon());
    }
}
