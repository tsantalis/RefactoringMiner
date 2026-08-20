// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Iterator;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link OsmReader} class.
 */
@BasicPreferences
class OsmJsonReaderTest {
    /**
     * Parse JSON.
     * @param osm OSM data in JSON format, without header/footer
     * @return data set
     * @throws Exception if any error occurs
     */
    private static DataSet parse(String osm) throws Exception {
        return parse(osm, "");
    }

    /**
     * Parse JSON.
     * @param osm OSM data in JSON format, without header/footer
     * @param extraContent extra content added after OSM elements
     * @return data set
     * @throws Exception if any error occurs
     */
    private static DataSet parse(String osm, String extraContent) throws Exception {
        try (InputStream in = new ByteArrayInputStream((
                "{\n" +
                "  \"version\": 0.6,\n" +
                "  \"generator\": \"Overpass API\",\n" +
                "  \"osm3s\": {\n" +
                "    \"timestamp_osm_base\": \"date\",\n" +
                "    \"copyright\": \"The data included in this document is from www.openstreetmap.org. " +
                                     "It has there been collected by a large group of contributors. " +
                                     "For individual attribution of each item please refer to " +
                                     "http://www.openstreetmap.org/api/0.6/[node|way|relation]/#id/history\"\n" +
                "  },\n" +
                "  \"elements\": [" + osm + "]\n" +
                extraContent +
                "}")
                .getBytes(StandardCharsets.UTF_8))) {
            return OsmJsonReader.parseDataSet(in, NullProgressMonitor.INSTANCE);
        }
    }


    /**
     * Test an example without data.
     * @throws Exception never
     */
    @Test
    void testHeader() throws Exception {
        DataSet ds = parse("");
        assertEquals("0.6", ds.getVersion());
    }

    /**
     * Test an example with the spatial data only.
     * @throws Exception never
     */
    @Test
    void testNodeSpatialData() throws Exception {
        DataSet ds = parse("{\n" +
                "  \"type\": \"node\",\n" +
                "  \"id\": 1,\n" +
                "  \"lat\": 2.0,\n" +
                "  \"lon\": -3.0\n" +
                "}");
        Node n = ds.getNodes().iterator().next();
        assertEquals(1, n.getUniqueId());
        assertEquals(new LatLon(2.0, -3.0), n.getCoor());
    }

    /**
     * Test an example with the meta data.
     * @throws Exception never
     */
    @Test
    void testNodeMetaData() throws Exception {
        DataSet ds = parse("{\n" +
                "  \"type\": \"node\",\n" +
                "  \"id\": 1,\n" +
                "  \"lat\": 2.0,\n" +
                "  \"lon\": -3.0,\n" +
                "  \"timestamp\": \"2018-01-01T00:00:00Z\",\n" +
                "  \"version\": 4,\n" +
                "  \"changeset\": 5,\n" +
                "  \"user\": \"somebody\",\n" +
                "  \"uid\": 6\n" +
                "}");
        Node n = ds.getNodes().iterator().next();
        assertEquals(1, n.getUniqueId());
        assertEquals(new LatLon(2.0, -3.0), n.getCoor());
        assertEquals(Instant.parse("2018-01-01T00:00:00Z"), n.getInstant());
        assertEquals(4, n.getVersion());
        assertEquals(5, n.getChangesetId());
        assertEquals(6, n.getUser().getId());
        assertEquals("somebody", n.getUser().getName());
    }

    /**
     * Test an example with tags.
     * @throws Exception never
     */
    @Test
    void testNodeTags() throws Exception {
        DataSet ds = parse("{\n" +
                "  \"type\": \"node\",\n" +
                "  \"id\": 1,\n" +
                "  \"lat\": 2.0,\n" +
                "  \"lon\": -3.0,\n" +
                "  \"tags\": {\n" +
                "    \"highway\": \"bus_stop\",\n" +
                "    \"name\": \"Main Street\"\n" +
                "  }" +
                "}");
        Node n = ds.getNodes().iterator().next();
        assertEquals(1, n.getUniqueId());
        assertEquals(new LatLon(2.0, -3.0), n.getCoor());
        assertTrue(n.isTagged());
        assertEquals("bus_stop", n.get("highway"));
        assertEquals("Main Street", n.get("name"));
    }

