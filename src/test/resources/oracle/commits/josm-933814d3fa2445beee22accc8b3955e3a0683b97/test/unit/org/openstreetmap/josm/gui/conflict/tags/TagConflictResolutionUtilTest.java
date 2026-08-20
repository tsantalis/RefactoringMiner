// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.gui.conflict.tags.TagConflictResolutionUtil.AutomaticChoice;
import org.openstreetmap.josm.gui.conflict.tags.TagConflictResolutionUtil.AutomaticChoiceGroup;
import org.openstreetmap.josm.gui.conflict.tags.TagConflictResolutionUtil.AutomaticCombine;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link TagConflictResolutionUtil} class.
 */
class TagConflictResolutionUtilTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static HashSet<String> newHashSet(String... values) {
        return Arrays.stream(values).collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Unit test of {@link TagConflictResolutionUtil#applyAutomaticTagConflictResolution}.
     * assume predefined rules for US TIGER and French Cadastre.
     */
    @Test
    void testApplyAutomaticTagConflictResolution() {
        // Check that general tag conflict are not resolved
        TagCollection tc = new TagCollection();
        tc.add(new Tag("building", "school"));
        tc.add(new Tag("building", "garage"));
        TagConflictResolutionUtil.applyAutomaticTagConflictResolution(tc);
        assertEquals(newHashSet("school", "garage"), new HashSet<>(tc.getValues("building")));

        // Check US Tiger tag conflict resolution
        tc = new TagCollection();
        tc.add(new Tag("tiger:test", "A:B"));
        tc.add(new Tag("tiger:test", "A"));
        TagConflictResolutionUtil.applyAutomaticTagConflictResolution(tc);
        assertEquals(newHashSet("A:B"), new HashSet<>(tc.getValues("tiger:test")));

        // Check FR:cadastre source tag conflict resolution (most common values from taginfo except last one without accentuated characters)
        tc = new TagCollection();
        // CHECKSTYLE.OFF: LineLength
        tc.add(new Tag("source", "cadastre-dgi-fr source : Direction Générale des Impôts - Cadastre ; mise à jour : 2007"));
        tc.add(new Tag("source", "cadastre-dgi-fr source : Direction Générale des Impôts - Cadastre ; mise à jour : 2008"));
        tc.add(new Tag("source", "cadastre-dgi-fr source : Direction Générale des Impôts - Cadastre ; mise à jour : 2009"));
        tc.add(new Tag("source", "cadastre-dgi-fr source : Direction Générale des Impôts - Cadastre ; mise à jour : 2010"));
        tc.add(new Tag("source", "cadastre-dgi-fr source : Direction Générale des Impôts - Cadastre. Mise à jour : 2008"));
        tc.add(new Tag("source", "cadastre-dgi-fr source : Direction Générale des Impôts - Cadastre. Mise à jour : 2009"));
        tc.add(new Tag("source", "cadastre-dgi-fr source : Direction Générale des Impôts - Cadastre. Mise à jour : 2010"));
        tc.add(new Tag("source", "cadastre-dgi-fr source : Direction Générale des Impôts - Cadastre. Mise à jour : 2011"));
        tc.add(new Tag("source", "cadastre-dgi-fr source : Direction Générale des Impôts - Cadastre. Mise à jour : 2012"));
        tc.add(new Tag("source", "cadastre-dgi-fr source : Direction Générale des Impôts - Cadastre. Mise à jour : 2013"));
        tc.add(new Tag("source", "cadastre-dgi-fr source : Direction Générale des Impôts - Cadastre. Mise à jour : 2014"));
        tc.add(new Tag("source", "cadastre-dgi-fr source : Direction Générale des Impôts - Cadas. Mise à jour : 2010"));
        tc.add(new Tag("source", "extraction vectorielle v1 cadastre-dgi-fr source : Direction Générale des Impôts - Cadas. Mise à jour : 2010"));
        tc.add(new Tag("source", "Direction Générale des Finances Publiques - Cadastre ; mise à jour : 2010"));
        tc.add(new Tag("source", "cadastre-dgi-fr source : Direction Générale des Finances Publiques - Cadastre. Mise à jour : 2013"));
        tc.add(new Tag("source", "cadastre-dgi-fr source : Direction Générale des Finances Publiques - Cadastre. Mise à jour : 2014"));
        tc.add(new Tag("source", "cadastre-dgi-fr source : Direction Generale des Finances Publiques - Cadastre. Mise a jour : 2015"));
        // CHECKSTYLE.ON: LineLength
        Tag otherSource = new Tag("source", "other");
        tc.add(otherSource); // other source should prevent resolution
        TagConflictResolutionUtil.applyAutomaticTagConflictResolution(tc);
        assertEquals(18, tc.getValues("source").size());
        tc.remove(otherSource);
        TagConflictResolutionUtil.applyAutomaticTagConflictResolution(tc);
        assertEquals(newHashSet("cadastre-dgi-fr source : Direction Generale des Finances Publiques - Cadastre. Mise a jour : 2015"),
                new HashSet<>(tc.getValues("source")));

        // Check CA:canvec source tag conflict resolution
        tc = new TagCollection();
        tc.add(new Tag("source", "CanVec_Import_2009"));
        tc.add(new Tag("source", "CanVec 4.0 - NRCan"));
        tc.add(new Tag("source", "CanVec 6.0 - NRCan"));
        tc.add(new Tag("source", "NRCan-CanVec-7.0"));
        tc.add(new Tag("source", "NRCan-CanVec-8.0"));
        tc.add(new Tag("source", "NRCan-CanVec-10.0"));
        tc.add(new Tag("source", "NRCan-CanVec-12.0"));
        TagConflictResolutionUtil.applyAutomaticTagConflictResolution(tc);
        assertEquals(newHashSet("NRCan-CanVec-12.0"), new HashSet<>(tc.getValues("source")));
    }

    /**
     * Unit tests of {@link AutomaticCombine} class.
     */
    static class AutomaticCombineTest {

        /**
         * Return AutomaticCombine instantiated with the two possible constructors.
         * @param ac a model for the constructed object.
         * @return AutomaticCombine object constructed with the two different constructors.
         */
        private static List<AutomaticCombine> differentlyConstructed(AutomaticCombine ac) {
            AutomaticCombine fullyConstructed = new AutomaticCombine(ac.key, ac.description, ac.isRegex, ac.separator, ac.sort);
            AutomaticCombine defaultConstructed = new AutomaticCombine();
            defaultConstructed.key = ac.key;
            defaultConstructed.description = ac.description;
            defaultConstructed.isRegex = ac.isRegex;
            defaultConstructed.separator = ac.separator;
            defaultConstructed.sort = ac.sort;
            return Arrays.asList(defaultConstructed, fullyConstructed);
        }

        /**
         * Setup test.
         */
        @RegisterExtension
        @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
        public JOSMTestRules test = new JOSMTestRules();

        /**
         * Unit test of {@link AutomaticCombine#matchesKey} with empty key.
         */
        @Test
        void testMatchesKeyEmptyKey() {
            for (AutomaticCombine resolver: differentlyConstructed(new AutomaticCombine("", "random description", true, ";", null))) {
                assertFalse(resolver.matchesKey("a"));
                assertTrue(resolver.matchesKey(""));
            }
        }

        /**
         * Unit test of {@link AutomaticCombine#matchesKey} when regex not used.
         */
        @Test
        void testMatchesKeyNotRegex() {
            for (AutomaticCombine resolver: differentlyConstructed(new AutomaticCombine(
                    "keyname", "random description", false, "|", null))) {
                assertFalse(resolver.matchesKey("key"));
                assertFalse(resolver.matchesKey("keyname2"));
                assertFalse(resolver.matchesKey("name"));
                assertFalse(resolver.matchesKey("key.*("));
                assertTrue(resolver.matchesKey("keyname"));
            }
        }

        /**
         * Unit test of {@link AutomaticCombine#matchesKey} when regex used.
         */
        @Test
        void testMatchesKeyRegex() {
            for (AutomaticCombine resolver: differentlyConstructed(new AutomaticCombine("test[45].*", "", true, ";", "Integer"))) {
                assertFalse(resolver.matchesKey("key"));
                assertFalse(resolver.matchesKey("test[45].*"));
                assertTrue(resolver.matchesKey("test400 !"));
            }
        }

        /**
         * Unit test of {@link AutomaticCombine} with invalid regex.
         */
        @Test
        void testInvalidRegex() {
            for (AutomaticCombine resolver: differentlyConstructed(new AutomaticCombine("invalidregex.(]", "", false, ";", null))) {
                // Should not raise exception if the resolver.isRexEx == false:
                assertTrue(resolver.matchesKey("invalidregex.(]"));
            }

            // Should not raise exception if isRexEx, invalid regex but only constructed:
            for (AutomaticCombine resolver: differentlyConstructed(new AutomaticCombine("invalidregex.(]", "", true, ";", null))) {
                assertTrue(resolver.isRegex);
            }
        }

        /**
         * Unit test of {@link AutomaticCombine} with invalid regex.
         */
        @Test
        void testInvalidRegexExceptionDefaultConstructed() {
            AutomaticCombine resolver = new AutomaticCombine("AB.(]", "", true, ";", null);
            assertThrows(PatternSyntaxException.class, () -> resolver.matchesKey("AB"));
        }

        /**
         * Unit test of {@link AutomaticCombine} with invalid regex.
         */
        @Test
        void testInvalidRegexExceptionFullyConstructed() {
            AutomaticCombine resolver = new AutomaticCombine();
            resolver.key = "AB.(]";
            resolver.isRegex = true;
            assertThrows(PatternSyntaxException.class, () -> resolver.matchesKey("AB"));
        }

        /**
         * Unit test of {@link AutomaticCombine#resolve}.
         */
        @Test
        void testResolve() {
            for (AutomaticCombine resolver: differentlyConstructed(new AutomaticCombine("random", "", true, "|", "String"))) {
                assertEquals("value1|value2", resolver.resolve(newHashSet("value1", "value2")));
                assertEquals("05|1|2|3|3;1|4|6", resolver.resolve(newHashSet("3|1", "4|2|1", "6|05", "3;1")));
            }

            for (AutomaticCombine resolver: differentlyConstructed(new AutomaticCombine("test[45].*", "", true, ";", "Integer"))) {
                assertEquals("3;24;25;95;1254545", resolver.resolve(newHashSet("1254545;95;24", "25;24;3")));
            }

            for (AutomaticCombine resolver: differentlyConstructed(new AutomaticCombine("AB", "", true, ";", null))) {
                String resolution = resolver.resolve(newHashSet("3;x;1", "4;x"));
                assertTrue(resolution.equals("3;x;1;4") || resolution.equals("4;x;3;1"));
            }

            for (AutomaticCombine resolver: differentlyConstructed(new AutomaticCombine("foo", "", true, ";", "Integer"))) {
                assertEquals("bar;1;2;3", resolver.resolve(newHashSet("1;2;3", "bar")));
            }
        }
    }

    /**
     * Unit tests of {@link AutomaticChoice} class.
     */
    static class AutomaticChoiceTest {
        /**
         * Setup test.
         */
        @RegisterExtension
        @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
        public JOSMTestRules test = new JOSMTestRules();

        /**

         * Return AutomaticCombine instantiated with the two possible constructors.
         * @param ac a model for the constructed object.
         * @return AutomaticCombine object constructed with the two different constructors.
         */
        private static List<AutomaticChoice> differentlyConstructed(AutomaticChoice ac) {
            AutomaticChoice fullyConstructed = new AutomaticChoice(ac.key, ac.group, ac.description, ac.isRegex, ac.value, ac.score);
            AutomaticChoice defaultConstructed = new AutomaticChoice();
            defaultConstructed.key = ac.key;
            defaultConstructed.group = ac.group;
            defaultConstructed.description = ac.description;
            defaultConstructed.isRegex = ac.isRegex;
            defaultConstructed.value = ac.value;
            defaultConstructed.score = ac.score;
            return Arrays.asList(defaultConstructed, fullyConstructed);
        }

        /**
         * Unit test of {@link AutomaticChoice#matchesValue}.
         */
        @Test
        void testMatchesValue() {
            for (AutomaticChoice resolver: differentlyConstructed(new AutomaticChoice(
                    "random key", "random group", "random description", false, ".*valueToMatch", "Score$0\\1"))) {
                assertTrue(resolver.matchesValue(".*valueToMatch"));
                assertFalse(resolver.matchesValue(".*valueToMatch.*"));
                assertFalse(resolver.matchesValue("test"));
                assertFalse(resolver.matchesValue(""));
            }
            for (AutomaticChoice resolver: differentlyConstructed(new AutomaticChoice(
                    "", "", "", true, "test([ab].*)", "ok $1"))) {
                assertTrue(resolver.matchesValue("testa"));
                assertTrue(resolver.matchesValue("testb129"));
                assertFalse(resolver.matchesValue("test[ab].*"));
                assertFalse(resolver.matchesValue("test"));
                assertFalse(resolver.matchesValue(""));
            }
        }

        /**
         * Unit test of {@link AutomaticChoice#computeScoreFromValue}.
         */
        @Test
        void testComputeScoreFromValue() {
            for (AutomaticChoice resolver: differentlyConstructed(new AutomaticChoice(
                    "random key", "random group", "random description", false, ".*valueToMatch", "Score$0\\1"))) {
                assertEquals(resolver.computeScoreFromValue(".*valueToMatch"), "Score$0\\1");
            }
            for (AutomaticChoice resolver: differentlyConstructed(new AutomaticChoice(
                    "", "", "", true, "test([ab].*)", "ok $1"))) {
                assertEquals(resolver.computeScoreFromValue("testa"), "ok a");
                assertEquals(resolver.computeScoreFromValue("testb129"), "ok b129");
            }
        }

        /**
         * Unit test of {@link AutomaticChoice} when invalid regex is used.
         */
        @Test
        void testInvalidRegex() {
            for (AutomaticChoice resolver: differentlyConstructed(new AutomaticChoice(
                    "k", "g", "", false, "invalidregex.(]", "InvalidScore$0\\1$-4"))) {
                // Should not raise exception if the resolver.isRexEx == false:
                assertTrue(resolver.matchesValue("invalidregex.(]"));
                assertFalse(resolver.matchesValue("test"));
                assertEquals(resolver.computeScoreFromValue("invalidregex.(]"), "InvalidScore$0\\1$-4");
            }
            // Should not raise exception if isRexEx, invalid regex but only constructed:
            for (AutomaticChoice resolver: differentlyConstructed(new AutomaticChoice(
                    "k", "g", "", true, "invalidregex.(]", "InvalidScore$0\\1$-4"))) {
                assertTrue(resolver.isRegex);
            }
        }

        /**
         * Unit test of {@link AutomaticChoice} when invalid regex is used.
         */
        @Test
        void testMatchesValueInvalidRegex() {
            AutomaticChoice resolver = new AutomaticChoice("k", "g", "", true, "invalidregex.(]", "InvalidScore$0\\1$-4");
            assertThrows(PatternSyntaxException.class, () -> resolver.matchesValue("test"));
        }

        /**
         * Unit test of {@link AutomaticChoice} when invalid regex is used.
         */
        @Test
        void testComputeScoreFromValueInvalidRegex() {
            AutomaticChoice resolver = new AutomaticChoice("k", "g", "", true, "invalidregex.(]", "valid");
            assertThrows(PatternSyntaxException.class, () -> resolver.computeScoreFromValue("valid"));
        }

        /**
         * Unit test of {@link AutomaticChoice} when invalid score replacement is used.
         */
        @Test
        void testComputeScoreFromValueInvalidReplacement() {
            AutomaticChoice resolver = new AutomaticChoice("k", "g", "", true, "valid", "InvalidScore$0\\1$-4");
            boolean exceptionThrown = false;
            try {
                resolver.computeScoreFromValue("valid");
            } catch (Exception e) {
                exceptionThrown = true;
            }
            assertTrue(exceptionThrown);
        }
    }

    /**
     * Unit tests of {@link AutomaticChoiceGroup} class.
     */
    static class AutomaticChoiceGroupTest {
        /**
         * Setup test.
         */
        @RegisterExtension
        @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
        public JOSMTestRules test = new JOSMTestRules();

        AutomaticChoice choiceKey1Group1 = new AutomaticChoice("Key1", "Group1", "", false, "value1", "score1");
        AutomaticChoice choiceKey1Group1bis = new AutomaticChoice("Key1", "Group1", "", false, "value2", "score2");
        AutomaticChoice choiceKey1Group2 = new AutomaticChoice("Key1", "Group2", "", false, "value1", "score1");
        AutomaticChoice choiceKey1Group2bis = new AutomaticChoice("Key1", "Group2", "", false, "value2", "score2");
        AutomaticChoice choiceKey2Group1 = new AutomaticChoice("test[45].*", "Group1", "", true, "value1", "score1");
        AutomaticChoice choiceKey2Group1bis = new AutomaticChoice("test[45].*", "Group1", "", true, "value2", "score2");
        AutomaticChoice choiceKey2Group2 = new AutomaticChoice("test[45].*", "Group2", "", true, "value1(.*)", "$1");
        AutomaticChoice choiceKey2Group2bis = new AutomaticChoice("test[45].*", "Group2", "", true, "value2(.*)", "$1");
        AutomaticChoice choiceEmpty = new AutomaticChoice();

        /**
         * Unit test of {@link AutomaticChoiceGroup#groupChoices}.
         */
        @Test
        void testGroupChoices() {
            Collection<AutomaticChoiceGroup> groups = AutomaticChoiceGroup.groupChoices(Arrays.asList(choiceKey1Group1, choiceKey1Group2));
            assertEquals(2, groups.size());

            groups = AutomaticChoiceGroup.groupChoices(Arrays.asList(
                choiceKey1Group1, choiceKey1Group2, choiceKey2Group1, choiceKey2Group2, choiceEmpty));
            assertEquals(5, groups.size());

            groups = AutomaticChoiceGroup.groupChoices(Arrays.asList(choiceKey1Group1, choiceKey1Group1bis));
            assertEquals(1, groups.size());
            AutomaticChoiceGroup group1 = groups.iterator().next();
            assertEquals(group1.key, choiceKey1Group1.key);
            assertEquals(group1.group, choiceKey1Group1.group);
            assertEquals(new HashSet<>(group1.choices), new HashSet<>(Arrays.asList(choiceKey1Group1, choiceKey1Group1bis)));

            groups = AutomaticChoiceGroup.groupChoices(Arrays.asList(
                choiceKey1Group1, choiceKey1Group1bis, choiceKey1Group2, choiceKey1Group2bis,
                choiceKey2Group1, choiceKey2Group1bis, choiceKey2Group2, choiceKey2Group2bis));
            assertEquals(4, groups.size());
            for (AutomaticChoiceGroup group: groups) {
                for (AutomaticChoice choice: group.choices) {
                    assertEquals(choice.key, group.key);
                    assertEquals(choice.group, group.group);
                    assertEquals(choice.isRegex, group.isRegex);
                }
            }
        }

        /**
         * Unit test of {@link AutomaticChoiceGroup#matchesKey}.
         */
        @Test
        void testMatchesKey() {
            AutomaticChoiceGroup group = new AutomaticChoiceGroup(
                    choiceKey1Group1.key, choiceKey1Group1.group, choiceKey1Group1.isRegex,
                    Arrays.asList(choiceKey1Group1, choiceKey1Group1bis));
            assertFalse(group.matchesKey("key"));
            assertFalse(group.matchesKey("keyname2"));
            assertFalse(group.matchesKey("name"));
            assertFalse(group.matchesKey("key.*("));
            assertTrue(group.matchesKey(choiceKey1Group1.key));

            group = new AutomaticChoiceGroup(
                    choiceKey2Group1.key, choiceKey2Group2.group, choiceKey2Group2.isRegex,
                    Arrays.asList(choiceKey2Group2, choiceKey2Group2bis));
            assertFalse(group.matchesKey("key"));
            assertFalse(group.matchesKey("test[45].*"));
            assertTrue(group.matchesKey("test400 !"));
        }

        /**
         * Unit test of {@link AutomaticChoiceGroup#resolve}.
         */
        @Test
        void testResolve() {
            AutomaticChoiceGroup group = new AutomaticChoiceGroup(
                    choiceKey1Group1.key, choiceKey1Group1.group, choiceKey1Group1.isRegex,
                    Arrays.asList(choiceKey1Group1, choiceKey1Group1bis));
            assertEquals(group.resolve(newHashSet(choiceKey1Group1.value)), choiceKey1Group1.value);
            assertEquals(group.resolve(newHashSet(choiceKey1Group1.value, choiceKey1Group1bis.value)), choiceKey1Group1bis.value);
            assertNull(group.resolve(newHashSet("random", choiceKey1Group1.value, choiceKey1Group1bis.value)));

            group = new AutomaticChoiceGroup(
                    choiceKey2Group1.key, choiceKey2Group2.group, choiceKey2Group2.isRegex,
                    Arrays.asList(choiceKey2Group2, choiceKey2Group2bis));
            assertEquals(group.resolve(newHashSet("value1")), "value1");
            assertEquals(group.resolve(newHashSet("value1Z", "value2A")), "value1Z");
            assertEquals(group.resolve(newHashSet("value1A", "value2Z")), "value2Z");
            assertNull(group.resolve(newHashSet("value1Z", "value2A", "other not matched value")));
        }
    }
}
