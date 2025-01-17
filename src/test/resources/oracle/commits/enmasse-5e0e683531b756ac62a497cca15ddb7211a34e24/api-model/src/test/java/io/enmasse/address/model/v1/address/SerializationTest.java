/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.address;

import io.enmasse.address.model.*;
import io.enmasse.address.model.v1.CodecV1;
import io.enmasse.address.model.v1.DeserializeException;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AddressSpacePlanBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.io.IOException;
import java.util.*;

import static io.enmasse.address.model.ExposeSpec.ExposeType.route;
import static io.enmasse.address.model.ExposeSpec.TlsTermination.passthrough;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

// TODO: Add more tests of invalid input to deserialization
public class SerializationTest {

    @Test
    public void testSerializeAddress() throws IOException {
        String uuid = UUID.randomUUID().toString();
        Address address = new Address.Builder()
                .setName("as1.a1")
                .setAddress("addr1")
                .setAddressSpace("as1")
                .setNamespace("ns")
                .setType("queue")
                .setPlan("inmemory")
                .setUid(uuid)
                .putAnnotation("my", "annotation")
                .setResourceVersion("1234")
                .setSelfLink("/my/link")
                .setCreationTimestamp("my stamp")
                .build();

        byte[] serialized = CodecV1.getMapper().writeValueAsBytes(address);

        Address deserialized = CodecV1.getMapper().readValue(serialized, Address.class);

        assertThat(deserialized, is(address));
        assertThat(deserialized.getName(), is(address.getName()));
        assertThat(deserialized.getAddressSpace(), is(address.getAddressSpace()));
        assertThat(deserialized.getType(), is(address.getType()));
        assertThat(deserialized.getUid(), is(address.getUid()));
        assertThat(deserialized.getResourceVersion(), is(address.getResourceVersion()));
        assertThat(deserialized.getSelfLink(), is(address.getSelfLink()));
        assertThat(deserialized.getCreationTimestamp(), is(address.getCreationTimestamp()));
        assertThat(deserialized.getPlan(), is(address.getPlan()));
        assertThat(deserialized.getAddress(), is(address.getAddress()));
        assertThat(deserialized.getAnnotations(), is(address.getAnnotations()));
    }

    @Test
    public void testSerializeAddressList() throws IOException {
        Address addr1 = new Address.Builder()
                .setName("a1.a1")
                .setAddress("addr1")
                .setAddressSpace("a1")
                .setNamespace("ns")
                .setType("queue")
                .setPlan("myplan")
                .build();

        Address addr2 = new Address.Builder()
                .setName("a1.a2")
                .setAddressSpace("a1")
                .setAddress("addr2")
                .setNamespace("ns")
                .setType("anycast")
                .setPlan("myplan")
                .build();


        AddressList list = new AddressList(Sets.newSet(addr1, addr2));

        String serialized = CodecV1.getMapper().writeValueAsString(list);
        List<Address> deserialized = CodecV1.getMapper().readValue(serialized, AddressList.class);

        assertThat(deserialized, is(list));
    }


    @Test
    public void testSerializeEmptyAddressList() throws IOException {

        AddressList list = new AddressList(Collections.emptySet());

        String serialized = CodecV1.getMapper().writeValueAsString(list);
        assertTrue(serialized.matches(".*\"items\"\\s*:\\s*\\[\\s*\\].*"),
                "Serialized form '" + serialized + "' does not include empty items list");
        List<Address> deserialized = CodecV1.getMapper().readValue(serialized, AddressList.class);

        assertThat(deserialized, is(list));
    }

    @Test
    public void testSerializeAddressSpaceWithIllegalName() throws IOException {
        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace.bar")
                .setPlan("myplan")
                .setType("mytype")
                .build();

        String serialized = CodecV1.getMapper().writeValueAsString(addressSpace);
        assertThrows(DeserializeException.class, () -> CodecV1.getMapper().readValue(serialized, AddressSpace.class));
    }

