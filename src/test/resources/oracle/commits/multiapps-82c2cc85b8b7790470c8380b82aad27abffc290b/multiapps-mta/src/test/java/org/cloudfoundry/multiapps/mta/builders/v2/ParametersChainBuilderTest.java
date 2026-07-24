package org.cloudfoundry.multiapps.mta.builders.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ParametersChainBuilderTest extends PropertiesChainBuilderTest {

    static Stream<Arguments> testBuildModuleChain() {
        return Stream.of(Arguments.of("mtad-02.yaml", "platform-04.json", new Expectation(Expectation.Type.JSON, "module-chain-01.json")),
                         Arguments.of("mtad-02.yaml", "platform-02.json", new Expectation(Expectation.Type.JSON, "module-chain-02.json")),
                         Arguments.of("mtad-02.yaml", "platform-05.json", new Expectation(Expectation.Type.JSON, "module-chain-03.json")));
    }

    static Stream<Arguments> testBuildModuleChainWithoutDependencies() {
        return Stream.of(Arguments.of("mtad-02.yaml", "platform-04.json",
                                      new Expectation(Expectation.Type.JSON, "module-chain-without-dependencies-01.json")),
                         Arguments.of("mtad-02.yaml", "platform-02.json",
                                      new Expectation(Expectation.Type.JSON, "module-chain-without-dependencies-02.json")),
                         Arguments.of("mtad-02.yaml", "platform-05.json",
                                      new Expectation(Expectation.Type.JSON, "module-chain-without-dependencies-03.json")));
    }

    static Stream<Arguments> testBuildResourceChain() {
        return Stream.of(Arguments.of("mtad-02.yaml", "platform-04.json", new Expectation(Expectation.Type.JSON, "resource-chain-04.json")),
                         Arguments.of("mtad-02.yaml", "platform-02.json", new Expectation(Expectation.Type.JSON, "resource-chain-02.json")),
                         Arguments.of("mtad-02.yaml", "platform-05.json",
                                      new Expectation(Expectation.Type.JSON, "resource-chain-05.json")));
    }

    static Stream<Arguments> testBuildResourceTypeChain() {
        return Stream.of(Arguments.of("mtad-02.yaml", "platform-04.json",
                                      new Expectation(Expectation.Type.JSON, "resource-type-chain-01.json")),
                         Arguments.of("mtad-02.yaml", "platform-02.json",
                                      new Expectation(Expectation.Type.JSON, "resource-type-chain-02.json")),
                         Arguments.of("mtad-02.yaml", "platform-05.json",
                                      new Expectation(Expectation.Type.JSON, "resource-type-chain-03.json")));
    }

    @ParameterizedTest
    @MethodSource
    void testBuildResourceTypeChain(String deploymentDescriptorLocation, String platformLocation, Expectation expectation) {
        DeploymentDescriptor deploymentDescriptor = parseDeploymentDescriptor(deploymentDescriptorLocation);
        Platform platform = parsePlatform(platformLocation);

        PropertiesChainBuilder builder = createPropertiesChainBuilder(deploymentDescriptor, platform);
        tester.test(() -> {
            List<List<Map<String, Object>>> resourceTypeChains = new ArrayList<>();
            for (String resourceName : getResourceNames(deploymentDescriptor)) {
                resourceTypeChains.add(builder.buildResourceTypeChain(resourceName));
            }
            return resourceTypeChains;
        }, expectation);
    }

    @Override
    protected PropertiesChainBuilder createPropertiesChainBuilder(DeploymentDescriptor deploymentDescriptor, Platform platform) {
        return new ParametersChainBuilder(deploymentDescriptor, platform);
    }

    @Override
    protected DescriptorParser createDescriptorParser() {
        return new DescriptorParser();
    }

}
