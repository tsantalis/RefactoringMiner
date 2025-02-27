/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.address.model;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link AuthenticationService}
 */
public class AuthenticationServiceTest {

    /**
     * Test if two empty services are equal
     */
    @Test
    public void testEqualsEmpty() {
        assertEquality(true,
                new AuthenticationService.Builder().build(),
                new AuthenticationService.Builder().build());
    }

    /**
     * Test if two services are equal having the same type
     */
    @Test
    public void testEqualsSameType() {
        assertEquality(true,
                new AuthenticationService.Builder()
                        .setType(AuthenticationServiceType.EXTERNAL)
                        .build(),
                new AuthenticationService.Builder()
                        .setType(AuthenticationServiceType.EXTERNAL)
                        .build());
    }

    /**
     * Test if two services are not equal having the different types
     */
    @Test
    public void testNonEqualsDifferentType() {
        assertEquality(false,
                new AuthenticationService.Builder()
                        .setType(AuthenticationServiceType.NONE)
                        .build(),
                new AuthenticationService.Builder()
                        .setType(AuthenticationServiceType.EXTERNAL)
                        .build());
    }

    /**
     * Test if two services are equal having the same type and the same details
     */
    @Test
    public void testEqualsSameTypeSameDetails() {
        final Map<String, Object> details1 = new HashMap<>();
        final Map<String, Object> details2 = new HashMap<>();

        for (final Map<String, Object> map : Arrays.asList(details1, details2)) {
            map.put("string", "foo");
            map.put("int", 1);
            map.put("bool", true);
        }

        assertEquality(true,
                new AuthenticationService.Builder()
                        .setType(AuthenticationServiceType.EXTERNAL)
                        .setDetails(details1)
                        .build(),
                new AuthenticationService.Builder()
                        .setType(AuthenticationServiceType.EXTERNAL)
                        .setDetails(details2)
                        .build());
    }

    /**
     * Test for equality
     * 
     * @param isEqual are values to be expected equal?
     * @param v1 value 1
     * @param v2 value 2
     */
    protected void assertEquality(final boolean isEqual, final Object v1, final Object v2) {
        assertEquals(isEqual, v1.equals(v2));
        assertEquals(isEqual, v2.equals(v1));
        assertEquals(isEqual, v1.hashCode() == v2.hashCode());
    }
}
