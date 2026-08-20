// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link MemberTableMemberCellRenderer} class.
 */
@BasicPreferences
class MemberTableMemberCellRendererTest {
    /**
     * Unit test of {@link MemberTableMemberCellRenderer#MemberTableMemberCellRenderer}.
     */
    @Test
    void testMemberTableMemberCellRenderer() {
        MemberTableMemberCellRenderer r = new MemberTableMemberCellRenderer();
        assertEquals(r, r.getTableCellRendererComponent(null, null, false, false, 0, 0));
        assertEquals(r, r.getTableCellRendererComponent(
                new JTable(new MemberTableModel(null, new OsmDataLayer(new DataSet(), "", null), null)),
                new Node(), false, false, 0, 0));
    }
}
