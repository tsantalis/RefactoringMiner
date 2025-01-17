package com.sap.olingo.jpa.processor.core.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.ex.ODataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAOnConditionItem;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.processor.core.modify.JPAConversionHelper;

public class TestJPARequestLinkImpl {
  private JPARequestLinkImpl cut;
  private JPAAssociationPath path;
  private List<JPAOnConditionItem> items;
  private JPAConversionHelper helper;
  private JPAAssociationAttribute pathLeaf;

  @BeforeEach
  public void setUp() throws ODataJPAModelException {
    helper = new JPAConversionHelper();
    items = new ArrayList<>();
    path = mock(JPAAssociationPath.class);
    pathLeaf = mock(JPAAssociationAttribute.class);
    when(path.getJoinColumnsList()).thenReturn(items);
  }

  @Disabled
  @Test
  public void testCreateMultipleStringKeysChildren() throws ODataJPAModelException, ODataException {
    String link = "AdministrativeDivisions(DivisionCode='DE100',CodeID='NUTS3',CodePublisher='Eurostat')";
    cut = new JPARequestLinkImpl(path, link, helper);

    items.add(createConditionItem("codePublisher", "CodePublisher", "codePublisher", "CodePublisher"));
    items.add(createConditionItem("codeID", "CodeID", "parentCodeID", "ParentCodeID"));
    items.add(createConditionItem("divisionCode", "DivisionCode", "parentDivisionCode", "ParentDivisionCode"));
    Map<String, Object> act = cut.getRelatedKeys();
    assertNotNull(act);
    assertEquals("DE100", act.get("parentDivisionCode"));
    assertEquals("NUTS3", act.get("parentCodeID"));
    assertEquals("Eurostat", act.get("codePublisher"));
  }

  @Test
  public void testCreateMultipleStringKeysParent() throws ODataJPAModelException, ODataException {
    String link = "AdministrativeDivisions(DivisionCode='DE100',CodeID='NUTS3',CodePublisher='Eurostat')";
    cut = new JPARequestLinkImpl(path, link, helper);

    items.add(createConditionItem("codePublisher", "CodePublisher", "codePublisher", "CodePublisher"));
    items.add(createConditionItem("parentCodeID", "ParentCodeID", "codeID", "CodeID"));
    items.add(createConditionItem("parentDivisionCode", "ParentDivisionCode", "divisionCode", "DivisionCode"));
    Map<String, Object> act = cut.getRelatedKeys();
    assertNotNull(act);
    assertEquals("DE100", act.get("divisionCode"));
    assertEquals("NUTS3", act.get("codeID"));
    assertEquals("Eurostat", act.get("codePublisher"));
  }

  @Disabled
  @Test
  public void testCreateMultipleStringValuesChildren() throws ODataJPAModelException, ODataException {
    String link = "AdministrativeDivisions(DivisionCode='DE100',CodeID='NUTS3',CodePublisher='Eurostat')";
    cut = new JPARequestLinkImpl(path, link, helper);

    items.add(createConditionItem("codePublisher", "CodePublisher", "codePublisher", "CodePublisher"));
    items.add(createConditionItem("codeID", "CodeID", "parentCodeID", "ParentCodeID"));
    items.add(createConditionItem("divisionCode", "DivisionCode", "parentDivisionCode", "ParentDivisionCode"));
    Map<String, Object> act = cut.getValues();

    assertNotNull(act);
    assertEquals("DE100", act.get("divisionCode"));
    assertEquals("NUTS3", act.get("codeID"));
    assertEquals("Eurostat", act.get("codePublisher"));
  }

  @Test
  public void testCreateMultipleStringValuesParent() throws ODataJPAModelException, ODataException {
    String link = "AdministrativeDivisions(DivisionCode='DE100',CodeID='NUTS3',CodePublisher='Eurostat')";
    cut = new JPARequestLinkImpl(path, link, helper);

    items.add(createConditionItem("codePublisher", "CodePublisher", "codePublisher", "CodePublisher"));
    items.add(createConditionItem("parentCodeID", "ParentCodeID", "codeID", "CodeID"));
    items.add(createConditionItem("parentDivisionCode", "ParentDivisionCode", "divisionCode", "DivisionCode"));
    Map<String, Object> act = cut.getValues();

    assertNotNull(act);
    assertEquals("DE100", act.get("parentDivisionCode"));
    assertEquals("NUTS3", act.get("parentCodeID"));
    assertEquals("Eurostat", act.get("codePublisher"));
  }

