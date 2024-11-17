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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metamodel.schema.ColumnType;
import org.apache.metamodel.schema.MutableSchema;
import org.apache.metamodel.schema.Table;
import org.apache.metamodel.util.SimpleTableDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HBaseUpdateCallbackTest extends HBaseTestCase {

    private static final Logger logger = LoggerFactory.getLogger(HBaseClient.class);

    private HBaseUpdateCallback updateCallback;
    private MutableSchema schema;

    private static boolean warningGiven = false;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (isConfigured()) {
            updateCallback = new HBaseUpdateCallback(getDataContext());
            schema = (MutableSchema) getDataContext().getDefaultSchema();
            dropTableIfItExists();
        } else {
            if (!warningGiven) {
                System.err.println(getInvalidConfigurationMessage());
                warningGiven = true;
            }
        }
    }

    @Override
    public void tearDown() throws Exception {
        if (isConfigured()) {
            dropTableIfItExists();
        }
        super.tearDown();
    }

    /**
     * Drop the table if it exists. 
     * After that check in the schema and the datastore if the actions have been executed succesfully.
     */
    protected void dropTableIfItExists() {
        final Table table = schema.getTableByName(TABLE_NAME);
        if (table != null) {
            updateCallback.dropTable(table).execute();
            // Check schema
            assertNull(schema.getTableByName(TABLE_NAME));
            // Check in the datastore
            try (final Admin admin = getDataContext().getAdmin()) {
                assertFalse(admin.tableExists(TableName.valueOf(TABLE_NAME)));
            } catch (IOException e) {
                fail("Should not an exception checking if the table exists");
            }
        }
    }

    /**
     * Check if the table has been inserted succesfully.
     * Checks are performed in the schema and the datastore.
     * @throws IOException because the admin object needs to be created
     */
    protected void checkSuccesfullyInsertedTable() throws IOException {
        // Check the schema
        assertNotNull(schema.getTableByName(TABLE_NAME));
        // Check in the datastore
        try (final Admin admin = getDataContext().getAdmin()) {
            assertTrue(admin.tableExists(TableName.valueOf(TABLE_NAME)));
        } catch (IOException e) {
            fail("Should not an exception checking if the table exists");
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
        final LinkedHashSet<String> columnFamilies = new LinkedHashSet<>();
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
    protected HBaseTable createHBaseTable(final String tableName, final String idColumn, final String columnFamily1,
            final String columnFamily2, final String columnFamily3) {
        String[] columnNames;
        ColumnType[] columnTypes;

        if (idColumn == null && columnFamily3 == null) {
            columnNames = new String[] { columnFamily1, columnFamily2 };
            columnTypes = new ColumnType[] { ColumnType.STRING, ColumnType.STRING };
        } else if (idColumn != null && columnFamily3 == null) {
            columnNames = new String[] { idColumn, columnFamily1, columnFamily2 };
            columnTypes = new ColumnType[] { ColumnType.STRING, ColumnType.STRING, ColumnType.STRING };
        } else if (idColumn == null && columnFamily3 != null) {
            columnNames = new String[] { columnFamily1, columnFamily2, columnFamily3 };
            columnTypes = new ColumnType[] { ColumnType.STRING, ColumnType.STRING, ColumnType.STRING };
        } else {
            columnNames = new String[] { idColumn, columnFamily1, columnFamily2, columnFamily3 };
            columnTypes = new ColumnType[] { ColumnType.STRING, ColumnType.STRING, ColumnType.STRING,
                    ColumnType.STRING };
        }
        final SimpleTableDef tableDef = new SimpleTableDef(tableName, columnNames, columnTypes);
        return new HBaseTable(getDataContext(), tableDef, schema, ColumnType.STRING);
    }

    /**
     * Creates a map that represents a row
     * @param table HBaseTable
     * @param idColumn ID-column, can be set to null to create a row without this column
     * @param columnFamily1 required columnFamily 1
     * @param columnFamily2 required columnFamily 1
     * @return {@link LinkedHashMap}<{@link HBaseColumn}, {@link Object}>
     */
    protected static LinkedHashMap<HBaseColumn, Object> createRow(final HBaseTable table, final String idColumn,
            final String columnFamily1, final String columnFamily2) {
        final LinkedHashMap<HBaseColumn, Object> map = new LinkedHashMap<>();

        // Columns
        final ArrayList<HBaseColumn> columns = new ArrayList<>();
        if (idColumn != null) {
            columns.add(new HBaseColumn(idColumn, table));
        }
        columns.add(new HBaseColumn(columnFamily1, Q_HELLO, table));
        columns.add(new HBaseColumn(columnFamily1, Q_HI, table));
        columns.add(new HBaseColumn(columnFamily2, Q_HEY, table));
        columns.add(new HBaseColumn(columnFamily2, Q_BAH, table));

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
    protected static List<HBaseColumn> getHBaseColumnsFromRow(final LinkedHashMap<HBaseColumn, Object> row) {
        final List<HBaseColumn> columns = new ArrayList<>();
        columns.addAll(row.keySet());
        return columns;
    }

    /**
     * Set the values of a {@link HBaseRowInsertionBuilder} from the values in the mapped row
     * @param row {@link LinkedHashMap}<{@link HBaseColumn}, {@link Object}> containing the values
     * @param rowInsertionBuilder insertionBuilder to be set
     */
    protected void setValuesInInsertionBuilder(final LinkedHashMap<HBaseColumn, Object> row,
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
     * @throws IOException
     */
    protected void checkRows(final boolean rowsExist) throws IOException {
        try (org.apache.hadoop.hbase.client.Table table = getDataContext().getConnection().getTable(TableName.valueOf(
                TABLE_NAME))) {
            final Get get = new Get(Bytes.toBytes(RK_1));
            final Result result = table.get(get);
            if (rowsExist) {
                assertFalse(result.isEmpty());
                assertEquals(V_WORLD, new String(result.getValue(Bytes.toBytes(CF_FOO), Bytes.toBytes(Q_HELLO))));
                assertEquals(V_THERE, new String(result.getValue(Bytes.toBytes(CF_FOO), Bytes.toBytes(Q_HI))));
                assertEquals(V_YO, new String(result.getValue(Bytes.toBytes(CF_BAR), Bytes.toBytes(Q_HEY))));
                assertEquals(V_123_BYTE_ARRAY.toString(), new String(result.getValue(Bytes.toBytes(CF_BAR), Bytes
                        .toBytes(Q_BAH))));
            } else {
                assertTrue(result.isEmpty());
            }
        }
    }

    /**
     * Warn that the test(method) of a class is not executed, because the test-file hasn't been set.
     * See {@link HBaseTestCase#getPropertyFilePath} 
     * @param className 
     * @param methodName
     */
    protected void warnAboutANotExecutedTest(String className, String methodName) {
        String logWarning = "Test \"" + className + "#" + methodName
                + "()\" is not executed, because the HBasetest is not configured.";
        System.err.println(logWarning);
        logger.warn(logWarning);
    }

    protected HBaseUpdateCallback getUpdateCallback() {
        return updateCallback;
    }

    protected MutableSchema getSchema() {
        return schema;
    }
}
