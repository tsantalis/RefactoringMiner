package com.sap.olingo.jpa.processor.core.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.olingo.commons.api.ex.ODataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import com.sap.olingo.jpa.processor.core.modify.JPAConversionHelper;
import com.sap.olingo.jpa.processor.core.testmodel.BusinessPartnerRole;
import com.sap.olingo.jpa.processor.core.testmodel.CommunicationData;
import com.sap.olingo.jpa.processor.core.testmodel.Person;

public class TestCreateDeltaBasedResult extends TestJPAModifyProcessor {
  private JPACUDRequestProcessor cut;
  private List<JPAElement> pathElements;
  private Person beforeImagePerson;
  private Person currentImagePerson;
  private JPAAssociationPath path;

  @BeforeEach
  public void setup() throws ODataException {
    cut = new JPACUDRequestProcessor(odata, serviceMetadata, sessionContext, requestContext, new JPAConversionHelper());
    pathElements = new ArrayList<>(3);
    path = mock(JPAAssociationPath.class);

    beforeImagePerson = new Person();
    beforeImagePerson.setID("1");
    currentImagePerson = new Person();
    currentImagePerson.setID("1");
    when(em.contains(beforeImagePerson)).thenReturn(Boolean.FALSE);
  }

  @Test
  public void testShallReturnNullIfBeforeImageNotPresent() throws ODataJPAProcessorException {

    final JPAAssociationPath path = mock(JPAAssociationPath.class);
    final JPAElement pathItem = mock(JPAElement.class);
    pathElements.add(pathItem);
    when(pathItem.getInternalName()).thenReturn("roles");
    final Object act = cut.getLinkedInstanceBasedResultByDelta(beforeImagePerson, path, Optional.empty());
    assertNull(act);
  }

  @Test
  public void testThrowsExceptionIfBeforeIfManaged() throws ODataJPAProcessorException {
    when(em.contains(beforeImagePerson)).thenReturn(Boolean.TRUE);
    assertThrows(ODataJPAProcessorException.class, () -> {
      cut.getLinkedInstanceBasedResultByDelta(currentImagePerson, path, Optional.ofNullable(beforeImagePerson));
    });
  }

  @Test
  public void testShallReturnNullIfTargetEmpty() throws ODataJPAProcessorException {

    prepareRole();
    final Object act = cut.getLinkedInstanceBasedResultByDelta(currentImagePerson, path, Optional.ofNullable(
        beforeImagePerson));
    assertNull(act);
  }

  @Test
  public void testShallReturnNullIfNoDeltaFound() throws ODataJPAProcessorException {

    prepareRole();
    final BusinessPartnerRole beforeRole = new BusinessPartnerRole();
    beforeRole.setBusinessPartner(beforeImagePerson);
    beforeRole.setRoleCategory("A");
    beforeImagePerson.getRoles().add(beforeRole);

    final Object act = cut.getLinkedInstanceBasedResultByDelta(beforeImagePerson, path, Optional.ofNullable(
        beforeImagePerson));
    assertNull(act);
  }

  @Test
  public void testShallReturnsValueIfDeltaFoundBeforeEmpty() throws ODataJPAProcessorException {

    prepareRole();
    final BusinessPartnerRole exp = new BusinessPartnerRole();
    exp.setBusinessPartner(currentImagePerson);
    exp.setRoleCategory("A");
    currentImagePerson.getRoles().add(exp);

    final Object act = cut.getLinkedInstanceBasedResultByDelta(currentImagePerson, path, Optional.ofNullable(
        beforeImagePerson));
    assertEquals(exp, act);
  }

  @Test
  public void testShallReturnsValueIfDeltaFoundBeforeOneNowTwo() throws ODataJPAProcessorException {

    prepareRole();
    final BusinessPartnerRole exp = new BusinessPartnerRole(currentImagePerson, "A");
    currentImagePerson.getRoles().add(exp);

    currentImagePerson.getRoles().add(new BusinessPartnerRole(currentImagePerson, "B"));
    beforeImagePerson.getRoles().add(new BusinessPartnerRole(beforeImagePerson, "B"));

    final Object act = cut.getLinkedInstanceBasedResultByDelta(currentImagePerson, path, Optional.ofNullable(
        beforeImagePerson));
    assertEquals(exp, act);
  }

  @Test
  public void testShallReturnsValueIfDeltaFoundBeforeOneNowTwoInversOrder() throws ODataJPAProcessorException {

    prepareRole();
    currentImagePerson.getRoles().add(new BusinessPartnerRole(currentImagePerson, "B"));
    beforeImagePerson.getRoles().add(new BusinessPartnerRole(beforeImagePerson, "B"));
    final BusinessPartnerRole exp = new BusinessPartnerRole(currentImagePerson, "A");
    currentImagePerson.getRoles().add(exp);

    final Object act = cut.getLinkedInstanceBasedResultByDelta(currentImagePerson, path, Optional.ofNullable(
        beforeImagePerson));
    assertEquals(exp, act);
  }

  @Test
  public void testShallReturnNewValueIfNotACollection() throws ODataJPAProcessorException {
    final CommunicationData exp = prepareBeforeImageCommunicationData();
    beforeImagePerson.setCommunicationData(exp);
    final Object act = cut.getLinkedInstanceBasedResultByDelta(currentImagePerson, path, Optional.ofNullable(
        beforeImagePerson));
    assertEquals(exp, act);
  }

  private void prepareRole() {

    final JPAAssociationAttribute pathItem = mock(JPAAssociationAttribute.class);
    pathElements.add(pathItem);
    when(path.getPath()).thenReturn(pathElements);
    when(pathItem.getInternalName()).thenReturn("roles");
    when(path.getLeaf()).thenReturn(pathItem);
    when(pathItem.isCollection()).thenReturn(Boolean.TRUE);
  }

  private CommunicationData prepareBeforeImageCommunicationData() {
    final CommunicationData afterCommData = new CommunicationData();
    currentImagePerson.setCommunicationData(afterCommData);
    final JPAAssociationAttribute pathItem = mock(JPAAssociationAttribute.class);
    pathElements.add(pathItem);
    when(path.getPath()).thenReturn(pathElements);
    when(pathItem.getInternalName()).thenReturn("communicationData");
    when(path.getLeaf()).thenReturn(pathItem);
    when(pathItem.isCollection()).thenReturn(Boolean.FALSE);
    return afterCommData;
  }
}
