package org.cloudfoundry.multiapps.controller.core.cf.v2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.cf.detect.AppSuffixDeterminer;
import org.cloudfoundry.multiapps.controller.core.cf.util.ModulesCloudModelBuilderContentCalculator;
import org.cloudfoundry.multiapps.controller.core.cf.util.ResourcesCloudModelBuilderContentCalculator;
import org.cloudfoundry.multiapps.controller.core.cf.util.UnresolvedModulesContentValidator;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.mta.handlers.ConfigurationParser;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorMerger;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.mergers.PlatformMerger;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.resolvers.ResolverBuilder;
import org.cloudfoundry.multiapps.mta.resolvers.v2.DescriptorReferenceResolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

public class CloudModelBuilderTest {

    protected static final String DEFAULT_DOMAIN_CF = "cfapps.neo.ondemand.com";
    protected static final String DEFAULT_DOMAIN_XS = "sofd60245639a";
    protected static final AppSuffixDeterminer DEFAULT_APP_SUFFIX_DETERMINER = new AppSuffixDeterminer(false, false);

    protected static final String DEPLOY_ID = "123";

    protected final Tester tester = Tester.forClass(getClass());
    protected final DescriptorParser descriptorParser = getDescriptorParser();
    protected final ConfigurationParser configurationParser = new ConfigurationParser();
    protected DeploymentDescriptor deploymentDescriptor;

    protected String deploymentDescriptorLocation;
    protected String extensionDescriptorLocation;
    protected String platformLocation;
    protected String deployedMtaLocation;
    protected String namespace;
    protected boolean applyNamespace;
    private ModulesCloudModelBuilderContentCalculator modulesCalculator;
    protected ModuleToDeployHelper moduleToDeployHelper;
    protected ResourcesCloudModelBuilderContentCalculator resourcesCalculator;

    protected ApplicationCloudModelBuilder appBuilder;
    protected ServicesCloudModelBuilder servicesBuilder;

