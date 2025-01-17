/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.api.common.DefaultExceptionMapper;
import io.enmasse.api.common.Status;
import io.enmasse.api.server.TestSchemaProvider;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.k8s.model.v1beta1.Table;
import io.enmasse.k8s.util.TimeUtil;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpAddressSpaceServiceTest {
    private HttpAddressSpaceService addressSpaceService;
    private TestAddressSpaceApi addressSpaceApi;
    private AddressSpace a1;
    private AddressSpace a2;
    private DefaultExceptionMapper exceptionMapper = new DefaultExceptionMapper();
    private SecurityContext securityContext;

    @Before
    public void setup() {
        addressSpaceApi = new TestAddressSpaceApi();
        addressSpaceService = new HttpAddressSpaceService(addressSpaceApi, new TestSchemaProvider(), Clock.systemUTC());
        securityContext = mock(SecurityContext.class);
        when(securityContext.isUserInRole(any())).thenReturn(true);
        a1 = new AddressSpace.Builder()
                .setName("a1")
                .setNamespace("myspace")
                .setType("type1")
                .setPlan("myplan")
                .setCreationTimestamp(TimeUtil.formatRfc3339(Instant.ofEpochSecond(123)))
                .setEndpointList(Arrays.asList(
                        new EndpointSpec.Builder()
                            .setName("messaging")
                            .setService("messaging")
                        .build(),
                        new EndpointSpec.Builder()
                            .setName("mqtt")
                            .setService("mqtt")
                        .build()))
                .build();

        a2 = new AddressSpace.Builder()
                .setName("a2")
                .setType("type1")
                .setPlan("myplan")
                .setCreationTimestamp(TimeUtil.formatRfc3339(Instant.ofEpochSecond(12)))
                .setNamespace("othernamespace")
                .build();
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
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.createAddressSpace(a2);
        Response response = invoke(() -> addressSpaceService.getAddressSpaceList(securityContext, MediaType.APPLICATION_JSON, null, null));
        assertThat(response.getStatus(), is(200));
        AddressSpaceList data = (AddressSpaceList) response.getEntity();

        assertThat(data.size(), is(2));
        assertThat(data, hasItem(a1));
        assertThat(data, hasItem(a2));
    }

    @Test
    public void testListTableFormat() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.createAddressSpace(a2);
        Response response = invoke(() -> addressSpaceService.getAddressSpaceList(securityContext, "application/json;as=Table;g=meta.k8s.io;v=v1beta1", null, null));
        assertThat(response.getStatus(), is(200));
        Table data = (Table) response.getEntity();

        assertThat(data.getColumnDefinitions().size(), is(6));
        assertThat(data.getRows().size(), is(2));
    }

    @Test
    public void testListException() {
        addressSpaceApi.throwException = true;
        Response response = invoke(() -> addressSpaceService.getAddressSpaceList(securityContext, MediaType.APPLICATION_JSON, null, null));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGet() {
        addressSpaceApi.createAddressSpace(a1);
        Response response = invoke(() -> addressSpaceService.getAddressSpace(securityContext, null, null, "a1"));
        assertThat(response.getStatus(), is(200));
        AddressSpace data = ((AddressSpace)response.getEntity());

        assertThat(data, is(a1));
        assertThat(data.getEndpoints().size(), is(a1.getEndpoints().size()));
    }

    @Test
    public void testGetTableFormat() {
        addressSpaceApi.createAddressSpace(a1);
        Response response = invoke(() -> addressSpaceService.getAddressSpace(securityContext, "application/json;as=Table;g=meta.k8s.io;v=v1beta1", null, "a1"));
        assertThat(response.getStatus(), is(200));
        Table data = ((Table)response.getEntity());

        assertThat(data.getColumnDefinitions().size(), is(6));
        assertThat(data.getRows().get(0).getObject().getMetadata().getName(), is(a1.getName()));
    }

    @Test
    public void testPut() {
        addressSpaceApi.createAddressSpace(a1);
        AddressSpace a1Adapted = new AddressSpace.Builder(a1).putAnnotation("foo", "bar").build();
        Response response = invoke(() -> addressSpaceService.replaceAddressSpace(securityContext, null, a1Adapted.getName(), a1Adapted));
        assertThat(response.getStatus(), is(200));

        assertFalse(addressSpaceApi.listAddressSpaces(null).isEmpty());
        assertThat(addressSpaceApi.listAddressSpaces(null).iterator().next().getAnnotation("foo"), is("bar"));
    }

    @Test
    public void testPutChangeType() {
        addressSpaceApi.createAddressSpace(a1);
        AddressSpace a1Adapted = new AddressSpace.Builder(a1).setType("type2").build();
        Response response = invoke(() -> addressSpaceService.replaceAddressSpace(securityContext, null, a1Adapted.getName(), a1Adapted));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testPutChangePlan() {
        addressSpaceApi.createAddressSpace(a1);
        AddressSpace a1Adapted = new AddressSpace.Builder(a1).setPlan("otherplan").build();
        Response response = invoke(() -> addressSpaceService.replaceAddressSpace(securityContext, null, a1Adapted.getName(), a1Adapted));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testPutNonMatchingAddressSpaceName() {
        Response response = invoke(() -> addressSpaceService.replaceAddressSpace(securityContext, null, "xxxxxx", a1));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testPutNonExistingAddressSpace() {
        Response response = invoke(() -> addressSpaceService.replaceAddressSpace(securityContext, null, a1.getName(), a1));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testDelete() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.createAddressSpace(a2);

        Response response = invoke(() -> addressSpaceService.deleteAddressSpace(securityContext, null,"a1"));
        assertThat(response.getStatus(), is(200));
        assertThat(((Status)response.getEntity()).getStatusCode(), is(200));

        assertThat(addressSpaceApi.listAddressSpaces(null), hasItem(a2));
        assertThat(addressSpaceApi.listAddressSpaces(null).size(), is(1));
    }

    @Test
    public void testDeleteException() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.throwException = true;
        Response response = invoke(() -> addressSpaceService.deleteAddressSpace(securityContext, null,"a1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDeleteNotFound() {
        addressSpaceApi.createAddressSpace(a1);
        Response response = invoke(() -> addressSpaceService.deleteAddressSpace(securityContext, null,"doesnotexist"));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testDeleteAll() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.createAddressSpace(a2);

        Response response = invoke(() -> addressSpaceService.deleteAddressSpaces(securityContext, "myspace"));
        assertThat(response.getStatus(), is(200));
        assertThat(((Status)response.getEntity()).getStatusCode(), is(200));
    }
}
