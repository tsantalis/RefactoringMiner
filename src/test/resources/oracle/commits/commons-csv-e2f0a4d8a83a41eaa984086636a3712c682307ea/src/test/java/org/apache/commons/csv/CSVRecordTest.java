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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CSVRecordTest {

    private enum EnumFixture {
        UNKNOWN_COLUMN
    }

    private String[] values;
    private CSVRecord record, recordWithHeader;
    private Map<String, Integer> headerMap;

    @BeforeEach
    public void setUp() throws Exception {
        values = new String[] { "A", "B", "C" };
        final String rowData = StringUtils.join(values, ',');
        try (final CSVParser parser = CSVFormat.DEFAULT.parse(new StringReader(rowData))) {
            record = parser.iterator().next();
        }
        final String[] headers = { "first", "second", "third" };
        try (final CSVParser parser = CSVFormat.DEFAULT.withHeader(headers).parse(new StringReader(rowData))) {
            recordWithHeader = parser.iterator().next();
            headerMap = parser.getHeaderMap();
        }
    }

    @Test
    public void testGetInt() {
        assertEquals(values[0], record.get(0));
        assertEquals(values[1], record.get(1));
        assertEquals(values[2], record.get(2));
    }

    @Test
    public void testGetString() {
        assertEquals(values[0], recordWithHeader.get("first"));
        assertEquals(values[1], recordWithHeader.get("second"));
        assertEquals(values[2], recordWithHeader.get("third"));
    }

    @Test
    public void testGetStringInconsistentRecord() {
        headerMap.put("fourth", Integer.valueOf(4));
        assertThrows(IllegalArgumentException.class, () -> recordWithHeader.get("fourth"));
    }

    @Test
    public void testGetStringNoHeader() {
        assertThrows(IllegalStateException.class, () -> record.get("first"));
    }

    @Test
    public void testGetUnmappedEnum() {
        assertThrows(IllegalArgumentException.class, () -> recordWithHeader.get(EnumFixture.UNKNOWN_COLUMN));
    }

    @Test
    public void testGetUnmappedName() {
        assertThrows(IllegalArgumentException.class, () -> assertNull(recordWithHeader.get("fourth")));
    }

    @Test
    public void testGetUnmappedNegativeInt() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> recordWithHeader.get(Integer.MIN_VALUE));
    }

    @Test
    public void testGetUnmappedPositiveInt() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> recordWithHeader.get(Integer.MAX_VALUE));
    }

    @Test
    public void testIsConsistent() {
        assertTrue(record.isConsistent());
        assertTrue(recordWithHeader.isConsistent());
        final Map<String, Integer> map = recordWithHeader.getParser().getHeaderMap();
        map.put("fourth", Integer.valueOf(4));
        // We are working on a copy of the map, so the record should still be OK.
        assertTrue(recordWithHeader.isConsistent());
    }

    @Test
    public void testIsInconsistent() throws IOException {
        final String[] headers = { "first", "second", "third" };
        final String rowData = StringUtils.join(values, ',');
        try (final CSVParser parser = CSVFormat.DEFAULT.withHeader(headers).parse(new StringReader(rowData))) {
            final Map<String, Integer> map = parser.getHeaderMapRaw();
            final CSVRecord record1 = parser.iterator().next();
            map.put("fourth", Integer.valueOf(4));
            assertFalse(record1.isConsistent());
        }
    }

    @Test
    public void testIsMapped() {
        assertFalse(record.isMapped("first"));
        assertTrue(recordWithHeader.isMapped("first"));
        assertFalse(recordWithHeader.isMapped("fourth"));
    }

    @Test
    public void testIsSet() {
        assertFalse(record.isSet("first"));
        assertTrue(recordWithHeader.isSet("first"));
        assertFalse(recordWithHeader.isSet("fourth"));
    }

    @Test
    public void testIterator() {
        int i = 0;
        for (final String value : record) {
            assertEquals(values[i], value);
            i++;
        }
    }

    @Test
    public void testPutInMap() {
        final Map<String, String> map = new ConcurrentHashMap<>();
        this.recordWithHeader.putIn(map);
        this.validateMap(map, false);
        // Test that we can compile with assigment to the same map as the param.
        final TreeMap<String, String> map2 = recordWithHeader.putIn(new TreeMap<String, String>());
        this.validateMap(map2, false);
    }

    @Test
    public void testRemoveAndAddColumns() throws IOException {
        // do:
        try (final CSVPrinter printer = new CSVPrinter(new StringBuilder(), CSVFormat.DEFAULT)) {
            final Map<String, String> map = recordWithHeader.toMap();
            map.remove("OldColumn");
            map.put("ZColumn", "NewValue");
            // check:
            final ArrayList<String> list = new ArrayList<>(map.values());
            Collections.sort(list);
            printer.printRecord(list);
            assertEquals("A,B,C,NewValue" + CSVFormat.DEFAULT.getRecordSeparator(), printer.getOut().toString());
        }
    }

    @Test
    public void testToMap() {
        final Map<String, String> map = this.recordWithHeader.toMap();
        this.validateMap(map, true);
    }

    @Test
    public void testToMapWithShortRecord() throws Exception {
        try (final CSVParser parser = CSVParser.parse("a,b", CSVFormat.DEFAULT.withHeader("A", "B", "C"))) {
            final CSVRecord shortRec = parser.iterator().next();
            shortRec.toMap();
        }
    }

    @Test
    public void testToMapWithNoHeader() throws Exception {
        try (final CSVParser parser = CSVParser.parse("a,b", CSVFormat.newFormat(','))) {
            final CSVRecord shortRec = parser.iterator().next();
            final Map<String, String> map = shortRec.toMap();
            assertNotNull(map, "Map is not null.");
            assertTrue(map.isEmpty(), "Map is empty.");
        }
    }

    private void validateMap(final Map<String, String> map, final boolean allowsNulls) {
        assertTrue(map.containsKey("first"));
        assertTrue(map.containsKey("second"));
        assertTrue(map.containsKey("third"));
        assertFalse(map.containsKey("fourth"));
        if (allowsNulls) {
            assertFalse(map.containsKey(null));
        }
        assertEquals("A", map.get("first"));
        assertEquals("B", map.get("second"));
        assertEquals("C", map.get("third"));
        assertEquals(null, map.get("fourth"));
    }

}
