package org.cloudfoundry.multiapps.mta.handlers.v3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation.Type;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DescriptorHandlerTest extends org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorHandlerTest {

    public static class DeployOrderSequentialTest extends org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorHandlerTest.DeployOrderTest {

        @Override
        protected DescriptorHandler getDescriptorHandler() {
            return new DescriptorHandler();
        }

        static Stream<Arguments> testGetSortedModules() {
            return Stream.of(Arguments.of("mtad-05.yaml", new Expectation("[bar, foo, baz, qux]")),
                             Arguments.of("mtad-06.yaml", new Expectation("[bar, foo]")),
                             Arguments.of("mtad-07.yaml", new Expectation("[foo, bar]")),
                             // The broker has a transitive dependency to the backend:
                             Arguments.of("mtad-08.yaml", new Expectation("[db, service, backend, dashboard, broker]")),
                             // There is a direct circular dependency between two modules, but one of the modules is soft:
                             Arguments.of("mtad-09.yaml", new Expectation("[bar, baz, foo]")),
                             // There is a direct circular dependency between two modules, and both of the modules are hard:
                             Arguments.of("mtad-10.yaml",
                                          new Expectation(Type.EXCEPTION, "Modules, \"baz\" and \"foo\", have hard circular dependencies")),
                             // There is a direct circular dependency between two modules, and both of the modules are soft:
                             Arguments.of("mtad-13.yaml", new Expectation("[bar, foo, baz]")),
                             // There is a circular transitive dependency between two modules, and both of the modules are hard:
                             Arguments.of("mtad-11.yaml",
                                          new Expectation(Type.EXCEPTION, "Modules, \"baz\" and \"foo\", have hard circular dependencies")),
                             // There is a circular transitive dependency between two modules, and both of the modules are hard:
                             Arguments.of("mtad-14.yaml", new Expectation("[foo, bar, baz]")),
                             // A module requires something from the same module:
                             Arguments.of("mtad-12.yaml",
                                          new Expectation("[di-core-db, di-local-npm-registry, di-core, di-builder, di-runner]")),
                             // There are two circular dependencies and all modules are soft:
                             Arguments.of("mtad-15.yaml", new Expectation("[foo, bar, baz, qux, quux]")),
                             // There are two circular dependencies and two modules are hard:
                             Arguments.of("mtad-16.yaml",
                                          new Expectation(Type.EXCEPTION, "Modules, \"baz\" and \"bar\", have hard circular dependencies")),
                             // There are two circular dependencies and one module is hard:
                             Arguments.of("mtad-17.yaml", new Expectation("[bar, foo, baz, qux, quux]")),
                             // There is a hard circular dependency between a module and itself:
                             Arguments.of("mtad-18.yaml", new Expectation("[bar, foo]")),
                             // TODO: Enable this test as part of LMCROSSITXSADEPLOY-1935.
                             // The deployed-after attribute is used, therefore requires section is not used. Test whether comparator
                             // handles transitive relations:
                             // Arguments.of("mtad-deployed-after-transitive-relations.yaml", new Expectation("[qux, foo, bar, baz]")),
                             // The deployed-after attribute is used, therefore requires section is not used:
                             Arguments.of("mtad-deployed-after.yaml", new Expectation("[baz, bar, foo, qux]")));
        }

    }

    public static class DeployOrderParallelTest extends org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorHandlerTest.DeployOrderTest {

        @Override
        protected DescriptorHandler getDescriptorHandler() {
            return new DescriptorHandler();
        }

        static Stream<Arguments> testGetSortedModules() {
            return Stream.of(Arguments.of("mtad-05-deployed-after-with-parallel-deployments.yaml",
                                          new Expectation("{bar=[], foo=[bar], baz=[foo, bar], qux=[]}")),
                             Arguments.of("mtad-06-deployed-after-with-parallel-deployments.yaml", new Expectation("{bar=[], foo=[bar]}")),
                             Arguments.of("mtad-07-deployed-after-with-parallel-deployments.yaml", new Expectation("{foo=[], bar=[foo]}")),
                             // The broker has a transitive dependency to the backend:
                             Arguments.of("mtad-08-deployed-after-with-parallel-deployments.yaml",
                                          new Expectation("{db=[], service=[db], backend=[], dashboard=[service, db, backend], broker=[dashboard, service, db, backend]}")),
                             // Circular dependencies:
                             Arguments.of("mtad-10-deployed-after-with-parallel-deployments.yaml",
                                          new Expectation(Type.EXCEPTION,
                                                          "Modules, \"baz\" and \"foo\", both depend on each others for deployment")),
                             // Circular transitive dependencies:
                             Arguments.of("mtad-11-deployed-after-with-parallel-deployments.yaml",
                                          new Expectation(Type.EXCEPTION,
                                                          "Modules, \"bar\" and \"foo\", both depend on each others for deployment")),
                             // A module depends on itself:
                             Arguments.of("mtad-18-deployed-after-with-parallel-deployments.yaml", new Expectation("{bar=[], foo=[bar]}")));
        }

        @Override
        @ParameterizedTest
        @MethodSource
        public void testGetSortedModules(String deploymentDescriptorLocation, Expectation expectation) {
            String deploymentDescriptorYaml = TestUtil.getResourceAsString(deploymentDescriptorLocation, getClass());
            DeploymentDescriptor deploymentDescriptor = new DescriptorParserFacade().parseDeploymentDescriptor(deploymentDescriptorYaml);

            tester.test(() -> {
                return getDeployedAfterMapString(handler.getModulesForDeployment(deploymentDescriptor, PARALLEL_DEPLOYMENTS_PROP,
                                                                                 DEPENDENCY_TYPE_PROP, DEPENDENCY_TYPE_HARD));
            }, expectation);
        }

        private String getDeployedAfterMapString(List<? extends Module> modules) {
            Map<String, List<String>> deployedAfterMap = new LinkedHashMap<>();
            for (Module module : modules) {
                deployedAfterMap.put(module.getName(), ListUtils.emptyIfNull(module.getDeployedAfter()));
            }
            return deployedAfterMap.toString();
        }

    }

}