    @SuppressWarnings("serial")
    @Test
    public void testSerializeAddressSpace() throws IOException {
        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .setPlan("defaultplan")
                .setType("standard")
                .setCreationTimestamp("some date")
                .setResourceVersion("1234")
                .setSelfLink("/my/resource")
                .setStatus(new AddressSpaceStatus(true).appendMessage("hello").appendEndpointStatus(
                        new EndpointStatus.Builder()
                                .setName("myendpoint")
                                .setExternalHost("example.com")
                                .setExternalPorts(Collections.singletonMap("amqps", 443))
                                .setServiceHost("messaging.svc")
                                .setServicePorts(Collections.singletonMap("amqp", 5672))
                                .build()))
                .setEndpointList(Arrays.asList(new EndpointSpec.Builder()
                        .setName("myendpoint")
                        .setService("messaging")
                        .setCertSpec(new CertSpec.Builder().setProvider("provider").setSecretName("mysecret").build())
                        .setExposeSpec(new ExposeSpec.Builder()
                                .setType(route)
                                .setRouteHost("example.com")
                                .setRouteTlsTermination(passthrough)
                                .setRouteServicePort("amqp")
                                .build())
                        .build()))
                .setAuthenticationService(new AuthenticationService.Builder()
                        .setType(AuthenticationServiceType.EXTERNAL)
                        .setDetails(new HashMap<String, Object>() {{
                            put("host", "my.example.com");
                            put("port", 5671);
                            put("caCertSecretName", "authservicesecret");
                            put("clientCertSecretName", "clientcertsecret");
                            put("saslInitHost", "my.example.com");
                        }})
                        .build())
                .build();

        String serialized = CodecV1.getMapper().writeValueAsString(addressSpace);
        AddressSpace deserialized = CodecV1.getMapper().readValue(serialized, AddressSpace.class);

        assertThat(deserialized.getName(), is(addressSpace.getName()));
        assertThat(deserialized.getNamespace(), is(addressSpace.getNamespace()));
        assertThat(deserialized.getType(), is(addressSpace.getType()));
        assertThat(deserialized.getPlan(), is(addressSpace.getPlan()));
        assertThat(deserialized.getSelfLink(), is(addressSpace.getSelfLink()));
        assertThat(deserialized.getCreationTimestamp(), is(addressSpace.getCreationTimestamp()));
        assertThat(deserialized.getResourceVersion(), is(addressSpace.getResourceVersion()));
        assertThat(deserialized.getStatus().isReady(), is(addressSpace.getStatus().isReady()));
        assertThat(deserialized.getStatus().getMessages(), is(addressSpace.getStatus().getMessages()));
        assertThat(deserialized.getStatus().getEndpointStatuses().size(), is(addressSpace.getEndpoints().size()));
        assertThat(deserialized.getStatus().getEndpointStatuses().get(0).getName(), is(addressSpace.getStatus().getEndpointStatuses().get(0).getName()));
        assertThat(deserialized.getStatus().getEndpointStatuses().get(0).getExternalHost(), is(addressSpace.getStatus().getEndpointStatuses().get(0).getExternalHost()));
        assertThat(deserialized.getStatus().getEndpointStatuses().get(0).getExternalPorts().values().iterator().next(), is(addressSpace.getStatus().getEndpointStatuses().get(0).getExternalPorts().values().iterator().next()));
        assertThat(deserialized.getStatus().getEndpointStatuses().get(0).getServiceHost(), is(addressSpace.getStatus().getEndpointStatuses().get(0).getServiceHost()));
        assertThat(deserialized.getStatus().getEndpointStatuses().get(0).getServicePorts(), is(addressSpace.getStatus().getEndpointStatuses().get(0).getServicePorts()));
        assertThat(deserialized.getEndpoints().size(), is(addressSpace.getEndpoints().size()));
        assertThat(deserialized.getEndpoints().get(0).getName(), is(addressSpace.getEndpoints().get(0).getName()));
        assertThat(deserialized.getEndpoints().get(0).getService(), is(addressSpace.getEndpoints().get(0).getService()));
        assertThat(deserialized.getEndpoints().get(0).getCertSpec().get().getProvider(), is(addressSpace.getEndpoints().get(0).getCertSpec().get().getProvider()));
        assertThat(deserialized.getEndpoints().get(0).getCertSpec().get().getSecretName(), is(addressSpace.getEndpoints().get(0).getCertSpec().get().getSecretName()));
        assertThat(deserialized.getAuthenticationService().getType(), is(addressSpace.getAuthenticationService().getType()));
        assertThat(deserialized.getAuthenticationService().getDetails(), is(addressSpace.getAuthenticationService().getDetails()));
        assertThat(addressSpace, is(deserialized));
    }

