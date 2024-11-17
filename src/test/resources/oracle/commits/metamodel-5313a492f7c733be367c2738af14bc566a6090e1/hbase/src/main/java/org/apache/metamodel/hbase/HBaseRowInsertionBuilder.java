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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.metamodel.MetaModelException;
import org.apache.metamodel.insert.AbstractRowInsertionBuilder;
import org.apache.metamodel.schema.Column;

/**
 * A builder-class to insert rows in a HBase datastore.
 */
// TODO: Possible future improvement: Make it possible to change the columns for each execute.
// Now each row will get exactly the same columns.
public class HBaseRowInsertionBuilder extends AbstractRowInsertionBuilder<HBaseUpdateCallback> {
    private final int _indexOfIdColumn;

    /**
     * Creates a {@link HBaseRowInsertionBuilder}. The table and the column's columnFamilies are checked to exist in the schema.
     * @param updateCallback
     * @param table
     * @param columns
     * @throws IllegalArgumentException the columns list can't be null or empty
     * @throws MetaModelException when no ID-column is found.
     */
    public HBaseRowInsertionBuilder(final HBaseUpdateCallback updateCallback, final HBaseTable table,
            final List<HBaseColumn> columns) {
        super(updateCallback, table, columns.stream().map(column -> (Column) column).collect(Collectors.toList()));

        this._indexOfIdColumn = getIndexOfIdColumn(columns);
        if (_indexOfIdColumn == -1) {
            throw new MetaModelException("The ID-Column was not found");
        }

        checkTable(updateCallback, table);
        // The columns parameter should match the table's columns, just to be sure, this is checked again
        checkColumnFamilies(table, getColumnFamilies(columns));
    }

    /**
     * Returns the index of the ID-column (see {@link HBaseDataContext#FIELD_ID}) in an array of HBaseColumns.
     *
     * @param columns
     * @return index of the ID-column
     */
    private static int getIndexOfIdColumn(final List<HBaseColumn> columns) {
        for (int i = 0; i < columns.size(); i++) {
            if (HBaseDataContext.FIELD_ID.equals(columns.get(i).getColumnFamily())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Check if the table and it's columnFamilies exist in the schema
     *
     * @param updateCallback
     * @param tableGettingInserts
     * @throws MetaModelException If the table or the columnFamilies don't exist
     */
    private void checkTable(final HBaseUpdateCallback updateCallback, final HBaseTable tableGettingInserts) {
        final HBaseTable tableInSchema = (HBaseTable) updateCallback.getDataContext().getDefaultSchema().getTableByName(
                tableGettingInserts.getName());
        if (tableInSchema == null) {
            throw new MetaModelException("Trying to insert data into table: " + tableGettingInserts.getName()
                    + ", which doesn't exist yet");
        }
        checkColumnFamilies(tableInSchema, tableGettingInserts.getColumnFamilies());
    }

    /**
     * Check if a list of columnNames all exist in this table
     * @param table Checked tabled
     * @param columnFamilyNamesOfCheckedTable
     * @throws MetaModelException If a column doesn't exist
     */
    public void checkColumnFamilies(final HBaseTable table, final Set<String> columnFamilyNamesOfCheckedTable) {
        Set<String> columnFamilyNamesOfExistingTable = table.getColumnFamilies();

        for (String columnNameOfCheckedTable : columnFamilyNamesOfCheckedTable) {
            boolean matchingColumnFound = false;
            Iterator<String> columnFamilies = columnFamilyNamesOfExistingTable.iterator();
            while (!matchingColumnFound && columnFamilies.hasNext()) {
                if (columnNameOfCheckedTable.equals(columnFamilies.next())) {
                    matchingColumnFound = true;
                }
            }
            if (!matchingColumnFound) {
                throw new MetaModelException(String.format("ColumnFamily: %s doesn't exist in the schema of the table",
                        columnNameOfCheckedTable));
            }
        }
    }

    /**
     * Creates a set of columnFamilies out of a list of hbaseColumns
     *
     * @param columns
     * @return {@link LinkedHashSet}
     */
    private static Set<String> getColumnFamilies(final List<HBaseColumn> columns) {
        return columns.stream().map(HBaseColumn::getColumnFamily).distinct().collect(Collectors.toSet());
    }

    @Override
    public synchronized void execute() {
        ((HBaseDataContext) getUpdateCallback().getDataContext()).getHBaseClient().insertRow(getTable().getName(),
                getColumns(), getValues(), _indexOfIdColumn);
    }

    @Override
    public HBaseColumn[] getColumns() {
        return Arrays.stream(super.getColumns()).map(column -> (HBaseColumn) column).toArray(
                size -> new HBaseColumn[size]);
    }
}
