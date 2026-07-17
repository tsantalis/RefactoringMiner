package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDockerInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableStaging;
import com.sap.cloudfoundry.client.facade.domain.Staging;
import com.sap.cloudfoundry.client.facade.util.JsonUtil;

class CreateOrUpdateStepWithExistingAppTest extends SyncFlowableStepTest<CreateOrUpdateAppStep> {

    private static final String APP_DIGEST = "12345";
    private static final String APP_NAME_ENV_KEY = "APP_NAME";
    private static final String APP_NAME = "test-application";

    static Stream<Arguments> testHandleStagingApplicationAttributes() {
        return Stream.of(
//@formatter:off
                         Arguments.of(ImmutableStaging.builder().addBuildpack("buildpack-1").command("command1").build(),
                                      ImmutableStaging.builder().addBuildpack("buildpack-1").command("command2").build(),
                                      true),
                         Arguments.of(ImmutableStaging.builder().addBuildpack("buildpack-1").build(),
                                      ImmutableStaging.builder().addBuildpack("buildpack-1").build(),
                                      false),
                         Arguments.of(ImmutableStaging.builder().addBuildpack("buildpack-1").command("command1").stack("stack1").
                                      healthCheckTimeout(5).healthCheckType("process").isSshEnabled(false)
                                                      .build(),
                                      ImmutableStaging.builder().addBuildpack("buildpack-2").command("command2").stack("stack2")
                                                      .healthCheckTimeout(10).healthCheckType("web").healthCheckHttpEndpoint("/test")
                                                      .isSshEnabled(true)
                                                      .build(),
                                      true),
                         Arguments.of(ImmutableStaging.builder().addBuildpack("buildpack-2").command("command2").stack("stack2")
                                                      .healthCheckTimeout(10).healthCheckType("web").healthCheckHttpEndpoint("/test")
                                                      .isSshEnabled(true)
                                                      .build(),
                                      ImmutableStaging.builder().addBuildpack("buildpack-2").command("command2").stack("stack2")
                                                      .healthCheckTimeout(10).healthCheckType("web").healthCheckHttpEndpoint("/test")
                                                      .isSshEnabled(true)
                                                      .build(),
                                      false),
                         Arguments.of(ImmutableStaging.builder().dockerInfo(ImmutableDockerInfo.builder().image("cloudfoundry/test-app").build()).build(),
                                      ImmutableStaging.builder().dockerInfo(ImmutableDockerInfo.builder().image("cloudfoundry/test-app2").build()).build(),
                                      true),
                         Arguments.of(ImmutableStaging.builder().dockerInfo(ImmutableDockerInfo.builder().image("cloudfoundry/test-app").build()).build(),
                                      ImmutableStaging.builder().dockerInfo(ImmutableDockerInfo.builder().image("cloudfoundry/test-app").build()).build(),
                                      false));
//@formatter:on
    }

    @ParameterizedTest
    @MethodSource
    void testHandleStagingApplicationAttributes(Staging existingStaging, Staging staging, boolean expectedPropertiesChanged) {
        CloudApplication existingApplication = getApplicationBuilder(false).staging(existingStaging)
                                                                           .build();
        CloudApplicationExtended application = getApplicationBuilder(false).staging(staging)
                                                                           .build();
        prepareContext(application, false);
        prepareClient(existingApplication);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(expectedPropertiesChanged, context.getVariable(Variables.VCAP_APP_PROPERTIES_CHANGED));
        if (expectedPropertiesChanged) {
            verify(client).updateApplicationStaging(APP_NAME, staging);
            return;
        }
        verify(client, never()).updateApplicationStaging(eq(APP_NAME), any());
    }

