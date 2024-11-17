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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.metamodel.MetaModelException;
import org.apache.metamodel.data.Style;
import org.apache.metamodel.insert.AbstractRowInsertionBuilder;
import org.apache.metamodel.insert.RowInsertionBuilder;
import org.apache.metamodel.schema.Column;

/**
 * A builder-class to insert rows in a HBase datastore.
 */
public class HBaseRowInsertionBuilder extends AbstractRowInsertionBuilder<HBaseUpdateCallback> {
    private List<HBaseColumn> columns = new ArrayList<>();
    private List<Object> values = new ArrayList<>();

    private int _indexOfIdColumn = -1;

    /**
     * Creates a {@link HBaseRowInsertionBuilder}. The table and the column's columnFamilies are checked to exist in the schema.
     * @param updateCallback
     * @param table
     * @param columns
     * @throws IllegalArgumentException the columns list can't be null or empty
     * @throws MetaModelException when no ID-column is found.
     */
    public HBaseRowInsertionBuilder(final HBaseUpdateCallback updateCallback, final HBaseTable table) {
        super(updateCallback, table);

        checkTable(updateCallback, table);
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
    private static Set<String> getColumnFamilies(final HBaseColumn[] columns) {
        return Arrays.stream(columns).map(HBaseColumn::getColumnFamily).distinct().collect(Collectors.toSet());
    }

    @Override
    public synchronized void execute() {
        if (_indexOfIdColumn == -1) {
            throw new MetaModelException("The ID-Column was not found");
        }

        // The columns parameter should match the table's columns, just to be sure, this is checked again
        checkColumnFamilies((HBaseTable) getTable(), getColumnFamilies(getColumns()));

        ((HBaseDataContext) getUpdateCallback().getDataContext()).getHBaseClient().insertRow(getTable().getName(),
                getColumns(), getValues(), _indexOfIdColumn);
    }

    @Override
    protected HBaseColumn[] getColumns() {
        return columns.toArray(new HBaseColumn[columns.size()]);
    }

    @Override
    protected Object[] getValues() {
        return values.toArray(new Object[values.size()]);
    }

    @Override
    public RowInsertionBuilder value(final Column column, final Object value, final Style style) {
        if (column == null) {
            throw new IllegalArgumentException("Column cannot be null.");
        }

        final HBaseColumn hbaseColumn = getHbaseColumn(column);

        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).equals(hbaseColumn)) {
                values.set(i, value);
                return this;
            }
        }

        if (hbaseColumn.isPrimaryKey()) {
            _indexOfIdColumn = columns.size();
        }

        columns.add((HBaseColumn) hbaseColumn);
        values.add(value);

        return this;
    }

    private HBaseColumn getHbaseColumn(final Column column) {
        if (column instanceof HBaseColumn) {
            return (HBaseColumn) column;
        } else {
            final String columnName = column.getName();
            final String[] columnNameParts = columnName.split(":");
            if (columnNameParts.length == 1) {
                return new HBaseColumn(columnNameParts[0], getTable());
            }
            if (columnNameParts.length == 2) {
                return new HBaseColumn(columnNameParts[0], columnNameParts[1], getTable());
            }
            throw new MetaModelException("Can't determine column family for column \"" + columnName + "\".");
        }
    }

    @Override
    public RowInsertionBuilder value(final int columnIndex, final Object value) {
        values.set(columnIndex, value);
        return this;
    }

    @Override
    public RowInsertionBuilder value(final String columnName, final Object value) {
        for (Column column : columns) {
            if (column.getName().equals(columnName)) {
                return value(column, value, null);
            }
        }

        throw new IllegalArgumentException("No such column in table: " + columnName + ", available columns are: "
                + columns);
    }

    @Override
    public boolean isSet(final Column column) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).equals(column)) {
                return values.get(i) != null;
            }
        }
        return false;
    }
}
