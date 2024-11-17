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

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HBaseClientTest extends HBaseTestCase {
    @Rule
    public ExpectedException exception = ExpectedException.none();

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
     * Creating a HBaseClient with the tableName null, should throw a exception
     */
    @Test
    public void testDeleteRowWithoutTableName() {
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
}
