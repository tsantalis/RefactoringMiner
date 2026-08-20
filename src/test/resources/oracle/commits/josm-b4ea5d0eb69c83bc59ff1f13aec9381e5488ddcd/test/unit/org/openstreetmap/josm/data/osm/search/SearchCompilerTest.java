// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.search;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.ExactKeyValue;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetMenu;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.gui.tagging.presets.items.Key;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.Logging;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Unit tests for class {@link SearchCompiler}.
 */
// We need prefs for this. We access preferences when creating OSM primitives
@BasicPreferences
@Timeout(30)
class SearchCompilerTest {
    private static final class SearchContext {
        final DataSet ds = new DataSet();
        final Node n1 = new Node(LatLon.ZERO);
        final Node n2 = new Node(new LatLon(5, 5));
        final Way w1 = new Way();
        final Way w2 = new Way();
        final Relation r1 = new Relation();
        final Relation r2 = new Relation();

        private final Match m;
        private final Match n;

        private SearchContext(String state) throws SearchParseError {
            m = SearchCompiler.compile(state);
            n = SearchCompiler.compile('-' + state);
            ds.addPrimitive(n1);
            ds.addPrimitive(n2);
            w1.addNode(n1);
            w1.addNode(n2);
            w2.addNode(n1);
            w2.addNode(n2);
            ds.addPrimitive(w1);
            ds.addPrimitive(w2);
            r1.addMember(new RelationMember("", w1));
            r1.addMember(new RelationMember("", w2));
            r2.addMember(new RelationMember("", w1));
            r2.addMember(new RelationMember("", w2));
            ds.addPrimitive(r1);
            ds.addPrimitive(r2);
        }

        private void match(OsmPrimitive p, boolean cond) {
            if (cond) {
                assertTrue(m.match(p), p::toString);
                assertFalse(n.match(p), p::toString);
            } else {
                assertFalse(m.match(p), p::toString);
                assertTrue(n.match(p), p::toString);
            }
        }
    }

