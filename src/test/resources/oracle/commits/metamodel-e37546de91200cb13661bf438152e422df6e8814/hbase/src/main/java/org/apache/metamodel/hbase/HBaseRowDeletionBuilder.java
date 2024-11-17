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
import java.util.List;

import org.apache.metamodel.MetaModelException;
import org.apache.metamodel.delete.AbstractRowDeletionBuilder;
import org.apache.metamodel.query.FilterItem;
import org.apache.metamodel.query.LogicalOperator;
import org.apache.metamodel.query.OperatorType;
import org.apache.metamodel.query.SelectItem;
import org.apache.metamodel.schema.Column;
import org.apache.metamodel.schema.Table;

/**
 * A builder-class to delete rows based on their keys in a HBase datastore
 */
public class HBaseRowDeletionBuilder extends AbstractRowDeletionBuilder {

    private final HBaseDataContext _dataContext;

    /**
     * Creates a {@link HBaseRowDeletionBuilder}
     * @param hBaseWriter
     * @param table
     * @throws IllegalArgumentException when the hBaseWriter is null
     */
    public HBaseRowDeletionBuilder(final HBaseDataContext dataContext, final Table table) {
        super(table);
        if (dataContext == null) {
            throw new IllegalArgumentException("hBaseClient cannot be null");
        }
        this._dataContext = dataContext;
    }

    /**
     * @throws MetaModelException when value is null
     */
    @Override
    public synchronized void execute() {
        final List<FilterItem> whereItems = getWhereItems();
        if (whereItems == null || whereItems.size() == 0) {
            throw new IllegalArgumentException("HBase currently only supports deleting items by their row key.");
        }

        final FilterItem filterItem = whereItems.get(0);
        if (!HBaseDataContext.FIELD_ID.equals(filterItem.getSelectItem().getColumn().getName())) {
            throw new IllegalArgumentException("HBase currently only supports deleting items by their row key.");
        }

        getRowKeys(filterItem).forEach(rowKey -> _dataContext.getHBaseClient().deleteRow(getTable().getName(), rowKey));
    }

    private List<Object> getRowKeys(final FilterItem whereItem) {
        final List<Object> rowKeys = new ArrayList<>();

        if (whereItem.isCompoundFilter()) {
            final LogicalOperator logicalOperator = whereItem.getLogicalOperator();
            if (logicalOperator != LogicalOperator.OR) {
                throw new IllegalStateException(
                        "HBase currently only supports deleting items by their row key. Violated by operator between where items: "
                                + whereItem);
            }

            Arrays.stream(whereItem.getChildItems()).forEach(childItem -> rowKeys.addAll(getRowKeys(childItem)));
        } else {
            final OperatorType operator = whereItem.getOperator();
            if (!OperatorType.EQUALS_TO.equals(operator) && !OperatorType.IN.equals(operator)) {
                throw new IllegalStateException(
                        "HBase currently only supports deleting items by their row key. Violated by operator in where item: "
                                + whereItem);
            }

            final SelectItem selectItem = whereItem.getSelectItem();
            final Column column = selectItem.getColumn();
            final Object operand = whereItem.getOperand();

            if (column == null || operand == null || !column.isPrimaryKey() || selectItem.hasFunction()) {
                throw new IllegalStateException(
                        "HBase currently only supports deleting items by their row key. Violated by where item: "
                                + whereItem);
            }
            rowKeys.add(operand);
        }
        return rowKeys;
    }
}
