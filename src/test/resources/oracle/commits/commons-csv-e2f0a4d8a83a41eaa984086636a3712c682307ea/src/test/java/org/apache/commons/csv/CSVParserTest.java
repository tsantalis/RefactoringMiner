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

import static org.apache.commons.csv.Constants.CR;
import static org.apache.commons.csv.Constants.CRLF;
import static org.apache.commons.csv.Constants.LF;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.io.input.BOMInputStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * CSVParserTest
 *
 * The test are organized in three different sections: The 'setter/getter' section, the lexer section and finally the
 * parser section. In case a test fails, you should follow a top-down approach for fixing a potential bug (its likely
 * that the parser itself fails if the lexer has problems...).
 */
public class CSVParserTest {

    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    private static final String UTF_8_NAME = UTF_8.name();

    private static final String CSV_INPUT = "a,b,c,d\n" + " a , b , 1 2 \n" + "\"foo baar\", b,\n"
            // + " \"foo\n,,\n\"\",,\n\\\"\",d,e\n";
            + "   \"foo\n,,\n\"\",,\n\"\"\",d,e\n"; // changed to use standard CSV escaping

    private static final String CSV_INPUT_1 = "a,b,c,d";

    private static final String CSV_INPUT_2 = "a,b,1 2";

    private static final String[][] RESULT = { { "a", "b", "c", "d" }, { "a", "b", "1 2" }, { "foo baar", "b", "" },
            { "foo\n,,\n\",,\n\"", "d", "e" } };

    private BOMInputStream createBOMInputStream(final String resource) throws IOException {
        final URL url = ClassLoader.getSystemClassLoader().getResource(resource);
        return new BOMInputStream(url.openStream());
    }

    private void parseFully(final CSVParser parser) {
        for (final CSVRecord csvRecord : parser) {
            assertNotNull(csvRecord);
        }
    }

    @Test
    public void testBackslashEscaping() throws IOException {

        // To avoid confusion over the need for escaping chars in java code,
        // We will test with a forward slash as the escape char, and a single
        // quote as the encapsulator.

        final String code = "one,two,three\n" // 0
        + "'',''\n" // 1) empty encapsulators
                + "/',/'\n" // 2) single encapsulators
                + "'/'','/''\n" // 3) single encapsulators encapsulated via escape
                + "'''',''''\n" // 4) single encapsulators encapsulated via doubling
                + "/,,/,\n" // 5) separator escaped
                + "//,//\n" // 6) escape escaped
                + "'//','//'\n" // 7) escape escaped in encapsulation
                + "   8   ,   \"quoted \"\" /\" // string\"   \n" // don't eat spaces
                + "9,   /\n   \n" // escaped newline
                + "";
        final String[][] res = { { "one", "two", "three" }, // 0
                { "", "" }, // 1
                { "'", "'" }, // 2
                { "'", "'" }, // 3
                { "'", "'" }, // 4
                { ",", "," }, // 5
                { "/", "/" }, // 6
                { "/", "/" }, // 7
                { "   8   ", "   \"quoted \"\" /\" / string\"   " }, { "9", "   \n   " }, };

        final CSVFormat format = CSVFormat.newFormat(',').withQuote('\'').withRecordSeparator(CRLF).withEscape('/')
                .withIgnoreEmptyLines();

        try (final CSVParser parser = CSVParser.parse(code, format)) {
            final List<CSVRecord> records = parser.getRecords();
            assertTrue(records.size() > 0);

            Utils.compare("Records do not match expected result", res, records);
        }
    }

    @Test
    public void testBackslashEscaping2() throws IOException {

        // To avoid confusion over the need for escaping chars in java code,
        // We will test with a forward slash as the escape char, and a single
        // quote as the encapsulator.

        final String code = "" + " , , \n" // 1)
                + " \t ,  , \n" // 2)
                + " // , /, , /,\n" // 3)
                + "";
        final String[][] res = { { " ", " ", " " }, // 1
                { " \t ", "  ", " " }, // 2
                { " / ", " , ", " ," }, // 3
        };

        final CSVFormat format = CSVFormat.newFormat(',').withRecordSeparator(CRLF).withEscape('/')
                .withIgnoreEmptyLines();

        try (final CSVParser parser = CSVParser.parse(code, format)) {
            final List<CSVRecord> records = parser.getRecords();
            assertTrue(records.size() > 0);

            Utils.compare("", res, records);
        }
    }

    @Test
    @Disabled
    public void testBackslashEscapingOld() throws IOException {
        final String code = "one,two,three\n" + "on\\\"e,two\n" + "on\"e,two\n" + "one,\"tw\\\"o\"\n" +
                "one,\"t\\,wo\"\n" + "one,two,\"th,ree\"\n" + "\"a\\\\\"\n" + "a\\,b\n" + "\"a\\\\,b\"";
        final String[][] res = { { "one", "two", "three" }, { "on\\\"e", "two" }, { "on\"e", "two" },
                { "one", "tw\"o" }, { "one", "t\\,wo" }, // backslash in quotes only escapes a delimiter (",")
                { "one", "two", "th,ree" }, { "a\\\\" }, // backslash in quotes only escapes a delimiter (",")
                { "a\\", "b" }, // a backslash must be returnd
                { "a\\\\,b" } // backslash in quotes only escapes a delimiter (",")
        };
        try (final CSVParser parser = CSVParser.parse(code, CSVFormat.DEFAULT)) {
            final List<CSVRecord> records = parser.getRecords();
            assertEquals(res.length, records.size());
            assertTrue(records.size() > 0);
            for (int i = 0; i < res.length; i++) {
                assertArrayEquals(res[i], records.get(i).values());
            }
        }
    }

    @Test
    @Disabled("CSV-107")
    public void testBOM() throws IOException {
        final URL url = ClassLoader.getSystemClassLoader().getResource("CSVFileParser/bom.csv");
        try (final CSVParser parser = CSVParser.parse(url, Charset.forName(UTF_8_NAME), CSVFormat.EXCEL.withHeader())) {
            for (final CSVRecord record : parser) {
                final String string = record.get("Date");
                assertNotNull(string);
                // System.out.println("date: " + record.get("Date"));
            }
        }
    }

