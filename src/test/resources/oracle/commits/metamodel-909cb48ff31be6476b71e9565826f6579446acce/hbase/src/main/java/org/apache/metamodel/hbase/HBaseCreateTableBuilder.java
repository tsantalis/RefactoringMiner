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

    private Set<String> _columnFamilies;

    public HBaseCreateTableBuilder(final HBaseUpdateCallback updateCallback, final Schema schema, final String name) {
        this(updateCallback, schema, name, null);
    }

    /**
     * Create a {@link HBaseCreateTableBuilder}.
     * Throws an {@link IllegalArgumentException} if the schema isn't a {@link MutableSchema}.
     * @param updateCallback
     * @param schema
     * @param name
     * @param columnFamilies
     */
    public HBaseCreateTableBuilder(final HBaseUpdateCallback updateCallback, final Schema schema, final String name,
            final Set<String> columnFamilies) {
        super(updateCallback, schema, name);
        if (!(schema instanceof MutableSchema)) {
            throw new IllegalArgumentException("Not a mutable schema: " + schema);
        }
        this._columnFamilies = columnFamilies;
    }

    @Override
    public Table execute() {
        if (_columnFamilies == null || _columnFamilies.isEmpty()) {
            throw new MetaModelException("Creating a table without columnFamilies");
        }

        final Table table = getTable();

        // Add the table to the datastore
        ((HBaseDataContext) getUpdateCallback().getDataContext()).getHBaseClient().createTable(table.getName(),
                _columnFamilies);

        // Update the schema
        addNewTableToSchema(table);
        return getSchema().getTableByName(table.getName());
    }

    /**
     * Set the columnFamilies. This should be used when creating this object using the
     * {@link HBaseCreateTableBuilder#HBaseCreateTableBuilder(HBaseUpdateCallback, Schema, String)}
     * constructor
     * @param columnFamilies
     */
    public void setColumnFamilies(final Set<String> columnFamilies) {
        this._columnFamilies = columnFamilies;
    }

    /**
     * Add the new {@link Table} to the {@link MutableSchema}
     * @param table
     * @param data.updateCallback
     * @return {@link MutableSchema}
     */
    private void addNewTableToSchema(final Table table) {
        final MutableSchema schema = (MutableSchema) getSchema();
        final SimpleTableDef emptyTableDef = new SimpleTableDef(table.getName(), _columnFamilies.toArray(
                new String[_columnFamilies.size()]));
        schema.addTable(new HBaseTable((HBaseDataContext) getUpdateCallback().getDataContext(), emptyTableDef, schema,
                HBaseConfiguration.DEFAULT_ROW_KEY_TYPE));
    }
}
