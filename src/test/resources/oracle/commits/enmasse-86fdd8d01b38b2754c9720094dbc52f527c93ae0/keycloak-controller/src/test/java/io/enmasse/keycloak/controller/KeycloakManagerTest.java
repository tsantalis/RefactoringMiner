/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.keycloak.controller;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserList;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KeycloakManagerTest {

    private KeycloakManager manager;
    private Set<String> realms;
    private List<String> updatedRealms;
    private Map<String, String> realmAdminUsers;
    private KubeApi mockKubeApi;

    @Before
    public void setup() {
        realms = new HashSet<>();
        updatedRealms = new LinkedList<>();
        realmAdminUsers = new HashMap<>();
        mockKubeApi = mock(KubeApi.class);
        when(mockKubeApi.findUserId(any())).thenReturn("");
        when(mockKubeApi.getIdentityProviderParams()).thenReturn(new KeycloakRealmParams("http://example.com", "id", "secret", Collections.singletonMap("hdr1", "value1")));

        manager = new KeycloakManager(new KeycloakApi() {
            @Override
            public Set<String> getRealmNames() {
                return new HashSet<>(realms);
            }

            @Override
            public void createRealm(String namespace, String realmName, String consoleRedirectURI, KeycloakRealmParams params) {
                realms.add(realmName);
            }

            @Override
            public void updateRealm(String realmName, KeycloakRealmParams updated) {
                updatedRealms.add(realmName);
            }

            @Override
            public void deleteRealm(String realmName) {
                realms.remove(realmName);
            }
        }, mockKubeApi, new UserApi() {
            @Override
            public Optional<User> getUserWithName(String realm, String name) {
                return Optional.empty();
            }

            @Override
            public void createUser(String realm, User user) {
                realmAdminUsers.put(realm, user.getSpec().getUsername());
            }

            @Override
            public boolean replaceUser(String realm, User user) {
                return false;
            }

            @Override
            public void deleteUser(String realm, User user) {

            }

            @Override
            public boolean realmExists(String realm) {
                return true;
            }

            @Override
            public UserList listUsers(String realm) {
                return null;
            }

            @Override
            public UserList listUsersWithLabels(String realm, Map<String, String> labels) {
                return null;
            }

            @Override
            public void deleteUsers(String namespace) {

            }
        });
    }

    @Test
    public void testAddAddressSpace() throws Exception {
        manager.onUpdate(Collections.singletonList(createAddressSpace("a1", AuthenticationServiceType.NONE)));
        assertTrue(realms.isEmpty());

        manager.onUpdate(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.NONE), createAddressSpace("a2", AuthenticationServiceType.STANDARD)));
        assertTrue(realms.contains("a2"));

        manager.onUpdate(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.NONE), createAddressSpace("a2", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));
        assertTrue(realms.contains("a2"));
        assertTrue(realms.contains("a3"));
        assertEquals(2, realms.size());

        assertTrue(realmAdminUsers.get("a2").length() > 0);
        assertTrue(realmAdminUsers.get("a3").length() > 0);
    }

    @Test
    public void testRemoveAddressSpace() throws Exception {
        manager.onUpdate(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.STANDARD), createAddressSpace("a2", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));
        manager.onUpdate(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));

        assertTrue(realms.contains("a1"));
        assertFalse(realms.contains("a2"));
        assertTrue(realms.contains("a3"));
        assertEquals(2, realms.size());
    }

    @Test
    public void testAuthTypeChanged() throws Exception {
        manager.onUpdate(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.STANDARD)));
        assertTrue(realms.contains("a1"));
        assertEquals(1, realms.size());

        manager.onUpdate(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.NONE)));
        assertFalse(realms.contains("a1"));
        assertEquals(0, realms.size());
    }

    @Test
    public void testUpdateRealm() throws Exception {
        List<AddressSpace> spaces = Collections.singletonList(createAddressSpace("a1", AuthenticationServiceType.STANDARD));
        manager.onUpdate(spaces);
        assertTrue(realms.contains("a1"));
        assertTrue(updatedRealms.isEmpty());

        manager.onUpdate(spaces);
        assertTrue(updatedRealms.isEmpty());

        when(mockKubeApi.getIdentityProviderParams()).thenReturn(new KeycloakRealmParams("http://example.com", "id", "secret2", Collections.singletonMap("hdr1", "value1")));

        manager.onUpdate(spaces);
        assertEquals(1, updatedRealms.size());
    }

    private AddressSpace createAddressSpace(String name, AuthenticationServiceType authType) {
        return new AddressSpace.Builder()
                .setName(name)
                .setNamespace("myns")
                .setPlan("myplan")
                .setType("standard")
                .putAnnotation(AnnotationKeys.CREATED_BY, "developer")
                .appendEndpoint(new EndpointSpec.Builder()
                        .setName("console")
                        .setService("console")
                        .build())
                .setStatus(new AddressSpaceStatus(true)
                        .appendEndpointStatus(new EndpointStatus.Builder()
                                .setName("console")
                                .setServiceHost("console.svc")
                                .setExternalPorts(Collections.singletonMap("http", 443))
                                .setExternalHost("console.example.com")
                                .build()))
                .setAuthenticationService(new AuthenticationService.Builder().setType(authType).build()).build();
    }
}
