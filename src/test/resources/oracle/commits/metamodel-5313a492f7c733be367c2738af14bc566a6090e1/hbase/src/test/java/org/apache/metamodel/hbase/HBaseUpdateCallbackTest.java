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

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metamodel.schema.ColumnType;
import org.apache.metamodel.schema.MutableSchema;
import org.apache.metamodel.schema.Table;
import org.apache.metamodel.util.SimpleTableDef;
import org.junit.After;
import org.junit.Before;

public abstract class HBaseUpdateCallbackTest extends HBaseTestCase {

    private HBaseUpdateCallback updateCallback;
    private MutableSchema schema;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        updateCallback = new HBaseUpdateCallback(getDataContext());
        schema = (MutableSchema) getDataContext().getDefaultSchema();
        dropTableIfItExists();
    }

    @After
    public void tearDown() throws Exception {
        dropTableIfItExists();
    }

    /**
     * Drop the table if it exists. After that check in the schema and the datastore if the actions have been executed
     * successfully.
     *
     * @throws IOException
     */
    protected void dropTableIfItExists() throws IOException {
        if (schema != null) {
            final Table table = schema.getTableByName(TABLE_NAME);
            if (table != null) {
                updateCallback.dropTable(table).execute();
                // Check schema
                assertNull(schema.getTableByName(TABLE_NAME));
                // Check in the datastore
                try (final Admin admin = getDataContext().getAdmin()) {
                    assertFalse(admin.tableExists(TableName.valueOf(TABLE_NAME)));
                }
            }
        }
    }

    /**
     * Check if the table has been inserted successfully. Checks are performed in the schema and the datastore.
     * 
     * @throws IOException because the admin object needs to be created
     */
    protected void checkSuccesfullyInsertedTable() throws IOException {
        // Check the schema
        assertNotNull(schema.getTableByName(TABLE_NAME));
        // Check in the datastore
        try (final Admin admin = getDataContext().getAdmin()) {
            assertTrue(admin.tableExists(TableName.valueOf(TABLE_NAME)));
        }
    }

    /**
     * Create a test HBase table and add it to the datastore
     * @param tableName name of the table
     * @param idColumn required ID-column
     * @param columnFamily1 required columnFamily 1
     * @param columnFamily2 required columnFamily 2
     * @return created and add {@link HBaseTable}
     * @throws IOException
     */
    protected HBaseTable createAndAddTableToDatastore(final String tableName, final String idColumn,
            final String columnFamily1, final String columnFamily2) throws IOException {
        final Set<String> columnFamilies = new LinkedHashSet<>();
        columnFamilies.add(idColumn);
        columnFamilies.add(columnFamily1);
        columnFamilies.add(columnFamily2);
        updateCallback.createTable(schema, tableName, columnFamilies).execute();
        checkSuccesfullyInsertedTable();
        return (HBaseTable) getDataContext().getDefaultSchema().getTableByName(tableName);
    }

    /**
     * Create a test HBase table
     * @param tableName name of the table
     * @param idColumn ID-column, can be set to null to create a table without this column
     * @param columnFamily1 required columnFamily 1
     * @param columnFamily2 required columnFamily 2
     * @param columnFamily3 columnFamily 3 is not required and can be used to test errors
     * @return created {@link HBaseTable}
     */
    protected HBaseTable createHBaseTable(final String tableName, final String idColumn,
            final String... columnFamilies) {
        String[] columnNames;

        if (idColumn == null) {
            columnNames = columnFamilies;
        } else {
            columnNames = new String[columnFamilies.length + 1];

            columnNames[0] = idColumn;

            for (int i = 0; i < columnFamilies.length; i++) {
                columnNames[i + 1] = columnFamilies[i];
            }
        }

        ColumnType[] columnTypes = new ColumnType[columnNames.length];
        Arrays.fill(columnTypes, ColumnType.STRING);

        final SimpleTableDef tableDef = new SimpleTableDef(tableName, columnNames, columnTypes);
        return new HBaseTable(getDataContext(), tableDef, schema, ColumnType.STRING);
    }

    /**
     * Creates a map that represents a row
     * @param table HBaseTable
     * @param idColumn ID-column, can be set to null to create a row without this column
     * @param columnFamily1 required columnFamily 1
     * @param columnFamily2 required columnFamily 1
     * @param qualifiersNull true will create all {@link HBaseColumn}'s with qualifier null
     * @return {@link LinkedHashMap}<{@link HBaseColumn}, {@link Object}>
     */
    protected static Map<HBaseColumn, Object> createRow(final HBaseTable table, final String idColumn,
            final String columnFamily1, final String columnFamily2, final boolean qualifiersNull) {
        final Map<HBaseColumn, Object> map = new LinkedHashMap<>();

        // Columns
        final ArrayList<HBaseColumn> columns = new ArrayList<>();
        if (idColumn != null) {
            columns.add(new HBaseColumn(idColumn, table));
        }
        if (!qualifiersNull) {
            columns.add(new HBaseColumn(columnFamily1, Q_HELLO, table));
            columns.add(new HBaseColumn(columnFamily1, Q_HI, table));
            columns.add(new HBaseColumn(columnFamily2, Q_HEY, table));
            columns.add(new HBaseColumn(columnFamily2, Q_BAH, table));
        } else {
            columns.add(new HBaseColumn(columnFamily1, null, table));
            columns.add(new HBaseColumn(columnFamily1, null, table));
            columns.add(new HBaseColumn(columnFamily2, null, table));
            columns.add(new HBaseColumn(columnFamily2, null, table));
        }

        // Values
        final ArrayList<Object> values = new ArrayList<>();
        if (idColumn != null) {
            values.add(RK_1);
        }
        values.add(V_WORLD);
        values.add(V_THERE);
        values.add(V_YO);
        values.add(V_123_BYTE_ARRAY);

        // Fill the map
        for (int i = 0; i < columns.size(); i++) {
            map.put(columns.get(i), values.get(i));
        }

        return map;
    }

    /**
     * Get the HBaseColumns out of a mapped row
     * @param row {@link LinkedHashMap}<{@link HBaseColumn}, {@link Object}>
     * @return {@link List}<{@link HBaseColumn}>
     */
    protected static List<HBaseColumn> getHBaseColumnsFromRow(final Map<HBaseColumn, Object> row) {
        return row.keySet().stream().collect(Collectors.toList());
    }

    /**
     * Set the values of a {@link HBaseRowInsertionBuilder} from the values in the mapped row
     * @param row {@link LinkedHashMap}<{@link HBaseColumn}, {@link Object}> containing the values
     * @param rowInsertionBuilder insertionBuilder to be set
     * @param enoughMatchingValues if true, the amount of columns match the amount of values
     */
    protected void setValuesInInsertionBuilder(final Map<HBaseColumn, Object> row,
            final HBaseRowInsertionBuilder rowInsertionBuilder) {
        int i = 0;
        for (Object value : row.values()) {
            rowInsertionBuilder.value(i, value);
            i++;
        }
    }

    /**
     * Checks that the table does or doesn't have rows, depending on the rowsExists parameter
     * @param rowsExist true, check that the rows exists. false, check that the result is empty.
     * @param qualifierNull true, check the results when the qualifier was set to null
     * @throws IOException
     */
    protected void checkRows(final boolean rowsExist, final boolean qualifierNull) throws IOException {
        try (org.apache.hadoop.hbase.client.Table table = getDataContext().getConnection().getTable(TableName.valueOf(
                TABLE_NAME))) {
            final Get get = new Get(Bytes.toBytes(RK_1));
            final Result result = table.get(get);

            if (rowsExist) {
                assertFalse(result.isEmpty());
                if (!qualifierNull) {
                    assertEquals(V_WORLD, new String(result.getValue(Bytes.toBytes(CF_FOO), Bytes.toBytes(Q_HELLO))));
                    assertEquals(V_THERE, new String(result.getValue(Bytes.toBytes(CF_FOO), Bytes.toBytes(Q_HI))));
                    assertEquals(V_YO, new String(result.getValue(Bytes.toBytes(CF_BAR), Bytes.toBytes(Q_HEY))));
                    assertArrayEquals(V_123_BYTE_ARRAY, result.getValue(Bytes.toBytes(CF_BAR), Bytes.toBytes(Q_BAH)));
                } else {
                    assertEquals(V_THERE, new String(result.getValue(Bytes.toBytes(CF_FOO), null)));
                    assertArrayEquals(V_123_BYTE_ARRAY, result.getValue(Bytes.toBytes(CF_BAR), null));
                }
            } else {
                assertTrue(result.isEmpty());
            }
        }
    }

    protected HBaseUpdateCallback getUpdateCallback() {
        return updateCallback;
    }

    protected MutableSchema getSchema() {
        return schema;
    }
}
