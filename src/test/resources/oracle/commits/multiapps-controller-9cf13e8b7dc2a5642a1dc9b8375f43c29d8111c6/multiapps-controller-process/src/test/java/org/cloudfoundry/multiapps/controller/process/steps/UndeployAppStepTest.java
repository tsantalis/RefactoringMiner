package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.ListUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.clients.ApplicationRoutesGetter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.CloudTask;

abstract class UndeployAppStepTest extends SyncFlowableStepTest<UndeployAppStep> {

    @Mock
    protected ApplicationRoutesGetter applicationRoutesGetter;

    protected StepInput stepInput;
    protected StepOutput stepOutput;

    static Stream<Arguments> testExecution() {
        return Stream.of(
                         // (0) There are applications to undeploy:
                         Arguments.of("undeploy-apps-step-input-00.json", "undeploy-apps-step-output-00.json"),

                         // (1) No applications to undeploy:
                         Arguments.of("undeploy-apps-step-input-02.json", "undeploy-apps-step-output-02.json"),

                         // (2) There are two routes that should be deleted, but one of them is bound to another application:
                         Arguments.of("undeploy-apps-step-input-03.json", "undeploy-apps-step-output-03.json"),

                         // (3) There are running one-off tasks to cancel:
                         Arguments.of("undeploy-apps-step-input-04.json", "undeploy-apps-step-output-04.json"),

                         // (4) There are not found routes matching app uri:
                         Arguments.of("undeploy-apps-step-input-05.json", "undeploy-apps-step-output-05.json"),

                         // (5) There is a route that should be deleted, but it is bound to a service instance:
                         Arguments.of("undeploy-apps-step-input-06.json", "undeploy-apps-step-output-06.json"));
    }

    @ParameterizedTest
    @MethodSource
    void testExecution(String stepInputLocation, String stepOutputLocation) throws Exception {
        initializeParameters(stepInputLocation, stepOutputLocation);
        for (CloudApplicationExtended cloudApplication : stepInput.appsToDelete) {
            undeployApp(cloudApplication);
        }

        performAfterUndeploymentValidation();
    }

    private void initializeParameters(String stepInputLocation, String stepOutputLocation) {
        String resourceAsString = TestUtil.getResourceAsString(stepInputLocation, UndeployAppStepTest.class);
        stepInput = JsonUtil.fromJson(resourceAsString, StepInput.class);
        stepOutput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepOutputLocation, UndeployAppStepTest.class), StepOutput.class);
        prepareContext();
        prepareClient();
        Mockito.when(client.getTasks(Mockito.anyString()))
               .thenReturn(Collections.emptyList());
    }

    protected abstract void performAfterUndeploymentValidation();

    private void undeployApp(CloudApplicationExtended cloudApplication) {
        context.setVariable(Variables.APP_TO_PROCESS, cloudApplication);
        step.execute(execution);

        assertStepFinishedSuccessfully();
        performValidation(cloudApplication);
    }

    protected abstract void performValidation(CloudApplication cloudApplication);

    private void prepareContext() {
        context.setVariable(Variables.APPS_TO_UNDEPLOY, ListUtil.cast(stepInput.appsToDelete));
    }

    private void prepareClient() {
        Mockito.when(applicationRoutesGetter.getRoutes(anyString()))
               .thenAnswer((invocation) -> {

                   String appName = (String) invocation.getArguments()[0];
                   return stepInput.appRoutesPerApplication.get(appName);

               });
        Mockito.when(client.getTasks(anyString()))
               .thenAnswer((invocation) -> {

                   String appName = (String) invocation.getArguments()[0];
                   return stepInput.tasksPerApplication.get(appName);

               });
        Mockito.when(client.getApplication(anyString(), any(Boolean.class)))
               .thenAnswer((invocation) -> {
                   String appName = (String) invocation.getArguments()[0];
                   return stepInput.appsToDelete.stream()
                                                .filter(app -> app.getName()
                                                                  .equals(appName))
                                                .findFirst()
                                                .orElse(null);
               });
    }

    protected static class StepInput {
        protected final List<CloudApplicationExtended> appsToDelete = Collections.emptyList();
        protected final Map<String, List<CloudRoute>> appRoutesPerApplication = Collections.emptyMap();
        protected final Map<String, List<CloudTask>> tasksPerApplication = Collections.emptyMap();
    }

    protected static class StepOutput {
        protected final List<Route> expectedRoutesToDelete = Collections.emptyList();
        protected List<String> expectedTasksToCancel = Collections.emptyList();
    }

    protected static class Route {
        protected String host = "";
        protected String domain = "";
        protected String path = "";
    }
}
