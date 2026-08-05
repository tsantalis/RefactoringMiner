package org.cloudfoundry.multiapps.mta.handlers.v2;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class DescriptorHandlerTest {

    @RunWith(Parameterized.class)
    public static class DeployOrderTest {

        protected final Tester tester = Tester.forClass(getClass());

        public static final String PARALLEL_DEPLOYMENTS_PROP = "parallel-deployments";
        public static final String DEPENDENCY_TYPE_PROP = "dependency-type";
        public static final String DEPENDENCY_TYPE_HARD = "hard";

        protected final DescriptorHandler handler = getDescriptorHandler();

        protected String descriptorLocation;
        protected Expectation expectation;

        public DeployOrderTest(String descriptorLocation, Expectation expectation) {
            this.descriptorLocation = descriptorLocation;
            this.expectation = expectation;
        }

        protected DescriptorHandler getDescriptorHandler() {
            return new DescriptorHandler();
        }

        @Parameters
        public static Iterable<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
// @formatter:off
                // (00)
                {
                    "mtad-05.yaml", new Expectation("[bar, foo, baz, qux]"),
                },
                // (01)
                {
                    "mtad-06.yaml", new Expectation("[bar, foo]"),
                },
                // (02)
                {
                    "mtad-07.yaml", new Expectation("[foo, bar]"),
                },
                // (03) The broker has a transitive dependency to the backend:
                {
                    "mtad-08.yaml", new Expectation("[db, service, backend, dashboard, broker]"),
                },
                // (04) There is a direct circular dependency between two modules, but  one of the modules  is soft:
                {
                    "mtad-09.yaml", new Expectation("[bar, baz, foo]"),
                },
                // (05) There is a direct circular dependency between two modules, and both of the modules are hard:
                {
                    "mtad-10.yaml", new Expectation(Expectation.Type.EXCEPTION, "Modules, \"baz\" and \"foo\", have hard circular dependencies"),
                },
                // (06) There is a direct circular dependency between two modules, and both of the modules are soft:
                {
                    "mtad-13.yaml", new Expectation("[bar, foo, baz]"),
                },
                // (07) There is a circular transitive dependency between two modules, and both of the modules are hard:
                {
                    "mtad-11.yaml", new Expectation(Expectation.Type.EXCEPTION, "Modules, \"baz\" and \"foo\", have hard circular dependencies"),
                },
                // (08) There is a circular transitive dependency between two modules, and both of the modules are hard:
                {
                    "mtad-14.yaml", new Expectation("[foo, bar, baz]"),
                },
                // (09) A module requires something from the same module:
                {
                    "mtad-12.yaml", new Expectation("[di-core-db, di-local-npm-registry, di-core, di-builder, di-runner]"),
                },
                // (10) There are two circular dependencies and all modules are soft:
                {
                    "mtad-15.yaml", new Expectation("[foo, bar, baz, qux, quux]"),
                },
                // (11) There are two circular dependencies and two modules are hard:
                {
                    "mtad-16.yaml", new Expectation(Expectation.Type.EXCEPTION, "Modules, \"baz\" and \"bar\", have hard circular dependencies"),
                },
                // (12) There are two circular dependencies and one  module  is hard:
                {
                    "mtad-17.yaml", new Expectation("[bar, foo, baz, qux, quux]"),
                },
                // (13) There is a hard circular dependency between a module and itself:
                {
                    "mtad-18.yaml", new Expectation("[bar, foo]"),
                },
// @formatter:on
            });
        }

        @Test
        public void testGetSortedModules() {
            String deploymentDescriptorYaml = TestUtil.getResourceAsString(descriptorLocation, getClass());
            Map<String, Object> deploymentDescriptorMap = new YamlParser().convertYamlToMap(deploymentDescriptorYaml);
            DeploymentDescriptor descriptor = getDescriptorParser().parseDeploymentDescriptor(deploymentDescriptorMap);

            tester.test(() -> {
                return Arrays.toString(getNames(handler.getModulesForDeployment(descriptor, PARALLEL_DEPLOYMENTS_PROP, DEPENDENCY_TYPE_PROP,
                                                                                DEPENDENCY_TYPE_HARD)));
            }, expectation);
        }

        private String[] getNames(List<? extends Module> modules) {
            return modules.stream()
                          .map(Module::getName)
                          .toArray(String[]::new);
        }

        protected DescriptorParser getDescriptorParser() {
            return new DescriptorParser();
        }

    }

}
