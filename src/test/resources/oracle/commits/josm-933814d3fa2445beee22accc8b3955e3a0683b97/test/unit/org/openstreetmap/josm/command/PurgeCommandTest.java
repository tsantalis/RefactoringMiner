// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.CommandTest.CommandTestDataWithRelation;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Hash;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Storage;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link PurgeCommand} class.
 */
class PurgeCommandTest {
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
     * Test {@link PurgeCommand#executeCommand()}
     */
    @Test
    void testExecute() {
        Relation relationParent = testData.createRelation(100, new RelationMember("child", testData.existingRelation));
        Relation relationParent2 = testData.createRelation(101, new RelationMember("child", testData.existingRelation));
        // to check that algorithm ignores it:
        Relation relationParent3 = testData.createRelation(102, new RelationMember("child", testData.existingRelation));
        PurgeCommand command = new PurgeCommand(testData.layer.getDataSet(),
                Arrays.<OsmPrimitive>asList(testData.existingNode, testData.existingNode2, testData.existingWay,
                        testData.existingRelation, relationParent, relationParent2),
                Arrays.<OsmPrimitive>asList(testData.existingNode2, testData.existingWay, testData.existingRelation));
        command.executeCommand();
        assertTrue(testData.existingNode2.isIncomplete());
        assertTrue(testData.existingWay.isIncomplete());
        assertTrue(testData.existingRelation.isIncomplete());
        assertNull(relationParent.getDataSet());
        assertNull(relationParent2.getDataSet());
        assertNotNull(relationParent3.getDataSet());
        assertFalse(relationParent3.isIncomplete());
        assertNull(testData.existingNode.getDataSet());
        assertFalse(testData.existingNode.isIncomplete());
    }

    /**
     * Test {@link PurgeCommand#undoCommand()}
     */
    @Test
    void testUndo() {
        PurgeCommand command = new PurgeCommand(testData.layer.getDataSet(),
                Arrays.<OsmPrimitive>asList(testData.existingNode, testData.existingWay),
                Arrays.<OsmPrimitive>asList(testData.existingWay));
        command.executeCommand();
        assertTrue(testData.existingWay.isIncomplete());
        assertNull(testData.existingNode.getDataSet());

        command.undoCommand();
        assertFalse(testData.existingWay.isIncomplete());
        assertSame(testData.layer.data, testData.existingNode.getDataSet());

        command.executeCommand();
        assertTrue(testData.existingWay.isIncomplete());
        assertNull(testData.existingNode.getDataSet());
    }

    /**
     * Tests {@link PurgeCommand#fillModifiedData(java.util.Collection, java.util.Collection, java.util.Collection)}
     */
    @Test
    void testFillModifiedData() {
        ArrayList<OsmPrimitive> modified = new ArrayList<>();
        ArrayList<OsmPrimitive> deleted = new ArrayList<>();
        ArrayList<OsmPrimitive> added = new ArrayList<>();
        PurgeCommand command = new PurgeCommand(testData.layer.getDataSet(), Arrays.<OsmPrimitive>asList(testData.existingNode),
                Arrays.<OsmPrimitive>asList(testData.existingRelation));
        command.fillModifiedData(modified, deleted, added);
        // intentionally empty (?)
        assertArrayEquals(new Object[] {}, modified.toArray());
        assertArrayEquals(new Object[] {}, deleted.toArray());
        assertArrayEquals(new Object[] {}, added.toArray());
    }

    /**
     * Tests {@link PurgeCommand#getParticipatingPrimitives()}
     */
    @Test
    void testGetParticipatingPrimitives() {
        PurgeCommand command = new PurgeCommand(testData.layer.getDataSet(), Arrays.<OsmPrimitive>asList(testData.existingNode),
                Arrays.<OsmPrimitive>asList(testData.existingRelation));
        assertArrayEquals(new Object[] {testData.existingNode }, command.getParticipatingPrimitives().toArray());
    }

    /**
     * Test {@link PurgeCommand#getDescriptionText()}
     */
    @Test
    void testDescription() {
        List<OsmPrimitive> shortList = Arrays.<OsmPrimitive>asList(testData.existingWay);
        assertTrue(new PurgeCommand(testData.layer.getDataSet(), shortList, Arrays.<OsmPrimitive>asList()).getDescriptionText()
                .matches("Purged 1 object"));
        List<OsmPrimitive> longList = Arrays.<OsmPrimitive>asList(testData.existingNode, testData.existingNode2,
                testData.existingWay);
        assertTrue(new PurgeCommand(testData.layer.getDataSet(), longList, Arrays.<OsmPrimitive>asList()).getDescriptionText()
                .matches("Purged 3 objects"));
    }

    /**
     * Unit test of methods {@link PurgeCommand#equals} and {@link PurgeCommand#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(PurgeCommand.class).usingGetClass()
            .withPrefabValues(DataSet.class,
                new DataSet(), new DataSet())
            .withPrefabValues(User.class,
                    User.createOsmUser(1, "foo"), User.createOsmUser(2, "bar"))
            .withPrefabValues(Conflict.class,
                    new Conflict<>(new Node(1, 1), new Node(2, 1)),
                    new Conflict<>(new Node(1, 1), new Node(3, 1)))
            .withPrefabValues(OsmDataLayer.class,
                new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .withPrefabValues(Hash.class,
                Storage.<OsmPrimitive>defaultHash(), Storage.<OsmPrimitive>defaultHash())
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
