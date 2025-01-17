package com.sap.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.constants.EdmTypeKind;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SelectItem;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.SystemQueryOptionKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sap.olingo.jpa.processor.core.api.JPAODataPage;
import com.sap.olingo.jpa.processor.core.api.JPAODataPagingProvider;
import com.sap.olingo.jpa.processor.core.util.IntegrationTestHelper;
import com.sap.olingo.jpa.processor.core.util.TestBase;

public class TestJPAServerDrivenPaging extends TestBase {
  @Test
  public void testReturnsNotImplementedIfPagingProviderNotAvailable() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$skiptoken=xyz");
    helper.assertStatus(501);
  }

  @Test
  public void testReturnsGoneIfPagingProviderRetunrsNullForSkiptoken() throws IOException, ODataException {
    final JPAODataPagingProvider provider = mock(JPAODataPagingProvider.class);
    when(provider.getNextPage("xyz")).thenReturn(null);
    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$skiptoken=xyz", provider);
    helper.assertStatus(410);

  }

  @Test
  public void testReturnsFullResultIfProviderDoesNotReturnPage() throws IOException, ODataException {
    final JPAODataPagingProvider provider = mock(JPAODataPagingProvider.class);
    when(provider.getFirstPage(any(), any(), any(), any())).thenReturn(null);
    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations", provider);
    helper.assertStatus(200);
    assertEquals(10, helper.getValues().size());
  }

  @Test
  public void testReturnsPartResultIfProviderPages() throws IOException, ODataException {

    final JPAODataPagingProvider provider = mock(JPAODataPagingProvider.class);
    when(provider.getFirstPage(any(), any(), any(), any())).thenAnswer(i -> new JPAODataPage((UriInfo) i
        .getArguments()[0], 0, 5, "Hugo"));
    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$orderby=ID desc", provider);
    helper.assertStatus(200);
    assertEquals(5, helper.getValues().size());
  }

  @Test
  public void testReturnsNextLinkIfProviderPages() throws IOException, ODataException {

    final JPAODataPagingProvider provider = mock(JPAODataPagingProvider.class);
    when(provider.getFirstPage(any(), any(), any(), any())).thenAnswer(i -> new JPAODataPage((UriInfo) i
        .getArguments()[0], 0, 5, "Hugo"));

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$orderby=ID desc", provider);
    helper.assertStatus(200);
    assertEquals(5, helper.getValues().size());
    assertEquals("Organizations?$skiptoken='Hugo'", helper.getValue().get("@odata.nextLink").asText());
  }

  @Test
  public void testReturnsNextLinkNotAStringIfProviderPages() throws IOException, ODataException {

    final JPAODataPagingProvider provider = mock(JPAODataPagingProvider.class);
    when(provider.getFirstPage(any(), any(), any(), any())).thenAnswer(i -> new JPAODataPage((UriInfo) i
        .getArguments()[0], 0, 5, new Integer(123456789)));

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$orderby=ID desc", provider);
    helper.assertStatus(200);
    assertEquals(5, helper.getValues().size());
    assertEquals("Organizations?$skiptoken=123456789", helper.getValue().get("@odata.nextLink").asText());
  }

  @Test
  public void testReturnsNextPagesRespectingFilter() throws IOException, ODataException {
    final UriInfo uriInfo = buildUriInfo();

    final JPAODataPagingProvider provider = mock(JPAODataPagingProvider.class);
    when(provider.getNextPage("xyz")).thenReturn(new JPAODataPage(uriInfo, 5, 5, null));

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$skiptoken=xyz", provider);
    helper.assertStatus(200);
    assertEquals(5, helper.getValues().size());
    ObjectNode org = (ObjectNode) helper.getValues().get(4);
    assertEquals("1", org.get("ID").asText());
  }

  @Test
  public void testEntityManagerProvided() throws IOException, ODataException {

    final JPAODataPagingProvider provider = mock(JPAODataPagingProvider.class);
    when(provider.getFirstPage(any(), any(), any(), any())).thenAnswer(i -> new JPAODataPage((UriInfo) i
        .getArguments()[0], 0, 5, "Hugo"));

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$orderby=ID desc", provider);
    helper.assertStatus(200);

    verify(provider).getFirstPage(any(), any(), any(), isNotNull());
  }

  @Test
  public void testCountQueryProvided() throws IOException, ODataException {

    final JPAODataPagingProvider provider = mock(JPAODataPagingProvider.class);
    when(provider.getFirstPage(any(), any(), any(), any())).thenAnswer(i -> new JPAODataPage((UriInfo) i
        .getArguments()[0], 0, 5, "Hugo"));
    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$orderby=ID desc", provider);
    helper.assertStatus(200);

    verify(provider).getFirstPage(any(), any(), isNotNull(), any());
  }

  @Test
  public void testMaxPageSizeHeaderProvided() throws IOException, ODataException {

    headers = new HashMap<>();
    final List<String> headerValues = new ArrayList<>(1);
    final JPAODataPagingProvider provider = mock(JPAODataPagingProvider.class);

    when(provider.getFirstPage(any(), any(), any(), any())).thenAnswer(i -> new JPAODataPage((UriInfo) i
        .getArguments()[0], 0, 5, "Hugo"));
    headerValues.add("odata.maxpagesize=50");
    headers.put("Prefer", headerValues);

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$orderby=ID desc", provider, headers);
    helper.assertStatus(200);

    verify(provider).getFirstPage(any(), isNotNull(), any(), any());
  }

  @Test
  public void testMaxPageSizeHeaderProvidedInLowerCase() throws IOException, ODataException {
    headers = new HashMap<>();
    final List<String> headerValues = new ArrayList<>(1);
    final JPAODataPagingProvider provider = mock(JPAODataPagingProvider.class);
    when(provider.getFirstPage(any(), any(), any(), any())).thenAnswer(i -> new JPAODataPage((UriInfo) i
        .getArguments()[0], 0, 5, "Hugo"));
    headerValues.add("odata.maxpagesize=50");
    headers.put("prefer", headerValues);

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$orderby=ID desc", provider, headers);
    helper.assertStatus(200);

    verify(provider).getFirstPage(any(), isNotNull(), any(), any());
  }

  @Test
  public void testUriInfoProvided() throws IOException, ODataException {

    final JPAODataPagingProvider provider = mock(JPAODataPagingProvider.class);

    when(provider.getFirstPage(any(), any(), any(), any())).thenAnswer(i -> new JPAODataPage((UriInfo) i
        .getArguments()[0], 0, 5, "Hugo"));

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$orderby=ID desc", provider);
    helper.assertStatus(200);

    verify(provider).getFirstPage(isNotNull(), any(), any(), any());
  }

  @Test
  public void testMaxPageSiteHeaderNotANumber() throws IOException, ODataException {

    headers = new HashMap<>();
    final List<String> headerValues = new ArrayList<>(1);
    final JPAODataPagingProvider provider = mock(JPAODataPagingProvider.class);

    when(provider.getFirstPage(any(), any(), any(), any())).thenAnswer(i -> new JPAODataPage((UriInfo) i
        .getArguments()[0], 0, 5, "Hugo"));
    headerValues.add("odata.maxpagesize=Hugo");
    headers.put("Prefer", headerValues);

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$orderby=ID desc", provider, headers);
    helper.assertStatus(400);

  }

  @Test
  public void testSelectSubsetOfFields() throws IOException, ODataException {
    final UriInfo uriInfo = buildUriInfo();
    final JPAODataPagingProvider provider = mock(JPAODataPagingProvider.class);
    final SelectOption selOpt = mock(SelectOption.class);
    final List<SelectItem> selItems = new ArrayList<>();
    final SelectItem selItem = mock(SelectItem.class);

    when(uriInfo.getSelectOption()).thenReturn(selOpt);
    when(selOpt.getKind()).thenReturn(SystemQueryOptionKind.SELECT);
    when(selOpt.getSelectItems()).thenReturn(selItems);
    selItems.add(selItem);

    final UriInfoResource selectPath = mock(UriInfoResource.class);
    final List<UriResource> selectPathItems = new ArrayList<>(1);
    final UriResourcePrimitiveProperty selectResource = mock(UriResourcePrimitiveProperty.class);
    final EdmProperty selectProperty = mock(EdmProperty.class);
    selectPathItems.add(selectResource);
    when(selItem.getResourcePath()).thenReturn(selectPath);
    when(selectPath.getUriResourceParts()).thenReturn(selectPathItems);
    when(selectResource.getSegmentValue()).thenReturn("ID");
    when(selectResource.getProperty()).thenReturn(selectProperty);
    when(selectProperty.getName()).thenReturn("ID");

    when(provider.getFirstPage(any(), any(), any(), any())).thenAnswer(i -> new JPAODataPage((UriInfo) i
        .getArguments()[0], 0, 5, "Hugo"));

    when(provider.getNextPage("'Hugo'")).thenReturn(new JPAODataPage(uriInfo, 5, 5, "Willi"));
    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$orderby=ID desc&$select=ID",
        provider);
    helper.assertStatus(200);
    assertNull(helper.getValues().get(0).get("Country"));
    IntegrationTestHelper act = new IntegrationTestHelper(emf, "Organizations?$skiptoken='Hugo'",
        provider);
    act.assertStatus(200);
    assertEquals(5, act.getValues().size());
    assertNull(act.getValues().get(0).get("Country"));

  }

  private UriInfo buildUriInfo() throws EdmPrimitiveTypeException {
    final UriInfo uriInfo = mock(UriInfo.class);
    final UriResourceEntitySet uriEs = mock(UriResourceEntitySet.class);
    final EdmEntitySet es = mock(EdmEntitySet.class);
    final EdmEntityType et = mock(EdmEntityType.class);
    final EdmType type = mock(EdmType.class);
    final OrderByOption order = mock(OrderByOption.class);
    final OrderByItem orderItem = mock(OrderByItem.class);
    final Member orderExpression = mock(Member.class);
    final UriInfoResource orderResourcePath = mock(UriInfoResource.class);
    final UriResourcePrimitiveProperty orderResourcePathItem = mock(UriResourcePrimitiveProperty.class);
    final EdmProperty orderProperty = mock(EdmProperty.class);
    final List<OrderByItem> orderItems = new ArrayList<>();
    final List<UriResource> orderResourcePathItems = new ArrayList<>();

    final EdmProperty propertyID = mock(EdmProperty.class); // type.getStructuralProperty(propertyName);
    final EdmProperty propertyCountry = mock(EdmProperty.class);
    final EdmPrimitiveType propertyType = mock(EdmPrimitiveType.class);

    orderItems.add(orderItem);
    orderResourcePathItems.add(orderResourcePathItem);
    when(uriEs.getKind()).thenReturn(UriResourceKind.entitySet);
    when(uriEs.getEntitySet()).thenReturn(es);
    when(uriEs.getType()).thenReturn(type);
    when(uriEs.isCollection()).thenReturn(true);
    when(es.getName()).thenReturn("Organizations");
    when(es.getEntityType()).thenReturn(et);
    when(type.getNamespace()).thenReturn("com.sap.olingo.jpa");
    when(type.getName()).thenReturn("Organization");
    when(et.getFullQualifiedName()).thenReturn(new FullQualifiedName("com.sap.olingo.jpa", "Organization"));
    when(et.getPropertyNames()).thenReturn(Arrays.asList("ID", "Country"));
    when(et.getStructuralProperty("ID")).thenReturn(propertyID);
    when(et.getStructuralProperty("Country")).thenReturn(propertyCountry);

    when(propertyID.getName()).thenReturn("ID");
    when(propertyID.isPrimitive()).thenReturn(true);
    when(propertyID.getType()).thenReturn(propertyType);
    when(propertyCountry.getName()).thenReturn("Country");
    when(propertyCountry.isPrimitive()).thenReturn(true);
    when(propertyCountry.getType()).thenReturn(propertyType);
    when(propertyType.getKind()).thenReturn(EdmTypeKind.PRIMITIVE);
    when(propertyType.valueToString(any(), any(), any(), any(), any(), any())).thenAnswer(i -> i.getArguments()[0]
        .toString());

    when(order.getKind()).thenReturn(SystemQueryOptionKind.ORDERBY);
    when(orderItem.isDescending()).thenReturn(true);
    when(orderItem.getExpression()).thenReturn(orderExpression);
    when(orderExpression.getResourcePath()).thenReturn(orderResourcePath);
    when(orderResourcePath.getUriResourceParts()).thenReturn(orderResourcePathItems);
    when(orderResourcePathItem.getProperty()).thenReturn(orderProperty);
    when(orderProperty.getName()).thenReturn("ID");
    when(order.getOrders()).thenReturn(orderItems);
    final List<UriResource> resourceParts = new ArrayList<>();
    resourceParts.add(uriEs);
    when(uriInfo.getUriResourceParts()).thenReturn(resourceParts);
    when(uriInfo.getOrderByOption()).thenReturn(order);
    return uriInfo;
  }
}
