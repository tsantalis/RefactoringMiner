package org.cloudfoundry.multiapps.mta.handlers.v2;

import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DescriptorParserTest {

    protected final DescriptorParser parser = createDescriptorParser();
    protected final Tester tester = Tester.forClass(getClass());

    protected DescriptorParser createDescriptorParser() {
        return new DescriptorParser();
    }

    static Stream<Arguments> testParseDeploymentDescriptorYaml() {
        return Stream.of(
                         // Valid deployment descriptor:
                         Arguments.of("/mta/sample/v2/mtad-01.yaml", new Expectation(Expectation.Type.JSON, "mtad-01.yaml.json")),
                         // Multiple modules with the same name:
                         Arguments.of("mtad-01.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Value \"foo\" for key \"name\" not unique for object \"MTA module\"")),
                         // Multiple provided dependencies with the same name:
                         Arguments.of("mtad-02.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Value \"bar\" for key \"name\" not unique for object \"MTA provided dependency\"")),
                         // Multiple required dependencies with the same name:
                         Arguments.of("mtad-03.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Value \"bar\" for key \"name\" not unique for object \"MTA required dependency\"")),
                         // Multiple provided dependencies with the same name (in the same module):
                         Arguments.of("mtad-04.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Value \"foo\" for key \"name\" not unique for object \"MTA provided dependency\"")),
                         // Explicit declaration that module provides itself as a dependency:
                         Arguments.of("/mta/sample/v2/mtad-05.yaml", new Expectation(Expectation.Type.JSON, "mtad-01.yaml.json")),
                         // Module and resource with the same name:
                         Arguments.of("mtad-19.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Value \"foo\" for key \"name\" not unique for object \"MTA resource\"")),
                         // Resource and module provided dependency with the same name:
                         Arguments.of("mtad-20.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Value \"bar\" for key \"name\" not unique for object \"MTA resource\"")),
                         // Provided dependency name same as another module:
                         Arguments.of("mtad-21.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Value \"bar\" for key \"name\" not unique for object \"MTA provided dependency\"")),
                         // Partial schema version support test (int):
                         Arguments.of("mtad-with-partial-schema-version-major.yaml",
                                      new Expectation(Expectation.Type.JSON, "parsed-mtad-with-partial-schema-version.json")),
                         // Partial schema version support test (double):
                         Arguments.of("mtad-with-partial-schema-version-major.minor.yaml",
                                      new Expectation(Expectation.Type.JSON, "parsed-mtad-with-partial-schema-version.json")),
                         // Partial schema version support test (string):
                         Arguments.of("mtad-with-partial-schema-version-major-quoted.yaml",
                                      new Expectation(Expectation.Type.JSON, "parsed-mtad-with-partial-schema-version.json")),
                         // Partial schema version support test (string):
                         Arguments.of("mtad-with-partial-schema-version-major.minor-quoted.yaml",
                                      new Expectation(Expectation.Type.JSON, "parsed-mtad-with-partial-schema-version.json")));
    }

    static Stream<Arguments> testParseExtensionDescriptorYaml() {
        return Stream.of(
                         // Valid extension descriptor:
                         Arguments.of("/mta/sample/v2/config-01.mtaext", new Expectation(Expectation.Type.JSON, "config-01.mtaext.json")),
                         // Multiple modules with the same name:
                         Arguments.of("config-01.mtaext",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Value \"foo\" for key \"name\" not unique for object \"MTA extension module\"")),
                         // Multiple provided dependencies with the same name:
                         Arguments.of("config-02.mtaext",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Value \"bar\" for key \"name\" not unique for object \"MTA extension provided dependency\"")),
                         // Multiple required dependencies with the same name:
                         Arguments.of("config-03.mtaext",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Value \"baz\" for key \"name\" not unique for object \"MTA extension required dependency\"")),
                         // Multiple provided dependencies with the same name (in the same module):
                         Arguments.of("config-04.mtaext",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Value \"bar\" for key \"name\" not unique for object \"MTA extension provided dependency\"")),
                         // Partial schema version support test (int):
                         Arguments.of("config-with-partial-schema-version-major.mtaext",
                                      new Expectation(Expectation.Type.JSON, "parsed-config-with-partial-schema-version.json")),
                         // Partial schema version support test (double):
                         Arguments.of("config-with-partial-schema-version-major.minor.mtaext",
                                      new Expectation(Expectation.Type.JSON, "parsed-config-with-partial-schema-version.json")),
                         // Partial schema version support test (string):
                         Arguments.of("config-with-partial-schema-version-major-quoted.mtaext",
                                      new Expectation(Expectation.Type.JSON, "parsed-config-with-partial-schema-version.json")),
                         // Partial schema version support test (string):
                         Arguments.of("config-with-partial-schema-version-major.minor-quoted.mtaext",
                                      new Expectation(Expectation.Type.JSON, "parsed-config-with-partial-schema-version.json")));
    }

    @ParameterizedTest
    @MethodSource
    public void testParseDeploymentDescriptorYaml(String deploymentDescriptorLocation, Expectation expectation) {
        InputStream deploymentDescriptorYaml = getClass().getResourceAsStream(deploymentDescriptorLocation);
        tester.test(() -> {
            Map<String, Object> deploymentDescriptorMap = new YamlParser().convertYamlToMap(deploymentDescriptorYaml);
            return parser.parseDeploymentDescriptor(deploymentDescriptorMap);
        }, expectation);
    }

    @ParameterizedTest
    @MethodSource
    public void testParseExtensionDescriptorYaml(String extensionDescriptorLocation, Expectation expectation) {
        InputStream extensionDescriptorYaml = getClass().getResourceAsStream(extensionDescriptorLocation);
        tester.test(() -> {
            Map<String, Object> extensionDescriptorMap = new YamlParser().convertYamlToMap(extensionDescriptorYaml);
            return parser.parseExtensionDescriptor(extensionDescriptorMap);
        }, expectation);
    }

}
