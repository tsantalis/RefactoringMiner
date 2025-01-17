package com.sap.olingo.jpa.processor.core.util;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.apache.olingo.commons.api.edm.EdmAnnotation;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmEntityContainer;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmMapping;
import org.apache.olingo.commons.api.edm.EdmNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.EdmTerm;

import com.sap.olingo.jpa.metadata.core.edm.mapper.impl.JPAEdmNameBuilder;

public class EdmEntitySetDouble implements EdmEntitySet {
  private final String name;
  private final EdmEntityType type;
  // private final JPAEdmNameBuilder nameBuilder;

  public EdmEntitySetDouble(final JPAEdmNameBuilder nameBuilder, final String name) {
    super();
    this.name = name;
    this.type = new EdmEntityTypeDouble(nameBuilder, name.substring(0, name.length() - 1));
    // this.nameBuilder = nameBuilder;
  }

  @Override
  public String getTitle() {
    return null;
  }

  @Override
  public EdmBindingTarget getRelatedBindingTarget(final String path) {
    return null;
  }

  @Override
  public List<EdmNavigationPropertyBinding> getNavigationPropertyBindings() {
    return null;
  }

  @Override
  public EdmEntityContainer getEntityContainer() {
    return null;
  }

  @Override
  public EdmEntityType getEntityType() {
    return type;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public EdmAnnotation getAnnotation(final EdmTerm term, final String qualifier) {
    return null;
  }

  @Override
  public List<EdmAnnotation> getAnnotations() {
    return null;
  }

  @Override
  public boolean isIncludeInServiceDocument() {
    return false;
  }

  @Override
  public EdmMapping getMapping() {
    fail();
    return null;
  }
}
