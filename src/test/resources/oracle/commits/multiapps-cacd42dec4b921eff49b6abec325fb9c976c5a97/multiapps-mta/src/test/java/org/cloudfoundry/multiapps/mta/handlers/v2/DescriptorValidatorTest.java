package org.cloudfoundry.multiapps.mta.handlers.v2;

import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.MtaTestUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DescriptorValidatorTest {

    protected static String platformLocation = "/mta/sample/platform-01.json";

    protected final Tester tester = Tester.forClass(getClass());

    private final String deploymentDescriptorLocation;
    private final String[] extensionDescriptorLocations;
    private final String mergedDescriptorLocation;
    private final Expectation[] expectations;

    private DeploymentDescriptor deploymentDescriptor;
    private List<ExtensionDescriptor> extensionDescriptors;
    private DeploymentDescriptor mergedDescriptor;
    private Platform platform;

    private DescriptorValidator validator;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
                // (00) Valid deployment and extension descriptors:
                {
                    "/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-01.mtaext" }, null,
                    new Expectation[] {
                        new Expectation(null),
                        new Expectation(null),
                        new Expectation(Expectation.Type.SKIP, null),
                    },
                },
                // (01) Deploy target not listed in merged descriptor (there aren't any other platforms listed):
                {
                    null, null, "/mta/sample/v2/merged-01.yaml",
                    new Expectation[] {
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(null),
                    },
                },
                // (02) Unresolved properties in merged descriptor:
                {
                    null, null, "/mta/sample/v2/merged-03.yaml",
                    new Expectation[] {
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(Expectation.Type.EXCEPTION, "Unresolved mandatory properties: [competitor-data#test, pricing#internal-odata#test, pricing-db#pricing-db-service#test, pricing-db#test]"),
                    },
                },
                // (03) Unknown module in extension descriptor:
                {
                    "/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-02.mtaext" }, null,
                    new Expectation[] {
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(Expectation.Type.EXCEPTION, "Unknown module \"web-serverx\" in extension descriptor \"com.sap.mta.sample.v2.config-02\""),
                        new Expectation(Expectation.Type.SKIP, null),
                    },
                },
                // (04) Unknown provided dependency in extension descriptor:
                {
                    "/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-03.mtaext" }, null,
                    new Expectation[] {
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(Expectation.Type.EXCEPTION, "Unknown provided dependency \"internal-odatax\" for module \"pricing\" in extension descriptor \"com.sap.mta.sample.v2.config-03\""),
                        new Expectation(Expectation.Type.SKIP, null),
                    },
                },
                // (05) Unknown resource in extension descriptor:
                {
                    "/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-04.mtaext" }, null,
                    new Expectation[] {
                        new Expectation(null),
                        new Expectation(Expectation.Type.EXCEPTION, "Unknown resource \"internal-odata-servicex\" in extension descriptor \"com.sap.mta.sample.v2.config-04\""),
                        new Expectation(Expectation.Type.SKIP, null),
                    },
                },
                // (06) Unsupported resource type in deployment descriptor:
                {
                    "/mta/sample/v2/mtad-02.yaml", null, null,
                    new Expectation[] {
                        new Expectation(Expectation.Type.EXCEPTION, "Unsupported resource type \"com.sap.hana.hdi-containerx\" for platform type \"CLOUD-FOUNDRY\""),
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(Expectation.Type.SKIP, null),
                    },
                },
                // (07) Unresolved required dependency in deployment descriptor:
                {
                    "/mta/sample/v2/mtad-03.yaml", null, null,
                    new Expectation[] {
                        new Expectation(Expectation.Type.EXCEPTION, "Unresolved required dependency \"internal-odatax\" for module \"web-server\""),
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(Expectation.Type.SKIP, null),
                    },
                },
                // (08) Unsupported module type in deployment descriptor:
                {
                    "/mta/sample/v2/mtad-04.yaml", null, null,
                    new Expectation[] {
                        new Expectation(Expectation.Type.EXCEPTION, "Unsupported module type \"com.sap.static-contentx\" for platform type \"CLOUD-FOUNDRY\""),
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(Expectation.Type.SKIP, null),
                    },
                },
                // (09) Unresolved parameters in merged descriptor:
                {
                    null, null, "merged-01.yaml",
                    new Expectation[] {
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(Expectation.Type.EXCEPTION, "Unresolved mandatory parameters: [baz#bar#test-5, baz#test-3, caz#test-6, foo#test-1, test-7]"),
                    },
                },
                // (10) Unknown required dependency in extension descriptor:
                {
                    "/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-09.mtaext", }, null,
                    new Expectation[] {
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(Expectation.Type.EXCEPTION, "Unknown required dependency \"pricing-dbx\" for module \"pricing\" in extension descriptor \"com.sap.mta.sample.v2.config-05\""),
                        new Expectation(Expectation.Type.SKIP, null),
                    },
                },
                // (11) Extension descriptor attempts to modify properties:
                {
                    "/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-07.mtaext", }, null,
                    new Expectation[]{
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(Expectation.Type.EXCEPTION, "Cannot modify property \"web-server#docu-url\" in extension descriptor \"com.sap.mta.sample.v2.config-07\""),
                        new Expectation(Expectation.Type.SKIP, null),
                    },
                },
                // (12) Extension descriptor attempts to modify parameters:
                {
                    "/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-10.mtaext", }, null,
                    new Expectation[]{
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(Expectation.Type.EXCEPTION, "Cannot modify parameter \"web-server#host\" in extension descriptor \"com.sap.mta.sample.v2.config-10\""),
                        new Expectation(Expectation.Type.SKIP, null),
                    },
                },
                // (13) Extension descriptor attempts to modify parameters:
                {
                    "/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-11.mtaext", }, null,
                    new Expectation[]{
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(Expectation.Type.EXCEPTION, "Cannot modify parameter \"com.sap.releaseMetadataRefGuid\" in extension descriptor \"com.sap.mta.sample.v2.config-11\""),
                        new Expectation(Expectation.Type.SKIP, null),
                    },
                },
                // (14) Merged descriptor contains properties and parameters with empty values (but not null):
                {
                    null, null, "/mta/sample/v2/merged-06.yaml",
                    new Expectation[] {
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(Expectation.Type.SKIP, null),
                        new Expectation(null),
                    },
                },
