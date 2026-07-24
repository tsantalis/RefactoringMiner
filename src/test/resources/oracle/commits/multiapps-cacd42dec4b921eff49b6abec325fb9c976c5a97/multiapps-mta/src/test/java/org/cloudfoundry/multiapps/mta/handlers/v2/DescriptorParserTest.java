package org.cloudfoundry.multiapps.mta.handlers.v2;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DescriptorParserTest {

    protected final Tester tester = Tester.forClass(getClass());

    private final String extensionDescriptorsLocation;
    private final String deploymentDescriptorLocation;
    private final Expectation[] expectations;

    private InputStream extensionDescriptorsYaml;
    private InputStream deploymentDescriptorYaml;

    private DescriptorParser parser;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Valid deployment and extension descriptors:
            {
                "/mta/sample/v2/mtad-01.yaml", "/mta/sample/v2/config-01.mtaext",
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "mtad-01.yaml.json"),
                    new Expectation(Expectation.Type.JSON, "config-01.mtaext.json"),
                },
            },
            // (1) Multiple modules with the same name:
            {
                "mtad-01.yaml", "config-01.mtaext",
                new Expectation[] {
                    new Expectation(Expectation.Type.EXCEPTION, "Value \"foo\" for key \"name\" not unique for object \"MTA module\""),
                    new Expectation(Expectation.Type.EXCEPTION, "Value \"foo\" for key \"name\" not unique for object \"MTA extension module\""),
                },
            },
            // (2) Multiple provided dependencies with the same name:
            {
                "mtad-02.yaml", "config-02.mtaext",
                new Expectation[] {
                    new Expectation(Expectation.Type.EXCEPTION, "Value \"bar\" for key \"name\" not unique for object \"MTA provided dependency\""),
                    new Expectation(Expectation.Type.EXCEPTION, "Value \"bar\" for key \"name\" not unique for object \"MTA extension provided dependency\""),
                },
            },
            // (3) Multiple required dependencies with the same name:
            {
                "mtad-03.yaml", "config-03.mtaext",
                new Expectation[] {
                    new Expectation(Expectation.Type.EXCEPTION, "Value \"bar\" for key \"name\" not unique for object \"MTA required dependency\""),
                    new Expectation(Expectation.Type.EXCEPTION, "Value \"baz\" for key \"name\" not unique for object \"MTA extension required dependency\""),
                },
            },
            // (4) Multiple provided dependencies with the same name (in the same module):
            {
                "mtad-04.yaml", "config-04.mtaext",
                new Expectation[] {
                    new Expectation(Expectation.Type.EXCEPTION, "Value \"foo\" for key \"name\" not unique for object \"MTA provided dependency\""),
                    new Expectation(Expectation.Type.EXCEPTION, "Value \"bar\" for key \"name\" not unique for object \"MTA extension provided dependency\""),
                },
            },
            // (5) Explicit declaration that module provides itself as a dependency:
            {
                "/mta/sample/v2/mtad-05.yaml", null,
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "mtad-01.yaml.json"),
                    new Expectation(Expectation.Type.SKIP, null),
                },
            },
            // (6) Module and resource with the same name:
            {
                "mtad-19.yaml", null,
                new Expectation[] {
                    new Expectation(Expectation.Type.EXCEPTION, "Value \"foo\" for key \"name\" not unique for object \"MTA resource\""),
                    new Expectation(Expectation.Type.SKIP, null),
                },
            },
            // (7) Resource and module provided dependency with the same name:
            {
                "mtad-20.yaml", null,
                new Expectation[] {
                    new Expectation(Expectation.Type.EXCEPTION, "Value \"bar\" for key \"name\" not unique for object \"MTA resource\""),
                    new Expectation(Expectation.Type.SKIP, null),
                },
            },
            // (8) Provided dependency name same as another module:
            {
                "mtad-21.yaml", null,
                new Expectation[] {
                    new Expectation(Expectation.Type.EXCEPTION, "Value \"bar\" for key \"name\" not unique for object \"MTA provided dependency\""),
                    new Expectation(Expectation.Type.SKIP, null),
                },
            },
            // (9) Partial schema version support test (int):
            {
                "mtad-with-partial-schema-version-major.yaml", "config-with-partial-schema-version-major.mtaext",
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "parsed-mtad-with-partial-schema-version.json"),
                    new Expectation(Expectation.Type.JSON, "parsed-config-with-partial-schema-version.json"),
                },
            },
            // (10) Partial schema version support test (double):
            {
                "mtad-with-partial-schema-version-major.minor.yaml", "config-with-partial-schema-version-major.minor.mtaext",
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "parsed-mtad-with-partial-schema-version.json"),
                    new Expectation(Expectation.Type.JSON, "parsed-config-with-partial-schema-version.json"),
                },
            },
            // (11) Partial schema version support test (string):
            {
                "mtad-with-partial-schema-version-major-quoted.yaml", "config-with-partial-schema-version-major-quoted.mtaext",
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "parsed-mtad-with-partial-schema-version.json"),
                    new Expectation(Expectation.Type.JSON, "parsed-config-with-partial-schema-version.json"),
                },
            },
            // (12) Partial schema version support test (string):
            {
                "mtad-with-partial-schema-version-major.minor-quoted.yaml", "config-with-partial-schema-version-major.minor-quoted.mtaext",
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "parsed-mtad-with-partial-schema-version.json"),
                    new Expectation(Expectation.Type.JSON, "parsed-config-with-partial-schema-version.json"),
                },
            },
// @formatter:on
        });
    }

    public DescriptorParserTest(String deploymentDescriptorLocation, String extensionDescriptorLocation, Expectation[] expectation) {
        this.extensionDescriptorsLocation = extensionDescriptorLocation;
        this.deploymentDescriptorLocation = deploymentDescriptorLocation;
        this.expectations = expectation;
    }

    @Before
    public void setUp() {
        if (extensionDescriptorsLocation != null) {
            extensionDescriptorsYaml = getClass().getResourceAsStream(extensionDescriptorsLocation);
        }
        if (deploymentDescriptorLocation != null) {
            deploymentDescriptorYaml = getClass().getResourceAsStream(deploymentDescriptorLocation);
        }
        parser = createDescriptorParser();
    }

    protected DescriptorParser createDescriptorParser() {
        return new DescriptorParser();
    }

    @Test
    public void testParseDeploymentDescriptorYaml() {
        tester.test(() -> {
            Map<String, Object> deploymentDescriptorMap = new YamlParser().convertYamlToMap(deploymentDescriptorYaml);
            return parser.parseDeploymentDescriptor(deploymentDescriptorMap);
        }, expectations[0]);
    }

    @Test
    public void testParseExtensionDescriptorYaml() {
        tester.test(() -> {
            Map<String, Object> extensionDescriptorMap = new YamlParser().convertYamlToMap(extensionDescriptorsYaml);
            return parser.parseExtensionDescriptor(extensionDescriptorMap);
        }, expectations[1]);
    }

}