    @Test
    public void testDeserializeAddressSpaceCompat() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1alpha1\"," +
                "\"kind\":\"AddressSpace\"," +
                "\"metadata\":{" +
                "  \"name\":\"myspace\"" +
                "}," +
                "\"spec\":{" +
                "  \"type\": \"standard\"," +
                "  \"plan\": \"unlimited-standard\"," +
                " \"endpoints\":[" +
                "   {\"name\":\"messaging\",\"service\":\"messaging\",\"servicePort\":\"amqps\"}" +
                "  ]" +
                "}}";
        AddressSpace addressSpace = CodecV1.getMapper().readValue(json, AddressSpace.class);
        assertThat(addressSpace.getEndpoints().size(), is(1));
        assertTrue(addressSpace.getEndpoints().get(0).getExposeSpec().isPresent());
        assertThat(addressSpace.getEndpoints().get(0).getExposeSpec().get().getType(), is(route));
        assertThat(addressSpace.getEndpoints().get(0).getExposeSpec().get().getRouteTlsTermination(), is(passthrough));
        assertThat(addressSpace.getEndpoints().get(0).getExposeSpec().get().getRouteServicePort(), is("amqps"));
    }

    @Test
    public void testDeserializeAddressSpaceMissingDefaults() throws IOException {
        String serialized = "{\"kind\": \"AddressSpace\", \"apiVersion\": \"v1alpha1\"}";
        assertThrows(DeserializeException.class, () -> CodecV1.getMapper().readValue(serialized, AddressSpace.class));
    }

    @Test
    public void testDeserializeAddressMissingDefaults() throws IOException {
        String serialized = "{\"kind\": \"Address\", \"apiVersion\": \"v1alpha1\"}";
        assertThrows(DeserializeException.class, () -> CodecV1.getMapper().readValue(serialized, Address.class));
    }

    @Test
    public void testDeserializeAddressSpacePlan() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1alpha1\"," +
                "\"kind\":\"AddressSpacePlan\"," +
                "\"metadata\":{" +
                "  \"name\":\"myspace\"," +
                "  \"annotations\": {" +
                "    \"mykey\": \"myvalue\"" +
                "  }" +
                "}," +
                "\"displayName\": \"MySpace\"," +
                "\"shortDescription\": \"MySpace is cool\"," +
                "\"longDescription\": \"MySpace is cool, but not much used anymore\"," +
                "\"uuid\": \"12345\"," +
                "\"addressPlans\":[\"plan1\"]," +
                "\"addressSpaceType\": \"standard\"," +
                "\"resources\": [" +
                "  { \"name\": \"router\", \"min\": 0.5, \"max\": 1.0 }, " +
                "  { \"name\": \"broker\", \"min\": 0.1, \"max\": 0.5 }" +
                "]" +
                "}";

        AddressSpacePlan addressSpacePlan = CodecV1.getMapper().readValue(json, AddressSpacePlan.class);
        assertThat(addressSpacePlan.getMetadata().getName(), is("myspace"));
        assertThat(addressSpacePlan.getAdditionalProperties().get("displayName"), is("MySpace"));
        assertFalse(addressSpacePlan.getUuid().isEmpty());
        assertThat(addressSpacePlan.getAddressPlans().size(), is(1));
        assertThat(addressSpacePlan.getAddressPlans().get(0), is("plan1"));
        assertThat(addressSpacePlan.getResources().size(), is(2));
        assertThat(addressSpacePlan.getResources().get(0).getName(), is("router"));
        assertThat(addressSpacePlan.getResources().get(1).getName(), is("broker"));
        assertThat(addressSpacePlan.getMetadata().getAnnotations().size(), is(1));
        assertThat(addressSpacePlan.getMetadata().getAnnotations().get("mykey"), is("myvalue"));
    }

    @Test
    public void testDeserializeAddressSpacePlanWithDefaults() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1alpha1\"," +
                "\"kind\":\"AddressSpacePlan\"," +
                "\"metadata\":{" +
                "  \"name\":\"myspace\"" +
                "}," +
                "\"addressPlans\":[\"plan1\"]," +
                "\"addressSpaceType\": \"standard\"," +
                "\"resources\": [" +
                "  { \"name\": \"router\", \"min\": 0.5, \"max\": 1.0 }, " +
                "  { \"name\": \"broker\", \"min\": 0.1, \"max\": 0.5 }" +
                "]" +
                "}";

        AddressSpacePlan addressSpacePlan = CodecV1.getMapper().readValue(json, AddressSpacePlan.class);
        assertThat(addressSpacePlan.getMetadata().getName(), is("myspace"));
        assertNull(addressSpacePlan.getUuid());
        assertThat(addressSpacePlan.getAddressPlans().size(), is(1));
        assertThat(addressSpacePlan.getAddressPlans().get(0), is("plan1"));
        assertThat(addressSpacePlan.getResources().size(), is(2));
        assertThat(addressSpacePlan.getResources().get(0).getName(), is("router"));
        assertThat(addressSpacePlan.getResources().get(1).getName(), is("broker"));
    }

    @Test
    public void testBuilder() {
        AddressSpacePlan plan = new AddressSpacePlanBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("plan1")
                        .withNamespace("ns")
                        .build())
                .withShortDescription("desc")
                .withUuid("uuid")
                .withAddressPlans(Arrays.asList("a", "b"))
                .build();
        assertEquals(2, plan.getAddressPlans().size());
        assertEquals("plan1", plan.getMetadata().getName());
        assertEquals("desc", plan.getShortDescription());
    }

    /*
    @Test
    public void testDeserializeResourceDefinitionWithTemplate() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1alpha1\"," +
                "\"kind\":\"ResourceDefinition\"," +
                "\"metadata\":{" +
                "  \"name\":\"rdef1\"" +
                "}," +
                "\"template\": \"mytemplate\"," +
                "\"parameters\": [" +
                "  {\"name\": \"MY_VAR1\", \"value\": \"MY_VAL1\"}," +
                "  {\"name\": \"MY_VAR2\", \"value\": \"MY_VAL2\"}" +
                "]}";

        ResourceDefinition rdef = CodecV1.getMapper().readValue(json, ResourceDefinition.class);
        assertThat(rdef.getName(), is("rdef1"));
        assertTrue(rdef.getTemplateName().isPresent());
        assertThat(rdef.getTemplateName().get(), is("mytemplate"));
        Map<String, String> parameters = rdef.getTemplateParameters();
        assertThat(parameters.size(), is(2));
        assertThat(parameters.get("MY_VAR1"), is("MY_VAL1"));
        assertThat(parameters.get("MY_VAR2"), is("MY_VAL2"));
    }

    @Test
    public void testDeserializeResourceDefinitionNoTemplate() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1alpha1\"," +
                "\"kind\":\"ResourceDefinition\"," +
                "\"metadata\":{" +
                "  \"name\":\"rdef1\"" +
                "}" +
                "}";

        ResourceDefinition rdef = CodecV1.getMapper().readValue(json, ResourceDefinition.class);
        assertThat(rdef.getName(), is("rdef1"));
        assertFalse(rdef.getTemplateName().isPresent());
    }*/

    @Test
    public void testDeserializeAddressPlan() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1alpha1\"," +
                "\"kind\":\"AddressPlan\"," +
                "\"metadata\":{" +
                "  \"name\":\"plan1\"" +
                "}," +
                "\"displayName\": \"MyPlan\"," +
                "\"shortDescription\": \"MyPlan is cool\"," +
                "\"longDescription\": \"MyPlan is cool, but not much used anymore\"," +
                "\"addressType\": \"queue\"," +
                "\"requiredResources\": [" +
                "  { \"name\": \"router\", \"credit\": 0.2 }," +
                "  { \"name\": \"broker\", \"credit\": 0.5 }" +
                "]" +
                "}";

        AddressPlan addressPlan = CodecV1.getMapper().readValue(json, AddressPlan.class);
        assertThat(addressPlan.getMetadata().getName(), is("plan1"));
        assertThat(addressPlan.getAdditionalProperties().get("displayName"), is("MyPlan"));
        assertThat(addressPlan.getAddressType(), is("queue"));
        assertNull(addressPlan.getUuid());
        assertThat(addressPlan.getRequiredResources().size(), is(2));
        assertThat(addressPlan.getRequiredResources().get(0).getName(), is("router"));
        assertThat(addressPlan.getRequiredResources().get(1).getName(), is("broker"));
    }

    @Test
    public void testDeserializeAddressPlanWithDefaults() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1alpha1\"," +
                "\"kind\":\"AddressPlan\"," +
                "\"metadata\":{" +
                "  \"name\":\"plan1\"" +
                "}," +
                "\"addressType\": \"queue\"," +
                "\"requiredResources\": [" +
                "  { \"name\": \"router\", \"credit\": 0.2 }," +
                "  { \"name\": \"broker\", \"credit\": 0.5 }" +
                "]" +
                "}";

        AddressPlan addressPlan = CodecV1.getMapper().readValue(json, AddressPlan.class);
        assertThat(addressPlan.getMetadata().getName(), is("plan1"));
        assertThat(addressPlan.getAddressType(), is("queue"));
        assertNull(addressPlan.getUuid());
        assertThat(addressPlan.getRequiredResources().size(), is(2));
        assertThat(addressPlan.getRequiredResources().get(0).getName(), is("router"));
        assertThat(addressPlan.getRequiredResources().get(1).getName(), is("broker"));
    }

    @Test
    public void testDeserializeAddressSpaceWithMissingAuthServiceValues() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1alpha1\"," +
                "\"kind\":\"AddressSpace\"," +
                "\"metadata\":{" +
                "  \"name\":\"myspace\"" +
                "}," +
                "\"spec\": {" +
                "  \"type\":\"standard\"," +
                "  \"authenticationService\": {" +
                "     \"type\": \"external\"" +
                "  }" +
                "}" +
                "}";

        assertThrows(RuntimeException.class, () -> CodecV1.getMapper().readValue(json, AddressSpace.class));
    }

    @Test
    public void testDeserializeAddressSpaceWithExtraAuthServiceValues() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.i/v1alpha1\"," +
                "\"kind\":\"AddressSpace\"," +
                "\"metadata\":{" +
                "  \"name\":\"myspace\"" +
                "}," +
                "\"spec\": {" +
                "  \"type\":\"standard\"," +
                "  \"authenticationService\": {" +
                "     \"type\": \"standard\"," +
                "     \"details\": {" +
                "       \"host\": \"my.example.com\"" +
                "     }" +
                "  }" +
                "}" +
                "}";

        assertThrows(DeserializeException.class, () -> CodecV1.getMapper().readValue(json, AddressSpace.class));
    }

    @Test
    public void testSerializeAddressSpaceList() throws IOException {
        AddressSpace a1 = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .setPlan("myplan")
                .setType("standard")
                .setStatus(new AddressSpaceStatus(true).appendMessage("hello"))
                .setEndpointList(Arrays.asList(new EndpointSpec.Builder()
                        .setName("myendpoint")
                        .setService("messaging")
                        .build()))
                .build();

        AddressSpace a2 = new AddressSpace.Builder()
                .setName("mysecondspace")
                .setNamespace("myothernamespace")
                .setPlan("myotherplan")
                .setType("brokered")
                .setStatus(new AddressSpaceStatus(false))
                .setEndpointList(Arrays.asList(new EndpointSpec.Builder()
                        .setName("bestendpoint")
                        .setService("mqtt")
                        .setCertSpec(new CertSpec.Builder().setProvider("myprovider").setSecretName("mysecret").build())
                        .build()))
                .build();

        AddressSpaceList list = new AddressSpaceList();
        list.add(a1);
        list.add(a2);

        String serialized = CodecV1.getMapper().writeValueAsString(list);

        AddressSpaceList deserialized = CodecV1.getMapper().readValue(serialized, AddressSpaceList.class);

        assertAddressSpace(deserialized, a1);
        assertAddressSpace(deserialized, a2);
    }

    private void assertAddressSpace(AddressSpaceList deserialized, AddressSpace expected) {
        AddressSpace found = null;
        for (AddressSpace addressSpace : deserialized) {
            if (addressSpace.getName().equals(expected.getName())) {
                found = addressSpace;
                break;
            }

        }
        assertNotNull(found);

        assertThat(found.getName(), is(expected.getName()));
        assertThat(found.getNamespace(), is(expected.getNamespace()));
        assertThat(found.getType(), is(expected.getType()));
        assertThat(found.getPlan(), is(expected.getPlan()));
        assertThat(found.getStatus().isReady(), is(expected.getStatus().isReady()));
        assertThat(found.getStatus().getMessages(), is(expected.getStatus().getMessages()));
    }
}
