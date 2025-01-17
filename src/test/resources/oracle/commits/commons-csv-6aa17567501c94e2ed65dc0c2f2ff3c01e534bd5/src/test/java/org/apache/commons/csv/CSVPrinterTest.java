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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.h2.tools.SimpleResultSet;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
public class CSVPrinterTest {

    private static final char EURO_CH = '\u20AC';
    private static final char DQUOTE_CHAR = '"';
    private static final char BACKSLASH_CH = '\\';
    private static final char QUOTE_CH = '\'';
    private static final int ITERATIONS_FOR_RANDOM_TEST = 50000;

    private static String printable(final String s) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            final char ch = s.charAt(i);
            if (ch <= ' ' || ch >= 128) {
                sb.append("(").append((int) ch).append(")");
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private final String recordSeparator = CSVFormat.DEFAULT.getRecordSeparator();
    private String longText2;

    private void doOneRandom(final CSVFormat format) throws Exception {
        final Random r = new Random();

        final int nLines = r.nextInt(4) + 1;
        final int nCol = r.nextInt(3) + 1;
        // nLines=1;nCol=2;
        final String[][] lines = generateLines(nLines, nCol);

        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, format)) {

            for (int i = 0; i < nLines; i++) {
                // for (int j=0; j<lines[i].length; j++) System.out.println("### VALUE=:" + printable(lines[i][j]));
                printer.printRecord((Object[]) lines[i]);
            }

            printer.flush();
        }
        final String result = sw.toString();
        // System.out.println("### :" + printable(result));

