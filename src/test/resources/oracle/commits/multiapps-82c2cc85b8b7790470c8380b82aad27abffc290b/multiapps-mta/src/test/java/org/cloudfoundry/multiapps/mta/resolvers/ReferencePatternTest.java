package org.cloudfoundry.multiapps.mta.resolvers;

import static org.cloudfoundry.multiapps.mta.resolvers.ReferencePattern.FULLY_QUALIFIED;
import static org.cloudfoundry.multiapps.mta.resolvers.ReferencePattern.PLACEHOLDER;
import static org.cloudfoundry.multiapps.mta.resolvers.ReferencePattern.SHORT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ReferencePatternTest {

    static Stream<Arguments> testMatch() {
        return Stream.of(Arguments.of(FULLY_QUALIFIED, "~{price_opt/protocol}://~{price_opt2/uri}/odata/:~{price_opt2/port}",
                                      new Reference[] { new Reference("~{price_opt/protocol}", "protocol", "price_opt"),
                                          new Reference("~{price_opt2/uri}", "uri", "price_opt2"),
                                          new Reference("~{price_opt2/port}", "port", "price_opt2") }),
                         Arguments.of(SHORT, "~{protocol}://~{uri}/odata/:~{port}",
                                      new Reference[] { new Reference("~{protocol}", "protocol"), new Reference("~{uri}", "uri"),
                                          new Reference("~{port}", "port") }),
                         Arguments.of(PLACEHOLDER, "url: ${protocol}://${url}/odata/:${port}",
                                      new Reference[] { new Reference("${protocol}", "protocol"), new Reference("${url}", "url"),
                                          new Reference("${port}", "port") }),
                         Arguments.of(FULLY_QUALIFIED, "url: ~{competitor_data/url}",
                                      new Reference[] { new Reference("~{competitor_data/url}", "url", "competitor_data") }),
                         Arguments.of(SHORT, "url: ~{url}", new Reference[] { new Reference("~{url}", "url") }),
                         Arguments.of(PLACEHOLDER, "url: ${url}", new Reference[] { new Reference("${url}", "url") }));
    }

    static Stream<Arguments> testToString() {
        return Stream.of(Arguments.of(ReferencePattern.FULLY_QUALIFIED, new Reference(null, "name", "plugins"), "~{plugins/name}"),
                         Arguments.of(ReferencePattern.FULLY_QUALIFIED, new Reference(null, "{to}", "plugins"), "~{plugins/{to}}"),
                         Arguments.of(ReferencePattern.SHORT, new Reference(null, null, null), "~{null}"),
                         Arguments.of(ReferencePattern.PLACEHOLDER, new Reference(null, "name", null), "${name}"),
                         Arguments.of(ReferencePattern.PLACEHOLDER, new Reference(null, "{to}", null), "${{to}}"),
                         Arguments.of(ReferencePattern.SHORT, new Reference(null, "name", null), "~{name}"),
                         Arguments.of(ReferencePattern.SHORT, new Reference(null, "{to}", null), "~{{to}}"),
                         Arguments.of(ReferencePattern.PLACEHOLDER, new Reference(null, null, null), "${null}"),
                         Arguments.of(ReferencePattern.FULLY_QUALIFIED, new Reference(null, null, null), "~{null/null}"));
    }

    @ParameterizedTest
    @MethodSource
    void testMatch(ReferencePattern pattern, String line, Reference[] expectedReferences) {
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

    @ParameterizedTest
    @MethodSource
    void testToString(ReferencePattern referencePattern, Reference reference, String expectedResult) {
        assertEquals(expectedResult, referencePattern.toString(reference));
    }

}
