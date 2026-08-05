package org.cloudfoundry.multiapps.mta.resolvers.v2;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.MtaTestUtil;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.resolvers.ResolverBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DescriptorReferenceResolverTest {

    private final Tester tester = Tester.forClass(getClass());

    private DescriptorReferenceResolver resolver;

    static Stream<Arguments> testResolve() {
        return Stream.of(
                         // (0) Resolve references to string properties:
                         Arguments.of("merged-01.yaml", new Expectation(Expectation.Type.JSON, "resolved-01.yaml.json")),
                         // (1) Resolve references to object properties:
                         Arguments.of("merged-02.yaml", new Expectation(Expectation.Type.JSON, "resolved-02.yaml.json")),
                         // (2) Test alternative grouping of properties when there is no corresponding requires dependency:
                         Arguments.of("merged-05.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Module \"foo\" does not contain a required dependency for \"bar\", but contains references to its properties")),
                         // (3) Attempt to resolve references to non-existing properties:
                         Arguments.of("merged-03.yaml",
                                      new Expectation(Expectation.Type.EXCEPTION, "Unable to resolve \"foo#baz#non-existing\"")),
                         // (4) Test support for partial plugins:
                         Arguments.of("merged-04.yaml", new Expectation(Expectation.Type.JSON, "resolved-04.yaml.json")),
                         // (5) TODO: Add description here.
                         Arguments.of("merged-06.yaml", new Expectation(Expectation.Type.JSON, "resolved-05.yaml.json")),
                         // (6) The same reference occurs multiple times in the same property:
                         Arguments.of("mtad-with-repeating-reference.yaml",
                                      new Expectation(Expectation.Type.JSON, "result-from-repeating-reference.json")),
                         // (7)
                         Arguments.of("mtad-with-escaped-references.yaml",
                                      new Expectation(Expectation.Type.JSON, "result-from-escaped-references.json")));
    }

    @ParameterizedTest
    @MethodSource
    void testResolve(String mergedDescriptorLocation, Expectation expectation) {
        init(mergedDescriptorLocation);

        tester.test(() -> resolver.resolve(), expectation);
    }

    private void init(String mergedDescriptorLocation) {
        DeploymentDescriptor mergedDescriptor = MtaTestUtil.loadDeploymentDescriptor(mergedDescriptorLocation, new DescriptorParser(),
                                                                                     getClass());
        resolver = new DescriptorReferenceResolver(mergedDescriptor, new ResolverBuilder(), new ResolverBuilder());
    }

}
