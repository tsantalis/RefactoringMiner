// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.autofilter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AutoFilterRule} class.
 */
class AutoFilterRuleTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().i18n();

    /**
     * Unit test of {@link AutoFilterRule#getTagValuesForPrimitive}.
     */
    @Test
    void testTagValuesForPrimitive() {
        // #17109, support values like 0.5 or 1.5 - level values are multiplied by 2 when parsing, values are divided by 2 for formatting
        final AutoFilterRule level = AutoFilterRule.getDefaultRule("level").orElseThrow(NoSuchElementException::new);
        assertTagValuesForPrimitive(level, "way level=-4--5", -10, -9, -8);
        assertTagValuesForPrimitive(level, "way level=-2", -4);
        assertTagValuesForPrimitive(level, "node level=0", 0);
        assertTagValuesForPrimitive(level, "way level=1", 2);
        assertTagValuesForPrimitive(level, "way level=2;3", 4, 6);
        assertTagValuesForPrimitive(level, "way level=6-9", 12, 13, 14, 15, 16, 17, 18);
        assertTagValuesForPrimitive(level, "way level=10;12-13", 20, 24, 25, 26);
        assertTagValuesForPrimitive(level, "way level=0;0.5;1;1.5;2;2.5;3", 0, 1, 2, 3, 4, 5, 6);
        assertEquals("0 0.5 1 1.5 2 2.5 3",
                IntStream.of(0, 1, 2, 3, 4, 5, 6).mapToObj(level::formatValue).collect(Collectors.joining(" ")));
    }

    /**
     * Unit test of {@link AutoFilterRule#getTagValuesForPrimitive} to deal with {@code %} of key {@code incline}.
     */
    @Test
    void testTagValuesForPrimitiveInclineUnit() {
        final AutoFilterRule incline = AutoFilterRule.getDefaultRule("incline").orElseThrow(NoSuchElementException::new);
        assertTagValuesForPrimitive(incline, "way incline=up");
        assertTagValuesForPrimitive(incline, "way incline=20", 20);
        assertTagValuesForPrimitive(incline, "way incline=20%", 20);
    }

    /**
     * Unit test of {@link AutoFilterRule#getTagValuesForPrimitive} provides sensible defaults, see #17496.
     */
    @Test
    void testTagValuesForPrimitivesDefaults() {
        final AutoFilterRule layer = AutoFilterRule.getDefaultRule("layer").orElseThrow(NoSuchElementException::new);
        assertTagValuesForPrimitive(layer, "way foo=bar");
        assertTagValuesForPrimitive(layer, "way bridge=yes", 1);
        assertTagValuesForPrimitive(layer, "way power=line", 1);
        assertTagValuesForPrimitive(layer, "way tunnel=yes", -1);
        assertTagValuesForPrimitive(layer, "way tunnel=building_passage", 0);
        assertTagValuesForPrimitive(layer, "way highway=residential", 0);
        assertTagValuesForPrimitive(layer, "way railway=rail", 0);
        assertTagValuesForPrimitive(layer, "way waterway=canal", 0);
    }

    private void assertTagValuesForPrimitive(AutoFilterRule rule, String assertion, int... expected) {
        final OsmPrimitive primitive = OsmUtils.createPrimitive(assertion);
        final int[] actual = rule.getTagValuesForPrimitive(primitive).toArray();
        assertArrayEquals(expected, actual);
    }

    /**
     * Unit test of {@link AutoFilterRule#formatValue}
     */
    @Test
    void testValueFormatter() {
        final AutoFilterRule voltage = AutoFilterRule.getDefaultRule("voltage").orElseThrow(NoSuchElementException::new);
        assertEquals("230V", voltage.formatValue(230));
        assertEquals("1kV", voltage.formatValue(1000));
        assertEquals("15kV", voltage.formatValue(15000));
        assertEquals("380kV", voltage.formatValue(380000));
    }
}
