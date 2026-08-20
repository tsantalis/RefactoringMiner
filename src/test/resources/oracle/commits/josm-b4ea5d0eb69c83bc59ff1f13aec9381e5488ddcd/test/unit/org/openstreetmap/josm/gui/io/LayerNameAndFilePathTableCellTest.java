// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import javax.swing.JTable;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link LayerNameAndFilePathTableCell} class.
 */
@BasicPreferences
class LayerNameAndFilePathTableCellTest {
    /**
     * Test of {@link LayerNameAndFilePathTableCell} class.
     */
    @Test
    void testLayerNameAndFilePathTableCell() {
        JTable table = new JTable();
        File file = new File("test");
        String name = "layername";
        AbstractModifiableLayer layer = new OsmDataLayer(new DataSet(), name, file);
        SaveLayerInfo value = new SaveLayerInfo(layer);
        LayerNameAndFilePathTableCell c = new LayerNameAndFilePathTableCell();
        assertEquals(c, c.getTableCellEditorComponent(table, value, false, 0, 0));
        assertEquals(c, c.getTableCellRendererComponent(table, value, false, false, 0, 0));
        assertTrue(c.isCellEditable(null));
        assertTrue(c.shouldSelectCell(null));
        assertNull(c.getCellEditorValue());
        assertTrue(c.stopCellEditing());
        assertEquals(file, c.getCellEditorValue());
    }
}
