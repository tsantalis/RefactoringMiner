// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Additional unit tests for {@link OsmReader}.
 */
class ParseWithChangesetReaderTest {

    /**
     * Setup rule
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static DataSet getDataSet(String doc) throws IOException, IllegalDataException {
        try (InputStream is = new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8))) {
            return OsmReader.parseDataSet(is, null);
        }
    }

    private static void shouldFail(String doc) throws IOException {
        try {
            getDataSet(doc);
            fail("should throw exception");
        } catch (IllegalDataException e) {
            Logging.trace(e);
        }
    }

    /**
     * A new node with a changeset id. Ignore it.
     * @throws Exception never
     */
    @Test
    void test_1() throws Exception {
        String doc =
            "<osm version=\"0.6\">\n" +
            "<node id=\"-1\" lat=\"0.0\" lon=\"0.0\" changeset=\"1\">\n" +
            "    <tag k=\"external-id\" v=\"-1\"/>\n" +
            "</node>\n" +
            "</osm>";

        DataSet ds = getDataSet(doc);
        Node n = ds.getNodes().stream().filter(x -> "-1".equals(x.get("external-id"))).findFirst().get();
        assertNotNull(n);
        assertEquals(0, n.getChangesetId());
    }

    /**
     * A new node with an invalid changeset id. Ignore it.
     * @throws Exception never
     */
    @Test
    void test_11() throws Exception {
        String doc =
        "<osm version=\"0.6\">\n" +
        "<node id=\"-1\" lat=\"0.0\" lon=\"0.0\" changeset=\"0\">\n" +
        "    <tag k=\"external-id\" v=\"-1\"/>\n" +
        "</node>\n" +
        "</osm>";

        DataSet ds = getDataSet(doc);
        Node n = ds.getNodes().stream().filter(x -> "-1".equals(x.get("external-id"))).findFirst().get();
        assertNotNull(n);
        assertEquals(0, n.getChangesetId());
    }

    /**
     * A new node with an invalid changeset id. Ignore it.
     * @throws Exception never
     */
    @Test
    void test_12() throws Exception {
        String doc =
        "<osm version=\"0.6\">\n" +
        "<node id=\"-1\" lat=\"0.0\" lon=\"0.0\" changeset=\"-1\">\n" +
        "    <tag k=\"external-id\" v=\"-1\"/>\n" +
        "</node>\n" +
        "</osm>";

        DataSet ds = getDataSet(doc);
        Node n = ds.getNodes().stream().filter(x -> "-1".equals(x.get("external-id"))).findFirst().get();
        assertNotNull(n);
        assertEquals(0, n.getChangesetId());
    }

    /**
     * A new node with an invalid changeset id. Ignore it.
     * @throws Exception never
     */
    @Test
    void test_13() throws Exception {
        String doc =
        "<osm version=\"0.6\">\n" +
        "<node id=\"-1\" lat=\"0.0\" lon=\"0.0\" changeset=\"aaa\">\n" +
        "    <tag k=\"external-id\" v=\"-1\"/>\n" +
        "</node>\n" +
        "</osm>";

        DataSet ds = getDataSet(doc);
        Node n = ds.getNodes().stream().filter(x -> "-1".equals(x.get("external-id"))).findFirst().get();
        assertNotNull(n);
        assertEquals(0, n.getChangesetId());
    }

    /**
     * A new node with a missing changeset id. That's fine. The changeset id
     * is reset to 0.
     * @throws Exception never
     */
    @Test
    void test_14() throws Exception {
        String doc =
        "<osm version=\"0.6\">\n" +
        "<node id=\"-1\" lat=\"0.0\" lon=\"0.0\" >\n" +
        "    <tag k=\"external-id\" v=\"-1\"/>\n" +
        "</node>\n" +
        "</osm>";

        DataSet ds = getDataSet(doc);
        Node n = ds.getNodes().stream().filter(x -> "-1".equals(x.get("external-id"))).findFirst().get();
        assertNotNull(n);
        assertEquals(0, n.getChangesetId());
    }


    /**
     * An existing node with a missing changeset id. That's fine. The changeset id
     * is reset to 0.
     * @throws Exception never
     */
    @Test
    void test_2() throws Exception {
        String doc =
        "<osm version=\"0.6\">\n" +
        "<node id=\"1\" lat=\"0.0\" lon=\"0.0\" version=\"1\"/>\n" +
        "</osm>";

        DataSet ds = getDataSet(doc);
        OsmPrimitive n = ds.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(n);
        assertEquals(1, n.getUniqueId());
        assertEquals(0, n.getChangesetId());
    }

    /**
     * An existing node with a valid changeset id id. That's fine. The changeset id
     * is applied.
     * @throws Exception never
     */
    @Test
    void test_3() throws Exception {
        String doc =
        "<osm version=\"0.6\">\n" +
        "<node id=\"1\" lat=\"0.0\" lon=\"0.0\" version=\"1\" changeset=\"4\"/>\n" +
        "</osm>";

        DataSet ds = getDataSet(doc);
        OsmPrimitive n = ds.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(n);
        assertEquals(1, n.getUniqueId());
        assertEquals(4, n.getChangesetId());
    }

    /**
     * An existing node with an invalid changeset id. That's a problem. An exception
     * is thrown.
     * @throws IOException never
     */
    @Test
    void test_4() throws IOException {
        String doc =
        "<osm version=\"0.6\">\n" +
        "<node id=\"1\" lat=\"0.0\" lon=\"0.0\" version=\"1\" changeset=\"-1\"/>\n" +
        "</osm>";

        shouldFail(doc);
    }

    /**
     * An existing node with an invalid changeset id. That's a problem. An exception
     * is thrown.
     * @throws IOException never
     */
    @Test
    void test_5() throws IOException {
        String doc =
        "<osm version=\"0.6\">\n" +
        "<node id=\"1\" lat=\"0.0\" lon=\"0.0\" version=\"1\" changeset=\"1.0\"/>\n" +
        "</osm>";

        shouldFail(doc);
    }

    /**
     * An existing node with an invalid changeset id. That's a problem. An exception
     * is thrown.
     * @throws IOException never
     */
    @Test
    void test_6() throws IOException {
        String doc =
            "<osm version=\"0.6\">\n" +
            "<node id=\"1\" lat=\"0.0\" lon=\"0.0\" version=\"1\" changeset=\"abc\"/>\n" +
            "</osm>";

        shouldFail(doc);
    }
}
