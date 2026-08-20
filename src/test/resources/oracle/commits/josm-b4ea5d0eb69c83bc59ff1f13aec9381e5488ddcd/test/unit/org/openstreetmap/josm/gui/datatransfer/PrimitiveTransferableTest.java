// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.gui.datatransfer.data.PrimitiveTransferData;
import org.openstreetmap.josm.gui.datatransfer.data.TagTransferData;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link PrimitiveTransferable} class.
 */
// Only needed for OSM primitives
@BasicPreferences
class PrimitiveTransferableTest {
    /**
     * Test of {@link PrimitiveTransferable#getTransferDataFlavors()} method response order
     */
    @Test
    void testGetTransferDataFlavors() {
        List<DataFlavor> flavors = Arrays.asList(new PrimitiveTransferable(null).getTransferDataFlavors());
        int ptd = flavors.indexOf(PrimitiveTransferData.DATA_FLAVOR);
        int tags = flavors.indexOf(TagTransferData.FLAVOR);
        int string = flavors.indexOf(DataFlavor.stringFlavor);

        assertTrue(ptd >= 0);
        assertTrue(tags >= 0);
        assertTrue(string >= 0);

        assertTrue(ptd < tags);
        assertTrue(tags < string);
    }

    /**
     * Test of {@link PrimitiveTransferable#isDataFlavorSupported} method.
     */
    @Test
    void testIsDataFlavorSupported() {
        assertTrue(new PrimitiveTransferable(null).isDataFlavorSupported(PrimitiveTransferData.DATA_FLAVOR));
        assertFalse(new PrimitiveTransferable(null).isDataFlavorSupported(DataFlavor.imageFlavor));
    }

    /**
     * Test of {@link PrimitiveTransferable#getTransferData} method - nominal case.
     * @throws UnsupportedFlavorException never
     */
    @Test
    void testGetTransferDataNominal() throws UnsupportedFlavorException {
        PrimitiveTransferData data = PrimitiveTransferData.getData(Collections.singleton(new Node(1)));
        PrimitiveTransferable pt = new PrimitiveTransferable(data);
        assertEquals("node 1", pt.getTransferData(DataFlavor.stringFlavor));
        Collection<PrimitiveData> td = ((PrimitiveTransferData) pt.getTransferData(PrimitiveTransferData.DATA_FLAVOR)).getAll();
        assertEquals(1, td.size());
        assertTrue(td.iterator().next() instanceof NodeData);


        data = PrimitiveTransferData.getData(Arrays.asList(new Node(1), new Node(2)));
        pt = new PrimitiveTransferable(data);
        assertEquals("node 1\nnode 2", pt.getTransferData(DataFlavor.stringFlavor));
    }

    /**
     * Test of {@link PrimitiveTransferable#getTransferData} method - error case.
     */
    @Test
    void testGetTransferDataError() {
        PrimitiveTransferData data = PrimitiveTransferData.getData(Collections.singleton(new Node(1)));
        assertThrows(UnsupportedFlavorException.class, () -> new PrimitiveTransferable(data).getTransferData(DataFlavor.imageFlavor));
    }
}
