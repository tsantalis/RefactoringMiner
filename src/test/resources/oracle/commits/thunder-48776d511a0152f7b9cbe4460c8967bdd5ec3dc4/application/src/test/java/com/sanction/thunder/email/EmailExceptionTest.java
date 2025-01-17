package com.sanction.thunder.email;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class EmailExceptionTest {

  @Test
  public void testEmailExceptionCreation() {
    EmailException exception = new EmailException();
    Assertions.assertNull(exception.getMessage());

    exception = new EmailException("Test message");
    Assertions.assertEquals("Test message", exception.getMessage());

    exception = new EmailException("Test message", new Exception());
    Assertions.assertEquals("Test message", exception.getMessage());

    exception = new EmailException(new Exception());
    Assertions.assertEquals(Exception.class, exception.getCause().getClass());
  }
}
