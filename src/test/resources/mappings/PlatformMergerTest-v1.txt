package org.cloudfoundry.multiapps.mta.mergers;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.mta.handlers.ConfigurationParser;
import org.cloudfoundry.multiapps.mta.handlers.HandlerFactory;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorHandler;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PlatformMergerTest {

    private final Tester tester = Tester.forClass(getClass());

    @Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0)
            {
                "mtad-00.yaml", "platform-00.json", new Expectation(Expectation.Type.JSON, "result-platform-00.json"),
            },
            // (1)
            {
                "mtad-01.yaml", "platform-01.json", new Expectation(Expectation.Type.JSON, "result-platform-01.json"),
            },
            // (2)
            {
                "mtad-00.yaml", "platform-02.json", new Expectation(Expectation.Type.JSON, "result-platform-02.json"),
            },
// @formatter:on
        });
    }

    private String deploymentDescriptorLocation;
    private String platformLocation;
    private Expectation expectation;
    private DeploymentDescriptor descriptor;
    private Platform platform;

    public PlatformMergerTest(String deploymentDescriptorLocation, String platformLocation, Expectation expectation) {
        this.deploymentDescriptorLocation = deploymentDescriptorLocation;
        this.platformLocation = platformLocation;
        this.expectation = expectation;
    }

    protected int getMajorVersion() {
        return 2;
    }

    protected PlatformMerger getPlatformMerger(Platform platform, DescriptorHandler handler) {
        return new PlatformMerger(platform, handler);
    }

    @Before
    public void prepare() {
        loadDeploymentDescriptor();
        loadPlatform();
    }

    private void loadDeploymentDescriptor() {
        DescriptorParser parser = getHandlerFactory().getDescriptorParser();
        InputStream deploymentDescriptorYaml = getClass().getResourceAsStream(deploymentDescriptorLocation);
        Map<String, Object> deploymentDescriptorMap = new YamlParser().convertYamlToMap(deploymentDescriptorYaml);
        this.descriptor = parser.parseDeploymentDescriptor(deploymentDescriptorMap);
    }

    private void loadPlatform() {
        InputStream platformJson = getClass().getResourceAsStream(platformLocation);
        this.platform = new ConfigurationParser().parsePlatformJson(platformJson);
    }

    @Test
    public void testMerge() {
        DescriptorHandler handler = getHandlerFactory().getDescriptorHandler();
        PlatformMerger merger = getPlatformMerger(platform, handler);
        tester.test(() -> {
            merger.mergeInto(descriptor);
            return descriptor;
        }, expectation);
    }

    protected HandlerFactory getHandlerFactory() {
        return new HandlerFactory(getMajorVersion());
    }

}
