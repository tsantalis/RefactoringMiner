package com.sap.olingo.jpa.processor.core.util;

import static org.junit.Assert.fail;

import java.util.List;

import org.apache.olingo.commons.api.edm.EdmAnnotation;
import org.apache.olingo.commons.api.edm.EdmElement;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmKeyPropertyRef;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmTerm;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.constants.EdmTypeKind;

import com.sap.olingo.jpa.metadata.core.edm.mapper.impl.JPAEdmNameBuilder;

public class EdmEntityTypeDouble implements EdmEntityType {

  private final String name;
  private final JPAEdmNameBuilder nameBuilder;

  public EdmEntityTypeDouble(final JPAEdmNameBuilder nameBuilder, final String name) {
    this.name = name;
    this.nameBuilder = nameBuilder;
  }

  @Override
  public EdmElement getProperty(final String name) {
    fail();
    return null;
  }

  @Override
  public List<String> getPropertyNames() {
    fail();
    return null;
  }

  @Override
  public EdmProperty getStructuralProperty(final String name) {
    fail();
    return null;
  }

  @Override
  public EdmNavigationProperty getNavigationProperty(final String name) {
    fail();
    return null;
  }

  @Override
  public List<String> getNavigationPropertyNames() {
    fail();
    return null;
  }

  @Override
  public boolean compatibleTo(final EdmType targetType) {
    fail();
    return false;
  }

  @Override
  public boolean isOpenType() {
    fail();
    return false;
  }

  @Override
  public boolean isAbstract() {
    fail();
    return false;
  }

  @Override
  public FullQualifiedName getFullQualifiedName() {
    return nameBuilder.buildFQN(name);
  }

  @Override
  public String getNamespace() {
    return nameBuilder.buildNamespace();
  }

  @Override
  public EdmTypeKind getKind() {
    fail();
    return null;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public EdmAnnotation getAnnotation(final EdmTerm term, final String qualifier) {
    fail();
    return null;
  }

  @Override
  public List<EdmAnnotation> getAnnotations() {
    fail();
    return null;
  }

  @Override
  public List<String> getKeyPredicateNames() {
    fail();
    return null;
  }

  @Override
  public List<EdmKeyPropertyRef> getKeyPropertyRefs() {
    fail();
    return null;
  }

  @Override
  public EdmKeyPropertyRef getKeyPropertyRef(final String keyPredicateName) {
    fail();
    return null;
  }

  @Override
  public boolean hasStream() {
    fail();
    return false;
  }

  @Override
  public EdmEntityType getBaseType() {
    fail();
    return null;
  }

}
