/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.standard;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.Status;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.metrics.api.Metrics;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

import static org.mockito.Mockito.*;

public class AddressControllerTest {
    private Kubernetes mockHelper;
    private AddressApi mockApi;
    private AddressController controller;
    private OpenShiftClient mockClient;
    private BrokerSetGenerator mockGenerator;

    @BeforeEach
    public void setUp() throws IOException {
        mockHelper = mock(Kubernetes.class);
        mockGenerator = mock(BrokerSetGenerator.class);
        mockApi = mock(AddressApi.class);
        mockClient = mock(OpenShiftClient.class);
        EventLogger eventLogger = mock(EventLogger.class);
        StandardControllerSchema standardControllerSchema = new StandardControllerSchema();
        when(mockHelper.getRouterCluster()).thenReturn(new RouterCluster("qdrouterd", 1, null));
        StandardControllerOptions options = new StandardControllerOptions();
        options.setAddressSpace("me1");
        options.setAddressSpacePlanName("plan1");
        options.setResyncInterval(Duration.ofSeconds(5));
        options.setVersion("1.0");
        controller = new AddressController(options, mockApi, mockHelper, mockGenerator, eventLogger, standardControllerSchema, new Metrics());
    }

    @Test
    public void testAddressGarbageCollection() throws Exception {
        Address alive = new Address.Builder()
                .setName("q1")
                .setAddress("q1")
                .setAddressSpace("myspace")
                .setNamespace("ns")
                .setType("queue")
                .setPlan("small-queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .setStatus(new Status(true).setPhase(Status.Phase.Active))
                .build();
        Address terminating = new Address.Builder()
                .setName("q2")
                .setAddress("q2")
                .setAddressSpace("myspace")
                .setNamespace("ns")
                .setType("queue")
                .setPlan("small-queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .setStatus(new Status(false).setPhase(Status.Phase.Terminating))
                .build();
        when(mockHelper.listClusters()).thenReturn(Arrays.asList(new BrokerCluster("broker", new KubernetesList())));
        controller.onUpdate(Arrays.asList(alive, terminating));
        verify(mockApi).deleteAddress(any());
        verify(mockApi).deleteAddress(eq(terminating));
    }

    @Test
    public void testDeleteUnusedClusters() throws Exception {
        Address alive = new Address.Builder()
                .setName("q1")
                .setAddress("q1")
                .setAddressSpace("myspace")
                .setNamespace("ns")
                .setType("queue")
                .setPlan("small-queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .putAnnotation(AnnotationKeys.CLUSTER_ID, "broker")
                .setStatus(new Status(true).setPhase(Status.Phase.Active))
                .build();

        KubernetesList oldList = new KubernetesListBuilder()
                .addToConfigMapItems(new ConfigMapBuilder()
                        .withNewMetadata()
                        .withName("mymap")
                        .endMetadata()
                        .build())
                .build();
        when(mockHelper.listClusters()).thenReturn(Arrays.asList(
                new BrokerCluster("broker", new KubernetesList()),
                new BrokerCluster("unused", oldList)));

        controller.onUpdate(Arrays.asList(alive));

        verify(mockHelper).delete(any());
        verify(mockHelper).delete(eq(oldList));
    }
}
