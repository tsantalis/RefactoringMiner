// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.CustomMatchers.hasSize;
import static org.CustomMatchers.isEmpty;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.gui.tagging.presets.items.KeyedItem;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of "Opening hours" validation test.
 * @see OpeningHourTest
 */
class OpeningHourTestTest {
    /**
     * We need preferences for this. We check strings so we need i18n.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().i18n();

    private OpeningHourTest openingHourTest;

    /**
     * Setup test.
     * @throws Exception if test cannot be initialized
     */
    @BeforeEach
    public void setUp() throws Exception {
        openingHourTest = new OpeningHourTest();
        openingHourTest.initialize();
        ValidatorPrefHelper.PREF_OTHER.put(true);
    }

    /**
     * Test #1 of opening_hours syntax.
     */
    @Test
    void testCheckOpeningHourSyntax1() {
        final String key = "opening_hours";
        // frequently used tags according to https://taginfo.openstreetmap.org/keys/opening_hours#values
        assertThat(checkOpeningHourSyntax(key, "24/7"), isEmpty());
        assertThat(checkOpeningHourSyntax(key, "Mo-Fr 08:30-20:00"), isEmpty());
        assertThat(checkOpeningHourSyntax(key, "Mo-Fr sunrise-sunset"), isEmpty());
        assertThat(checkOpeningHourSyntax(key, "09:00-21:00"), isEmpty());
        assertThat(checkOpeningHourSyntax(key, "Su-Th sunset-24:00,04:00-sunrise; Fr-Sa sunset-sunrise"), isEmpty());
        assertThat(checkOpeningHourSyntax(key, "PH Su 10:00-12:00"), isEmpty()); // see #19743
        assertThat(checkOpeningHourSyntax(key, "SH Mo-Fr 09:33-15:35"), isEmpty()); // see #19743
    }

    /**
     * Test translated messages.
     */
    @Test
    void testI18n() {
        final String key = "opening_hours";
        String value = ".";
        assertEquals(String.format("Vorgefunden wurde:  \".\" \". \" in Zeile 0, Spalte 0%nErwartet wurde: <EOF>"),
                checkOpeningHourSyntax(key, value, Locale.GERMAN).get(0).getDescription());
        assertEquals(String.format("Encountered:  \".\" \". \" at line 0, column 0%nWas expecting: <EOF>"),
                checkOpeningHourSyntax(key, value, Locale.ENGLISH).get(0).getDescription());
        value = "Mon-Thu 12-18";
        assertEquals("Wochentag mit 3 Buchstaben in Zeile 1, Spalte 4; Wochentag mit 3 Buchstaben in Zeile 1, Spalte 9; Stunden ohne Minuten",
                checkOpeningHourSyntax(key, value, Locale.GERMAN).get(0).getDescription());
        assertEquals("Three character weekday at line 1, column 4; Three character weekday at line 1, column 9; Hours without minutes",
                checkOpeningHourSyntax(key, value, Locale.ENGLISH).get(0).getDescription());
    }

    /**
     * Test #2 of opening_hours syntax.
     */
    @Test
    void testCheckOpeningHourSyntax2() {
        final String key = "opening_hours";
        final List<TestError> errors = checkOpeningHourSyntax(key, "Mo-Tue");
        assertThat(errors, hasSize(1));
        assertFixEquals("Mo-Tu", errors.get(0));
        assertEquals("Three character weekday at line 1, column 6", errors.get(0).getDescription());
        assertEquals(Severity.WARNING, errors.get(0).getSeverity());
    }

    /**
     * Test #3 of opening_hours syntax.
     */
    @Test
    void testCheckOpeningHourSyntax3() {
        final String key = "opening_hours";
        final List<TestError> errors = checkOpeningHourSyntax(key, "Sa-Su 10.00-20.00");
        assertThat(errors, hasSize(1));
        assertFixEquals("Sa-Su 10:00-20:00", errors.get(0));
        assertEquals("Invalid minutes at line 1, column 12; Invalid minutes at line 1, column 17", errors.get(0).getDescription());
        assertEquals(Severity.WARNING, errors.get(0).getSeverity());
    }

