/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.osb.api;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.Status;
import io.enmasse.api.common.ConflictException;
import io.enmasse.api.common.GoneException;
import io.enmasse.api.common.UnprocessableEntityException;
import io.enmasse.osb.api.lastoperation.LastOperationResponse;
import io.enmasse.osb.api.lastoperation.LastOperationState;
import io.enmasse.osb.api.provision.ProvisionRequest;
import io.enmasse.osb.api.provision.ProvisionResponse;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@Disabled
public class ProvisionServiceTest extends OSBTestBase {

    public static final String QUEUE_SERVICE_ID_STRING = QUEUE_SERVICE_ID.toString();
    public static final String QUEUE_PLAN_ID_STRING = QUEUE_PLAN_ID.toString();

    public static final String ADDRESS = "my-queue";
    public static final String TRANSACTIONAL = "transactional";
    private UriInfo uriInfo = mock(UriInfo.class);

    @Test
    public void testSyncProvisioningRequest() throws Exception {
        assertThrows(UnprocessableEntityException.class,
                () -> provisioningService.provisionService(getSecurityContext(), null, "123", false, new ProvisionRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID, ORGANIZATION_ID, SPACE_ID)));
    }

    @Test
    public void testInvalidServiceUuid() throws Exception {
        assertThrows(BadRequestException.class,
                () -> provisioningService.provisionService(getSecurityContext(), null, "123", true, new ProvisionRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID, ORGANIZATION_ID, SPACE_ID)));
    }

    @Test
    public void testInvalidPlan() throws Exception {
        assertThrows(BadRequestException.class,
                () -> provisioningService.provisionService(getSecurityContext(), null, "123", true, new ProvisionRequest(QUEUE_SERVICE_ID, TOPIC_PLAN_ID, ORGANIZATION_ID, SPACE_ID)));
    }

    @Test
    public void testInvalidServiceInstandeUuid() throws Exception {
        assertThrows(BadRequestException.class,
                () -> provisioningService.provisionService(getSecurityContext(), null, "123", true, new ProvisionRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID, ORGANIZATION_ID, SPACE_ID)));
    }

    @Test
    public void testProvision() throws Exception {
        ProvisionRequest provisionRequest = new ProvisionRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID, ORGANIZATION_ID, SPACE_ID);
        provisionRequest.putParameter("name", ADDRESS);
        provisionRequest.putParameter("transactional", "true");
        Response response = provisioningService.provisionService(getSecurityContext(), null, SERVICE_INSTANCE_ID, true, provisionRequest);
        ProvisionResponse provisionResponse = (ProvisionResponse) response.getEntity();

        assertThat(response.getStatus(), is(HttpResponseCodes.SC_ACCEPTED));
