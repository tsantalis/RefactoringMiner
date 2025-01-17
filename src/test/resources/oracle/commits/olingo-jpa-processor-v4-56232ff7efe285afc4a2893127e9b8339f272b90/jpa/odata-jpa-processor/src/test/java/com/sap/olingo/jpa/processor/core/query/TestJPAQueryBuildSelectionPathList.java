package com.sap.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.UriResourceValue;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.junit.Before;
import org.junit.Test;

import com.sap.olingo.jpa.metadata.api.JPAEdmProvider;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.impl.JPAEdmNameBuilder;
import com.sap.olingo.jpa.processor.core.api.JPAODataContextAccessDouble;
import com.sap.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import com.sap.olingo.jpa.processor.core.util.SelectOptionDouble;
import com.sap.olingo.jpa.processor.core.util.TestBase;
import com.sap.olingo.jpa.processor.core.util.TestHelper;
import com.sap.olingo.jpa.processor.core.util.UriInfoDouble;

public class TestJPAQueryBuildSelectionPathList extends TestBase {

  private JPAAbstractJoinQuery cut;
  private JPAODataSessionContextAccess context;
  private UriInfo uriInfo;

  @Before
  public void setup() throws ODataException {
    buildUriInfo("BusinessPartners", "BusinessPartner");

    helper = new TestHelper(emf, PUNIT_NAME);
    nameBuilder = new JPAEdmNameBuilder(PUNIT_NAME);
    createHeaders();
    context = new JPAODataContextAccessDouble(new JPAEdmProvider(PUNIT_NAME, emf, null, TestBase.enumPackages), ds,
        null);

    cut = new JPAJoinQuery(null, context, emf.createEntityManager(), headers, uriInfo);

  }

  private List<UriResource> buildUriInfo(final String esName, final String etName) {
    uriInfo = mock(UriInfo.class);
    final EdmEntitySet odataEs = mock(EdmEntitySet.class);
    final EdmType odataType = mock(EdmEntityType.class);
    final List<UriResource> resources = new ArrayList<>();
    final UriResourceEntitySet esResource = mock(UriResourceEntitySet.class);
    when(uriInfo.getUriResourceParts()).thenReturn(resources);
    when(esResource.getKeyPredicates()).thenReturn(new ArrayList<>(1));
    when(esResource.getEntitySet()).thenReturn(odataEs);
    when(esResource.getKind()).thenReturn(UriResourceKind.entitySet);
    when(esResource.getType()).thenReturn(odataType);
    when(odataEs.getName()).thenReturn(esName);
    when(odataType.getNamespace()).thenReturn(PUNIT_NAME);
    when(odataType.getName()).thenReturn(etName);
    resources.add(esResource);
    return resources;
  }

  @Test
  public void checkSelectAllAsNoSelectionGiven() throws ODataApplicationException {
    final List<JPAPath> act = cut.buildSelectionPathList(uriInfo);
    assertEquals(23, act.size());
  }

  @Test
  public void checkSelectAllAsStarGiven() throws ODataApplicationException {

    final List<JPAPath> act = cut.buildSelectionPathList(new UriInfoDouble(new SelectOptionDouble("*")));
    assertEquals(23, act.size());
  }

  @Test
  public void checkSelectPrimitiveWithKey() throws ODataApplicationException {
    final List<JPAPath> act = cut.buildSelectionPathList(new UriInfoDouble(new SelectOptionDouble("Country")));
    assertEquals(2, act.size());
  }

  @Test
  public void checkSelectAllFromComplexWithKey() throws ODataApplicationException {
    final List<JPAPath> act = cut.buildSelectionPathList(new UriInfoDouble(new SelectOptionDouble("Address")));
    assertEquals(10, act.size());
  }

  @Test
  public void checkSelectKeyNoDuplicates() throws ODataApplicationException {
    final List<JPAPath> act = cut.buildSelectionPathList(new UriInfoDouble(new SelectOptionDouble("ID")));
    assertEquals(1, act.size());
  }

  @Test
  public void checkSelectAllFromNavigationComplexPrimitiveWithKey() throws ODataApplicationException {
    final List<JPAPath> act = cut.buildSelectionPathList(new UriInfoDouble(new SelectOptionDouble(
        "Address/CountryName")));
    assertEquals(2, act.size());
  }

  @Test
  public void checkSelectTwoPrimitiveWithKey() throws ODataApplicationException {
    final List<JPAPath> act = cut.buildSelectionPathList(new UriInfoDouble(new SelectOptionDouble("Country,ETag")));
    assertEquals(3, act.size());
  }

  @Test
  public void checkSelectAllFromComplexAndOnePrimitiveWithKey() throws ODataApplicationException {
    final List<JPAPath> act = cut.buildSelectionPathList(new UriInfoDouble(new SelectOptionDouble("Address,ETag")));
    assertEquals(11, act.size());
  }

  @Test
  public void checkSelectAllFromNavgateComplexPrimitiveAndOnePrimitiveWithKey() throws ODataApplicationException {
    final List<JPAPath> act = cut.buildSelectionPathList(new UriInfoDouble(new SelectOptionDouble(
        "Address/CountryName,Country")));
    assertEquals(3, act.size());
  }