    /**
     * Test #4 of opening_hours syntax.
     */
    @Test
    void testCheckOpeningHourSyntax4() {
        assertThat(checkOpeningHourSyntax(null, null), isEmpty());
        assertThat(checkOpeningHourSyntax(null, ""), isEmpty());
        assertEquals("opening_hours value can be prettified",
                checkOpeningHourSyntax("opening_hours", " ").get(0).getDescription());
        assertEquals("null value can be prettified",
                checkOpeningHourSyntax(null, " ").get(0).getDescription());
    }

    /**
     * Test #5 of opening_hours syntax.
     */
    @Test
    void testCheckOpeningHourSyntax5() {
        final String key = "opening_hours";
        assertThat(checkOpeningHourSyntax(key, "badtext"), hasSize(1));
        assertEquals(String.format("Encountered:  <UNEXPECTED_CHAR> \"b \" at line 0, column 0%nWas expecting: <EOF>"),
                checkOpeningHourSyntax(key, "badtext").get(0).getDescription().trim());
        assertThat(checkOpeningHourSyntax(key, "5.00 p.m-11.00 p.m"), hasSize(1));
        assertEquals(String.format("Encountered:  <UNEXPECTED_CHAR> \"p \" at line 1, column 3%nWas expecting: <EOF>; " +
                        "Encountered:  <UNEXPECTED_CHAR> \"p \" at line 1, column 13%n" +
                        "Was expecting: <EOF>"),
                checkOpeningHourSyntax(key, "5.00 p.m-11.00 p.m").get(0).getDescription());
    }

    /**
     * Test #6 of opening_hours syntax.
     */
    @Test
    void testCheckOpeningHourSyntax6() {
        final String key = "opening_hours";
        assertThat(checkOpeningHourSyntax(key, "PH open \"always open on public holidays\""), isEmpty());
    }

    /**
     * Test #7 of opening_hours syntax.
     */
    @Test
    void testCheckOpeningHourSyntax7() {
        final String key = "opening_hours";
        assertThat(checkOpeningHourSyntax(key, "9:00-18:00"), hasSize(1));
        assertEquals(Severity.OTHER, checkOpeningHourSyntax(key, "9:00-18:00").get(0).getSeverity());
        assertFixEquals("09:00-18:00", checkOpeningHourSyntax(key, "9:00-18:00").get(0));
    }

    /**
     * Non-regression Test of opening_hours syntax for bug #9367.
     */
    @Test
    void testCheckOpeningHourSyntaxTicket9367() {
        final String key = "opening_hours";
        assertEquals(Severity.WARNING, checkOpeningHourSyntax(key, "Mo,Tu 04-17").get(0).getSeverity());
        assertEquals("Hours without minutes",
                checkOpeningHourSyntax(key, "Mo,Tu 04-17").get(0).getDescription());
    }

    /**
     * Test #1 of service_times syntax.
     */
    @Test
    void testCheckServiceTimeSyntax1() {
        final String key = "service_times";
        // frequently used tags according to https://taginfo.openstreetmap.org/keys/service_times#values
        assertThat(checkOpeningHourSyntax(key, "Su 10:00"), isEmpty());
        assertThat(checkOpeningHourSyntax(key, "automatic"), not(isEmpty()));
        assertThat(checkOpeningHourSyntax(key, "Mo-Sa 09:00-18:00"), isEmpty());
        assertThat(checkOpeningHourSyntax(key, "Su 09:30; We 19:30"), isEmpty());
        // assertThat(checkOpeningHourSyntax(key, "Mo-Fr 00:00-00:30,04:00-00:30; Sa,Su,PH 00:00-24:00"), isEmpty());
        assertThat(checkOpeningHourSyntax(key, "Mo-Fr 0:00-0:30,4:00-00:30; Sa,Su,PH 0:00-24:00"), hasSize(1));
        assertFixEquals("Mo-Fr 00:00-00:30,04:00-00:30; Sa,Su,PH 00:00-24:00",
                checkOpeningHourSyntax(key, "Mo-Fr 0:00-0:30,4:00-00:30; Sa,Su,PH 0:00-24:00").get(0));
        assertFixEquals("Mo-Fr 00:00-00:30,04:00-00:30; Sa,Su,PH 00:00-24:00",
                checkOpeningHourSyntax(key, "Mo-Fr 0:00-0:30,4:00-00:30; Sa,Su,PH 0:00-24:00").get(0));
    }

