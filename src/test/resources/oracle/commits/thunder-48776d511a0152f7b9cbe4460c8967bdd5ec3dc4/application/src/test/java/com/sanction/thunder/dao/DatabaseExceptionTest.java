package com.sanction.thunder.dao;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class DatabaseExceptionTest {

  @Test
  public void testDatabaseExceptionCreation() {
    DatabaseException exception = new DatabaseException(DatabaseError.USER_NOT_FOUND);
    Assertions.assertEquals(DatabaseError.USER_NOT_FOUND, exception.getErrorKind());

    exception = new DatabaseException("Error", DatabaseError.CONFLICT);
    Assertions.assertEquals(DatabaseError.CONFLICT, exception.getErrorKind());
    Assertions.assertEquals("Error", exception.getMessage());

    exception = new DatabaseException("Error", new Exception(), DatabaseError.DATABASE_DOWN);
    Assertions.assertEquals(DatabaseError.DATABASE_DOWN, exception.getErrorKind());
    Assertions.assertEquals("Error", exception.getMessage());

    exception = new DatabaseException(new Exception(), DatabaseError.REQUEST_REJECTED);
    Assertions.assertEquals(DatabaseError.REQUEST_REJECTED, exception.getErrorKind());
  }
}
