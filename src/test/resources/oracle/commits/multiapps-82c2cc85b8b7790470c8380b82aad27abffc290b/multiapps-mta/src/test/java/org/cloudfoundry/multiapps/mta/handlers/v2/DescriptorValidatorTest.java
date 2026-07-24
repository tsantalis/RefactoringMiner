package org.cloudfoundry.multiapps.mta.handlers.v2;

import java.util.List;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.MtaTestUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DescriptorValidatorTest {

    private static final String PLATFORM_LOCATION = "/mta/sample/platform-01.json";

    protected final Tester tester = Tester.forClass(getClass());

    private DescriptorValidator validator = createDescriptorValidator();
    private Platform platform;

    static Stream<Arguments> testValidateDeploymentDescriptor() {
        return Stream.of(
                         // Valid deployment descriptor:
                         Arguments.of("/mta/sample/v2/mtad-01.yaml", new Expectation(null)),
                         // Unsupported resource type in deployment descriptor:
                         Arguments.of("/mta/sample/v2/mtad-02.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Unsupported resource type \"com.sap.hana.hdi-containerx\" for platform type \"CLOUD-FOUNDRY\"")),
                         // Unresolved required dependency in deployment descriptor:
                         Arguments.of("/mta/sample/v2/mtad-03.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Unresolved required dependency \"internal-odatax\" for module \"web-server\"")),
                         // Unsupported module type in deployment descriptor:
                         Arguments.of("/mta/sample/v2/mtad-04.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Unsupported module type \"com.sap.static-contentx\" for platform type \"CLOUD-FOUNDRY\"")));
    }

    static Stream<Arguments> testValidateExtensionDescriptors() {
        return Stream.of(
                         // Valid extension descriptors:
                         Arguments.of("/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-01.mtaext" },
                                      new Expectation(null)),
                         // Unknown module in extension descriptor:
                         Arguments.of("/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-02.mtaext" },
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Unknown module \"web-serverx\" in extension descriptor \"com.sap.mta.sample.v2.config-02\"")),
                         // Unknown provided dependency in extension descriptor:
                         Arguments.of("/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-03.mtaext" },
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Unknown provided dependency \"internal-odatax\" for module \"pricing\" in extension descriptor \"com.sap.mta.sample.v2.config-03\"")),
                         // Unknown resource in extension descriptor:
                         Arguments.of("/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-04.mtaext" },
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Unknown resource \"internal-odata-servicex\" in extension descriptor \"com.sap.mta.sample.v2.config-04\"")),
                         // Unknown required dependency in extension descriptor:
                         Arguments.of("/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-09.mtaext" },
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Unknown required dependency \"pricing-dbx\" for module \"pricing\" in extension descriptor \"com.sap.mta.sample.v2.config-05\"")),
                         // Extension descriptor attempts to modify properties:
                         Arguments.of("/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-07.mtaext" },
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Cannot modify property \"web-server#docu-url\" in extension descriptor \"com.sap.mta.sample.v2.config-07\"")),
                         // Extension descriptor attempts to modify parameters:
                         Arguments.of("/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-10.mtaext" },
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Cannot modify parameter \"web-server#host\" in extension descriptor \"com.sap.mta.sample.v2.config-10\"")),
                         // Extension descriptor attempts to modify parameters:
                         Arguments.of("/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-11.mtaext" },
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Cannot modify parameter \"com.sap.releaseMetadataRefGuid\" in extension descriptor \"com.sap.mta.sample.v2.config-11\"")));
    }

    static Stream<Arguments> testValidateMergedDescriptor() {
        return Stream.of(
                         // Deploy target not listed in merged descriptor (there aren't any other platforms listed):
                         Arguments.of("/mta/sample/v2/merged-01.yaml", new Expectation(null)),
                         // Unresolved properties in merged descriptor:
                         Arguments.of("/mta/sample/v2/merged-03.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Unresolved mandatory properties: [competitor-data#test, pricing#internal-odata#test, pricing-db#pricing-db-service#test, pricing-db#test]")),
                         // Unresolved parameters in merged descriptor:
                         Arguments.of("merged-01.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Unresolved mandatory parameters: [baz#bar#test-5, baz#test-3, caz#test-6, foo#test-1, test-7]")),
                         // Merged descriptor contains properties and parameters with empty values (but not null):
                         Arguments.of("/mta/sample/v2/merged-06.yaml", new Expectation(null)));
    }

    @BeforeEach
    public void setUp() throws Exception {
        platform = MtaTestUtil.loadPlatform(PLATFORM_LOCATION, getClass());
    }

    @ParameterizedTest
    @MethodSource
    public void testValidateDeploymentDescriptor(String deploymentDescriptorLocation, Expectation expectation) throws Exception {
        DeploymentDescriptor deploymentDescriptor = parseDeploymentDescriptor(deploymentDescriptorLocation);
        tester.test(() -> validator.validateDeploymentDescriptor(deploymentDescriptor, platform), expectation);
    }

    @ParameterizedTest
    @MethodSource
    public void testValidateExtensionDescriptors(String deploymentDescriptorLocation, String[] extensionDescriptorLocations,
                                                 Expectation expectation)
        throws Exception {
        DeploymentDescriptor deploymentDescriptor = parseDeploymentDescriptor(deploymentDescriptorLocation);
        List<ExtensionDescriptor> extensionDescriptors = parseExtensionDescriptors(extensionDescriptorLocations);
        tester.test(() -> validator.validateExtensionDescriptors(extensionDescriptors, deploymentDescriptor), expectation);
    }

    @ParameterizedTest
    @MethodSource
    public void testValidateMergedDescriptor(String deploymentDescriptorLocation, Expectation expectation) throws Exception {
        DeploymentDescriptor deploymentDescriptor = parseDeploymentDescriptor(deploymentDescriptorLocation);
        tester.test(() -> validator.validateMergedDescriptor(deploymentDescriptor), expectation);
    }

    private DeploymentDescriptor parseDeploymentDescriptor(String deploymentDescriptorLocation) {
        return MtaTestUtil.loadDeploymentDescriptor(deploymentDescriptorLocation, createDescriptorParser(), getClass());
    }

    private List<ExtensionDescriptor> parseExtensionDescriptors(String[] extensionDescriptorLocations) {
        return MtaTestUtil.loadExtensionDescriptors(extensionDescriptorLocations, createDescriptorParser(), getClass());
    }

    protected DescriptorParser createDescriptorParser() {
        return new DescriptorParser();
    }

    protected DescriptorValidator createDescriptorValidator() {
        return new DescriptorValidator();
    }

}
