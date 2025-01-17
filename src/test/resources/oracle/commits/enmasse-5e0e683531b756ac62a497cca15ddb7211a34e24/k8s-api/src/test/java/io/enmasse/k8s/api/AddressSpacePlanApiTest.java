/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AddressSpacePlanList;
import io.enmasse.admin.model.v1.AdminCrd;
import io.enmasse.admin.model.v1.DoneableAddressSpacePlan;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.ExternalResourceSupport;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;


@ExtendWith(ExternalResourceSupport.class)
public class AddressSpacePlanApiTest {

    @Rule
    public OpenShiftServer openShiftServer = new OpenShiftServer(false, true);

    @Test
    public void testNotifiesExisting() throws Exception {
        NamespacedOpenShiftClient client = openShiftServer.getOpenshiftClient();
        CustomResourceDefinition crd = AdminCrd.addressSpacePlans();
        AddressSpacePlanApi addressSpacePlanApi = new KubeAddressSpacePlanApi(client, client.getNamespace(), crd);

        client.customResources(crd, AddressSpacePlan.class, AddressSpacePlanList.class, DoneableAddressSpacePlan.class)
                .createNew()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("plan1")
                        .withNamespace(client.getNamespace())
                        .build())
                .withAddressSpaceType("standard")
                .withAddressPlans(Arrays.asList("p1", "p2"))
                .done();

        CompletableFuture<List<AddressSpacePlan>> promise = new CompletableFuture<>();
        try (Watch watch = addressSpacePlanApi.watchAddressSpacePlans(items -> {
            if (!items.isEmpty()) {
                promise.complete(items);
            }
        }, Duration.ofMinutes(1))) {
            List<AddressSpacePlan> list = promise.get(30, TimeUnit.SECONDS);
            assertEquals(1, list.size());
            assertEquals("plan1", list.get(0).getMetadata().getName());
        }
    }

    @Test
    public void testNotifiesCreated() throws Exception {
        NamespacedOpenShiftClient client = openShiftServer.getOpenshiftClient();
        CustomResourceDefinition crd = AdminCrd.addressSpacePlans();
        AddressSpacePlanApi addressSpacePlanApi = new KubeAddressSpacePlanApi(client, client.getNamespace(), crd);

        CompletableFuture<List<AddressSpacePlan>> promise = new CompletableFuture<>();
        try (Watch watch = addressSpacePlanApi.watchAddressSpacePlans(items -> {
            if (!items.isEmpty()) {
                promise.complete(items);
            }

        }, Duration.ofSeconds(2))) {
            client.customResources(crd, AddressSpacePlan.class, AddressSpacePlanList.class, DoneableAddressSpacePlan.class)
                .createNew()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("plan1")
                        .withNamespace(client.getNamespace())
                        .build())
                .withAddressSpaceType("standard")
                .withAddressPlans(Arrays.asList("p1", "p2"))
                .done();

            List<AddressSpacePlan> list = promise.get(30, TimeUnit.SECONDS);
            assertEquals(1, list.size());
            assertEquals("plan1", list.get(0).getMetadata().getName());
        }
    }
}