    /**
     * Test #1 of collection_times syntax.
     */
    @Test
    void testCheckCollectionTimeSyntax1() {
        final String key = "collection_times";
        // frequently used tags according to https://taginfo.openstreetmap.org/keys/collection_times#values
        assertThat(checkOpeningHourSyntax(key, "Mo-Sa 09:00"), isEmpty());
        assertThat(checkOpeningHourSyntax(key, "fixme"), not(isEmpty()));
        assertThat(checkOpeningHourSyntax(key, "daily"), not(isEmpty()));
        assertThat(checkOpeningHourSyntax(key, "Mo-Fr 13:30,17:45,19:00; Sa 15:00; Su 11:00"), isEmpty());
        assertThat(checkOpeningHourSyntax(key, "Mo-Fr 13:30, 17:45, 19:00; Sa 15:00; Su 11:00"), hasSize(1));
        assertEquals(Severity.OTHER,
                checkOpeningHourSyntax(key, "Mo-Fr 13:30, 17:45, 19:00; Sa 15:00; Su 11:00").get(0).getSeverity());
        assertFixEquals("Mo-Fr 13:30,17:45,19:00; Sa 15:00; Su 11:00",
                checkOpeningHourSyntax(key, "Mo-Fr 13:30, 17:45, 19:00; Sa 15:00; Su 11:00").get(0));
    }

    /**
     * Tests that predefined values in presets are correct.
     */
    @Test
    void testPresetValues() {
        final Collection<TaggingPreset> presets = TaggingPresetReader.readFromPreferences(false, false);
        final Set<Tag> values = new LinkedHashSet<>();
        for (final TaggingPreset p : presets) {
            for (final TaggingPresetItem i : p.data) {
                if (i instanceof KeyedItem &&
                        Arrays.asList("opening_hours", "service_times", "collection_times").contains(((KeyedItem) i).key)) {
                    for (final String v : ((KeyedItem) i).getValues()) {
                        values.add(new Tag(((KeyedItem) i).key, v));
                    }
                }
            }
        }
        for (final Tag t : values) {
            final List<TestError> errors = checkOpeningHourSyntax(t.getKey(), t.getValue());
            if (!errors.isEmpty() && errors.get(0).getDescription().startsWith("Holiday after weekday")) {
                continue;
            }
            assertThat(t + " is valid", errors, isEmpty());
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17932">Bug #17932</a>.
     */
    @Test
    void testTicket17932() {
        Logging.clearLastErrorAndWarnings();
        assertTrue(checkOpeningHourSyntax("opening_hours", "SH off").isEmpty());
    }

    private List<TestError> checkOpeningHourSyntax(final String key, final String value, final Locale... locales) {
        final Locale locale = locales.length > 0 ? locales[0] : Locale.ENGLISH;
        final Node node = new Node(LatLon.ZERO);
        node.put(key, value);
        new DataSet(node);
        return openingHourTest.checkOpeningHourSyntax(key, value, node, locale);
    }

    private static void assertFixEquals(String value, TestError error) {
        assertNotNull(error.getFix(), "fix is not null");
        assertTrue(error.getFix() instanceof ChangePropertyCommand, "fix is ChangePropertyCommand");
        final ChangePropertyCommand command = (ChangePropertyCommand) error.getFix();
        assertEquals(1, command.getTags().size());
        assertEquals(value, command.getTags().values().iterator().next());
    }
}
