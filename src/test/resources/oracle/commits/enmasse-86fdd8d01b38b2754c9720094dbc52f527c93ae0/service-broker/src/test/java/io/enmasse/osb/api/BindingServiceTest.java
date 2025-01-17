/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.osb.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import io.enmasse.api.common.ConflictException;
import io.enmasse.api.common.GoneException;
import io.enmasse.api.common.UnprocessableEntityException;
import io.enmasse.osb.api.bind.BindRequest;
import io.enmasse.osb.api.bind.BindResponse;
import io.enmasse.osb.api.provision.ProvisionRequest;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.junit.Ignore;
import org.junit.Test;

import java.util.UUID;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Ignore
public class BindingServiceTest extends OSBTestBase {

    public static final String BINDING_ID = UUID.randomUUID().toString();

    @Override
    public void setup() throws Exception {
        super.setup();
        provisionService(SERVICE_INSTANCE_ID);
    }

    @Test(expected = UnprocessableEntityException.class)
    public void testSyncProvisioningRequest() throws Exception {
        UriInfo uriInfo = mock(UriInfo.class);
        provisioningService.provisionService(getSecurityContext(), null, "123", false, new ProvisionRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID, ORGANIZATION_ID, SPACE_ID));
    }

    @Test
    @Ignore
    public void testBind() throws Exception {
        Response response = bindingService.bindServiceInstance(getSecurityContext(), SERVICE_INSTANCE_ID, BINDING_ID, new BindRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID));
        BindResponse bindResponse = (BindResponse) response.getEntity();

        assertThat(response.getStatus(), is(HttpResponseCodes.SC_CREATED));
        assertThat(bindResponse.getCredentials().get("namespace"), notNullValue());
        assertThat(bindResponse.getCredentials().get("destination-address"), notNullValue());
        // TODO: Set fake hosts
//        assertThat(bindResponse.getCredentials().get("internal-messaging-host"), notNullValue());
//        assertThat(bindResponse.getCredentials().get("internal-mqtt-host"), notNullValue());
    }

    @Test
    public void testInvalidBindingUuid() throws Exception {
        bindingService.bindServiceInstance(getSecurityContext(), SERVICE_INSTANCE_ID, "123", new BindRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID));
    }

    @Test(expected = NotFoundException.class)
    @Ignore
    public void testBindOnNonexistentService() throws Exception {
        bindingService.bindServiceInstance(getSecurityContext(), UUID.randomUUID().toString(), BINDING_ID, new BindRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID));
    }

    @Test(expected = BadRequestException.class)
    @Ignore
    public void testBindWithoutServiceId() throws Exception {
        bindingService.bindServiceInstance(getSecurityContext(), SERVICE_INSTANCE_ID, BINDING_ID, new BindRequest(null, QUEUE_PLAN_ID));
    }

    @Test(expected = BadRequestException.class)
    public void testBindWithoutPlanId() throws Exception {
        bindingService.bindServiceInstance(getSecurityContext(), SERVICE_INSTANCE_ID, BINDING_ID, new BindRequest(QUEUE_SERVICE_ID, null));
    }

    @Ignore("Not implemented yet")
    @Test(expected = BadRequestException.class)
    public void testWrongServiceId() throws Exception {
        bindingService.bindServiceInstance(getSecurityContext(), SERVICE_INSTANCE_ID, BINDING_ID, new BindRequest(UUID.randomUUID(), QUEUE_PLAN_ID));
    }

    @Ignore("Not implemented yet")
    @Test(expected = BadRequestException.class)
    public void testWrongPlanId() throws Exception {
        bindingService.bindServiceInstance(getSecurityContext(), SERVICE_INSTANCE_ID, BINDING_ID, new BindRequest(QUEUE_SERVICE_ID, UUID.randomUUID()));
    }

    @Ignore("bindings aren't persisted yet, so we can't do this yet")
    @Test
    public void testBindTwiceWithDifferentPrameters() throws Exception {
        bindingService.bindServiceInstance(getSecurityContext(), SERVICE_INSTANCE_ID, BINDING_ID, new BindRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID));

        String otherServiceId = UUID.randomUUID().toString();
        provisionService(otherServiceId);

        exceptionGrabber.expect(ConflictException.class);
        bindingService.bindServiceInstance(getSecurityContext(), otherServiceId, BINDING_ID, new BindRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID));
    }

    @Ignore("bindings aren't persisted yet, so we can't do this yet")
    @Test
    public void testBindTwiceWithSameParameters() throws Exception {
        bindingService.bindServiceInstance(getSecurityContext(), SERVICE_INSTANCE_ID, BINDING_ID, new BindRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID));

        Response response = bindingService.bindServiceInstance(getSecurityContext(), SERVICE_INSTANCE_ID, BINDING_ID, new BindRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID));
        assertThat(response.getStatus(), is(HttpResponseCodes.SC_OK));
    }

    @Test
    public void testUnbind() throws Exception {
        bindingService.bindServiceInstance(getSecurityContext(), SERVICE_INSTANCE_ID, BINDING_ID, new BindRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID));

        Response response = bindingService.unbindServiceInstance(getSecurityContext(), SERVICE_INSTANCE_ID, BINDING_ID);
        assertThat(response.getStatus(), is(HttpResponseCodes.SC_OK));
    }

    @Ignore("bindings aren't persisted yet, so we can't do this yet. OSB spec mandates the broker MUST return Gone, when binding doesn't exist")
    @Test(expected = GoneException.class)
    public void testUnbindNonexistingBinding() throws Exception {
        bindingService.unbindServiceInstance(getSecurityContext(), SERVICE_INSTANCE_ID, UUID.randomUUID().toString());
    }

    @Ignore("bindings aren't persisted yet, so we can't do this yet. OSB spec mandates the broker MUST return Gone, when binding doesn't exist")
    @Test
    public void testUnbindTwice() throws Exception {
        bindingService.bindServiceInstance(getSecurityContext(), SERVICE_INSTANCE_ID, BINDING_ID, new BindRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID));
        bindingService.unbindServiceInstance(getSecurityContext(), SERVICE_INSTANCE_ID, BINDING_ID);

        exceptionGrabber.expect(GoneException.class);
        bindingService.unbindServiceInstance(getSecurityContext(), SERVICE_INSTANCE_ID, BINDING_ID);
    }

}
