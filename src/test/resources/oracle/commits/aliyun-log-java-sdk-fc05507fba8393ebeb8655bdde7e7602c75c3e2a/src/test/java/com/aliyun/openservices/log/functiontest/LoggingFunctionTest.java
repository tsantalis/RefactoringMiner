package com.aliyun.openservices.log.functiontest;


import com.aliyun.openservices.log.common.LogStore;
import com.aliyun.openservices.log.common.Logging;
import com.aliyun.openservices.log.common.LoggingDetail;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.request.CreateLoggingRequest;
import com.aliyun.openservices.log.request.DeleteLoggingRequest;
import com.aliyun.openservices.log.request.GetLoggingRequest;
import com.aliyun.openservices.log.request.UpdateLoggingRequest;
import com.aliyun.openservices.log.response.GetLoggingResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

    private static String TEST_PROJECT;
    private static List<String> TEST_LOGSTORES;

    @BeforeClass
    public static void setUp() {
        TEST_PROJECT = "project-to-test-logging-" + getNowTimestamp();
        safeCreateProject(TEST_PROJECT, "");
        TEST_LOGSTORES = new ArrayList<String>();
        for (String type : TYPES_ALLOWED) {
            String logstoreName = "internal-" + type;
            LogStore logStore = new LogStore(logstoreName, 1, 1);
            createOrUpdateLogStore(TEST_PROJECT, logStore);
            TEST_LOGSTORES.add(logstoreName);
        }
    }

    @Test
    public void testCreateLogging() throws Exception {
        // testing a not exist logstore
        List<LoggingDetail> details = new ArrayList<LoggingDetail>();
        details.add(new LoggingDetail(randomFrom(TYPES_ALLOWED), "logstore-not-exist"));
        Logging logging = new Logging(TEST_PROJECT, details);
        CreateLoggingRequest createLoggingRequest = new CreateLoggingRequest(TEST_PROJECT, logging);
        createShouldFail(createLoggingRequest, "logstore logstore-not-exist dose not exist", "LogStoreNotExist");

        // testing a invalid type
        details.clear();
        details.add(new LoggingDetail("invalid-type", randomFrom(TEST_LOGSTORES)));
        logging = new Logging(TEST_PROJECT, details);
        createLoggingRequest = new CreateLoggingRequest(TEST_PROJECT, logging);
        createShouldFail(createLoggingRequest, "Invalid type 'invalid-type'", "ParameterInvalid");

        logging.setLoggingProject("invalid-project-name");
        createLoggingRequest = new CreateLoggingRequest(TEST_PROJECT, logging);
        createShouldFail(createLoggingRequest, "The Project does not exist : invalid-project-name", "ProjectNotExist");

        // create a valid logging
        details.clear();
        String goodType = randomFrom(TYPES_ALLOWED);
        String goodLogstore = randomFrom(TEST_LOGSTORES);
        details.add(new LoggingDetail(goodType, goodLogstore));
        logging = new Logging(TEST_PROJECT, details);
        createLoggingRequest = new CreateLoggingRequest(TEST_PROJECT, logging);
        client.createLogging(createLoggingRequest);

        GetLoggingRequest getLoggingRequest = new GetLoggingRequest(TEST_PROJECT);
        GetLoggingResponse getLoggingResponse = client.getLogging(getLoggingRequest);
        Logging created = getLoggingResponse.getLogging();
        assertEquals(created.getLoggingProject(), TEST_PROJECT);
        List<LoggingDetail> createdDetails = created.getLoggingDetails();
        assertEquals(createdDetails.size(), 1);
        LoggingDetail createdItem = createdDetails.get(0);
        assertEquals(goodType, createdItem.getType());
        assertEquals(goodLogstore, createdItem.getLogstore());

        createShouldFail(createLoggingRequest,
                "Logging already exists for project '" + TEST_PROJECT + "'", "LoggingAlreadyExist");
    }

    private void createShouldFail(CreateLoggingRequest request,
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
        GetLoggingRequest getLoggingRequest = new GetLoggingRequest(TEST_PROJECT);
        try {
            client.getLogging(getLoggingRequest);
            fail();
        } catch (LogException ex) {
            assertEquals(ex.GetErrorMessage(), "Logging does not exist for project '" + TEST_PROJECT + "'");
            assertEquals(ex.GetErrorCode(), "LoggingNotExist");
        }

        List<LoggingDetail> details = new ArrayList<LoggingDetail>();
        String goodType = randomFrom(TYPES_ALLOWED);
        String goodLogstore = randomFrom(TEST_LOGSTORES);
        details.add(new LoggingDetail(goodType, goodLogstore));
        Logging logging = new Logging(TEST_PROJECT, details);
        CreateLoggingRequest createLoggingRequest = new CreateLoggingRequest(TEST_PROJECT, logging);
        client.createLogging(createLoggingRequest);

        // check get logging
        getLoggingRequest = new GetLoggingRequest(TEST_PROJECT);
        GetLoggingResponse getLoggingResponse = client.getLogging(getLoggingRequest);
        Logging created = getLoggingResponse.getLogging();
        assertEquals(created.getLoggingProject(), TEST_PROJECT);
        List<LoggingDetail> createdDetails = created.getLoggingDetails();
        assertEquals(createdDetails.size(), 1);
        LoggingDetail createdItem = createdDetails.get(0);
        assertEquals(goodType, createdItem.getType());
        assertEquals(goodLogstore, createdItem.getLogstore());
    }

    @Test
    public void testDeleteLogging() throws Exception {
        DeleteLoggingRequest deleteLoggingRequest = new DeleteLoggingRequest(TEST_PROJECT);
        try {
            client.deleteLogging(deleteLoggingRequest);
            fail();
        } catch (LogException ex) {
            assertEquals(ex.GetErrorMessage(), "Logging does not exist for project '" + TEST_PROJECT + "'");
            assertEquals(ex.GetErrorCode(), "LoggingNotExist");
        }

        List<LoggingDetail> details = new ArrayList<LoggingDetail>();
        String goodType = randomFrom(TYPES_ALLOWED);
        String goodLogstore = randomFrom(TEST_LOGSTORES);
        details.add(new LoggingDetail(goodType, goodLogstore));
        Logging logging = new Logging(TEST_PROJECT, details);
        CreateLoggingRequest createLoggingRequest = new CreateLoggingRequest(TEST_PROJECT, logging);
        client.createLogging(createLoggingRequest);

        // check delete logging
        deleteLoggingRequest = new DeleteLoggingRequest(TEST_PROJECT);
        client.deleteLogging(deleteLoggingRequest);
    }

    @Test
    public void testUpdateLogging() throws Exception {
        // testing update a not exist logstore
        List<LoggingDetail> details = new ArrayList<LoggingDetail>();
        String goodType = randomFrom(TYPES_ALLOWED);
        String goodLogstore = randomFrom(TEST_LOGSTORES);
        details.add(new LoggingDetail(goodType, goodLogstore));
        Logging logging = new Logging(TEST_PROJECT, details);

        UpdateLoggingRequest updateLoggingRequest = new UpdateLoggingRequest(TEST_PROJECT, logging);
        updateShouldFail(updateLoggingRequest, "Logging does not exist for project '" + TEST_PROJECT + "'", "LoggingNotExist");

        // create a valid logging
        details.clear();
        details.add(new LoggingDetail(goodType, goodLogstore));
        logging = new Logging(TEST_PROJECT, details);
        CreateLoggingRequest createLoggingRequest = new CreateLoggingRequest(TEST_PROJECT, logging);
        client.createLogging(createLoggingRequest);

        // testing a not exist logstore
        details.clear();
        details.add(new LoggingDetail(randomFrom(TYPES_ALLOWED), "logstore-not-exist"));
        logging = new Logging(TEST_PROJECT, details);
        updateLoggingRequest = new UpdateLoggingRequest(TEST_PROJECT, logging);
        updateShouldFail(updateLoggingRequest, "logstore logstore-not-exist dose not exist", "LogStoreNotExist");

        // testing a invalid type
        details.clear();
        details.add(new LoggingDetail("invalid-type", randomFrom(TEST_LOGSTORES)));
        logging = new Logging(TEST_PROJECT, details);
        updateLoggingRequest = new UpdateLoggingRequest(TEST_PROJECT, logging);
        updateShouldFail(updateLoggingRequest, "Invalid type 'invalid-type'", "ParameterInvalid");

        // testing a invalid project
        logging = new Logging("invalid-project-name", details);
        updateLoggingRequest = new UpdateLoggingRequest(TEST_PROJECT, logging);
        updateShouldFail(updateLoggingRequest, "The Project does not exist : invalid-project-name", "ProjectNotExist");

        // check update logging
        details.clear();
        for (String type : TYPES_ALLOWED) {
            if (!type.endsWith(goodType)) {
                details.add(new LoggingDetail(type, goodLogstore));
            }
        }
        logging = new Logging(TEST_PROJECT, details);
        updateLoggingRequest = new UpdateLoggingRequest(TEST_PROJECT, logging);
        client.updateLogging(updateLoggingRequest);

        GetLoggingRequest getLoggingRequest = new GetLoggingRequest(TEST_PROJECT);
        GetLoggingResponse response = client.getLogging(getLoggingRequest);
        Logging updated = response.getLogging();
        assertEquals(updated.getLoggingProject(), TEST_PROJECT);

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

    private void updateShouldFail(UpdateLoggingRequest request, String errorMessage, String errorCode) {
        try {
            client.updateLogging(request);
            fail();
        } catch (LogException ex) {
            assertEquals(errorMessage, ex.GetErrorMessage());
            assertEquals(errorCode, ex.GetErrorCode());
        }
    }

    @Test
    public void testDeleteProjectWillDeleteLogging() throws Exception {
        List<LoggingDetail> details = new ArrayList<LoggingDetail>();
        String goodType = randomFrom(TYPES_ALLOWED);
        String goodLogstore = randomFrom(TEST_LOGSTORES);
        details.add(new LoggingDetail(goodType, goodLogstore));
        Logging logging = new Logging(TEST_PROJECT, details);
        client.createLogging(new CreateLoggingRequest(TEST_PROJECT, logging));
        GetLoggingResponse response = client.getLogging(new GetLoggingRequest(TEST_PROJECT));
        assertNotNull(response.getLogging());
        LoggingDetail detail = response.getLogging().getLoggingDetails().get(0);
        assertEquals(detail.getLogstore(), goodLogstore);
        assertEquals(detail.getType(), goodType);
        client.DeleteProject(TEST_PROJECT);
        try {
            client.getLogging(new GetLoggingRequest(TEST_PROJECT));
            fail();
        } catch (LogException ex) {
            assertEquals(ex.GetErrorCode(), "LoggingNotExist");
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        client.DeleteProject(TEST_PROJECT);
    }
}
