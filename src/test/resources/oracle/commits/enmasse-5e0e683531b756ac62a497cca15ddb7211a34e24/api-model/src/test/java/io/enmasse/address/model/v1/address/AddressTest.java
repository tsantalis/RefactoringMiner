/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.address;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.Status;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddressTest {
    @Test
    public void testCreateFromBuilder() {
        Address.Builder b1 = new Address.Builder()
                .setAddress("addr1")
                .setAddressSpace("space1")
                .setName("myname")
                .setType("queue")
                .setPlan("myplan")
                .setStatus(new Status(true))
                .setUid("myuuid")
                .setResourceVersion("1234")
                .setSelfLink("/my/link")
                .setCreationTimestamp("my stamp");

        Address a1 = b1.build();

        Address.Builder b2 = new Address.Builder(a1);

        Address a2 = b2.build();

        assertThat(a1.getAddress(), is(a2.getAddress()));
        assertThat(a1.getAddressSpace(), is(a2.getAddressSpace()));
        assertThat(a1.getName(), is(a2.getName()));
        assertThat(a1.getPlan(), is(a2.getPlan()));
        assertThat(a1.getStatus(), is(a2.getStatus()));
        assertThat(a1.getType(), is(a2.getType()));
        assertThat(a1.getUid(), is(a2.getUid()));
        assertThat(a1.getResourceVersion(), is(a2.getResourceVersion()));
        assertThat(a1.getSelfLink(), is(a2.getSelfLink()));
        assertThat(a1.getCreationTimestamp(), is(a2.getCreationTimestamp()));
    }

    @Test
    public void testSanitizer() {
        Address b1 = new Address.Builder()
                .setNamespace("ns1")
                .setAddress("myAddr_-")
                .setAddressSpace("myspace")
                .setPlan("p1")
                .setType("t1")
                .build();

        Address b2 = new Address.Builder()
                .setNamespace("ns1")
                .setAddress(b1.getAddress())
                .setAddressSpace("myspace")
                .setName(b1.getName())
                .setPlan(b1.getPlan())
                .setType(b1.getType())
                .build();
        assertNull(b1.getName());
        String generated = Address.generateName(b1.getAddressSpace(), b1.getAddress());
        System.out.println(generated);
        assertTrue(generated.startsWith("myspace.myaddr1."));
        assertThat(b1.getName(), is(b2.getName()));
        assertThat(b1.getAddress(), is(b2.getAddress()));
        assertThat(b1.getPlan(), is(b2.getPlan()));
        assertThat(b1.getType(), is(b2.getType()));
    }

    @Test
    public void testCopy() {
        Address a = new Address.Builder()
                .setAddress("a1")
                .setPlan("p1")
                .setType("t1")
                .setNamespace("ns")
                .setAddressSpace("myspace")
                .setStatus(new Status(true).setPhase(Status.Phase.Active).appendMessage("foo"))
                .build();

        Address b = new Address.Builder(a).build();

        assertThat(a, is(b));
        assertTrue(b.getStatus().isReady());
        assertThat(b.getStatus().getPhase(), is(Status.Phase.Active));
        assertThat(b.getStatus().getMessages(), hasItem("foo"));
    }
}
