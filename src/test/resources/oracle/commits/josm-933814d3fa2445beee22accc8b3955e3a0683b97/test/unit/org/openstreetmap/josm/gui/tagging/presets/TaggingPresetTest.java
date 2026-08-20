// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.gui.tagging.presets.items.Key;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests of {@code TaggingPreset}
 */
class TaggingPresetTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Tests {@link TaggingPreset#test(IPrimitive)}
     */
    @Test
    void test() throws Exception {
        Key key = new Key();
        key.key = "railway";
        key.value = "tram_stop";
        TaggingPreset preset = new TaggingPreset();
        preset.data.add(key);

        assertFalse(preset.test(OsmUtils.createPrimitive("node foo=bar")));
        assertTrue(preset.test(OsmUtils.createPrimitive("node railway=tram_stop")));

        preset.types = EnumSet.of(TaggingPresetType.NODE);
        assertTrue(preset.test(OsmUtils.createPrimitive("node railway=tram_stop")));
        assertFalse(preset.test(OsmUtils.createPrimitive("way railway=tram_stop")));

        preset.matchExpression = SearchCompiler.compile("-public_transport");
        assertTrue(preset.test(OsmUtils.createPrimitive("node railway=tram_stop")));
        assertFalse(preset.test(OsmUtils.createPrimitive("node railway=tram_stop public_transport=stop_position")));
    }
}
