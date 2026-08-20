// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.CommandTest.CommandTestDataWithRelation;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.I18n;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link ChangeMembersCommand} class.
 */
@I18n
// We need prefs for nodes.
@BasicPreferences
class ChangeMembersCommandTest {
    private CommandTestDataWithRelation testData;

    /**
     * Set up the test data.
     */
    @BeforeEach
    public void createTestData() {
        testData = new CommandTestDataWithRelation();
    }

    /**
     * Test {@link ChangeMembersCommand#executeCommand()}
     */
    @Test
    void testChange() {
        assertTrue(testData.existingNode.getReferrers().contains(testData.existingRelation));
        assertEquals(2, testData.existingRelation.getMembersCount());
        List<RelationMember> members = testData.existingRelation.getMembers();
        members.add(new RelationMember("n2", testData.existingNode2));
        new ChangeMembersCommand(testData.existingRelation, members).executeCommand();
        assertEquals(3, testData.existingRelation.getMembersCount());
        members = testData.existingRelation.getMembers();
        members.remove(0);
        new ChangeMembersCommand(testData.existingRelation, members).executeCommand();
        assertEquals(2, testData.existingRelation.getMembersCount());
        assertTrue(testData.existingRelation.getMembersFor(Collections.singleton(testData.existingNode)).isEmpty());
        assertEquals(testData.existingWay, testData.existingRelation.getMember(0).getMember());
        assertEquals(testData.existingNode2, testData.existingRelation.getMember(1).getMember());
    }

    /**
     * Test {@link ChangeMembersCommand#undoCommand()}
     */
    @Test
    void testUndo() {
        List<RelationMember> members = testData.existingRelation.getMembers();
        members.add(new RelationMember("n2", testData.existingNode2));
        Command command = new ChangeMembersCommand(testData.existingRelation, members);
        command.executeCommand();

        assertEquals(3, testData.existingRelation.getMembersCount());

        command.undoCommand();
        assertEquals(2, testData.existingRelation.getMembersCount());
    }

    /**
     * Test {@link ChangeMembersCommand#getDescriptionText()}
     */
    @Test
    void testDescription() {
        testData.existingRelation.put("name", "xy");
        List<RelationMember> members = testData.existingRelation.getMembers();
        members.remove(1);
        assertTrue(new ChangeMembersCommand(testData.existingRelation, members).getDescriptionText().matches("Change members of .*xy.*"));
    }

    /**
     * Unit test of methods {@link ChangeMembersCommand#equals} and {@link ChangeMembersCommand#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(ChangeMembersCommand.class).usingGetClass()
            .withPrefabValues(DataSet.class,
                new DataSet(), new DataSet())
            .withPrefabValues(User.class,
                    User.createOsmUser(1, "foo"), User.createOsmUser(2, "bar"))
            .withPrefabValues(OsmPrimitive.class,
                new Node(1), new Node(2))
            .withPrefabValues(OsmDataLayer.class,
                new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }

}
