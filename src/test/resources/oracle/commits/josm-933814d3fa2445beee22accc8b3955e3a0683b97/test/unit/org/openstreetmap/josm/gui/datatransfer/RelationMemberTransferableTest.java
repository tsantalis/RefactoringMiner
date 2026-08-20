// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openstreetmap.josm.gui.datatransfer.RelationMemberTransferable.RELATION_MEMBER_DATA;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.gui.datatransfer.data.PrimitiveTransferData;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link RelationMemberTransferable} class.
 */
class RelationMemberTransferableTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test of {@link RelationMemberTransferable#getTransferDataFlavors()} method.
     */
    @Test
    void testGetTransferDataFlavors() {
        DataFlavor[] flavors = new RelationMemberTransferable(Collections.<RelationMember>emptyList()).getTransferDataFlavors();
        assertEquals(3, flavors.length);
        assertEquals(RELATION_MEMBER_DATA, flavors[0]);
        assertEquals(PrimitiveTransferData.DATA_FLAVOR, flavors[1]);
        assertEquals(DataFlavor.stringFlavor, flavors[2]);
    }

    /**
     * Test of {@link RelationMemberTransferable#isDataFlavorSupported} method.
     */
    @Test
    void testIsDataFlavorSupported() {
        RelationMemberTransferable transferable = new RelationMemberTransferable(Collections.<RelationMember>emptyList());
        assertTrue(transferable.isDataFlavorSupported(RELATION_MEMBER_DATA));
        assertTrue(transferable.isDataFlavorSupported(PrimitiveTransferData.DATA_FLAVOR));
        assertFalse(transferable.isDataFlavorSupported(null));
    }

    /**
     * Test of {@link RelationMemberTransferable#getTransferData} method - nominal case.
     * @throws UnsupportedFlavorException never
     */
    @Test
    void testGetTransferDataNominal() throws UnsupportedFlavorException {
        RelationMemberTransferable rmt = new RelationMemberTransferable(Collections.singleton(new RelationMember("test", new Node(1))));
        assertEquals("node 1 test # incomplete\n", rmt.getTransferData(DataFlavor.stringFlavor));
        Collection<RelationMemberData> td = ((RelationMemberTransferable.Data) rmt.getTransferData(RELATION_MEMBER_DATA))
                .getRelationMemberData();
        assertEquals(1, td.size());
        assertEquals(1, td.iterator().next().getMemberId());
        assertEquals("test", td.iterator().next().getRole());

        Collection<PrimitiveData> primitives = ((PrimitiveTransferData) rmt.getTransferData(PrimitiveTransferData.DATA_FLAVOR))
                .getDirectlyAdded();
        assertEquals(1, primitives.size());
        assertEquals(1, primitives.iterator().next().getId());
    }

    /**
     * Test of {@link RelationMemberTransferable#getTransferData} method - error case.
     */
    @Test
    void testGetTransferDataError() {
        assertThrows(UnsupportedFlavorException.class,
                () -> new RelationMemberTransferable(Collections.singleton(new RelationMember(null, new Node(1)))).getTransferData(null));
    }
}
