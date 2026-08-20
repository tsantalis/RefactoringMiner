// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.CommandTest.CommandTestDataWithRelation;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link RemoveNodesCommand} class.
 */
class RemoveNodesCommandTest {

    /**
     * We need prefs for nodes.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();
    private CommandTestDataWithRelation testData;

    /**
     * Set up the test data.
     */
    @BeforeEach
    public void createTestData() {
        testData = new CommandTestDataWithRelation();
    }

    /**
     * Test {@link RemoveNodesCommand#executeCommand()}
     */
    @Test
    void testExecute() {
        RemoveNodesCommand command = new RemoveNodesCommand(testData.existingWay,
                Collections.singleton(testData.existingNode));

        command.executeCommand();

        assertFalse(testData.existingWay.containsNode(testData.existingNode));
        assertTrue(testData.existingWay.containsNode(testData.existingNode2));
        assertTrue(testData.existingWay.isModified());
    }

    /**
     * Test {@link RemoveNodesCommand#undoCommand()}
     */
    @Test
    void testUndo() {
        RemoveNodesCommand command = new RemoveNodesCommand(testData.existingWay,
                Collections.singleton(testData.existingNode));

        command.executeCommand();

        command.undoCommand();
        assertTrue(testData.existingWay.containsNode(testData.existingNode));
        assertTrue(testData.existingWay.containsNode(testData.existingNode2));
        assertFalse(testData.existingWay.isModified());

        command.executeCommand();

        assertFalse(testData.existingWay.containsNode(testData.existingNode));
        assertTrue(testData.existingWay.containsNode(testData.existingNode2));
        assertTrue(testData.existingWay.isModified());
    }

    /**
     * Tests {@link RemoveNodesCommand#fillModifiedData(java.util.Collection, java.util.Collection, java.util.Collection)}
     */
    @Test
    void testFillModifiedData() {
        ArrayList<OsmPrimitive> modified = new ArrayList<>();
        ArrayList<OsmPrimitive> deleted = new ArrayList<>();
        ArrayList<OsmPrimitive> added = new ArrayList<>();
        RemoveNodesCommand command = new RemoveNodesCommand(testData.existingWay,
                Collections.singleton(testData.existingNode));
        command.fillModifiedData(modified, deleted, added);
        assertArrayEquals(new Object[] {testData.existingWay }, modified.toArray());
        assertArrayEquals(new Object[] {}, deleted.toArray());
        assertArrayEquals(new Object[] {}, added.toArray());
    }

    /**
     * Tests {@link RemoveNodesCommand#getParticipatingPrimitives()}
     */
    @Test
    void testGetParticipatingPrimitives() {
        RemoveNodesCommand command = new RemoveNodesCommand(testData.existingWay,
                Collections.singleton(testData.existingNode));
        command.executeCommand();
        assertArrayEquals(new Object[] {testData.existingWay }, command.getParticipatingPrimitives().toArray());
    }

    /**
     * Test {@link RemoveNodesCommand#getDescriptionText()}
     */
    @Test
    void testDescription() {
        assertTrue(new RemoveNodesCommand(testData.existingWay, Collections.singleton(testData.existingNode))
                .getDescriptionText().matches("Removed nodes from .*"));
    }

    /**
     * Unit test of methods {@link RemoveNodesCommand#equals} and {@link RemoveNodesCommand#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(RemoveNodesCommand.class).usingGetClass()
            .withPrefabValues(Way.class,
                new Way(1), new Way(2))
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
