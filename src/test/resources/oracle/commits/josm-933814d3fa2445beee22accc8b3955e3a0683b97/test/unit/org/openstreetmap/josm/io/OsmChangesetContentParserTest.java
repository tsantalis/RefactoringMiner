// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.ChangesetDataSet;
import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetModificationType;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.data.osm.history.HistoryRelation;
import org.openstreetmap.josm.data.osm.history.HistoryWay;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.XmlParsingException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link OsmChangesetContentParser}.
 */
class OsmChangesetContentParserTest {

    /**
     * Setup rule
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static void shouldFail(Runnable r) {
        try {
            r.run();
            fail("should throw exception");
        } catch (IllegalArgumentException e) {
            Logging.trace(e);
        }
    }

    /**
     * Test various constructor invocations
     */
    @Test
    @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF_NONVIRTUAL")
    void test_Constructor() {

        // should be OK
        new OsmChangesetContentParser(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));

        shouldFail(() -> {
            new OsmChangesetContentParser((String) null);
        });

        shouldFail(() -> {
            new OsmChangesetContentParser((InputStream) null);
        });
    }

    /**
     * Test various invocations of {@link OsmChangesetContentParser#parse}
     * @throws XmlParsingException never
     */
    @Test
    void test_parse_arguments() throws XmlParsingException {
        OsmChangesetContentParser parser;

        String doc = "<osmChange version=\"0.6\" generator=\"OpenStreetMap server\"></osmChange>";

        // should be OK
        parser = new OsmChangesetContentParser(new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8)));
        parser.parse(null);

        // should be OK
        parser = new OsmChangesetContentParser(new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8)));
        parser.parse(NullProgressMonitor.INSTANCE);

        // should be OK
        parser = new OsmChangesetContentParser(doc);
        parser.parse(null);
    }

    /**
     * A simple changeset content document with one created node
     * @throws XmlParsingException never
     */
    @Test
    void test_OK_OneCreatedNode() throws XmlParsingException {
        OsmChangesetContentParser parser;

        String doc =
            "<osmChange version=\"0.6\" generator=\"OpenStreetMap server\">\n" +
            "  <create>\n" +
            "    <node id=\"1\" version=\"1\" visible=\"true\" changeset=\"1\" lat=\"1.0\" lon=\"1.0\" timestamp=\"2009-12-22\" />\n" +
            "  </create>\n" +
            "</osmChange>";

        // should be OK
        parser = new OsmChangesetContentParser(doc);
        ChangesetDataSet ds = parser.parse();

        assertEquals(1, ds.size());
        HistoryOsmPrimitive p = ds.getPrimitive(new SimplePrimitiveId(1, OsmPrimitiveType.NODE));
        assertNotNull(p);
        assertEquals(1, p.getId());
        assertEquals(1, p.getVersion());
        assertEquals(1, p.getChangesetId());
        assertNotNull(p.getInstant());
        assertEquals(ChangesetModificationType.CREATED, ds.getModificationType(p.getPrimitiveId()));
        assertTrue(ds.isCreated(p.getPrimitiveId()));
    }

    /**
     * A simple changeset content document with one updated node
     * @throws XmlParsingException never
     */
    @Test
    void test_OK_OneUpdatedNode() throws XmlParsingException {
        OsmChangesetContentParser parser;

        String doc =
            "<osmChange version=\"0.6\" generator=\"OpenStreetMap server\">\n" +
            "  <modify>\n" +
            "    <node id=\"1\" version=\"1\" visible=\"true\" changeset=\"1\" lat=\"1.0\" lon=\"1.0\" timestamp=\"2009-12-22\" />\n" +
            "  </modify>\n" +
            "</osmChange>";

        // should be OK
        parser = new OsmChangesetContentParser(doc);
        ChangesetDataSet ds = parser.parse();

        assertEquals(1, ds.size());
        HistoryOsmPrimitive p = ds.getPrimitive(new SimplePrimitiveId(1, OsmPrimitiveType.NODE));
        assertNotNull(p);
        assertEquals(1, p.getId());
        assertEquals(1, p.getVersion());
        assertEquals(1, p.getChangesetId());
        assertNotNull(p.getInstant());
        assertEquals(ChangesetModificationType.UPDATED, ds.getModificationType(p.getPrimitiveId()));
        assertTrue(ds.isUpdated(p.getPrimitiveId()));
    }

    /**
     * A simple changeset content document with one deleted node
     * @throws XmlParsingException never
     */
    @Test
    void test_OK_OneDeletedNode() throws XmlParsingException {
        OsmChangesetContentParser parser;

        String doc =
            "<osmChange version=\"0.6\" generator=\"OpenStreetMap server\">\n" +
            "  <delete>\n" +
            "    <node id=\"1\" version=\"1\" visible=\"true\" changeset=\"1\" lat=\"1.0\" lon=\"1.0\" timestamp=\"2009-12-22\" />\n" +
            "  </delete>\n" +
            "</osmChange>";

        // should be OK
        parser = new OsmChangesetContentParser(doc);
        ChangesetDataSet ds = parser.parse();

        assertEquals(1, ds.size());
        HistoryOsmPrimitive p = ds.getPrimitive(new SimplePrimitiveId(1, OsmPrimitiveType.NODE));
        assertNotNull(p);
        assertEquals(1, p.getId());
        assertEquals(1, p.getVersion());
        assertEquals(1, p.getChangesetId());
        assertNotNull(p.getInstant());
        assertEquals(ChangesetModificationType.DELETED, ds.getModificationType(p.getPrimitiveId()));
        assertTrue(ds.isDeleted(p.getPrimitiveId()));
    }

    /**
     * A more complex test with a document including nodes, ways, and relations.
     * @throws XmlParsingException never
     */
    @Test
    void test_OK_ComplexTestCase() throws XmlParsingException {
        OsmChangesetContentParser parser;

        String doc =
            "<osmChange version=\"0.6\" generator=\"OpenStreetMap server\">\n" +
            "  <create>\n" +
            "    <node id=\"1\" version=\"1\" visible=\"true\" changeset=\"1\" lat=\"1.0\" lon=\"1.0\" timestamp=\"2009-12-22\">\n" +
            "      <tag k=\"a.key\" v=\"a.value\" />\n" +
            "    </node>\n" +
            "  </create>\n" +
            "  <modify>\n" +
            "   <way id=\"2\" version=\"2\" visible=\"true\" changeset=\"1\" timestamp=\"2009-12-22\">\n" +
            "      <nd ref=\"21\"/>\n" +
            "      <nd ref=\"22\"/>\n" +
            "   </way>\n" +
            " </modify>\n" +
            " <delete>\n" +
            "    <relation id=\"3\" version=\"3\" visible=\"true\" changeset=\"1\" timestamp=\"2009-12-22\" />\n" +
            "  </delete>\n" +
            "</osmChange>";

        // should be OK
        parser = new OsmChangesetContentParser(doc);
        ChangesetDataSet ds = parser.parse();

        assertEquals(3, ds.size());

        HistoryOsmPrimitive p = ds.getPrimitive(new SimplePrimitiveId(1, OsmPrimitiveType.NODE));
        assertNotNull(p);
        assertEquals(1, p.getId());
        assertEquals(1, p.getVersion());
        assertEquals(1, p.getChangesetId());
        assertNotNull(p.getInstant());
        assertEquals(ChangesetModificationType.CREATED, ds.getModificationType(p.getPrimitiveId()));
        assertTrue(ds.isCreated(p.getPrimitiveId()));
        assertEquals("a.value", p.get("a.key"));

        HistoryWay w = (HistoryWay) ds.getPrimitive(new SimplePrimitiveId(2, OsmPrimitiveType.WAY));
        assertNotNull(w);
        assertEquals(2, w.getId());
        assertEquals(2, w.getVersion());
        assertEquals(1, w.getChangesetId());
        assertNotNull(w.getInstant());
        assertEquals(ChangesetModificationType.UPDATED, ds.getModificationType(w.getPrimitiveId()));
        assertTrue(ds.isUpdated(w.getPrimitiveId()));
        assertEquals(2, w.getNumNodes());
        assertEquals(Arrays.asList(21L, 22L), w.getNodes());

        HistoryRelation r = (HistoryRelation) ds.getPrimitive(new SimplePrimitiveId(3, OsmPrimitiveType.RELATION));
        assertNotNull(r);
        assertEquals(3, r.getId());
        assertEquals(3, r.getVersion());
        assertEquals(1, r.getChangesetId());
        assertNotNull(r.getInstant());
        assertEquals(ChangesetModificationType.DELETED, ds.getModificationType(r.getPrimitiveId()));
        assertTrue(ds.isDeleted(r.getPrimitiveId()));
    }
}
