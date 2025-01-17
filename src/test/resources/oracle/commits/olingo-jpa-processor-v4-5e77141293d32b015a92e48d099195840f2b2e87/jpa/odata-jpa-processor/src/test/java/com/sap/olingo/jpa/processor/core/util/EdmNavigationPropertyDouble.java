package com.sap.olingo.jpa.processor.core.util;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.apache.olingo.commons.api.edm.EdmAnnotation;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.EdmReferentialConstraint;
import org.apache.olingo.commons.api.edm.EdmTerm;

public class EdmNavigationPropertyDouble implements EdmNavigationProperty {
  private final String name;

  public EdmNavigationPropertyDouble(final String name) {
    super();
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isCollection() {
    fail();
    return false;
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
  public EdmEntityType getType() {
    fail();
    return null;
  }

  @Override
  public boolean isNullable() {
    fail();
    return false;
  }

  @Override
  public boolean containsTarget() {
    fail();
    return false;
  }

  @Override
  public EdmNavigationProperty getPartner() {
    fail();
    return null;
  }

  @Override
  public String getReferencingPropertyName(final String referencedPropertyName) {
    fail();
    return null;
  }

  @Override
  public List<EdmReferentialConstraint> getReferentialConstraints() {
    fail();
    return null;
  }

}
