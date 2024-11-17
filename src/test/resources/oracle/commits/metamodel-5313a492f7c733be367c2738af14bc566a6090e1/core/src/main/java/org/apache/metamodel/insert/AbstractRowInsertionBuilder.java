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
package org.apache.metamodel.insert;

import org.apache.metamodel.UpdateCallback;
import org.apache.metamodel.data.AbstractRowBuilder;
import org.apache.metamodel.data.Row;
import org.apache.metamodel.query.SelectItem;
import org.apache.metamodel.schema.Column;
import org.apache.metamodel.schema.Table;

import java.util.List;

/**
 * Abstract implementation of the {@link RowInsertionBuilder} interface,
 * provided as a convenience to {@link RowInsertable} implementations. Handles
 * all the building operations, but not the commit operation.
 */
public abstract class AbstractRowInsertionBuilder<U extends UpdateCallback> extends
        AbstractRowBuilder<RowInsertionBuilder> implements RowInsertionBuilder {

    private final U _updateCallback;
    private final Table _table;

    public AbstractRowInsertionBuilder(U updateCallback, Table table) {
        super(table);
        _updateCallback = updateCallback;
        _table = table;
    }

    public AbstractRowInsertionBuilder(final U updateCallback, final Table table, final List<Column> columns) {
        super(columns);
        _updateCallback = updateCallback;
        _table = table;
    }

    @Override
    public Table getTable() {
        return _table;
    }

    protected U getUpdateCallback() {
        return _updateCallback;
    }

    @Override
    public RowInsertionBuilder like(Row row) {
        List<SelectItem> selectItems = row.getSelectItems();
        for (int i = 0; i < selectItems.size(); i++) {
            SelectItem selectItem = selectItems.get(i);
            Column column = selectItem.getColumn();
            if (column != null) {
                if (_table == column.getTable()) {
                    value(column, row.getValue(i));
                } else {
                    value(column.getName(), row.getValue(i));
                }
            }
        }
        return this;
    }

    @Override
    public String toSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ");
        sb.append(_table.getQualifiedLabel());
        sb.append("(");
        Column[] columns = getColumns();
        for (int i = 0; i < columns.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(columns[i].getName());
        }
        sb.append(") VALUES (");
        Object[] values = getValues();
        for (int i = 0; i < values.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            final Object value = values[i];
            final String stringValue;
            if (value == null) {
                stringValue = "NULL";
            } else if (value instanceof String) {
                stringValue = "\"" + value + "\"";
            } else {
                stringValue = value.toString();
            }
            sb.append(stringValue);
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String toString() {
        return toSql();
    }
}
