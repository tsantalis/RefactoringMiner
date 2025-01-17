/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.standard;

import io.enmasse.address.model.Address;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.openshift.client.ParameterValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class TemplateBrokerSetGeneratorTest {
    private Kubernetes kubernetes;
    private StandardControllerSchema standardControllerSchema;
    private BrokerSetGenerator generator;

    @BeforeEach
    public void setUp() {
        kubernetes = mock(Kubernetes.class);

        standardControllerSchema = new StandardControllerSchema();
        generator = new TemplateBrokerSetGenerator(kubernetes, new StandardControllerOptions());
    }

    @Test
    public void testDirect() throws Exception {
        Address dest = createAddress("foo_bar_FOO", "anycast");
        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);
        BrokerCluster clusterList = generateCluster(dest, captor);
        List<HasMetadata> resources = clusterList.getResources().getItems();
        assertThat(resources.size(), is(1));
        List<ParameterValue> parameters = captor.getAllValues();
        assertThat(parameters.size(), is(13));
    }

    @Test
    public void testStoreAndForward() throws Exception {
        Address dest = createAddress("foo.bar", "queue");
        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);
        BrokerCluster clusterList = generateCluster(dest, captor);
        List<HasMetadata> resources = clusterList.getResources().getItems();
        assertThat(resources.size(), is(1));
        for (HasMetadata resource : resources) {
            Map<String, String> annotations = resource.getMetadata().getAnnotations();
            assertNotNull(annotations.get(AnnotationKeys.CLUSTER_ID));
            assertThat(annotations.get(AnnotationKeys.CLUSTER_ID), is(dest.getName()));
        }
        List<ParameterValue> parameters = captor.getAllValues();
        assertThat(parameters.size(), is(13));
    }

    private Address createAddress(String address, String type) {
        return new Address.Builder()
                .setName(address)
                .setAddress(address)
                .setAddressSpace("myinstance")
                .setType(type)
                .setPlan("plan1")
                .build();
    }

    private BrokerCluster generateCluster(Address address, ArgumentCaptor<ParameterValue> captor) throws Exception {
        when(kubernetes.processTemplate(anyString(), captor.capture())).thenReturn(new KubernetesListBuilder().addNewConfigMapItem().withNewMetadata().withName("testmap").endMetadata().endConfigMapItem().build());

        return generator.generateCluster(address.getName(), 1, address, null,
                standardControllerSchema.getSchema().findAddressSpaceType("standard").map(type -> (StandardInfraConfig) type.findInfraConfig("cfg1").orElse(null)).orElse(null));
    }

}
