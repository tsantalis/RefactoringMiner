// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DownloadPolicy;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.UploadPolicy;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link OsmWriter} class.
 */
@BasicPreferences
class OsmWriterTest {
    /**
     * Unit test of {@link OsmWriter#byIdComparator}.
     */
    @Test
    void testByIdComparator() {

        final List<NodeData> ids = new ArrayList<>();
        for (Long id : Arrays.asList(12L, Long.MIN_VALUE, 65L, -12L, 2L, 0L, -3L, -20L, Long.MAX_VALUE)) {
            final NodeData n = new NodeData();
            n.setId(id);
            ids.add(n);
        }

        Collections.sort(ids, OsmWriter.byIdComparator);

        final long[] longIds = ids.stream().mapToLong(NodeData::getUniqueId).toArray();
        assertArrayEquals(new long[] {
                -3, -12, -20, -9223372036854775808L, 0, 2, 12, 65, 9223372036854775807L
        }, longIds);
    }

    /**
     * Unit test of {@link OsmWriter#header(DownloadPolicy, UploadPolicy)}.
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testHeader() throws IOException {
        doTestHeader(null, null,
                "<osm version='0.6' generator='JOSM'>");
        doTestHeader(DownloadPolicy.NORMAL, UploadPolicy.NORMAL,
                "<osm version='0.6' generator='JOSM'>");
        doTestHeader(DownloadPolicy.BLOCKED, UploadPolicy.BLOCKED,
                "<osm version='0.6' download='never' upload='never' generator='JOSM'>");
    }

    private static void doTestHeader(DownloadPolicy download, UploadPolicy upload, String expected) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
             OsmWriter writer = OsmWriterFactory.createOsmWriter(out, true, OsmWriter.DEFAULT_API_VERSION)) {
            writer.header(download, upload);
        }
        assertEquals("<?xml version='1.0' encoding='UTF-8'?>" + expected,
                new String(baos.toByteArray(), StandardCharsets.UTF_8)
                        .replaceAll("\r", "")
                        .replaceAll("\n", ""));
    }

    /**
     * Unit test of {@link OsmWriter#write} with dataset locked.
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteLock() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
             OsmWriter writer = OsmWriterFactory.createOsmWriter(out, true, OsmWriter.DEFAULT_API_VERSION)) {
            DataSet ds = new DataSet();
            ds.lock();
            writer.write(ds);
        }
        assertEquals("<?xml version='1.0' encoding='UTF-8'?><osm version='0.6' locked='true' generator='JOSM'></osm>",
                new String(baos.toByteArray(), StandardCharsets.UTF_8)
                        .replaceAll("\r", "")
                        .replaceAll("\n", ""));
    }

    /**
     * Unit test of {@link OsmWriter#visit(Changeset)}.
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testChangeset() throws IOException {
        Changeset cs = new Changeset();
        cs.setUser(User.getAnonymous());
        cs.setId(38038262);
        cs.setMin(new LatLon(12., 34.));
        cs.setMax(new LatLon(56., 78.));
        cs.setCreatedAt(Instant.EPOCH);
        try (StringWriter stringWriter = new StringWriter();
             OsmWriter osmWriter = OsmWriterFactory.createOsmWriter(new PrintWriter(stringWriter), true, OsmWriter.DEFAULT_API_VERSION)) {
            osmWriter.visit(cs);
            assertEquals("  <changeset id='38038262' user='&lt;anonymous&gt;' uid='-1' created_at='1970-01-01T00:00:00Z' open='false' " +
                            "min_lon='34.0' min_lat='12.0' max_lon='78.0' max_lat='56.0'>\n  </changeset>\n",
                    stringWriter.toString().replace("\r", ""));
        }
    }

    /**
     * Unit test of {@link OsmWriter#visit(INode)}.
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testNode() throws IOException {
        Node node = new Node(1, 42);
        node.setCoor(new LatLon(12., 34.));
        node.setInstant(Instant.parse("2006-05-10T18:27:47Z"));
        try (StringWriter stringWriter = new StringWriter();
             OsmWriter osmWriter = OsmWriterFactory.createOsmWriter(new PrintWriter(stringWriter), true, OsmWriter.DEFAULT_API_VERSION)) {
            osmWriter.visit(node);
            assertEquals("  <node id='1' timestamp='2006-05-10T18:27:47Z' visible='true' version='42' lat='12.0' lon='34.0' />\n",
                    stringWriter.toString().replace("\r", ""));
        }
    }
}
