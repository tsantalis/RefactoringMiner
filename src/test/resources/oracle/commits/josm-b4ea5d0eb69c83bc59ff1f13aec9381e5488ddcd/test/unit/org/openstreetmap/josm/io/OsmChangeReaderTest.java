// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.Pair;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link OsmChangeReader}.
 */
@BasicPreferences
class OsmChangeReaderTest {
    /**
     * Parse osmChange.
     * @param osm OSM data in osmChange format, without header/footer
     * @return data set
     * @throws Exception if any error occurs
     */
    private static Pair<DataSet, NoteData> parse(String osm) throws Exception {
        try (InputStream in = new ByteArrayInputStream((
                "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n" +
                "<osmChange generator=\"test\" version=\"0.6\">" + osm + "</osmChange>")
                .getBytes(StandardCharsets.UTF_8))) {
            return OsmChangeReader.parseDataSetAndNotes(in, NullProgressMonitor.INSTANCE);
        }
    }

    /**
     * Checks reading of OsmAnd notes.
     * @throws Exception never
     */
    @Test
    void testNotes() throws Exception {
        NoteData nd = parse(
                "<create>\r\n" +
                "    <note lat=\"50.23887555404037\" lon=\"13.358299552342795\" id=\"-2\">\r\n" +
                "      <comment text=\"something\" />\r\n" +
                "    </note>\r\n" +
                "    <note lat=\"50.5\" lon=\"13.5\" id=\"-3\">\r\n" +
                "      <comment text=\"something else\" />\r\n" +
                "    </note>\r\n" +
                "    <note lat=\"50.75\" lon=\"13.75\" id=\"-4\">\r\n" +
                "      <comment text=\"\" />\r\n" +
                "    </note>\r\n" +
                "  </create>\r\n" +
                "  <modify />\r\n" +
                "  <delete />").b;
        Collection<Note> notes = nd.getSortedNotes();
        assertEquals(2, notes.size());
        Iterator<Note> iterator = notes.iterator();
        Note n = iterator.next();
        assertEquals(new LatLon(50.23887555404037, 13.358299552342795), n.getLatLon());
        assertEquals("something", n.getFirstComment().getText());
        n = iterator.next();
        assertEquals(new LatLon(50.5, 13.5), n.getLatLon());
        assertEquals("something else", n.getFirstComment().getText());
    }
}
