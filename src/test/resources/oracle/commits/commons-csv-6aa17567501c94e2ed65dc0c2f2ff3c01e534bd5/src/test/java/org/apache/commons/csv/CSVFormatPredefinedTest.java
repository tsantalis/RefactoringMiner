/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.csv;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link CSVFormat.Predefined}.
 */
public class CSVFormatPredefinedTest {

    private void test(final CSVFormat format, final String enumName) {
        Assert.assertEquals(format, CSVFormat.Predefined.valueOf(enumName).getFormat());
        Assert.assertEquals(format, CSVFormat.valueOf(enumName));
    }

    @Test
    public void testDefault() {
        test(CSVFormat.DEFAULT, "Default");
    }

    @Test
    public void testExcel() {
        test(CSVFormat.EXCEL, "Excel");
    }

    @Test
    public void testMySQL() {
        test(CSVFormat.MYSQL, "MySQL");
    }

    @Test
    public void testOracle() {
        test(CSVFormat.ORACLE, "Oracle");
    }

    @Test
    public void testPostgreSqlCsv() {
        test(CSVFormat.POSTGRESQL_CSV, "PostgreSQLCsv");
    }

    @Test
    public void testMongoDbCsv() {
        test(CSVFormat.MONGODB_CSV, "MongoDBCsv");
    }

    @Test
    public void testMongoDbTsv() {
        test(CSVFormat.MONGODB_TSV, "MongoDBTsv");
    }

    @Test
    public void testPostgreSqlText() {
        test(CSVFormat.POSTGRESQL_TEXT, "PostgreSQLText");
    }

    @Test
    public void testRFC4180() {
        test(CSVFormat.RFC4180, "RFC4180");
    }

    @Test
    public void testTDF() {
        test(CSVFormat.TDF, "TDF");
    }
}
