/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.KubeUtil;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.admin.model.v1.NetworkPolicy;
import io.enmasse.admin.model.v1.NetworkPolicyBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyEgressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyIngressRuleBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.ExternalResourceSupport;

import java.util.Collections;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(ExternalResourceSupport.class)
public class NetworkPolicyControllerTest {
    private static final ObjectMapper mapper = new ObjectMapper();
    private OpenShiftClient client;

    @Rule
    public OpenShiftServer openShiftServer = new OpenShiftServer(false, true);

    @BeforeEach
    public void setup() {
        client = openShiftServer.getOpenshiftClient();
    }

    @Test
    public void testCreateFromInfraConfig() throws Exception {
        InfraConfig infraConfig = createTestInfra(createTestPolicy("my", "label"));
        AddressSpace addressSpace = createTestSpace(infraConfig, null);

        NetworkPolicyController controller = new NetworkPolicyController(client, new TestSchemaProvider());
        controller.handle(addressSpace);

        assertEquals(1, client.network().networkPolicies().list().getItems().size());
        io.fabric8.kubernetes.api.model.networking.NetworkPolicy networkPolicy = client.network().networkPolicies().withName(KubeUtil.getNetworkPolicyName(addressSpace)).get();
        assertNotNull(networkPolicy);
        assertEquals("enmasse", networkPolicy.getMetadata().getLabels().get(LabelKeys.APP));
        assertEquals("1234", networkPolicy.getMetadata().getLabels().get(LabelKeys.INFRA_UUID));
        assertThat(networkPolicy.getSpec().getPolicyTypes(), hasItem("Ingress"));
        assertEquals("label", networkPolicy.getSpec().getIngress().get(0).getFrom().get(0).getPodSelector().getMatchLabels().get("my"));
    }

    @Test
    public void testCreateFromAddressSpaceConfig() throws Exception {
        InfraConfig infraConfig = createTestInfra(null);
        AddressSpace addressSpace = createTestSpace(infraConfig, createTestPolicy("my", "label"));

        NetworkPolicyController controller = new NetworkPolicyController(client, new TestSchemaProvider());
        controller.handle(addressSpace);

        assertEquals(1, client.network().networkPolicies().list().getItems().size());
        io.fabric8.kubernetes.api.model.networking.NetworkPolicy networkPolicy = client.network().networkPolicies().withName(KubeUtil.getNetworkPolicyName(addressSpace)).get();
        assertNotNull(networkPolicy);
        assertEquals("enmasse", networkPolicy.getMetadata().getLabels().get(LabelKeys.APP));
        assertEquals("1234", networkPolicy.getMetadata().getLabels().get(LabelKeys.INFRA_UUID));
        assertThat(networkPolicy.getSpec().getPolicyTypes(), hasItem("Ingress"));
        assertEquals("label", networkPolicy.getSpec().getIngress().get(0).getFrom().get(0).getPodSelector().getMatchLabels().get("my"));
    }

    @Test
    public void testAddressSpaceOverridesInfra() throws Exception {
        InfraConfig infraConfig = createTestInfra(createTestPolicy("my", "label"));
        AddressSpace addressSpace = createTestSpace(infraConfig, createTestPolicy("my", "overridden"));

        NetworkPolicyController controller = new NetworkPolicyController(client, new TestSchemaProvider());
        controller.handle(addressSpace);

        assertEquals(1, client.network().networkPolicies().list().getItems().size());
        io.fabric8.kubernetes.api.model.networking.NetworkPolicy networkPolicy = client.network().networkPolicies().withName(KubeUtil.getNetworkPolicyName(addressSpace)).get();
        assertNotNull(networkPolicy);
        assertEquals("enmasse", networkPolicy.getMetadata().getLabels().get(LabelKeys.APP));
        assertEquals("1234", networkPolicy.getMetadata().getLabels().get(LabelKeys.INFRA_UUID));
        assertEquals("type1", networkPolicy.getMetadata().getLabels().get(LabelKeys.INFRA_TYPE));
        assertThat(networkPolicy.getSpec().getPolicyTypes(), hasItem("Ingress"));
        assertEquals("overridden", networkPolicy.getSpec().getIngress().get(0).getFrom().get(0).getPodSelector().getMatchLabels().get("my"));
    }