    private ImmutableCloudApplicationExtended.Builder getApplicationBuilder(boolean shouldKeepExistingEnv) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(APP_NAME)
                                                .staging(ImmutableStaging.builder()
                                                                         .build())
                                                .attributesUpdateStrategy(ImmutableCloudApplicationExtended.AttributeUpdateStrategy.builder()
                                                                                                                                   .shouldKeepExistingEnv(shouldKeepExistingEnv)
                                                                                                                                   .build());
    }

    private void prepareContext(CloudApplicationExtended application, boolean shouldSkipServiceRebinding) {
        context.setVariable(Variables.APP_TO_PROCESS, application);
        context.setVariable(Variables.SERVICE_KEYS_CREDENTIALS_TO_INJECT, Collections.emptyMap());
        context.setVariable(Variables.SHOULD_SKIP_SERVICE_REBINDING, shouldSkipServiceRebinding);
    }

    private void prepareClient(CloudApplication application) {
        when(client.getApplication(APP_NAME, false)).thenReturn(application);
    }

    static Stream<Arguments> testHandleMemoryApplicationAttributes() {
        return Stream.of(Arguments.of(128, 256, true), Arguments.of(512, 128, true), Arguments.of(1024, 0, false),
                         Arguments.of(1024, 1024, false));
    }

    @ParameterizedTest
    @MethodSource
    void testHandleMemoryApplicationAttributes(int existingMemorySize, int memorySize, boolean expectedPropertiesChanged) {
        CloudApplication existingApplication = getApplicationBuilder(false).memory(existingMemorySize)
                                                                           .build();
        CloudApplicationExtended application = getApplicationBuilder(false).memory(memorySize)
                                                                           .build();
        prepareContext(application, false);
        prepareClient(existingApplication);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(expectedPropertiesChanged, context.getVariable(Variables.VCAP_APP_PROPERTIES_CHANGED));
        if (expectedPropertiesChanged) {
            verify(client).updateApplicationMemory(APP_NAME, memorySize);
            return;
        }
        verify(client, never()).updateApplicationMemory(eq(APP_NAME), anyInt());
    }

    static Stream<Arguments> testHandleDiskQuotaApplicationAttributes() {
        return Stream.of(Arguments.of(128, 256, true), Arguments.of(512, 128, true), Arguments.of(1024, 0, false),
                         Arguments.of(1024, 1024, false));
    }

    @ParameterizedTest
    @MethodSource
    void testHandleDiskQuotaApplicationAttributes(int existingDiskQuotaSize, int diskQuotaSize, boolean expectedPropertiesChanged) {
        CloudApplication existingApplication = getApplicationBuilder(false).diskQuota(existingDiskQuotaSize)
                                                                           .build();
        CloudApplicationExtended application = getApplicationBuilder(false).diskQuota(diskQuotaSize)
                                                                           .build();
        prepareContext(application, false);
        prepareClient(existingApplication);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(expectedPropertiesChanged, context.getVariable(Variables.VCAP_APP_PROPERTIES_CHANGED));
        if (expectedPropertiesChanged) {
            verify(client).updateApplicationDiskQuota(APP_NAME, diskQuotaSize);
            return;
        }
        verify(client, never()).updateApplicationDiskQuota(eq(APP_NAME), anyInt());
    }

    static Stream<Arguments> testHandleUrisApplicationAttributes() {
        return Stream.of(Arguments.of(Collections.emptyList(), List.of("example.com"), true),
                         Arguments.of(List.of("example.com"), Collections.emptyList(), true),
                         Arguments.of(List.of("example.com"), List.of("example.com", "example1.com"), true),
                         Arguments.of(List.of("example.com"), List.of("example.com"), false));
    }

    @ParameterizedTest
    @MethodSource
    void testHandleUrisApplicationAttributes(List<String> existingUris, List<String> uris, boolean expectedPropertiesChanged) {
        CloudApplication existingApplication = getApplicationBuilder(false).uris(existingUris)
                                                                           .build();
        CloudApplicationExtended application = getApplicationBuilder(false).uris(uris)
                                                                           .build();
        prepareContext(application, false);
        prepareClient(existingApplication);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(expectedPropertiesChanged, context.getVariable(Variables.VCAP_APP_PROPERTIES_CHANGED));
        if (expectedPropertiesChanged) {
            verify(client).updateApplicationUris(APP_NAME, uris);
            return;
        }
        verify(client, never()).updateApplicationUris(eq(APP_NAME), anyList());
    }

    static Stream<Arguments> testHandleApplicationServices() {
        return Stream.of(Arguments.of(List.of("service-1"), List.of("service-1", "service-2"), false, List.of("service-1", "service-2")),
                         Arguments.of(Collections.emptyList(), List.of("service-1"), false, List.of("service-1")),
                         Arguments.of(List.of("service-1", "service-2"), Collections.emptyList(), true, null),
                         Arguments.of(List.of("service-1"), List.of("service-2"), false, List.of("service-1", "service-2")),
                         Arguments.of(List.of("service-1"), Collections.emptyList(), false, List.of("service-1")),
                         Arguments.of(Collections.emptyList(), List.of("service-1", "service-2"), true, null));
    }

    @ParameterizedTest
    @MethodSource
    void testHandleApplicationServices(List<String> existingServices, List<String> services, boolean shouldSkipServiceRebinding,
                                       List<String> expectedServicestoUpdate) {
        CloudApplication existingApplication = getApplicationBuilder(false).services(existingServices)
                                                                           .build();
        CloudApplicationExtended application = getApplicationBuilder(false).services(services)
                                                                           .build();
        prepareContext(application, shouldSkipServiceRebinding);
        prepareClient(existingApplication);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        if (shouldSkipServiceRebinding) {
            assertTrue(context.getVariable(Variables.SERVICES_TO_UNBIND_BIND)
                              .isEmpty());
            return;
        }
        assertTrue(expectedServicestoUpdate.containsAll(context.getVariable(Variables.SERVICES_TO_UNBIND_BIND)));
    }

    static Stream<Arguments> testHandleApplicationEnv() {
        return Stream.of(Arguments.of(Map.of("foo", "bar"), Map.of("foo1", "bar2"), true, true),
                         Arguments.of(Map.of("foo", "bar"), Map.of("foo1", "bar2"), false, true),
                         Arguments.of(Map.of("foo", "bar"), Map.of("foo", "bar"), true, false),
                         Arguments.of(Map.of("foo", "bar"), Map.of("foo", "bar"), false, false));
    }

    @ParameterizedTest
    @MethodSource
    void testHandleApplicationEnv(Map<String, String> existingAppEnv, Map<String, String> newAppEnv, boolean keepExistingEnv,
                                  boolean expectedUserPropertiesChanged) {
        CloudApplication existingApplication = getApplicationBuilder(keepExistingEnv).env(existingAppEnv)
                                                                                     .build();
        CloudApplicationExtended application = getApplicationBuilder(keepExistingEnv).env(newAppEnv)
                                                                                     .build();
        prepareContext(application, false);
        prepareClient(existingApplication);

        step.shouldPrettyPrint = () -> false;
        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(expectedUserPropertiesChanged, context.getVariable(Variables.USER_PROPERTIES_CHANGED));
        if (expectedUserPropertiesChanged) {
            Map<String, String> expectedEnvMap = buildExpectedEnvMap(existingAppEnv, newAppEnv, keepExistingEnv);
            verify(client).updateApplicationEnv(eq(APP_NAME), eq(expectedEnvMap));
            return;
        }
        verify(client, never()).updateApplicationEnv(eq(APP_NAME), anyMap());
    }

    private Map<String, String> buildExpectedEnvMap(Map<String, String> existingAppEnv, Map<String, String> newAppEnv,
                                                    boolean keepExistingEnv) {
        if (!keepExistingEnv) {
            return newAppEnv;
        }
        Map<String, String> expectedEnvMap = new HashMap<>(existingAppEnv);
        expectedEnvMap.putAll(newAppEnv);
        return expectedEnvMap;
    }

    @Test
    void testAddExistingAppDigestToNewEnv() {
        String applicationDigestJsonEnv = JsonUtil.convertToJson(Map.of(Constants.ATTR_APP_CONTENT_DIGEST, APP_DIGEST));
        CloudApplication existingApplication = getApplicationBuilder(false).env(Map.of(Constants.ENV_DEPLOY_ATTRIBUTES,
                                                                                       applicationDigestJsonEnv))
                                                                           .build();
        Map<String, String> newApplicationEnv = Map.of(APP_NAME_ENV_KEY, APP_NAME);
        CloudApplicationExtended application = getApplicationBuilder(false).env(newApplicationEnv)
                                                                           .build();
        prepareContext(application, false);
        prepareClient(existingApplication);

        step.shouldPrettyPrint = () -> false;
        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(true, context.getVariable(Variables.USER_PROPERTIES_CHANGED));

        Map<String, String> expectedEnv = buildExpectedEnvWithDeployAttributes(newApplicationEnv, applicationDigestJsonEnv);
        verify(client).updateApplicationEnv(eq(APP_NAME), eq(expectedEnv));
    }

    private Map<String, String> buildExpectedEnvWithDeployAttributes(Map<String, String> newApplicationEnv,
                                                                     String applicationDigestJsonEnv) {
        Map<String, String> expectedEnvMap = new HashMap<>(newApplicationEnv);
        expectedEnvMap.put(Constants.ENV_DEPLOY_ATTRIBUTES, applicationDigestJsonEnv);
        return expectedEnvMap;
    }

    @Override
    protected CreateOrUpdateAppStep createStep() {
        return new CreateOrUpdateAppStep();
    }

}
