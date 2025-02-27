/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.api;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.osb.api.bind.OSBBindingService;
import io.enmasse.osb.api.lastoperation.OSBLastOperationService;
import io.enmasse.osb.api.provision.ConsoleProxy;
import io.enmasse.osb.api.provision.OSBProvisioningService;
import io.enmasse.osb.api.provision.ProvisionRequest;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.ExpectedExceptionSupport;
import org.junit.rules.ExpectedException;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */

@ExtendWith(ExpectedExceptionSupport.class)
public class OSBTestBase {
    protected static final UUID QUEUE_SERVICE_ID = ServiceType.QUEUE.uuid();
    protected static final UUID TOPIC_SERVICE_ID = ServiceType.TOPIC.uuid();
    protected static final UUID QUEUE_PLAN_ID = UUID.fromString("3c7f4fdc-0597-11e8-abdb-507b9def37d9");
    protected static final UUID TOPIC_PLAN_ID = UUID.fromString("48837510-0597-11e8-8517-507b9def37d9");
    protected static final String SERVICE_INSTANCE_ID = UUID.randomUUID().toString();
    protected static final String ORGANIZATION_ID = UUID.randomUUID().toString();
    protected static final String SPACE_ID = UUID.randomUUID().toString();

    @Rule
    public ExpectedException exceptionGrabber = ExpectedException.none();

    protected OSBProvisioningService provisioningService;
    protected TestAddressSpaceApi addressSpaceApi;
    protected OSBBindingService bindingService;
    protected OSBLastOperationService lastOperationService;

    @BeforeEach
    public void setup() throws Exception {
        addressSpaceApi = new TestAddressSpaceApi();
        String brokerId = "myspace";
        provisioningService = new OSBProvisioningService(addressSpaceApi, null, null, new ConsoleProxy() {
            @Override
            public String getConsoleUrl(AddressSpace addressSpace) {
                return "http://localhost/console/" + addressSpace.getName();
            }
        });
        bindingService = new OSBBindingService(addressSpaceApi, null, null, null);
        lastOperationService = new OSBLastOperationService(addressSpaceApi, null, null);
    }

    protected void provisionService(String serviceInstanceId) throws Exception {
        provisionService(serviceInstanceId, ORGANIZATION_ID, SPACE_ID);
    }

    protected SecurityContext getSecurityContext() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.isUserInRole(any())).thenReturn(true);
        when(securityContext.isSecure()).thenReturn(true);
        when(securityContext.getUserPrincipal()).thenReturn(new BasicUserPrincipal("myuser"));
        return securityContext;
    }

    protected String provisionService(String serviceInstanceId, String organizationId, String spaceId) throws Exception {
        ProvisionRequest provisionRequest = new ProvisionRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID, organizationId, spaceId);
        provisionRequest.putParameter("name", "my-queue");
        provisionRequest.putParameter("group", "my-group");

        UriInfo uriInfo = mock(UriInfo.class);
        provisioningService.provisionService(getSecurityContext(), null, serviceInstanceId, true, provisionRequest);
        // TODO: wait for provisioning to finish (poll lastOperation endpoint)
        return serviceInstanceId;
    }
}
