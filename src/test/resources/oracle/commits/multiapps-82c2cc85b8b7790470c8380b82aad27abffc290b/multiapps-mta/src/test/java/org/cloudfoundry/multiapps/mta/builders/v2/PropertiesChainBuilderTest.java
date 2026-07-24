package org.cloudfoundry.multiapps.mta.builders.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.MtaTestUtil;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PropertiesChainBuilderTest {

    protected final Tester tester = Tester.forClass(getClass());

    static Stream<Arguments> testBuildModuleChain() {
        return Stream.of(Arguments.of("mtad-01.yaml", "platform-01.json", new Expectation(Expectation.Type.JSON, "module-chain-04.json")),
                         Arguments.of("mtad-01.yaml", "platform-02.json", new Expectation(Expectation.Type.JSON, "module-chain-05.json")),
                         Arguments.of("mtad-01.yaml", "platform-03.json", new Expectation(Expectation.Type.JSON, "module-chain-06.json")));
    }

    static Stream<Arguments> testBuildModuleChainWithoutDependencies() {
        return Stream.of(Arguments.of("mtad-01.yaml", "platform-01.json",
                                      new Expectation(Expectation.Type.JSON, "module-chain-without-dependencies-04.json")),
                         Arguments.of("mtad-01.yaml", "platform-02.json",
                                      new Expectation(Expectation.Type.JSON, "module-chain-without-dependencies-05.json")),
                         Arguments.of("mtad-01.yaml", "platform-03.json",
                                      new Expectation(Expectation.Type.JSON, "module-chain-without-dependencies-06.json")));
    }

    static Stream<Arguments> testBuildResourceChain() {
        return Stream.of(Arguments.of("mtad-01.yaml", "platform-01.json", new Expectation(Expectation.Type.JSON, "resource-chain-01.json")),
                         Arguments.of("mtad-01.yaml", "platform-02.json", new Expectation(Expectation.Type.JSON, "resource-chain-06.json")),
                         Arguments.of("mtad-01.yaml", "platform-03.json",
                                      new Expectation(Expectation.Type.JSON, "resource-chain-03.json")));
    }

    @ParameterizedTest
    @MethodSource
    void testBuildModuleChain(String deploymentDescriptorLocation, String platformLocation, Expectation expectation) {
        DeploymentDescriptor deploymentDescriptor = parseDeploymentDescriptor(deploymentDescriptorLocation);
        Platform platform = parsePlatform(platformLocation);

        PropertiesChainBuilder builder = createPropertiesChainBuilder(deploymentDescriptor, platform);
        tester.test(() -> {
            List<List<Map<String, Object>>> moduleChains = new ArrayList<>();
            for (String moduleName : getModuleNames(deploymentDescriptor)) {
                moduleChains.add(builder.buildModuleChain(moduleName));
            }
            return moduleChains;
        }, expectation);
    }

    @ParameterizedTest
    @MethodSource
    void testBuildModuleChainWithoutDependencies(String deploymentDescriptorLocation, String platformLocation, Expectation expectation) {
        DeploymentDescriptor deploymentDescriptor = parseDeploymentDescriptor(deploymentDescriptorLocation);
        Platform platform = parsePlatform(platformLocation);

        PropertiesChainBuilder builder = createPropertiesChainBuilder(deploymentDescriptor, platform);
        tester.test(() -> {
            List<List<Map<String, Object>>> moduleChains = new ArrayList<>();
            for (String moduleName : getModuleNames(deploymentDescriptor)) {
                moduleChains.add(builder.buildModuleChainWithoutDependencies(moduleName));
            }
            return moduleChains;
        }, expectation);
    }

    @ParameterizedTest
    @MethodSource
    void testBuildResourceChain(String deploymentDescriptorLocation, String platformLocation, Expectation expectation) {
        DeploymentDescriptor deploymentDescriptor = parseDeploymentDescriptor(deploymentDescriptorLocation);
        Platform platform = parsePlatform(platformLocation);

        PropertiesChainBuilder builder = createPropertiesChainBuilder(deploymentDescriptor, platform);
        tester.test(() -> {
            List<List<Map<String, Object>>> resourceChains = new ArrayList<>();
            for (String resourceName : getResourceNames(deploymentDescriptor)) {
                resourceChains.add(builder.buildResourceChain(resourceName));
            }
            return resourceChains;
        }, expectation);
    }

    protected DeploymentDescriptor parseDeploymentDescriptor(String deploymentDescriptorLocation) {
        return MtaTestUtil.loadDeploymentDescriptor(deploymentDescriptorLocation, createDescriptorParser(), getClass());
    }

    protected Platform parsePlatform(String platformLocation) {
        return MtaTestUtil.loadPlatform(platformLocation, getClass());
    }

    protected List<String> getResourceNames(DeploymentDescriptor deploymentDescriptor) {
        List<String> resourceNames = new ArrayList<>();
        for (Resource resource : deploymentDescriptor.getResources()) {
            resourceNames.add(resource.getName());
        }
        return resourceNames;
    }

    protected List<String> getModuleNames(DeploymentDescriptor deploymentDescriptor) {
        List<String> moduleNames = new ArrayList<>();
        for (Module module : deploymentDescriptor.getModules()) {
            moduleNames.add(module.getName());
        }
        return moduleNames;
    }

    protected PropertiesChainBuilder createPropertiesChainBuilder(DeploymentDescriptor deploymentDescriptor, Platform platform) {
        return new PropertiesChainBuilder(deploymentDescriptor, platform);
    }

    protected DescriptorParser createDescriptorParser() {
        return new DescriptorParser();
    }

}
