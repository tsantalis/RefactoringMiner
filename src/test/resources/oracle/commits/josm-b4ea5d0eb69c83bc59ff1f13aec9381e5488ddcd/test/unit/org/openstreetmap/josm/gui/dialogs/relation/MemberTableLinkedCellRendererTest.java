// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JTable;

import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link MemberTableLinkedCellRenderer} class.
 */
@BasicPreferences
class MemberTableLinkedCellRendererTest {
    /**
     * Unit test of {@link MemberTableLinkedCellRenderer#MemberTableLinkedCellRenderer}.
     */
    @Test
    void testMemberTableLinkedCellRenderer() {
        MemberTableLinkedCellRenderer r = new MemberTableLinkedCellRenderer();
        assertEquals(r, r.getTableCellRendererComponent(null, null, false, false, 0, 0));
        r.paintComponent(TestUtils.newGraphics());
        assertEquals(r, r.getTableCellRendererComponent(
                new JTable(new MemberTableModel(null, null, null)),
                new WayConnectionType(), false, false, 0, 0));
        r.paintComponent(TestUtils.newGraphics());
    }
}
