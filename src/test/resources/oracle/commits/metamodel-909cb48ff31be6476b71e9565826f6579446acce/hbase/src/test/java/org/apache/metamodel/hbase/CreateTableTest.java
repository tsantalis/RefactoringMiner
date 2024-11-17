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
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.metamodel.MetaModelException;
import org.apache.metamodel.schema.ImmutableSchema;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.Sets;

public class CreateTableTest extends HBaseUpdateCallbackTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * Check if creating table is supported
     */
    @Test
    public void testCreateTableSupported() {
        assertTrue(getUpdateCallback().isCreateTableSupported());
    }

    /**
     * Create a table with an immutableSchema, should throw a IllegalArgumentException
     */
    @Test
    public void testWrongSchema() {
        final ImmutableSchema immutableSchema = new ImmutableSchema(getSchema());

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Not a mutable schema: " + immutableSchema);

        getUpdateCallback().createTable(immutableSchema, TABLE_NAME).execute();
    }

    /**
     * Create a table without columnFamilies, should throw a MetaModelException
     */
    @Test
    public void testCreateTableWithoutColumnFamilies() {
        exception.expect(MetaModelException.class);
        exception.expectMessage("Creating a table without columnFamilies");

        getUpdateCallback().createTable(getSchema(), TABLE_NAME).execute();
    }

    /**
     * Create a table with columnFamilies null, should throw a MetaModelException
     */
    @Test
    public void testColumnFamiliesNull() {
        exception.expect(MetaModelException.class);
        exception.expectMessage("Creating a table without columnFamilies");

        getUpdateCallback().createTable(getSchema(), TABLE_NAME, null).execute();
    }

    /**
     * Create a table with columnFamilies empty, should throw a MetaModelException
     */
    @Test
    public void testColumnFamiliesEmpty() {
        exception.expect(MetaModelException.class);
        exception.expectMessage("Creating a table without columnFamilies");

        final Set<String> columnFamilies = new LinkedHashSet<String>();
        getUpdateCallback().createTable(getSchema(), TABLE_NAME, columnFamilies).execute();
    }

    /**
     * Creating a HBaseClient with the tableName null, should throw a exception
     */
    @Test
    public void testCreatingTheHBaseClientWithTableNameNull() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Can't create a table without having the tableName or columnFamilies");

        final Set<String> columnFamilies = new LinkedHashSet<>();
        columnFamilies.add("1");
        new HBaseClient(getDataContext().getConnection()).createTable(null, columnFamilies);
    }

    /**
     * Creating a HBaseClient with the tableName null, should throw a exception
     */
    @Test
    public void testCreatingTheHBaseClientWithColumnFamiliesNull() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Can't create a table without having the tableName or columnFamilies");

        new HBaseClient(getDataContext().getConnection()).createTable("1", null);
    }

    /**
     * Creating a HBaseClient with the tableName null, should throw a exception
     */
    @Test
    public void testCreatingTheHBaseClientWithColumnFamiliesEmpty() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Can't create a table without having the tableName or columnFamilies");

        final Set<String> columnFamilies = new LinkedHashSet<>();
        new HBaseClient(getDataContext().getConnection()).createTable("1", columnFamilies);
    }

    /**
     * Goodflow. Create a table without the ID-Column, should work
     *
     * @throws IOException
     */
    @Test
    public void testCreateTableWithoutIDColumn() throws IOException {
        final HBaseCreateTableBuilder hBaseCreateTableBuilder = (HBaseCreateTableBuilder) getUpdateCallback()
                .createTable(getSchema(), TABLE_NAME);

        hBaseCreateTableBuilder.setColumnFamilies(Sets.newHashSet(CF_FOO, CF_BAR));
        hBaseCreateTableBuilder.execute();
        checkSuccesfullyInsertedTable();
    }

    /**
     * Goodflow. Create a table including the ID-Column (columnFamilies not in constructor), should work
     *
     * @throws IOException
     */
    @Test
    public void testSettingColumnFamiliesAfterConstrutor() throws IOException {
        final HBaseCreateTableBuilder hBaseCreateTableBuilder = (HBaseCreateTableBuilder) getUpdateCallback()
                .createTable(getSchema(), TABLE_NAME);

        hBaseCreateTableBuilder.setColumnFamilies(Sets.newHashSet(CF_FOO, CF_BAR));
        hBaseCreateTableBuilder.execute();
        checkSuccesfullyInsertedTable();
    }

    /**
     * Goodflow. Create a table including the ID-Column (columnFamilies in constructor), should work
     *
     * @throws IOException
     */
    @Test
    public void testCreateTableColumnFamiliesInConstrutor() throws IOException {
        getUpdateCallback().createTable(getSchema(), TABLE_NAME, Sets.newHashSet(CF_FOO, CF_BAR)).execute();
        checkSuccesfullyInsertedTable();
    }
}