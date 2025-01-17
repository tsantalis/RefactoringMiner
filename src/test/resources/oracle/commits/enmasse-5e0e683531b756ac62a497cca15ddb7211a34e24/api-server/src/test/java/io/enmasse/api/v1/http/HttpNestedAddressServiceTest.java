/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.v1.Either;
import io.enmasse.api.common.DefaultExceptionMapper;
import io.enmasse.api.common.Status;
import io.enmasse.api.server.TestSchemaProvider;
import io.enmasse.k8s.api.TestAddressApi;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.k8s.model.v1beta1.Table;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpNestedAddressServiceTest {
    private HttpNestedAddressService addressService;
    private TestAddressSpaceApi addressSpaceApi;
    private TestAddressApi addressApi;
    private Address q1;
    private Address a1;
    private DefaultExceptionMapper exceptionMapper = new DefaultExceptionMapper();
    private SecurityContext securityContext;

    @BeforeEach
    public void setup() {
        addressSpaceApi = new TestAddressSpaceApi();
        this.addressService = new HttpNestedAddressService(addressSpaceApi, new TestSchemaProvider(), Clock.fixed(Instant.ofEpochSecond(1234), ZoneId.of("UTC")));
        securityContext = mock(SecurityContext.class);
        when(securityContext.isUserInRole(any())).thenReturn(true);

        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setType("type1")
                .setPlan("myplan")
                .build();

        addressSpaceApi.createAddressSpace(addressSpace);
        addressApi = (TestAddressApi) addressSpaceApi.withAddressSpace(addressSpace);
        q1 = new Address.Builder()
                .setName("q1")
                .setAddress("Q1")
                .setAddressSpace("myspace")
                .setNamespace("ns")
                .setType("queue")
                .build();
        a1 = new Address.Builder()
                .setName("a1")
                .setAddress("A1")
                .setAddressSpace("myspace")
                .setNamespace("ns")
                .setType("anycast")
                .build();
        addressApi.createAddress(q1);
        addressApi.createAddress(a1);
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
        Response response = invoke(() -> addressService.getAddressList(securityContext, null, "ns", "myspace", null, null));

        assertThat(response.getStatus(), is(200));
        AddressList list = (AddressList) response.getEntity();

        assertThat(list.size(), is(2));
        assertThat(list, hasItem(q1));
        assertThat(list, hasItem(a1));
    }

    @Test
    public void testListTableFormat() {
        Response response = invoke(() -> addressService.getAddressList(securityContext, "application/json;as=Table;g=meta.k8s.io;v=v1beta1", "ns", "myspace", null, null));

        assertThat(response.getStatus(), is(200));
        Table table = (Table) response.getEntity();

        assertThat(table.getColumnDefinitions().size(), is(9));
        assertThat(table.getRows().size(), is(2));
    }

    @Test
    public void testGetByAddress() {
        Response response = invoke(() -> addressService.getAddressList(securityContext, null, "ns", "myspace", "A1", null));

        assertThat(response.getStatus(), is(200));
        Address address = (Address) response.getEntity();

        assertThat(address, is(a1));
    }

    @Test
    public void testGetByAddressNotFound() {
        Response response = invoke(() -> addressService.getAddressList(securityContext, null, "ns", "myspace", "b1", null));

        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testListException() {
        addressApi.throwException = true;
        Response response = invoke(() -> addressService.getAddressList(securityContext, null, "ns", "myspace", null, null));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGet() {
        Response response = invoke(() -> addressService.getAddress(securityContext, null, "ns", "myspace", "q1"));
        assertThat(response.getStatus(), is(200));
        Address address = (Address) response.getEntity();

        assertThat(address, is(q1));
    }

    @Test
    public void testGetTableFormat() {
        Response response = invoke(() -> addressService.getAddress(securityContext, "application/json;as=Table;g=meta.k8s.io;v=v1beta1", "ns", "myspace", "q1"));
        assertThat(response.getStatus(), is(200));
        Table table = (Table) response.getEntity();

        assertThat(table.getColumnDefinitions().size(), is(9));
        assertThat(table.getRows().get(0).getObject().getMetadata().getName(), is(q1.getName()));
    }

    @Test
    public void testGetException() {
        addressApi.throwException = true;
        Response response = invoke(() -> addressService.getAddress(securityContext, null, "ns", "myspace", "q1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGetUnknown() {
        Response response = invoke(() -> addressService.getAddress(securityContext, null, "ns", "myspace", "doesnotexist"));
        assertThat(response.getStatus(), is(404));
    }


    @Test
    public void testCreate() {
        Address a2 = new Address.Builder()
                .setAddress("a2")
                .setType("anycast")
                .setPlan("plan1")
                .setAddressSpace("myspace")
                .build();
        Response response = invoke(() -> addressService.createAddress(securityContext, new ResteasyUriInfo("http://localhost:8443/", null, "/"), "ns", "myspace", Either.createLeft(a2)));
        assertThat(response.getStatus(), is(201));

        Address a2ns = new Address.Builder(a2).setNamespace("ns").build();
        assertThat(addressApi.listAddresses("ns"), hasItem(a2ns));
    }

    @Test
    public void testCreateException() {
        addressApi.throwException = true;
        Address a2 = new Address.Builder()
                .setAddress("a2")
                .setPlan("plan1")
                .setAddressSpace("myspace")
                .setType("anycast")
                .build();
        Response response = invoke(() -> addressService.createAddress(securityContext, null, "ns", "myspace", Either.createLeft(a2)));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testPut() {
        Set<Address> addresses = addressApi.listAddresses("ns");
        assertThat(addresses.isEmpty(), is(false));
        Address address = addresses.iterator().next();
        Address a1 = new Address.Builder(address).setPlan("plan1").build();

        Response response = invoke(() -> addressService.replaceAddress(securityContext, "ns", "myspace", a1.getName(), a1));
        assertThat(response.getStatus(), is(200));

        Address a2ns = new Address.Builder(a1).setNamespace("ns").build();
        assertThat(addressApi.listAddresses("ns"), hasItem(a2ns));
    }

    @Test
    public void testPutNonMatchingAddressName() {
        Address a2 = new Address.Builder()
                .setName("a2")
                .setAddress("a2")
                .setType("anycast")
                .setPlan("plan1")
                .setAddressSpace("myspace")
                .build();
        Response response = invoke(() -> addressService.replaceAddress(securityContext, "ns", "myspace", "xxxxxxx", a2));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testPutNonExistingAddress() {
        Address a2 = new Address.Builder()
                .setName("a2")
                .setAddress("a2")
                .setType("anycast")
                .setPlan("plan1")
                .setAddressSpace("myspace")
                .build();
        Response response = invoke(() -> addressService.replaceAddress(securityContext, "ns", "myspace", a2.getName(), a2));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testDelete() {
        Response response = invoke(() -> addressService.deleteAddress(securityContext, "ns", "myspace", "a1"));
        assertThat(response.getStatus(), is(200));
        assertThat(((Status) response.getEntity()).getStatusCode(), is(200));

        assertThat(addressApi.listAddresses("ns"), hasItem(q1));
        assertThat(addressApi.listAddresses("ns").size(), is(1));
    }

    @Test
    public void testDeleteException() {
        addressApi.throwException = true;
        Response response = invoke(() -> addressService.deleteAddress(securityContext, "ns", "myspace", "a1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDeleteNotFound() {
        Response response = invoke(() -> addressService.deleteAddress(securityContext, "ns", "myspace", "notFound"));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void deleteAllAddresses() {
        Response response = invoke(() -> addressService.deleteAddresses(securityContext, "unknown"));
        assertThat(response.getStatus(), is(200));
        assertThat(addressApi.listAddresses("ns").size(), is(2));

        response = invoke(() -> addressService.deleteAddresses(securityContext, "ns"));
        assertThat(response.getStatus(), is(200));
        assertThat(((Status) response.getEntity()).getStatusCode(), is(200));
        assertThat(addressApi.listAddresses("ns").size(), is(0));
    }
}
