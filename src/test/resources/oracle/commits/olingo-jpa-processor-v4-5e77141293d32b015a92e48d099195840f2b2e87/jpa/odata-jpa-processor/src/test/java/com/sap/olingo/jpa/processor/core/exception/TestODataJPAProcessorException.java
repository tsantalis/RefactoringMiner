package com.sap.olingo.jpa.processor.core.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.junit.jupiter.api.Test;

import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAMessageKey;

public class TestODataJPAProcessorException {
  // private static String BUNDLE_NAME = "exceptions-i18n";

  public static enum MessageKeys implements ODataJPAMessageKey {
    RESULT_NOT_FOUND;

    @Override
    public String getKey() {
      return name();
    }

  }

  @Test
  public void checkSimpleRaiseExeption() {
    try {
      RaiseExeption();
    } catch (ODataApplicationException e) {
      assertEquals("No result was fond by Serializer", e.getMessage());
      assertEquals(400, e.getStatusCode());
      return;
    }
    fail();
  }

  @Test
  public void checkSimpleViaMessageKeyRaiseExeption() {
    try {
      RaiseExeptionParam();
    } catch (ODataApplicationException e) {
      assertEquals("Unable to convert value 'Willi' of parameter 'Hugo'", e.getMessage());
      assertEquals(500, e.getStatusCode());
      return;
    }
    fail();
  }

  private void RaiseExeptionParam() throws ODataJPAProcessException {
    throw new ODataJPADBAdaptorException(ODataJPADBAdaptorException.MessageKeys.PARAMETER_CONVERSION_ERROR,
        HttpStatusCode.INTERNAL_SERVER_ERROR, "Willi", "Hugo");
  }

  private void RaiseExeption() throws ODataJPAProcessException {
    throw new ODataJPASerializerException(ODataJPASerializerException.MessageKeys.RESULT_NOT_FOUND,
        HttpStatusCode.BAD_REQUEST);
  }

//  private class TestException extends ODataJPAProcessException {
//    private static final long serialVersionUID = 1L;
//
//    public TestException(Throwable e, final HttpStatusCode statusCode) {
//      super(e, statusCode);
//    }
//
//    public TestException(final MessageKeys messageKey, final HttpStatusCode statusCode,
//        final Throwable cause, final String... params) {
//      super(messageKey.getKey(), statusCode, cause, params);
//    }
//
//    public TestException(final String id, final HttpStatusCode statusCode) {
//      super(id, statusCode);
//    }
//
//    public TestException(final MessageKeys messageKey, final HttpStatusCode statusCode,
//        final String... params) {
//      super(messageKey.getKey(), statusCode, params);
//    }
//
//    public TestException(final MessageKeys messageKey, final HttpStatusCode statusCode, final Throwable e) {
//      super(messageKey.getKey(), statusCode, e);
//    }
//
//    @Override
//    protected String getBundleName() {
//      return BUNDLE_NAME;
//    }
//  }
}
