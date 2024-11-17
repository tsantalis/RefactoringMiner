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

import org.apache.metamodel.schema.ColumnType;
import org.apache.metamodel.schema.ColumnTypeImpl;
import org.apache.metamodel.schema.MutableColumn;
import org.apache.metamodel.schema.SuperColumnType;
import org.apache.metamodel.schema.Table;

public final class HBaseColumn extends MutableColumn {
    public static final ColumnType DEFAULT_COLUMN_TYPE_FOR_ID_COLUMN = new ColumnTypeImpl("BYTE[]",
            SuperColumnType.LITERAL_TYPE);
    public static final ColumnType DEFAULT_COLUMN_TYPE_FOR_COLUMN_FAMILIES = ColumnType.LIST;

    private final String columnFamily;
    private final String qualifier;

    public HBaseColumn(final String columnFamily, final Table table) {
        this(columnFamily, null, table, -1);
    }

    public HBaseColumn(final String columnFamily, final String qualifier, final Table table) {
        this(columnFamily, qualifier, table, -1);
    }

    public HBaseColumn(final String columnFamily, final Table table, final int columnNumber) {
        this(columnFamily, null, table, columnNumber);
    }

    public HBaseColumn(final String columnFamily, final String qualifier, final Table table, final int columnNumber) {
        this(columnFamily, qualifier, table, columnNumber, null);
    }

    public HBaseColumn(final String columnFamily, final String qualifier, final Table table, final int columnNumber,
            final ColumnType columnType) {
        super(columnFamily, table);
        if (columnFamily == null) {
            throw new IllegalArgumentException("Column family isn't allowed to be null.");
        } else if (table == null || !(table instanceof HBaseTable)) {
            throw new IllegalArgumentException("Table is null or isn't a HBaseTable.");
        }

        this.columnFamily = columnFamily;
        this.qualifier = qualifier;

        setColumnNumber(columnNumber);
        setPrimaryKey(HBaseDataContext.FIELD_ID.equals(columnFamily));

        // Set the columnType
        if (columnType != null) {
            setType(columnType);
        } else {
            if (isPrimaryKey() || qualifier != null) {
                setType(DEFAULT_COLUMN_TYPE_FOR_ID_COLUMN);
            } else {
                setType(DEFAULT_COLUMN_TYPE_FOR_COLUMN_FAMILIES);
            }
        }
    }

    public String getColumnFamily() {
        return columnFamily;
    }

    public String getQualifier() {
        return qualifier;
    }

    @Override
    public String getName() {
        if (qualifier == null) {
            return columnFamily;
        }
        return columnFamily + ":" + qualifier;
    }

    @Override
    public Boolean isNullable() {
        return !isPrimaryKey();
    }

    @Override
    public String getRemarks() {
        return null;
    }

    @Override
    public Integer getColumnSize() {
        return null;
    }

    @Override
    public String getNativeType() {
        // TODO: maybe change if no qualifier is present (and not identifier column).
        return "byte[]";
    }

    @Override
    public boolean isIndexed() {
        return false;
    }

    @Override
    public String getQuote() {
        return null;
    }
}