    @Test
    public void testUpdatesWhenChanged() throws Exception {
        InfraConfig infraConfig = createTestInfra(null);
        AddressSpace addressSpace = createTestSpace(infraConfig,
                createTestPolicy("my", "label1"));

        NetworkPolicyController controller = new NetworkPolicyController(client, new TestSchemaProvider());
        controller.handle(addressSpace);

        assertEquals(1, client.network().networkPolicies().list().getItems().size());
        io.fabric8.kubernetes.api.model.networking.NetworkPolicy networkPolicy = client.network().networkPolicies().withName(KubeUtil.getNetworkPolicyName(addressSpace)).get();
        assertNotNull(networkPolicy);
        assertThat(networkPolicy.getSpec().getPolicyTypes(), hasItem("Ingress"));
        assertEquals("label1", networkPolicy.getSpec().getIngress().get(0).getFrom().get(0).getPodSelector().getMatchLabels().get("my"));

        addressSpace = createTestSpace(infraConfig, createTestPolicy("my", "label2"));
        controller.handle(addressSpace);

        assertEquals(1, client.network().networkPolicies().list().getItems().size());
        networkPolicy = client.network().networkPolicies().withName(KubeUtil.getNetworkPolicyName(addressSpace)).get();
        assertNotNull(networkPolicy);
        assertThat(networkPolicy.getSpec().getPolicyTypes(), hasItem("Ingress"));
        assertEquals("label2", networkPolicy.getSpec().getIngress().get(0).getFrom().get(0).getPodSelector().getMatchLabels().get("my"));

        addressSpace = createTestSpace(infraConfig, createTestPolicy("my", "label2", "other", "label3"));
        controller.handle(addressSpace);
        networkPolicy = client.network().networkPolicies().withName(KubeUtil.getNetworkPolicyName(addressSpace)).get();
        assertNotNull(networkPolicy);
        assertThat(networkPolicy.getSpec().getPolicyTypes(), hasItem("Ingress"));
        assertThat(networkPolicy.getSpec().getPolicyTypes(), hasItem("Egress"));
        assertEquals("label2", networkPolicy.getSpec().getIngress().get(0).getFrom().get(0).getPodSelector().getMatchLabels().get("my"));
        assertEquals("label3", networkPolicy.getSpec().getEgress().get(0).getTo().get(0).getPodSelector().getMatchLabels().get("other"));
    }

    @Test
    public void testDeletesWhenRemoved() throws Exception {
        InfraConfig infraConfig = createTestInfra(null);
        AddressSpace addressSpace = createTestSpace(infraConfig, createTestPolicy("my", "label"));

        NetworkPolicyController controller = new NetworkPolicyController(client, new TestSchemaProvider());
        controller.handle(addressSpace);

        assertEquals(1, client.network().networkPolicies().list().getItems().size());
        io.fabric8.kubernetes.api.model.networking.NetworkPolicy networkPolicy = client.network().networkPolicies().withName(KubeUtil.getNetworkPolicyName(addressSpace)).get();
        assertNotNull(networkPolicy);
        assertThat(networkPolicy.getSpec().getPolicyTypes(), hasItem("Ingress"));
        assertEquals("label", networkPolicy.getSpec().getIngress().get(0).getFrom().get(0).getPodSelector().getMatchLabels().get("my"));

        addressSpace = createTestSpace(infraConfig, null);
        controller.handle(addressSpace);
        assertEquals(0, client.network().networkPolicies().list().getItems().size());
        networkPolicy = client.network().networkPolicies().withName(KubeUtil.getNetworkPolicyName(addressSpace)).get();
        assertNull(networkPolicy);
    }

    private NetworkPolicy createTestPolicy(String labelKey, String labelValue) {
        return new NetworkPolicyBuilder()
                .withIngress(Collections.singletonList(new NetworkPolicyIngressRuleBuilder()
                        .addNewFrom()
                        .withNewPodSelector()
                        .addToMatchLabels(labelKey, labelValue)
                        .endPodSelector()
                        .endFrom()
                        .build()))
                .build();
    }

    private NetworkPolicy createTestPolicy(String ingressLabelKey, String ingressLabelValue, String egressLabelKey, String egressLabelValue) {
        return new NetworkPolicyBuilder()
                .withIngress(Collections.singletonList(new NetworkPolicyIngressRuleBuilder()
                        .addNewFrom()
                        .withNewPodSelector()
                        .addToMatchLabels(ingressLabelKey, ingressLabelValue)
                        .endPodSelector()
                        .endFrom()
                        .build()))
                .withEgress(Collections.singletonList(new NetworkPolicyEgressRuleBuilder()
                        .addNewTo()
                        .withNewPodSelector()
                        .addToMatchLabels(egressLabelKey, egressLabelValue)
                        .endPodSelector()
                        .endTo()
                        .build()))
                .build();
    }

    private InfraConfig createTestInfra(NetworkPolicy networkPolicy) throws JsonProcessingException {
        return new StandardInfraConfigBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("test")
                        .build())
                .withNewSpec()
                .withNetworkPolicy(networkPolicy)
                .endSpec()
                .build();
    }

    private AddressSpace createTestSpace(InfraConfig infraConfig, NetworkPolicy networkPolicy) throws JsonProcessingException {
        return new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("ns")
                .setType("type1")
                .setPlan("plan1")
                .setNetworkPolicy(networkPolicy)
                .putAnnotation(AnnotationKeys.INFRA_UUID, "1234")
                .putAnnotation(AnnotationKeys.APPLIED_INFRA_CONFIG, mapper.writeValueAsString(infraConfig))
                .build();
    }
}
