// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JTree;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link RelationTreeCellRenderer} class.
 */
class RelationTreeCellRendererTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link RelationTreeCellRenderer#RelationTreeCellRenderer}.
     */
    @Test
    void testRelationTreeCellRenderer() {
        RelationTreeCellRenderer r = new RelationTreeCellRenderer();
        assertEquals(r, r.getTreeCellRendererComponent(new JTree(), new Relation(), false, false, false, 0, false));
    }
}
