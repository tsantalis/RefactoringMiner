// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Map;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Test {@link PrimitiveTagTransferData}
 * @author Michael Zangl
 */
// Only required due to OSM primitive dependencies
@BasicPreferences
class PrimitiveTagTransferDataTest {
    private static boolean isHeterogeneousSource(PrimitiveData... t) {
        return new PrimitiveTagTransferData(Arrays.asList(t)).isHeterogeneousSource();
    }

    /**
     * Test method for {@link PrimitiveTagTransferData#PrimitiveTagTransferData(PrimitiveTransferData)}.
     */
    @Test
    void testPrimitiveTagTransferDataPrimitiveTransferData() {
        PrimitiveTagTransferData data = new PrimitiveTagTransferData(PrimitiveTransferData.getData(Arrays.asList(new Node(), new Node())));
        assertEquals(2, data.getSourcePrimitiveCount(OsmPrimitiveType.NODE));
        assertEquals(0, data.getSourcePrimitiveCount(OsmPrimitiveType.WAY));
        assertEquals(0, data.getSourcePrimitiveCount(OsmPrimitiveType.RELATION));
    }

    /**
     * Test method for {@link PrimitiveTagTransferData#isHeterogeneousSource()}.
     */
    @Test
    void testIsHeterogeneousSource() {
        // 0 item
        assertFalse(isHeterogeneousSource());
        // 1 item
        assertFalse(isHeterogeneousSource(new NodeData()));
        assertFalse(isHeterogeneousSource(new WayData()));
        assertFalse(isHeterogeneousSource(new RelationData()));
        // 2 items of same type
        assertFalse(isHeterogeneousSource(new NodeData(), new NodeData()));
        assertFalse(isHeterogeneousSource(new WayData(), new WayData()));
        assertFalse(isHeterogeneousSource(new RelationData(), new RelationData()));
        // 2 items of different type
        assertTrue(isHeterogeneousSource(new NodeData(), new WayData()));
        assertTrue(isHeterogeneousSource(new NodeData(), new RelationData()));
        assertTrue(isHeterogeneousSource(new WayData(), new RelationData()));
    }

    /**
     * Test method for {@link PrimitiveTagTransferData#getForPrimitives(OsmPrimitiveType)}.
     */
    @Test
    void testGetForPrimitives() {
        PrimitiveTagTransferData data = createTestData();
        TagCollection forNode = data.getForPrimitives(OsmPrimitiveType.NODE);
        assertEquals(1, forNode.getKeys().size());
        assertEquals(2, forNode.getValues("k").size());
        TagCollection forWay = data.getForPrimitives(OsmPrimitiveType.WAY);
        assertEquals(1, forWay.getKeys().size());
        assertEquals(1, forWay.getValues("x").size());
        TagCollection forRelation = data.getForPrimitives(OsmPrimitiveType.RELATION);
        assertEquals(0, forRelation.getKeys().size());
    }

    private PrimitiveTagTransferData createTestData() {
        NodeData nd1 = new NodeData();
        nd1.put("k", "v");
        NodeData nd2 = new NodeData();
        nd2.put("k", "v2");
        WayData way = new WayData();
        way.put("x", "v");
        return new PrimitiveTagTransferData(Arrays.asList(nd1, nd2, way));
    }

    /**
     * Test method for {@link PrimitiveTagTransferData#getSourcePrimitiveCount(OsmPrimitiveType)}.
     */
    @Test
    void testGetSourcePrimitiveCount() {
        PrimitiveTagTransferData data = createTestData();
        assertEquals(2, data.getSourcePrimitiveCount(OsmPrimitiveType.NODE));
        assertEquals(1, data.getSourcePrimitiveCount(OsmPrimitiveType.WAY));
        assertEquals(0, data.getSourcePrimitiveCount(OsmPrimitiveType.RELATION));
    }

    /**
     * Test method for {@link PrimitiveTagTransferData#getStatistics()}.
     */
    @Test
    void testGetStatistics() {
        PrimitiveTagTransferData data = createTestData();
        Map<OsmPrimitiveType, Integer> stats = data.getStatistics();
        assertEquals(2, (int) stats.get(OsmPrimitiveType.NODE));
        assertEquals(1, (int) stats.get(OsmPrimitiveType.WAY));
        assertEquals(null, stats.get(OsmPrimitiveType.RELATION));
    }
}
