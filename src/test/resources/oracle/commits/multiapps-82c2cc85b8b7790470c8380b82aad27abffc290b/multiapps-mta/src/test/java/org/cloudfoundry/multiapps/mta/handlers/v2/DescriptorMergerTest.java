package org.cloudfoundry.multiapps.mta.handlers.v2;

import java.util.List;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.MtaTestUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DescriptorMergerTest {

    protected final Tester tester = Tester.forClass(getClass());
    protected final DescriptorMerger descriptorMerger = createDescriptorMerger();

    protected DescriptorMerger createDescriptorMerger() {
        return new DescriptorMerger();
    }

    protected DescriptorParser createDescriptorParser() {
        return new DescriptorParser();
    }

    static Stream<Arguments> testMerge() {
        return Stream.of(
                         // Valid deployment and extension descriptor:
                         Arguments.of("/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-01.mtaext", },
                                      new Expectation(Expectation.Type.JSON, "/mta/sample/v2/merged-04.yaml.json")),
                         // Valid deployment and extension descriptors (multiple):
                         Arguments.of("/mta/sample/v2/mtad-01.yaml",
                                      new String[] { "/mta/sample/v2/config-01.mtaext", "/mta/sample/v2/config-05.mtaext", },
                                      new Expectation(Expectation.Type.JSON, "/mta/sample/v2/merged-05.yaml.json")));
    }

    @ParameterizedTest
    @MethodSource
    public void testMerge(String deploymentDescriptorLocation, String[] extensionDescriptorLocations, Expectation expectation) {
        DescriptorParser descriptorParser = createDescriptorParser();
        DeploymentDescriptor deploymentDescriptor = parseDeploymentDescriptor(deploymentDescriptorLocation, descriptorParser);
        List<ExtensionDescriptor> extensionDescriptors = parseExtensionDescriptor(extensionDescriptorLocations, descriptorParser);
        tester.test(() -> descriptorMerger.merge(deploymentDescriptor, extensionDescriptors), expectation);
    }

    private List<ExtensionDescriptor> parseExtensionDescriptor(String[] extensionDescriptorLocations, DescriptorParser descriptorParser) {
        return MtaTestUtil.loadExtensionDescriptors(extensionDescriptorLocations, descriptorParser, getClass());
    }

    private DeploymentDescriptor parseDeploymentDescriptor(String deploymentDescriptorLocation, DescriptorParser descriptorParser) {
        return MtaTestUtil.loadDeploymentDescriptor(deploymentDescriptorLocation, descriptorParser, getClass());
    }

}
