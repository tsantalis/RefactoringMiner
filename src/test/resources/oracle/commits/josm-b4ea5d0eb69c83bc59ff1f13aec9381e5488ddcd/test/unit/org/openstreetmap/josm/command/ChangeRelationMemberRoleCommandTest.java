// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.CommandTest.CommandTestDataWithRelation;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.I18n;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link ChangeRelationMemberRoleCommand} class.
 */
@I18n
// We need prefs for nodes.
@BasicPreferences
class ChangeRelationMemberRoleCommandTest {
    private CommandTestDataWithRelation testData;

    /**
     * Set up the test data.
     */
    @BeforeEach
    public void createTestData() {
        testData = new CommandTestDataWithRelation();
    }

    /**
     * Test if {@link ChangeRelationMemberRoleCommand} changes the role by index
     */
    @Test
    void testRoleChanged() {
        assertTrue(new ChangeRelationMemberRoleCommand(testData.existingRelation, 0, "newRole").executeCommand());
        assertEquals("newRole", testData.existingRelation.getMember(0).getRole());
        assertEquals(testData.existingNode, testData.existingRelation.getMember(0).getMember());
        assertEquals("way", testData.existingRelation.getMember(1).getRole());

        assertTrue(testData.existingRelation.isModified());

        assertTrue(new ChangeRelationMemberRoleCommand(testData.existingRelation, 1, "newRole").executeCommand());
        assertEquals("newRole", testData.existingRelation.getMember(1).getRole());
    }

    /**
     * Wrong index should be ignored.
     */
    @Test
    void testWrongIndex() {
        // should be ignored
        ChangeRelationMemberRoleCommand command1 = new ChangeRelationMemberRoleCommand(testData.existingRelation, -1, "newRole");
        assertTrue(command1.executeCommand());
        ChangeRelationMemberRoleCommand command2 = new ChangeRelationMemberRoleCommand(testData.existingRelation, 8, "newRole");
        assertTrue(command2.executeCommand());
        assertFalse(testData.existingRelation.isModified());

        command1.undoCommand();
        command2.undoCommand();
        assertFalse(testData.existingRelation.isModified());
    }


    /**
     * Same role should be ignored.
     */
    @Test
    void testSameRole() {
        // should be ignored
        assertTrue(new ChangeRelationMemberRoleCommand(testData.existingRelation, 0, "node").executeCommand());
        assertFalse(testData.existingRelation.isModified());
    }

    /**
     * Test {@link ChangeRelationMemberRoleCommand#undoCommand()}.
     */
    @Test
    void testUndo() {
        ChangeRelationMemberRoleCommand command = new ChangeRelationMemberRoleCommand(testData.existingRelation, 0, "newRole");
        command.executeCommand();
        assertEquals("newRole", testData.existingRelation.getMember(0).getRole());
        assertTrue(testData.existingRelation.isModified());

        command.undoCommand();
        assertEquals("node", testData.existingRelation.getMember(0).getRole());
        assertFalse(testData.existingRelation.isModified());

        command.executeCommand();
        assertEquals("newRole", testData.existingRelation.getMember(0).getRole());
        assertTrue(testData.existingRelation.isModified());
    }

    /**
     * Tests {@link ChangeCommand#fillModifiedData(java.util.Collection, java.util.Collection, java.util.Collection)}
     */
    @Test
    void testFillModifiedData() {
        ArrayList<OsmPrimitive> modified = new ArrayList<>();
        ArrayList<OsmPrimitive> deleted = new ArrayList<>();
        ArrayList<OsmPrimitive> added = new ArrayList<>();
        new ChangeRelationMemberRoleCommand(testData.existingRelation, 0, "newRole").fillModifiedData(modified, deleted, added);
        assertArrayEquals(new Object[] {testData.existingRelation}, modified.toArray());
        assertArrayEquals(new Object[] {}, deleted.toArray());
        assertArrayEquals(new Object[] {}, added.toArray());
    }

    /**
     * Test {@link ChangeRelationMemberRoleCommand#getDescriptionText()}
     */
    @Test
    void testDescription() {
        testData.existingRelation.put("name", "xy");
        assertTrue(new ChangeRelationMemberRoleCommand(testData.existingRelation, 0, "newRole").getDescriptionText()
                .matches("Change relation member role for relation.*xy.*"));
    }

    /**
     * Test {@link ChangeRelationMemberRoleCommand#getChildren()}
     */
    @Test
    void testChildren() {
        assertNull(new ChangeRelationMemberRoleCommand(testData.existingRelation, 0, "newRole").getChildren());
    }

    /**
     * Unit test of methods {@link ChangeRelationMemberRoleCommand#equals} and {@link ChangeRelationMemberRoleCommand#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(ChangeRelationMemberRoleCommand.class).usingGetClass()
            .withPrefabValues(Relation.class,
                new Relation(1), new Relation(2))
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
