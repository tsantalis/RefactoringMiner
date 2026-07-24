package org.cloudfoundry.multiapps.mta.resolvers.v2;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.resolvers.ReferencesUnescaper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ReferencesUnescaperTest {

    private Tester tester = Tester.forClass(getClass());
    private ReferencesUnescaper referencesUnescaper = new ReferencesUnescaper();

    static Stream<Arguments> testUnescaping() {
        return Stream.of(Arguments.of("mtad-with-escaped-placeholders.yaml",
                                      new Expectation(Expectation.Type.JSON, "result-from-unescaping-placeholders.json")),
                         Arguments.of("mtad-with-escaped-references.yaml",
                                      new Expectation(Expectation.Type.JSON, "result-from-unescaping-references.json")));
    }

    @ParameterizedTest
    @MethodSource
    void testUnescaping(String descriptorResource, Expectation expectation) {
        DeploymentDescriptor descriptor = parseDeploymentDescriptor(descriptorResource);
        tester.test(() -> {
            referencesUnescaper.unescapeReferences(descriptor);
            return descriptor;
        }, expectation);
    }

    private DeploymentDescriptor parseDeploymentDescriptor(String descriptorResource) {
        String yaml = TestUtil.getResourceAsString(descriptorResource, getClass());
        return new DescriptorParserFacade().parseDeploymentDescriptor(yaml);
    }

}