    /**
     * Search anything.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testAny() throws SearchParseError {
        final SearchCompiler.Match c = SearchCompiler.compile("foo");
        assertTrue(c.match(OsmUtils.createPrimitive("node foobar=true")));
        assertTrue(c.match(OsmUtils.createPrimitive("node name=hello-foo-xy")));
        assertFalse(c.match(OsmUtils.createPrimitive("node name=X")));
        assertEquals("foo", c.toString());
    }

    /**
     * Search by equality key=value.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testEquals() throws SearchParseError {
        final SearchCompiler.Match c = SearchCompiler.compile("foo=bar");
        assertFalse(c.match(OsmUtils.createPrimitive("node foobar=true")));
        assertTrue(c.match(OsmUtils.createPrimitive("node foo=bar")));
        assertFalse(c.match(OsmUtils.createPrimitive("node fooX=bar")));
        assertFalse(c.match(OsmUtils.createPrimitive("node foo=barX")));
        assertEquals("foo=bar", c.toString());
    }

    /**
     * Search by regular expression: key~value.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testRegexp() throws SearchParseError {
        final SearchCompiler.Match c = SearchCompiler.compile("foo~[Bb]a[rz]");
        assertFalse(c.match(OsmUtils.createPrimitive("node foobar=true")));
        assertFalse(c.match(OsmUtils.createPrimitive("node foo=foo")));
        assertTrue(c.match(OsmUtils.createPrimitive("node foo=bar")));
        assertTrue(c.match(OsmUtils.createPrimitive("node foo=baz")));
        assertTrue(c.match(OsmUtils.createPrimitive("node foo=Baz")));
        assertEquals("foo=[Bb]a[rz]", c.toString());
    }

    /**
     * Search by case-sensitive regular expression.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testRegexpCaseSensitive() throws Exception {
        SearchSetting searchSetting = new SearchSetting();
        searchSetting.regexSearch = true;
        searchSetting.text = "foo=\"^bar$\"";
        assertTrue(SearchCompiler.compile(searchSetting).match(OsmUtils.createPrimitive("node foo=bar")));
        assertTrue(SearchCompiler.compile(searchSetting).match(OsmUtils.createPrimitive("node foo=BAR")));
        searchSetting.caseSensitive = true;
        assertTrue(SearchCompiler.compile(searchSetting).match(OsmUtils.createPrimitive("node foo=bar")));
        assertFalse(SearchCompiler.compile(searchSetting).match(OsmUtils.createPrimitive("node foo=BAR")));
    }

    /**
     * Search by comparison.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testCompare() throws SearchParseError {
        final SearchCompiler.Match c1 = SearchCompiler.compile("start_date>1950");
        assertTrue(c1.match(OsmUtils.createPrimitive("node start_date=1950-01-01")));
        assertTrue(c1.match(OsmUtils.createPrimitive("node start_date=1960")));
        assertFalse(c1.match(OsmUtils.createPrimitive("node start_date=1950")));
        assertFalse(c1.match(OsmUtils.createPrimitive("node start_date=1000")));
        assertTrue(c1.match(OsmUtils.createPrimitive("node start_date=101010")));

        final SearchCompiler.Match c2 = SearchCompiler.compile("start_date<1960");
        assertTrue(c2.match(OsmUtils.createPrimitive("node start_date=1950-01-01")));
        assertFalse(c2.match(OsmUtils.createPrimitive("node start_date=1960")));
        assertTrue(c2.match(OsmUtils.createPrimitive("node start_date=1950")));
        assertTrue(c2.match(OsmUtils.createPrimitive("node start_date=1000")));
        assertTrue(c2.match(OsmUtils.createPrimitive("node start_date=200")));

        final SearchCompiler.Match c3 = SearchCompiler.compile("name<I");
        assertTrue(c3.match(OsmUtils.createPrimitive("node name=Alpha")));
        assertFalse(c3.match(OsmUtils.createPrimitive("node name=Sigma")));

        final SearchCompiler.Match c4 = SearchCompiler.compile("\"start_date\"<1960");
        assertTrue(c4.match(OsmUtils.createPrimitive("node start_date=1950-01-01")));
        assertFalse(c4.match(OsmUtils.createPrimitive("node start_date=2000")));

        final SearchCompiler.Match c5 = SearchCompiler.compile("height>180");
        assertTrue(c5.match(OsmUtils.createPrimitive("node height=200")));
        assertTrue(c5.match(OsmUtils.createPrimitive("node height=99999")));
        assertFalse(c5.match(OsmUtils.createPrimitive("node height=50")));
        assertFalse(c5.match(OsmUtils.createPrimitive("node height=-9999")));
        assertFalse(c5.match(OsmUtils.createPrimitive("node height=fixme")));

        final SearchCompiler.Match c6 = SearchCompiler.compile("name>C");
        assertTrue(c6.match(OsmUtils.createPrimitive("node name=Delta")));
        assertFalse(c6.match(OsmUtils.createPrimitive("node name=Alpha")));
    }

    /**
     * Search by nth.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testNth() throws SearchParseError {
        final DataSet dataSet = new DataSet();
        final Way way = new Way();
        final Node node0 = new Node(new LatLon(1, 1));
        final Node node1 = new Node(new LatLon(2, 2));
        final Node node2 = new Node(new LatLon(3, 3));
        dataSet.addPrimitive(way);
        dataSet.addPrimitive(node0);
        dataSet.addPrimitive(node1);
        dataSet.addPrimitive(node2);
        way.addNode(node0);
        way.addNode(node1);
        way.addNode(node2);
        assertFalse(SearchCompiler.compile("nth:2").match(node1));
        assertTrue(SearchCompiler.compile("nth:1").match(node1));
        assertFalse(SearchCompiler.compile("nth:0").match(node1));
        assertTrue(SearchCompiler.compile("nth:0").match(node0));
        assertTrue(SearchCompiler.compile("nth:2").match(node2));
        assertTrue(SearchCompiler.compile("nth:-1").match(node2));
        assertTrue(SearchCompiler.compile("nth:-2").match(node1));
        assertTrue(SearchCompiler.compile("nth:-3").match(node0));
    }

    /**
     * Search by negative nth.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testNthParseNegative() throws SearchParseError {
        assertEquals("Nth{nth=-1, modulo=false}", SearchCompiler.compile("nth:-1").toString());
    }

    /**
     * Search by modified status.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testModified() throws SearchParseError {
        SearchContext sc = new SearchContext("modified");
        // Not modified but new
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.w1, sc.r1}) {
            assertFalse(p.isModified(), p::toString);
            assertTrue(p.isNewOrUndeleted(), p::toString);
            sc.match(p, true);
        }
        // Modified and new
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.w1, sc.r1}) {
            p.setModified(true);
            assertTrue(p.isModified(), p::toString);
            assertTrue(p.isNewOrUndeleted(), p::toString);
            sc.match(p, true);
        }
        // Modified but not new
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.w1, sc.r1}) {
            p.setOsmId(1, 1);
            assertTrue(p.isModified(), p::toString);
            assertFalse(p.isNewOrUndeleted(), p::toString);
            sc.match(p, true);
        }
        // Not modified nor new
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n2, sc.w2, sc.r2}) {
            p.setOsmId(2, 2);
            assertFalse(p.isModified(), p::toString);
            assertFalse(p.isNewOrUndeleted(), p::toString);
            sc.match(p, false);
        }
    }

    /**
     * Search by selected status.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testSelected() throws SearchParseError {
        SearchContext sc = new SearchContext("selected");
        // Not selected
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.w1, sc.r1}) {
            assertFalse(p.isSelected(), p::toString);
            sc.match(p, false);
        }
        // Selected
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n2, sc.w2, sc.r2}) {
            sc.ds.addSelected(p);
            assertTrue(p.isSelected(), p::toString);
            sc.match(p, true);
        }
    }

    /**
     * Search by incomplete status.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testIncomplete() throws SearchParseError {
        SearchContext sc = new SearchContext("incomplete");
        // Not incomplete
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.w1, sc.r1}) {
            assertFalse(p.isIncomplete(), p::toString);
            sc.match(p, false);
        }
        // Incomplete
        sc.n2.setCoor(null);
        WayData wd = new WayData();
        wd.setIncomplete(true);
        sc.w2.load(wd);
        RelationData rd = new RelationData();
        rd.setIncomplete(true);
        sc.r2.load(rd);
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n2, sc.w2, sc.r2}) {
            assertTrue(p.isIncomplete(), p::toString);
            sc.match(p, true);
        }
    }

    /**
     * Search by untagged status.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testUntagged() throws SearchParseError {
        SearchContext sc = new SearchContext("untagged");
        // Untagged
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.w1, sc.r1}) {
            assertFalse(p.isTagged(), p::toString);
            sc.match(p, true);
        }
        // Tagged
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n2, sc.w2, sc.r2}) {
            p.put("foo", "bar");
            assertTrue(p.isTagged(), p::toString);
            sc.match(p, false);
        }
    }

    /**
     * Search by closed status.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testClosed() throws SearchParseError {
        SearchContext sc = new SearchContext("closed");
        // Closed
        sc.w1.addNode(sc.n1);
        for (Way w : new Way[]{sc.w1}) {
            assertTrue(w.isClosed(), w::toString);
            sc.match(w, true);
        }
        // Unclosed
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.n2, sc.w2, sc.r1, sc.r2}) {
            sc.match(p, false);
        }
    }

    /**
     * Search by new status.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testNew() throws SearchParseError {
        SearchContext sc = new SearchContext("new");
        // New
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.w1, sc.r1}) {
            assertTrue(p.isNew(), p::toString);
            sc.match(p, true);
        }
        // Not new
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n2, sc.w2, sc.r2}) {
            p.setOsmId(2, 2);
            assertFalse(p.isNew(), p::toString);
            sc.match(p, false);
        }
    }

    /**
     * Search for node objects.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testTypeNode() throws SearchParseError {
        final SearchContext sc = new SearchContext("type:node");
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.n2, sc.w1, sc.w2, sc.r1, sc.r2}) {
            sc.match(p, OsmPrimitiveType.NODE.equals(p.getType()));
        }
    }

    /**
     * Search for way objects.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testTypeWay() throws SearchParseError {
        final SearchContext sc = new SearchContext("type:way");
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.n2, sc.w1, sc.w2, sc.r1, sc.r2}) {
            sc.match(p, OsmPrimitiveType.WAY.equals(p.getType()));
        }
    }

    /**
     * Search for relation objects.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testTypeRelation() throws SearchParseError {
        final SearchContext sc = new SearchContext("type:relation");
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.n2, sc.w1, sc.w2, sc.r1, sc.r2}) {
            sc.match(p, OsmPrimitiveType.RELATION.equals(p.getType()));
        }
    }

    /**
     * Search for users.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testUser() throws SearchParseError {
        final SearchContext foobar = new SearchContext("user:foobar");
        foobar.n1.setUser(User.createLocalUser("foobar"));
        foobar.match(foobar.n1, true);
        foobar.match(foobar.n2, false);
        final SearchContext anonymous = new SearchContext("user:anonymous");
        anonymous.n1.setUser(User.createLocalUser("foobar"));
        anonymous.match(anonymous.n1, false);
        anonymous.match(anonymous.n2, true);
    }

    /**
     * Compiles "foo type bar" and tests the parse error message
     * @throws SearchParseError always
     */
    @Test
    void testFooTypeBar() throws SearchParseError {
        Exception e = assertThrows(SearchParseError.class, () -> SearchCompiler.compile("foo type bar"));
        assertEquals("<html>Expecting <code>:</code> after <i>type</i></html>", e.getMessage());
    }

