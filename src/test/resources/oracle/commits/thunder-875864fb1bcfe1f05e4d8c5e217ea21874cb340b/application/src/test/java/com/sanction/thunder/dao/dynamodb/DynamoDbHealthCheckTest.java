package com.sanction.thunder.dao.dynamodb;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Page;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableCollection;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;

import com.codahale.metrics.health.HealthCheck;

import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamoDbHealthCheckTest extends HealthCheck {
  @SuppressWarnings("unchecked")
  private static final TableCollection<ListTablesResult> tables = mock(TableCollection.class);
  private static final DynamoDB dynamo = mock(DynamoDB.class);
  private static final DynamoDbHealthCheck healthCheck = new DynamoDbHealthCheck(dynamo);

  @BeforeClass
  public static void setup() {
    when(dynamo.listTables()).thenReturn(tables);
  }

  @Test(expected = NullPointerException.class)
  public void testNullDynamoObject() {
    new DynamoDbHealthCheck(null);
  }

  @Test
  public void testCheckHealthy() {
    when(tables.firstPage()).thenReturn(
        new Page<Table, ListTablesResult>(
            Collections.singletonList(mock(Table.class)),
            mock(ListTablesResult.class)) {
          @Override
          public boolean hasNextPage() {
            return false;
          }

          @Override
          public Page<Table, ListTablesResult> nextPage() {
            return null;
          }
        });

    assertTrue(healthCheck.check().isHealthy());
  }

  @Test
  public void testCheckUnhealthy() {
    when(tables.firstPage()).thenReturn(
        new Page<Table, ListTablesResult>(Collections.emptyList(), mock(ListTablesResult.class)) {
          @Override
          public boolean hasNextPage() {
            return false;
          }

          @Override
          public Page<Table, ListTablesResult> nextPage() {
            return null;
          }
        });

    assertFalse(healthCheck.check().isHealthy());
  }

  // Not used - exists in order to extend HealthCheck
  protected Result check() {
    return Result.healthy();
  }
}