    @Test
    public void testBOMInputStream_ParserWithInputStream() throws IOException {
        try (final BOMInputStream inputStream = createBOMInputStream("CSVFileParser/bom.csv");
                final CSVParser parser = CSVParser.parse(inputStream, UTF_8, CSVFormat.EXCEL.withHeader())) {
            for (final CSVRecord record : parser) {
                final String string = record.get("Date");
                assertNotNull(string);
                // System.out.println("date: " + record.get("Date"));
            }
        }
    }

    @Test
    public void testBOMInputStream_ParserWithReader() throws IOException {
        try (final Reader reader = new InputStreamReader(createBOMInputStream("CSVFileParser/bom.csv"), UTF_8_NAME);
                final CSVParser parser = new CSVParser(reader, CSVFormat.EXCEL.withHeader())) {
            for (final CSVRecord record : parser) {
                final String string = record.get("Date");
                assertNotNull(string);
                // System.out.println("date: " + record.get("Date"));
            }
        }
    }

    @Test
    public void testBOMInputStream_parseWithReader() throws IOException {
        try (final Reader reader = new InputStreamReader(createBOMInputStream("CSVFileParser/bom.csv"), UTF_8_NAME);
                final CSVParser parser = CSVParser.parse(reader, CSVFormat.EXCEL.withHeader())) {
            for (final CSVRecord record : parser) {
                final String string = record.get("Date");
                assertNotNull(string);
                // System.out.println("date: " + record.get("Date"));
            }
        }
    }

    @Test
    public void testCarriageReturnEndings() throws IOException {
        final String code = "foo\rbaar,\rhello,world\r,kanu";
        try (final CSVParser parser = CSVParser.parse(code, CSVFormat.DEFAULT)) {
            final List<CSVRecord> records = parser.getRecords();
            assertEquals(4, records.size());
        }
    }

    @Test
    public void testCarriageReturnLineFeedEndings() throws IOException {
        final String code = "foo\r\nbaar,\r\nhello,world\r\n,kanu";
        try (final CSVParser parser = CSVParser.parse(code, CSVFormat.DEFAULT)) {
            final List<CSVRecord> records = parser.getRecords();
            assertEquals(4, records.size());
        }
    }

    @Test
    public void testClose() throws Exception {
        final Reader in = new StringReader("# comment\na,b,c\n1,2,3\nx,y,z");
        final Iterator<CSVRecord> records;
        try (final CSVParser parser = CSVFormat.DEFAULT.withCommentMarker('#').withHeader().parse(in)) {
            records = parser.iterator();
            assertTrue(records.hasNext());
        }
        assertFalse(records.hasNext());
        assertThrows(NoSuchElementException.class, records::next);
    }

    @Test
    public void testCSV57() throws Exception {
        try (final CSVParser parser = CSVParser.parse("", CSVFormat.DEFAULT)) {
            final List<CSVRecord> list = parser.getRecords();
            assertNotNull(list);
            assertEquals(0, list.size());
        }
    }

    @Test
    public void testDefaultFormat() throws IOException {
        final String code = "" + "a,b#\n" // 1)
                + "\"\n\",\" \",#\n" // 2)
                + "#,\"\"\n" // 3)
                + "# Final comment\n"// 4)
                ;
        final String[][] res = { { "a", "b#" }, { "\n", " ", "#" }, { "#", "" }, { "# Final comment" } };

        CSVFormat format = CSVFormat.DEFAULT;
        assertFalse(format.isCommentMarkerSet());
        final String[][] res_comments = { { "a", "b#" }, { "\n", " ", "#" }, };

        try (final CSVParser parser = CSVParser.parse(code, format)) {
            final List<CSVRecord> records = parser.getRecords();
            assertTrue(records.size() > 0);

            Utils.compare("Failed to parse without comments", res, records);

            format = CSVFormat.DEFAULT.withCommentMarker('#');
        }
        try (final CSVParser parser = CSVParser.parse(code, format)) {
            final List<CSVRecord> records = parser.getRecords();

            Utils.compare("Failed to parse with comments", res_comments, records);
        }
    }