        try (final CSVParser parser = CSVParser.parse(result, format)) {
            final List<CSVRecord> parseResult = parser.getRecords();

            final String[][] expected = lines.clone();
            for (int i = 0; i < expected.length; i++) {
                expected[i] = expectNulls(expected[i], format);
            }
            Utils.compare("Printer output :" + printable(result), expected, parseResult);
        }
    }

    private void doRandom(final CSVFormat format, final int iter) throws Exception {
        for (int i = 0; i < iter; i++) {
            doOneRandom(format);
        }
    }

    /**
     * Converts an input CSV array into expected output values WRT NULLs. NULL strings are converted to null values
     * because the parser will convert these strings to null.
     */
    private <T> T[] expectNulls(final T[] original, final CSVFormat csvFormat) {
        final T[] fixed = original.clone();
        for (int i = 0; i < fixed.length; i++) {
            if (Objects.equals(csvFormat.getNullString(), fixed[i])) {
                fixed[i] = null;
            }
        }
        return fixed;
    }

    private Connection geH2Connection() throws SQLException, ClassNotFoundException {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection("jdbc:h2:mem:my_test;", "sa", "");
    }

    private String[][] generateLines(final int nLines, final int nCol) {
        final String[][] lines = new String[nLines][];
        for (int i = 0; i < nLines; i++) {
            final String[] line = new String[nCol];
            lines[i] = line;
            for (int j = 0; j < nCol; j++) {
                line[j] = randStr();
            }
        }
        return lines;
    }

    private CSVPrinter printWithHeaderComments(final StringWriter sw, final Date now, final CSVFormat baseFormat)
            throws IOException {
        CSVFormat format = baseFormat;
        // Use withHeaderComments first to test CSV-145
        format = format.withHeaderComments("Generated by Apache Commons CSV 1.1", now);
        format = format.withCommentMarker('#');
        format = format.withHeader("Col1", "Col2");
        final CSVPrinter csvPrinter = format.print(sw);
        csvPrinter.printRecord("A", "B");
        csvPrinter.printRecord("C", "D");
        csvPrinter.close();
        return csvPrinter;
    }

    private String randStr() {
        final Random r = new Random();

        final int sz = r.nextInt(20);
        // sz = r.nextInt(3);
        final char[] buf = new char[sz];
        for (int i = 0; i < sz; i++) {
            // stick in special chars with greater frequency
            char ch;
            final int what = r.nextInt(20);
            switch (what) {
            case 0:
                ch = '\r';
                break;
            case 1:
                ch = '\n';
                break;
            case 2:
                ch = '\t';
                break;
            case 3:
                ch = '\f';
                break;
            case 4:
                ch = ' ';
                break;
            case 5:
                ch = ',';
                break;
            case 6:
                ch = DQUOTE_CHAR;
                break;
            case 7:
                ch = '\'';
                break;
            case 8:
                ch = BACKSLASH_CH;
                break;
            default:
                ch = (char) r.nextInt(300);
                break;
            // default: ch = 'a'; break;
            }
            buf[i] = ch;
        }
        return new String(buf);
    }

    private void setUpTable(final Connection connection) throws SQLException {
        try (final Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE TEST(ID INT PRIMARY KEY, NAME VARCHAR(255), TEXT CLOB)");
            statement.execute("insert into TEST values(1, 'r1', 'long text 1')");
            longText2 = StringUtils.repeat('a', IOUtils.DEFAULT_BUFFER_SIZE - 4);
            longText2 += "\"\r\n\"a\"";
            longText2 += StringUtils.repeat('a', IOUtils.DEFAULT_BUFFER_SIZE - 1);
            statement.execute("insert into TEST values(2, 'r2', '" + longText2 + "')");
            longText2 = longText2.replace("\"","\"\"");
        }
    }

    @Test
    public void testCloseBackwardCompatibility() throws IOException {
        try (final Writer writer = mock(Writer.class)) {
            final CSVFormat csvFormat = CSVFormat.DEFAULT;
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                // empty
            }
            verify(writer, never()).flush();
            verify(writer, times(1)).close();
        }}

    @Test
    public void testCloseWithCsvFormatAutoFlushOff() throws IOException {
        try (final Writer writer = mock(Writer.class)) {
            final CSVFormat csvFormat = CSVFormat.DEFAULT.withAutoFlush(false);
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                // empty
            }
            verify(writer, never()).flush();
            verify(writer, times(1)).close();
        }
    }

    @Test
    public void testCloseWithCsvFormatAutoFlushOn() throws IOException {
        // System.out.println("start method");
        try (final Writer writer = mock(Writer.class)) {
            final CSVFormat csvFormat = CSVFormat.DEFAULT.withAutoFlush(true);
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                // empty
            }
            verify(writer, times(1)).flush();
            verify(writer, times(1)).close();
        }}

    @Test
    public void testCloseWithFlushOff() throws IOException {
        try (final Writer writer = mock(Writer.class)) {
            final CSVFormat csvFormat = CSVFormat.DEFAULT;
            @SuppressWarnings("resource")
            final CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);
            csvPrinter.close(false);
            verify(writer, never()).flush();
            verify(writer, times(1)).close();
        }
    }

    @Test
    public void testCloseWithFlushOn() throws IOException {
        try (final Writer writer = mock(Writer.class)) {
            final CSVFormat csvFormat = CSVFormat.DEFAULT;
            @SuppressWarnings("resource")
            final CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);
            csvPrinter.close(true);
            verify(writer, times(1)).flush();
        }
    }

    @Test
    public void testCSV135() throws IOException {
        List<String> l = new LinkedList<String>();
        l.add("\"\"");   // ""
        l.add("\\\\");   // \\
        l.add("\\\"\\"); // \"\
        tryFormat(l, null, null, "\"\",\\\\,\\\"\\"); // "",\\,\"\ (unchanged)
        tryFormat(l, '"',  null, "\"\"\"\"\"\",\\\\,\"\\\"\"\\\"");              // """""",\\,"\""\" (quoted, and embedded DQ doubled)
        tryFormat(l, null, '\\', "\"\",\\\\\\\\,\\\\\"\\\\");                    // "",\\\\,\\"\\ (escapes escaped, not quoted)
        tryFormat(l, '"',  '\\', "\"\\\"\\\"\",\"\\\\\\\\\",\"\\\\\\\"\\\\\"");  // "\"\"","\\\\","\\\"\\" (quoted, and embedded DQ & escape escaped)
        tryFormat(l, '"',  '"',  "\"\"\"\"\"\",\\\\,\"\\\"\"\\\"");              // """""",\\,"\""\" (quoted, embedded DQ escaped)
    }

    @Test
    public void testDelimeterQuoted() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuote('\''))) {
            printer.print("a,b,c");
            printer.print("xyz");
            assertEquals("'a,b,c',xyz", sw.toString());
        }
    }

    @Test
    public void testDelimeterQuoteNone() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVFormat format = CSVFormat.DEFAULT.withEscape('!').withQuoteMode(QuoteMode.NONE);
        try (final CSVPrinter printer = new CSVPrinter(sw, format)) {
            printer.print("a,b,c");
            printer.print("xyz");
            assertEquals("a!,b!,c,xyz", sw.toString());
        }
    }

    @Test
    public void testDelimiterEscaped() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withEscape('!').withQuote(null))) {
            printer.print("a,b,c");
            printer.print("xyz");
            assertEquals("a!,b!,c,xyz", sw.toString());
        }
    }

    @Test
    public void testDelimiterPlain() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuote(null))) {
            printer.print("a,b,c");
            printer.print("xyz");
            assertEquals("a,b,c,xyz", sw.toString());
        }
    }

    @Test
    public void testDisabledComment() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            printer.printComment("This is a comment");
            assertEquals("", sw.toString());
        }
    }

    @Test
    public void testDontQuoteEuroFirstChar() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.RFC4180)) {
            printer.printRecord(EURO_CH, "Deux");
            assertEquals(EURO_CH + ",Deux" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testEolEscaped() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuote(null).withEscape('!'))) {
            printer.print("a\rb\nc");
            printer.print("x\fy\bz");
            assertEquals("a!rb!nc,x\fy\bz", sw.toString());
        }
    }

    @Test
    public void testEolPlain() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuote(null))) {
            printer.print("a\rb\nc");
            printer.print("x\fy\bz");
            assertEquals("a\rb\nc,x\fy\bz", sw.toString());
        }
    }

    @Test
    public void testEolQuoted() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuote('\''))) {
            printer.print("a\rb\nc");
            printer.print("x\by\fz");
            assertEquals("'a\rb\nc',x\by\fz", sw.toString());
        }
    }

    @Test
    public void testEscapeBackslash1() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuote(QUOTE_CH))) {
            printer.print("\\");
        }
        assertEquals("\\", sw.toString());
    }

    @Test
    public void testEscapeBackslash2() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuote(QUOTE_CH))) {
            printer.print("\\\r");
        }
        assertEquals("'\\\r'", sw.toString());
    }

    @Test
    public void testEscapeBackslash3() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuote(QUOTE_CH))) {
            printer.print("X\\\r");
        }
        assertEquals("'X\\\r'", sw.toString());
    }

    @Test
    public void testEscapeBackslash4() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuote(QUOTE_CH))) {
            printer.print("\\\\");
        }
        assertEquals("\\\\", sw.toString());
    }

    @Test
    public void testEscapeBackslash5() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuote(QUOTE_CH))) {
            printer.print("\\\\");
        }
        assertEquals("\\\\", sw.toString());
    }

    @Test
    public void testEscapeNull1() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withEscape(null))) {
            printer.print("\\");
        }
        assertEquals("\\", sw.toString());
    }

    @Test
    public void testEscapeNull2() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withEscape(null))) {
            printer.print("\\\r");
        }
        assertEquals("\"\\\r\"", sw.toString());
    }

    @Test
    public void testEscapeNull3() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withEscape(null))) {
            printer.print("X\\\r");
        }
        assertEquals("\"X\\\r\"", sw.toString());
    }

    @Test
    public void testEscapeNull4() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withEscape(null))) {
            printer.print("\\\\");
        }
        assertEquals("\\\\", sw.toString());
    }

    @Test
    public void testEscapeNull5() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withEscape(null))) {
            printer.print("\\\\");
        }
        assertEquals("\\\\", sw.toString());
    }

    @Test
    public void testExcelPrintAllArrayOfArrays() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.EXCEL)) {
            printer.printRecords((Object[]) new String[][] { { "r1c1", "r1c2" }, { "r2c1", "r2c2" } });
            assertEquals("r1c1,r1c2" + recordSeparator + "r2c1,r2c2" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testExcelPrintAllArrayOfLists() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.EXCEL)) {
            printer.printRecords(
                    (Object[]) new List[] { Arrays.asList("r1c1", "r1c2"), Arrays.asList("r2c1", "r2c2") });
            assertEquals("r1c1,r1c2" + recordSeparator + "r2c1,r2c2" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testExcelPrintAllIterableOfArrays() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.EXCEL)) {
            printer.printRecords(Arrays.asList(new String[][] { { "r1c1", "r1c2" }, { "r2c1", "r2c2" } }));
            assertEquals("r1c1,r1c2" + recordSeparator + "r2c1,r2c2" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testExcelPrintAllIterableOfLists() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.EXCEL)) {
            printer.printRecords(
                    Arrays.asList(new List[] { Arrays.asList("r1c1", "r1c2"), Arrays.asList("r2c1", "r2c2") }));
            assertEquals("r1c1,r1c2" + recordSeparator + "r2c1,r2c2" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testExcelPrinter1() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.EXCEL)) {
            printer.printRecord("a", "b");
            assertEquals("a,b" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testExcelPrinter2() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.EXCEL)) {
            printer.printRecord("a,b", "b");
            assertEquals("\"a,b\",b" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testHeader() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw,
                CSVFormat.DEFAULT.withQuote(null).withHeader("C1", "C2", "C3"))) {
            printer.printRecord("a", "b", "c");
            printer.printRecord("x", "y", "z");
            assertEquals("C1,C2,C3\r\na,b,c\r\nx,y,z\r\n", sw.toString());
        }
    }

    @Test
    public void testHeaderCommentExcel() throws IOException {
        final StringWriter sw = new StringWriter();
        final Date now = new Date();
        final CSVFormat format = CSVFormat.EXCEL;
        try (final CSVPrinter csvPrinter = printWithHeaderComments(sw, now, format)) {
            assertEquals("# Generated by Apache Commons CSV 1.1\r\n# " + now + "\r\nCol1,Col2\r\nA,B\r\nC,D\r\n",
                    sw.toString());
        }
    }

    @Test
    public void testHeaderCommentTdf() throws IOException {
        final StringWriter sw = new StringWriter();
        final Date now = new Date();
        final CSVFormat format = CSVFormat.TDF;
        try (final CSVPrinter csvPrinter = printWithHeaderComments(sw, now, format)) {
            assertEquals("# Generated by Apache Commons CSV 1.1\r\n# " + now + "\r\nCol1\tCol2\r\nA\tB\r\nC\tD\r\n",
                    sw.toString());
        }
    }

    @Test
    public void testHeaderNotSet() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuote(null))) {
            printer.printRecord("a", "b", "c");
            printer.printRecord("x", "y", "z");
            assertEquals("a,b,c\r\nx,y,z\r\n", sw.toString());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFormat() throws Exception {
        final CSVFormat invalidFormat = CSVFormat.DEFAULT.withDelimiter(CR);
        try (final CSVPrinter printer = new CSVPrinter(new StringWriter(), invalidFormat)) {
            Assert.fail("This test should have thrown an exception.");
        }
    }

    @Test
    public void testJdbcPrinter() throws IOException, ClassNotFoundException, SQLException {
        final StringWriter sw = new StringWriter();
        try (final Connection connection = geH2Connection()) {
            setUpTable(connection);
            try (final Statement stmt = connection.createStatement();
                    final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
                printer.printRecords(stmt.executeQuery("select ID, NAME, TEXT from TEST"));
            }
        }
        assertEquals("1,r1,\"long text 1\"" + recordSeparator + "2,r2,\"" + longText2 + "\"" + recordSeparator,
                sw.toString());
    }

    @Test
    public void testJdbcPrinterWithResultSet() throws IOException, ClassNotFoundException, SQLException {
        final StringWriter sw = new StringWriter();
        Class.forName("org.h2.Driver");
        try (final Connection connection = geH2Connection();) {
            setUpTable(connection);
            try (final Statement stmt = connection.createStatement();
                    final ResultSet resultSet = stmt.executeQuery("select ID, NAME, TEXT from TEST");
                    final CSVPrinter printer = CSVFormat.DEFAULT.withHeader(resultSet).print(sw)) {
                printer.printRecords(resultSet);
            }
        }
        assertEquals("ID,NAME,TEXT" + recordSeparator + "1,r1,\"long text 1\"" + recordSeparator + "2,r2,\"" + longText2
                + "\"" + recordSeparator, sw.toString());
    }

    @Test
    public void testJdbcPrinterWithResultSetMetaData() throws IOException, ClassNotFoundException, SQLException {
        final StringWriter sw = new StringWriter();
        Class.forName("org.h2.Driver");
        try (final Connection connection = geH2Connection()) {
            setUpTable(connection);
            try (final Statement stmt = connection.createStatement();
                    final ResultSet resultSet = stmt.executeQuery("select ID, NAME, TEXT from TEST");
                    final CSVPrinter printer = CSVFormat.DEFAULT.withHeader(resultSet.getMetaData()).print(sw)) {
                printer.printRecords(resultSet);
                assertEquals("ID,NAME,TEXT" + recordSeparator + "1,r1,\"long text 1\"" + recordSeparator + "2,r2,\""
                        + longText2 + "\"" + recordSeparator, sw.toString());
            }
        }
    }

    @Test
    @Ignore
    public void testJira135_part1() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withRecordSeparator('\n').withQuote(DQUOTE_CHAR).withEscape(BACKSLASH_CH);
        final StringWriter sw = new StringWriter();
        final List<String> list = new LinkedList<>();
        try (final CSVPrinter printer = new CSVPrinter(sw, format)) {
            list.add("\"");
            printer.printRecord(list);
        }
        final String expected = "\"\\\"\"" + format.getRecordSeparator();
        assertEquals(expected, sw.toString());
        final String[] record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(list.toArray(), format), record0);
    }

    @Test
    @Ignore
    public void testJira135_part2() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withRecordSeparator('\n').withQuote(DQUOTE_CHAR).withEscape(BACKSLASH_CH);
        final StringWriter sw = new StringWriter();
        final List<String> list = new LinkedList<>();
        try (final CSVPrinter printer = new CSVPrinter(sw, format)) {
            list.add("\n");
            printer.printRecord(list);
        }
        final String expected = "\"\\n\"" + format.getRecordSeparator();
        assertEquals(expected, sw.toString());
        final String[] record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(list.toArray(), format), record0);
    }

    @Test
    @Ignore
    public void testJira135_part3() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withRecordSeparator('\n').withQuote(DQUOTE_CHAR).withEscape(BACKSLASH_CH);
        final StringWriter sw = new StringWriter();
        final List<String> list = new LinkedList<>();
        try (final CSVPrinter printer = new CSVPrinter(sw, format)) {
            list.add("\\");
            printer.printRecord(list);
        }
        final String expected = "\"\\\\\"" + format.getRecordSeparator();
        assertEquals(expected, sw.toString());
        final String[] record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(list.toArray(), format), record0);
    }

    @Test
    @Ignore
    public void testJira135All() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withRecordSeparator('\n').withQuote(DQUOTE_CHAR).withEscape(BACKSLASH_CH);
        final StringWriter sw = new StringWriter();
        final List<String> list = new LinkedList<>();
        try (final CSVPrinter printer = new CSVPrinter(sw, format)) {
            list.add("\"");
            list.add("\n");
            list.add("\\");
            printer.printRecord(list);
        }
        final String expected = "\"\\\"\",\"\\n\",\"\\\"" + format.getRecordSeparator();
        assertEquals(expected, sw.toString());
        final String[] record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(list.toArray(), format), record0);
    }

    @Test
    public void testMongoDbCsvBasic() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.MONGODB_CSV)) {
            printer.printRecord("a", "b");
            assertEquals("a,b" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testMongoDbCsvCommaInValue() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.MONGODB_CSV)) {
            printer.printRecord("a,b", "c");
            assertEquals("\"a,b\",c" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testMongoDbCsvDoubleQuoteInValue() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.MONGODB_CSV)) {
            printer.printRecord("a \"c\" b", "d");
            assertEquals("\"a \"\"c\"\" b\",d" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testMongoDbCsvTabInValue() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.MONGODB_CSV)) {
            printer.printRecord("a\tb", "c");
            assertEquals("a\tb,c" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testMongoDbTsvBasic() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.MONGODB_TSV)) {
            printer.printRecord("a", "b");
            assertEquals("a\tb" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testMongoDbTsvCommaInValue() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.MONGODB_TSV)) {
            printer.printRecord("a,b", "c");
            assertEquals("a,b\tc" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testMongoDbTsvTabInValue() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.MONGODB_TSV)) {
            printer.printRecord("a\tb", "c");
            assertEquals("\"a\tb\"\tc" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testMultiLineComment() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withCommentMarker('#'))) {
            printer.printComment("This is a comment\non multiple lines");

            assertEquals("# This is a comment" + recordSeparator + "# on multiple lines" + recordSeparator,
                    sw.toString());
        }
    }

    @Test
    public void testMySqlNullOutput() throws IOException {
        Object[] s = new String[] { "NULL", null };
        CSVFormat format = CSVFormat.MYSQL.withQuote(DQUOTE_CHAR).withNullString("NULL").withQuoteMode(QuoteMode.NON_NUMERIC);
        StringWriter writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        String expected = "\"NULL\"\tNULL\n";
        assertEquals(expected, writer.toString());
        String[] record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(new Object[2], record0);

        s = new String[] { "\\N", null };
        format = CSVFormat.MYSQL.withNullString("\\N");
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\\\\N\t\\N\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "\\N", "A" };
        format = CSVFormat.MYSQL.withNullString("\\N");
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\\\\N\tA\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "\n", "A" };
        format = CSVFormat.MYSQL.withNullString("\\N");
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\\n\tA\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "", null };
        format = CSVFormat.MYSQL.withNullString("NULL");
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\tNULL\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "", null };
        format = CSVFormat.MYSQL;
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\t\\N\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "\\N", "", "\u000e,\\\r" };
        format = CSVFormat.MYSQL;
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\\\\N\t\t\u000e,\\\\\\r\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "NULL", "\\\r" };
        format = CSVFormat.MYSQL;
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "NULL\t\\\\\\r\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "\\\r" };
        format = CSVFormat.MYSQL;
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\\\\\\r\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);
    }

    @Test
    public void testMySqlNullStringDefault() {
        assertEquals("\\N", CSVFormat.MYSQL.getNullString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewCsvPrinterAppendableNullFormat() throws Exception {
        try (final CSVPrinter printer = new CSVPrinter(new StringWriter(), null)) {
            Assert.fail("This test should have thrown an exception.");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewCsvPrinterNullAppendableFormat() throws Exception {
        try (final CSVPrinter printer = new CSVPrinter(null, CSVFormat.DEFAULT)) {
            Assert.fail("This test should have thrown an exception.");
        }
    }

    @Test
    public void testParseCustomNullValues() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVFormat format = CSVFormat.DEFAULT.withNullString("NULL");
        try (final CSVPrinter printer = new CSVPrinter(sw, format)) {
            printer.printRecord("a", null, "b");
        }
        final String csvString = sw.toString();
        assertEquals("a,NULL,b" + recordSeparator, csvString);
        try (final CSVParser iterable = format.parse(new StringReader(csvString))) {
            final Iterator<CSVRecord> iterator = iterable.iterator();
            final CSVRecord record = iterator.next();
            assertEquals("a", record.get(0));
            assertEquals(null, record.get(1));
            assertEquals("b", record.get(2));
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testPlainEscaped() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuote(null).withEscape('!'))) {
            printer.print("abc");
            printer.print("xyz");
            assertEquals("abc,xyz", sw.toString());
        }
    }

    @Test
    public void testPlainPlain() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuote(null))) {
            printer.print("abc");
            printer.print("xyz");
            assertEquals("abc,xyz", sw.toString());
        }
    }

    @Test
    public void testPlainQuoted() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuote('\''))) {
            printer.print("abc");
            assertEquals("abc", sw.toString());
        }
    }

    @Test
    @Ignore
    public void testPostgreSqlCsvNullOutput() throws IOException {
        Object[] s = new String[] { "NULL", null };
        CSVFormat format = CSVFormat.POSTGRESQL_CSV.withQuote(DQUOTE_CHAR).withNullString("NULL").withQuoteMode(QuoteMode.ALL_NON_NULL);
        StringWriter writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        String expected = "\"NULL\",NULL\n";
        assertEquals(expected, writer.toString());
        String[] record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(new Object[2], record0);

        s = new String[] { "\\N", null };
        format = CSVFormat.POSTGRESQL_CSV.withNullString("\\N");
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\\\\N\t\\N\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "\\N", "A" };
        format = CSVFormat.POSTGRESQL_CSV.withNullString("\\N");
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\\\\N\tA\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "\n", "A" };
        format = CSVFormat.POSTGRESQL_CSV.withNullString("\\N");
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\\n\tA\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "", null };
        format = CSVFormat.POSTGRESQL_CSV.withNullString("NULL");
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\tNULL\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "", null };
        format = CSVFormat.POSTGRESQL_CSV;
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\t\\N\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "\\N", "", "\u000e,\\\r" };
        format = CSVFormat.POSTGRESQL_CSV;
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\\\\N\t\t\u000e,\\\\\\r\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "NULL", "\\\r" };
        format = CSVFormat.POSTGRESQL_CSV;
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "NULL\t\\\\\\r\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "\\\r" };
        format = CSVFormat.POSTGRESQL_CSV;
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\\\\\\r\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);
    }

    @Test
    @Ignore
    public void testPostgreSqlCsvTextOutput() throws IOException {
        Object[] s = new String[] { "NULL", null };
        CSVFormat format = CSVFormat.POSTGRESQL_TEXT.withQuote(DQUOTE_CHAR).withNullString("NULL").withQuoteMode(QuoteMode.ALL_NON_NULL);
        StringWriter writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        String expected = "\"NULL\"\tNULL\n";
        assertEquals(expected, writer.toString());
        String[] record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(new Object[2], record0);

        s = new String[] { "\\N", null };
        format = CSVFormat.POSTGRESQL_TEXT.withNullString("\\N");
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\\\\N\t\\N\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "\\N", "A" };
        format = CSVFormat.POSTGRESQL_TEXT.withNullString("\\N");
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\\\\N\tA\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "\n", "A" };
        format = CSVFormat.POSTGRESQL_TEXT.withNullString("\\N");
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\\n\tA\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "", null };
        format = CSVFormat.POSTGRESQL_TEXT.withNullString("NULL");
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\tNULL\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "", null };
        format = CSVFormat.POSTGRESQL_TEXT;
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\t\\N\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "\\N", "", "\u000e,\\\r" };
        format = CSVFormat.POSTGRESQL_TEXT;
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\\\\N\t\t\u000e,\\\\\\r\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "NULL", "\\\r" };
        format = CSVFormat.POSTGRESQL_TEXT;
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "NULL\t\\\\\\r\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);

        s = new String[] { "\\\r" };
        format = CSVFormat.POSTGRESQL_TEXT;
        writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(s);
        }
        expected = "\\\\\\r\n";
        assertEquals(expected, writer.toString());
        record0 = toFirstRecordValues(expected, format);
        assertArrayEquals(expectNulls(s, format), record0);
    }

    @Test
    public void testPostgreSqlNullStringDefaultCsv() {
        assertEquals("", CSVFormat.POSTGRESQL_CSV.getNullString());
    }

    @Test
    public void testPostgreSqlNullStringDefaultText() {
        assertEquals("\\N", CSVFormat.POSTGRESQL_TEXT.getNullString());
    }

    @Test
    public void testPrint() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = CSVFormat.DEFAULT.print(sw)) {
            printer.printRecord("a", "b\\c");
            assertEquals("a,b\\c" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testPrintCustomNullValues() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withNullString("NULL"))) {
            printer.printRecord("a", null, "b");
            assertEquals("a,NULL,b" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testPrinter1() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            printer.printRecord("a", "b");
            assertEquals("a,b" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testPrinter2() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            printer.printRecord("a,b", "b");
            assertEquals("\"a,b\",b" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testPrinter3() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            printer.printRecord("a, b", "b ");
            assertEquals("\"a, b\",\"b \"" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testPrinter4() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            printer.printRecord("a", "b\"c");
            assertEquals("a,\"b\"\"c\"" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testPrinter5() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            printer.printRecord("a", "b\nc");
            assertEquals("a,\"b\nc\"" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testPrinter6() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            printer.printRecord("a", "b\r\nc");
            assertEquals("a,\"b\r\nc\"" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testPrinter7() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            printer.printRecord("a", "b\\c");
            assertEquals("a,b\\c" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testPrintNullValues() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            printer.printRecord("a", null, "b");
            assertEquals("a,,b" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testPrintOnePositiveInteger() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuoteMode(QuoteMode.MINIMAL))) {
            printer.print(Integer.MAX_VALUE);
            assertEquals(String.valueOf(Integer.MAX_VALUE), sw.toString());
        }
    }

    /**
     * Test to target the use of {@link IOUtils#copy(java.io.Reader, Appendable)} which directly
     * buffers the value from the Reader to the Appendable.
     *
     * <p>Requires the format to have no quote or escape character, value to be a
     * {@link java.io.Reader Reader} and the output <i>MUST NOT</i> be a
     * {@link java.io.Writer Writer} but some other Appendable.</p>
     *
     * @throws IOException Not expected to happen
     */
    @Test
    public void testPrintReaderWithoutQuoteToAppendable() throws IOException {
        final StringBuilder sb = new StringBuilder();
        final String content = "testValue";
        try (final CSVPrinter printer = new CSVPrinter(sb, CSVFormat.DEFAULT.withQuote(null))) {
            final StringReader value = new StringReader(content);
            printer.print(value);
        }
        assertEquals(content, sb.toString());
    }

    /**
     * Test to target the use of {@link IOUtils#copyLarge(java.io.Reader, Writer)} which directly
     * buffers the value from the Reader to the Writer.
     *
     * <p>Requires the format to have no quote or escape character, value to be a
     * {@link java.io.Reader Reader} and the output <i>MUST</i> be a
     * {@link java.io.Writer Writer}.</p>
     *
     * @throws IOException Not expected to happen
     */
    @Test
    public void testPrintReaderWithoutQuoteToWriter() throws IOException {
        final StringWriter sw = new StringWriter();
        final String content = "testValue";
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuote(null))) {
            final StringReader value = new StringReader(content);
            printer.print(value);
        }
        assertEquals(content, sw.toString());
    }

    @Test
    public void testPrintRecordsWithEmptyVector() throws IOException {
        final PrintStream out = System.out;
        try {
            System.setOut(new PrintStream(NullOutputStream.NULL_OUTPUT_STREAM));
            try (CSVPrinter csvPrinter = CSVFormat.POSTGRESQL_TEXT.printer()) {
                final Vector<CSVFormatTest.EmptyEnum> vector = new Vector<>();
                final int expectedCapacity = 23;
                vector.setSize(expectedCapacity);
                csvPrinter.printRecords(vector);
                assertEquals(expectedCapacity, vector.capacity());
            }
        } finally {
            System.setOut(out);
        }
    }

    @Test
    public void testPrintRecordsWithObjectArray() throws IOException {
        final CharArrayWriter charArrayWriter = new CharArrayWriter(0);
        try (CSVPrinter csvPrinter = CSVFormat.INFORMIX_UNLOAD.print(charArrayWriter)) {
            final HashSet<BatchUpdateException> hashSet = new HashSet<>();
            final Object[] objectArray = new Object[6];
            objectArray[3] = hashSet;
            csvPrinter.printRecords(objectArray);
        }
        assertEquals(6, charArrayWriter.size());
        assertEquals("\n\n\n\n\n\n", charArrayWriter.toString());
    }

    @Test
    public void testPrintRecordsWithResultSetOneRow() throws IOException, SQLException {
        try (CSVPrinter csvPrinter = CSVFormat.MYSQL.printer()) {
            try (ResultSet resultSet = new SimpleResultSet()) {
                csvPrinter.printRecords(resultSet);
                assertEquals(0, resultSet.getRow());
            }
        }
    }

    @Test
    public void testPrintToFileWithCharsetUtf16Be() throws IOException {
        final File file = File.createTempFile(getClass().getName(), ".csv");
        try (final CSVPrinter printer = CSVFormat.DEFAULT.print(file, StandardCharsets.UTF_16BE)) {
            printer.printRecord("a", "b\\c");
        }
        assertEquals("a,b\\c" + recordSeparator, FileUtils.readFileToString(file, StandardCharsets.UTF_16BE));
    }

    @Test
    public void testPrintToFileWithDefaultCharset() throws IOException {
        final File file = File.createTempFile(getClass().getName(), ".csv");
        try (final CSVPrinter printer = CSVFormat.DEFAULT.print(file, Charset.defaultCharset())) {
            printer.printRecord("a", "b\\c");
        }
        assertEquals("a,b\\c" + recordSeparator, FileUtils.readFileToString(file, Charset.defaultCharset()));
    }

    @Test
    public void testPrintToPathWithDefaultCharset() throws IOException {
        final File file = File.createTempFile(getClass().getName(), ".csv");
        try (final CSVPrinter printer = CSVFormat.DEFAULT.print(file.toPath(), Charset.defaultCharset())) {
            printer.printRecord("a", "b\\c");
        }
        assertEquals("a,b\\c" + recordSeparator, FileUtils.readFileToString(file, Charset.defaultCharset()));
    }

    @Test
    public void testQuoteAll() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuoteMode(QuoteMode.ALL))) {
            printer.printRecord("a", "b\nc", "d");
            assertEquals("\"a\",\"b\nc\",\"d\"" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testQuoteCommaFirstChar() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.RFC4180)) {
            printer.printRecord(",");
            assertEquals("\",\"" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testQuoteNonNumeric() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuoteMode(QuoteMode.NON_NUMERIC))) {
            printer.printRecord("a", "b\nc", Integer.valueOf(1));
            assertEquals("\"a\",\"b\nc\",1" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testRandomDefault() throws Exception {
        doRandom(CSVFormat.DEFAULT, ITERATIONS_FOR_RANDOM_TEST);
    }

    @Test
    public void testRandomExcel() throws Exception {
        doRandom(CSVFormat.EXCEL, ITERATIONS_FOR_RANDOM_TEST);
    }

    @Test
    @Ignore
    public void testRandomMongoDbCsv() throws Exception {
        doRandom(CSVFormat.MONGODB_CSV, ITERATIONS_FOR_RANDOM_TEST);
    }

    @Test
    public void testRandomMySql() throws Exception {
        doRandom(CSVFormat.MYSQL, ITERATIONS_FOR_RANDOM_TEST);
    }

    @Test
    @Ignore
    public void testRandomOracle() throws Exception {
        doRandom(CSVFormat.ORACLE, ITERATIONS_FOR_RANDOM_TEST);
    }

    @Test
    @Ignore
    public void testRandomPostgreSqlCsv() throws Exception {
        doRandom(CSVFormat.POSTGRESQL_CSV, ITERATIONS_FOR_RANDOM_TEST);
    }

    @Test
    @Ignore
    public void testRandomPostgreSqlText() throws Exception {
        doRandom(CSVFormat.POSTGRESQL_TEXT, ITERATIONS_FOR_RANDOM_TEST);
    }

    @Test
    public void testRandomRfc4180() throws Exception {
        doRandom(CSVFormat.RFC4180, ITERATIONS_FOR_RANDOM_TEST);
    }

    @Test
    public void testRandomTdf() throws Exception {
        doRandom(CSVFormat.TDF, ITERATIONS_FOR_RANDOM_TEST);
    }


    @Test
    public void testSingleLineComment() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withCommentMarker('#'))) {
            printer.printComment("This is a comment");
            assertEquals("# This is a comment" + recordSeparator, sw.toString());
        }
    }

    @Test
    public void testSingleQuoteQuoted() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuote('\''))) {
            printer.print("a'b'c");
            printer.print("xyz");
            assertEquals("'a''b''c',xyz", sw.toString());
        }
    }

    @Test
    public void testSkipHeaderRecordFalse() throws IOException {
        // functionally identical to testHeader, used to test CSV-153
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw,
                CSVFormat.DEFAULT.withQuote(null).withHeader("C1", "C2", "C3").withSkipHeaderRecord(false))) {
            printer.printRecord("a", "b", "c");
            printer.printRecord("x", "y", "z");
            assertEquals("C1,C2,C3\r\na,b,c\r\nx,y,z\r\n", sw.toString());
        }
    }

    @Test
    public void testSkipHeaderRecordTrue() throws IOException {
        // functionally identical to testHeaderNotSet, used to test CSV-153
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw,
                CSVFormat.DEFAULT.withQuote(null).withHeader("C1", "C2", "C3").withSkipHeaderRecord(true))) {
            printer.printRecord("a", "b", "c");
            printer.printRecord("x", "y", "z");
            assertEquals("a,b,c\r\nx,y,z\r\n", sw.toString());
        }
    }

    @Test
    public void testTrailingDelimiterOnTwoColumns() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withTrailingDelimiter())) {
            printer.printRecord("A", "B");
            assertEquals("A,B,\r\n", sw.toString());
        }
    }

    @Test
    public void testTrimOffOneColumn() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withTrim(false))) {
            printer.print(" A ");
            assertEquals("\" A \"", sw.toString());
        }
    }

    @Test
    public void testTrimOnOneColumn() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withTrim())) {
            printer.print(" A ");
            assertEquals("A", sw.toString());
        }
    }

    @Test
    public void testTrimOnTwoColumns() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withTrim())) {
            printer.print(" A ");
            printer.print(" B ");
            assertEquals("A,B", sw.toString());
        }
    }

    private String[] toFirstRecordValues(final String expected, final CSVFormat format) throws IOException {
        return CSVParser.parse(expected, format).getRecords().get(0).values();
    }

    private void tryFormat(List<String> l, Character quote, Character escape, String expected) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.withQuote(quote).withEscape(escape).withRecordSeparator(null);
        Appendable out = new StringBuilder();
        CSVPrinter printer = new CSVPrinter(out, format);
        printer.printRecord(l);
        printer.close();
        assertEquals(expected, out.toString());
    }
}
