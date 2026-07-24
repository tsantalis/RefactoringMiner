package org.cloudfoundry.multiapps.mta.builders.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ParametersChainBuilderTest extends PropertiesChainBuilderTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) All module and resource types are present in the platform:
            {
                "mtad-02.yaml", "platform-04.json",
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "module-chain-01.json"),
                    new Expectation(Expectation.Type.JSON, "module-chain-without-dependencies-01.json"),
                    new Expectation(Expectation.Type.JSON, "resource-chain-04.json"),
                    new Expectation(Expectation.Type.JSON, "resource-type-chain-01.json"),
                },
            },
            // (1) No module and resource types in the platform:
            {
                "mtad-02.yaml", "platform-02.json",
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "module-chain-02.json"),
                    new Expectation(Expectation.Type.JSON, "module-chain-without-dependencies-02.json"),
                    new Expectation(Expectation.Type.JSON, "resource-chain-02.json"),
                    new Expectation(Expectation.Type.JSON, "resource-type-chain-02.json"),
                },
            },
            // (2) Some module and resource types are present in the platform:
            {
                "mtad-02.yaml", "platform-05.json",
                new Expectation[] {
                    new Expectation(Expectation.Type.JSON, "module-chain-03.json"),
                    new Expectation(Expectation.Type.JSON, "module-chain-without-dependencies-03.json"),
                    new Expectation(Expectation.Type.JSON, "resource-chain-05.json"),
                    new Expectation(Expectation.Type.JSON, "resource-type-chain-03.json"),
                },
            },
// @formatter:on
        });
    }

    public ParametersChainBuilderTest(String deploymentDescriptorLocation, String platformLocation, Expectation[] expectations) {
        super(deploymentDescriptorLocation, platformLocation, expectations);
    }

    @Override
    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    @Override
    protected PropertiesChainBuilder createPropertiesChainBuilder(DeploymentDescriptor deploymentDescriptor, Platform platform) {
        return new ParametersChainBuilder(deploymentDescriptor, platform);
    }

    @Test
    public void testBuildResourceTypeChain() {
        tester.test(() -> {
            List<List<Map<String, Object>>> resourceTypeChains = new ArrayList<>();
            for (String resourceName : resourceNames) {
                resourceTypeChains.add(builder.buildResourceTypeChain(resourceName));
            }
            return resourceTypeChains;
        }, expectations[3]);
    }

}