    @Test
    public void testDuplicateHeadersNotAllowed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CSVParser.parse("a,b,a\n1,2,3\nx,y,z",
                        CSVFormat.DEFAULT.withHeader(new String[] {}).withAllowDuplicateHeaderNames(false)));
    }

    @Test
    public void testDuplicateHeadersAllowedByDefault() throws Exception {
        CSVParser.parse("a,b,a\n1,2,3\nx,y,z", CSVFormat.DEFAULT.withHeader(new String[] {}));
    }

    @Test
    public void testEmptyFileHeaderParsing() throws Exception {
        try (final CSVParser parser = CSVParser.parse("", CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            assertNull(parser.nextRecord());
            assertTrue(parser.getHeaderNames().isEmpty());
        }
    }

    @Test
    public void testEmptyFile() throws Exception {
        try (final CSVParser parser = CSVParser.parse("", CSVFormat.DEFAULT)) {
            assertNull(parser.nextRecord());
        }
    }

    @Test
    public void testEmptyLineBehaviorCSV() throws Exception {
        final String[] codes = { "hello,\r\n\r\n\r\n", "hello,\n\n\n", "hello,\"\"\r\n\r\n\r\n", "hello,\"\"\n\n\n" };
        final String[][] res = { { "hello", "" } // CSV format ignores empty lines
        };
        for (final String code : codes) {
            try (final CSVParser parser = CSVParser.parse(code, CSVFormat.DEFAULT)) {
                final List<CSVRecord> records = parser.getRecords();
                assertEquals(res.length, records.size());
                assertTrue(records.size() > 0);
                for (int i = 0; i < res.length; i++) {
                    assertArrayEquals(res[i], records.get(i).values());
                }
            }
        }
    }

    @Test
    public void testEmptyLineBehaviorExcel() throws Exception {
        final String[] codes = { "hello,\r\n\r\n\r\n", "hello,\n\n\n", "hello,\"\"\r\n\r\n\r\n", "hello,\"\"\n\n\n" };
        final String[][] res = { { "hello", "" }, { "" }, // Excel format does not ignore empty lines
                { "" } };
        for (final String code : codes) {
            try (final CSVParser parser = CSVParser.parse(code, CSVFormat.EXCEL)) {
                final List<CSVRecord> records = parser.getRecords();
                assertEquals(res.length, records.size());
                assertTrue(records.size() > 0);
                for (int i = 0; i < res.length; i++) {
                    assertArrayEquals(res[i], records.get(i).values());
                }
            }
        }
    }

    @Test
    public void testEndOfFileBehaviorCSV() throws Exception {
        final String[] codes = { "hello,\r\n\r\nworld,\r\n", "hello,\r\n\r\nworld,", "hello,\r\n\r\nworld,\"\"\r\n",
                "hello,\r\n\r\nworld,\"\"", "hello,\r\n\r\nworld,\n", "hello,\r\n\r\nworld,",
                "hello,\r\n\r\nworld,\"\"\n", "hello,\r\n\r\nworld,\"\"" };
        final String[][] res = { { "hello", "" }, // CSV format ignores empty lines
                { "world", "" } };
        for (final String code : codes) {
            try (final CSVParser parser = CSVParser.parse(code, CSVFormat.DEFAULT)) {
                final List<CSVRecord> records = parser.getRecords();
                assertEquals(res.length, records.size());
                assertTrue(records.size() > 0);
                for (int i = 0; i < res.length; i++) {
                    assertArrayEquals(res[i], records.get(i).values());
                }
            }
        }
    }

    @Test
    public void testEndOfFileBehaviorExcel() throws Exception {
        final String[] codes = { "hello,\r\n\r\nworld,\r\n", "hello,\r\n\r\nworld,", "hello,\r\n\r\nworld,\"\"\r\n",
                "hello,\r\n\r\nworld,\"\"", "hello,\r\n\r\nworld,\n", "hello,\r\n\r\nworld,",
                "hello,\r\n\r\nworld,\"\"\n", "hello,\r\n\r\nworld,\"\"" };
        final String[][] res = { { "hello", "" }, { "" }, // Excel format does not ignore empty lines
                { "world", "" } };

        for (final String code : codes) {
            try (final CSVParser parser = CSVParser.parse(code, CSVFormat.EXCEL)) {
                final List<CSVRecord> records = parser.getRecords();
                assertEquals(res.length, records.size());
                assertTrue(records.size() > 0);
                for (int i = 0; i < res.length; i++) {
                    assertArrayEquals(res[i], records.get(i).values());
                }
            }
        }
    }

    @Test
    public void testExcelFormat1() throws IOException {
        final String code = "value1,value2,value3,value4\r\na,b,c,d\r\n  x,,," +
                "\r\n\r\n\"\"\"hello\"\"\",\"  \"\"world\"\"\",\"abc\ndef\",\r\n";
        final String[][] res = { { "value1", "value2", "value3", "value4" }, { "a", "b", "c", "d" },
                { "  x", "", "", "" }, { "" }, { "\"hello\"", "  \"world\"", "abc\ndef", "" } };
        try (final CSVParser parser = CSVParser.parse(code, CSVFormat.EXCEL)) {
            final List<CSVRecord> records = parser.getRecords();
            assertEquals(res.length, records.size());
            assertTrue(records.size() > 0);
            for (int i = 0; i < res.length; i++) {
                assertArrayEquals(res[i], records.get(i).values());
            }
        }
    }

    @Test
    public void testExcelFormat2() throws Exception {
        final String code = "foo,baar\r\n\r\nhello,\r\n\r\nworld,\r\n";
        final String[][] res = { { "foo", "baar" }, { "" }, { "hello", "" }, { "" }, { "world", "" } };
        try (final CSVParser parser = CSVParser.parse(code, CSVFormat.EXCEL)) {
            final List<CSVRecord> records = parser.getRecords();
            assertEquals(res.length, records.size());
            assertTrue(records.size() > 0);
            for (int i = 0; i < res.length; i++) {
                assertArrayEquals(res[i], records.get(i).values());
            }
        }
    }

    /**
     * Tests an exported Excel worksheet with a header row and rows that have more columns than the headers
     * @throws Exception
     */
    @Test
    public void testExcelHeaderCountLessThanData() throws Exception {
        final String code = "A,B,C,,\r\na,b,c,d,e\r\n";
        try (final CSVParser parser = CSVParser.parse(code, CSVFormat.EXCEL.withHeader())) {
            for (final CSVRecord record : parser.getRecords()) {
                assertEquals("a", record.get("A"));
                assertEquals("b", record.get("B"));
                assertEquals("c", record.get("C"));
            }
        }
    }

    @Test
    public void testFirstEndOfLineCr() throws IOException {
        final String data = "foo\rbaar,\rhello,world\r,kanu";
        try (final CSVParser parser = CSVParser.parse(data, CSVFormat.DEFAULT)) {
            final List<CSVRecord> records = parser.getRecords();
            assertEquals(4, records.size());
            assertEquals("\r", parser.getFirstEndOfLine());
        }
    }

    @Test
    public void testFirstEndOfLineCrLf() throws IOException {
        final String data = "foo\r\nbaar,\r\nhello,world\r\n,kanu";
        try (final CSVParser parser = CSVParser.parse(data, CSVFormat.DEFAULT)) {
            final List<CSVRecord> records = parser.getRecords();
            assertEquals(4, records.size());
            assertEquals("\r\n", parser.getFirstEndOfLine());
        }
    }

    @Test
    public void testFirstEndOfLineLf() throws IOException {
        final String data = "foo\nbaar,\nhello,world\n,kanu";
        try (final CSVParser parser = CSVParser.parse(data, CSVFormat.DEFAULT)) {
            final List<CSVRecord> records = parser.getRecords();
            assertEquals(4, records.size());
            assertEquals("\n", parser.getFirstEndOfLine());
        }
    }

    @Test
    public void testForEach() throws Exception {
        final List<CSVRecord> records = new ArrayList<>();
        try (final Reader in = new StringReader("a,b,c\n1,2,3\nx,y,z")) {
            for (final CSVRecord record : CSVFormat.DEFAULT.parse(in)) {
                records.add(record);
            }
            assertEquals(3, records.size());
            assertArrayEquals(new String[] { "a", "b", "c" }, records.get(0).values());
            assertArrayEquals(new String[] { "1", "2", "3" }, records.get(1).values());
            assertArrayEquals(new String[] { "x", "y", "z" }, records.get(2).values());
        }
    }

    @Test
    public void testGetHeaderMap() throws Exception {
        try (final CSVParser parser = CSVParser.parse("a,b,c\n1,2,3\nx,y,z",
                CSVFormat.DEFAULT.withHeader("A", "B", "C"))) {
            final Map<String, Integer> headerMap = parser.getHeaderMap();
            final Iterator<String> columnNames = headerMap.keySet().iterator();
            // Headers are iterated in column order.
            assertEquals("A", columnNames.next());
            assertEquals("B", columnNames.next());
            assertEquals("C", columnNames.next());
            final Iterator<CSVRecord> records = parser.iterator();

            // Parse to make sure getHeaderMap did not have a side-effect.
            for (int i = 0; i < 3; i++) {
                assertTrue(records.hasNext());
                final CSVRecord record = records.next();
                assertEquals(record.get(0), record.get("A"));
                assertEquals(record.get(1), record.get("B"));
                assertEquals(record.get(2), record.get("C"));
            }

            assertFalse(records.hasNext());
        }
    }

    @Test
    public void testGetHeaderNames() throws IOException {
        try (final CSVParser parser = CSVParser.parse("a,b,c\n1,2,3\nx,y,z",
            CSVFormat.DEFAULT.withHeader("A", "B", "C"))) {
            final Map<String, Integer> nameIndexMap = parser.getHeaderMap();
            final List<String> headerNames = parser.getHeaderNames();
            assertNotNull(headerNames);
            assertEquals(nameIndexMap.size(), headerNames.size());
            for (int i = 0; i < headerNames.size(); i++) {
                final String name = headerNames.get(i);
                assertEquals(i, nameIndexMap.get(name).intValue());
            }
        }
    }

    @Test
    public void testGetHeaderNamesReadOnly() throws IOException {
        try (final CSVParser parser = CSVParser.parse("a,b,c\n1,2,3\nx,y,z",
            CSVFormat.DEFAULT.withHeader("A", "B", "C"))) {
            final List<String> headerNames = parser.getHeaderNames();
            assertNotNull(headerNames);
            try {
                headerNames.add("This is a read-only list.");
                fail();
            } catch (final UnsupportedOperationException e) {
                // Yes.
            }
        }
    }

    @Test
    public void testGetLine() throws IOException {
        try (final CSVParser parser = CSVParser.parse(CSV_INPUT, CSVFormat.DEFAULT.withIgnoreSurroundingSpaces())) {
            for (final String[] re : RESULT) {
                assertArrayEquals(re, parser.nextRecord().values());
            }

            assertNull(parser.nextRecord());
        }
    }

    @Test
    public void testGetLineNumberWithCR() throws Exception {
        this.validateLineNumbers(String.valueOf(CR));
    }

    @Test
    public void testGetLineNumberWithCRLF() throws Exception {
        this.validateLineNumbers(CRLF);
    }

    @Test
    public void testGetLineNumberWithLF() throws Exception {
        this.validateLineNumbers(String.valueOf(LF));
    }

    @Test
    public void testGetOneLine() throws IOException {
        try (final CSVParser parser = CSVParser.parse(CSV_INPUT_1, CSVFormat.DEFAULT)) {
            final CSVRecord record = parser.getRecords().get(0);
            assertArrayEquals(RESULT[0], record.values());
        }
    }

    /**
     * Tests reusing a parser to process new string records one at a time as they are being discovered. See [CSV-110].
     *
     * @throws IOException
     */
    @Test
    public void testGetOneLineOneParser() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT;
        try (final PipedWriter writer = new PipedWriter();
                final CSVParser parser = new CSVParser(new PipedReader(writer), format)) {
            writer.append(CSV_INPUT_1);
            writer.append(format.getRecordSeparator());
            final CSVRecord record1 = parser.nextRecord();
            assertArrayEquals(RESULT[0], record1.values());
            writer.append(CSV_INPUT_2);
            writer.append(format.getRecordSeparator());
            final CSVRecord record2 = parser.nextRecord();
            assertArrayEquals(RESULT[1], record2.values());
        }
    }

    @Test
    public void testGetRecordNumberWithCR() throws Exception {
        this.validateRecordNumbers(String.valueOf(CR));
    }

    @Test
    public void testGetRecordNumberWithCRLF() throws Exception {
        this.validateRecordNumbers(CRLF);
    }

    @Test
    public void testGetRecordNumberWithLF() throws Exception {
        this.validateRecordNumbers(String.valueOf(LF));
    }

    @Test
    public void testGetRecordPositionWithCRLF() throws Exception {
        this.validateRecordPosition(CRLF);
    }

    @Test
    public void testGetRecordPositionWithLF() throws Exception {
        this.validateRecordPosition(String.valueOf(LF));
    }

    @Test
    public void testGetRecords() throws IOException {
        try (final CSVParser parser = CSVParser.parse(CSV_INPUT, CSVFormat.DEFAULT.withIgnoreSurroundingSpaces())) {
            final List<CSVRecord> records = parser.getRecords();
            assertEquals(RESULT.length, records.size());
            assertTrue(records.size() > 0);
            for (int i = 0; i < RESULT.length; i++) {
                assertArrayEquals(RESULT[i], records.get(i).values());
            }
        }
    }

    @Test
    public void testGetRecordWithMultiLineValues() throws Exception {
        try (final CSVParser parser = CSVParser.parse(
                "\"a\r\n1\",\"a\r\n2\"" + CRLF + "\"b\r\n1\",\"b\r\n2\"" + CRLF + "\"c\r\n1\",\"c\r\n2\"",
                CSVFormat.DEFAULT.withRecordSeparator(CRLF))) {
            CSVRecord record;
            assertEquals(0, parser.getRecordNumber());
            assertEquals(0, parser.getCurrentLineNumber());
            assertNotNull(record = parser.nextRecord());
            assertEquals(3, parser.getCurrentLineNumber());
            assertEquals(1, record.getRecordNumber());
            assertEquals(1, parser.getRecordNumber());
            assertNotNull(record = parser.nextRecord());
            assertEquals(6, parser.getCurrentLineNumber());
            assertEquals(2, record.getRecordNumber());
            assertEquals(2, parser.getRecordNumber());
            assertNotNull(record = parser.nextRecord());
            assertEquals(8, parser.getCurrentLineNumber());
            assertEquals(3, record.getRecordNumber());
            assertEquals(3, parser.getRecordNumber());
            assertNull(record = parser.nextRecord());
            assertEquals(8, parser.getCurrentLineNumber());
            assertEquals(3, parser.getRecordNumber());
        }
    }

    @Test
    public void testHeader() throws Exception {
        final Reader in = new StringReader("a,b,c\n1,2,3\nx,y,z");

        final Iterator<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in).iterator();

        for (int i = 0; i < 2; i++) {
            assertTrue(records.hasNext());
            final CSVRecord record = records.next();
            assertEquals(record.get(0), record.get("a"));
            assertEquals(record.get(1), record.get("b"));
            assertEquals(record.get(2), record.get("c"));
        }

        assertFalse(records.hasNext());
    }

    @Test
    public void testHeaderComment() throws Exception {
        final Reader in = new StringReader("# comment\na,b,c\n1,2,3\nx,y,z");

        final Iterator<CSVRecord> records = CSVFormat.DEFAULT.withCommentMarker('#').withHeader().parse(in).iterator();

        for (int i = 0; i < 2; i++) {
            assertTrue(records.hasNext());
            final CSVRecord record = records.next();
            assertEquals(record.get(0), record.get("a"));
            assertEquals(record.get(1), record.get("b"));
            assertEquals(record.get(2), record.get("c"));
        }

        assertFalse(records.hasNext());
    }

    @Test
    public void testHeaderMissing() throws Exception {
        final Reader in = new StringReader("a,,c\n1,2,3\nx,y,z");

        final Iterator<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in).iterator();

        for (int i = 0; i < 2; i++) {
            assertTrue(records.hasNext());
            final CSVRecord record = records.next();
            assertEquals(record.get(0), record.get("a"));
            assertEquals(record.get(2), record.get("c"));
        }

        assertFalse(records.hasNext());
    }

    @Test
    public void testHeaderMissingWithNull() throws Exception {
        final Reader in = new StringReader("a,,c,,d\n1,2,3,4\nx,y,z,zz");
        CSVFormat.DEFAULT.withHeader().withNullString("").withAllowMissingColumnNames().parse(in).iterator();
    }

    @Test
    public void testHeadersMissing() throws Exception {
        final Reader in = new StringReader("a,,c,,d\n1,2,3,4\nx,y,z,zz");
        CSVFormat.DEFAULT.withHeader().withAllowMissingColumnNames().parse(in).iterator();
    }

    @Test
    public void testHeadersMissingException() {
        final Reader in = new StringReader("a,,c,,d\n1,2,3,4\nx,y,z,zz");
        assertThrows(IllegalArgumentException.class, () -> CSVFormat.DEFAULT.withHeader().parse(in).iterator());
    }

    @Test
    public void testIgnoreCaseHeaderMapping() throws Exception {
        final Reader reader = new StringReader("1,2,3");
        final Iterator<CSVRecord> records = CSVFormat.DEFAULT.withHeader("One", "TWO", "three").withIgnoreHeaderCase()
                .parse(reader).iterator();
        final CSVRecord record = records.next();
        assertEquals("1", record.get("one"));
        assertEquals("2", record.get("two"));
        assertEquals("3", record.get("THREE"));
    }

    @Test
    public void testIgnoreEmptyLines() throws IOException {
        final String code = "\nfoo,baar\n\r\n,\n\n,world\r\n\n";
        // String code = "world\r\n\n";
        // String code = "foo;baar\r\n\r\nhello;\r\n\r\nworld;\r\n";
        try (final CSVParser parser = CSVParser.parse(code, CSVFormat.DEFAULT)) {
            final List<CSVRecord> records = parser.getRecords();
            assertEquals(3, records.size());
        }
    }

    @Test
    public void testInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> CSVFormat.DEFAULT.withDelimiter(CR));
    }

    @Test
    public void testIterator() throws Exception {
        final Reader in = new StringReader("a,b,c\n1,2,3\nx,y,z");

        final Iterator<CSVRecord> iterator = CSVFormat.DEFAULT.parse(in).iterator();

        assertTrue(iterator.hasNext());
        try {
            iterator.remove();
            fail("expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException expected) {
            // expected
        }
        assertArrayEquals(new String[] { "a", "b", "c" }, iterator.next().values());
        assertArrayEquals(new String[] { "1", "2", "3" }, iterator.next().values());
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertArrayEquals(new String[] { "x", "y", "z" }, iterator.next().values());
        assertFalse(iterator.hasNext());

        try {
            iterator.next();
            fail("NoSuchElementException expected");
        } catch (final NoSuchElementException e) {
            // expected
        }
    }

    @Test
    public void testIteratorSequenceBreaking() throws IOException {
        final String fiveRows = "1\n2\n3\n4\n5\n";

        // Iterator hasNext() shouldn't break sequence
        try (CSVParser parser = CSVFormat.DEFAULT.parse(new StringReader(fiveRows))) {
            int recordNumber = 0;
            final Iterator<CSVRecord> iter = parser.iterator();
            recordNumber = 0;
            while (iter.hasNext()) {
                final CSVRecord record = iter.next();
                recordNumber++;
                assertEquals(String.valueOf(recordNumber), record.get(0));
                if (recordNumber >= 2) {
                    break;
                }
            }
            iter.hasNext();
            while (iter.hasNext()) {
                final CSVRecord record = iter.next();
                recordNumber++;
                assertEquals(String.valueOf(recordNumber), record.get(0));
            }
        }

        // Consecutive enhanced for loops shouldn't break sequence
        try (CSVParser parser = CSVFormat.DEFAULT.parse(new StringReader(fiveRows))) {
            int recordNumber = 0;
            for (final CSVRecord record : parser) {
                recordNumber++;
                assertEquals(String.valueOf(recordNumber), record.get(0));
                if (recordNumber >= 2) {
                    break;
                }
            }
            for (final CSVRecord record : parser) {
                recordNumber++;
                assertEquals(String.valueOf(recordNumber), record.get(0));
            }
        }

        // Consecutive enhanced for loops with hasNext() peeking shouldn't break sequence
        try (CSVParser parser = CSVFormat.DEFAULT.parse(new StringReader(fiveRows))) {
            int recordNumber = 0;
            for (final CSVRecord record : parser) {
                recordNumber++;
                assertEquals(String.valueOf(recordNumber), record.get(0));
                if (recordNumber >= 2) {
                    break;
                }
            }
            parser.iterator().hasNext();
            for (final CSVRecord record : parser) {
                recordNumber++;
                assertEquals(String.valueOf(recordNumber), record.get(0));
            }
        }
    }

    @Test
    public void testLineFeedEndings() throws IOException {
        final String code = "foo\nbaar,\nhello,world\n,kanu";
        try (final CSVParser parser = CSVParser.parse(code, CSVFormat.DEFAULT)) {
            final List<CSVRecord> records = parser.getRecords();
            assertEquals(4, records.size());
        }
    }

    @Test
    public void testMappedButNotSetAsOutlook2007ContactExport() throws Exception {
        final Reader in = new StringReader("a,b,c\n1,2\nx,y,z");
        final Iterator<CSVRecord> records = CSVFormat.DEFAULT.withHeader("A", "B", "C").withSkipHeaderRecord().parse(in)
                .iterator();
        CSVRecord record;

        // 1st record
        record = records.next();
        assertTrue(record.isMapped("A"));
        assertTrue(record.isMapped("B"));
        assertTrue(record.isMapped("C"));
        assertTrue(record.isSet("A"));
        assertTrue(record.isSet("B"));
        assertFalse(record.isSet("C"));
        assertEquals("1", record.get("A"));
        assertEquals("2", record.get("B"));
        assertFalse(record.isConsistent());

        // 2nd record
        record = records.next();
        assertTrue(record.isMapped("A"));
        assertTrue(record.isMapped("B"));
        assertTrue(record.isMapped("C"));
        assertTrue(record.isSet("A"));
        assertTrue(record.isSet("B"));
        assertTrue(record.isSet("C"));
        assertEquals("x", record.get("A"));
        assertEquals("y", record.get("B"));
        assertEquals("z", record.get("C"));
        assertTrue(record.isConsistent());

        assertFalse(records.hasNext());
    }

    @Test
    @Disabled
    public void testMongoDbCsv() throws Exception {
        try (final CSVParser parser = CSVParser.parse("\"a a\",b,c" + LF + "d,e,f", CSVFormat.MONGODB_CSV)) {
            final Iterator<CSVRecord> itr1 = parser.iterator();
            final Iterator<CSVRecord> itr2 = parser.iterator();

            final CSVRecord first = itr1.next();
            assertEquals("a a", first.get(0));
            assertEquals("b", first.get(1));
            assertEquals("c", first.get(2));

            final CSVRecord second = itr2.next();
            assertEquals("d", second.get(0));
            assertEquals("e", second.get(1));
            assertEquals("f", second.get(2));
        }
    }

    @Test
    // TODO this may lead to strange behavior, throw an exception if iterator() has already been called?
    public void testMultipleIterators() throws Exception {
        try (final CSVParser parser = CSVParser.parse("a,b,c" + CRLF + "d,e,f", CSVFormat.DEFAULT)) {
            final Iterator<CSVRecord> itr1 = parser.iterator();

            final CSVRecord first = itr1.next();
            assertEquals("a", first.get(0));
            assertEquals("b", first.get(1));
            assertEquals("c", first.get(2));

            final CSVRecord second = itr1.next();
            assertEquals("d", second.get(0));
            assertEquals("e", second.get(1));
            assertEquals("f", second.get(2));
        }
    }

    @Test
    public void testNewCSVParserNullReaderFormat() {
        assertThrows(IllegalArgumentException.class, () -> new CSVParser(null, CSVFormat.DEFAULT));
    }

    @Test
    public void testNewCSVParserReaderNullFormat() {
        assertThrows(IllegalArgumentException.class, () -> new CSVParser(new StringReader(""), null));
    }

    @Test
    public void testNoHeaderMap() throws Exception {
        try (final CSVParser parser = CSVParser.parse("a,b,c\n1,2,3\nx,y,z", CSVFormat.DEFAULT)) {
            assertNull(parser.getHeaderMap());
        }
    }

    @Test
    public void testParse() throws Exception {
        final ClassLoader loader = ClassLoader.getSystemClassLoader();
        final URL url = loader.getResource("CSVFileParser/test.csv");
        final CSVFormat format = CSVFormat.DEFAULT.withHeader("A", "B", "C", "D");
        final Charset charset = StandardCharsets.UTF_8;

        try(final CSVParser parser = CSVParser.parse(new InputStreamReader(url.openStream(), charset), format)) {
            parseFully(parser);
        }
        try(final CSVParser parser = CSVParser.parse(new String(Files.readAllBytes(Paths.get(url.toURI())), charset), format)) {
            parseFully(parser);
        }
        try(final CSVParser parser = CSVParser.parse(new File(url.toURI()), charset, format)) {
            parseFully(parser);
        }
        try(final CSVParser parser = CSVParser.parse(url.openStream(), charset, format)) {
            parseFully(parser);
        }
        try(final CSVParser parser = CSVParser.parse(Paths.get(url.toURI()), charset, format)) {
            parseFully(parser);
        }
        try(final CSVParser parser = CSVParser.parse(url, charset, format)) {
            parseFully(parser);
        }
        try(final CSVParser parser = new CSVParser(new InputStreamReader(url.openStream(), charset), format)) {
            parseFully(parser);
        }
        try(final CSVParser parser = new CSVParser(new InputStreamReader(url.openStream(), charset), format, /*characterOffset=*/0, /*recordNumber=*/1)) {
            parseFully(parser);
        }
    }

    @Test
    public void testParseFileNullFormat() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CSVParser.parse(new File("CSVFileParser/test.csv"), Charset.defaultCharset(), null));
    }

    @Test
    public void testParseNullFileFormat() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CSVParser.parse((File) null, Charset.defaultCharset(), CSVFormat.DEFAULT));
    }

    @Test
    public void testParseNullPathFormat() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CSVParser.parse((Path) null, Charset.defaultCharset(), CSVFormat.DEFAULT));
    }

    @Test
    public void testParseNullStringFormat() {
        assertThrows(IllegalArgumentException.class, () -> CSVParser.parse((String) null, CSVFormat.DEFAULT));
    }

    @Test
    public void testParseNullUrlCharsetFormat() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CSVParser.parse((URL) null, Charset.defaultCharset(), CSVFormat.DEFAULT));
    }

    @Test
    public void testParserUrlNullCharsetFormat() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CSVParser.parse(new URL("https://commons.apache.org"), null, CSVFormat.DEFAULT));
    }

    @Test
    public void testParseStringNullFormat() {
        assertThrows(IllegalArgumentException.class, () -> CSVParser.parse("csv data", (CSVFormat) null));
    }

    @Test
    public void testParseUrlCharsetNullFormat() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CSVParser.parse(new URL("https://commons.apache.org"), Charset.defaultCharset(), null));
    }

    @Test
    public void testProvidedHeader() throws Exception {
        final Reader in = new StringReader("a,b,c\n1,2,3\nx,y,z");

        final Iterator<CSVRecord> records = CSVFormat.DEFAULT.withHeader("A", "B", "C").parse(in).iterator();

        for (int i = 0; i < 3; i++) {
            assertTrue(records.hasNext());
            final CSVRecord record = records.next();
            assertTrue(record.isMapped("A"));
            assertTrue(record.isMapped("B"));
            assertTrue(record.isMapped("C"));
            assertFalse(record.isMapped("NOT MAPPED"));
            assertEquals(record.get(0), record.get("A"));
            assertEquals(record.get(1), record.get("B"));
            assertEquals(record.get(2), record.get("C"));
        }

        assertFalse(records.hasNext());
    }

    @Test
    public void testProvidedHeaderAuto() throws Exception {
        final Reader in = new StringReader("a,b,c\n1,2,3\nx,y,z");

        final Iterator<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in).iterator();

        for (int i = 0; i < 2; i++) {
            assertTrue(records.hasNext());
            final CSVRecord record = records.next();
            assertTrue(record.isMapped("a"));
            assertTrue(record.isMapped("b"));
            assertTrue(record.isMapped("c"));
            assertFalse(record.isMapped("NOT MAPPED"));
            assertEquals(record.get(0), record.get("a"));
            assertEquals(record.get(1), record.get("b"));
            assertEquals(record.get(2), record.get("c"));
        }

        assertFalse(records.hasNext());
    }

    @Test
    public void testRoundtrip() throws Exception {
        final StringWriter out = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT)) {
            final String input = "a,b,c\r\n1,2,3\r\nx,y,z\r\n";
            for (final CSVRecord record : CSVParser.parse(input, CSVFormat.DEFAULT)) {
                printer.printRecord(record);
            }
            assertEquals(input, out.toString());
        }
    }

    @Test
    public void testSkipAutoHeader() throws Exception {
        final Reader in = new StringReader("a,b,c\n1,2,3\nx,y,z");
        final Iterator<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in).iterator();
        final CSVRecord record = records.next();
        assertEquals("1", record.get("a"));
        assertEquals("2", record.get("b"));
        assertEquals("3", record.get("c"));
    }

    @Test
    public void testSkipHeaderOverrideDuplicateHeaders() throws Exception {
        final Reader in = new StringReader("a,a,a\n1,2,3\nx,y,z");
        final Iterator<CSVRecord> records = CSVFormat.DEFAULT.withHeader("X", "Y", "Z").withSkipHeaderRecord().parse(in)
                .iterator();
        final CSVRecord record = records.next();
        assertEquals("1", record.get("X"));
        assertEquals("2", record.get("Y"));
        assertEquals("3", record.get("Z"));
    }

    @Test
    public void testSkipSetAltHeaders() throws Exception {
        final Reader in = new StringReader("a,b,c\n1,2,3\nx,y,z");
        final Iterator<CSVRecord> records = CSVFormat.DEFAULT.withHeader("X", "Y", "Z").withSkipHeaderRecord().parse(in)
                .iterator();
        final CSVRecord record = records.next();
        assertEquals("1", record.get("X"));
        assertEquals("2", record.get("Y"));
        assertEquals("3", record.get("Z"));
    }

    @Test
    public void testSkipSetHeader() throws Exception {
        final Reader in = new StringReader("a,b,c\n1,2,3\nx,y,z");
        final Iterator<CSVRecord> records = CSVFormat.DEFAULT.withHeader("a", "b", "c").withSkipHeaderRecord().parse(in)
                .iterator();
        final CSVRecord record = records.next();
        assertEquals("1", record.get("a"));
        assertEquals("2", record.get("b"));
        assertEquals("3", record.get("c"));
    }

    @Test
    @Disabled
    public void testStartWithEmptyLinesThenHeaders() throws Exception {
        final String[] codes = { "\r\n\r\n\r\nhello,\r\n\r\n\r\n", "hello,\n\n\n", "hello,\"\"\r\n\r\n\r\n",
                "hello,\"\"\n\n\n" };
        final String[][] res = { { "hello", "" }, { "" }, // Excel format does not ignore empty lines
                { "" } };
        for (final String code : codes) {
            try (final CSVParser parser = CSVParser.parse(code, CSVFormat.EXCEL)) {
                final List<CSVRecord> records = parser.getRecords();
                assertEquals(res.length, records.size());
                assertTrue(records.size() > 0);
                for (int i = 0; i < res.length; i++) {
                    assertArrayEquals(res[i], records.get(i).values());
                }
            }
        }
    }

    @Test
    public void testTrailingDelimiter() throws Exception {
        final Reader in = new StringReader("a,a,a,\n\"1\",\"2\",\"3\",\nx,y,z,");
        final Iterator<CSVRecord> records = CSVFormat.DEFAULT.withHeader("X", "Y", "Z").withSkipHeaderRecord()
                .withTrailingDelimiter().parse(in).iterator();
        final CSVRecord record = records.next();
        assertEquals("1", record.get("X"));
        assertEquals("2", record.get("Y"));
        assertEquals("3", record.get("Z"));
        assertEquals(3, record.size());
    }

    @Test
    public void testTrim() throws Exception {
        final Reader in = new StringReader("a,a,a\n\" 1 \",\" 2 \",\" 3 \"\nx,y,z");
        final Iterator<CSVRecord> records = CSVFormat.DEFAULT.withHeader("X", "Y", "Z").withSkipHeaderRecord()
                .withTrim().parse(in).iterator();
        final CSVRecord record = records.next();
        assertEquals("1", record.get("X"));
        assertEquals("2", record.get("Y"));
        assertEquals("3", record.get("Z"));
        assertEquals(3, record.size());
    }

    @Test
    public void testRepeatedHeadersAreReturnedInCSVRecordHeaderNames() throws IOException {
        final Reader in = new StringReader("header1,header2,header1\n1,2,3\n4,5,6");
        final Iterator<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim().parse(in).iterator();
        final CSVRecord record = records.next();
        assertEquals(Arrays.asList("header1", "header2", "header1"), record.getParser().getHeaderNames());
    }

    @Test
    public void testCSV235() throws IOException {
        final String dqString = "\"aaa\",\"b\"\"bb\",\"ccc\""; // "aaa","b""bb","ccc"
        final Iterator<CSVRecord> records = CSVFormat.RFC4180.parse(new StringReader(dqString)).iterator();
        final CSVRecord record = records.next();
        assertFalse(records.hasNext());
        assertEquals(3, record.size());
        assertEquals("aaa", record.get(0));
        assertEquals("b\"bb", record.get(1));
        assertEquals("ccc", record.get(2));
    }

    private void validateLineNumbers(final String lineSeparator) throws IOException {
        try (final CSVParser parser = CSVParser.parse("a" + lineSeparator + "b" + lineSeparator + "c",
                CSVFormat.DEFAULT.withRecordSeparator(lineSeparator))) {
            assertEquals(0, parser.getCurrentLineNumber());
            assertNotNull(parser.nextRecord());
            assertEquals(1, parser.getCurrentLineNumber());
            assertNotNull(parser.nextRecord());
            assertEquals(2, parser.getCurrentLineNumber());
            assertNotNull(parser.nextRecord());
            // Still 2 because the last line is does not have EOL chars
            assertEquals(2, parser.getCurrentLineNumber());
            assertNull(parser.nextRecord());
            // Still 2 because the last line is does not have EOL chars
            assertEquals(2, parser.getCurrentLineNumber());
        }
    }

    private void validateRecordNumbers(final String lineSeparator) throws IOException {
        try (final CSVParser parser = CSVParser.parse("a" + lineSeparator + "b" + lineSeparator + "c",
                CSVFormat.DEFAULT.withRecordSeparator(lineSeparator))) {
            CSVRecord record;
            assertEquals(0, parser.getRecordNumber());
            assertNotNull(record = parser.nextRecord());
            assertEquals(1, record.getRecordNumber());
            assertEquals(1, parser.getRecordNumber());
            assertNotNull(record = parser.nextRecord());
            assertEquals(2, record.getRecordNumber());
            assertEquals(2, parser.getRecordNumber());
            assertNotNull(record = parser.nextRecord());
            assertEquals(3, record.getRecordNumber());
            assertEquals(3, parser.getRecordNumber());
            assertNull(record = parser.nextRecord());
            assertEquals(3, parser.getRecordNumber());
        }
    }

    private void validateRecordPosition(final String lineSeparator) throws IOException {
        final String nl = lineSeparator; // used as linebreak in values for better distinction

        final String code = "a,b,c" + lineSeparator + "1,2,3" + lineSeparator +
                // to see if recordPosition correctly points to the enclosing quote
                "'A" + nl + "A','B" + nl + "B',CC" + lineSeparator +
                // unicode test... not very relevant while operating on strings instead of bytes, but for
                // completeness...
                "\u00c4,\u00d6,\u00dc" + lineSeparator + "EOF,EOF,EOF";

        final CSVFormat format = CSVFormat.newFormat(',').withQuote('\'').withRecordSeparator(lineSeparator);
        CSVParser parser = CSVParser.parse(code, format);

        CSVRecord record;
        assertEquals(0, parser.getRecordNumber());

        assertNotNull(record = parser.nextRecord());
        assertEquals(1, record.getRecordNumber());
        assertEquals(code.indexOf('a'), record.getCharacterPosition());

        assertNotNull(record = parser.nextRecord());
        assertEquals(2, record.getRecordNumber());
        assertEquals(code.indexOf('1'), record.getCharacterPosition());

        assertNotNull(record = parser.nextRecord());
        final long positionRecord3 = record.getCharacterPosition();
        assertEquals(3, record.getRecordNumber());
        assertEquals(code.indexOf("'A"), record.getCharacterPosition());
        assertEquals("A" + lineSeparator + "A", record.get(0));
        assertEquals("B" + lineSeparator + "B", record.get(1));
        assertEquals("CC", record.get(2));

        assertNotNull(record = parser.nextRecord());
        assertEquals(4, record.getRecordNumber());
        assertEquals(code.indexOf('\u00c4'), record.getCharacterPosition());

        assertNotNull(record = parser.nextRecord());
        assertEquals(5, record.getRecordNumber());
        assertEquals(code.indexOf("EOF"), record.getCharacterPosition());

        parser.close();

        // now try to read starting at record 3
        parser = new CSVParser(new StringReader(code.substring((int) positionRecord3)), format, positionRecord3, 3);

        assertNotNull(record = parser.nextRecord());
        assertEquals(3, record.getRecordNumber());
        assertEquals(code.indexOf("'A"), record.getCharacterPosition());
        assertEquals("A" + lineSeparator + "A", record.get(0));
        assertEquals("B" + lineSeparator + "B", record.get(1));
        assertEquals("CC", record.get(2));

        assertNotNull(record = parser.nextRecord());
        assertEquals(4, record.getRecordNumber());
        assertEquals(code.indexOf('\u00c4'), record.getCharacterPosition());
        assertEquals("\u00c4", record.get(0));

        parser.close();
    }
}