// @formatter:on
        });
    }

    public DescriptorValidatorTest(String deploymentDescriptorLocation, String[] extensionDescriptorLocations,
                                   String mergedDescriptorLocation, Expectation[] expectations) {
        this.deploymentDescriptorLocation = deploymentDescriptorLocation;
        this.extensionDescriptorLocations = extensionDescriptorLocations;
        this.mergedDescriptorLocation = mergedDescriptorLocation;
        this.expectations = expectations;
    }

    @Before
    public void setUp() throws Exception {
        DescriptorParser descriptorParser = getDescriptorParser();
        if (deploymentDescriptorLocation != null) {
            deploymentDescriptor = MtaTestUtil.loadDeploymentDescriptor(deploymentDescriptorLocation, descriptorParser, getClass());
        }
        if (extensionDescriptorLocations != null) {
            extensionDescriptors = MtaTestUtil.loadExtensionDescriptors(extensionDescriptorLocations, descriptorParser, getClass());
        }
        if (mergedDescriptorLocation != null) {
            mergedDescriptor = MtaTestUtil.loadDeploymentDescriptor(mergedDescriptorLocation, descriptorParser, getClass());
        }

        platform = MtaTestUtil.loadPlatform(platformLocation, getClass());

        validator = createValidator();
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    protected DescriptorValidator createValidator() {
        return new DescriptorValidator();
    }

    @Test
    public void testValidateDeploymentDescriptor() throws Exception {
        tester.test(() -> validator.validateDeploymentDescriptor(deploymentDescriptor, platform), expectations[0]);
    }

    @Test
    public void testValidateExtensionDescriptors() throws Exception {
        tester.test(() -> validator.validateExtensionDescriptors(extensionDescriptors, deploymentDescriptor), expectations[1]);

    }

    @Test
    public void testValidateMergedDescriptor() throws Exception {
        tester.test(() -> validator.validateMergedDescriptor(mergedDescriptor), expectations[2]);
    }

}
