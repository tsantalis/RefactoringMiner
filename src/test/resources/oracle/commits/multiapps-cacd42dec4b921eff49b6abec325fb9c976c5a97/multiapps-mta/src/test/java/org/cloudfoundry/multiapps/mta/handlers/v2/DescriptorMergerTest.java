package org.cloudfoundry.multiapps.mta.handlers.v2;

import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.MtaTestUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DescriptorMergerTest {

    protected final Tester tester = Tester.forClass(getClass());

    private final String deploymentDescriptorLocation;
    private final String[] extensionDescriptorLocations;
    private final Expectation expectation;

    private DeploymentDescriptor deploymentDescriptor;
    private List<ExtensionDescriptor> extensionDescriptors;

    private DescriptorMerger merger;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Valid deployment and extension descriptor:
            {
                "/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-01.mtaext", },
                new Expectation(Expectation.Type.JSON, "/mta/sample/v2/merged-04.yaml.json"),
            },
            // (1) Valid deployment and extension descriptors (multiple):
            {
                "/mta/sample/v2/mtad-01.yaml", new String[] { "/mta/sample/v2/config-01.mtaext", "/mta/sample/v2/config-05.mtaext", },
                new Expectation(Expectation.Type.JSON, "/mta/sample/v2/merged-05.yaml.json"),
            },
// @formatter:on
        });
    }

    public DescriptorMergerTest(String deploymentDescriptorLocation, String[] extensionDescriptorLocations, Expectation expectation) {
        this.deploymentDescriptorLocation = deploymentDescriptorLocation;
        this.extensionDescriptorLocations = extensionDescriptorLocations;
        this.expectation = expectation;
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

        merger = createDescriptorMerger();
    }

    protected DescriptorMerger createDescriptorMerger() {
        return new DescriptorMerger();
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    @Test
    public void testMerge() {
        tester.test(() -> merger.merge(deploymentDescriptor, extensionDescriptors), expectation);
    }

}
