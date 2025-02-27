/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package enmasse.discovery;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class DiscoveryTest {

    @Test
    public void testDiscovery() throws Exception {
        Map<String, String> expectedLabelFilter = Collections.singletonMap("my", "key");
        Map<String, String> expectedAnnotationFilter = Collections.singletonMap("my", "annotation");
        CompletableFuture<Set<Host>> changedHosts = new CompletableFuture<>();
        KubernetesClient kubeClient = mock(KubernetesClient.class);

        System.out.println("Deploying server verticle");
        DiscoveryClient client = new DiscoveryClient(kubeClient, expectedLabelFilter, expectedAnnotationFilter, null);
        client.addListener(changedHosts::complete);

        System.out.println("Waiting for subscriber to be created");
        client.resourcesUpdated(Collections.singletonList(createPod("False", "Pending")));

        System.out.println("Sending second response");
        client.resourcesUpdated(Collections.singletonList(createPod("False", "Running")));
        try {
            changedHosts.get(10, TimeUnit.SECONDS);
            fail("Ready must be true before returning host");
        } catch (TimeoutException ignored) {
        }

        System.out.println("Sending third response");
        client.resourcesUpdated(Collections.singletonList(createPod("True", "Running")));
        try {
            Set<Host> actual = changedHosts.get(2, TimeUnit.MINUTES);
            assertEquals(actual.size(), 1);
            Host actualHost = actual.iterator().next();
            assertEquals(actualHost.getHostname(), "10.0.0.1");
        } catch (Exception e) {
            fail("Unexpected exception" + e.getMessage());
            e.printStackTrace();
        }
    }

    public enmasse.discovery.Pod createPod(String ready, String phase) {
       return new enmasse.discovery.Pod(new PodBuilder()
               .editOrNewMetadata()
               .withName("mypod")
               .withLabels(Collections.singletonMap("my", "key"))
               .withAnnotations(Collections.singletonMap("my", "annotation"))
               .endMetadata()
               .editOrNewStatus()
               .withPhase(phase)
               .withPodIP("10.0.0.1")
               .addNewCondition()
               .withType("Ready")
               .withStatus(ready)
               .endCondition()
               .endStatus()
               .withNewSpec()
               .addToContainers(new ContainerBuilder()
                       .withName("c")
                       .addToPorts(new ContainerPortBuilder()
                               .withName("http")
                               .withContainerPort(1234)
                               .build())
                       .build())
               .endSpec()
               .build());
    }
}
