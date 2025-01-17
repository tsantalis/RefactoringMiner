/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;


import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


public class AddressSpaceTest {

    @Test
    public void testSimpleCreateFromBuilder() {
        AddressSpace space = new AddressSpace.Builder()
                .setName("name")
                .setType("type")
                .setPlan("plan")
                .build();

        assertNotNull(space);

        assertThat(space.getName(), is("name"));
        assertThat(space.getType(), is("type"));
        assertThat(space.getPlan(), is("plan"));
        assertThat(space.getStatus(), is(new AddressSpaceStatus(false)));
        assertNotNull(space.getEndpoints());
        assertThat(space.getEndpoints().size(), is(0));
        assertNotNull(space.getAuthenticationService());
        assertNotNull(space.getAnnotations());
        assertThat(space.getAnnotations().size(), is(0));
        assertNotNull(space.getLabels());
        assertThat(space.getLabels().size(), is(0));
    }


    @Test
    public void testSimpleWithMissingMandatory() {
        try {
            new AddressSpace.Builder()
                    .setType("type")
                    .setPlan("plan")
                    .build();
            fail();
        } catch (NullPointerException e) {
            // pass
        }

        try {
            new AddressSpace.Builder()
                    .setName("name")
                    .setPlan("plan")
                    .build();
            fail();
        } catch (NullPointerException e) {
            // pass
        }

        try {
            new AddressSpace.Builder()
                    .setName("name")
                    .setType("type")
                    .build();
            fail();
        } catch (NullPointerException e) {
            // pass
        }
    }

    @Test
    public void testEqualityIsBasedOnNameAndNamespace() {
        AddressSpace space1 = new AddressSpace.Builder()
                .setName("name")
                .setType("type")
                .setPlan("plan")
                .build();
        AddressSpace space2 = new AddressSpace.Builder()
                .setName("name")
                .setType("type2")
                .setPlan("plan")
                .build();
        assertEquals(space1, space2);
        AddressSpace space3 = new AddressSpace.Builder()
                .setName("name")
                .setNamespace("ns")
                .setType("type2")
                .setPlan("plan")
                .build();
        assertNotEquals(space1, space3);
        AddressSpace space4 = new AddressSpace.Builder()
                .setName("name")
                .setNamespace("ns")
                .setType("type")
                .setPlan("plan2")
                .build();
        assertEquals(space3, space4);
        AddressSpace space5 = new AddressSpace.Builder()
                .setName("name2")
                .setNamespace("ns")
                .setType("type")
                .setPlan("plan2")
                .build();
        assertNotEquals(space4, space5);



    }
}
