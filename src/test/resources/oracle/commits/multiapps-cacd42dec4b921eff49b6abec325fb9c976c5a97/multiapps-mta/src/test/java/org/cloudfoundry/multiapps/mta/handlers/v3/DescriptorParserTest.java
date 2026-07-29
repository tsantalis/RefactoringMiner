package org.cloudfoundry.multiapps.mta.handlers.v3;

import java.util.Arrays;

import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DescriptorParserTest extends org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParserTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0)
            {
                "mtad-00.yaml", "config-00.mtaext",
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "parsed-mtad-00.json"),
                    new Expectation(Expectation.Type.JSON, "parsed-config-00.mtaext"),
                },
            },
            // (1)
            {
                "mtad-01.yaml", "config-01.mtaext",
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "parsed-mtad-01.json"),
                    new Expectation(Expectation.Type.JSON, "parsed-config-01.mtaext"),
                },
            },
            // (2) Sensitive properties and parameters test:
            {
                "mtad-02.yaml", null,
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "parsed-mtad-02.json"),
                    new Expectation(Expectation.Type.SKIP, null),
                },
            },
            // (3) Partial schema version support test (int):
            {
                "mtad-with-partial-schema-version-major.yaml", "config-with-partial-schema-version-major.mtaext",
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "parsed-mtad-with-partial-schema-version.json"),
                    new Expectation(Expectation.Type.JSON, "parsed-config-with-partial-schema-version.json"),
                },
            },
            // (4) Partial schema version support test (double):
            {
                "mtad-with-partial-schema-version-major.minor.yaml", "config-with-partial-schema-version-major.minor.mtaext",
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "parsed-mtad-with-partial-schema-version.json"),
                    new Expectation(Expectation.Type.JSON, "parsed-config-with-partial-schema-version.json"),
                },
            },
            // (5) Partial schema version support test (string):
            {
                "mtad-with-partial-schema-version-major-quoted.yaml", "config-with-partial-schema-version-major-quoted.mtaext",
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "parsed-mtad-with-partial-schema-version.json"),
                    new Expectation(Expectation.Type.JSON, "parsed-config-with-partial-schema-version.json"),
                },
            },
            // (6) Partial schema version support test (string):
            {
                "mtad-with-partial-schema-version-major.minor-quoted.yaml", "config-with-partial-schema-version-major.minor-quoted.mtaext",
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "parsed-mtad-with-partial-schema-version.json"),
                    new Expectation(Expectation.Type.JSON, "parsed-config-with-partial-schema-version.json"),
                },
            },
            // (7) Module hooks with correct data:
            {
                "mtad-with-module-hooks-with-correct-data.yaml", null,
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "parsed-mtad-with-hooks-correct-data.json"),
                    new Expectation(Expectation.Type.SKIP, null),
                },
            },
            // (8) Module hooks with more than one hook phase:
            {
                "mtad-with-module-hooks-with-more-hook-phases.yaml", null,
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "parsed-mtad-with-more-hook-phases.json"),
                    new Expectation(Expectation.Type.SKIP, null),
                },
            },
            // (9) Module hooks without type:
            {
                "mtad-with-module-hooks-without-type.yaml", null,
                new Expectation[] {
                    new Expectation(Expectation.Type.EXCEPTION, "Missing required key \"modules#0#hooks#0#type\""),
                    new Expectation(Expectation.Type.SKIP, null),
                },
            },
            // (10) Module hooks with two hooks:
            {
                "mtad-with-module-hooks-with-two-hooks.yaml", null,
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "parsed-mtad-with-two-hooks.json"),
                    new Expectation(Expectation.Type.SKIP, null),
                },
            },
// @formatter:on
        });
    }

    public DescriptorParserTest(String deploymentDescriptorLocation, String extensionDescriptorLocation, Expectation[] expectations) {
        super(deploymentDescriptorLocation, extensionDescriptorLocation, expectations);
    }

    @Override
    protected DescriptorParser createDescriptorParser() {
        return new DescriptorParser();
    }

}
