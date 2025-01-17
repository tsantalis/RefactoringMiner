/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressResolver;
import io.enmasse.address.model.AddressSpaceResolver;
import io.enmasse.address.model.Status;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.EventLogger;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.*;
import java.util.function.Consumer;

import static io.enmasse.address.model.Status.Phase.Configuring;
import static io.enmasse.address.model.Status.Phase.Pending;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AddressProvisionerTest {
    private BrokerSetGenerator generator;
    private Kubernetes kubernetes;

    @BeforeEach
    public void setup() {
        generator = mock(BrokerSetGenerator.class);
        kubernetes = mock(Kubernetes.class);
    }

    private AddressProvisioner createProvisioner() {
        StandardControllerSchema standardControllerSchema = new StandardControllerSchema();
        AddressResolver resolver = new AddressResolver(standardControllerSchema.getType());
        AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(standardControllerSchema.getSchema());
        EventLogger logger = mock(EventLogger.class);

        return new AddressProvisioner(addressSpaceResolver, resolver, standardControllerSchema.getPlan(), generator, kubernetes, logger, "1234");
    }

    private AddressProvisioner createProvisioner(List<ResourceAllowance> resourceAllowances) {
        StandardControllerSchema standardControllerSchema = new StandardControllerSchema(resourceAllowances);
        AddressResolver resolver = new AddressResolver(standardControllerSchema.getType());
        AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(standardControllerSchema.getSchema());
        EventLogger logger = mock(EventLogger.class);

        return new AddressProvisioner(addressSpaceResolver, resolver, standardControllerSchema.getPlan(), generator, kubernetes, logger, "1234");
    }

    @Test
    public void testUsageCheck() {
        Set<Address> addresses = new HashSet<>();
        addresses.add(new Address.Builder()
                .setAddress("a1")
                .setAddressSpace("myspace")
                .setNamespace("ns")
                .setPlan("small-anycast")
                .setType("anycast")
                .build());
        AddressProvisioner provisioner = createProvisioner();
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        assertThat(usageMap.size(), is(1));
        assertThat(usageMap.get("router").size(), is(1));
        assertEquals(0.2, usageMap.get("router").get("all").getUsed(), 0.01);

        addresses.add(new Address.Builder()
                .setAddress("q1")
                .setAddressSpace("myspace")
                .setNamespace("ns")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .build());

        usageMap = provisioner.checkUsage(addresses);

        assertThat(usageMap.size(), is(2));
        assertThat(usageMap.get("router").size(), is(1));
        assertThat(usageMap.get("broker").size(), is(1));
        assertEquals(0.4, usageMap.get("router").get("all").getUsed(), 0.01);
        assertEquals(0.4, usageMap.get("broker").get("broker-0").getUsed(), 0.01);

        addresses.add(new Address.Builder()
                .setAddress("q2")
                .setAddressSpace("myspace")
                .setNamespace("ns")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .build());

        usageMap = provisioner.checkUsage(addresses);

        assertThat(usageMap.size(), is(2));
        assertThat(usageMap.get("router").size(), is(1));
        assertThat(usageMap.get("broker").size(), is(1));
        assertEquals(0.6, usageMap.get("router").get("all").getUsed(), 0.01);
        assertEquals(0.8, usageMap.get("broker").get("broker-0").getUsed(), 0.01);
    }

    @Test
    public void testQuotaCheck() {
        Set<Address> addresses = new HashSet<>();
        addresses.add(createQueue("q1", "small-queue").putAnnotation(AnnotationKeys.BROKER_ID, "broker-pooled-1234-0"));
        addresses.add(createQueue("q2", "small-queue").putAnnotation(AnnotationKeys.BROKER_ID, "broker-pooled-1234-0"));
        addresses.add(createQueue("q3", "small-queue").putAnnotation(AnnotationKeys.BROKER_ID, "broker-pooled-1234-1"));

        AddressProvisioner provisioner = createProvisioner();
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        Address largeQueue = createQueue("q4", "xlarge-queue");
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Sets.newSet(largeQueue), Sets.newSet(largeQueue));

        assertThat(neededMap, is(usageMap));
        assertThat(largeQueue.getStatus().getPhase(), is(Pending));

        Address smallQueue = createQueue("q4", "small-queue");
        neededMap = provisioner.checkQuota(usageMap, Sets.newSet(smallQueue), Sets.newSet(smallQueue));

        assertThat(neededMap, is(not(usageMap)));
    }

    @Test
    public void testQuotaCheckMany() {
        Map<String, Address> addresses = new HashMap<>();
        for (int i = 0; i < 200; i++) {
            addresses.put("a" + i, createAddress("a" + i, "anycast", "small-anycast"));
        }


        AddressProvisioner provisioner = createProvisioner();

        Map<String, Map<String, UsageInfo>> usageMap = new HashMap<>();
        Map<String, Map<String, UsageInfo>> provisionMap = provisioner.checkQuota(usageMap, new LinkedHashSet<>(addresses.values()), new LinkedHashSet<>(addresses.values()));

        assertThat(provisionMap.get("router").get("all").getNeeded(), is(1));
        int numConfiguring = 0;
        for (Address address : addresses.values()) {
            if (address.getStatus().getPhase().equals(Configuring)) {
                numConfiguring++;
            }
        }
        assertThat(numConfiguring, is(5));
    }

    @Test
    public void testProvisioningColocated() {
        Set<Address> addresses = new HashSet<>();
        addresses.add(createAddress("a1", "anycast", "small-anycast"));
        addresses.add(createAddress("q1", "queue", "small-queue").putAnnotation(AnnotationKeys.BROKER_ID, "broker-0"));


        AddressProvisioner provisioner = createProvisioner();
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        Address queue = createAddress("q2", "queue", "small-queue");
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Sets.newSet(queue), Sets.newSet(queue));

        List<BrokerCluster> clusterList = Arrays.asList(new BrokerCluster("broker-pooled-1234", new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), clusterList, neededMap, Sets.newSet(queue));

        assertThat(clusterList.get(0).getResources().getItems().size(), is(0));
        assertTrue(queue.getStatus().getMessages().isEmpty(), queue.getStatus().getMessages().toString());
        assertThat(queue.getStatus().getPhase(), is(Status.Phase.Configuring));
        assertThat(queue.getAnnotations().get(AnnotationKeys.BROKER_ID), is("broker-pooled-1234-0"));
    }

    private static RouterCluster createDeployment(int replicas) {
        return new RouterCluster("router", replicas, null);
    }

    @Test
    public void testScalingColocated() {
        Set<Address> addresses = new HashSet<>();
        addresses.add(createAddress("a1", "anycast", "small-anycast"));
        addresses.add(createAddress("q1", "queue", "small-queue").putAnnotation(AnnotationKeys.BROKER_ID, "broker-pooled-1234-0"));
        addresses.add(createAddress("q2", "queue", "small-queue").putAnnotation(AnnotationKeys.BROKER_ID, "broker-pooled-1234-0"));

        AddressProvisioner provisioner = createProvisioner();
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        Address queue = createAddress("q3", "queue", "small-queue");
        Map<String, Map<String, UsageInfo>> provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(queue), Sets.newSet(queue));

        List<BrokerCluster> clusterList = Arrays.asList(new BrokerCluster("broker-pooled-1234", new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), clusterList, provisionMap, Sets.newSet(queue));
        verify(kubernetes).scaleStatefulSet(eq("broker-pooled-1234"), eq(2));

        assertTrue(queue.getStatus().getMessages().isEmpty(), queue.getStatus().getMessages().toString());
        assertThat(queue.getStatus().getPhase(), is(Status.Phase.Configuring));
        assertThat(queue.getAnnotations().get(AnnotationKeys.BROKER_ID), is("broker-pooled-1234-1"));
    }

    @Test
    public void testProvisionColocated() {
        AddressProvisioner provisioner = createProvisioner(Arrays.asList(
                new ResourceAllowance("broker", 2),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 2)));

        Set<Address> addressSet = Sets.newSet(
                createQueue("q9", "pooled-queue-tiny"),
                createQueue("q8", "pooled-queue-tiny"),
                createQueue("q11", "pooled-queue-tiny"),
                createQueue("q12", "pooled-queue-tiny"),
                createQueue("q10", "pooled-queue-tiny"),
                createQueue("q1", "pooled-queue-large"),
                createQueue("q7", "pooled-queue-tiny"),
                createQueue("q6", "pooled-queue-small"),
                createQueue("q5", "pooled-queue-small"),
                createQueue("q4", "pooled-queue-small"),
                createQueue("q3", "pooled-queue-small"),
                createQueue("q2", "pooled-queue-large"));

        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(Collections.emptySet());
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, addressSet, addressSet);

        assertThat(neededMap.keySet().size(), is(1));
        assertThat(AddressProvisioner.sumTotalNeeded(neededMap), is(2));

        List<BrokerCluster> brokerClusters = Arrays.asList(
                createCluster("broker-pooled-1234", 2));

        provisioner.provisionResources(new RouterCluster("router", 1, null), brokerClusters, neededMap, addressSet);

        for (Address address : addressSet) {
            assertThat(address.getStatus().getPhase(), is(Configuring));
        }
    }

    private BrokerCluster createCluster(String clusterId, int replicas) {
        KubernetesListBuilder builder = new KubernetesListBuilder();
        builder.addToStatefulSetItems(new StatefulSetBuilder()
                .editOrNewMetadata()
                .withName(clusterId)
                .endMetadata()
                .editOrNewSpec()
                .withReplicas(replicas)
                .endSpec()
                .build());
        return new BrokerCluster(clusterId, builder.build());
    }

    private Address createQueue(String address, String plan) {
        return createQueue(address, plan, null);
    }

    private Address createQueue(String address, String plan, Consumer<Map<String, String>> customizeAnnotations) {
        return createAddress(address, "queue", plan, customizeAnnotations);
    }

    private static Address createAddress(String address, String type, String plan) {
        return createAddress(address, type, plan, null);
    }

    private static Address createAddress(String address, String type, String plan, Consumer<Map<String, String>> customizeAnnotations) {
        final Address.Builder addressBuilder = new Address.Builder()
                .setName(address)
                .setAddress(address)
                .setAddressSpace("myspace")
                .setNamespace("ns")
                .setPlan(plan)
                .setType(type);

        if (customizeAnnotations != null) {
            final Map<String, String> annotations = new HashMap<>();
            customizeAnnotations.accept(annotations);
            addressBuilder.setAnnotations(annotations);
        }

        return addressBuilder.build();
    }

    private Address createSubscription(String address, String topic, String plan) {
        return new Address.Builder()
                .setAddress(address)
                .setAddressSpace("myspace")
                .setNamespace("ns")
                .setPlan(plan)
                .setType("subscription")
                .setTopic(topic)
                .build();
    }


    @Test
    public void testProvisioningSharded() throws Exception {
        Set<Address> addresses = new HashSet<>();
        addresses.add(createAddress("a1", "anycast", "small-anycast"));

        AddressProvisioner provisioner = createProvisioner(Arrays.asList(
                new ResourceAllowance("broker", 3),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 4)));
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        Address q1 = createQueue("q1", "xlarge-queue");
        Address q2 = createQueue("q2", "large-queue");
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Sets.newSet(q1, q2), Sets.newSet(q1, q2));

        when(generator.generateCluster(eq(provisioner.getShardedClusterId(q1)), anyInt(), eq(q1), any(), any())).thenReturn(new BrokerCluster(provisioner.getShardedClusterId(q1), new KubernetesList()));
        when(generator.generateCluster(eq(provisioner.getShardedClusterId(q2)), anyInt(), eq(q2), any(), any())).thenReturn(new BrokerCluster(provisioner.getShardedClusterId(q2), new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), new ArrayList<>(), neededMap, Sets.newSet(q1, q2));

        assertTrue(q1.getStatus().getMessages().isEmpty(), q1.getStatus().getMessages().toString());
        assertThat(q1.getStatus().getPhase(), is(Status.Phase.Configuring));
        assertNull(q1.getAnnotations().get(AnnotationKeys.BROKER_ID));
        verify(generator).generateCluster(eq(provisioner.getShardedClusterId(q1)), eq(2), eq(q1), any(), any());

        assertTrue(q2.getStatus().getMessages().isEmpty(), q2.getStatus().getMessages().toString());
        assertThat(q2.getStatus().getPhase(), is(Status.Phase.Configuring));
        assertNull(q2.getAnnotations().get(AnnotationKeys.BROKER_ID));
        verify(generator).generateCluster(eq(provisioner.getShardedClusterId(q2)), eq(1), eq(q2), any(), any());
    }

    @Test
    public void testProvisioningShardedWithClusterId() throws Exception {
        final Set<Address> addresses = new HashSet<>();
        addresses.add(createAddress("a1", "anycast", "small-anycast"));

        final AddressProvisioner provisioner = createProvisioner(Arrays.asList(
                new ResourceAllowance("broker", 3),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 4)));
        final Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        final String manualClusterId = "foobar";

        final Address q = createQueue("q1", "xlarge-queue", annotations -> {
            annotations.put(AnnotationKeys.CLUSTER_ID, manualClusterId);
        });

        final Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, singleton(q), singleton(q));

        when(generator.generateCluster(eq(provisioner.getShardedClusterId(q)), anyInt(), eq(q), any(), any()))
                .thenReturn(new BrokerCluster(provisioner.getShardedClusterId(q), new KubernetesList()));

        provisioner.provisionResources(createDeployment(1), new ArrayList<>(), neededMap, singleton(q));

        assertTrue(q.getStatus().getMessages().isEmpty(), q.getStatus().getMessages().toString());
        assertThat(q.getStatus().getPhase(), is(Status.Phase.Configuring));
        assertNull(q.getAnnotations().get(AnnotationKeys.BROKER_ID));

        verify(generator)
                .generateCluster(eq(manualClusterId), eq(2), eq(q), any(), any());
    }

    @Test
    public void testScalingRouter() {
        Set<Address> addresses = new HashSet<>();
        for (int i = 0; i < 199; i++) {
            addresses.add(createAddress("a" + i, "anycast", "small-anycast"));
        }


        AddressProvisioner provisioner = createProvisioner(Arrays.asList(
                new ResourceAllowance("broker", 0),
                new ResourceAllowance("router", 100000),
                new ResourceAllowance("aggregate", 100000)));

        Map<String, Map<String, UsageInfo>> usageMap = new HashMap<>();
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, addresses, addresses);

        provisioner.provisionResources(createDeployment(1), new ArrayList<>(), neededMap, addresses);

        verify(kubernetes, atLeast(1)).scaleStatefulSet(eq("router"), eq(40));
        verify(kubernetes, never()).scaleStatefulSet(eq("router"), eq(41));
    }

    @Test
    public void testDurableSubscriptionsColocated() {
        AddressProvisioner provisioner = createProvisioner(Arrays.asList(
                new ResourceAllowance("broker", 2),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 3)));

        Set<Address> addressSet = Sets.newSet(
                createAddress("t1", "topic", "small-topic"),
                createAddress("t2", "topic", "small-topic"),
                createSubscription("s1", "t1", "small-subscription"));


        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(Collections.emptySet());
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, addressSet, addressSet);

        assertThat(neededMap.keySet().size(), is(3));
        assertThat(AddressProvisioner.sumTotalNeeded(neededMap), is(2));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("router")), is(1));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("broker")), is(1));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("subscription")), is(1));

        for (Address address : addressSet) {
            assertThat(address.getStatus().getPhase(), is(Configuring));
        }
    }

    @Test
    public void testDurableSubscriptionsColocatedStaysOnTopicBroker() {
        AddressProvisioner provisioner = createProvisioner(Arrays.asList(
                new ResourceAllowance("broker", 2),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 3)));

        Set<Address> addressSet = Sets.newSet(
                createAddress("t1", "topic", "small-topic"),
                createSubscription("s1", "t1", "small-subscription"),
                createSubscription("s2", "t1", "small-subscription"),
                createSubscription("s3", "t1", "small-subscription"),
                createSubscription("s4", "t1", "small-subscription"),
                createSubscription("s5", "t1", "small-subscription"),
                createSubscription("s6", "t1", "small-subscription"),
                createSubscription("s7", "t1", "small-subscription"),
                createSubscription("s8", "t1", "small-subscription"),
                createSubscription("s9", "t1", "small-subscription"),
                createSubscription("s10", "t1", "small-subscription"),
                createSubscription("s11", "t1", "small-subscription"),
                createSubscription("s12", "t1", "small-subscription"));

        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(Collections.emptySet());
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, addressSet, addressSet);

        assertThat(AddressProvisioner.sumTotalNeeded(neededMap), is(2));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("router")), is(1));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("broker")), is(1));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("subscription")), is(1));

        Set<Address> configured = new HashSet<Address>();
        Set<Address> unConfigured = new HashSet<Address>();


        for (Address address : addressSet) {
            if (address.getStatus().getPhase().equals(Pending)) {
                unConfigured.add(address);
            } else if (address.getStatus().getPhase().equals(Configuring)) {
                configured.add(address);
            }
        }
        assertEquals(2, unConfigured.size());
        assertEquals(11, configured.size(), "contains topic + 10 subscriptions");
        Iterator<Address> unconfiguredIterator = unConfigured.iterator();
        assertFalse(configured.contains(unconfiguredIterator.next()));
        assertFalse(configured.contains(unconfiguredIterator.next()));
    }

    @Test
    public void testDurableSubscriptionsSharded() throws Exception {
        AddressProvisioner provisioner = createProvisioner(Arrays.asList(
                new ResourceAllowance("broker", 2),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 3)));

        Address t1 = createAddress("t1", "topic", "xlarge-topic");
        Address t2 = createAddress("t2", "topic", "xlarge-topic");
        Address s1 = createSubscription("s1", "t1", "small-subscription");
        Set<Address> addressSet = Sets.newSet(
                t1,
                t2,
                s1);

        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(Collections.emptySet());
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, addressSet, addressSet);

        assertThat(neededMap.keySet().size(), is(3));
        assertThat(AddressProvisioner.sumTotalNeeded(neededMap), is(3));

        List<BrokerCluster> brokerClusters = new ArrayList<BrokerCluster>(Arrays.asList(createCluster("broker", 1)));

        when(generator.generateCluster(eq(provisioner.getShardedClusterId(t1)), anyInt(), eq(t1), any(), any())).thenReturn(new BrokerCluster(provisioner.getShardedClusterId(t1), new KubernetesList()));
        when(generator.generateCluster(eq(provisioner.getShardedClusterId(t2)), anyInt(), eq(t2), any(), any())).thenReturn(new BrokerCluster(provisioner.getShardedClusterId(t2), new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), brokerClusters, neededMap, addressSet);

        for (Address address : addressSet) {
            assertThat(address.getStatus().getPhase(), is(Configuring));
        }
        verify(generator).generateCluster(eq(provisioner.getShardedClusterId(t2)), eq(1), eq(t2), any(), any());
        verify(generator).generateCluster(eq(provisioner.getShardedClusterId(t1)), eq(1), eq(t1), any(), any());
    }

    @Test
    public void testDurableSubscriptionsShardedStaysOnTopicBroker() {
        AddressProvisioner provisioner = createProvisioner(Arrays.asList(
                new ResourceAllowance("broker", 2),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 3)));

        Address t1 = createAddress("t1", "topic", "small-topic");
        Address t2 = createAddress("t2", "topic", "small-topic");

        Set<Address> addressSet = Sets.newSet(
                t1,
                createSubscription("s1", "t1", "small-subscription"),
                createSubscription("s2", "t1", "small-subscription"),
                createSubscription("s3", "t1", "small-subscription"),
                createSubscription("s4", "t1", "small-subscription"),
                createSubscription("s5", "t1", "small-subscription"),
                createSubscription("s6", "t1", "small-subscription"),
                createSubscription("s7", "t1", "small-subscription"),
                createSubscription("s8", "t1", "small-subscription"),
                createSubscription("s9", "t1", "small-subscription"),
                createSubscription("s10", "t1", "small-subscription"),
                createSubscription("s11", "t1", "small-subscription"),
                createSubscription("s12", "t1", "small-subscription"),
                t2);

        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(Collections.emptySet());
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, addressSet, addressSet);

        assertThat(neededMap.keySet().size(), is(3));
        assertThat(AddressProvisioner.sumTotalNeeded(neededMap), is(2));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("router")), is(1));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("broker")), is(1));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("subscription")), is(1));

        Set<Address> configured = new HashSet<Address>();
        Set<Address> unConfigured = new HashSet<Address>();

        for (Address address : addressSet) {
            if (address.getStatus().getPhase().equals(Pending)) {
                unConfigured.add(address);
            } else if (address.getStatus().getPhase().equals(Configuring)) {
                configured.add(address);
            }
        }
        assertEquals(2, unConfigured.size());
        assertTrue(configured.contains(t1));
        assertTrue(configured.contains(t2));
        assertEquals(12, configured.size(), "contains 2 topic + 10 subscriptions");
        Iterator<Address> unconfiguredIterator = unConfigured.iterator();
        assertFalse(configured.contains(unconfiguredIterator.next()));
        assertFalse(configured.contains(unconfiguredIterator.next()));
    }
}
