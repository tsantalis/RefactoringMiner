package com.sap.olingo.jpa.processor.core.util;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmStructuredType;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriHelper;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;

public class UriHelperDouble implements UriHelper {
  private Map<String, String> keyPredicates;
  private String idPropertyName;

  @Override
  public String buildContextURLSelectList(EdmStructuredType type, ExpandOption expand, SelectOption select)
      throws SerializerException {
    fail();
    return null;
  }

  @Override
  public String buildContextURLKeyPredicate(List<UriParameter> keys) throws SerializerException {
    fail();
    return null;
  }

  @Override
  public String buildCanonicalURL(EdmEntitySet edmEntitySet, Entity entity) throws SerializerException {
    fail();
    return null;
  }

  @Override
  public String buildKeyPredicate(EdmEntityType edmEntityType, Entity entity) throws SerializerException {

    return keyPredicates.get(entity.getProperty(idPropertyName).getValue());
  }

  @Override
  public UriResourceEntitySet parseEntityId(Edm edm, String entityId, String rawServiceRoot)
      throws DeserializerException {
    fail();
    return null;
  }

  public Map<String, String> getKeyPredicates() {
    return keyPredicates;
  }

  public void setKeyPredicates(Map<String, String> keyPredicates, String idPropertyName) {
    this.keyPredicates = keyPredicates;
    this.idPropertyName = idPropertyName;
  }

}
