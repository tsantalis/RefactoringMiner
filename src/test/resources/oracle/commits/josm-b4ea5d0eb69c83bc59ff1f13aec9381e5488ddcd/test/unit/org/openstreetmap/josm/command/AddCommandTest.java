// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.I18n;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link AddCommand} class.
 */
@I18n
// We need prefs for nodes.
@BasicPreferences
class AddCommandTest {
    /**
     * Test if the add command is executed correctly and sets the modified flag.
     */
    @Test
    void testAdd() {
        DataSet ds = new DataSet();
        assertArrayEquals(new Object[0], ds.allPrimitives().toArray());

        Node osm = new Node(LatLon.ZERO);
        assertTrue(new AddCommand(ds, osm).executeCommand());

        assertArrayEquals(new Object[] {osm}, ds.allPrimitives().toArray());
        assertArrayEquals(new Object[] {osm}, ds.allModifiedPrimitives().toArray());
        assertTrue(osm.isModified());
    }

    /**
     * Tests if the add command respects the data set.
     */
    @Test
    void testAddToLayer() {
        DataSet ds1 = new DataSet();
        DataSet ds2 = new DataSet();

        Node osm = new Node(LatLon.ZERO);
        assertTrue(new AddCommand(ds2, osm).executeCommand());

        assertArrayEquals(new Object[0], ds1.allPrimitives().toArray());
        assertArrayEquals(new Object[] {osm}, ds2.allPrimitives().toArray());
    }

    /**
     * Test {@link AddCommand#undoCommand()}
     */
    @Test
    void testUndo() {
        Node osm = new Node(LatLon.ZERO);
        DataSet ds = new DataSet(osm);

        AddCommand command = new AddCommand(ds, new Node(LatLon.ZERO));
        command.executeCommand();

        command.undoCommand();
        assertArrayEquals(new Object[] {osm}, ds.allPrimitives().toArray());
    }

    /**
     * Test {@link AddCommand#getParticipatingPrimitives()}
     */
    @Test
    void testParticipatingPrimitives() {
        Node osm = new Node(LatLon.ZERO);

        assertArrayEquals(new Object[] {osm}, new AddCommand(new DataSet(), osm).getParticipatingPrimitives().toArray());
    }

    /**
     * Tests {@link AddCommand#fillModifiedData(java.util.Collection, java.util.Collection, java.util.Collection)}
     */
    @Test
    void testFillModifiedData() {
        Node osm = new Node(LatLon.ZERO);

        ArrayList<OsmPrimitive> modified = new ArrayList<>();
        ArrayList<OsmPrimitive> deleted = new ArrayList<>();
        ArrayList<OsmPrimitive> added = new ArrayList<>();
        new AddCommand(new DataSet(), osm).fillModifiedData(modified, deleted, added);
        assertArrayEquals(new Object[] {}, modified.toArray());
        assertArrayEquals(new Object[] {}, deleted.toArray());
        assertArrayEquals(new Object[] {osm}, added.toArray());
   }

    /**
     * Test {@link AddCommand#getDescriptionText()}
     */
    @Test
    void testDescription() {
        Node node = new Node(LatLon.ZERO);
        node.put("name", "xy");
        Way way = new Way();
        way.addNode(node);
        way.put("name", "xy");
        Relation relation = new Relation();
        relation.put("name", "xy");

        DataSet ds = new DataSet();
        assertTrue(new AddCommand(ds, node).getDescriptionText().matches("Add node.*xy.*"));
        assertTrue(new AddCommand(ds, way).getDescriptionText().matches("Add way.*xy.*"));
        assertTrue(new AddCommand(ds, relation).getDescriptionText().matches("Add relation.*xy.*"));
    }

    /**
     * Unit test of methods {@link AddCommand#equals} and {@link AddCommand#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(AddCommand.class).usingGetClass()
            .withPrefabValues(OsmPrimitive.class,
                new Node(1), new Node(2))
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