    private static Stream<Arguments> getParameters() {
        return Stream.of(
// @formatter:off
				// (01) Full MTA:
				Arguments.of("/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config.mtaext",
						"/mta/cf-platform.json", null, null, false,
						new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
						new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/services.json"),
						new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/apps.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (02)
				Arguments.of("/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/xs2-config.mtaext",
						"/mta/xs-platform.json", null, null, false,
						new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
						new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/xs2-services.json"),
						new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/xs2-apps.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (03) Full MTA with namespace:
				Arguments.of("/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config.mtaext",
						"/mta/cf-platform.json", null, "namespace1", true,
						new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
						new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/services-ns-1.json"),
						new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/apps-ns-1.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (04) Full MTA with long namespace:
				Arguments.of("/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config.mtaext",
						"/mta/cf-platform.json", null, "namespace2-but-it-is-really-really-long", true,
						new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
						new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/services-ns-2.json"),
						new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/apps-ns-2.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (05) Patch MTA (resolved inter-module dependencies):
				Arguments.of("/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config.mtaext",
						"/mta/cf-platform.json", null, null, false, new String[] { "java-hello-world" }, // mtaArchiveModules
						new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
						new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // deployedApps
						new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/services-patch.json"),
						new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/apps-patch.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (06) Patch MTA with namespaces (resolved inter-module dependencies):
				Arguments.of("/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config.mtaext",
						"/mta/cf-platform.json", null, "namespace", true, new String[] { "java-hello-world" }, // mtaArchiveModules
						new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
						new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // deployedApps
						new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/services-patch-ns.json"),
						new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/apps-patch-ns.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (07) Patch MTA (unresolved inter-module dependencies):
				Arguments.of("/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config.mtaext",
						"/mta/cf-platform.json", null, null, false, new String[] { "java-hello-world" }, // mtaArchiveModules
						new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
						new String[] { "java-hello-world", }, // deployedApps
						new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/services-patch.json"),
						new Expectation(Expectation.Type.EXCEPTION,
								"Unresolved MTA modules [java-hello-world-backend, java-hello-world-db]"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (08)
				Arguments.of("/mta/shine/mtad.yaml", "/mta/shine/config.mtaext", "/mta/cf-platform.json", null, null,
						false, new String[] { "shine", "shine-xsjs", "shine-odata" }, // mtaArchiveModules
						new String[] { "shine", "shine-xsjs", "shine-odata" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation(Expectation.Type.JSON, "/mta/shine/services.json"),
						new Expectation(Expectation.Type.JSON, "/mta/shine/apps.json"), DEFAULT_APP_SUFFIX_DETERMINER),
				// (09)
				Arguments.of("/mta/sample/mtad.yaml", "/mta/sample/config.mtaext", "/mta/sample/platform.json", null,
						null, false, new String[] { "pricing", "pricing-db", "web-server" }, // mtaArchiveModules
						new String[] { "pricing", "pricing-db", "web-server" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation(Expectation.Type.JSON, "/mta/sample/services.json"),
						new Expectation(Expectation.Type.JSON, "/mta/sample/apps.json"), DEFAULT_APP_SUFFIX_DETERMINER),
				// (10)
				Arguments.of("/mta/devxwebide/mtad.yaml", "/mta/devxwebide/config.mtaext", "/mta/cf-platform.json",
						null, null, false, new String[] { "webide" }, // mtaArchiveModules
						new String[] { "webide" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation(Expectation.Type.JSON, "/mta/devxwebide/services.json"),
						new Expectation(Expectation.Type.JSON, "/mta/devxwebide/apps.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (11)
				Arguments.of("/mta/devxwebide/mtad.yaml", "/mta/devxwebide/xs2-config-1.mtaext",
						"/mta/xs-platform.json", null, null, false, new String[] { "webide" }, // mtaArchiveModules
						new String[] { "webide" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation(Expectation.Type.JSON, "/mta/devxwebide/services.json"),
						new Expectation(Expectation.Type.JSON, "/mta/devxwebide/xs2-apps.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (12)
				Arguments.of("/mta/devxdi/mtad.yaml", "/mta/devxdi/config.mtaext", "/mta/cf-platform.json", null, null,
						false, new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
						new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation(Expectation.Type.JSON, "/mta/devxdi/services.json"),
						new Expectation(Expectation.Type.JSON, "/mta/devxdi/apps.json"), DEFAULT_APP_SUFFIX_DETERMINER),
				// (13)
				Arguments.of("/mta/devxdi/mtad.yaml", "/mta/devxdi/xs2-config-1.mtaext", "/mta/xs-platform.json", null,
						null, false, new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
						new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation(Expectation.Type.JSON, "/mta/devxdi/xs2-services.json"),
						new Expectation(Expectation.Type.JSON, "/mta/devxdi/xs2-apps.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (14)
				Arguments.of("/mta/devxwebide/mtad.yaml", "/mta/devxwebide/xs2-config-2.mtaext",
						"/mta/xs-platform.json", null, null, false, new String[] { "webide" }, // mtaArchiveModules
						new String[] { "webide" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation(Expectation.Type.JSON, "/mta/devxwebide/services.json"),
						new Expectation(Expectation.Type.JSON, "/mta/devxwebide/xs2-apps.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (15) Unknown typed resource parameters:
				Arguments.of("/mta/devxdi/mtad.yaml", "/mta/devxdi/xs2-config-2.mtaext", "/mta/xs-platform.json", null,
						null, false, new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
						new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation(Expectation.Type.JSON, "/mta/devxdi/xs2-services.json"),
						new Expectation(Expectation.Type.JSON, "/mta/devxdi/xs2-apps.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (16) Service binding parameters in requires dependency:
				Arguments.of("mtad-01.yaml", "config-01.mtaext", "/mta/cf-platform.json", null, null, false,
						new String[] { "foo", }, // mtaArchiveModules
						new String[] { "foo", }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), new Expectation(Expectation.Type.JSON, "apps-01.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (17) Service binding parameters in requires dependency:
				Arguments.of("mtad-02.yaml", "config-01.mtaext", "/mta/cf-platform.json", null, null, false,
						new String[] { "foo", }, // mtaArchiveModules
						new String[] { "foo", }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"),
						new Expectation(Expectation.Type.EXCEPTION,
								"Invalid type for key \"foo#bar#config\", expected \"Map\" but got \"String\""),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (18) Custom application names are used:
				Arguments.of("mtad-03.yaml", "config-02.mtaext", "/mta/xs-platform.json", null, null, false,
						new String[] { "module-1", "module-2" }, // mtaArchiveModules
						new String[] { "module-1", "module-2" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), new Expectation(Expectation.Type.JSON, "apps-02.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (19) Custom application names are used:
				Arguments.of("mtad-03.yaml", "config-02.mtaext", "/mta/xs-platform.json", null, "something", true,
						new String[] { "module-1", "module-2" }, // mtaArchiveModules
						new String[] { "module-1", "module-2" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), new Expectation(Expectation.Type.JSON, "apps-03.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (20) Temporary URIs are used:
				Arguments.of("mtad-05.yaml", "config-02.mtaext", "/mta/xs-platform.json", null, null, false,
						new String[] { "module-1", "module-2" }, // mtaArchiveModules
						new String[] { "module-1", "module-2" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), new Expectation(Expectation.Type.JSON, "apps-05.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (21) Use list parameter:
				Arguments.of("mtad-06.yaml", "config-02.mtaext", "/mta/xs-platform.json", null, null, false,
						new String[] { "framework" }, // mtaArchiveModules
						new String[] { "framework" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), new Expectation(Expectation.Type.JSON, "apps-06.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (22) Use partial plugin:
				Arguments.of("mtad-07.yaml", "config-02.mtaext", "/mta/xs-platform.json", null, null, false,
						new String[] { "framework" }, // mtaArchiveModules
						new String[] { "framework" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), new Expectation(Expectation.Type.JSON, "apps-07.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (23) Overwrite service-name resource property in ext. descriptor:
				Arguments.of("mtad-08.yaml", "config-03.mtaext", "/mta/xs-platform.json", null, null, false,
						new String[] { "module-1" }, // mtaArchiveModules
						new String[] { "module-1" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation(Expectation.Type.JSON, "services-03.json"),
						new Expectation(Expectation.Type.JSON, "apps-08.json"), DEFAULT_APP_SUFFIX_DETERMINER),
				// (24) Test support for one-off tasks:
				Arguments.of("mtad-09.yaml", "config-03.mtaext", "/mta/xs-platform.json", null, null, false,
						new String[] { "module-1", "module-2", "module-3", "module-4" }, // mtaArchiveModules
						new String[] { "module-1", "module-2", "module-3", "module-4" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), new Expectation(Expectation.Type.JSON, "apps-09.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (25) With 'health-check-type' set to 'port':
				Arguments.of("mtad-health-check-type-port.yaml", "config-03.mtaext", "/mta/xs-platform.json", null,
						null, false, new String[] { "foo" }, // mtaArchiveModules
						new String[] { "foo" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"),
						new Expectation(Expectation.Type.JSON, "apps-with-health-check-type-port.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (26) With 'health-check-type' set to 'http' and a non-default
				// 'health-check-http-endpoint':
				Arguments.of("mtad-health-check-type-http-with-endpoint.yaml", "config-03.mtaext",
						"/mta/xs-platform.json", null, null, false, new String[] { "foo" }, // mtaArchiveModules
						new String[] { "foo" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"),
						new Expectation(Expectation.Type.JSON, "apps-with-health-check-type-http-with-endpoint.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (27) With 'health-check-type' set to 'http' and no
				// 'health-check-http-endpoint':
				Arguments.of("mtad-health-check-type-http-without-endpoint.yaml", "config-03.mtaext",
						"/mta/xs-platform.json", null, null, false, new String[] { "foo" }, // mtaArchiveModules
						new String[] { "foo" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"),
						new Expectation(Expectation.Type.JSON,
								"apps-with-health-check-type-http-without-endpoint.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (28) Test inject service keys:
				Arguments.of("mtad-10.yaml", "config-02.mtaext", "/mta/xs-platform.json", null, null, false,
						new String[] { "module-1" }, // mtaArchiveModules
						new String[] { "module-1" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), new Expectation(Expectation.Type.JSON, "apps-10.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (29) With 'enable-ssh' set to true:
				Arguments.of("mtad-ssh-enabled-true.yaml", "config-02.mtaext", "/mta/xs-platform.json", null, null,
						false, new String[] { "foo" }, // mtaArchiveModules
						new String[] { "foo" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"),
						new Expectation(Expectation.Type.JSON, "apps-with-ssh-enabled-true.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (30) With 'enable-ssh' set to false:
				Arguments.of("mtad-ssh-enabled-false.yaml", "config-02.mtaext", "/mta/xs-platform.json", null, null,
						false, new String[] { "foo" }, // mtaArchiveModules
						new String[] { "foo" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"),
						new Expectation(Expectation.Type.JSON, "apps-with-ssh-enabled-false.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (31) Do not restart on env change - bg-deploy
				Arguments.of("mtad-restart-on-env-change.yaml", "config-02.mtaext", "/mta/xs-platform.json", null, null,
						false, new String[] { "module-1", "module-2", "module-3" }, // mtaArchiveModules
						new String[] { "module-1", "module-2", "module-3" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"),
						new Expectation(Expectation.Type.JSON, "apps-with-restart-parameters-false.json") // services
						, DEFAULT_APP_SUFFIX_DETERMINER),
				// (32) With 'keep-existing-routes' set to true and no deployed MTA:
				Arguments.of("keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/xs-platform.json", null, null,
						false, new String[] { "foo" }, // mtaArchiveModules
						new String[] { "foo" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), new Expectation(Expectation.Type.JSON, "keep-existing-routes/apps.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (33) With 'keep-existing-routes' set to true and no deployed module:
				Arguments.of("keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/xs-platform.json",
						"keep-existing-routes/deployed-mta-without-foo-module.json", null, false,
						new String[] { "foo" }, // mtaArchiveModules
						new String[] { "foo" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), new Expectation(Expectation.Type.JSON, "keep-existing-routes/apps.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (34) With 'keep-existing-routes' set to true and an already deployed module
				// with no URIs:
				Arguments.of("keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/xs-platform.json",
						"keep-existing-routes/deployed-mta-without-routes.json", null, false, new String[] { "foo" }, // mtaArchiveModules
						new String[] { "foo" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), new Expectation(Expectation.Type.JSON, "keep-existing-routes/apps.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (35) With 'keep-existing-routes' set to true and an already deployed module:
				Arguments.of("keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/xs-platform.json",
						"keep-existing-routes/deployed-mta.json", null, false, new String[] { "foo" }, // mtaArchiveModules
						new String[] { "foo" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"),
						new Expectation(Expectation.Type.JSON, "keep-existing-routes/apps-with-existing-routes.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (36) With global 'keep-existing-routes' set to true and an already deployed
				// module:
				Arguments.of("keep-existing-routes/mtad-with-global-parameter.yaml", "config-02.mtaext",
						"/mta/xs-platform.json", "keep-existing-routes/deployed-mta.json", null, false,
						new String[] { "foo" }, // mtaArchiveModules
						new String[] { "foo" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"),
						new Expectation(Expectation.Type.JSON, "keep-existing-routes/apps-with-existing-routes.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (37) With new parameter - 'route'
				Arguments.of("mtad-12.yaml", "config-01.mtaext", "/mta/cf-platform.json", null, null, false,
						new String[] { "foo", }, // mtaArchiveModules
						new String[] { "foo", }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), // services
						new Expectation(Expectation.Type.JSON, "apps-12.json") // applications
						, DEFAULT_APP_SUFFIX_DETERMINER),
				// (38) With new parameter - 'routes'
				Arguments.of("mtad-13.yaml", "config-01.mtaext", "/mta/cf-platform.json", null, null, false,
						new String[] { "foo", }, // mtaArchiveModules
						new String[] { "foo", }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), // services
						new Expectation(Expectation.Type.JSON, "apps-13.json") // applications
						, DEFAULT_APP_SUFFIX_DETERMINER),
				// (39) Test plural priority over singular for hosts and domains
				Arguments.of("mtad-14.yaml", "config-01.mtaext", "/mta/cf-platform.json", null, null, false,
						new String[] { "foo", }, // mtaArchiveModules
						new String[] { "foo", }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), // services
						new Expectation(Expectation.Type.JSON, "apps-14.json") // applications
						, DEFAULT_APP_SUFFIX_DETERMINER),
				// (40) Test multiple buildpacks functionality
				Arguments.of("mtad-15.yaml", "config-01.mtaext", "/mta/cf-platform.json", null, null, false,
						new String[] { "foo", }, // mtaArchiveModules
						new String[] { "foo", }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), // services
						new Expectation(Expectation.Type.JSON, "apps-15.json") // applications
						, DEFAULT_APP_SUFFIX_DETERMINER),
				// (41) Full MTA with namespace, global apply flag set to false:
				Arguments.of("/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config.mtaext",
						"/mta/cf-platform.json", null, "namespace3", false,
						new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
						new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/services-ns-3.json"),
						new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/apps-ns-3.json"),
						DEFAULT_APP_SUFFIX_DETERMINER),
				// (42) Test app-name parameter resolution:
				Arguments.of("mtad-16.yaml", "config-01.mtaext", "/mta/cf-platform.json", null, null, false,
						new String[] { "foo", }, // mtaArchiveModules
						new String[] { "foo", }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), // services
						new Expectation(Expectation.Type.JSON, "apps-16.json") // applications
						, new AppSuffixDeterminer(true, true)),
				// (43) With hostless routes
				Arguments.of("mtad-routes-with-nohostname.yaml", "config-01.mtaext", "/mta/cf-platform.json", null,
						null, false, new String[] { "foo", }, // mtaArchiveModules
						new String[] { "foo", }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), // services
						new Expectation(Expectation.Type.JSON, "apps-with-nohostname.json") // applications
						, DEFAULT_APP_SUFFIX_DETERMINER),
				// (44) With hostless routes and existing mta
				Arguments.of("keep-existing-routes/mtad-routes-with-nohostname.yaml", "config-01.mtaext", "/mta/cf-platform.json",
						"keep-existing-routes/deployed-mta.json", null, false, new String[] { "foo", }, // mtaArchiveModules
						new String[] { "foo", }, // mtaModules
						new String[] {}, // deployedApps
						new Expectation("[]"), // services
						new Expectation(Expectation.Type.JSON, "keep-existing-routes/apps-with-nohostname.json") // applications
						, DEFAULT_APP_SUFFIX_DETERMINER)
// @formatter:on
        );
    }

    protected UserMessageLogger getUserMessageLogger() {
        return null;
    }

    protected DescriptorParser getDescriptorParser() {
        return getHandlerFactory().getDescriptorParser();
    }

    protected CloudHandlerFactory getHandlerFactory() {
        return CloudHandlerFactory.forSchemaVersion(2);
    }

    protected Map<String, Object> getParameters(Module module) {
        return module.getParameters();
    }

    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor) {
        return new ServicesCloudModelBuilder(deploymentDescriptor, namespace);
    }

    protected ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
                                                                           boolean prettyPrinting, DeployedMta deployedMta,
                                                                           AppSuffixDeterminer appSuffixDeterminer) {
        deploymentDescriptor = new DescriptorReferenceResolver(deploymentDescriptor,
                                                               new ResolverBuilder(),
                                                               new ResolverBuilder()).resolve();
        return new ApplicationCloudModelBuilder.Builder().deploymentDescriptor(deploymentDescriptor)
                                                         .prettyPrinting(prettyPrinting)
                                                         .deployedMta(deployedMta)
                                                         .deployId(DEPLOY_ID)
                                                         .namespace(namespace)
                                                         .userMessageLogger(Mockito.mock(UserMessageLogger.class))
                                                         .appSuffixDeterminer(appSuffixDeterminer)
                                                         .build();
    }

    protected PlatformMerger getPlatformMerger(Platform platform) {
        return getHandlerFactory().getPlatformMerger(platform);
    }

    protected DescriptorMerger getDescriptorMerger() {
        return new DescriptorMerger();
    }

    private DeploymentDescriptor loadDeploymentDescriptor() {
        InputStream deploymentDescriptorYaml = getClass().getResourceAsStream(deploymentDescriptorLocation);
        Map<String, Object> deploymentDescriptorMap = new YamlParser().convertYamlToMap(deploymentDescriptorYaml);
        return descriptorParser.parseDeploymentDescriptor(deploymentDescriptorMap);
    }

    private ExtensionDescriptor loadExtensionDescriptor() {
        InputStream extensionDescriptorYaml = getClass().getResourceAsStream(extensionDescriptorLocation);
        Map<String, Object> extensionDescriptorMap = new YamlParser().convertYamlToMap(extensionDescriptorYaml);
        return descriptorParser.parseExtensionDescriptor(extensionDescriptorMap);
    }

    private Platform loadPlatform() {
        InputStream platformJson = getClass().getResourceAsStream(platformLocation);
        return configurationParser.parsePlatformJson(platformJson);
    }

    private DeployedMta loadDeployedMta() throws IOException {
        if (deployedMtaLocation == null) {
            return null;
        }
        InputStream deployedMtaStream = getClass().getResourceAsStream(deployedMtaLocation);
        String deployedMtaJson = IOUtils.toString(deployedMtaStream, StandardCharsets.UTF_8);
        return JsonUtil.fromJson(deployedMtaJson, DeployedMta.class);
    }

    protected void insertProperNames(DeploymentDescriptor descriptor) {
        insertProperAppNames(descriptor);
        insertProperServiceNames(descriptor);
    }

    private void insertProperAppNames(DeploymentDescriptor descriptor) {
        for (Module module : descriptor.getModules()) {
            String appName = computeAppName(module);
            Map<String, Object> parameters = new TreeMap<>(module.getParameters());
            parameters.put(SupportedParameters.APP_NAME, appName);
            module.setParameters(parameters);
        }
    }

    private void insertProperServiceNames(DeploymentDescriptor descriptor) {
        for (Resource resource : descriptor.getResources()) {
            String serviceName = computeServiceName(resource);
            Map<String, Object> parameters = new TreeMap<>(resource.getParameters());
            parameters.put(SupportedParameters.SERVICE_NAME, serviceName);
            resource.setParameters(parameters);
        }
    }

    private String computeAppName(Module module) {
        String appName = NameUtil.getApplicationName(module);
        appName = appName != null ? appName : module.getName();
        return NameUtil.computeValidApplicationName(appName, namespace, applyNamespace);
    }

    private String computeServiceName(Resource resource) {
        String serviceName = NameUtil.getServiceName(resource);
        serviceName = serviceName != null ? serviceName : resource.getName();
        return NameUtil.computeValidServiceName(serviceName, namespace, applyNamespace);
    }

    protected String getDefaultDomain(String targetName) {
        return targetName.equals("CLOUD-FOUNDRY") ? DEFAULT_DOMAIN_CF : DEFAULT_DOMAIN_XS;
    }

    protected void injectSystemParameters(DeploymentDescriptor descriptor, String defaultDomain) {
        Map<String, Object> generalSystemParameters = Map.of(SupportedParameters.DEFAULT_DOMAIN, defaultDomain);
        descriptor.setParameters(MapUtil.merge(generalSystemParameters, descriptor.getParameters()));
        for (Module module : descriptor.getModules()) {
            Map<String, Object> moduleSystemParameters = Map.of(SupportedParameters.DEFAULT_HOST, module.getName());
            module.setParameters(MapUtil.merge(moduleSystemParameters, module.getParameters()));
        }
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testGetApplications(String deploymentDescriptorLocation, String extensionDescriptorLocation, String platformsLocation,
                             String deployedMtaLocation, String namespace, boolean applyNamespace, String[] mtaArchiveModules,
                             String[] mtaModules, String[] deployedApps, Expectation expectedServices, Expectation expectedApps,
                             AppSuffixDeterminer appSuffixDeterminer)
        throws Exception {
        initializeParameters(deploymentDescriptorLocation, extensionDescriptorLocation, platformsLocation, deployedMtaLocation, namespace,
                             applyNamespace, mtaArchiveModules, mtaModules, deployedApps, appSuffixDeterminer);
        tester.test(() -> modulesCalculator.calculateContentForBuilding(deploymentDescriptor.getModules())
                                           .stream()
                                           .map(module -> appBuilder.build(module, moduleToDeployHelper))
                                           .collect(Collectors.toList()),
                    expectedApps);
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testGetServices(String deploymentDescriptorLocation, String extensionDescriptorLocation, String platformsLocation,
                         String deployedMtaLocation, String namespace, boolean applyNamespace, String[] mtaArchiveModules,
                         String[] mtaModules, String[] deployedApps, Expectation expectedServices, Expectation expectedApps,
                         AppSuffixDeterminer appSuffixDeterminer)
        throws Exception {
        initializeParameters(deploymentDescriptorLocation, extensionDescriptorLocation, platformsLocation, deployedMtaLocation, namespace,
                             applyNamespace, mtaArchiveModules, mtaModules, deployedApps, appSuffixDeterminer);
        tester.test(() -> servicesBuilder.build(resourcesCalculator.calculateContentForBuilding(deploymentDescriptor.getResources())),
                    expectedServices);
    }

    protected void initializeParameters(String deploymentDescriptorLocation, String extensionDescriptorLocation, String platformsLocation,
                                        String deployedMtaLocation, String namespace, boolean applyNamespace, String[] mtaArchiveModules,
                                        String[] mtaModules, String[] deployedApps, AppSuffixDeterminer appSuffixDeterminer)
        throws Exception {
        this.deploymentDescriptorLocation = deploymentDescriptorLocation;
        this.extensionDescriptorLocation = extensionDescriptorLocation;
        this.platformLocation = platformsLocation;
        this.deployedMtaLocation = deployedMtaLocation;
        this.namespace = namespace;
        this.applyNamespace = applyNamespace;
        deploymentDescriptor = loadDeploymentDescriptor();
        ExtensionDescriptor extensionDescriptor = loadExtensionDescriptor();
        Platform platform = loadPlatform();
        DeployedMta deployedMta = loadDeployedMta();
        deploymentDescriptor = getDescriptorMerger().merge(deploymentDescriptor, List.of(extensionDescriptor));
        PlatformMerger platformMerger = getPlatformMerger(platform);
        platformMerger.mergeInto(deploymentDescriptor);
        String defaultDomain = getDefaultDomain(platform.getName());
        insertProperNames(deploymentDescriptor);
        injectSystemParameters(deploymentDescriptor, defaultDomain);
        appBuilder = getApplicationCloudModelBuilder(deploymentDescriptor, false, deployedMta, appSuffixDeterminer);
        servicesBuilder = getServicesCloudModelBuilder(deploymentDescriptor);
        modulesCalculator = getModulesCalculator(Set.of(mtaArchiveModules), Set.of(mtaModules), Set.of(deployedApps));
        moduleToDeployHelper = new ModuleToDeployHelper();
        resourcesCalculator = new ResourcesCloudModelBuilderContentCalculator(null, getUserMessageLogger());
    }

    private ModulesCloudModelBuilderContentCalculator getModulesCalculator(Set<String> mtaArchiveModules, Set<String> mtaModules,
                                                                           Set<String> deployedApps) {
        return new ModulesCloudModelBuilderContentCalculator(mtaArchiveModules,
                                                             deployedApps,
                                                             null,
                                                             getUserMessageLogger(),
                                                             new ModuleToDeployHelper(),
                                                             List.of(new UnresolvedModulesContentValidator(mtaModules, deployedApps)));
    }

}