    /**
     * Search for primitive timestamps.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testTimestamp() throws SearchParseError {
        final Node n1 = new Node();
        n1.setInstant(Instant.parse("2010-01-22T00:00:00Z"));
        assertTrue(SearchCompiler.compile("timestamp:2010/2011").match(n1));
        assertTrue(SearchCompiler.compile("timestamp:2010-01/2011").match(n1));
        assertTrue(SearchCompiler.compile("timestamp:2010-01-22/2011").match(n1));
        assertFalse(SearchCompiler.compile("timestamp:2010-01-23/2011").match(n1));
        assertFalse(SearchCompiler.compile("timestamp:2010/2010-01-21").match(n1));
        n1.setInstant(Instant.parse("2016-01-22T00:00:00Z"));
        assertFalse(SearchCompiler.compile("timestamp:2010/2011").match(n1));
    }

    /**
     * Tests the implementation of the Boolean logic.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testBooleanLogic() throws SearchParseError {
        final SearchCompiler.Match c1 = SearchCompiler.compile("foo AND bar AND baz");
        assertTrue(c1.match(OsmUtils.createPrimitive("node foobar=baz")));
        assertEquals("foo && bar && baz", c1.toString());
        final SearchCompiler.Match c2 = SearchCompiler.compile("foo AND (bar OR baz)");
        assertTrue(c2.match(OsmUtils.createPrimitive("node foobar=yes")));
        assertTrue(c2.match(OsmUtils.createPrimitive("node foobaz=yes")));
        assertEquals("foo && (bar || baz)", c2.toString());
        final SearchCompiler.Match c3 = SearchCompiler.compile("foo OR (bar baz)");
        assertEquals("foo || (bar && baz)", c3.toString());
        final SearchCompiler.Match c4 = SearchCompiler.compile("foo1 OR (bar1 bar2 baz1 XOR baz2) OR foo2");
        assertEquals("foo1 || (bar1 && bar2 && (baz1 ^ baz2)) || foo2", c4.toString());
        final SearchCompiler.Match c5 = SearchCompiler.compile("foo1 XOR (baz1 XOR (bar baz))");
        assertEquals("foo1 ^ baz1 ^ (bar && baz)", c5.toString());
        final SearchCompiler.Match c6 = SearchCompiler.compile("foo1 XOR ((baz1 baz2) XOR (bar OR baz))");
        assertEquals("foo1 ^ (baz1 && baz2) ^ (bar || baz)", c6.toString());
    }

    /**
     * Tests {@code buildSearchStringForTag}.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testBuildSearchStringForTag() throws SearchParseError {
        final Tag tag1 = new Tag("foo=", "bar\"");
        final Tag tag2 = new Tag("foo=", "=bar");
        final String search1 = SearchCompiler.buildSearchStringForTag(tag1.getKey(), tag1.getValue());
        assertEquals("\"foo=\"=\"bar\\\"\"", search1);
        assertTrue(SearchCompiler.compile(search1).match(tag1));
        assertFalse(SearchCompiler.compile(search1).match(tag2));
        final String search2 = SearchCompiler.buildSearchStringForTag(tag1.getKey(), "");
        assertEquals("\"foo=\"=*", search2);
        assertTrue(SearchCompiler.compile(search2).match(tag1));
        assertTrue(SearchCompiler.compile(search2).match(tag2));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/13870">Bug #13870</a>.
     */
    @Test
    void testPattern13870() {
        // https://bugs.openjdk.java.net/browse/JI-9044959
        SearchSetting setting = new SearchSetting();
        setting.regexSearch = true;
        setting.text = "[";
        assertThrows(SearchParseError.class, () -> SearchCompiler.compile(setting));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14217">Bug #14217</a>.
     * @throws Exception never
     */
    @Test
    void testTicket14217() throws Exception {
        assertNotNull(SearchCompiler.compile(new String(Files.readAllBytes(
                Paths.get(TestUtils.getRegressionDataFile(14217, "filter.txt"))), StandardCharsets.UTF_8)));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17112">Bug #17112</a>.
     */
    @Test
    void testTicket17112() {
        SearchSetting setting = new SearchSetting();
        setting.mapCSSSearch = true;
        setting.text = "w"; // partial input
        assertThrows(SearchParseError.class, () -> SearchCompiler.compile(setting));
    }

    /**
     * Test empty values.
     * @throws SearchParseError never
     */
    @Test
    void testEmptyValues15943() throws SearchParseError {
        Match matcher = SearchCompiler.compile("access=");
        assertTrue(matcher.match(new Tag("access", null)));
        assertTrue(matcher.match(new Tag("access", "")));
        assertFalse(matcher.match(new Tag("access", "private")));
    }

    /**
     * Test whether a key exists.
     * @throws SearchParseError never
     */
    @Test
    void testKeyExists15943() throws SearchParseError {
        Match matcher = SearchCompiler.compile("surface:");
        assertTrue(matcher.match(new Tag("surface", "")));
        assertTrue(matcher.match(new Tag("surface", "wood")));
        assertFalse(matcher.match(new Tag("surface:source", "xxx")));
        assertFalse(matcher.match(new Tag("foo", "bar")));
        assertFalse(matcher.match(new Tag("name", "foo:surface:bar")));
    }

    /**
     * Unit test of {@link SearchCompiler.ExactKeyValue.Mode} enum.
     */
    @Test
    void testEnumExactKeyValueMode() {
        assertDoesNotThrow(() -> TestUtils.superficialEnumCodeCoverage(ExactKeyValue.Mode.class));
    }

    /**
     * Robustness test for preset searching. Ensures that the query 'preset:' is not accepted.
     * @since 12464
     */
    @Test
    void testPresetSearchMissingValue() {
        SearchSetting settings = new SearchSetting();
        settings.text = "preset:";
        settings.mapCSSSearch = false;

        TaggingPresets.readFromPreferences();

        assertThrows(SearchParseError.class, () -> SearchCompiler.compile(settings));
    }

    /**
     * Robustness test for preset searching. Validates that it is not possible to search for
     * non existing presets.
     * @since 12464
     */
    @Test
    void testPresetNotExist() {
        String testPresetName = "groupnamethatshouldnotexist/namethatshouldnotexist";
        SearchSetting settings = new SearchSetting();
        settings.text = "preset:" + testPresetName;
        settings.mapCSSSearch = false;

        // load presets
        TaggingPresets.readFromPreferences();

        assertThrows(SearchParseError.class, () -> SearchCompiler.compile(settings));
    }

    /**
     * Robustness tests for preset searching. Ensures that combined preset names (having more than
     * 1 word) must be enclosed with " .
     * @since 12464
     */
    @Test
    void testPresetMultipleWords() {
        TaggingPreset testPreset = new TaggingPreset();
        testPreset.name = "Test Combined Preset Name";
        testPreset.group = new TaggingPresetMenu();
        testPreset.group.name = "TestGroupName";

        String combinedPresetname = testPreset.getRawName();
        SearchSetting settings = new SearchSetting();
        settings.text = "preset:" + combinedPresetname;
        settings.mapCSSSearch = false;

        // load presets
        TaggingPresets.readFromPreferences();

        assertThrows(SearchParseError.class, () -> SearchCompiler.compile(settings));
    }

    /**
     * Ensures that correct presets are stored in the {@link org.openstreetmap.josm.data.osm.search.SearchCompiler.Preset}
     * class against which the osm primitives are tested.
     * @throws SearchParseError if an error has been encountered while compiling
     * @throws NoSuchFieldException if there is no field called 'presets'
     * @throws IllegalAccessException if cannot access the field where all matching presets are stored
     * @since 12464
     */
    @Test
    void testPresetLookup() throws SearchParseError, NoSuchFieldException, IllegalAccessException {
        TaggingPreset testPreset = new TaggingPreset();
        testPreset.name = "Test Preset Name";
        testPreset.group = new TaggingPresetMenu();
        testPreset.group.name = "Test Preset Group Name";

        String query = "preset:" +
                "\"" + testPreset.getRawName() + "\"";
        SearchSetting settings = new SearchSetting();
        settings.text = query;
        settings.mapCSSSearch = false;

        // load presets and add the test preset
        TaggingPresets.readFromPreferences();
        TaggingPresets.addTaggingPresets(Collections.singletonList(testPreset));

        Match match = SearchCompiler.compile(settings);

        // access the private field where all matching presets are stored
        // and ensure that indeed the correct ones are there
        Field field = match.getClass().getDeclaredField("presets");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Collection<TaggingPreset> foundPresets = (Collection<TaggingPreset>) field.get(match);

        assertEquals(1, foundPresets.size());
        assertTrue(foundPresets.contains(testPreset));
    }

    /**
     * Ensures that the wildcard search works and that correct presets are stored in
     * the {@link org.openstreetmap.josm.data.osm.search.SearchCompiler.Preset} class against which
     * the osm primitives are tested.
     * @throws SearchParseError if an error has been encountered while compiling
     * @throws NoSuchFieldException if there is no field called 'presets'
     * @throws IllegalAccessException if cannot access the field where all matching presets are stored
     * @since 12464
     */
    @Test
    void testPresetLookupWildcard() throws SearchParseError, NoSuchFieldException, IllegalAccessException {
        TaggingPresetMenu group = new TaggingPresetMenu();
        group.name = "TestPresetGroup";

        TaggingPreset testPreset1 = new TaggingPreset();
        testPreset1.name = "TestPreset1";
        testPreset1.group = group;

        TaggingPreset testPreset2 = new TaggingPreset();
        testPreset2.name = "TestPreset2";
        testPreset2.group = group;

        TaggingPreset testPreset3 = new TaggingPreset();
        testPreset3.name = "TestPreset3";
        testPreset3.group = group;

        String query = "preset:" + "\"" + group.getRawName() + "/*\"";
        SearchSetting settings = new SearchSetting();
        settings.text = query;
        settings.mapCSSSearch = false;

        TaggingPresets.readFromPreferences();
        TaggingPresets.addTaggingPresets(Arrays.asList(testPreset1, testPreset2, testPreset3));

        Match match = SearchCompiler.compile(settings);

        // access the private field where all matching presets are stored
        // and ensure that indeed the correct ones are there
        Field field = match.getClass().getDeclaredField("presets");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Collection<TaggingPreset> foundPresets = (Collection<TaggingPreset>) field.get(match);

        assertEquals(3, foundPresets.size());
        assertTrue(foundPresets.contains(testPreset1));
        assertTrue(foundPresets.contains(testPreset2));
        assertTrue(foundPresets.contains(testPreset3));
    }

    /**
     * Ensures that correct primitives are matched against the specified preset.
     * @throws SearchParseError if an error has been encountered while compiling
     * @since 12464
     */
    @Test
    void testPreset() throws SearchParseError {
        final String presetName = "Test Preset Name";
        final String presetGroupName = "Test Preset Group";
        final String key = "test_key1";
        final String val = "test_val1";

        Key key1 = new Key();
        key1.key = key;
        key1.value = val;

        TaggingPreset testPreset = new TaggingPreset();
        testPreset.name = presetName;
        testPreset.types = Collections.singleton(TaggingPresetType.NODE);
        testPreset.data.add(key1);
        testPreset.group = new TaggingPresetMenu();
        testPreset.group.name = presetGroupName;

        TaggingPresets.readFromPreferences();
        TaggingPresets.addTaggingPresets(Collections.singleton(testPreset));

        String query = "preset:" + "\"" + testPreset.getRawName() + "\"";

        SearchContext ctx = new SearchContext(query);
        ctx.n1.put(key, val);
        ctx.n2.put(key, val);

        for (OsmPrimitive osm : new OsmPrimitive[] {ctx.n1, ctx.n2}) {
            ctx.match(osm, true);
        }

        for (OsmPrimitive osm : new OsmPrimitive[] {ctx.r1, ctx.r2, ctx.w1, ctx.w2}) {
            ctx.match(osm, false);
        }
    }

    /**
     * Unit test of methods {@link Match#equals} and {@link Match#hashCode}, including all subclasses.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        Set<Class<? extends Match>> matchers = TestUtils.getJosmSubtypes(Match.class);
        Assert.assertTrue(matchers.size() >= 10); // if it finds less than 10 classes, something is broken
        for (Class<?> c : matchers) {
            Logging.debug(c.toString());
            EqualsVerifier.forClass(c).usingGetClass()
                .suppress(Warning.NONFINAL_FIELDS, Warning.INHERITED_DIRECTLY_FROM_OBJECT)
                .withPrefabValues(TaggingPreset.class, newTaggingPreset("foo"), newTaggingPreset("bar"))
                .verify();
        }
    }

    private static TaggingPreset newTaggingPreset(String name) {
        TaggingPreset result = new TaggingPreset();
        result.name = name;
        return result;
    }

    /**
     * Search for {@code nodes:2}.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testNodeCount() throws SearchParseError {
        final SearchContext sc = new SearchContext("nodes:2");
        sc.match(sc.n1, false);
        sc.match(sc.w1, true);
        Node n3 = new Node(new LatLon(0, 5));
        sc.ds.addPrimitive(n3);
        sc.w1.addNode(n3);
        sc.match(sc.w1, false);
        sc.match(sc.r1, false);
    }

    /**
     * Search for {@code ways:2}.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testWayCount() throws SearchParseError {
        final SearchContext sc = new SearchContext("ways:2");
        sc.match(sc.n1, true);
        sc.ds.addPrimitive(new Way(sc.w2, true));
        sc.match(sc.n1, false);
        sc.match(sc.w1, false);
        sc.match(sc.r1, true);
        sc.r1.addMember(new RelationMember("", sc.n1));
        sc.match(sc.r1, true);
    }

    /**
     * Search for {@code members:2}.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testMemberCount() throws SearchParseError {
        final SearchContext sc = new SearchContext("members:2");
        sc.match(sc.n1, false);
        sc.match(sc.w1, false);
        sc.match(sc.r1, true);
        sc.r1.addMember(new RelationMember("", sc.n1));
        sc.match(sc.r1, false);
    }

    /**
     * Search for {@code role:foo}.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    void testRole() throws SearchParseError {
        final SearchContext sc = new SearchContext("role:foo");
        sc.match(sc.r1, false);
        sc.match(sc.w1, false);
        sc.match(sc.n1, false);
        sc.match(sc.n2, false);
        sc.r1.addMember(new RelationMember("foo", sc.n1));
        sc.match(sc.n1, true);
        sc.match(sc.n2, false);
    }
}
