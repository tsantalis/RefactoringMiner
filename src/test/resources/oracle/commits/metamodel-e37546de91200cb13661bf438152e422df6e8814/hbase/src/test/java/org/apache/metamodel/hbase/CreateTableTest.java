/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.metamodel.hbase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.metamodel.MetaModelException;
import org.apache.metamodel.schema.ImmutableSchema;
import org.apache.metamodel.schema.Table;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CreateTableTest extends HBaseUpdateCallbackTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * Check if creating table is supported
     */
    @Test
    public void testCreateTableSupported() {
        assertTrue(getUpdateCallback().isCreateTableSupported());
    }

    /**
     * Create a table with an immutableSchema, should throw a IllegalArgumentException
     */
    @Test
    public void testWrongSchema() {
        final ImmutableSchema immutableSchema = new ImmutableSchema(getSchema());

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Not a mutable schema: " + immutableSchema);

        getUpdateCallback().createTable(immutableSchema, TABLE_NAME).execute();
    }

    /**
     * Create a table without column families, should throw a MetaModelException
     */
    @Test
    public void testWithoutColumnFamilies() {
        exception.expect(MetaModelException.class);
        exception.expectMessage("Can't create a table without column families.");

        getUpdateCallback().createTable(getSchema(), TABLE_NAME).execute();
    }

    /**
     * Create a table without column families, should throw a MetaModelException
     */
    @Test
    public void testWithEmptyColumn() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Family name can not be empty");

        getUpdateCallback().createTable(getSchema(), TABLE_NAME).withColumn("").execute();
    }

    /**
     * Create a table without column families, should throw a MetaModelException
     */
    @Test
    public void testWithIndeterminableColumn() {
        exception.expect(MetaModelException.class);
        exception.expectMessage("Can't determine column family for column \"a:b:c\".");

        getUpdateCallback().createTable(getSchema(), TABLE_NAME).withColumn("a:b:c").execute();
    }

    /**
     * Goodflow. Create a table without the ID-Column, should work
     *
     * @throws IOException
     */
    @Test
    public void testCreateTableWithoutIDColumn() throws IOException {
        final HBaseCreateTableBuilder hBaseCreateTableBuilder = (HBaseCreateTableBuilder) getUpdateCallback()
                .createTable(getSchema(), TABLE_NAME);

        hBaseCreateTableBuilder.withColumn(CF_FOO);
        hBaseCreateTableBuilder.withColumn(CF_BAR);
        hBaseCreateTableBuilder.execute();
        checkSuccesfullyInsertedTable();

        final Table table = getDataContext().getDefaultSchema().getTableByName(TABLE_NAME);
        assertTrue(table instanceof HBaseTable);

        // Assert that the Table has 3 column families, a default "_id" one, and two based on the column families for
        // the columns.
        assertEquals(3, ((HBaseTable) table).getColumnFamilies().size());
    }

    /**
     * Goodflow. Create a table including the ID-Column (columnFamilies not in constructor), should work
     *
     * @throws IOException
     */
    @Test
    public void testCreateTableWithQualifiedColumns() throws IOException {
        final HBaseCreateTableBuilder hBaseCreateTableBuilder = (HBaseCreateTableBuilder) getUpdateCallback()
                .createTable(getSchema(), TABLE_NAME);

        hBaseCreateTableBuilder.withColumn(CF_FOO + ":" + Q_BAH);
        hBaseCreateTableBuilder.withColumn(CF_FOO + ":" + Q_HELLO);
        hBaseCreateTableBuilder.withColumn(CF_BAR + ":" + Q_HEY);
        hBaseCreateTableBuilder.withColumn(CF_BAR + ":" + Q_HI);
        hBaseCreateTableBuilder.execute();
        checkSuccesfullyInsertedTable();

        final Table table = getDataContext().getDefaultSchema().getTableByName(TABLE_NAME);
        assertTrue(table instanceof HBaseTable);

        // Assert that the Table has 3 column families, a default "_id" one, and two based on the column families for
        // the columns.
        assertEquals(3, ((HBaseTable) table).getColumnFamilies().size());
    }

    /**
     * Goodflow. Create a table including the ID-Column (columnFamilies in constructor), should work
     *
     * @throws IOException
     */
    @Test
    public void testCreateTableColumnFamiliesInConstrutor() throws IOException {
        final HBaseCreateTableBuilder hBaseCreateTableBuilder = (HBaseCreateTableBuilder) getUpdateCallback()
                .createTable(getSchema(), TABLE_NAME);

        hBaseCreateTableBuilder.withColumn(HBaseDataContext.FIELD_ID);
        hBaseCreateTableBuilder.withColumn(CF_BAR);
        hBaseCreateTableBuilder.execute();
        checkSuccesfullyInsertedTable();

        final Table table = getDataContext().getDefaultSchema().getTableByName(TABLE_NAME);
        assertTrue(table instanceof HBaseTable);

        // Assert that the Table has 2 column families.
        assertEquals(2, ((HBaseTable) table).getColumnFamilies().size());
    }
}