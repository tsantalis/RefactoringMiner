/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesList;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CreateControllerTest {

    @Test
    public void testAddressSpaceCreate() throws Exception {
        Kubernetes kubernetes = mock(Kubernetes.class);
        when(kubernetes.getNamespace()).thenReturn("otherspace");

        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setUid(UUID.randomUUID().toString())
                .setNamespace("mynamespace")
                .setType("type1")
                .setPlan("myplan")
                .build();


        EventLogger eventLogger = mock(EventLogger.class);
        InfraResourceFactory mockResourceFactory = mock(InfraResourceFactory.class);
        when(mockResourceFactory.createInfraResources(eq(addressSpace), any())).thenReturn(Arrays.asList(new ConfigMapBuilder()
                .editOrNewMetadata()
                .withName("mymap")
                .endMetadata()
                .build()));

        SchemaProvider testSchema = new TestSchemaProvider();
        CreateController createController = new CreateController(kubernetes, testSchema, mockResourceFactory, eventLogger, null, "1.0", new TestAddressSpaceApi());

        createController.handle(addressSpace);

        ArgumentCaptor<KubernetesList> resourceCaptor = ArgumentCaptor.forClass(KubernetesList.class);
        verify(kubernetes).create(resourceCaptor.capture());
        KubernetesList value = resourceCaptor.getValue();
        assertThat(value.getItems().size(), is(1));
    }
}
