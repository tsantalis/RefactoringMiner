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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.metamodel.MetaModelException;
import org.apache.metamodel.schema.ImmutableSchema;

public class CreateTableTest extends HBaseUpdateCallbackTest {

    /**
     * Check if creating table is supported
     */
    public void testDropTableSupported() {
        if (isConfigured()) {
            assertTrue(getUpdateCallback().isCreateTableSupported());
        } else {
            warnAboutANotExecutedTest(getClass().getName(), new Object() {
            }.getClass().getEnclosingMethod().getName());
        }
    }

    /**
     * Create a table with an immutableSchema, should throw a IllegalArgumentException
     */
    public void testWrongSchema() {
        if (isConfigured()) {
            final ImmutableSchema immutableSchema = new ImmutableSchema(getSchema());
            try {
                getUpdateCallback().createTable(immutableSchema, TABLE_NAME).execute();
                fail("Should get an exception that the schema isn't mutable");
            } catch (IllegalArgumentException e) {
                assertEquals("Not a mutable schema: " + immutableSchema, e.getMessage());
            }
        } else {
            warnAboutANotExecutedTest(getClass().getName(), new Object() {
            }.getClass().getEnclosingMethod().getName());
        }
    }

    /**
     * Create a table without columnFamilies, should throw a MetaModelException
     */
    public void testCreateTableWithoutColumnFamilies() {
        if (isConfigured()) {
            try {
                getUpdateCallback().createTable(getSchema(), TABLE_NAME).execute();
                fail("Should get an exception that the columnFamilies haven't been set");
            } catch (MetaModelException e) {
                assertEquals("Creating a table without columnFamilies", e.getMessage());
            }
        } else {
            warnAboutANotExecutedTest(getClass().getName(), new Object() {
            }.getClass().getEnclosingMethod().getName());
        }
    }

    /**
     * Create a table with columnFamilies null, should throw a MetaModelException
     */
    public void testColumnFamiliesNull() {
        if (isConfigured()) {
            try {
                getUpdateCallback().createTable(getSchema(), TABLE_NAME, null).execute();
                fail("Should get an exception that the columnFamilies haven't been set");
            } catch (MetaModelException e) {
                assertEquals("Creating a table without columnFamilies", e.getMessage());
            }
        } else {
            warnAboutANotExecutedTest(getClass().getName(), new Object() {
            }.getClass().getEnclosingMethod().getName());
        }
    }

    /**
     * Create a table with columnFamilies empty, should throw a MetaModelException
     */
    public void testColumnFamiliesEmpty() {
        if (isConfigured()) {
            try {
                final LinkedHashSet<String> columnFamilies = new LinkedHashSet<String>();
                getUpdateCallback().createTable(getSchema(), TABLE_NAME, columnFamilies).execute();
                fail("Should get an exception that the columnFamilies haven't been set");
            } catch (MetaModelException e) {
                assertEquals("Creating a table without columnFamilies", e.getMessage());
            }
        } else {
            warnAboutANotExecutedTest(getClass().getName(), new Object() {
            }.getClass().getEnclosingMethod().getName());
        }
    }

    /**
     * Create a table without the ID-Column, should throw a MetaModelException
     */
    public void testCreateTableWithoutIDColumn() {
        if (isConfigured()) {
            final HBaseTable table = createHBaseTable(TABLE_NAME, HBaseDataContext.FIELD_ID, CF_FOO, CF_BAR, null);
            final LinkedHashMap<HBaseColumn, Object> row = createRow(table, null, CF_FOO, CF_BAR);
            final Set<String> columnFamilies = getColumnFamilies(getHBaseColumnsFromRow(row));
            try {
                final HBaseCreateTableBuilder hBaseCreateTableBuilder = (HBaseCreateTableBuilder) getUpdateCallback()
                        .createTable(getSchema(), TABLE_NAME);

                hBaseCreateTableBuilder.setColumnFamilies(columnFamilies);
                hBaseCreateTableBuilder.execute();
                fail("Should get an exception that the ID-colum is missing");
            } catch (MetaModelException e) {
                assertEquals("ColumnFamily: " + HBaseDataContext.FIELD_ID + " not found", e.getMessage());
            }
        } else {
            warnAboutANotExecutedTest(getClass().getName(), new Object() {
            }.getClass().getEnclosingMethod().getName());
        }
    }

