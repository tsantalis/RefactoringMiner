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
package org.apache.commons.csv.issues;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Assert;
import org.junit.Test;

public class JiraCsv198Test {

    private static final CSVFormat CSV_FORMAT = CSVFormat.EXCEL.withDelimiter('^').withFirstRecordAsHeader();

    @Test
    public void test() throws UnsupportedEncodingException, IOException {
        final InputStream pointsOfReference = getClass().getResourceAsStream("/CSV-198/optd_por_public.csv");
        Assert.assertNotNull(pointsOfReference);
        try (@SuppressWarnings("resource")
        CSVParser parser = CSV_FORMAT.parse(new InputStreamReader(pointsOfReference, "UTF-8"))) {
            for (final CSVRecord record : parser) {
                final String locationType = record.get("location_type");
                Assert.assertNotNull(locationType);
            }
        }
    }

}