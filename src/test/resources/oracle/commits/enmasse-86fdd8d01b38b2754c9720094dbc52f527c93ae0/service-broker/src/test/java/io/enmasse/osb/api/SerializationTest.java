/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import io.enmasse.osb.api.bind.BindResponse;
import io.enmasse.osb.api.catalog.CatalogResponse;
import io.enmasse.osb.api.catalog.InputParameters;
import io.enmasse.osb.api.catalog.Plan;
import io.enmasse.osb.api.catalog.Schemas;
import io.enmasse.osb.api.catalog.Service;
import io.enmasse.osb.api.catalog.ServiceInstanceSchema;
import io.enmasse.osb.api.lastoperation.LastOperationResponse;
import io.enmasse.osb.api.lastoperation.LastOperationState;
import io.enmasse.osb.api.provision.ProvisionResponse;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 */
public class SerializationTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testCatalogResponse() throws IOException {
        UUID serviceId = UUID.randomUUID();
        Service service = new Service(serviceId, "test-service", "test-description", true);
        service.setPlanUpdatable(false);

        UUID planId = UUID.randomUUID();
        Plan plan = new Plan(planId, "test-plan", "test-plan-description", true, true);
        plan.setSchemas(new Schemas(new ServiceInstanceSchema(new InputParameters(new ObjectSchema()), null), null));
        service.getPlans().add(plan);

        CatalogResponse response = new CatalogResponse(Collections.singletonList(service));

        String serialized = mapper.writeValueAsString(response);
        Map map = mapper.readValue(serialized, Map.class);

        List services = (List) map.get("services");
        assertThat(services.size(), is(1));

        Map serviceMap = (Map) services.get(0);
        assertThat(serviceMap.get("name"), is("test-service"));
        assertThat(serviceMap.get("id"), is(serviceId.toString()));
        assertThat(serviceMap.get("description"), is("test-description"));
        assertThat(serviceMap.get("tags"), is(Collections.EMPTY_LIST));
        assertThat(serviceMap.get("requires"), is(Collections.EMPTY_LIST));
        assertThat(serviceMap.get("bindable"), is(true));
        assertThat(serviceMap.get("metadata"), notNullValue());
        assertThat(serviceMap.get("dashboard_client"), nullValue());
        assertThat(serviceMap.get("plan_updateable"), is(false));

        List plans = (List) serviceMap.get("plans");
        assertThat(plans.size(), is(1));

        Map planMap = (Map)plans.get(0);
        assertThat(planMap.get("id"), is(planId.toString()));
        assertThat(planMap.get("name"), is("test-plan"));
        assertThat(planMap.get("description"), is("test-plan-description"));
        assertThat(planMap.get("metadata"), notNullValue());
        assertThat(planMap.get("free"), is(true));
        assertThat(planMap.get("bindable"), is(true));

        assertThat(planMap.get("schemas"), notNullValue()); // TODO: expand this
    }

    @Test
    public void testProvisionResponse() throws IOException {
        ProvisionResponse response = new ProvisionResponse("dashboard-url", "some-operation");
        String serialized = mapper.writeValueAsString(response);

        Map map = mapper.readValue(serialized, Map.class);

        assertThat(map.get("dashboard_url"), is("dashboard-url"));
        assertThat(map.get("operation"), is("some-operation"));
    }

    @Test
    public void testBindResponse() throws IOException {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("foo", "bar");
        credentials.put("baz", "qux");
        BindResponse response = new BindResponse(credentials);
        response.setRouteServiceUrl("route-service-url");
        response.setSyslogDrainUrl("syslog-drain-url");
        String serialized = mapper.writeValueAsString(response);

        Map map = mapper.readValue(serialized, Map.class);
        assertThat(map.get("route_service_url"), is("route-service-url"));
        assertThat(map.get("syslog_drain_url"), is("syslog-drain-url"));
        assertThat(map.get("credentials"), is(credentials));
    }

    @Test
    public void testLastOperationResponse() throws IOException {
        assertLastOperationResponse(new LastOperationResponse(LastOperationState.IN_PROGRESS, "operation-in-progress"), "in progress");
        assertLastOperationResponse(new LastOperationResponse(LastOperationState.SUCCEEDED, "operation-succeeded"), "succeeded");
        assertLastOperationResponse(new LastOperationResponse(LastOperationState.FAILED, "operation-failed"), "failed");
    }

    private void assertLastOperationResponse(LastOperationResponse response, String expectedState) throws IOException {
        String serialized = mapper.writeValueAsString(response);

        Map map = mapper.readValue(serialized, Map.class);
        assertThat(map.get("state"), is(expectedState));
        assertThat(map.get("description"), is(response.getDescription()));
    }

}
