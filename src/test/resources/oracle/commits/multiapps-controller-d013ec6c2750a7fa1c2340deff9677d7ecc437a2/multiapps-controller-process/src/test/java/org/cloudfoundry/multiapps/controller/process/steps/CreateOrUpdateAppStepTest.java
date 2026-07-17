package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ServiceKeyToInject;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;
import com.sap.cloudfoundry.client.facade.domain.DockerInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDockerCredentials;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDockerInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableStaging;
import com.sap.cloudfoundry.client.facade.domain.Staging;

class CreateOrUpdateAppStepTest extends SyncFlowableStepTest<CreateOrUpdateAppStep> {

    private static final String APP_NAME = "test-application";
    private static final String SERVICE_NAME = "test-service";
    private static final String SERVICE_KEY_NAME = "test-service-key";
    private static final String SERVICE_KEY_ENV_NAME = "test-service-key-env";

    static Stream<Arguments> testHandleApplicationAttributes() {
        return Stream.of(
//@formatter:off
                         // (1) Everything is specified properly:
                         Arguments.of(ImmutableStaging.builder().command("command1").healthCheckType("none").addBuildpack("buildpackUrl").build(),
                                      128, 256, TestData.routeSummarySet("example.com", "foo-bar.xyz")),
                         // (2) Disk quota is 0:
                         Arguments.of(null, 0, 256, Collections.emptySet()),
                         // (3) Memory is 0:
                         Arguments.of(null, 1024, 0, Collections.emptySet())
//@formatter:on             
        );
    }

    @ParameterizedTest
    @MethodSource
    void testHandleApplicationAttributes(Staging staging, int diskQuota, int memory, Set<CloudRouteSummary> routes) {
        CloudApplicationExtended application = buildApplication(staging, diskQuota, memory, routes);
        prepareContext(application, Collections.emptyMap());

        step.execute(execution);

        assertStepFinishedSuccessfully();
        Integer expectedDiskQuota = diskQuota == 0 ? null : diskQuota;
        Integer expectedMemory = memory == 0 ? null : memory;
        verify(client).createApplication(APP_NAME, staging, expectedDiskQuota, expectedMemory, routes, null);
        assertTrue(context.getVariable(Variables.VCAP_APP_PROPERTIES_CHANGED));
    }

    private CloudApplicationExtended buildApplication(Staging staging, int diskQuota, int memory, Set<CloudRouteSummary> routes) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(APP_NAME)
                                                .staging(staging)
                                                .diskQuota(diskQuota)
                                                .memory(memory)
                                                .routes(routes)
                                                .build();
    }

    private void prepareContext(CloudApplicationExtended application, Map<String, Map<String, String>> serviceKeysCredentialsToInject) {
        context.setVariable(Variables.APP_TO_PROCESS, application);
        context.setVariable(Variables.SERVICE_KEYS_CREDENTIALS_TO_INJECT, serviceKeysCredentialsToInject);
    }

    @Test
    void testCreateApplicationFromDockerImage() {
        DockerInfo dockerInfo = ImmutableDockerInfo.builder()
                                                   .image("cloudfoundry/test-app")
                                                   .credentials(ImmutableDockerCredentials.builder()
                                                                                          .username("someUser")
                                                                                          .password("somePassword")
                                                                                          .build())
                                                   .build();

        CloudApplicationExtended application = buildApplication(null, 128, 256, Collections.emptySet());
        application = ImmutableCloudApplicationExtended.copyOf(application)
                                                       .withDockerInfo(dockerInfo);
        prepareContext(application, Collections.emptyMap());

        step.execute(execution);

        assertStepFinishedSuccessfully();
        verify(client).createApplication(APP_NAME, null, 128, 256, Collections.emptySet(), dockerInfo);
        verify(stepLogger).info(Messages.CREATING_APP_FROM_DOCKER_IMAGE, APP_NAME, dockerInfo.getImage());
    }

    @Test
    void testHandleApplicationServices() {
        CloudApplicationExtended application = buildApplication(null, 0, 0, Collections.emptySet());
        List<String> services = List.of("service-1", "service-2");
        application = ImmutableCloudApplicationExtended.copyOf(application)
                                                       .withServices(services);
        prepareContext(application, Collections.emptyMap());

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertTrue(services.containsAll(context.getVariable(Variables.SERVICES_TO_UNBIND_BIND)));
    }

    @Test
    void testHandleApplicationEnv() {
        CloudApplicationExtended application = buildApplication(null, 0, 0, Collections.emptySet());
        Map<String, String> applicationEnv = Map.of("restart-policy", "always");
        application = ImmutableCloudApplicationExtended.copyOf(application)
                                                       .withEnv(applicationEnv);
        prepareContext(application, Collections.emptyMap());

        step.execute(execution);

        assertStepFinishedSuccessfully();
        verify(client).updateApplicationEnv(APP_NAME, applicationEnv);
        assertTrue(context.getVariable(Variables.USER_PROPERTIES_CHANGED));
    }

    @Test
    void testInjectServiceKeysCredentialsInAppEnv() {
        CloudApplicationExtended application = buildApplication(null, 0, 0, Collections.emptySet());
        Map<String, String> applicationEnv = Map.of("restart-policy", "always");
        ServiceKeyToInject serviceKey = new ServiceKeyToInject(SERVICE_KEY_ENV_NAME, SERVICE_NAME, SERVICE_KEY_NAME);
        application = ImmutableCloudApplicationExtended.copyOf(application)
                                                       .withEnv(applicationEnv)
                                                       .withServiceKeysToInject(serviceKey);
        Map<String, String> serviceKeyCredentials = Map.of("user", "service-key-user", "password", "service-key-password");
        when(client.getServiceKeys(SERVICE_NAME)).thenReturn(List.of(ImmutableCloudServiceKey.builder()
                                                                                             .name(SERVICE_KEY_NAME)
                                                                                             .credentials(serviceKeyCredentials)
                                                                                             .build()));
        Map<String, Map<String, String>> serviceKeysToInjectCredentials = Map.of(APP_NAME, serviceKeyCredentials);
        prepareContext(application, serviceKeysToInjectCredentials);

        step.shouldPrettyPrint = () -> false;
        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(JsonUtil.toJson(serviceKeyCredentials), context.getVariable(Variables.APP_TO_PROCESS)
                                                                    .getEnv()
                                                                    .get(serviceKey.getEnvVarName()));
        assertEquals(Map.of(SERVICE_KEY_ENV_NAME, JsonUtil.toJson(serviceKeyCredentials)),
                     context.getVariable(Variables.SERVICE_KEYS_CREDENTIALS_TO_INJECT)
                            .get(APP_NAME));
    }

    @Test
    void testThrowExceptionWhenSpecifiedServiceKeyNotExist() {
        CloudApplicationExtended application = buildApplication(null, 0, 0, Collections.emptySet());
        Map<String, String> applicationEnv = Map.of("restart-policy", "always");
        ServiceKeyToInject serviceKey = new ServiceKeyToInject(SERVICE_KEY_ENV_NAME, SERVICE_NAME, SERVICE_KEY_NAME);
        application = ImmutableCloudApplicationExtended.copyOf(application)
                                                       .withEnv(applicationEnv)
                                                       .withServiceKeysToInject(serviceKey);
        when(client.getServiceKeys(SERVICE_NAME)).thenReturn(Collections.emptyList());
        prepareContext(application, Collections.emptyMap());

        assertThrows(SLException.class, () -> step.execute(execution));
    }

    @Override
    protected CreateOrUpdateAppStep createStep() {
        return new CreateOrUpdateAppStep();
    }

}
