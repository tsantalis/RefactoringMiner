// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link OsmChangeBuilder}
 */
class OsmChangeBuilderTest {

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
        } catch (IllegalStateException e) {
            Logging.trace(e);
        }
    }

    /**
     * Test various constructor invocations
     */
    @Test
    void testConstructor() {
        Changeset cs = new Changeset(1);
        // should not fail
        new OsmChangeBuilder(cs);
        new OsmChangeBuilder(null);
        new OsmChangeBuilder(cs, "0.5");
        new OsmChangeBuilder(cs, null);
        new OsmChangeBuilder(null, null);
    }

    /**
     * Test the sequence of method calls. Should throw IllegalStateException if
     * the protocol start(),append()*, finish() is violated.
     */
    @Test
    void testSequenceOfMethodCalls() {
        Changeset cs = new Changeset(1);
        OsmChangeBuilder csBuilder = new OsmChangeBuilder(cs);

        // should be OK
        csBuilder.start();
        Node n = new Node(LatLon.ZERO);
        csBuilder.append(n);
        csBuilder.finish();

        shouldFail(() -> {
            OsmChangeBuilder builder = new OsmChangeBuilder(cs);
            builder.append(n);
        });

        shouldFail(() -> {
            OsmChangeBuilder builder = new OsmChangeBuilder(cs);
            builder.append(Arrays.asList(n));
        });

        shouldFail(() -> {
            OsmChangeBuilder builder = new OsmChangeBuilder(cs);
            builder.finish();
        });

        shouldFail(() -> {
            OsmChangeBuilder builder = new OsmChangeBuilder(cs);
            builder.start();
            builder.start();
        });
    }

    /**
     * Test building a document with a new node
     */
    @Test
    void testDocumentWithNewNode() {
        Changeset cs = new Changeset(1);
        OsmChangeBuilder builder = new OsmChangeBuilder(cs);
        Node n = new Node(LatLon.ZERO);

        builder.start();
        builder.append(n);
        builder.finish();

        assertEquals(String.format(
                "<osmChange version=\"0.6\" generator=\"JOSM\">%n" +
                "<create>%n" +
                "  <node id='" + n.getUniqueId() + "' changeset='1' lat='0.0' lon='0.0' />%n" +
                "</create>%n" +
                "</osmChange>%n"), builder.getDocument());
    }

    /**
     * Test building a document with a modified node
     */
    @Test
    void testDocumentWithModifiedNode() {
        Changeset cs = new Changeset(1);
        OsmChangeBuilder builder = new OsmChangeBuilder(cs);
        Node n = new Node(LatLon.ZERO);
        n.setOsmId(1, 1);
        n.setModified(true);

        builder.start();
        builder.append(n);
        builder.finish();

        assertEquals(String.format(
                "<osmChange version=\"0.6\" generator=\"JOSM\">%n" +
                "<modify>%n" +
                "  <node id='1' version='1' changeset='1' lat='0.0' lon='0.0' />%n" +
                "</modify>%n" +
                "</osmChange>%n"), builder.getDocument());
    }

    /**
     * Test building a document with a deleted node
     */
    @Test
    void testDocumentWithDeletedNode() {
        Changeset cs = new Changeset(1);
        OsmChangeBuilder builder = new OsmChangeBuilder(cs);
        Node n = new Node(LatLon.ZERO);
        n.setOsmId(1, 1);
        n.setDeleted(true);

        builder.start();
        builder.append(n);
        builder.finish();

        assertEquals(String.format(
                "<osmChange version=\"0.6\" generator=\"JOSM\">%n" +
                "<delete>%n" +
                "  <node id='1' version='1' changeset='1'/>%n" +
                "</delete>%n" +
                "</osmChange>%n"), builder.getDocument());
    }

    /**
     * Test building a mixed document.
     */
    @Test
    void testMixed() {
        Changeset cs = new Changeset(1);
        OsmChangeBuilder builder = new OsmChangeBuilder(cs);
        Node n1 = new Node(LatLon.ZERO);
        n1.setOsmId(1, 1);
        n1.setDeleted(true);

        Node n2 = new Node(LatLon.ZERO);

        Node n3 = new Node(LatLon.ZERO);
        n3.setOsmId(2, 1);
        n3.setModified(true);

        builder.start();
        builder.append(Arrays.asList(n1, n2, n3));
        builder.finish();

        assertEquals(String.format(
                "<osmChange version=\"0.6\" generator=\"JOSM\">%n" +
                "<delete>%n" +
                "  <node id='1' version='1' changeset='1'/>%n" +
                "</delete>%n" +
                "<create>%n" +
                "  <node id='" + n2.getUniqueId() + "' changeset='1' lat='0.0' lon='0.0' />%n" +
                "</create>%n" +
                "<modify>%n" +
                "  <node id='2' version='1' changeset='1' lat='0.0' lon='0.0' />%n" +
                "</modify>%n" +
                "</osmChange>%n"), builder.getDocument());
    }
}
