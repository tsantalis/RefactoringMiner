// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetHandler;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link MemberTableModel} class.
 */
@BasicPreferences
class MemberTableModelTest {
    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/12443">#12443</a>.
     */
    @Test
    void testTicket12443() {
        final Node n = new Node(1);
        assertNotNull(new MemberTableModel(null, null, new TaggingPresetHandler() {
            @Override
            public void updateTags(List<Tag> tags) {
                // Do nothing
            }

            @Override
            public Collection<OsmPrimitive> getSelection() {
                return Collections.<OsmPrimitive>singleton(n);
            }
        }).getRelationMemberForPrimitive(n));
    }
}
