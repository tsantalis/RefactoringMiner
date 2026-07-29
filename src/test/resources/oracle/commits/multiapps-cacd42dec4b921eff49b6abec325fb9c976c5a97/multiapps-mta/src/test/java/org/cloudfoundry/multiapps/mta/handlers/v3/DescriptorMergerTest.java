package org.cloudfoundry.multiapps.mta.handlers.v3;

import java.util.Arrays;

import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.junit.runners.Parameterized.Parameters;

public class DescriptorMergerTest extends org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorMergerTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Valid deployment and extension descriptor:
            {
                "/mta/sample/v3/mtad-01.yaml", new String[] { "/mta/sample/v3/config-01.mtaext", },
                new Expectation(Expectation.Type.JSON, "/mta/sample/v3/merged-04.yaml.json"),
            },
            // (1) Valid deployment and extension descriptors (multiple):
            {
                "/mta/sample/v3/mtad-01.yaml", new String[] { "/mta/sample/v3/config-01.mtaext", "/mta/sample/v3/config-05.mtaext", },
                new Expectation(Expectation.Type.JSON, "/mta/sample/v3/merged-05.yaml.json"),
            },
            // (2) Merge hook parameters from deployment and extension descriptor
            {
                "/mta/sample/v3/mtad-06.yaml", new String[] { "/mta/sample/v3/config-08.mtaext", },
                new Expectation(Expectation.Type.JSON, "/mta/sample/v3/merged-06.yaml.json"),
            },
            // (3) Merge hook required dependencies from deployment and extension descriptor
            {
                "/mta/sample/v3/mtad-06.yaml", new String[] { "/mta/sample/v3/config-09.mtaext", },
                new Expectation(Expectation.Type.JSON, "/mta/sample/v3/merged-07.yaml.json"),
            },
// @formatter:on
        });
    }

    public DescriptorMergerTest(String deploymentDescriptorLocation, String[] extensionDescriptorLocations, Expectation expectation) {
        super(deploymentDescriptorLocation, extensionDescriptorLocations, expectation);
    }

    @Override
    protected DescriptorMerger createDescriptorMerger() {
        return new DescriptorMerger();
    }

    @Override
    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

}
