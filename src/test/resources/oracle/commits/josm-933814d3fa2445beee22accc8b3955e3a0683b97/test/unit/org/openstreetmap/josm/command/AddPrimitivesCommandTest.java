// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link AddPrimitivesCommand} class.
 */
class AddPrimitivesCommandTest {

    /**
     * We need prefs for nodes.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().i18n();

    /**
     * Test if the add command is executed correctly and does not set the modified flag.
     */
    @Test
    void testAdd() {
        DataSet ds = new DataSet();

        List<PrimitiveData> testData = createTestData();
        assertTrue(new AddPrimitivesCommand(testData, ds).executeCommand());

        testContainsTestData(ds);
        assertEquals(3, ds.getAllSelected().size());
    }

    /**
     * Test if the add command sets the selection.
     */
    @Test
    void testAddSetSelection() {
        DataSet ds = new DataSet();

        List<PrimitiveData> testData = createTestData();
        assertTrue(new AddPrimitivesCommand(testData, testData.subList(2, 3), ds).executeCommand());

        testContainsTestData(ds);

        assertEquals(1, ds.getAllSelected().size());
        assertEquals(1, ds.getSelectedWays().size());
    }

    /**
     * Tests if the add command respects the data set.
     */
    @Test
    void testAddToLayer() {
        DataSet ds1 = new DataSet();
        DataSet ds2 = new DataSet();

        List<PrimitiveData> testData = createTestData();
        assertTrue(new AddPrimitivesCommand(testData, testData.subList(2, 3), ds1).executeCommand());

        testContainsTestData(ds1);
        assertTrue(ds2.allPrimitives().isEmpty());

        assertEquals(1, ds1.getAllSelected().size());
        assertEquals(1, ds1.getSelectedWays().size());
    }

    /**
     * Tests if the add command ignores existing data
     */
    @Test
    void testAddIgnoresExisting() {
        DataSet ds = new DataSet();

        List<PrimitiveData> testData = createTestData();
        assertTrue(new AddPrimitivesCommand(testData, ds).executeCommand());
        assertEquals(2, ds.getNodes().size());
        assertEquals(1, ds.getWays().size());

        testData.set(2, createTestNode(7));
        assertTrue(new AddPrimitivesCommand(testData, ds).executeCommand());

        assertEquals(3, ds.getNodes().size());
        assertEquals(1, ds.getWays().size());
    }

    /**
     * Test {@link AddPrimitivesCommand#getDescriptionText()}
     */
    @Test
    void testDescription() {
        DataSet ds = new DataSet();

        List<PrimitiveData> testData = createTestData();
        NodeData data2 = createTestNode(7);

        AddPrimitivesCommand command1 = new AddPrimitivesCommand(testData, ds);
        AddPrimitivesCommand command2 = new AddPrimitivesCommand(Arrays.<PrimitiveData>asList(data2), ds);

        assertEquals("Added 3 objects", command1.getDescriptionText());
        assertEquals("Added 1 object", command2.getDescriptionText());

        // Name must be the same after execution.
        assertTrue(command1.executeCommand());
        assertTrue(command2.executeCommand());

        assertEquals("Added 3 objects", command1.getDescriptionText());
        assertEquals("Added 1 object", command2.getDescriptionText());
    }

    /**
     * Test {@link AddPrimitivesCommand#undoCommand()}
     */
    @Test
    void testUndo() {
        DataSet ds = new DataSet();

        List<PrimitiveData> testData = createTestData();

        AddPrimitivesCommand command = new AddPrimitivesCommand(testData, ds);

        assertTrue(command.executeCommand());

        assertEquals(3, ds.allPrimitives().size());
        assertEquals(1, ds.getWays().size());
        Way way = ds.getWays().iterator().next();

        for (int i = 0; i < 2; i++) {
            // Needs to work multiple times.
            command.undoCommand();

            assertEquals(0, ds.allPrimitives().size());
            assertEquals(0, ds.getWays().size());

            // redo
            assertTrue(command.executeCommand());

            assertEquals(3, ds.allPrimitives().size());
            assertEquals(1, ds.getWays().size());
            assertSame(way, ds.getWays().iterator().next());
        }
    }

    /**
     * Tests if the undo command does not remove
     * data ignored by by the add command because they where already existing.
     */
    @Test
    void testUndoIgnoresExisting() {
        DataSet ds = new DataSet();

        List<PrimitiveData> testData = createTestData();

        assertTrue(new AddPrimitivesCommand(testData, ds).executeCommand());
        assertEquals(2, ds.getNodes().size());
        assertEquals(1, ds.getWays().size());

        testData.set(2, createTestNode(7));

        AddPrimitivesCommand command = new AddPrimitivesCommand(testData, ds);

        assertTrue(command.executeCommand());

        for (int i = 0; i < 2; i++) {
            // Needs to work multiple times.
            assertEquals(3, ds.getNodes().size());
            assertEquals(1, ds.getWays().size());

            command.undoCommand();

            assertEquals(2, ds.getNodes().size());
            assertEquals(1, ds.getWays().size());

            // redo
            assertTrue(command.executeCommand());
        }
    }

