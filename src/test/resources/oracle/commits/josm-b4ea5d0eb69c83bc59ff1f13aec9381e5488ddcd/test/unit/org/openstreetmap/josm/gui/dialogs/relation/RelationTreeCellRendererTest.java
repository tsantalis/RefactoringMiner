// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JTree;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link RelationTreeCellRenderer} class.
 */
@BasicPreferences
class RelationTreeCellRendererTest {
    /**
     * Unit test of {@link RelationTreeCellRenderer#RelationTreeCellRenderer}.
     */
    @Test
    void testRelationTreeCellRenderer() {
        RelationTreeCellRenderer r = new RelationTreeCellRenderer();
        assertEquals(r, r.getTreeCellRendererComponent(new JTree(), new Relation(), false, false, false, 0, false));
    }
}
