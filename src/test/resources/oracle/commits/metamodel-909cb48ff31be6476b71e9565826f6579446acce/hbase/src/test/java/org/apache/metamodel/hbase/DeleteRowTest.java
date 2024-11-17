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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.apache.metamodel.MetaModelException;
import org.apache.metamodel.insert.RowInsertionBuilder;
import org.apache.metamodel.schema.MutableTable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DeleteRowTest extends HBaseUpdateCallbackTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * Delete is supported
     */
    @Test
    public void testDeleteSupported() {
        assertTrue(getUpdateCallback().isDeleteSupported());
    }

    /**
     * Having the table type wrong, should throw an exception
     */
    @Test
    public void testTableWrongType() {
        final MutableTable mutableTable = new MutableTable();

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Not an HBase table: " + mutableTable);

        getUpdateCallback().deleteFrom(mutableTable);
    }

    /**
     * Creating a HBaseRowDeletionBuilder with the hBaseClient null, should throw an exception
     *
     * @throws IOException
     */
    @Test
    public void testHBaseClientNullAtBuilder() throws IOException {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("hBaseClient cannot be null");
        final HBaseTable existingTable = createAndAddTableToDatastore(TABLE_NAME, HBaseDataContext.FIELD_ID, CF_FOO,
                CF_BAR);
        new HBaseRowDeletionBuilder(null, existingTable);
    }

    /**
     * Not setting the rowkey, should throw an exception
     *
     * @throws IOException
     */
    @Test
    public void testNotSettingRowkey() throws IOException {
        exception.expect(MetaModelException.class);
        exception.expectMessage("Key cannot be null");

        final HBaseTable existingTable = createAndAddTableToDatastore(TABLE_NAME, HBaseDataContext.FIELD_ID, CF_FOO,
                CF_BAR);
        getUpdateCallback().deleteFrom(existingTable).execute();
    }

    /**
     * Creating a HBaseClient with the tableName null, should throw a exception
     */
    @Test
    public void testCreatingTheHBaseClientWithTableNameNull() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Can't delete a row without having tableName or rowKey");

        new HBaseClient(getDataContext().getConnection()).deleteRow(null, new String("1"));
    }

    /**
     * Creating a HBaseClient with the rowKey null, should throw a exception
     */
    @Test
    public void testCreatingTheHBaseClientWithRowKeyNull() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Can't delete a row without having tableName or rowKey");

        new HBaseClient(getDataContext().getConnection()).deleteRow("tableName", null);
    }

    /**
     * Goodflow. Deleting a row, that doesn't exist, should not throw an exception
     *
     * @throws IOException
     */
    @Test
    public void testDeletingNotExistingRow() throws IOException {
        final HBaseTable existingTable = createAndAddTableToDatastore(TABLE_NAME, HBaseDataContext.FIELD_ID, CF_FOO,
                CF_BAR);

        checkRows(false, false);
        final HBaseRowDeletionBuilder rowDeletionBuilder = (HBaseRowDeletionBuilder) getUpdateCallback().deleteFrom(
                existingTable);
        rowDeletionBuilder.setKey(RK_1);
        rowDeletionBuilder.execute();
        checkRows(false, false);
    }

    /**
     * Goodflow. Deleting a row, which has an empty rowKey value, should not throw an exception
     *
     * @throws IOException
     */
    @Test
    public void testUsingAnEmptyRowKeyValue() throws IOException {
        final HBaseTable existingTable = createAndAddTableToDatastore(TABLE_NAME, HBaseDataContext.FIELD_ID, CF_FOO,
                CF_BAR);

        checkRows(false, false);
        final HBaseRowDeletionBuilder rowDeletionBuilder = (HBaseRowDeletionBuilder) getUpdateCallback().deleteFrom(
                existingTable);
        rowDeletionBuilder.setKey("");
        rowDeletionBuilder.execute();
        checkRows(false, false);
    }

    /**
     * Goodflow. Deleting a row succesfully.
     *
     * @throws IOException
     */
    @Test
    public void testDeleteRowSuccesfully() throws IOException {
        final HBaseTable existingTable = createAndAddTableToDatastore(TABLE_NAME, HBaseDataContext.FIELD_ID, CF_FOO,
                CF_BAR);
        final Map<HBaseColumn, Object> row = createRow(existingTable, HBaseDataContext.FIELD_ID, CF_FOO, CF_BAR, false);

        checkRows(false, false);
        final RowInsertionBuilder rowInsertionBuilder = getUpdateCallback().insertInto(existingTable);
        setValuesInInsertionBuilder(row, rowInsertionBuilder);
        rowInsertionBuilder.execute();
        checkRows(true, false);
        final HBaseRowDeletionBuilder rowDeletionBuilder = (HBaseRowDeletionBuilder) getUpdateCallback().deleteFrom(
                existingTable);
        rowDeletionBuilder.setKey(RK_1);
        rowDeletionBuilder.execute();
        checkRows(false, false);
    }
}
