// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JTable;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link MemberTableLinkedCellRenderer} class.
 */
class MemberTableLinkedCellRendererTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

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
