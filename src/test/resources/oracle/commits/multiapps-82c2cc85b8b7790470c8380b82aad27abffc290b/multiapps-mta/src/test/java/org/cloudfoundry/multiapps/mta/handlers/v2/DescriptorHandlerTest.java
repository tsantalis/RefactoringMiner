package org.cloudfoundry.multiapps.mta.handlers.v2;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DescriptorHandlerTest {

    public static class DeployOrderTest {

        public static final String PARALLEL_DEPLOYMENTS_PROP = "parallel-deployments";
        public static final String DEPENDENCY_TYPE_PROP = "dependency-type";
        public static final String DEPENDENCY_TYPE_HARD = "hard";

        protected final DescriptorHandler handler = getDescriptorHandler();
        protected final Tester tester = Tester.forClass(getClass());

        protected DescriptorHandler getDescriptorHandler() {
            return new DescriptorHandler();
        }

        static Stream<Arguments> testGetSortedModules() {
            return Stream.of(Arguments.of("mtad-05.yaml", new Expectation("[bar, foo, baz, qux]")),
                             Arguments.of("mtad-06.yaml", new Expectation("[bar, foo]")),
                             Arguments.of("mtad-07.yaml", new Expectation("[foo, bar]")),
                             Arguments.of("mtad-08.yaml", new Expectation("[db, service, backend, dashboard, broker]")),
                             // There is a direct circular dependency between two modules, but one of the modules is soft:
                             Arguments.of("mtad-09.yaml", new Expectation("[bar, baz, foo]")),
                             // There is a direct circular dependency between two modules, and both of the modules are hard:
                             Arguments.of("mtad-10.yaml",
                                          new Expectation(Expectation.Type.EXCEPTION,
                                                          "Modules, \"baz\" and \"foo\", have hard circular dependencies")),
                             // There is a direct circular dependency between two modules, and both of the modules are soft:
                             Arguments.of("mtad-13.yaml", new Expectation("[bar, foo, baz]")),
                             // There is a circular transitive dependency between two modules, and both of the modules are hard:
                             Arguments.of("mtad-11.yaml",
                                          new Expectation(Expectation.Type.EXCEPTION,
                                                          "Modules, \"baz\" and \"foo\", have hard circular dependencies")),
                             // There is a circular transitive dependency between two modules, and both of the modules are hard:
                             Arguments.of("mtad-14.yaml", new Expectation("[foo, bar, baz]")),
                             // A module requires something from the same module:
                             Arguments.of("mtad-12.yaml",
                                          new Expectation("[di-core-db, di-local-npm-registry, di-core, di-builder, di-runner]")),
                             // There are two circular dependencies and all modules are soft:
                             Arguments.of("mtad-15.yaml", new Expectation("[foo, bar, baz, qux, quux]")),
                             // There are two circular dependencies and two modules are hard:
                             Arguments.of("mtad-16.yaml",
                                          new Expectation(Expectation.Type.EXCEPTION,
                                                          "Modules, \"baz\" and \"bar\", have hard circular dependencies")),
                             // There are two circular dependencies and one module is hard:
                             Arguments.of("mtad-17.yaml", new Expectation("[bar, foo, baz, qux, quux]")),
                             // There is a hard circular dependency between a module and itself:
                             Arguments.of("mtad-18.yaml", new Expectation("[bar, foo]")));
        }

        @ParameterizedTest
        @MethodSource
        public void testGetSortedModules(String deploymentDescriptorLocation, Expectation expectation) {
            String deploymentDescriptorYaml = TestUtil.getResourceAsString(deploymentDescriptorLocation, getClass());
            DeploymentDescriptor deploymentDescriptor = new DescriptorParserFacade().parseDeploymentDescriptor(deploymentDescriptorYaml);

            tester.test(() -> {
                return Arrays.toString(getNames(handler.getModulesForDeployment(deploymentDescriptor, PARALLEL_DEPLOYMENTS_PROP,
                                                                                DEPENDENCY_TYPE_PROP, DEPENDENCY_TYPE_HARD)));
            }, expectation);
        }

        private String[] getNames(List<? extends Module> modules) {
            return modules.stream()
                          .map(Module::getName)
                          .toArray(String[]::new);
        }

    }

}
