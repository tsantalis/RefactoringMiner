// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Insets;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.conflict.tags.PasteTagsConflictResolverDialog.StatisticsInfo;
import org.openstreetmap.josm.gui.conflict.tags.PasteTagsConflictResolverDialog.StatisticsInfoTable;
import org.openstreetmap.josm.gui.conflict.tags.PasteTagsConflictResolverDialog.StatisticsTableModel;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link PasteTagsConflictResolverDialog} class.
 */
class PasteTagsConflictResolverDialogTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link PasteTagsConflictResolverDialog#PANE_TITLES}.
     */
    @Test
    void testPaneTitles() {
        assertNotNull(PasteTagsConflictResolverDialog.PANE_TITLES);
        assertNotNull(PasteTagsConflictResolverDialog.PANE_TITLES.get(OsmPrimitiveType.NODE));
        assertNotNull(PasteTagsConflictResolverDialog.PANE_TITLES.get(OsmPrimitiveType.WAY));
        assertNotNull(PasteTagsConflictResolverDialog.PANE_TITLES.get(OsmPrimitiveType.RELATION));
    }

    /**
     * Unit test of {@link PasteTagsConflictResolverDialog.StatisticsInfoTable} class.
     */
    @Test
    void testStatisticsInfoTable() {
        StatisticsInfo info = new StatisticsInfo();
        StatisticsTableModel model = new StatisticsTableModel();
        assertFalse(model.isCellEditable(0, 0));
        assertEquals(1, model.getRowCount());
        model.append(info);
        assertEquals(2, model.getRowCount());
        assertEquals("Paste ...", model.getValueAt(0, 0));
        assertEquals(info, model.getValueAt(1, 0));
        assertNull(model.getValueAt(2, 0));
        model.reset();
        assertEquals(1, model.getRowCount());
        assertEquals(new Insets(0, 0, 20, 0), new StatisticsInfoTable(model).getInsets());
    }
}