//        assertThat(provisionResponse.getDashboardUrl(), notNullValue());
        assertThat(provisionResponse.getOperation(), notNullValue());

        Address destination = new Address.Builder()
                .setName(ADDRESS)
                .setAddress(ADDRESS)
                .setAddressSpace("unknown")
                .setStatus(new Status(false))
                .setType("queue")
                .setPlan("myplan")
                .build();
        //assertThat(addressSpaceApi.getAddresses(), is(new HashSet<>(Collections.singletonList(destination))));

        LastOperationResponse lastOperationResponse = getLastOperationResponse(SERVICE_INSTANCE_ID, QUEUE_SERVICE_ID_STRING, QUEUE_PLAN_ID_STRING, provisionResponse.getOperation());
        assertThat(lastOperationResponse.getState(), is(LastOperationState.IN_PROGRESS));

        addressSpaceApi.setAllInstancesReady(true);

        lastOperationResponse = getLastOperationResponse(SERVICE_INSTANCE_ID, QUEUE_SERVICE_ID_STRING, QUEUE_PLAN_ID_STRING, provisionResponse.getOperation());
        assertThat(lastOperationResponse.getState(), is(LastOperationState.IN_PROGRESS));

        addressSpaceApi.getAddressApis().iterator().next().setAllAddressesReady(true);

        lastOperationResponse = getLastOperationResponse(SERVICE_INSTANCE_ID, QUEUE_SERVICE_ID_STRING, QUEUE_PLAN_ID_STRING, provisionResponse.getOperation());
        assertThat(lastOperationResponse.getState(), is(LastOperationState.SUCCEEDED));

        destination = new Address.Builder()
                .setName(ADDRESS)
                .setAddress(ADDRESS)
                .setAddressSpace("unknown")
                .setStatus(new Status(true))
                .setType("queue")
                .setPlan("myplan")
                .build();

        //assertThat(addressSpaceApi.getAddresses(), is(new HashSet<>(Collections.singletonList(destination))));
    }

    private LastOperationResponse getLastOperationResponse(String serviceInstanceId, String serviceId, String planId, String operation) throws Exception {
        Response response = lastOperationService.getLastOperationStatus(getSecurityContext(), serviceInstanceId, serviceId, planId, operation);
        return (LastOperationResponse) response.getEntity();
    }


    @Test
    public void testProvisionTwiceWithDifferentPrameters() throws Exception {
        provisioningService.provisionService(getSecurityContext(), null, SERVICE_INSTANCE_ID, true, new ProvisionRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID, ORGANIZATION_ID, SPACE_ID));
        exceptionGrabber.expect(ConflictException.class);
        provisioningService.provisionService(getSecurityContext(), null, SERVICE_INSTANCE_ID, true, new ProvisionRequest(ServiceType.TOPIC.uuid(), TOPIC_PLAN_ID, ORGANIZATION_ID, SPACE_ID));
    }

    @Test
    public void testProvisionTwiceWithSameParameters() throws Exception {
        ProvisionRequest provisionRequest = new ProvisionRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID, ORGANIZATION_ID, SPACE_ID);
        provisioningService.provisionService(getSecurityContext(), null, SERVICE_INSTANCE_ID, true, provisionRequest);
        Response response = provisioningService.provisionService(getSecurityContext(), null, SERVICE_INSTANCE_ID, true, provisionRequest);
        assertThat(response.getStatus(), is(HttpResponseCodes.SC_OK));
    }

    @Test
    public void testDeprovisionNonexistingServiceInstance() throws Exception {
        assertThrows(GoneException.class,
                () -> provisioningService.deprovisionService(getSecurityContext(), SERVICE_INSTANCE_ID, QUEUE_SERVICE_ID_STRING, QUEUE_PLAN_ID_STRING));
    }

    @Test
    public void testDeprovisionWithoutServiceId() throws Exception {
        assertThrows(BadRequestException.class,
                () -> provisioningService.deprovisionService(getSecurityContext(), SERVICE_INSTANCE_ID, null, QUEUE_PLAN_ID_STRING));
    }

    @Test
    public void testDeprovisionWithoutPlanId() throws Exception {
        assertThrows(BadRequestException.class,
                () -> provisioningService.deprovisionService(getSecurityContext(), SERVICE_INSTANCE_ID, QUEUE_SERVICE_ID_STRING, null));
    }

    @Test
    public void testDeprovision() throws Exception {
        provisionService(SERVICE_INSTANCE_ID);
        Response response = provisioningService.deprovisionService(getSecurityContext(), SERVICE_INSTANCE_ID, QUEUE_SERVICE_ID_STRING, QUEUE_PLAN_ID_STRING);
        assertThat(response.getStatus(), is(HttpResponseCodes.SC_OK));
        //assertThat(addressSpaceApi.getAddresses(), is(Collections.EMPTY_SET));
    }

    @Test
    public void testDeprovisionTwice() throws Exception {
        provisionService(SERVICE_INSTANCE_ID);
        provisioningService.deprovisionService(getSecurityContext(), SERVICE_INSTANCE_ID, QUEUE_SERVICE_ID_STRING, QUEUE_PLAN_ID_STRING);

        exceptionGrabber.expect(GoneException.class);
        provisioningService.deprovisionService(getSecurityContext(), SERVICE_INSTANCE_ID, QUEUE_SERVICE_ID_STRING, QUEUE_PLAN_ID_STRING);
    }

    @Test
    public void testDeprovisionGivenMultipleOrganizations() throws Exception {
        String serviceId11 = provisionService(randomUUID(), ORGANIZATION_ID, SPACE_ID);
        provisionService(randomUUID(), ORGANIZATION_ID, SPACE_ID);

        String organizationId2 = randomUUID();
        String spaceId2 = randomUUID();
        String serviceId21 = provisionService(randomUUID(), organizationId2, spaceId2);
        provisionService(randomUUID(), organizationId2, spaceId2);

        Response response = provisioningService.deprovisionService(getSecurityContext(), serviceId21, QUEUE_SERVICE_ID_STRING, QUEUE_PLAN_ID_STRING);
        assertThat(response.getStatus(), is(HttpResponseCodes.SC_OK));
        //assertThat(addressSpaceApi.getAddressUuids(), not(hasItem(serviceId21)));

        response = provisioningService.deprovisionService(getSecurityContext(), serviceId11, QUEUE_SERVICE_ID_STRING, QUEUE_PLAN_ID_STRING);
        assertThat(response.getStatus(), is(HttpResponseCodes.SC_OK));
        //assertThat(addressSpaceApi.getAddressUuids(), not(hasItem(serviceId11)));
    }

    private String randomUUID() {
        return UUID.randomUUID().toString();
    }


}