  @Test
  public void checkSelectNavigationCompex() throws ODataException {
    List<UriResource> resourcePath = buildUriInfo("BusinessPartners", "BusinessPartner");
    final UriResourceComplexProperty complexResource = mock(UriResourceComplexProperty.class);
    final EdmProperty property = mock(EdmProperty.class);
    when(complexResource.getProperty()).thenReturn(property);
    when(property.getName()).thenReturn("AdministrativeInformation");
    resourcePath.add(complexResource);

    cut = new JPAJoinQuery(null, context, emf.createEntityManager(), headers, uriInfo);

    final List<JPAPath> act = cut.buildSelectionPathList(uriInfo);
    assertEquals(5, act.size());
  }

  @Test
  public void checkSelectNavigationCompexComplex() throws ODataException {
    List<UriResource> resourcePath = buildUriInfo("BusinessPartners", "BusinessPartner");
    final UriResourceComplexProperty adminInfoResource = mock(UriResourceComplexProperty.class);
    final EdmProperty adminInfoProperty = mock(EdmProperty.class);
    when(adminInfoResource.getProperty()).thenReturn(adminInfoProperty);
    when(adminInfoProperty.getName()).thenReturn("AdministrativeInformation");
    resourcePath.add(adminInfoResource);

    final UriResourceComplexProperty createdResource = mock(UriResourceComplexProperty.class);
    final EdmProperty createdProperty = mock(EdmProperty.class);
    when(createdResource.getProperty()).thenReturn(createdProperty);
    when(createdProperty.getName()).thenReturn("Created");
    resourcePath.add(createdResource);

    cut = new JPAJoinQuery(null, context, emf.createEntityManager(), headers, uriInfo);

    final List<JPAPath> act = cut.buildSelectionPathList(uriInfo);
    assertEquals(3, act.size());
  }

  @Test
  public void checkSelectNavigationCompexComplexProperty() throws ODataException {
    List<UriResource> resourcePath = buildUriInfo("BusinessPartners", "BusinessPartner");
    final UriResourceComplexProperty adminInfoResource = mock(UriResourceComplexProperty.class);
    final EdmProperty adminInfoProperty = mock(EdmProperty.class);
    when(adminInfoResource.getProperty()).thenReturn(adminInfoProperty);
    when(adminInfoProperty.getName()).thenReturn("AdministrativeInformation");
    resourcePath.add(adminInfoResource);

    final UriResourceComplexProperty createdResource = mock(UriResourceComplexProperty.class);
    final EdmProperty createdProperty = mock(EdmProperty.class);
    when(createdResource.getProperty()).thenReturn(createdProperty);
    when(createdProperty.getName()).thenReturn("Created");
    resourcePath.add(createdResource);

    final UriResourcePrimitiveProperty byResource = mock(UriResourcePrimitiveProperty.class);
    final EdmProperty byProperty = mock(EdmProperty.class);
    when(byResource.getProperty()).thenReturn(byProperty);
    when(byProperty.getName()).thenReturn("By");
    resourcePath.add(byResource);

    cut = new JPAJoinQuery(null, context, emf.createEntityManager(), headers, uriInfo);

    final List<JPAPath> act = cut.buildSelectionPathList(uriInfo);
    assertEquals(2, act.size());
  }

  @Test
  public void checkSelectNavigationPropertyValue() throws ODataException {
    List<UriResource> resourcePath = buildUriInfo("BusinessPartners", "BusinessPartner");

    final UriResourcePrimitiveProperty byResource = mock(UriResourcePrimitiveProperty.class);
    final EdmProperty byProperty = mock(EdmProperty.class);
    when(byResource.getProperty()).thenReturn(byProperty);
    when(byProperty.getName()).thenReturn("Country");
    resourcePath.add(byResource);

    final UriResourceValue valueResource = mock(UriResourceValue.class);
    when(valueResource.getSegmentValue()).thenReturn(Util.VALUE_RESOURCE.toLowerCase());
    resourcePath.add(valueResource);

    cut = new JPAJoinQuery(null, context, emf.createEntityManager(), headers, uriInfo);

    final List<JPAPath> act = cut.buildSelectionPathList(uriInfo);
    assertEquals(2, act.size());
  }

  @Test
  public void checkSelectNavigationCompexWithSelectPrimitive() throws ODataException {
    List<UriResource> resourcePath = buildUriInfo("BusinessPartners", "BusinessPartner");
    final UriResourceComplexProperty addressResource = mock(UriResourceComplexProperty.class);
    final EdmProperty addressProperty = mock(EdmProperty.class);
    when(addressResource.getProperty()).thenReturn(addressProperty);
    when(addressProperty.getName()).thenReturn("Address");
    resourcePath.add(addressResource);

    SelectOption selOptions = new SelectOptionDouble("CountryName");

    when(uriInfo.getSelectOption()).thenReturn(selOptions);

    cut = new JPAJoinQuery(null, context, emf.createEntityManager(), headers, uriInfo);

    final List<JPAPath> act = cut.buildSelectionPathList(uriInfo);
    assertEquals(2, act.size());
  }
}
