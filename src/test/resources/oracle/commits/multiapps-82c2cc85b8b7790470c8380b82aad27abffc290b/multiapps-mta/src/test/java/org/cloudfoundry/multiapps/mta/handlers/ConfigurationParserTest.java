package org.cloudfoundry.multiapps.mta.handlers;

import java.io.InputStream;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ConfigurationParserTest {

    private final Tester tester = Tester.forClass(getClass());

    static Stream<Arguments> testParsePlatformJson() {
        return Stream.of(// (0) Valid JSON:
                         Arguments.of("/mta/sample/platform-01.json", new Expectation(Expectation.Type.JSON, "platform-01.json.json")),
                         // (1) Invalid JSON (invalid key 'properties' in resource types):
                         Arguments.of("/mta/sample/platform-02.json", new Expectation(Expectation.Type.JSON, "platform-02.json.json")),
                         // (3) Valid JSON (containing only a name):
                         Arguments.of("/mta/sample/platform-03.json", new Expectation(Expectation.Type.JSON, "platform-03.json.json")));
    }

    @ParameterizedTest
    @MethodSource
    void testParsePlatformJson(String platformLocation, Expectation expectation) throws Exception {
        InputStream platformJson = getClass().getResourceAsStream(platformLocation);
        tester.test(() -> new ConfigurationParser().parsePlatformJson(platformJson), expectation);
    }

}
