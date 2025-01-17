/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UserModelTest {
    @Test
    public void testSerializeUserPassword() throws IOException {
        User user = new UserBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("myspace.user1")
                        .withNamespace("ns1")
                        .build())
                .withSpec(new UserSpecBuilder()
                        .withUsername("user1")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.password)
                                .withPassword("p4ssw0rd")
                                .build())
                        .withAuthorization(Arrays.asList(
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("queue1", "topic1"))
                                        .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build(),
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("direct*"))
                                        .withOperations(Arrays.asList(Operation.view))
                                        .build()))
                        .build())
                .build();

        ObjectMapper mapper = new ObjectMapper();
        byte [] serialized = mapper.writeValueAsBytes(user);

        User deserialized = mapper.readValue(serialized, User.class);

        assertEquals(user.getMetadata().getName(), deserialized.getMetadata().getName());
        assertEquals(user.getMetadata().getNamespace(), deserialized.getMetadata().getNamespace());
        assertEquals(user.getSpec().getUsername(), deserialized.getSpec().getUsername());
        assertEquals(user.getSpec().getAuthentication().getType(), deserialized.getSpec().getAuthentication().getType());
        assertEquals(user.getSpec().getAuthentication().getPassword(), deserialized.getSpec().getAuthentication().getPassword());
        assertEquals(user.getSpec().getAuthorization().size(), deserialized.getSpec().getAuthorization().size());

        assertAuthorization(deserialized, Arrays.asList("queue1", "topic1"), Arrays.asList(Operation.send, Operation.recv));

        UserList list = new UserList();
        list.getItems().add(user);

        serialized = mapper.writeValueAsBytes(list);
        UserList deserializedList = mapper.readValue(serialized, UserList.class);

        assertEquals(1, deserializedList.getItems().size());

        deserialized = deserializedList.getItems().get(0);

        assertEquals(user.getMetadata().getName(), deserialized.getMetadata().getName());
        assertEquals(user.getMetadata().getNamespace(), deserialized.getMetadata().getNamespace());
        assertEquals(user.getSpec().getUsername(), deserialized.getSpec().getUsername());
        assertEquals(user.getSpec().getAuthentication().getType(), deserialized.getSpec().getAuthentication().getType());
        assertEquals(user.getSpec().getAuthentication().getPassword(), deserialized.getSpec().getAuthentication().getPassword());
        assertEquals(user.getSpec().getAuthorization().size(), deserialized.getSpec().getAuthorization().size());

        assertAuthorization(deserialized, Arrays.asList("queue1", "topic1"), Arrays.asList(Operation.send, Operation.recv));
    }

    @Test
    public void testSerializeUserFederated() throws IOException {
        User user = new UserBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("myspace.user1")
                        .withNamespace("ns1")
                        .build())
                .withSpec(new UserSpecBuilder()
                        .withUsername("user1")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.federated)
                                .withProvider("openshift")
                                .withFederatedUserid("uuid")
                                .withFederatedUsername("user1")
                                .build())
                        .withAuthorization(Arrays.asList(
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("queue1", "topic1"))
                                        .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build(),
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("direct*"))
                                        .withOperations(Arrays.asList(Operation.view))
                                        .build()))
                        .build())
                .build();

        ObjectMapper mapper = new ObjectMapper();
        byte [] serialized = mapper.writeValueAsBytes(user);

        User deserialized = mapper.readValue(serialized, User.class);

        assertEquals(user.getMetadata().getName(), deserialized.getMetadata().getName());
        assertEquals(user.getMetadata().getNamespace(), deserialized.getMetadata().getNamespace());
        assertEquals(user.getSpec().getUsername(), deserialized.getSpec().getUsername());
        assertEquals(user.getSpec().getAuthentication().getType(), deserialized.getSpec().getAuthentication().getType());
        assertEquals(user.getSpec().getAuthentication().getPassword(), deserialized.getSpec().getAuthentication().getPassword());
        assertEquals(user.getSpec().getAuthorization().size(), deserialized.getSpec().getAuthorization().size());

        assertAuthorization(deserialized, Arrays.asList("queue1", "topic1"), Arrays.asList(Operation.send, Operation.recv));

        UserList list = new UserList();
        list.getItems().add(user);

        serialized = mapper.writeValueAsBytes(list);
        UserList deserializedList = mapper.readValue(serialized, UserList.class);

        assertEquals(1, deserializedList.getItems().size());

        deserialized = deserializedList.getItems().get(0);

        assertEquals(user.getMetadata().getName(), deserialized.getMetadata().getName());
        assertEquals(user.getMetadata().getNamespace(), deserialized.getMetadata().getNamespace());
        assertEquals(user.getSpec().getUsername(), deserialized.getSpec().getUsername());
        assertEquals(user.getSpec().getAuthentication().getType(), deserialized.getSpec().getAuthentication().getType());
        assertEquals(user.getSpec().getAuthentication().getPassword(), deserialized.getSpec().getAuthentication().getPassword());
        assertEquals(user.getSpec().getAuthorization().size(), deserialized.getSpec().getAuthorization().size());

        assertAuthorization(deserialized, Arrays.asList("queue1", "topic1"), Arrays.asList(Operation.send, Operation.recv));
    }

    @Test
    public void testValidation() {
        createAndValidate("myspace.user1", "user1", true);
        createAndValidate("myspace.usEr1", "user1", false);
        createAndValidate("myspace.user1", "usEr1", false);
        createAndValidate("myspace.user1", "usEr1", false);
        createAndValidate("myspaceuser1", "user1", false);

        createAndValidate("myspace.user1_", "user1", false);
        createAndValidate("myspace_.user1", "user1", false);
        createAndValidate("_myspace.user1", "user1", false);
        createAndValidate("myspace._user1", "user1", false);
        createAndValidate("myspace.user1", "user1_", false);
        createAndValidate("myspace.user_1", "user1", false);
        createAndValidate("myspace.user1", "user_1", true);

        createAndValidate("myspace.user1@", "user1", false);
        createAndValidate("myspace@.user1", "user1", false);
        createAndValidate("@myspace.user1", "user1", false);
        createAndValidate("myspace.@user1", "user1", false);
        createAndValidate("myspace.user1", "user1@", false);
        createAndValidate("myspace.user@1", "user1", true);
        createAndValidate("myspace.user1", "user@1", true);

        createAndValidate("myspace.user1@example.com", "user1@example.com", true);

        createAndValidate("myspace.user1-", "user1", false);
        createAndValidate("myspace-.user1", "user1", false);
        createAndValidate("-myspace.user1", "user1", false);
        createAndValidate("myspace.-user1", "user1", false);
        createAndValidate("myspace.user1", "user1-", false);
        createAndValidate("myspace.user1", "-user1-", false);
        createAndValidate("myspace.user1-foo-bar", "user1-foo-bar", true);
        UUID uuid = UUID.randomUUID();
        createAndValidate("myspace." + uuid.toString(), uuid.toString(), true);
        createAndValidate("a.ab", "ab", true);
        createAndValidate("aa.b", "b", true);
        createAndValidate("a.b", "b", true);
    }

    private void createAndValidate(String name, String username, boolean shouldValidate) {
        User u1 = new UserBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(name)
                        .withNamespace("ns1")
                        .build())
                .withSpec(new UserSpecBuilder()
                        .withUsername(username)
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.federated)
                                .withProvider("openshift")
                                .withFederatedUserid("uuid")
                                .withFederatedUsername("user1")
                                .build())
                        .withAuthorization(Arrays.asList(
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("queue1", "topic1"))
                                        .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build(),
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("direct*"))
                                        .withOperations(Arrays.asList(Operation.view))
                                        .build()))
                        .build())
                .build();

        try {
            u1.validate();
            assertTrue(shouldValidate);
        } catch (UserValidationFailedException e) {
            // e.printStackTrace();
            assertFalse(shouldValidate);
        }
    }

    private void assertAuthorization(User deserialized, List<String> addresses, List<Operation> operations) {
        for (UserAuthorization authorization : deserialized.getSpec().getAuthorization()) {
            if (authorization.getOperations().equals(operations) && authorization.getAddresses().equals(addresses)) {
                return;
            }
        }
        fail("Unable to find matching authorization");
    }
}
