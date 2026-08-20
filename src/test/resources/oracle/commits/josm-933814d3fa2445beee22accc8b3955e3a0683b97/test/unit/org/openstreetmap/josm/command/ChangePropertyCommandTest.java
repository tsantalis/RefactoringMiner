// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.CommandTest.CommandTestData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link ChangePropertyCommand} class.
 */
class ChangePropertyCommandTest {

    /**
     * We need prefs for nodes.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().i18n();
    private CommandTestData testData;

    /**
     * Set up the test data.
     */
    @BeforeEach
    public void createTestData() {
        testData = new CommandTestData();
    }

    /**
     * Checks that the short constructors create the right {@link ChangePropertyCommand}
     */
    @Test
    void testShortConstructor() {
        ChangePropertyCommand command = new ChangePropertyCommand(Arrays.asList(testData.existingNode), "a", "b");
        assertEquals("b", command.getTags().get("a"));
        assertEquals(1, command.getTags().size());
        assertEquals(1, command.getObjectsNumber());

        command = new ChangePropertyCommand(testData.existingNode, "a", "b");
        assertEquals("b", command.getTags().get("a"));
        assertEquals(1, command.getTags().size());
        assertEquals(1, command.getObjectsNumber());
    }

    /**
     * Checks that {@link ChangePropertyCommand} adds/updates a property
     */
    @Test
    void testUpdateSingleProperty() {
        Node node1 = testData.createNode(14);
        Node node2 = testData.createNode(15);
        node2.removeAll();

        TagMap tags = new TagMap();
        tags.put("existing", "new");
        new ChangePropertyCommand(Arrays.<OsmPrimitive>asList(node1, node2), tags).executeCommand();
        assertEquals("new", node1.get("existing"));
        assertEquals("new", node2.get("existing"));

        assertTrue(node1.isModified());
        assertTrue(node2.isModified());
    }

    /**
     * Checks that {@link ChangePropertyCommand} removes a property
     */
    @Test
    void testRemoveProperty() {
        Node node1 = testData.createNode(14);
        Node node2 = testData.createNode(15);
        node2.removeAll();

        HashMap<String, String> tags = new HashMap<>();
        tags.put("existing", "");
        new ChangePropertyCommand(Arrays.<OsmPrimitive>asList(node1, node2), tags).executeCommand();
        assertNull(node1.get("existing"));
        assertNull(node2.get("existing"));

        assertTrue(node1.isModified());
        assertFalse(node2.isModified());
    }

    /**
     * Checks that {@link ChangePropertyCommand} adds/updates multiple properties
     */
    @Test
    void testUpdateMultipleProperties() {
        Node node1 = testData.createNode(14);
        Node node2 = testData.createNode(15);
        node2.removeAll();
        node2.put("test", "xx");
        node2.put("remove", "xx");

        HashMap<String, String> tags = new HashMap<>();
        tags.put("existing", "existing");
        tags.put("test", "test");
        tags.put("remove", "");
        new ChangePropertyCommand(Arrays.<OsmPrimitive>asList(node1, node2), tags).executeCommand();
        assertEquals("existing", node1.get("existing"));
        assertEquals("existing", node2.get("existing"));
        assertEquals("test", node1.get("test"));
        assertEquals("test", node2.get("test"));
        assertNull(node1.get("remove"));
        assertNull(node2.get("remove"));

        assertTrue(node1.isModified());
        assertTrue(node2.isModified());
    }

    /**
     * Checks that {@link ChangePropertyCommand} adds/updates a property
     */
    @Test
    void testUpdateIgnoresExistingProperty() {
        Node node1 = testData.createNode(14);
        Node node2 = testData.createNode(15);
        node2.removeAll();

        TagMap tags = new TagMap();
        tags.put("existing", "existing");
        new ChangePropertyCommand(Arrays.<OsmPrimitive>asList(node1, node2), tags).executeCommand();
        assertEquals("existing", node1.get("existing"));
        assertEquals("existing", node2.get("existing"));

        assertFalse(node1.isModified());
        assertTrue(node2.isModified());
    }

    /**
     * Tests {@link ChangePropertyCommand#fillModifiedData(java.util.Collection, java.util.Collection, java.util.Collection)}
     * and {@link ChangePropertyCommand#getObjectsNumber()}
     */
    @Test
    void testFillModifiedData() {
        Node node1 = testData.createNode(14);
        Node node2 = testData.createNode(15);
        node2.put("existing", "new");

        TagMap tags = new TagMap();
        tags.put("existing", "new");

        ArrayList<OsmPrimitive> modified = new ArrayList<>();
        ArrayList<OsmPrimitive> deleted = new ArrayList<>();
        ArrayList<OsmPrimitive> added = new ArrayList<>();
        new ChangePropertyCommand(Arrays.asList(node1, node2), tags).fillModifiedData(modified, deleted, added);
        assertArrayEquals(new Object[] {node1}, modified.toArray());
        assertArrayEquals(new Object[] {}, deleted.toArray());
        assertArrayEquals(new Object[] {}, added.toArray());

        assertEquals(1, new ChangePropertyCommand(Arrays.asList(node1, node2), tags).getObjectsNumber());

        tags.clear();
        assertEquals(0, new ChangePropertyCommand(Arrays.asList(node1, node2), tags).getObjectsNumber());

        tags.put("a", "b");
        assertEquals(2, new ChangePropertyCommand(Arrays.asList(node1, node2), tags).getObjectsNumber());
    }

