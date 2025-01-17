/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TestUserApi implements UserApi {
    private final Map<String, Map<String, User>> userMap = new HashMap<>();
    public boolean throwException = false;

    @Override
    public Optional<User> getUserWithName(String realm, String name) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        return Optional.ofNullable(userMap.get(realm).get(name));
    }

    @Override
    public void createUser(String realm, User user) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        String name = user.getSpec().getUsername();
        userMap.computeIfAbsent(realm, k -> new HashMap<>()).put(name, user);

    }

    @Override
    public boolean replaceUser(String realm, User user) {
        if (throwException) {
            throw new RuntimeException("exception");
        }

        String name = user.getSpec().getUsername();
        Map<String, User> users = userMap.computeIfAbsent(realm, k -> new HashMap<>());
        if (!users.containsKey(name)) {
            return false;
        }
        users.put(name, user);
        return true;
    }

    @Override
    public void deleteUser(String realm, User user) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        Map<String, User> m = userMap.get(realm);
        if (m != null) {
            m.remove(user.getSpec().getUsername());
        }
    }

    @Override
    public boolean realmExists(String realm) {
        return userMap.containsKey(realm);
    }

    @Override
    public UserList listUsers(String namespace) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        UserList list = new UserList();
        for (Map<String, User> users : userMap.values()) {
            for (User user : users.values()) {
                if (user.getMetadata().getNamespace().equals(namespace)) {
                    list.getItems().add(user);
                }
            }
        }
        return list;
    }

    @Override
    public UserList listUsersWithLabels(String namespace, Map<String, String> labels) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        return listUsers(namespace);
    }

    @Override
    public void deleteUsers(String namespace) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        for (Map<String, User> users : userMap.values()) {
            for (User user : new ArrayList<>(users.values())) {
                if (user.getMetadata().getNamespace().equals(namespace)) {
                    users.remove(user.getSpec().getUsername());
                }
            }
        }
    }
}
