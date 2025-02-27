/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;


import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceStatus;
import io.enmasse.controller.common.Kubernetes;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatusControllerTest {

    @Test
    public void testStatusControllerSetsNotReady() throws Exception {
        InfraResourceFactory infraResourceFactory = mock(InfraResourceFactory.class);
        Kubernetes kubernetes = mock(Kubernetes.class);

        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName("mydepl1")
                .endMetadata()
                .withNewStatus()
                .withUnavailableReplicas(1)
                .withAvailableReplicas(0)
                .endStatus()
                .build();

        when(kubernetes.getReadyDeployments()).thenReturn(Collections.emptySet());

        StatusController controller = new StatusController(kubernetes, new TestSchemaProvider(), infraResourceFactory, null);

        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setType("type1")
                .setPlan("myplan")
                .build();

        when(infraResourceFactory.createInfraResources(eq(addressSpace), any())).thenReturn(Collections.singletonList(deployment));

        assertFalse(addressSpace.getStatus().isReady());
        controller.handle(addressSpace);
        assertFalse(addressSpace.getStatus().isReady());
    }
}
