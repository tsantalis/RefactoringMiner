package org.cloudfoundry.multiapps.mta.resolvers;

import static org.cloudfoundry.multiapps.mta.resolvers.ReferencePattern.FULLY_QUALIFIED;
import static org.cloudfoundry.multiapps.mta.resolvers.ReferencePattern.PLACEHOLDER;
import static org.cloudfoundry.multiapps.mta.resolvers.ReferencePattern.SHORT;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class ReferencePatternTest {

    @RunWith(Parameterized.class)
    public static class ReferencePatternTest1 {

        private String line;
        private Reference[] expectedReferences;
        private ReferencePattern pattern;

        public ReferencePatternTest1(ReferencePattern pattern, String line, Reference[] refs) {
            this.pattern = pattern;
            this.line = line;
            this.expectedReferences = refs;

        }

        @Parameters
        public static Iterable<Object[]> getParameters() {
            Object[][] parameters = new Object[][] {
                { FULLY_QUALIFIED, "~{price_opt/protocol}://~{price_opt2/uri}/odata/:~{price_opt2/port}",
                    new Reference[] { new Reference("~{price_opt/protocol}", "protocol", "price_opt"),
                        new Reference("~{price_opt2/uri}", "uri", "price_opt2"),
                        new Reference("~{price_opt2/port}", "port", "price_opt2") } },
                { SHORT, "~{protocol}://~{uri}/odata/:~{port}",
                    new Reference[] { new Reference("~{protocol}", "protocol"), new Reference("~{uri}", "uri"),
                        new Reference("~{port}", "port") } },
                { PLACEHOLDER, "url: ${protocol}://${url}/odata/:${port}",
                    new Reference[] { new Reference("${protocol}", "protocol"), new Reference("${url}", "url"),
                        new Reference("${port}", "port") } },
                { FULLY_QUALIFIED, "url: ~{competitor_data/url}",
                    new Reference[] { new Reference("~{competitor_data/url}", "url", "competitor_data") } },
                { SHORT, "url: ~{url}", new Reference[] { new Reference("~{url}", "url") } },
                { PLACEHOLDER, "url: ${url}", new Reference[] { new Reference("${url}", "url") } } };

            return Arrays.asList(parameters);
        }

        @Test
        public void testMatch() {
            List<Reference> actualRefs = pattern.match(line);
            for (int i = 0; i < expectedReferences.length; i++) {
                Reference expected = expectedReferences[i];
                Reference actual = actualRefs.get(i);
                assertEquals(expected.getMatchedPattern(), actual.getMatchedPattern());
                assertEquals(expected.getKey(), actual.getKey());
                if (pattern.hasPropertySetSegment()) {
                    assertEquals(expected.getDependencyName(), actual.getDependencyName());
                }
            }
        }

    }

    @RunWith(Parameterized.class)
    public static class ReferencePatternTest2 {

        private ReferencePattern referencePattern;
        private Reference reference;
        private String expectedResult;

        public ReferencePatternTest2(ReferencePattern referencePattern, Reference reference, String expectedResult) {
            this.referencePattern = referencePattern;
            this.reference = reference;
            this.expectedResult = expectedResult;
        }

        @Parameters
        public static Iterable<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
// @formatter:off
                // (0)
                {
                    ReferencePattern.FULLY_QUALIFIED, new Reference(null, "name", "plugins"), "~{plugins/name}",
                },
                // (1)
                {
                    ReferencePattern.FULLY_QUALIFIED, new Reference(null, "{to}", "plugins"), "~{plugins/{to}}",
                },
                // (5)
                {
                    ReferencePattern.SHORT, new Reference(null, null, null), "~{null}",
                },
                // (6)
                {
                    ReferencePattern.PLACEHOLDER, new Reference(null, "name", null), "${name}",
                },
                // (7)
                {
                    ReferencePattern.PLACEHOLDER, new Reference(null, "{to}", null), "${{to}}",
                },
                // (3)
                {
                    ReferencePattern.SHORT, new Reference(null, "name", null), "~{name}",
                },
                // (4)
                {
                    ReferencePattern.SHORT, new Reference(null, "{to}", null), "~{{to}}",
                },
                // (8)
                {
                    ReferencePattern.PLACEHOLDER, new Reference(null, null, null), "${null}",
                },
                // (2)
                {
                    ReferencePattern.FULLY_QUALIFIED, new Reference(null, null, null),"~{null/null}",
                },
// @formatter:on
            });
        }

        @Test
        public void testToString() {
            assertEquals(expectedResult, referencePattern.toString(reference));
        }

    }

}
