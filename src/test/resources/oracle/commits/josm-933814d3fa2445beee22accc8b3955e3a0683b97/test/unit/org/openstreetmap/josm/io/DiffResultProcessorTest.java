// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.DiffResultProcessor.DiffResultEntry;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.XmlParsingException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link DiffResultProcessor}
 */
class DiffResultProcessorTest {

    /**
     * Setup rule
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static void shouldFail(String s) {
        try {
            new DiffResultProcessor(null).parse(s, NullProgressMonitor.INSTANCE);
            fail("should throw exception");
        } catch (IllegalArgumentException | XmlParsingException e) {
            Logging.trace(e);
        }
    }

    /**
     * Unit test of {@link DiffResultProcessor#DiffResultProcessor}.
     */
    @Test
    void testConstructor() {
        Node n = new Node(1);
        // these calls should not fail
        //
        new DiffResultProcessor(null);
        new DiffResultProcessor(Collections.emptyList());
        new DiffResultProcessor(Collections.singleton(n));
    }

    /**
     * Test invalid cases.
     */
    @Test
    void testParse_NOK_Cases() {
        shouldFail(null);
        shouldFail("");
        shouldFail("<x></x>");
    }

    /**
     * Test valid cases.
     * @throws XmlParsingException never
     */
    @Test
    void testParse_OK_Cases() throws XmlParsingException {
        DiffResultProcessor processor = new DiffResultProcessor(null);
        String doc =
        "<diffResult version=\"0.6\" generator=\"Test Data\">\n" +
        "    <node old_id=\"-1\" new_id=\"1\" new_version=\"1\"/>\n" +
        "    <way old_id=\"-2\" new_id=\"2\" new_version=\"1\"/>\n" +
        "    <relation old_id=\"-3\" new_id=\"3\" new_version=\"1\"/>\n" +
        "</diffResult>";

        processor.parse(doc, null);
        assertEquals(3, processor.getDiffResults().size());
        SimplePrimitiveId id = new SimplePrimitiveId(-1, OsmPrimitiveType.NODE);
        DiffResultEntry entry = processor.getDiffResults().get(id);
        assertNotNull(entry);
        assertEquals(1, entry.newId);
        assertEquals(1, entry.newVersion);

        id = new SimplePrimitiveId(-2, OsmPrimitiveType.WAY);
        entry = processor.getDiffResults().get(id);
        assertNotNull(entry);
        assertEquals(2, entry.newId);
        assertEquals(1, entry.newVersion);

        id = new SimplePrimitiveId(-3, OsmPrimitiveType.RELATION);
        entry = processor.getDiffResults().get(id);
        assertNotNull(entry);
        assertEquals(3, entry.newId);
        assertEquals(1, entry.newVersion);
    }

    /**
     * Test {@link DiffResultProcessor#postProcess}
     * @throws XmlParsingException never
     */
    @Test
    void testPostProcess_Invocation_Variants() throws XmlParsingException {
        DiffResultProcessor processor = new DiffResultProcessor(null);
        String doc =
        "<diffResult version=\"0.6\" generator=\"Test Data\">\n" +
        "    <node old_id=\"-1\" new_id=\"1\" new_version=\"1\"/>\n" +
        "    <way old_id=\"-2\" new_id=\"2\" new_version=\"1\"/>\n" +
        "    <relation old_id=\"-3\" new_id=\"3\" new_version=\"1\"/>\n" +
        "</diffResult>";

        processor.parse(doc, null);

        // should all be ok
        //
        processor.postProcess(null, null);
        processor.postProcess(null, NullProgressMonitor.INSTANCE);
        processor.postProcess(new Changeset(1), null);
        processor.postProcess(new Changeset(1), NullProgressMonitor.INSTANCE);
    }

