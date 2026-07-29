package org.cloudfoundry.multiapps.mta.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PropertiesUtilTest {

    private final Tester tester = Tester.forClass(getClass());

    static Stream<Arguments> mergeTest() {
        return Stream.of(
// @formatter:off
            // (0) Merge normal properties maps
            Arguments.of("deployment-properties-01.json", "extension-properties-01.json", new Expectation(Expectation.Type.JSON, "merged-properties-01.json")),
            // (1) No changes in the extension => merged properties should be the same
            Arguments.of("deployment-properties-02.json", "extension-properties-02.json", new Expectation(Expectation.Type.JSON, "merged-properties-02.json")),
            // (2) Merging of nested maps
            Arguments.of("deployment-properties-03.json", "extension-properties-03.json", new Expectation(Expectation.Type.JSON, "merged-properties-03.json")),
            // (3) Scalar parameter cannot be overwritten by a structured parameter
            Arguments.of("deployment-properties-04.json", "extension-properties-04.json", new Expectation(Expectation.Type.EXCEPTION, "")),
            // (4) Structured parameter cannot be overwritten by a scalar parameter
            Arguments.of("deployment-properties-05.json", "extension-properties-05.json", new Expectation(Expectation.Type.EXCEPTION, ""))
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void mergeTest(String deploymentDescriptorPath, String extensionDescriptorPath, Expectation expectation) {
        Map<String, Object> deploymentDescriptorProperties = JsonUtil.convertJsonToMap(TestUtil.getResourceAsString(deploymentDescriptorPath,
                                                                                                                    getClass()));
        Map<String, Object> extensionDescriptorProperties = JsonUtil.convertJsonToMap(TestUtil.getResourceAsString(extensionDescriptorPath,
                                                                                                                   getClass()));

        tester.test(() -> PropertiesUtil.mergeExtensionProperties(deploymentDescriptorProperties, extensionDescriptorProperties),
                    expectation);
    }

    static Stream<Arguments> testGetPluralOrSingular() {
        return Stream.of(
// @formatter:off
            Arguments.of("host-1", null, new Expectation(Expectation.Type.STRING, Collections.singletonList("host-1").toString())),
            Arguments.of(null, Arrays.asList("host-1", "host-2"), new Expectation(Expectation.Type.STRING, Arrays.asList("host-1", "host-2").toString())),
            Arguments.of("host-1", Arrays.asList("host-2", "host-3"), new Expectation(Expectation.Type.STRING, Arrays.asList("host-2", "host-3").toString()))
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGetPluralOrSingular(String singular, List<String> plural, Expectation expectation) {
        String singularKey = "host";
        String pluralKey = "hosts";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(singularKey, singular);
        parameters.put(pluralKey, plural);
        List<Map<String, Object>> parametersList = Collections.singletonList(parameters);

        tester.test(() -> PropertiesUtil.getPluralOrSingular(parametersList, pluralKey, singularKey), expectation);

    }
}