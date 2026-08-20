// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link SelectionTableCellRenderer} class.
 */
@BasicPreferences
class SelectionTableCellRendererTest {
    /**
     * Unit test of {@link SelectionTableCellRenderer#SelectionTableCellRenderer}.
     */
    @Test
    void testSelectionTableCellRenderer() {
        MemberTableModel model = new MemberTableModel(null, null, null);
        SelectionTableCellRenderer r = new SelectionTableCellRenderer(model);
        assertEquals(r, r.getTableCellRendererComponent(null, null, false, false, 0, 0));
        assertEquals(r, r.getTableCellRendererComponent(new JTable(model), new Node(), false, false, 0, 0));
    }
}
