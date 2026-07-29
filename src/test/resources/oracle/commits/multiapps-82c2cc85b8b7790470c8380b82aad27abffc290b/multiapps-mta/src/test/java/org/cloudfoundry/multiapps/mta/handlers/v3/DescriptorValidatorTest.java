package org.cloudfoundry.multiapps.mta.handlers.v3;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.junit.jupiter.params.provider.Arguments;

public class DescriptorValidatorTest extends org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorValidatorTest {

    static Stream<Arguments> testValidateDeploymentDescriptor() {
        return Stream.of(
                         // Valid deployment descriptor:
                         Arguments.of("/mta/sample/v3/mtad-01.yaml", new Expectation(null)),
                         // With no extension descriptors and non-modifiable value:
                         Arguments.of("/mta/sample/v3/mtad-02.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "The parameter \"web-server#test\" is not optional and has no value.")),
                         // With overwritable parameter in the deployment descriptor:
                         Arguments.of("/mta/sample/v3/mtad-03.yaml", new Expectation(null)),
                         // With overwritable parameter in the deployment descriptor:
                         Arguments.of("/mta/sample/v3/mtad-04.yaml", new Expectation(null)));
    }

    static Stream<Arguments> testValidateExtensionDescriptors() {
        return Stream.of(
                         // Valid extension descriptors:
                         Arguments.of("/mta/sample/v3/mtad-01.yaml", new String[] { "/mta/sample/v3/config-01.mtaext" },
                                      new Expectation(null)),
                         // With unknown module in extension descriptor:
                         Arguments.of("/mta/sample/v3/mtad-02.yaml", new String[] { "/mta/sample/v3/config-02.mtaext" },
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Unknown module \"web-serverx\" in extension descriptor \"com.sap.mta.sample.v3.config-02\"")),
                         // With non-modifiable property in the descriptor:
                         Arguments.of("/mta/sample/v3/mtad-02.yaml", new String[] { "/mta/sample/v3/config-03.mtaext" },
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Cannot modify property \"web-server#test\" in extension descriptor \"com.sap.mta.sample.v3.config-03\"")),
                         // With overwritable parameter in the deployment descriptor:
                         Arguments.of("/mta/sample/v3/mtad-03.yaml", new String[] { "/mta/sample/v3/config-05.mtaext" },
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Cannot modify parameter \"web-server#test\" in extension descriptor \"com.sap.mta.sample.v3.config-05\"")),
                         // With overwritable parameter in the deployment descriptor:
                         Arguments.of("/mta/sample/v3/mtad-04.yaml", new String[] { "/mta/sample/v3/config-06.mtaext" },
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Cannot modify parameter \"web-server#test\" in extension descriptor \"com.sap.mta.sample.v3.config-06\"")));
    }

    static Stream<Arguments> testValidateMergedDescriptor() {
        return Stream.of(
                         // With non-overwritable parameter in the merged descriptor:
                         Arguments.of("/mta/sample/v3/merged-03.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "The parameter \"web-server#test\" is not optional and has no value.")),
                         // Unresolved properties in merged descriptor:
                         Arguments.of("/mta/sample/v3/merged-06.yaml", new Expectation(null)));
    }

    @Override
    protected DescriptorParser createDescriptorParser() {
        return new DescriptorParser();
    }

    @Override
    protected DescriptorValidator createDescriptorValidator() {
        return new DescriptorValidator();
    }

}
