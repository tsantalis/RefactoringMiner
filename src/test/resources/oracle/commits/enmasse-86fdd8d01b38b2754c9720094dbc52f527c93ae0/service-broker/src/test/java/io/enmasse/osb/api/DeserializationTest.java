/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.osb.api.bind.BindRequest;
import io.enmasse.osb.api.provision.ProvisionRequest;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 */
public class DeserializationTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testProvisionRequest() throws IOException {
        UUID serviceUuid = UUID.randomUUID();
        UUID planUuid = UUID.randomUUID();
        UUID organizationUuid = UUID.randomUUID();
        UUID spaceUuid = UUID.randomUUID();

        Map<String, Object> map = new HashMap<>();
        map.put("service_id", serviceUuid.toString());
        map.put("plan_id", planUuid.toString());
        map.put("organization_guid", organizationUuid.toString());
        map.put("space_guid", spaceUuid.toString());

        Map<String, String> parameters = new HashMap<>();
        parameters.put("foo", "bar");
        parameters.put("baz", "qux");
        map.put("parameters", parameters);

        String serialized = mapper.writeValueAsString(map);

        ProvisionRequest request = mapper.readValue(serialized, ProvisionRequest.class);
        assertThat(request.getServiceId(), is(serviceUuid));
        assertThat(request.getPlanId(), is(planUuid));
        assertThat(request.getOrganizationId(), is(organizationUuid.toString()));
        assertThat(request.getSpaceId(), is(spaceUuid.toString()));
        assertThat(request.getParameters(), is(parameters));
    }

    @Test
    public void testBindRequest() throws IOException {
        UUID serviceUuid = UUID.randomUUID();
        UUID planUuid = UUID.randomUUID();
        UUID appUuid = UUID.randomUUID();

        Map<String, Object> map = new HashMap<>();
        map.put("service_id", serviceUuid.toString());
        map.put("plan_id", planUuid.toString());

        Map<String, String> bindResource = new HashMap<>();
        bindResource.put("app_guid", appUuid.toString());
        bindResource.put("route", "some-address");
        map.put("bind_resource", bindResource);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("foo", "bar");
        parameters.put("baz", "qux");
        map.put("parameters", parameters);


        String serialized = mapper.writeValueAsString(map);

        BindRequest request = mapper.readValue(serialized, BindRequest.class);
        assertThat(request.getServiceId(), is(serviceUuid));
        assertThat(request.getPlanId(), is(planUuid));
        assertThat(request.getBindResource().getAppId(), is(appUuid.toString()));
        assertThat(request.getBindResource().getRoute(), is("some-address"));
        assertThat(request.getParameters(), is(parameters));
    }

}