    /**
     * Creating a HBaseClient with the tableName null, should throw a exception
     */
    public void testCreatingTheHBaseClientWithTableNameNull() {
        if (isConfigured()) {
            try {
                final LinkedHashSet<String> columnFamilies = new LinkedHashSet<>();
                columnFamilies.add("1");
                new HBaseClient(getDataContext().getConnection()).createTable(null, columnFamilies);
                fail("Should get an exception that tableName is null");
            } catch (IllegalArgumentException e) {
                assertEquals("Can't create a table without having the tableName or columnFamilies", e.getMessage());
            }
        } else {
            warnAboutANotExecutedTest(getClass().getName(), new Object() {
            }.getClass().getEnclosingMethod().getName());
        }
    }

    /**
     * Creating a HBaseClient with the tableName null, should throw a exception
     */
    public void testCreatingTheHBaseClientWithColumnFamiliesNull() {
        if (isConfigured()) {
            try {
                new HBaseClient(getDataContext().getConnection()).createTable("1", null);
                fail("Should get an exception that columnFamilies is null");
            } catch (IllegalArgumentException e) {
                assertEquals("Can't create a table without having the tableName or columnFamilies", e.getMessage());
            }
        } else {
            warnAboutANotExecutedTest(getClass().getName(), new Object() {
            }.getClass().getEnclosingMethod().getName());
        }
    }

    /**
     * Creating a HBaseClient with the tableName null, should throw a exception
     */
    public void testCreatingTheHBaseClientWithColumnFamiliesEmpty() {
        if (isConfigured()) {
            try {
                final LinkedHashSet<String> columnFamilies = new LinkedHashSet<>();
                new HBaseClient(getDataContext().getConnection()).createTable("1", columnFamilies);
                fail("Should get an exception that columnFamilies is empty");
            } catch (IllegalArgumentException e) {
                assertEquals("Can't create a table without having the tableName or columnFamilies", e.getMessage());
            }
        } else {
            warnAboutANotExecutedTest(getClass().getName(), new Object() {
            }.getClass().getEnclosingMethod().getName());
        }
    }

    /**
     * Goodflow. Create a table including the ID-Column (columnFamilies not in constructor), should work
     */
    public void testSettingColumnFamiliesAfterConstrutor() {
        if (isConfigured()) {
            final HBaseTable table = createHBaseTable(TABLE_NAME, HBaseDataContext.FIELD_ID, CF_FOO, CF_BAR, null);
            final LinkedHashMap<HBaseColumn, Object> row = createRow(table, HBaseDataContext.FIELD_ID, CF_FOO, CF_BAR);
            final Set<String> columnFamilies = getColumnFamilies(getHBaseColumnsFromRow(row));
            try {
                final HBaseCreateTableBuilder hBaseCreateTableBuilder = (HBaseCreateTableBuilder) getUpdateCallback()
                        .createTable(getSchema(), TABLE_NAME);

                hBaseCreateTableBuilder.setColumnFamilies(columnFamilies);
                hBaseCreateTableBuilder.execute();
                checkSuccesfullyInsertedTable();
            } catch (Exception e) {
                fail("Should not get an exception");
            }
        } else {
            warnAboutANotExecutedTest(getClass().getName(), new Object() {
            }.getClass().getEnclosingMethod().getName());
        }
    }

    /**
     * Goodflow. Create a table including the ID-Column (columnFamilies in constructor), should work
     */
    public void testCreateTableColumnFamiliesInConstrutor() {
        if (isConfigured()) {
            final HBaseTable table = createHBaseTable(TABLE_NAME, HBaseDataContext.FIELD_ID, CF_FOO, CF_BAR, null);
            final LinkedHashMap<HBaseColumn, Object> row = createRow(table, HBaseDataContext.FIELD_ID, CF_FOO, CF_BAR);
            final Set<String> columnFamilies = getColumnFamilies(getHBaseColumnsFromRow(row));
            try {
                getUpdateCallback().createTable(getSchema(), TABLE_NAME, columnFamilies).execute();
                checkSuccesfullyInsertedTable();
            } catch (Exception e) {
                fail("Should not get an exception");
            }
        } else {
            warnAboutANotExecutedTest(getClass().getName(), new Object() {
            }.getClass().getEnclosingMethod().getName());
        }
    }

    /**
     * Creates a set of columnFamilies out of a list of hbaseColumns
     *
     * @param columns
     * @return {@link Set}<{@link String}> of columnFamilies
     */
    private static Set<String> getColumnFamilies(List<HBaseColumn> columns) {
        return columns.stream().map(HBaseColumn::getColumnFamily).distinct().collect(Collectors.toSet());
    }
}