package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;

class DeleteIdleRoutesStepTest extends SyncFlowableStepTest<DeleteIdleRoutesStep> {

    static Stream<Arguments> testExecute() {
        return Stream.of(
                         // (1) One old route is replaced with a new one in redeploy:
                         Arguments.of("existing-app-1.json", "app-to-deploy-1.json",
                                      TestData.routeSummarySet("module-1.domain.com", "module-1.domain.com/with/path"), null, StepPhase.DONE),
                         // (2) There are no differences between old and new route:
                         Arguments.of("existing-app-2.json", "app-to-deploy-2.json", Collections.emptySet(), null, StepPhase.DONE),
                         // (3) The new URIs are a subset of the old:
                         Arguments.of("existing-app-3.json", "app-to-deploy-3.json",
                                      TestData.routeSummarySet("test.domain.com/51052", "test.domain.com/51054"), null, StepPhase.DONE),
                         // (4) There is no previous version of app:
                         Arguments.of(null, "app-to-deploy-3.json", Collections.emptySet(), null, StepPhase.DONE),
                         // (5) Not Found Exception is thrown
                         Arguments.of("existing-app-1.json", "app-to-deploy-1.json",
                                      TestData.routeSummarySet("module-1.domain.com", "module-1.domain.com/with/path"),
                                      new CloudOperationException(HttpStatus.NOT_FOUND), StepPhase.DONE),
                         // (6) Conflict Exception is thrown
                         Arguments.of("existing-app-1.json", "app-to-deploy-1.json",
                                      TestData.routeSummarySet("module-1.domain.com", "module-1.domain.com/with/path"),
                                      new CloudOperationException(HttpStatus.CONFLICT), StepPhase.DONE),
                         // (7) No-Hostname: There are no differences between old and new routes:
                         Arguments.of("existing-app-4.json", "app-to-deploy-4.json", Collections.emptySet(), null, StepPhase.DONE),
                         // (8) No-Hostname: The new routes are a subset of the old:
                         Arguments.of("existing-app-5.json", "app-to-deploy-4.json",
                                      TestData.routeSummarySet(TestData.NOHOSTNAME_URI_FLAG + "testdomain.com", "bar.testdomain.com/another/path"), null,
                                      StepPhase.DONE));
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(String existingAppFile, String appToDeployFile, Set<CloudRouteSummary> routesToDelete,
                     CloudOperationException exceptionThrownByClient, StepPhase expectedStepPhase) {
        prepareContext(existingAppFile, appToDeployFile, exceptionThrownByClient);
        step.execute(execution);
        assertStepPhaseMatch(expectedStepPhase);
        verifyClient(routesToDelete);
    }

    private void prepareContext(String existingAppFile, String appToDeployFile, CloudOperationException exceptionThrownByClient) {
        prepareClient(exceptionThrownByClient);
        context.setVariable(Variables.DELETE_IDLE_URIS, true);
        setExistingAppInContext(existingAppFile);
        CloudApplicationExtended appToDeploy = JsonUtil.fromJson(TestUtil.getResourceAsString(appToDeployFile, getClass()),
                                                                 new TypeReference<>() {
                                                                 });
        context.setVariable(Variables.APP_TO_PROCESS, appToDeploy);
    }

    private void prepareClient(CloudOperationException exceptionThrownByClient) {
        if (exceptionThrownByClient != null) {
            Mockito.doThrow(exceptionThrownByClient)
                   .when(client)
                   .deleteRoute(anyString(), anyString(), anyString());
        }
    }

    private void setExistingAppInContext(String existingAppFile) {
        if (existingAppFile == null) {
            return;
        }
        CloudApplicationExtended existingApp = JsonUtil.fromJson(TestUtil.getResourceAsString(existingAppFile, getClass()),
                                                                 new TypeReference<>() {
                                                                 });
        context.setVariable(Variables.EXISTING_APP, existingApp);
    }

    private void assertStepPhaseMatch(StepPhase stepPhase) {
        Assertions.assertEquals(stepPhase.toString(), getExecutionStatus());
    }

    private void verifyClient(Set<CloudRouteSummary> routesToDelete) {
        if (CollectionUtils.isEmpty(routesToDelete)) {
            verify(client, never()).deleteRoute(anyString(), anyString(), anyString());
            return;
        }

        for (CloudRouteSummary route : routesToDelete) {
            verify(client, times(1)).deleteRoute(route.getHost(), route.getDomain(), route.getPath());
        }
    }

    @Test
    void testErrorMessage() {
        Assertions.assertEquals(Messages.ERROR_DELETING_IDLE_ROUTES, step.getStepErrorMessage(context));
    }

    @Test
    void testIfNotHandledExceptionIsThrown() {
        prepareContext("existing-app-1.json", "app-to-deploy-1.json", new CloudOperationException(HttpStatus.INTERNAL_SERVER_ERROR));
        Assertions.assertThrows(SLException.class, () -> step.execute(execution));
    }

    @Override
    protected DeleteIdleRoutesStep createStep() {
        return new DeleteIdleRoutesStep();
    }

}