    /**
     * Test {@link ChangePropertyCommand#getDescriptionText()}
     */
    @Test
    void testDescription() {
        Node node1 = testData.createNode(14);
        Node node2 = testData.createNode(15);
        Node node3 = testData.createNode(16);
        node1.put("name", "xy");
        node2.put("existing", "new");
        node3.put("existing", null);

        TagMap tags = new TagMap();
        tags.put("existing", "new");

        HashMap<String, String> tagsRemove = new HashMap<>();
        tagsRemove.put("existing", "");

        Way way = testData.createWay(20, node1);
        way.put("name", "xy");
        way.put("existing", "existing");
        Relation relation = testData.createRelation(30);
        relation.put("name", "xy");
        relation.put("existing", "existing");

        // nop
        assertTrue(new ChangePropertyCommand(Arrays.asList(node2), tags).getDescriptionText()
                .matches("Set.*tags for 0 objects"));

        // change 1 key on 1 element.
        assertTrue(new ChangePropertyCommand(Arrays.asList(node1, node2), tags).getDescriptionText()
                .matches("Set existing=new for node.*xy.*"));
        assertTrue(new ChangePropertyCommand(Arrays.asList(way, node2), tags).getDescriptionText()
                .matches("Set existing=new for way.*xy.*"));
        assertTrue(new ChangePropertyCommand(Arrays.asList(relation, node2), tags).getDescriptionText()
                .matches("Set existing=new for relation.*xy.*"));

        // remove 1 key on 1 element
        assertTrue(new ChangePropertyCommand(Arrays.asList(node1, node3), tagsRemove).getDescriptionText()
                .matches("Remove \"existing\" for node.*xy.*"));
        assertTrue(new ChangePropertyCommand(Arrays.asList(way, node3), tagsRemove).getDescriptionText()
                .matches("Remove \"existing\" for way.*xy.*"));
        assertTrue(new ChangePropertyCommand(Arrays.asList(relation, node3), tagsRemove).getDescriptionText()
                .matches("Remove \"existing\" for relation.*xy.*"));

        // change 1 key on 3 elements
        assertEquals("Set existing=new for 3 objects",
                new ChangePropertyCommand(Arrays.asList(node1, node2, way, relation), tags).getDescriptionText());
        // remove 1 key on 3 elements
        assertEquals("Remove \"existing\" for 3 objects",
                new ChangePropertyCommand(Arrays.asList(node1, node3, way, relation), tagsRemove).getDescriptionText());

        // add 2 keys on 3 elements
        tags.put("name", "a");
        node2.put("name", "a");
        assertEquals("Set 2 tags for 3 objects",
                new ChangePropertyCommand(Arrays.asList(node1, node2, way, relation), tags).getDescriptionText());

        tagsRemove.put("name", "");
        // remove 2 key on 3 elements
        assertEquals("Deleted 2 tags for 3 objects",
                new ChangePropertyCommand(Arrays.asList(node1, node3, way, relation), tagsRemove).getDescriptionText());
    }

    /**
     * Test {@link ChangePropertyCommand#getChildren()}
     */
    @Test
    void testChildren() {
        Node node1 = testData.createNode(15);
        Node node2 = testData.createNode(16);
        node1.put("name", "node1");
        node2.put("name", "node2");

        assertNull(new ChangePropertyCommand(Arrays.asList(node1), "a", "b").getChildren());

        Collection<PseudoCommand> children = new ChangePropertyCommand(Arrays.asList(node1, node2), "a", "b").getChildren();
        assertEquals(2, children.size());
        List<Node> nodesToExpect = new ArrayList<>(Arrays.asList(node1, node2));
        for (PseudoCommand c : children) {
            assertNull(c.getChildren());
            Collection<? extends OsmPrimitive> part = c.getParticipatingPrimitives();
            assertEquals(1, part.size());
            OsmPrimitive node = part.iterator().next();
            assertTrue(nodesToExpect.remove(node));

            assertTrue(c.getDescriptionText().matches(".*" + node.get("name") + ".*"));
        }
    }

    /**
     * Unit test of methods {@link ChangePropertyCommand#equals} and {@link ChangePropertyCommand#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(ChangePropertyCommand.class).usingGetClass()
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
