// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Test class for {@link VectorWay}
 * @author Taylor Smock
 * @since 17862
 */
class VectorWayTest {
    @Test
    void testBBox() {
        VectorNode node1 = new VectorNode("test");
        VectorWay way = new VectorWay("test");
        way.setNodes(Collections.singletonList(node1));
        node1.setCoor(new LatLon(-5, 1));
        assertTrue(node1.getBBox().bboxIsFunctionallyEqual(way.getBBox(), 0.0));

        VectorNode node2 = new VectorNode("test");
        node2.setCoor(new LatLon(-10, 2));

        way.setNodes(Arrays.asList(node1, node2));
        assertTrue(way.getBBox().bboxIsFunctionallyEqual(new BBox(2, -10, 1, -5), 0.0));
    }

    @Test
    void testIdGenerator() {
        assertSame(new VectorWay("test").getIdGenerator(), new VectorWay("test").getIdGenerator());
    }

    @Test
    @BasicPreferences
    void testNodes() {
        VectorNode node1 = new VectorNode("test");
        VectorNode node2 = new VectorNode("test");
        VectorNode node3 = new VectorNode("test");
        node1.setId(1);
        node2.setId(2);
        node3.setId(3);
        VectorWay way = new VectorWay("test");
        assertNull(way.firstNode());
        assertNull(way.lastNode());
        assertFalse(way.isClosed());
        assertFalse(way.isFirstLastNode(node1));
        assertFalse(way.isInnerNode(node2));
        way.setNodes(Arrays.asList(node1, node2, node3));
        assertEquals(3, way.getNodesCount());
        assertEquals(node1, way.getNode(0));
        assertEquals(node2, way.getNode(1));
        assertEquals(node3, way.getNode(2));
        assertTrue(way.isFirstLastNode(node1));
        assertTrue(way.isFirstLastNode(node3));
        assertFalse(way.isFirstLastNode(node2));
        assertTrue(way.isInnerNode(node2));
        assertFalse(way.isInnerNode(node1));
        assertFalse(way.isInnerNode(node3));

        assertEquals(1, way.getNodeIds().get(0));
        assertEquals(2, way.getNodeIds().get(1));
        assertEquals(3, way.getNodeIds().get(2));
        assertEquals(1, way.getNodeId(0));
        assertEquals(2, way.getNodeId(1));
        assertEquals(3, way.getNodeId(2));

        assertFalse(way.isClosed());
        assertEquals(OsmPrimitiveType.WAY, way.getType());
        List<VectorNode> nodes = new ArrayList<>(way.getNodes());
        nodes.add(nodes.get(0));
        way.setNodes(nodes);
        assertTrue(way.isClosed());
        assertEquals(OsmPrimitiveType.CLOSEDWAY, way.getType());
    }

    @Test
    void testAccept() {
        VectorWay way = new VectorWay("test");
        List<VectorWay> visited = new ArrayList<>(1);
        way.accept(new PrimitiveVisitor() {
            @Override
            public void visit(INode n) {
                fail("No nodes should be visited");
            }

            @Override
            public void visit(IWay<?> w) {
                visited.add((VectorWay) w);
            }

            @Override
            public void visit(IRelation<?> r) {
                fail("No relations should be visited");
            }
        });

        assertEquals(1, visited.size());
        assertSame(way, visited.get(0));
    }
}
