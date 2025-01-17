/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.api.v1;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.*;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.api.server.TestSchemaProvider;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.AddressSpaceApi;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.internal.util.collections.Sets;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.SecurityContext;

public class AddressApiHelperTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private AddressApiHelper helper;
    private AddressApi addressApi;
    private SecurityContext securityContext;

    @Before
    public void setup() {
        AddressSpace addressSpace = mock(AddressSpace.class);
        when(addressSpace.getType()).thenReturn("type1");
        AddressSpaceApi addressSpaceApi = mock(AddressSpaceApi.class);
        addressApi = mock(AddressApi.class);
        securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(new BasicUserPrincipal("me"));
        when(securityContext.isUserInRole(any())).thenReturn(true);
        when(addressSpaceApi.getAddressSpaceWithName(any(), eq("test"))).thenReturn(Optional.of(addressSpace));
        when(addressSpaceApi.withAddressSpace(eq(addressSpace))).thenReturn(addressApi);
        helper = new AddressApiHelper(addressSpaceApi, new TestSchemaProvider());
    }

    @Test
    public void testCreateAddressResourceNameAlreadyExists() throws Exception {
        when(addressApi.listAddresses(any())).thenReturn(Collections.singleton(createAddress("q1", "q1")));
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Address 'q1' already exists with resource name 'q1'");

        Address invalidAddress = createAddress("someOtherName", "q1");
        helper.createAddress("test", invalidAddress);
        verify(addressApi, never()).createAddress(any(Address.class));
    }

    @Test
    public void testCreateAddress() throws Exception {
        when(addressApi.listAddresses(any())).thenReturn(Collections.emptySet());

        Address addr = createAddress("someOtherName", "q1");
        helper.createAddress("test", addr);
        verify(addressApi).createAddress(eq(addr));
    }

    @Test
    public void testCreateAddresses() throws Exception {
        when(addressApi.listAddresses(any())).thenReturn(Collections.emptySet());

        Address addr1 = createAddress("test.q1", "q1");
        Address addr2 = createAddress("test.q2", "q2");
        helper.createAddresses("test", new HashSet<>(Arrays.asList(addr1, addr2)));
        verify(addressApi).createAddress(eq(addr1));
        verify(addressApi).createAddress(eq(addr2));
    }

    @Test
    public void testCreateAddressesResourceNameAlreadyExists() throws Exception {
        when(addressApi.listAddresses(any())).thenReturn(Collections.singleton(createAddress("test1.q1", "q1")));

        Address addr1 = createAddress("test.q1", "q1");
        Address addr2 = createAddress("test.q2", "q2");
        try {
            helper.createAddresses("test", new HashSet<>(Arrays.asList(addr1, addr2)));
            fail("Exception not thrown");
        } catch (BadRequestException e) {
            // PASS
        }
        verify(addressApi, never()).createAddress(any(Address.class));
    }

    @Test
    public void testCreateAddressesResourceNameDuplicates() throws Exception {
        when(addressApi.listAddresses(any())).thenReturn(Collections.emptySet());

        Address addr1 = createAddress("dup", "q1");
        Address addr2 = createAddress("dup", "q2");
        Address addr3 = createAddress("test.q3", "q3");
        try {
            helper.createAddresses("test", new HashSet<>(Arrays.asList(addr1, addr2)));
            fail("Exception not thrown");
        } catch (BadRequestException e) {
            // PASS
            assertEquals("Address resource names must be unique. Duplicate resource names: [dup]", e.getMessage());
        }
        verify(addressApi, never()).createAddress(any(Address.class));
    }

    @Test
    public void testReplaceAddress() throws Exception {
        when(addressApi.replaceAddress(any())).thenReturn(true);

        helper.replaceAddress("test", createAddress("q1"));
        verify(addressApi).replaceAddress(eq(createAddress("q1")));
    }

    @Test
    public void testReplaceAddressNotFound() throws Exception {
        when(addressApi.replaceAddress(any())).thenReturn(false);
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("Address q1 not found");

        helper.replaceAddress("test", createAddress("q1"));
        verify(addressApi, never()).replaceAddress(any(Address.class));
    }

    @Test
    public void testReplaceAddressWithInvalidAddress() throws Exception {
        Set<Address> addresses = new HashSet<>();
        addresses.add(createAddress("q1", "q1"));
        addresses.add(createAddress("q2", "q2"));
        when(addressApi.listAddresses(any())).thenReturn(addresses);
        when(addressApi.replaceAddress(any())).thenReturn(true);
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Address 'q2' already exists with resource name 'q2'");

        Address invalidAddress = createAddress("q1", "q2");
        helper.replaceAddress("test", invalidAddress);
        verify(addressApi, never()).replaceAddress(any(Address.class));
    }

    @Test
    public void testDeleteAddress() throws Exception {
        Address address = createAddress("testAddress");
        when(addressApi.getAddressWithName(same("ns"), same(address.getAddress()))).thenReturn(Optional.of(address));
        when(addressApi.deleteAddress(same(address))).thenReturn(true);
        assertNotNull(helper.deleteAddress("ns", "test", address.getName()));
    }

    @Test
    public void testDeleteAddressNotFound() throws Exception {
        Address address = createAddress("testAddress");
        when(addressApi.getAddressWithName(same("ns"), same(address.getAddress()))).thenReturn(Optional.empty());
        assertNull(helper.deleteAddress("ns", "test", address.getName()));
    }

    @Test
    public void testDeleteAddressReturningFalse() throws Exception {
        Address address = createAddress("testAddress");
        when(addressApi.getAddressWithName(same("ns"), same(address.getAddress()))).thenReturn(Optional.of(address));
        when(addressApi.deleteAddress(same(address))).thenReturn(false);
        assertNull(helper.deleteAddress("ns", "test", address.getName()));
    }

    @Test
    public void testDuplicateAddresses() throws Exception {
        when(addressApi.listAddresses(any())).thenReturn(Sets.newSet(createAddress("q1"), createAddress("q2")));

        try {
            helper.createAddress("test", createAddress("q3", "q1"));
            fail("Expected exception for duplicate address");
        } catch (BadRequestException e) {
            assertThat(e.getMessage(), is("Address 'q1' already exists with resource name 'q1'"));
        }
    }

    @Test
    public void testParseLabelSelector() throws Exception {
        Map<String, String> labels = AddressApiHelper.parseLabelSelector("key=value");
        assertThat(labels.size(), is(1));
        assertThat(labels.get("key"), is("value"));

        labels = AddressApiHelper.parseLabelSelector("key1=value1,key2=value2,key3=value3");
        assertThat(labels.size(), is(3));
        assertThat(labels.get("key1"), is("value1"));
        assertThat(labels.get("key2"), is("value2"));
        assertThat(labels.get("key3"), is("value3"));
    }

    private Address createAddress(String name, String address)
    {
        return new Address.Builder().setName(name).setNamespace("ns").setAddress(address).setAddressSpace("test").setType("queue").setPlan("plan1").build();
    }

    private Address createAddress(String address)
    {
        return createAddress(address, address);
    }
}
