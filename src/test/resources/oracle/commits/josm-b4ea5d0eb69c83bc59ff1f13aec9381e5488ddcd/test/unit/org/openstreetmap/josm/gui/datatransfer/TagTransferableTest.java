// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.gui.datatransfer.data.TagTransferData;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link TagTransferable} class.
 */
class TagTransferableTest {
    /**
     * Test of {@link TagTransferable#isDataFlavorSupported} method.
     */
    @Test
    void testIsDataFlavorSupported() {
        TagTransferable tt = new TagTransferable(null);
        assertTrue(tt.isDataFlavorSupported(TagTransferData.FLAVOR));
        assertTrue(tt.isDataFlavorSupported(DataFlavor.stringFlavor));
        assertFalse(tt.isDataFlavorSupported(DataFlavor.imageFlavor));
        assertFalse(tt.isDataFlavorSupported(null));
    }

    /**
     * Test of {@link RelationMemberTransferable#getTransferData} method - nominal case.
     * @throws Exception if an error occurs
     */
    @Test
    void testGetTransferDataNominal() throws Exception {
        Map<String, String> tags = new HashMap<>();
        tags.put("foo", "bar");
        TagTransferable tt = new TagTransferable(new TagTransferData(tags));
        assertEquals("foo=bar", tt.getTransferData(DataFlavor.stringFlavor));
        assertEquals(tags, ((TagTransferData) tt.getTransferData(TagTransferData.FLAVOR)).getTags());
    }

    /**
     * Test of {@link TagTransferable#getTransferData} method - error case.
     */
    @Test
    void testGetTransferDataError() {
        assertThrows(UnsupportedFlavorException.class, () -> new TagTransferable(null).getTransferData(null));
    }
}