    /**
     * Tests if the undo command does not remove
     * data ignored by by the add command because they where already existing.
     * Simulates regression in #14620
     */
    @Test
    void testUndoIgnoresExistingAsDeleted() {
        DataSet ds = new DataSet();

        List<PrimitiveData> testData = createTestData();

        assertTrue(new AddPrimitivesCommand(testData, ds).executeCommand());
        assertEquals(2, ds.getNodes().size());
        assertEquals(1, ds.getWays().size());

        ds.getNodes().forEach(n -> n.setDeleted(true));

        AddPrimitivesCommand command = new AddPrimitivesCommand(testData, ds);

        assertTrue(command.executeCommand());

        for (int i = 0; i < 2; i++) {
            // Needs to work multiple times.
            assertEquals(2, ds.getNodes().size());
            assertEquals(1, ds.getWays().size());

            command.undoCommand();

            assertEquals(2, ds.getNodes().size());
            assertEquals(1, ds.getWays().size());

            // redo
            assertTrue(command.executeCommand());
        }
    }

    /**
     * Tests if the undo command does not remove
     * data ignored by by the add command because they where already existing.
     */
    @Test
    void testUndoIgnoresExistingSameUniqueIdDifferentType() {
        DataSet ds = new DataSet();

        List<PrimitiveData> testData = new ArrayList<>(createTestData());

        assertTrue(new AddPrimitivesCommand(testData, ds).executeCommand());
        assertEquals(2, ds.getNodes().size());
        assertEquals(1, ds.getWays().size());

        NodeData n7Data = createTestNode(7);
        NodeData n8Data = createTestNode(8);
        Way w2 = new Way(5);
        Node n7 = new Node(7);
        n7.load(n7Data);
        Node n8 = new Node(8);
        n8.load(n8Data);
        w2.setNodes(Arrays.asList(n7, n8));
        testData.set(2, createTestNode(7));
        testData.add(n8.save());
        testData.add(w2.save());

        AddPrimitivesCommand command = new AddPrimitivesCommand(testData, ds);

        assertTrue(command.executeCommand());

        for (int i = 0; i < 2; i++) {
            assertEquals(4, ds.getNodes().size());
            assertEquals(2, ds.getWays().size());

            // Needs to work multiple times.
            command.undoCommand();

            assertEquals(2, ds.getNodes().size());
            assertEquals(1, ds.getWays().size());

            // redo
            assertTrue(command.executeCommand());

        }
    }

    /**
     * Test {@link AddPrimitivesCommand#getParticipatingPrimitives()}
     */
    @Test
    void testParticipatingPrimitives() {
        DataSet ds = new DataSet();

        List<PrimitiveData> testData = createTestData();
        AddPrimitivesCommand command = new AddPrimitivesCommand(testData, ds);
        assertTrue(command.executeCommand());

        assertEquals(3, command.getParticipatingPrimitives().size());
        HashSet<OsmPrimitive> should = new HashSet<>(ds.allPrimitives());
        assertEquals(should, new HashSet<>(command.getParticipatingPrimitives()));

        // needs to be the same after undo
        command.undoCommand();
        assertEquals(should, new HashSet<>(command.getParticipatingPrimitives()));
    }

    /**
     * Tests {@link AddPrimitivesCommand#fillModifiedData(java.util.Collection, java.util.Collection, java.util.Collection)}
     */
    @Test
    void testFillModifiedData() {
        ArrayList<OsmPrimitive> modified = new ArrayList<>();
        ArrayList<OsmPrimitive> deleted = new ArrayList<>();
        ArrayList<OsmPrimitive> added = new ArrayList<>();

        List<PrimitiveData> testData = createTestData();
        new AddPrimitivesCommand(testData, new DataSet()).fillModifiedData(modified, deleted, added);

        assertArrayEquals(new Object[] {}, modified.toArray());
        assertArrayEquals(new Object[] {}, deleted.toArray());
        assertArrayEquals(new Object[] {}, added.toArray());
    }

    private void testContainsTestData(DataSet data) {
        assertEquals(3, data.allPrimitives().size());
        assertEquals(2, data.getNodes().size());
        assertEquals(1, data.getWays().size());
        assertEquals(3, data.allModifiedPrimitives().size());
        for (OsmPrimitive n : data.allPrimitives()) {
            assertEquals("test", n.get("test"));
            assertTrue(n.isModified());
        }

        for (Node n : data.getNodes()) {
            assertEquals(LatLon.ZERO, n.getCoor());
        }

        for (Way w : data.getWays()) {
            assertEquals(2, w.getNodes().size());
            assertEquals(5, w.getNode(0).getId());
            assertEquals(6, w.getNode(1).getId());
        }
    }

    private List<PrimitiveData> createTestData() {
        NodeData node1 = createTestNode(5);
        NodeData node2 = createTestNode(6);
        WayData way = new WayData(2);
        way.put("test", "test");
        way.setNodeIds(Arrays.asList(node1.getId(), node2.getId()));
        List<PrimitiveData> testData = Arrays.<PrimitiveData>asList(node1, node2, way);
        return testData;
    }

    private NodeData createTestNode(int id) {
        NodeData node1 = new NodeData();
        node1.setCoor(LatLon.ZERO);
        node1.put("test", "test");
        node1.setId(id);
        return node1;
    }

    /**
     * Unit test of methods {@link AddPrimitivesCommand#equals} and {@link AddPrimitivesCommand#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(AddPrimitivesCommand.class).usingGetClass()
            .withPrefabValues(DataSet.class,
                new DataSet(), new DataSet())
            .withPrefabValues(User.class,
                    User.createOsmUser(1, "foo"), User.createOsmUser(2, "bar"))
            .withPrefabValues(OsmDataLayer.class,
                new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