    /**
     * Test {@link DiffResultProcessor#postProcess}
     * @throws XmlParsingException never
     */
    @Test
    void testPostProcess_OK() throws XmlParsingException {

        Node n = new Node();
        Way w = new Way();
        Relation r = new Relation();

        String doc =
            "<diffResult version=\"0.6\" generator=\"Test Data\">\n" +
            "    <node old_id=\""+n.getUniqueId()+"\" new_id=\"1\" new_version=\"10\"/>\n" +
            "    <way old_id=\""+w.getUniqueId()+"\" new_id=\"2\" new_version=\"11\"/>\n" +
            "    <relation old_id=\""+r.getUniqueId()+"\" new_id=\"3\" new_version=\"12\"/>\n" +
            "</diffResult>";

        DiffResultProcessor processor = new DiffResultProcessor(Arrays.asList(n, w, r));
        processor.parse(doc, null);
        Set<OsmPrimitive> processed = processor.postProcess(new Changeset(5), null);
        assertEquals(3, processed.size());
        n = (Node) processed.stream().filter(x -> x.getUniqueId() == 1).findFirst().get();
        assertEquals(5, n.getChangesetId());
        assertEquals(10, n.getVersion());

        w = (Way) processed.stream().filter(x -> x.getUniqueId() == 2).findFirst().get();
        assertEquals(5, w.getChangesetId());
        assertEquals(11, w.getVersion());

        r = (Relation) processed.stream().filter(x -> x.getUniqueId() == 3).findFirst().get();
        assertEquals(5, r.getChangesetId());
        assertEquals(12, r.getVersion());
    }

    /**
     * Test {@link DiffResultProcessor#postProcess}
     * @throws XmlParsingException never
     */
    @Test
    void testPostProcess_ForCreatedElement() throws XmlParsingException {

        Node n = new Node();
        String doc =
            "<diffResult version=\"0.6\" generator=\"Test Data\">\n" +
            "    <node old_id=\""+n.getUniqueId()+"\" new_id=\"1\" new_version=\"1\"/>\n" +
            "</diffResult>";

        DiffResultProcessor processor = new DiffResultProcessor(Collections.singleton(n));
        processor.parse(doc, null);
        Set<OsmPrimitive> processed = processor.postProcess(new Changeset(5), null);
        assertEquals(1, processed.size());
        n = (Node) processed.stream().filter(x -> x.getUniqueId() == 1).findFirst().get();
        assertEquals(5, n.getChangesetId());
        assertEquals(1, n.getVersion());
    }

    /**
     * Test {@link DiffResultProcessor#postProcess}
     * @throws XmlParsingException never
     */
    @Test
    void testPostProcess_ForModifiedElement() throws XmlParsingException {

        Node n = new Node(1);
        n.setCoor(new LatLon(1, 1));
        n.setOsmId(1, 2);
        n.setModified(true);
        String doc =
            "<diffResult version=\"0.6\" generator=\"Test Data\">\n" +
            "    <node old_id=\""+n.getUniqueId()+"\" new_id=\""+n.getUniqueId()+"\" new_version=\"3\"/>\n" +
            "</diffResult>";

        DiffResultProcessor processor = new DiffResultProcessor(Collections.singleton(n));
        processor.parse(doc, null);
        Set<OsmPrimitive> processed = processor.postProcess(new Changeset(5), null);
        assertEquals(1, processed.size());
        n = (Node) processed.stream().filter(x -> x.getUniqueId() == 1).findFirst().get();
        assertEquals(5, n.getChangesetId());
        assertEquals(3, n.getVersion());
    }

    /**
     * Test {@link DiffResultProcessor#postProcess}
     * @throws XmlParsingException never
     */
    @Test
    void testPostProcess_ForDeletedElement() throws XmlParsingException {

        Node n = new Node(1);
        n.setCoor(new LatLon(1, 1));
        n.setOsmId(1, 2);
        n.setDeleted(true);
        String doc =
            "<diffResult version=\"0.6\" generator=\"Test Data\">\n" +
            "    <node old_id=\""+n.getUniqueId()+"\"/>\n" +
            "</diffResult>";

        DiffResultProcessor processor = new DiffResultProcessor(Collections.singleton(n));
        processor.parse(doc, null);
        Set<OsmPrimitive> processed = processor.postProcess(new Changeset(5), null);
        assertEquals(1, processed.size());
        n = (Node) processed.stream().filter(x -> x.getUniqueId() == 1).findFirst().get();
        assertEquals(5, n.getChangesetId());
        assertEquals(2, n.getVersion());
    }
}
