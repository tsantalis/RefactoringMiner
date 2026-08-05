package org.cloudfoundry.multiapps.mta.mergers;

import java.io.InputStream;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.handlers.ConfigurationParser;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorHandler;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PlatformMergerTest {

    private final Tester tester = Tester.forClass(getClass());

    static Stream<Arguments> testMerge() {
        return Stream.of(Arguments.of("mtad-00.yaml", "platform-00.json",
                                      new Expectation(Expectation.Type.JSON, "result-platform-00.json")),
                         Arguments.of("mtad-01.yaml", "platform-01.json",
                                      new Expectation(Expectation.Type.JSON, "result-platform-01.json")),
                         Arguments.of("mtad-00.yaml", "platform-02.json",
                                      new Expectation(Expectation.Type.JSON, "result-platform-02.json")));
    }

    @ParameterizedTest
    @MethodSource
    void testMerge(String deploymentDescriptorLocation, String platformLocation, Expectation expectation) {
        DeploymentDescriptor deploymentDescriptor = parseDeploymentDescriptor(deploymentDescriptorLocation);
        Platform platform = parsePlatform(platformLocation);
        PlatformMerger merger = createPlatformMerger(platform, createDescriptorHandler());
        tester.test(() -> {
            merger.mergeInto(deploymentDescriptor);
            return deploymentDescriptor;
        }, expectation);
    }

    private DeploymentDescriptor parseDeploymentDescriptor(String deploymentDescriptorLocation) {
        InputStream deploymentDescriptorYaml = getClass().getResourceAsStream(deploymentDescriptorLocation);
        return new DescriptorParserFacade().parseDeploymentDescriptor(deploymentDescriptorYaml);
    }

    private Platform parsePlatform(String platformLocation) {
        InputStream platformJson = getClass().getResourceAsStream(platformLocation);
        return new ConfigurationParser().parsePlatformJson(platformJson);
    }

    protected DescriptorHandler createDescriptorHandler() {
        return new DescriptorHandler();
    }

    protected PlatformMerger createPlatformMerger(Platform platform, DescriptorHandler handler) {
        return new PlatformMerger(platform, handler);
    }

}
