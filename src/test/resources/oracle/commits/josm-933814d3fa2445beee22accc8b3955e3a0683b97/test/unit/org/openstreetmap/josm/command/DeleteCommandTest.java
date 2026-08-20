// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.CommandTest.CommandTestDataWithRelation;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link DeleteCommand} class.
 */
class DeleteCommandTest {

    /**
     * We need prefs for nodes.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().i18n();
    private CommandTestDataWithRelation testData;

    /**
     * Set up the test data.
     */
    @BeforeEach
    public void createTestData() {
        testData = new CommandTestDataWithRelation();
    }

    /**
     * A simple deletion test with no references
     */
    @Test
    void testSimpleDelete() {
        Node node = testData.createNode(15);
        assertTrue(testData.layer.data.allPrimitives().contains(node));

        new DeleteCommand(node).executeCommand();

        assertTrue(node.isDeleted());
        assertTrue(node.isModified());
        assertFalse(testData.layer.data.allNonDeletedPrimitives().contains(node));
    }

    /**
     * A delete should not delete referred objects but should should remove the reference.
     */
    @Test
    void testDeleteIgnoresReferences() {
        assertTrue(testData.existingNode.getReferrers().contains(testData.existingRelation));
        new DeleteCommand(testData.existingRelation).executeCommand();

        assertTrue(testData.existingRelation.isDeleted());
        assertEquals(0, testData.existingRelation.getMembersCount());
        assertFalse(testData.existingNode.isDeleted());
        assertFalse(testData.existingWay.isDeleted());
        assertFalse(testData.existingNode.getReferrers().contains(testData.existingRelation));

        // same for the way
        assertTrue(testData.existingNode.getReferrers().contains(testData.existingWay));
        new DeleteCommand(testData.existingWay).executeCommand();
        assertEquals(0, testData.existingWay.getNodesCount());
        assertFalse(testData.existingNode.getReferrers().contains(testData.existingWay));
    }

    /**
     * A delete should delete all objects with references to the deleted one
     */
    @Test
    void testDeleteFailsOnDeleted() {
        new DeleteCommand(testData.existingRelation).executeCommand();

        assertThrows(IllegalArgumentException.class, () -> new DeleteCommand(testData.existingRelation).executeCommand());
    }

    /**
     * A delete should delete all objects with references to the deleted one
     */
    @Test
    void testReferredDelete() {
        DeleteCommand.deleteWithReferences(Arrays.asList(testData.existingNode), true).executeCommand();

        assertTrue(testData.existingNode.isDeleted());
        assertEquals(0, testData.existingWay.getNodesCount());
        assertTrue(testData.existingWay.isDeleted());
    }

    /**
     * Delete nodes that would be without reference afterwards.
     */
    @Test
    void testDeleteNodesInWay() {
        testData.existingNode.removeAll();
        // That untagged node should be deleted.
        testData.existingNode2.removeAll();
        DeleteCommand.delete(Arrays.asList(testData.existingWay), true, true).executeCommand();

        assertTrue(testData.existingWay.isDeleted());
        assertTrue(testData.existingNode2.isDeleted());
        assertFalse(testData.existingNode.isDeleted());
        assertFalse(testData.existingRelation.isDeleted());

        // Same test, now with tagged nodes
        Node node1 = testData.createNode(15);
        Node node2 = testData.createNode(16);
        Node node3 = testData.createNode(17);
        Node node4 = testData.createNode(18);
        node2.removeAll();
        node4.removeAll();
        Way way1 = new Way(25, 1);
        way1.setNodes(Arrays.asList(node1, node2, node3));
        testData.layer.data.addPrimitive(way1);
        Way way2 = new Way(26, 1);
        way2.setNodes(Arrays.asList(node2, node3, node4));
        testData.layer.data.addPrimitive(way2);
        DeleteCommand.delete(Arrays.asList(way1, way2), true, true).executeCommand();

        assertTrue(way1.isDeleted());
        assertTrue(way2.isDeleted());
        assertFalse(node1.isDeleted());
        assertTrue(node2.isDeleted());
        assertFalse(node3.isDeleted());
        assertTrue(node4.isDeleted());
    }

    /**
     * Test that {@link DeleteCommand} checks for non-null.
     */
    @Test
    void testConsistency() {
        assertThrows(IllegalArgumentException.class, () -> new DeleteCommand(Arrays.asList(testData.existingNode, testData.existingWay, null)));
    }

    /**
     * Test that {@link DeleteCommand} checks for the dataset
     */
    @Test
    void testConsistencyDataset() {
        testData.layer.getDataSet().removePrimitive(testData.existingNode);
        assertThrows(IllegalArgumentException.class, () -> new DeleteCommand(Arrays.asList(testData.existingNode, testData.existingWay)));
    }

