package com.sanction.thunder.dao.dynamodb;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.document.Expected;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.sanction.thunder.dao.DatabaseError;
import com.sanction.thunder.dao.DatabaseException;
import com.sanction.thunder.dao.UsersDao;
import com.sanction.thunder.models.Email;
import com.sanction.thunder.models.User;

import io.dropwizard.jackson.Jackson;

import java.util.Collections;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DynamoDbUsersDaoTest {
  private final Table table = mock(Table.class);
  private final Item item = mock(Item.class);
  private final ObjectMapper mapper = Jackson.newObjectMapper();

  private final Email email = new Email("email", true, "hashToken");

  private final User user = new User(
      email, "password",
      Collections.singletonMap("facebookAccessToken", "fb"));

  private final UsersDao usersDao = new DynamoDbUsersDao(table, mapper);

  @BeforeEach
  public void setup() {
    when(item.getJSON(anyString())).thenReturn(UsersDao.toJson(mapper, user));
    when(item.getString(anyString())).thenReturn("example");

    when(item.withString(anyString(), anyString())).thenReturn(item);
    when(item.withLong(anyString(), anyLong())).thenReturn(item);
    when(item.withJSON(anyString(), anyString())).thenReturn(item);
  }

  @Test
  public void testSuccessfulInsert() {
    User result = usersDao.insert(user);

    verify(table, times(1)).putItem(any(Item.class), any(Expected.class));

    Assertions.assertEquals(user, result);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testConflictingInsert() {
    when(table.putItem(any(), any())).thenThrow(ConditionalCheckFailedException.class);

    try {
      usersDao.insert(user);
    } catch (DatabaseException e) {
      Assertions.assertEquals(DatabaseError.CONFLICT, e.getErrorKind());
      verify(table, times(1)).putItem(any(Item.class), any(Expected.class));

      return;
    }

    Assertions.fail("Database exception not thrown.");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInsertWithUnsupportedData() {
    when(table.putItem(any(), any())).thenThrow(AmazonServiceException.class);

    try {
      usersDao.insert(user);
    } catch (DatabaseException e) {
      Assertions.assertEquals(DatabaseError.REQUEST_REJECTED, e.getErrorKind());
      verify(table, times(1)).putItem(any(Item.class), any(Expected.class));

      return;
    }

    Assertions.fail("Database exception not thrown.");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInsertWithDatabaseDown() {
    when(table.putItem(any(), any())).thenThrow(AmazonClientException.class);

    try {
      usersDao.insert(user);
    } catch (DatabaseException e) {
      Assertions.assertEquals(DatabaseError.DATABASE_DOWN, e.getErrorKind());
      verify(table, times(1)).putItem(any(Item.class), any(Expected.class));

      return;
    }

    Assertions.fail("Database exception not thrown.");
  }

  @Test
  public void testSuccessfulFindByEmail() {
    when(table.getItem(anyString(), anyString())).thenReturn(item);

    User result = usersDao.findByEmail("email");

    verify(table, times(1)).getItem(anyString(), anyString());
    Assertions.assertEquals(user, result);
  }

  @Test
  public void testUnsuccessfulFindByEmail() {
    when(table.getItem(anyString(), anyString())).thenReturn(null);

    try {
      usersDao.findByEmail("email");
    } catch (DatabaseException e) {
      Assertions.assertEquals(DatabaseError.USER_NOT_FOUND, e.getErrorKind());
      verify(table, times(1)).getItem(anyString(), anyString());

      return;
    }

    Assertions.fail("Database exception not thrown.");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testFindByEmailDatabaseDown() {
    when(table.getItem(anyString(), anyString())).thenThrow(AmazonClientException.class);

    try {
      usersDao.findByEmail("email");
    } catch (DatabaseException e) {
      Assertions.assertEquals(DatabaseError.DATABASE_DOWN, e.getErrorKind());
      verify(table, times(1)).getItem(anyString(), anyString());

      return;
    }

    Assertions.fail("Database exception not thrown.");
  }

  @Test
  public void testSuccessfulUpdate() {
    when(table.getItem(anyString(), anyString())).thenReturn(item);

    User result = usersDao.update(null, user);

    verify(table, times(1)).getItem(anyString(), anyString());
    verify(table, times(1)).putItem(any(Item.class), any(Expected.class));
    Assertions.assertEquals(user, result);
  }

  @Test
  public void testSuccessfulEmailUpdate() {
    when(table.getItem(anyString(), anyString())).thenReturn(item);

    User result = usersDao.update("existingEmail", user);

    Assertions.assertEquals(user, result);

    verify(table, times(1)).getItem(anyString(), anyString());
    verify(table, times(1)).deleteItem(any(DeleteItemSpec.class));
    verify(table, times(1)).putItem(any(Item.class), any(Expected.class));
  }

  @Test
  public void testUpdateGetNotFound() {
    when(table.getItem(anyString(), anyString())).thenReturn(null);

    try {
      usersDao.update(null, user);
    } catch (DatabaseException e) {
      Assertions.assertEquals(DatabaseError.USER_NOT_FOUND, e.getErrorKind());
      verify(table, times(1)).getItem(anyString(), anyString());

      return;
    }

    Assertions.fail("Database exception not thrown.");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testUpdateGetDatabaseDown() {
    when(table.getItem(anyString(), anyString())).thenThrow(AmazonClientException.class);

    try {
      usersDao.update(null, user);
    } catch (DatabaseException e) {
      Assertions.assertEquals(DatabaseError.DATABASE_DOWN, e.getErrorKind());
      verify(table, times(1)).getItem(anyString(), anyString());

      return;
    }

    Assertions.fail("Database exception not thrown.");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testUpdatePutConflict() {
    when(table.getItem(anyString(), anyString())).thenReturn(item);
    when(table.putItem(any(), any())).thenThrow(ConditionalCheckFailedException.class);

    try {
      usersDao.update(null, user);
    } catch (DatabaseException e) {
      Assertions.assertEquals(DatabaseError.CONFLICT, e.getErrorKind());
      verify(table, times(1)).getItem(anyString(), anyString());
      verify(table, times(1)).putItem(any(Item.class), any(Expected.class));

      return;
    }

    Assertions.fail("Database exception not thrown.");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testUpdatePutUnsupportedData() {
    when(table.getItem(anyString(), anyString())).thenReturn(item);
    when(table.putItem(any(), any())).thenThrow(AmazonServiceException.class);

    try {
      usersDao.update(null, user);
    } catch (DatabaseException e) {
      Assertions.assertEquals(DatabaseError.REQUEST_REJECTED, e.getErrorKind());
      verify(table, times(1)).getItem(anyString(), anyString());
      verify(table, times(1)).putItem(any(Item.class), any(Expected.class));

      return;
    }

    Assertions.fail("Database exception not thrown.");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testUpdatePutDatabaseDown() {
    when(table.getItem(anyString(), anyString())).thenReturn(item);
    when(table.putItem(any(), any())).thenThrow(AmazonClientException.class);

    try {
      usersDao.update(null, user);
    } catch (DatabaseException e) {
      Assertions.assertEquals(DatabaseError.DATABASE_DOWN, e.getErrorKind());
      verify(table, times(1)).getItem(anyString(), anyString());
      verify(table, times(1)).putItem(any(Item.class), any(Expected.class));

      return;
    }

    Assertions.fail("Database exception not thrown.");
  }

  @Test
  public void testSuccessfulDelete() {
    when(table.getItem(anyString(), anyString())).thenReturn(item);

    User result = usersDao.delete("email");

    verify(table, times(1)).getItem(anyString(), anyString());
    verify(table, times(1)).deleteItem(any(DeleteItemSpec.class));
    Assertions.assertEquals(user, result);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testUnsuccessfulDeleteGetFailure() {
    when(table.getItem(anyString(), anyString())).thenThrow(AmazonClientException.class);

    try {
      usersDao.delete("email");
    } catch (DatabaseException e) {
      Assertions.assertEquals(DatabaseError.DATABASE_DOWN, e.getErrorKind());
      verify(table, times(1)).getItem(anyString(), anyString());

      return;
    }

    Assertions.fail("Database exception not thrown.");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testUnsuccessfulDeleteNotFound() {
    when(table.getItem(anyString(), anyString())).thenReturn(item);
    when(table.deleteItem(any(DeleteItemSpec.class)))
        .thenThrow(ConditionalCheckFailedException.class);

    try {
      usersDao.delete("email");
    } catch (DatabaseException e) {
      Assertions.assertEquals(DatabaseError.USER_NOT_FOUND, e.getErrorKind());
      verify(table, times(1)).getItem(anyString(), anyString());
      verify(table, times(1)).deleteItem(any(DeleteItemSpec.class));

      return;
    }

    Assertions.fail("Database exception not thrown.");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testUnsuccessfulDeleteDatabaseDown() {
    when(table.getItem(anyString(), anyString())).thenReturn(item);
    when(table.deleteItem(any(DeleteItemSpec.class)))
        .thenThrow(AmazonClientException.class);

    try {
      usersDao.delete("email");
    } catch (DatabaseException e) {
      Assertions.assertEquals(DatabaseError.DATABASE_DOWN, e.getErrorKind());
      verify(table, times(1)).getItem(anyString(), anyString());
      verify(table, times(1)).deleteItem(any(DeleteItemSpec.class));

      return;
    }

    Assertions.fail("Database exception not thrown.");
  }
}
