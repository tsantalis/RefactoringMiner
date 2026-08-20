// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JList;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.Instant;

/**
 * Unit tests of {@link ChangesetCellRenderer} class.
 */
class ChangesetCellRendererTest {
    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test of {@link ChangesetCellRenderer} class.
     */
    @Test
    void testChangesetCellRenderer() {
        JList<Changeset> list = new JList<>();
        Changeset cs = new Changeset();
        cs.setCreatedAt(Instant.EPOCH);
        ChangesetCellRenderer c = new ChangesetCellRenderer();
        assertEquals(c, c.getListCellRendererComponent(list, cs, 0, false, false));
    }
}