    /**
     * Test that {@link DeleteCommand} checks for non-empty list
     */
    @Test
    void testConsistencyNonEmpty() {
        assertThrows(NoSuchElementException.class, () -> new DeleteCommand(Arrays.<OsmPrimitive>asList()));
    }

    /**
     * Test that {@link DeleteCommand} checks for non-null list
     */
    @Test
    void testConsistencyNonNull() {
        assertThrows(NullPointerException.class, () -> new DeleteCommand((Collection<OsmPrimitive>) null));
    }

    /**
     * Test {@link DeleteCommand#undoCommand()}
     */
    @Test
    void testUndo() {
        DeleteCommand command = new DeleteCommand(
                Arrays.asList(testData.existingNode, testData.existingNode2, testData.existingWay));
        command.executeCommand();

        assertTrue(testData.existingNode.isDeleted());
        assertTrue(testData.existingWay.isDeleted());

        command.undoCommand();

        assertFalse(testData.existingNode.isDeleted());
        assertFalse(testData.existingWay.isDeleted());
        assertEquals("existing", testData.existingNode.get("existing"));

        command.executeCommand();

        assertTrue(testData.existingNode.isDeleted());
        assertTrue(testData.existingWay.isDeleted());
    }

    /**
     * Test {@link DeleteCommand#deleteWaySegment(WaySegment)}
     * Way with only 1 segment
     */
    @Test
    void testDeleteWaySegment() {
        Way way1 = testData.createWay(100, testData.createNode(101), testData.createNode(102));
        WaySegment ws = new WaySegment(way1, 0);
        Command command = DeleteCommand.deleteWaySegment(ws);
        command.executeCommand();

        assertTrue(way1.isDeleted());
    }

    /**
     * Test {@link DeleteCommand#deleteWaySegment(WaySegment)}
     * Delete end of way
     */
    @Test
    void testDeleteWaySegmentEndOfWay() {
        Way way = testData.createWay(200, testData.createNode(201), testData.createNode(202), testData.createNode(203),
                testData.createNode(204));
        WaySegment ws = new WaySegment(way, 2);
        Command command = DeleteCommand.deleteWaySegment(ws);
        command.executeCommand();

        assertEquals(3, way.getNodesCount());
        assertEquals(201, way.getNodeId(0));
        assertEquals(202, way.getNodeId(1));
        assertEquals(203, way.getNodeId(2));
    }

    /**
     * Test {@link DeleteCommand#deleteWaySegment(WaySegment)}
     * Delete start of way
     */
    @Test
    void testDeleteWaySegmentStartOfWay() {
        Way way = testData.createWay(100, testData.createNode(101), testData.createNode(102), testData.createNode(103),
                testData.createNode(104));
        WaySegment ws = new WaySegment(way, 0);
        Command command = DeleteCommand.deleteWaySegment(ws);
        command.executeCommand();

        assertEquals(3, way.getNodesCount());
        assertEquals(102, way.getNodeId(0));
        assertEquals(103, way.getNodeId(1));
        assertEquals(104, way.getNodeId(2));
    }

    /**
     * Test {@link DeleteCommand#deleteWaySegment(WaySegment)}
     * Delete start of way
     */
    @Test
    void testDeleteWaySegmentSplit() {
        Node node103 = testData.createNode(103);
        Node node104 = testData.createNode(104);
        Way way = testData.createWay(100, testData.createNode(101), testData.createNode(102), node103, node104);
        WaySegment ws = new WaySegment(way, 1);
        Command command = DeleteCommand.deleteWaySegment(ws);
        command.executeCommand();

        assertEquals(2, way.getNodesCount());
        assertEquals(101, way.getNodeId(0));
        assertEquals(102, way.getNodeId(1));
        // there needs to be a new way
        assertEquals(1, node104.getReferrers().size());
        Way newWay = (Way) node104.getReferrers().get(0);
        assertEquals(2, newWay.getNodesCount());
        assertEquals(103, newWay.getNodeId(0));
        assertEquals(104, newWay.getNodeId(1));
    }

    /**
     * Test {@link DeleteCommand#deleteWaySegment(WaySegment)}
     * Delete start of way
     */
    @Test
    void testDeleteWaySegmentCycle() {
        Node n = testData.createNode(101);
        Way way = testData.createWay(100, n, testData.createNode(102), testData.createNode(103),
                testData.createNode(104), n);
        WaySegment ws = new WaySegment(way, 2);
        Command command = DeleteCommand.deleteWaySegment(ws);
        command.executeCommand();

        assertEquals(4, way.getNodesCount());
        assertEquals(104, way.getNodeId(0));
        assertEquals(101, way.getNodeId(1));
        assertEquals(102, way.getNodeId(2));
        assertEquals(103, way.getNodeId(3));
    }

