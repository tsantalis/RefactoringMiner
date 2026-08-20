// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.spi.preferences.AbstractSetting;
import org.openstreetmap.josm.spi.preferences.ListListSetting;
import org.openstreetmap.josm.spi.preferences.ListSetting;
import org.openstreetmap.josm.spi.preferences.MapListSetting;
import org.openstreetmap.josm.spi.preferences.Setting;
import org.openstreetmap.josm.spi.preferences.StringSetting;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for class {@link PreferencesWriter}.
 */
// This is a preference test
@BasicPreferences
class PreferencesWriterTest {

    private static <T extends AbstractSetting<?>> T setting(T s, long time) {
        s.setTime(time);
        return s;
    }

    /**
     * Unit test of {@link PreferencesWriter#visit(ListListSetting)}.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testListList() throws IOException {
        try (StringWriter out = new StringWriter(); PreferencesWriter w = new PreferencesWriter(new PrintWriter(out), true, true)) {
            long time = System.currentTimeMillis() / 1000;
            w.visit(setting(new ListListSetting(Arrays.asList(Arrays.asList("bar"))), time));
            assertEquals(String.format(
                    "  <lists key='null' time='%d'>%n" +
                    "    <list>%n" +
                    "      <entry value='bar'/>%n" +
                    "    </list>%n" +
                    "  </lists>%n", time),
                    out.toString());
        }
    }

    /**
     * Unit test of {@link PreferencesWriter#visit(ListSetting)}.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testList() throws IOException {
        try (StringWriter out = new StringWriter(); PreferencesWriter w = new PreferencesWriter(new PrintWriter(out), true, true)) {
            long time = System.currentTimeMillis() / 1000;
            w.visit(setting(new ListSetting(Arrays.asList("bar")), time));
            assertEquals(String.format(
                    "  <list key='null' time='%d'>%n" +
                    "    <entry value='bar'/>%n" +
                    "  </list>%n", time),
                    out.toString());
        }
    }

    /**
     * Unit test of {@link PreferencesWriter#visit(MapListSetting)}.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testMapList() throws IOException {
        try (StringWriter out = new StringWriter(); PreferencesWriter w = new PreferencesWriter(new PrintWriter(out), true, true)) {
            long time = System.currentTimeMillis() / 1000;
            Map<String, String> map = new HashMap<>();
            map.put("foo", "bar");
            w.visit(setting(new MapListSetting(Arrays.asList(map)), time));
            assertEquals(String.format(
                    "  <maps key='null' time='%d'>%n" +
                    "    <map>%n" +
                    "      <tag key='foo' value='bar'/>%n" +
                    "    </map>%n" +
                    "  </maps>%n", time),
                    out.toString());
        }
    }

    /**
     * Unit test of {@link PreferencesWriter#visit(StringSetting)}.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testString() throws IOException {
        try (StringWriter out = new StringWriter(); PreferencesWriter w = new PreferencesWriter(new PrintWriter(out), true, true)) {
            long time = System.currentTimeMillis() / 1000;
            w.visit(setting(new StringSetting("bar"), time));
            assertEquals(String.format(
                    "  <tag key='null' time='%d' value='bar'/>%n", time),
                    out.toString());
        }
    }

    /**
     * Unit test of {@link PreferencesWriter#write(java.util.Collection)}.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testWrite() throws IOException {
        try (StringWriter out = new StringWriter(); PreferencesWriter w = new PreferencesWriter(new PrintWriter(out), true, true)) {
            long time = System.currentTimeMillis() / 1000;
            Map<String, Setting<?>> map = new HashMap<>();
            map.put("foo", setting(new StringSetting("bar"), time));
            w.write(map.entrySet());
            assertEquals(String.format(
                    // CHECKSTYLE.OFF: LineLength
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n" +
                    "<preferences-defaults xmlns='http://josm.openstreetmap.de/preferences-1.0' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' version='%d'>%n" +
                    "  <tag key='foo' time='%d' value='bar'/>%n" +
                    "</preferences-defaults>%n",
                    // CHECKSTYLE.ON: LineLength
                    Version.getInstance().getVersion(), time),
                    out.toString());
        }
    }

    /**
     * Test null value in default preferences.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testNullValue() throws IOException {
        long time = System.currentTimeMillis() / 1000;
        // CHECKSTYLE.OFF: LineLength
        String expected = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n" +
                "<preferences-defaults xmlns='http://josm.openstreetmap.de/preferences-1.0' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' version='%d'>%n" +
                "  <list key='foo_list' time='%2$d' xsi:nil='true'/>%n" +
                "  <lists key='foo_listlist' time='%2$d' xsi:nil='true'/>%n" +
                "  <maps key='foo_maplist' time='%2$d' xsi:nil='true'/>%n" +
                "  <tag key='foo_tag' time='%2$d' xsi:nil='true'/>%n" +
                "</preferences-defaults>%n",
                Version.getInstance().getVersion(), time);
        // CHECKSTYLE.ON: LineLength
        try (StringWriter out = new StringWriter(); PreferencesWriter w = new PreferencesWriter(new PrintWriter(out), true, true)) {
            SortedMap<String, Setting<?>> map = new TreeMap<>();
            map.put("foo_tag", setting(new StringSetting(null), time));
            map.put("foo_list", setting(new ListSetting(null), time));
            map.put("foo_listlist", setting(new ListListSetting(null), time));
            map.put("foo_maplist", setting(new MapListSetting(null), time));
            w.write(map.entrySet());
            assertEquals(expected, out.toString());
        }
    }
}
