/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.api.common.DefaultExceptionMapper;
import io.enmasse.api.common.Status;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.k8s.model.v1beta1.Table;
import io.enmasse.user.model.v1.*;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.time.Clock;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpUserServiceTest {
    private HttpUserService userService;
    private TestAddressSpaceApi addressSpaceApi;
    private TestUserApi userApi;
    private User u1;
    private User u2;
    private DefaultExceptionMapper exceptionMapper = new DefaultExceptionMapper();
    private SecurityContext securityContext;

    @BeforeEach
    public void setup() {
        addressSpaceApi = new TestAddressSpaceApi();
        userApi = new TestUserApi();
        this.userService = new HttpUserService(addressSpaceApi, userApi, Clock.systemUTC());
        securityContext = mock(SecurityContext.class);
        when(securityContext.isUserInRole(any())).thenReturn(true);


        addressSpaceApi.createAddressSpace(new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("ns1")
                .putAnnotation(AnnotationKeys.REALM_NAME, "r1")
                .setType("type1")
                .setPlan("myplan")
                .build());

        addressSpaceApi.createAddressSpace(new AddressSpace.Builder()
                .setName("otherspace")
                .setNamespace("ns2")
                .putAnnotation(AnnotationKeys.REALM_NAME, "r2")
                .setType("type1")
                .setPlan("myplan")
                .build());
        u1 = new UserBuilder()
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

        u2 = new UserBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("otherspace.user2")
                        .withNamespace("ns2")
                        .build())
                .withSpec(new UserSpecBuilder()
                        .withUsername("user2")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.password)
                                .withPassword("pswd")
                                .build())
                        .withAuthorization(Arrays.asList(
                                new UserAuthorizationBuilder()
                                        .withOperations(Arrays.asList(Operation.manage))
                                        .build()))
                        .build())
                .build();

        userApi.createUser("r1", u1);
        userApi.createUser("r2", u2);
    }

    private Response invoke(Callable<Response> fn) {
        try {
            return fn.call();
        } catch (Exception e) {
            return exceptionMapper.toResponse(e);
        }
    }

    @Test
    public void testList() {
        Response response = invoke(() -> userService.getUserList(securityContext, null, "ns1", null));

        assertThat(response.getStatus(), is(200));
        UserList list = (UserList) response.getEntity();

        assertThat(list.getItems().size(), is(1));
        assertThat(list.getItems(), hasItem(u1));
    }

    @Test
    public void testListTable() {
        Response response = invoke(() -> userService.getUserList(securityContext, "application/json;as=Table;g=meta.k8s.io;v=v1beta1", "ns1", null));

        assertThat(response.getStatus(), is(200));
        Table table = (Table) response.getEntity();

        assertThat(table.getColumnDefinitions().size(), is(4));
        assertThat(table.getRows().size(), is(1));
    }

    @Test
    public void testGetByUser() {
        Response response = invoke(() -> userService.getUser(securityContext, null, "ns1", "myspace.user1"));

        assertThat(response.getStatus(), is(200));
        User user = (User) response.getEntity();

        assertThat(user, is(u1));
    }

    @Test
    public void testGetByUserTable() {
        Response response = invoke(() -> userService.getUser(securityContext, "application/json;as=Table;g=meta.k8s.io;v=v1beta1", "ns1", "myspace.user1"));

        Table table = (Table) response.getEntity();

        assertThat(response.getStatus(), is(200));
        assertThat(table.getColumnDefinitions().size(), is(4));
        assertThat(table.getRows().get(0).getObject().getMetadata().getName(), is(u1.getMetadata().getName()));
    }

    @Test
    public void testGetByUserNotFound() {
        Response response = invoke(() -> userService.getUser(securityContext, null, "ns1", "myspace.user2"));

        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testListException() {
        userApi.throwException = true;
        Response response = invoke(() -> userService.getUserList(securityContext, null, "ns1", null));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGetException() {
        userApi.throwException = true;
        Response response = invoke(() -> userService.getUser(securityContext, null, "ns1", "myspace.user1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testCreate() {
        User u3 = new UserBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("myspace.user3")
                        .withNamespace("ns1")
                        .build())
                .withSpec(new UserSpecBuilder()
                        .withUsername("user3")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.password)
                                .withPassword("p4ssw0rd")
                                .build())
                        .withAuthorization(Arrays.asList(
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("queue1", "topic1"))
                                        .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build()))
                        .build())
                .build();
        Response response = invoke(() -> userService.createUser(securityContext, new ResteasyUriInfo("http://localhost:8443/", null, "/"), "ns1", u3));
        assertThat(response.getStatus(), is(201));

        assertThat(userApi.listUsers("ns1").getItems(), hasItem(u3));
    }

    @Test
    public void testCreateException() {
        userApi.throwException = true;
        User u3 = new UserBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("myspace.user3")
                        .withNamespace("ns1")
                        .build())
                .withSpec(new UserSpecBuilder()
                        .withUsername("user3")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.password)
                                .withPassword("p4ssw0rd")
                                .build())
                        .withAuthorization(Arrays.asList(
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("queue1", "topic1"))
                                        .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build()))
                        .build())
                .build();
        Response response = invoke(() -> userService.createUser(securityContext, null, "ns1", u3));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testPutException() {
        userApi.throwException = true;
        User u3 = new UserBuilder()
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
                                        .build()))
                        .build())
                .build();
        Response response = invoke(() -> userService.replaceUser(securityContext, "ns1", "myspace.user1", u3));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testPutNotMatchingName() {
        User u3 = new UserBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("myspace.user3")
                        .withNamespace("ns1")
                        .build())
                .withSpec(new UserSpecBuilder()
                        .withUsername("user3")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.password)
                                .withPassword("p4ssw0rd")
                                .build())
                        .withAuthorization(Arrays.asList(
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("queue1", "topic1"))
                                        .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build()))
                        .build())
                .build();
        Response response = invoke(() -> userService.replaceUser(securityContext, "ns1", "myspace.user1", u3));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testPutNotExists() {
        User u3 = new UserBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("myspace.user3")
                        .withNamespace("ns1")
                        .build())
                .withSpec(new UserSpecBuilder()
                        .withUsername("user3")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.password)
                                .withPassword("p4ssw0rd")
                                .build())
                        .withAuthorization(Arrays.asList(
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("queue1", "topic1"))
                                        .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build()))
                        .build())
                .build();
        Response response = invoke(() -> userService.replaceUser(securityContext, "ns1", "myspace.user3", u3));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testPut() {
        User u3 = new UserBuilder()
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
                                        .withAddresses(Arrays.asList("queue2", "topic2"))
                                        .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build()))
                        .build())
                .build();
        Response response = invoke(() -> userService.replaceUser(securityContext, "ns1", "myspace.user1", u3));
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void testDelete() {
        Response response = invoke(() -> userService.deleteUser(securityContext, "ns1", "myspace.user1"));
        assertThat(response.getStatus(), is(200));
        assertThat(((Status) response.getEntity()).getStatusCode(), is(200));

        assertTrue(userApi.listUsers("ns1").getItems().isEmpty());
        assertFalse(userApi.listUsers("ns2").getItems().isEmpty());
    }

    @Test
    public void testDeleteException() {
        userApi.throwException = true;
        Response response = invoke(() -> userService.deleteUser(securityContext, "ns1", "myspace.user1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void deleteAllUsers() {
        Response response = invoke(() -> userService.deleteUsers(securityContext, "unknown"));
        assertThat(response.getStatus(), is(200));
        assertThat(userApi.listUsers("ns1").getItems().size(), is(1));

        response = invoke(() -> userService.deleteUsers(securityContext, "ns1"));
        assertThat(response.getStatus(), is(200));
        assertThat(userApi.listUsers("ns1").getItems().size(), is(0));
    }
}
