// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.CommandTest.CommandTestDataWithRelation;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link SelectCommand} class.
 */
class SelectCommandTest {

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
     * Test {@link SelectCommand#executeCommand()}
     */
    @Test
    void testExecute() {
        SelectCommand command = new SelectCommand(testData.layer.getDataSet(), Arrays.asList(testData.existingNode, testData.existingWay));

        testData.layer.getDataSet().setSelected(Arrays.asList(testData.existingNode2));

        command.executeCommand();

        assertTrue(testData.existingNode.isSelected());
        assertFalse(testData.existingNode2.isSelected());
        assertTrue(testData.existingWay.isSelected());
    }

    /**
     * Test {@link SelectCommand#executeCommand()}
     */
    @Test
    void testExecuteAfterModify() {
        List<OsmPrimitive> list = new ArrayList<>(Arrays.asList(testData.existingNode, testData.existingWay));
        SelectCommand command = new SelectCommand(testData.layer.getDataSet(), list);

        list.remove(testData.existingNode);
        list.add(testData.existingNode2);

        command.executeCommand();

        assertTrue(testData.existingNode.isSelected());
        assertFalse(testData.existingNode2.isSelected());
        assertTrue(testData.existingWay.isSelected());
    }

    /**
     * Test {@link SelectCommand#undoCommand()}
     */
    @Test
    void testUndo() {
        SelectCommand command = new SelectCommand(testData.layer.getDataSet(), Arrays.asList(testData.existingNode, testData.existingWay));
        testData.layer.getDataSet().setSelected(Arrays.asList(testData.existingNode2));

        command.executeCommand();

        command.undoCommand();

        assertFalse(testData.existingNode.isSelected());
        assertTrue(testData.existingNode2.isSelected());
        assertFalse(testData.existingWay.isSelected());

        command.executeCommand();

        assertTrue(testData.existingNode.isSelected());
        assertFalse(testData.existingNode2.isSelected());
        assertTrue(testData.existingWay.isSelected());
    }

    /**
     * Tests {@link SelectCommand#fillModifiedData(java.util.Collection, java.util.Collection, java.util.Collection)}
     */
    @Test
    void testFillModifiedData() {
        ArrayList<OsmPrimitive> modified = new ArrayList<>();
        ArrayList<OsmPrimitive> deleted = new ArrayList<>();
        ArrayList<OsmPrimitive> added = new ArrayList<>();
        SelectCommand command = new SelectCommand(testData.layer.getDataSet(), Arrays.asList(testData.existingNode, testData.existingWay));
        command.fillModifiedData(modified, deleted, added);
        // intentionally empty.
        assertArrayEquals(new Object[] {}, modified.toArray());
        assertArrayEquals(new Object[] {}, deleted.toArray());
        assertArrayEquals(new Object[] {}, added.toArray());
    }

    /**
     * Tests {@link SelectCommand#getParticipatingPrimitives()}
     */
    @Test
    void testGetParticipatingPrimitives() {
        SelectCommand command = new SelectCommand(testData.layer.getDataSet(), Arrays.asList(testData.existingNode));
        command.executeCommand();
        assertArrayEquals(new Object[] {testData.existingNode}, command.getParticipatingPrimitives().toArray());
    }

    /**
     * Test {@link SelectCommand#getDescriptionText()}
     */
    @Test
    void testDescription() {
        DataSet ds = testData.layer.getDataSet();
        assertTrue(new SelectCommand(ds, Arrays.<OsmPrimitive>asList(testData.existingNode))
                .getDescriptionText().matches("Selected 1 object"));
        assertTrue(new SelectCommand(ds, Arrays.asList(testData.existingNode, testData.existingWay))
                .getDescriptionText().matches("Selected 2 objects"));
        assertTrue(new SelectCommand(ds, Arrays.<OsmPrimitive>asList())
                .getDescriptionText().matches("Selected 0 objects"));
        assertTrue(new SelectCommand(ds, null)
                .getDescriptionText().matches("Selected 0 objects"));
    }

    /**
     * Unit test of methods {@link SelectCommand#equals} and {@link SelectCommand#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(SelectCommand.class).usingGetClass()
            .withPrefabValues(DataSet.class,
                new DataSet(), new DataSet())
            .withPrefabValues(User.class,
                    User.createOsmUser(1, "foo"), User.createOsmUser(2, "bar"))
            .withPrefabValues(OsmDataLayer.class,
                new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }

    /**
     * Unit test of {@link SelectCommand#SelectCommand}.
     */
    @Test
    void testConstructorIAE() {
        assertThrows(IllegalArgumentException.class, () -> new SelectCommand(new DataSet(), Arrays.asList(new OsmPrimitive[] {null})));
    }
}
