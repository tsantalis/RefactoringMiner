// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.CommandTest.CommandTestData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link ChangePropertyKeyCommand} class.
 */
class ChangePropertyKeyCommandTest {

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
     * Tests that a key is changed.
     */
    @Test
    void testChangeKeySingle() {
        assertTrue(new ChangePropertyKeyCommand(testData.existingNode, "existing", "newKey").executeCommand());

        assertNull(testData.existingNode.get("existing"));
        assertEquals("existing", testData.existingNode.get("newKey"));
        assertTrue(testData.existingNode.isModified());
    }

    /**
     * Tests that a key is changed.
     */
    @Test
    void testChangeKey() {
        assertTrue(new ChangePropertyKeyCommand(Arrays.asList(testData.existingNode, testData.existingWay), "existing",
                "newKey").executeCommand());

        assertNull(testData.existingNode.get("existing"));
        assertEquals("existing", testData.existingNode.get("newKey"));
        assertTrue(testData.existingNode.isModified());
        assertNull(testData.existingWay.get("existing"));
        assertEquals("existing", testData.existingWay.get("newKey"));
        assertTrue(testData.existingWay.isModified());
    }

    /**
     * Tests that nop operations are ignored.
     */
    @Test
    void testChangeKeyIgnored() {
        Node node1 = testData.createNode(15);
        node1.removeAll();
        Node node2 = testData.createNode(16);
        Node node3 = testData.createNode(17);

        assertTrue(new ChangePropertyKeyCommand(Arrays.asList(node1, node2), "nonexisting", "newKey").executeCommand());

        assertFalse(node1.isModified());
        assertFalse(node2.isModified());

        assertTrue(new ChangePropertyKeyCommand(Arrays.asList(node1, node2), "existing", "newKey").executeCommand());

        assertFalse(node1.isModified());
        assertTrue(node2.isModified());

        // removes existing
        assertTrue(new ChangePropertyKeyCommand(Arrays.asList(node1, node3), "newKey", "existing").executeCommand());

        assertFalse(node1.isModified());
        assertTrue(node3.isModified());
        assertNull(node3.get("newKey"));
        assertNull(node3.get("existing"));
    }

    /**
     * Test {@link ChangePropertyKeyCommand#getDescriptionText()}
     */
    @Test
    void testDescription() {
        Node node1 = testData.createNode(15);
        node1.put("name", "xy");

        assertTrue(new ChangePropertyKeyCommand(node1, "a", "b").getDescriptionText()
                .matches("Replace \"a\" by \"b\" for.*xy.*"));
        assertTrue(new ChangePropertyKeyCommand(Arrays.asList(node1, testData.existingNode), "a", "b")
                .getDescriptionText().matches("Replace \"a\" by \"b\" for 2 objects"));
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

        ArrayList<Node> nodesToExpect = new ArrayList<>(Arrays.asList(node1, node2));

        assertNull(new ChangePropertyKeyCommand(node1, "a", "b").getChildren());
        Collection<PseudoCommand> children = new ChangePropertyKeyCommand(Arrays.asList(node1, node2), "a", "b").getChildren();
        assertEquals(2, children.size());
        for (PseudoCommand c : children) {
            assertNull(c.getChildren());
            Collection<? extends OsmPrimitive> part = c.getParticipatingPrimitives();
            assertEquals(1, part.size());
            OsmPrimitive node = part.iterator().next();
            assertTrue(nodesToExpect.remove(node));

            assertTrue(c.getDescriptionText().contains(node.getName()));
        }
    }

    /**
     * Unit test of methods {@link ChangePropertyKeyCommand#equals} and {@link ChangePropertyKeyCommand#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(ChangePropertyKeyCommand.class).usingGetClass()
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