  @Test
  public void testCreateSingleStringKey() throws ODataJPAModelException, ODataException {
    String link = "BusinessPartners('123456')";
    cut = new JPARequestLinkImpl(path, link, helper);

    completeJPAPath();

    Map<String, Object> act = cut.getRelatedKeys();
    assertNotNull(act);
    assertEquals("123456", act.get("ID"));
  }

  @Test
  public void testCreateSingleStringValue() throws ODataJPAModelException, ODataException {
    String link = "BusinessPartners('123456')";
    cut = new JPARequestLinkImpl(path, link, helper);
    completeJPAPath();

    Map<String, Object> act = cut.getValues();
    assertNotNull(act);
    assertEquals("123456", act.get("businessPartnerID"));
  }

  private void completeJPAPath() throws ODataJPAModelException {
    JPAEntityType targetEt = mock(JPAEntityType.class);
    JPAEntityType sourceEt = mock(JPAEntityType.class);
    JPAAttribute bupaKey = mock(JPAAttribute.class);
    JPAAttribute roleKey1 = mock(JPAAttribute.class);
    JPAAttribute roleKey2 = mock(JPAAttribute.class);
    List<JPAAttribute> bupaKeys = new ArrayList<>();
    List<JPAAttribute> roleKeys = new ArrayList<>();
    bupaKeys.add(bupaKey);
    roleKeys.add(roleKey1);
    roleKeys.add(roleKey2);
    when(bupaKey.getInternalName()).thenReturn("ID");
    when(bupaKey.getExternalName()).thenReturn("ID");
    when(roleKey1.getInternalName()).thenReturn("businessPartnerID");
    when(roleKey1.getExternalName()).thenReturn("BusinessPartnerID");
    when(roleKey2.getInternalName()).thenReturn("roleCategory");
    when(roleKey2.getExternalName()).thenReturn("BusinessPartnerRole");

    items.add(createConditionItem("businessPartnerID", "BusinessPartnerID", "ID", "ID"));
    when(path.getLeaf()).thenReturn(pathLeaf);
    when(pathLeaf.getInternalName()).thenReturn("businessPartner");
    when(path.getTargetType()).thenReturn(targetEt);
    when(path.getSourceType()).thenReturn(sourceEt);
    when(targetEt.getKey()).thenReturn(bupaKeys);
    when(sourceEt.getKey()).thenReturn(roleKeys);
  }

  private JPAOnConditionItem createConditionItem(String leftInternalName, String leftExternalName,
      String rightInternalName, String rightExternalName) throws ODataJPAModelException {

    JPAOnConditionItem item = mock(JPAOnConditionItem.class);

    JPAPath leftPath = mock(JPAPath.class);
    JPAPath rightPath = mock(JPAPath.class);
    when(item.getLeftPath()).thenReturn(leftPath);
    when(item.getRightPath()).thenReturn(rightPath);

    JPAAttribute leftAttribute = mock(JPAAttribute.class);
    JPAAttribute rightAttribute = mock(JPAAttribute.class);
    when(leftPath.getLeaf()).thenReturn(leftAttribute);
    when(rightPath.getLeaf()).thenReturn(rightAttribute);

    when(leftAttribute.getInternalName()).thenReturn(leftInternalName);
    when(leftAttribute.getExternalName()).thenReturn(leftExternalName);
    when(rightAttribute.getInternalName()).thenReturn(rightInternalName);
    when(rightAttribute.getExternalName()).thenReturn(rightExternalName);
    when(leftAttribute.getEdmType()).thenReturn(EdmPrimitiveTypeKind.String);
    when(rightAttribute.getEdmType()).thenReturn(EdmPrimitiveTypeKind.String);

    return item;
  }
}
