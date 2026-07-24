package org.cloudfoundry.multiapps.mta.handlers.v3;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.junit.jupiter.params.provider.Arguments;

public class DescriptorMergerTest extends org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorMergerTest {

    static Stream<Arguments> testMerge() {
        return Stream.of(
                         // Valid deployment and extension descriptor:
                         Arguments.of("/mta/sample/v3/mtad-01.yaml", new String[] { "/mta/sample/v3/config-01.mtaext", },
                                      new Expectation(Expectation.Type.JSON, "/mta/sample/v3/merged-04.yaml.json")),
                         // Valid deployment and extension descriptors (multiple):
                         Arguments.of("/mta/sample/v3/mtad-01.yaml",
                                      new String[] { "/mta/sample/v3/config-01.mtaext", "/mta/sample/v3/config-05.mtaext", },
                                      new Expectation(Expectation.Type.JSON, "/mta/sample/v3/merged-05.yaml.json")),
                         // Merge hook parameters from deployment and extension descriptor:
                         Arguments.of("/mta/sample/v3/mtad-06.yaml", new String[] { "/mta/sample/v3/config-08.mtaext", },
                                      new Expectation(Expectation.Type.JSON, "/mta/sample/v3/merged-06.yaml.json")),
                         // Merge hook required dependencies from deployment and extension descriptor:
                         Arguments.of("/mta/sample/v3/mtad-06.yaml", new String[] { "/mta/sample/v3/config-09.mtaext", },
                                      new Expectation(Expectation.Type.JSON, "/mta/sample/v3/merged-07.yaml.json")));
    }

    @Override
    protected DescriptorMerger createDescriptorMerger() {
        return new DescriptorMerger();
    }

    @Override
    protected DescriptorParser createDescriptorParser() {
        return new DescriptorParser();
    }

}
