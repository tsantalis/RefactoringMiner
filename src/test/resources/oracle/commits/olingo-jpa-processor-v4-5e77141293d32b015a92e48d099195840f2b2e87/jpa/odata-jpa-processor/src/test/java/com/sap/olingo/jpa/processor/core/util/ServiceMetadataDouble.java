package com.sap.olingo.jpa.processor.core.util;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.EdmAnnotations;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmEntityContainer;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmFunction;
import org.apache.olingo.commons.api.edm.EdmSchema;
import org.apache.olingo.commons.api.edm.EdmTerm;
import org.apache.olingo.commons.api.edm.EdmTypeDefinition;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.constants.ODataServiceVersion;
import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.etag.ServiceMetadataETagSupport;

import com.sap.olingo.jpa.metadata.core.edm.mapper.impl.JPAEdmNameBuilder;

public class ServiceMetadataDouble implements ServiceMetadata {
  private final Edm edm;

  public ServiceMetadataDouble() {
    super();
    edm = new EdmDouble();
  }

  public ServiceMetadataDouble(JPAEdmNameBuilder nameBuilder, String typeName) {
    super();
    this.nameBuilder = nameBuilder;
    this.edm = new EdmDouble(typeName);
  }

  private JPAEdmNameBuilder nameBuilder;

  @Override
  public Edm getEdm() {
    return edm;
  }

  @Override
  public ODataServiceVersion getDataServiceVersion() {
    fail();
    return null;
  }

  @Override
  public List<EdmxReference> getReferences() {
    fail();
    return null;
  }

  @Override
  public ServiceMetadataETagSupport getServiceMetadataETagSupport() {
    fail();
    return null;
  }

  class EdmDouble implements Edm {
    private Map<FullQualifiedName, EdmEntityType> typeMap;

    public EdmDouble() {
      super();
      typeMap = new HashMap<>();
    }

    public EdmDouble(String name) {
      super();
      typeMap = new HashMap<>();
      EdmEntityType edmType = new EdmEntityTypeDouble(nameBuilder, name);
      typeMap.put(edmType.getFullQualifiedName(), edmType);
    }

    @Override
    public List<EdmSchema> getSchemas() {
      fail();
      return null;
    }

    @Override
    public EdmSchema getSchema(String namespace) {
      fail();
      return null;
    }

    @Override
    public EdmEntityContainer getEntityContainer() {
      fail();
      return null;
    }

    @Override
    public EdmEntityContainer getEntityContainer(FullQualifiedName name) {
      fail();
      return null;
    }

    @Override
    public EdmEnumType getEnumType(FullQualifiedName name) {
      fail();
      return null;
    }

    @Override
    public EdmTypeDefinition getTypeDefinition(FullQualifiedName name) {
      fail();
      return null;
    }

    @Override
    public EdmEntityType getEntityType(FullQualifiedName name) {
      return typeMap.get(name);
    }

    @Override
    public EdmComplexType getComplexType(FullQualifiedName name) {
      fail();
      return null;
    }

    @Override
    public EdmAction getUnboundAction(FullQualifiedName actionName) {
      fail();
      return null;
    }

    @Override
    public EdmAction getBoundAction(FullQualifiedName actionName, FullQualifiedName bindingParameterTypeName,
        Boolean isBindingParameterCollection) {
      fail();
      return null;
    }

    @Override
    public List<EdmFunction> getUnboundFunctions(FullQualifiedName functionName) {
      fail();
      return null;
    }

    @Override
    public EdmFunction getUnboundFunction(FullQualifiedName functionName, List<String> parameterNames) {
      fail();
      return null;
    }

    @Override
    public EdmFunction getBoundFunction(FullQualifiedName functionName, FullQualifiedName bindingParameterTypeName,
        Boolean isBindingParameterCollection, List<String> parameterNames) {
      fail();
      return null;
    }

    @Override
    public EdmTerm getTerm(FullQualifiedName termName) {
      fail();
      return null;
    }

    @Override
    public EdmAnnotations getAnnotationGroup(FullQualifiedName targetName, String qualifier) {
      fail();
      return null;
    }

    @Override
    public EdmAction getBoundActionWithBindingType(FullQualifiedName bindingParameterTypeName,
        Boolean isBindingParameterCollection) {
      return null;
    }

    @Override
    public List<EdmFunction> getBoundFunctionsWithBindingType(FullQualifiedName bindingParameterTypeName,
        Boolean isBindingParameterCollection) {
      // TODO Check what this is used for
      return null;
    }

  }

}
