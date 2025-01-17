package com.sap.olingo.jpa.metadata.core.edm.mapper.testobjects;

import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmFunction;
import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmFunction.ReturnType;
import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmMediaStream;
import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmParameter;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.ODataFunction;

public class ExampleJavaFunctionsReturnsStream implements ODataFunction {

  @EdmFunction(hasFunctionImport = true, returnType = @ReturnType(stream = @EdmMediaStream(
      contentType = "audio/x-wav")), name = "")
  public byte[] simpleStream(@EdmParameter(name = "A") short a, @EdmParameter(name = "B") int b) {
    return new byte[0];
  }
}
