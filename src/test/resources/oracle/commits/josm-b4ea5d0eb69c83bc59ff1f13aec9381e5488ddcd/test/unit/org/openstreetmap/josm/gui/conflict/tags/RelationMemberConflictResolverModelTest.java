// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link RelationMemberConflictResolverModel} class.
 */
// Only needed for OSM primitives
@BasicPreferences
class RelationMemberConflictResolverModelTest {
    List<Way> buildTestDataSet() {
        final DataSet ds = new DataSet();
        final Node n1 = new Node(new LatLon(1, 1));
        final Node n2 = new Node(new LatLon(2, 2));
        final Node n3 = new Node(new LatLon(3, 3));
        final Way w1 = new Way();
        final Way w2 = new Way();
        final Way w3 = new Way();
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        ds.addPrimitive(n3);
        ds.addPrimitive(w1);
        ds.addPrimitive(w2);
        ds.addPrimitive(w3);
        w1.addNode(n1);
        w1.addNode(n2);
        w2.addNode(n2);
        w2.addNode(n3);
        w3.addNode(n3);
        w3.addNode(n1);
        return Arrays.asList(w1, w2, w3);
    }

    @Test
    void testSameRoles() throws Exception {
        final List<Way> ways = buildTestDataSet();
        final Relation r = new Relation();
        r.addMember(new RelationMember("foo", ways.get(0)));
        r.addMember(new RelationMember("foo", ways.get(1)));

        final RelationMemberConflictResolverModel model = new RelationMemberConflictResolverModel();
        model.populate(Collections.singleton(r), ways.subList(0, 2));
        model.prepareDefaultRelationDecisions();
        assertTrue(model.isResolvedCompletely());
        assertEquals(RelationMemberConflictDecisionType.KEEP, model.getDecision(0).getDecision());
        assertEquals(ways.get(0), model.getDecision(0).getOriginalPrimitive());
        assertEquals("foo", model.getDecision(0).getRole());
        assertEquals(RelationMemberConflictDecisionType.REMOVE, model.getDecision(1).getDecision());
        assertEquals(ways.get(1), model.getDecision(1).getOriginalPrimitive());
    }

    @Test
    void testDifferentRoles() throws Exception {
        final List<Way> ways = buildTestDataSet();
        final Relation r = new Relation();
        r.addMember(new RelationMember("foo", ways.get(0)));
        r.addMember(new RelationMember("bar", ways.get(1)));

        final RelationMemberConflictResolverModel model = new RelationMemberConflictResolverModel();
        model.populate(Collections.singleton(r), ways.subList(0, 2));
        model.prepareDefaultRelationDecisions();
        assertFalse(model.isResolvedCompletely());
    }

    @Test
    void testDifferentPresence() throws Exception {
        final List<Way> ways = buildTestDataSet();
        final Relation r = new Relation();
        r.addMember(new RelationMember("foo", ways.get(0)));

        final RelationMemberConflictResolverModel model = new RelationMemberConflictResolverModel();
        model.populate(Collections.singleton(r), ways.subList(0, 2));
        model.prepareDefaultRelationDecisions();
        assertFalse(model.isResolvedCompletely());
    }

    @Test
    void testEveryMemberIsPresentTwice() throws Exception {
        final List<Way> ways = buildTestDataSet();
        final Relation r = new Relation();
        r.addMember(new RelationMember("foo", ways.get(0)));
        r.addMember(new RelationMember("foo", ways.get(1)));
        r.addMember(new RelationMember("xoo", ways.get(2)));
        r.addMember(new RelationMember("bar", ways.get(0)));
        r.addMember(new RelationMember("bar", ways.get(1)));
        r.addMember(new RelationMember("xoo", ways.get(2)));

        final RelationMemberConflictResolverModel model = new RelationMemberConflictResolverModel();
        model.populate(Collections.singleton(r), ways.subList(0, 2));
        model.prepareDefaultRelationDecisions();
        assertTrue(model.isResolvedCompletely());
        assertEquals(RelationMemberConflictDecisionType.KEEP, model.getDecision(0).getDecision());
        assertEquals(ways.get(0), model.getDecision(0).getOriginalPrimitive());
        assertEquals("foo", model.getDecision(0).getRole());
        assertEquals(RelationMemberConflictDecisionType.KEEP, model.getDecision(1).getDecision());
        assertEquals(ways.get(0), model.getDecision(1).getOriginalPrimitive());
        assertEquals("bar", model.getDecision(1).getRole());
        assertEquals(RelationMemberConflictDecisionType.REMOVE, model.getDecision(2).getDecision());
        assertEquals(RelationMemberConflictDecisionType.REMOVE, model.getDecision(3).getDecision());
    }
}