    /**
     * Test a way example.
     * @throws Exception never
     */
    @Test
    void testWay() throws Exception {
        DataSet ds = parse("{\n" +
                "  \"type\": \"way\",\n" +
                "  \"id\": 1,\n" +
                "  \"nodes\": [\n" +
                "    10,\n" +
                "    11,\n" +
                "    12\n" +
                "  ],\n" +
                "  \"tags\": {\n" +
                "    \"highway\": \"tertiary\",\n" +
                "    \"name\": \"Main Street\"\n" +
                "  }\n" +
                "}");
        Way w = ds.getWays().iterator().next();
        assertEquals(1, w.getUniqueId());
        assertEquals(3, w.getNodesCount());
        Iterator<Node> it = w.getNodes().iterator();
        assertEquals(10, it.next().getUniqueId());
        assertEquals(11, it.next().getUniqueId());
        assertEquals(12, it.next().getUniqueId());
        assertFalse(it.hasNext());
        assertTrue(w.isTagged());
        assertEquals("tertiary", w.get("highway"));
        assertEquals("Main Street", w.get("name"));
    }

    /**
     * Test a relation example.
     * @throws Exception never
     */
    @Test
    void testRelation() throws Exception {
        DataSet ds = parse("{\n" +
                "  \"type\": \"relation\",\n" +
                "  \"id\": 1,\n" +
                "  \"members\": [\n" +
                "    {\n" +
                "      \"type\": \"way\",\n" +
                "      \"ref\": 1745069,\n" +
                "      \"role\": \"\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"way\",\n" +
                "      \"ref\": 172789,\n" +
                "      \"role\": \"\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"tags\": {\n" +
                "    \"from\": \"Konrad-Adenauer-Platz\",\n" +
                "    \"name\": \"VRS 636\",\n" +
                "    \"network\": \"VRS\",\n" +
                "    \"operator\": \"SWB\",\n" +
                "    \"ref\": \"636\",\n" +
                "    \"route\": \"bus\",\n" +
                "    \"to\": \"Gielgen\",\n" +
                "    \"type\": \"route\",\n" +
                "    \"via\": \"Ramersdorf\"\n" +
                "  }\n" +
                "}");
        Relation r = ds.getRelations().iterator().next();
        assertEquals(1, r.getUniqueId());
        assertEquals(2, r.getMembersCount());
        Iterator<RelationMember> it = r.getMembers().iterator();
        assertEquals(1745069, it.next().getUniqueId());
        assertEquals(172789, it.next().getUniqueId());
        assertFalse(it.hasNext());
        assertTrue(r.isTagged());
        assertEquals("route", r.get("type"));

    }

    /**
     * Test a relation example without members.
     * @throws Exception never
     */
    @Test
    void testEmptyRelation() throws Exception {
        DataSet ds = parse("{\n" +
                "  \"type\": \"relation\",\n" +
                "  \"id\": 1,\n" +
                "  \"tags\": {}\n" +
                "}");
        Relation r = ds.getRelations().iterator().next();
        assertEquals(1, r.getUniqueId());
        assertEquals(0, r.getMembersCount());
    }

    /**
     * Test reading remark from Overpass API.
     * @throws Exception if any error occurs
     */
    @Test
    void testRemark() throws Exception {
        DataSet ds = parse("", "," +
                "  \"remark\": \"runtime error: Query ran out of memory in \\\"query\\\" at line 5.\"\n");
        assertEquals("runtime error: Query ran out of memory in \"query\" at line 5.", ds.getRemark());
    }
}
