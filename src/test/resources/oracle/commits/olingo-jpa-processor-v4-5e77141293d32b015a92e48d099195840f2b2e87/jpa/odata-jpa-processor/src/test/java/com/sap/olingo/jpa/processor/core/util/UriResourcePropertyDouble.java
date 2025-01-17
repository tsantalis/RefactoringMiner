package com.sap.olingo.jpa.processor.core.util;

import static org.junit.jupiter.api.Assertions.fail;

import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceProperty;

public class UriResourcePropertyDouble implements UriResourceProperty {
  private final EdmProperty property;

  public UriResourcePropertyDouble(EdmProperty property) {
    super();
    this.property = property;
  }

  @Override
  public EdmType getType() {
    fail();
    return null;
  }

  @Override
  public boolean isCollection() {
    return false;
  }

  @Override
  public String getSegmentValue(boolean includeFilters) {
    fail();
    return null;
  }

  @Override
  public String toString(boolean includeFilters) {
    fail();
    return null;
  }

  @Override
  public UriResourceKind getKind() {
    fail();
    return null;
  }

  @Override
  public String getSegmentValue() {
    fail();
    return null;
  }

  @Override
  public EdmProperty getProperty() {

    return property;
  }

}
