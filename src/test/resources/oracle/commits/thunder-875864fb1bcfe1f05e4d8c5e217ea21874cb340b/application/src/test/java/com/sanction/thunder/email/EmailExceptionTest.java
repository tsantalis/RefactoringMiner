package com.sanction.thunder.email;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EmailExceptionTest {

  @Test
  public void testEmailExceptionCreation() {
    EmailException exception = new EmailException();
    assertNull(exception.getMessage());

    exception = new EmailException("Test message");
    assertEquals("Test message", exception.getMessage());

    exception = new EmailException("Test message", new Exception());
    assertEquals("Test message", exception.getMessage());

    exception = new EmailException(new Exception());
    assertEquals(Exception.class, exception.getCause().getClass());
  }
}
