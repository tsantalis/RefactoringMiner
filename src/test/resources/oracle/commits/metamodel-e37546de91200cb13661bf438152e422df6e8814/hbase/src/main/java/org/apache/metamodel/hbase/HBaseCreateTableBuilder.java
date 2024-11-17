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

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.metamodel.MetaModelException;
import org.apache.metamodel.create.AbstractTableCreationBuilder;
import org.apache.metamodel.schema.MutableSchema;
import org.apache.metamodel.schema.Schema;
import org.apache.metamodel.schema.Table;
import org.apache.metamodel.util.SimpleTableDef;

/**
 * A builder-class to create tables in a HBase datastore
 */
public class HBaseCreateTableBuilder extends AbstractTableCreationBuilder<HBaseUpdateCallback> {

    /**
     * Create a {@link HBaseCreateTableBuilder}.
     * Throws an {@link IllegalArgumentException} if the schema isn't a {@link MutableSchema}.
     * @param updateCallback
     * @param schema
     * @param name
     * @param columnFamilies
     */
    public HBaseCreateTableBuilder(final HBaseUpdateCallback updateCallback, final Schema schema, final String name) {
        super(updateCallback, schema, name);
        if (!(schema instanceof MutableSchema)) {
            throw new IllegalArgumentException("Not a mutable schema: " + schema);
        }
    }

    @Override
    public Table execute() {
        Set<String> columnFamilies = getColumnFamilies();

        if (columnFamilies == null || columnFamilies.isEmpty()) {
            throw new MetaModelException("Can't create a table without column families.");
        }

        final Table table = getTable();

        // Add the table to the datastore
        ((HBaseDataContext) getUpdateCallback().getDataContext()).getHBaseClient().createTable(table.getName(),
                columnFamilies);

        // Update the schema
        addNewTableToSchema(table);
        return getSchema().getTableByName(table.getName());
    }

    private Set<String> getColumnFamilies() {
        return getTable().getColumns().stream().map(column -> {
            if (column instanceof HBaseColumn) {
                return ((HBaseColumn) column).getColumnFamily();
            } else {
                String columnName = column.getName();

                String[] columnNameParts = columnName.split(":");
                if (columnNameParts.length > 0 && columnNameParts.length < 3) {
                    return columnNameParts[0];
                } else {
                    throw new MetaModelException("Can't determine column family for column \"" + columnName + "\".");
                }
            }
        }).distinct().collect(Collectors.toSet());
    }

    /**
     * Add the new {@link Table} to the {@link MutableSchema}
     * @param table
     * @param data.updateCallback
     * @return {@link MutableSchema}
     */
    private void addNewTableToSchema(final Table table) {
        final MutableSchema schema = (MutableSchema) getSchema();
        final Set<String> columnFamilies = getColumnFamilies();
        final SimpleTableDef emptyTableDef = new SimpleTableDef(table.getName(), columnFamilies.toArray(
                new String[columnFamilies.size()]));
        schema.addTable(new HBaseTable((HBaseDataContext) getUpdateCallback().getDataContext(), emptyTableDef, schema,
                HBaseConfiguration.DEFAULT_ROW_KEY_TYPE));
    }
}
