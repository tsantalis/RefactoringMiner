package com.aliyun.openservices.log.functiontest;


import com.aliyun.openservices.log.common.Logging;
import com.aliyun.openservices.log.common.LoggingDetail;
import com.aliyun.openservices.log.common.Project;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.request.CreateLoggingRequest;
import com.aliyun.openservices.log.request.DeleteLoggingRequest;
import com.aliyun.openservices.log.request.GetLoggingRequest;
import com.aliyun.openservices.log.request.UpdateLoggingRequest;
import com.aliyun.openservices.log.response.GetLoggingResponse;
import com.aliyun.openservices.log.response.ListLogStoresResponse;
import com.aliyun.openservices.log.response.ListProjectResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class LoggingFunctionTest extends FunctionTest {

    private static final String[] TYPES_ALLOWED = new String[]{
            "operation_log",
            "consumergroup_log",
            "logtail_alarm",
            "logtail_profile",
            "logtail_status",
            "metering"
    };

    private String projectName;
    private ArrayList<String> logStores;


    @Before
    public void setUp() throws Exception {
        ListProjectResponse response = client.ListProject();
        List<Project> projects = response.getProjects();

        for (Project project : projects) {
            projectName = project.getProjectName();
            ListLogStoresResponse logStoresResponse = client.ListLogStores(projectName, 0, 100, "");
            logStores = logStoresResponse.GetLogStores();
            if (!logStores.isEmpty()) {
                break;
            }
        }
        if (logStores.isEmpty()) {
            fail("No logstores are found");
        }
    }

    @Test
    public void testCreateLogging() throws Exception {
        // testing a not exist logstore
        List<LoggingDetail> details = new ArrayList<LoggingDetail>();
        details.add(new LoggingDetail(randomFrom(TYPES_ALLOWED), "logstore-not-exist"));
        Logging logging = new Logging(projectName, details);
        CreateLoggingRequest createLoggingRequest = new CreateLoggingRequest(projectName, logging);
        expectThrows(createLoggingRequest, "logstore logstore-not-exist dose not exist", "LogStoreNotExist");

        // testing a invalid type
        details.clear();
        details.add(new LoggingDetail("invalid-type", randomFrom(logStores)));
        logging = new Logging(projectName, details);
        createLoggingRequest = new CreateLoggingRequest(projectName, logging);
        expectThrows(createLoggingRequest, "Invalid type 'invalid-type'", "ParameterInvalid");

        logging.setLoggingProject("invalid-project-name");
        createLoggingRequest = new CreateLoggingRequest(projectName, logging);
        expectThrows(createLoggingRequest, "The Project does not exist : invalid-project-name", "ProjectNotExist");

        // create a valid logging
        details.clear();
        String goodType = randomFrom(TYPES_ALLOWED);
        String goodLogstore = randomFrom(logStores);
        details.add(new LoggingDetail(goodType, goodLogstore));
        logging = new Logging(projectName, details);
        createLoggingRequest = new CreateLoggingRequest(projectName, logging);
        client.createLogging(createLoggingRequest);

        GetLoggingRequest getLoggingRequest = new GetLoggingRequest(projectName);
        GetLoggingResponse getLoggingResponse = client.getLogging(getLoggingRequest);
        Logging created = getLoggingResponse.getLogging();
        assertEquals(created.getLoggingProject(), projectName);
        List<LoggingDetail> createdDetails = created.getLoggingDetails();
        assertEquals(createdDetails.size(), 1);
        LoggingDetail createdItem = createdDetails.get(0);
        assertEquals(goodType, createdItem.getType());
        assertEquals(goodLogstore, createdItem.getLogstore());

        expectThrows(createLoggingRequest,
                "Logging already exists for project '" + projectName + "'", "LoggingAlreadyExist");
    }

    private void expectThrows(CreateLoggingRequest request,
                              String errorMessage,
                              String errorCode) {
        try {
            client.createLogging(request);
            fail();
        } catch (LogException ex) {
            assertEquals(errorMessage, ex.GetErrorMessage());
            assertEquals(errorCode, ex.GetErrorCode());
        }
    }

    @Test
    public void testGetLogging() throws Exception {
        GetLoggingRequest getLoggingRequest = new GetLoggingRequest(projectName);
        try {
            client.getLogging(getLoggingRequest);
            fail();
        } catch (LogException ex) {
            assertEquals(ex.GetErrorMessage(), "Logging does not exist for project '" + projectName + "'");
            assertEquals(ex.GetErrorCode(), "LoggingNotExist");
        }

        List<LoggingDetail> details = new ArrayList<LoggingDetail>();
        String goodType = randomFrom(TYPES_ALLOWED);
        String goodLogstore = randomFrom(logStores);
        details.add(new LoggingDetail(goodType, goodLogstore));
        Logging logging = new Logging(projectName, details);
        CreateLoggingRequest createLoggingRequest = new CreateLoggingRequest(projectName, logging);
        client.createLogging(createLoggingRequest);

        // check get logging
        getLoggingRequest = new GetLoggingRequest(projectName);
        GetLoggingResponse getLoggingResponse = client.getLogging(getLoggingRequest);
        Logging created = getLoggingResponse.getLogging();
        assertEquals(created.getLoggingProject(), projectName);
        List<LoggingDetail> createdDetails = created.getLoggingDetails();
        assertEquals(createdDetails.size(), 1);
        LoggingDetail createdItem = createdDetails.get(0);
        assertEquals(goodType, createdItem.getType());
        assertEquals(goodLogstore, createdItem.getLogstore());
    }

    @Test
    public void testDeleteLogging() throws Exception {
        DeleteLoggingRequest deleteLoggingRequest = new DeleteLoggingRequest(projectName);
        try {
            client.deleteLogging(deleteLoggingRequest);
            fail();
        } catch (LogException ex) {
            assertEquals(ex.GetErrorMessage(), "Logging does not exist for project '" + projectName + "'");
            assertEquals(ex.GetErrorCode(), "LoggingNotExist");
        }

        List<LoggingDetail> details = new ArrayList<LoggingDetail>();
        String goodType = randomFrom(TYPES_ALLOWED);
        String goodLogstore = randomFrom(logStores);
        details.add(new LoggingDetail(goodType, goodLogstore));
        Logging logging = new Logging(projectName, details);
        CreateLoggingRequest createLoggingRequest = new CreateLoggingRequest(projectName, logging);
        client.createLogging(createLoggingRequest);

        // check delete logging
        deleteLoggingRequest = new DeleteLoggingRequest(projectName);
        client.deleteLogging(deleteLoggingRequest);
    }

    @Test
    public void testUpdateLogging() throws Exception {
        // testing update a not exist logstore
        List<LoggingDetail> details = new ArrayList<LoggingDetail>();
        String goodType = randomFrom(TYPES_ALLOWED);
        String goodLogstore = randomFrom(logStores);
        details.add(new LoggingDetail(goodType, goodLogstore));
        Logging logging = new Logging(projectName, details);

        UpdateLoggingRequest updateLoggingRequest = new UpdateLoggingRequest(projectName, logging);
        expectThrows(updateLoggingRequest, "Logging does not exist for project '" + projectName + "'", "LoggingNotExist");

        // create a valid logging
        details.clear();
        details.add(new LoggingDetail(goodType, goodLogstore));
        logging = new Logging(projectName, details);
        CreateLoggingRequest createLoggingRequest = new CreateLoggingRequest(projectName, logging);
        client.createLogging(createLoggingRequest);

        // testing a not exist logstore
        details.clear();
        details.add(new LoggingDetail(randomFrom(TYPES_ALLOWED), "logstore-not-exist"));
        logging = new Logging(projectName, details);
        updateLoggingRequest = new UpdateLoggingRequest(projectName, logging);
        expectThrows(updateLoggingRequest, "logstore logstore-not-exist dose not exist", "LogStoreNotExist");

        // testing a invalid type
        details.clear();
        details.add(new LoggingDetail("invalid-type", randomFrom(logStores)));
        logging = new Logging(projectName, details);
        updateLoggingRequest = new UpdateLoggingRequest(projectName, logging);
        expectThrows(updateLoggingRequest, "Invalid type 'invalid-type'", "ParameterInvalid");

        // testing a invalid project
        logging = new Logging("invalid-project-name", details);
        updateLoggingRequest = new UpdateLoggingRequest(projectName, logging);
        expectThrows(updateLoggingRequest, "The Project does not exist : invalid-project-name", "ProjectNotExist");

        // check update logging
        details.clear();
        for (String type : TYPES_ALLOWED) {
            if (!type.endsWith(goodType)) {
                details.add(new LoggingDetail(type, goodLogstore));
            }
        }
        logging = new Logging(projectName, details);
        updateLoggingRequest = new UpdateLoggingRequest(projectName, logging);
        client.updateLogging(updateLoggingRequest);

        GetLoggingRequest getLoggingRequest = new GetLoggingRequest(projectName);
        GetLoggingResponse response = client.getLogging(getLoggingRequest);
        Logging updated = response.getLogging();
        assertEquals(updated.getLoggingProject(), projectName);

        final int numTypes = details.size();
        assertEquals(numTypes, logging.getLoggingDetails().size());

        Set<String> typeCreated = new HashSet<String>(numTypes);
        for (LoggingDetail detail : logging.getLoggingDetails()) {
            assertEquals(goodLogstore, detail.getLogstore());
            typeCreated.add(detail.getType());
        }
        assertEquals(numTypes, typeCreated.size());
        for (LoggingDetail detail : details) {
            assertTrue(typeCreated.contains(detail.getType()));
        }
    }

    private void expectThrows(UpdateLoggingRequest request, String errorMessage, String errorCode) {
        try {
            client.updateLogging(request);
            fail();
        } catch (LogException ex) {
            assertEquals(errorMessage, ex.GetErrorMessage());
            assertEquals(errorCode, ex.GetErrorCode());
        }
    }

    @After
    public void tearDown() {
        try {
            DeleteLoggingRequest request = new DeleteLoggingRequest(projectName);
            client.deleteLogging(request);
        } catch (Exception ex) {
            // ignore it
        }
    }
}
