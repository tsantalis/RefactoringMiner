package org.cloudfoundry.multiapps.mta.handlers.v3;

import java.util.Arrays;

import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.junit.runners.Parameterized.Parameters;

public class DescriptorValidatorTest extends org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorValidatorTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) Valid deployment and extension descriptors without metadata:
            {
                "/mta/sample/v3/mtad-01.yaml", new String[] { "/mta/sample/v3/config-01.mtaext" }, null,
                new Expectation[] {
                    new Expectation(null),
                    new Expectation(null),
                    new Expectation(Expectation.Type.SKIP, null),
                },
            },
            // (01) With no extension descriptors and non-modifiable value:
            {
                "/mta/sample/v3/mtad-02.yaml", new String[] { }, null,
                new Expectation[] {
                    new Expectation(Expectation.Type.EXCEPTION, "The parameter \"web-server#test\" is not optional and has no value."),
                    new Expectation(Expectation.Type.SKIP, null),
                    new Expectation(Expectation.Type.SKIP, null),
                },
            },
            // (02) With unknown module in ext descriptor:
            {
                "/mta/sample/v3/mtad-02.yaml", new String[] { "/mta/sample/v3/config-02.mtaext" }, null,
                new Expectation[] {
                    new Expectation(Expectation.Type.SKIP, null),
                    new Expectation(Expectation.Type.EXCEPTION, "Unknown module \"web-serverx\" in extension descriptor \"com.sap.mta.sample.v3.config-02\""),
                    new Expectation(Expectation.Type.SKIP, null),
                },
            },
            // (03) With non-modifiable property in the descriptor:
            {
                "/mta/sample/v3/mtad-02.yaml", new String[] { "/mta/sample/v3/config-03.mtaext" }, null,
                new Expectation[] {
                    new Expectation(Expectation.Type.SKIP, null),
                    new Expectation(Expectation.Type.EXCEPTION, "Cannot modify property \"web-server#test\" in extension descriptor \"com.sap.mta.sample.v3.config-03\""),
                    new Expectation(Expectation.Type.SKIP, null),
                },
            },
            // (04) With empty parameter in the deployment descriptor which cannot be overwritten:
            {
                "/mta/sample/v3/mtad-02.yaml", new String[] { "/mta/sample/v3/config-04.mtaext" }, null,
                new Expectation[] {
                    new Expectation(Expectation.Type.EXCEPTION, "The parameter \"web-server#test\" is not optional and has no value."),
                    new Expectation(Expectation.Type.SKIP, null),
                    new Expectation(Expectation.Type.SKIP, null),
                },
            },
            // (05) With overwriteable parameter in the deployment descriptor:
            {
                "/mta/sample/v3/mtad-03.yaml", new String[] { "/mta/sample/v3/config-05.mtaext" }, null,
                new Expectation[] {
                    new Expectation(null),
                    new Expectation(Expectation.Type.EXCEPTION, "Cannot modify parameter \"web-server#test\" in extension descriptor \"com.sap.mta.sample.v3.config-05\""),
                    new Expectation(Expectation.Type.SKIP, null),
                },
            },
            // (06) With non-overwritable parameter in the merged descriptor:
            {
                null, null, "/mta/sample/v3/merged-03.yaml",
                new Expectation[] {
                    new Expectation(Expectation.Type.SKIP, null),
                    new Expectation(Expectation.Type.SKIP, null),
                    new Expectation(Expectation.Type.EXCEPTION, "The parameter \"web-server#test\" is not optional and has no value."),
                },
            },
            // (07) With overwritable parameter in the descriptor:
            {
                "/mta/sample/v3/mtad-04.yaml", new String[] { "/mta/sample/v3/config-06.mtaext" }, null,
                new Expectation[] {
                    new Expectation(null),
                    new Expectation(Expectation.Type.EXCEPTION, "Cannot modify parameter \"web-server#test\" in extension descriptor \"com.sap.mta.sample.v3.config-05\""),
                    new Expectation(Expectation.Type.SKIP, null),
                },
            },
            // (08) Merged descriptor contains properties and parameters with empty values (but not null):
            {
                null, null, "/mta/sample/v3/merged-06.yaml",
                new Expectation[] {
                    new Expectation(Expectation.Type.SKIP, null),
                    new Expectation(Expectation.Type.SKIP, null),
                    new Expectation(null), },
            },
// @formatter:on
        });
    }

    public DescriptorValidatorTest(String deploymentDescriptorLocation, String[] extensionDescriptorLocations,
                                   String mergedDescriptorLocation, Expectation[] expectations) {
        super(deploymentDescriptorLocation, extensionDescriptorLocations, mergedDescriptorLocation, expectations);
    }

    @Override
    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    @Override
    protected DescriptorValidator createValidator() {
        return new DescriptorValidator();
    }

}