    /**
     * Tests {@link DeleteCommand#getChildren()}
     */
    @Test
    void testGetChildren() {
        testData.existingNode.put("name", "xy");
        Collection<PseudoCommand> children = new DeleteCommand(Arrays.<OsmPrimitive>asList(testData.existingNode, testData.existingNode2))
                .getChildren();
        assertEquals(2, children.size());
        assertTrue(children.stream().allMatch(c -> c.getParticipatingPrimitives().size() == 1));
        assertTrue(children.stream().anyMatch(c -> c.getParticipatingPrimitives().iterator().next() == testData.existingNode));
        assertTrue(children.stream().anyMatch(c -> c.getParticipatingPrimitives().iterator().next() == testData.existingNode2));
        assertTrue(children.stream().anyMatch(c -> c.getDescriptionText().matches("Deleted '.*xy.*'")));
    }

    /**
     * Tests {@link DeleteCommand#fillModifiedData(java.util.Collection, java.util.Collection, java.util.Collection)}
     */
    @Test
    void testFillModifiedData() {
        ArrayList<OsmPrimitive> modified = new ArrayList<>();
        ArrayList<OsmPrimitive> deleted = new ArrayList<>();
        ArrayList<OsmPrimitive> added = new ArrayList<>();
        new DeleteCommand(Arrays.<OsmPrimitive>asList(testData.existingNode)).fillModifiedData(modified, deleted, added);
        // intentionally left empty.
        assertArrayEquals(new Object[] {}, modified.toArray());
        assertArrayEquals(new Object[] {}, deleted.toArray());
        assertArrayEquals(new Object[] {}, added.toArray());
    }

    /**
     * Tests {@link DeleteCommand#getParticipatingPrimitives()}
     */
    @Test
    void testGetParticipatingPrimitives() {
        DeleteCommand command = new DeleteCommand(Arrays.<OsmPrimitive>asList(testData.existingNode));
        assertArrayEquals(new Object[] {testData.existingNode }, command.getParticipatingPrimitives().toArray());

        DeleteCommand command2 = new DeleteCommand(
                Arrays.<OsmPrimitive>asList(testData.existingNode, testData.existingWay));
        assertArrayEquals(new Object[] {testData.existingNode, testData.existingWay},
                command2.getParticipatingPrimitives().toArray());
    }

    /**
     * Test {@link DeleteCommand#getDescriptionText()}
     */
    @Test
    void testDescription() {
        Node node = testData.createNode(100);
        node.put("name", "xy");
        Way way = testData.createWay(101);
        way.put("name", "xy");
        Relation relation = testData.createRelation(102);
        relation.put("name", "xy");

        List<OsmPrimitive> nodeList = Arrays.<OsmPrimitive>asList(node);
        assertTrue(new DeleteCommand(nodeList).getDescriptionText().matches("Delete node .*xy.*"));
        List<OsmPrimitive> wayList = Arrays.<OsmPrimitive>asList(way);
        assertTrue(new DeleteCommand(wayList).getDescriptionText().matches("Delete way .*xy.*"));
        List<OsmPrimitive> relationList = Arrays.<OsmPrimitive>asList(relation);
        assertTrue(new DeleteCommand(relationList).getDescriptionText().matches("Delete relation .*xy.*"));

        List<OsmPrimitive> nodesList = Arrays.<OsmPrimitive>asList(node, testData.createNode(110));
        assertTrue(new DeleteCommand(nodesList).getDescriptionText().matches("Delete 2 nodes"));
        List<OsmPrimitive> waysList = Arrays.<OsmPrimitive>asList(way, testData.createWay(111));
        assertTrue(new DeleteCommand(waysList).getDescriptionText().matches("Delete 2 ways"));
        List<OsmPrimitive> relationsList = Arrays.<OsmPrimitive>asList(relation, testData.createRelation(112));
        assertTrue(new DeleteCommand(relationsList).getDescriptionText().matches("Delete 2 relations"));

        List<OsmPrimitive> mixed = Arrays.<OsmPrimitive>asList(node, way, relation);
        assertTrue(new DeleteCommand(mixed).getDescriptionText().matches("Delete 3 objects"));
    }

    /**
     * Unit test of methods {@link DeleteCommand#equals} and {@link DeleteCommand#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(DeleteCommand.class).usingGetClass()
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
