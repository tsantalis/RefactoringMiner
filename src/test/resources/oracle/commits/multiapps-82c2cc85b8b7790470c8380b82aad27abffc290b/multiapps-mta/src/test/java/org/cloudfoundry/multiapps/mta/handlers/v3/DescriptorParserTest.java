package org.cloudfoundry.multiapps.mta.handlers.v3;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.junit.jupiter.params.provider.Arguments;

public class DescriptorParserTest extends org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParserTest {

    @Override
    protected DescriptorParser createDescriptorParser() {
        return new DescriptorParser();
    }

    static Stream<Arguments> testParseDeploymentDescriptorYaml() {
        return Stream.of(Arguments.of("mtad-00.yaml", new Expectation(Expectation.Type.JSON, "parsed-mtad-00.json")),
                         Arguments.of("mtad-01.yaml", new Expectation(Expectation.Type.JSON, "parsed-mtad-01.json")),
                         // Sensitive properties and parameters test:
                         Arguments.of("mtad-02.yaml", new Expectation(Expectation.Type.JSON, "parsed-mtad-02.json")),
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
                                      new Expectation(Expectation.Type.JSON, "parsed-mtad-with-partial-schema-version.json")),
                         // Module hooks with correct data:
                         Arguments.of("mtad-with-module-hooks-with-correct-data.yaml",
                                      new Expectation(Expectation.Type.JSON, "parsed-mtad-with-hooks-correct-data.json")),
                         // Module hooks with more than one hook phase:
                         Arguments.of("mtad-with-module-hooks-with-more-hook-phases.yaml",
                                      new Expectation(Expectation.Type.JSON, "parsed-mtad-with-more-hook-phases.json")),
                         // Module hooks without type:
                         Arguments.of("mtad-with-module-hooks-without-type.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION, "Missing required key \"modules#0#hooks#0#type\"")),
                         // Module hooks with two hooks:
                         Arguments.of("mtad-with-module-hooks-with-two-hooks.yaml",
                                      new Expectation(Expectation.Type.JSON, "parsed-mtad-with-two-hooks.json")));
    }

    static Stream<Arguments> testParseExtensionDescriptorYaml() {
        return Stream.of(
                         // Valid extension descriptor:
                         Arguments.of("config-00.mtaext", new Expectation(Expectation.Type.JSON, "parsed-config-00.mtaext")),
                         // Multiple modules with the same name:
                         Arguments.of("config-01.mtaext", new Expectation(Expectation.Type.JSON, "parsed-config-01.mtaext")),
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

}
