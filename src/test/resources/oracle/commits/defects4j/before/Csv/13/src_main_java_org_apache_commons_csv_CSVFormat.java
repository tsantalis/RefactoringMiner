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

import static org.apache.commons.csv.Constants.BACKSLASH;
import static org.apache.commons.csv.Constants.COMMA;
import static org.apache.commons.csv.Constants.CR;
import static org.apache.commons.csv.Constants.CRLF;
import static org.apache.commons.csv.Constants.DOUBLE_QUOTE_CHAR;
import static org.apache.commons.csv.Constants.LF;
import static org.apache.commons.csv.Constants.TAB;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Specifies the format of a CSV file and parses input.
 *
 * <h2>Using predefined formats</h2>
 *
 * <p>
 * You can use one of the predefined formats:
 * </p>
 *
 * <ul>
 * <li>{@link #DEFAULT}</li>
 * <li>{@link #EXCEL}</li>
 * <li>{@link #MYSQL}</li>
 * <li>{@link #RFC4180}</li>
 * <li>{@link #TDF}</li>
 * </ul>
 *
 * <p>
 * For example:
 * </p>
 *
 * <pre>
 * CSVParser parser = CSVFormat.EXCEL.parse(reader);
 * </pre>
 *
 * <p>
 * The {@link CSVParser} provides static methods to parse other input types, for example:
 * </p>
 *
 * <pre>
 * CSVParser parser = CSVParser.parse(file, StandardCharsets.US_ASCII, CSVFormat.EXCEL);
 * </pre>
 *
 * <h2>Defining formats</h2>
 *
 * <p>
 * You can extend a format by calling the {@code with} methods. For example:
 * </p>
 *
 * <pre>
 * CSVFormat.EXCEL.withNullString(&quot;N/A&quot;).withIgnoreSurroundingSpaces(true);
 * </pre>
 *
 * <h2>Defining column names</h2>
 *
 * <p>
 * To define the column names you want to use to access records, write:
 * </p>
 *
 * <pre>
 * CSVFormat.EXCEL.withHeader(&quot;Col1&quot;, &quot;Col2&quot;, &quot;Col3&quot;);
 * </pre>
 *
 * <p>
 * Calling {@link #withHeader(String...)} let's you use the given names to address values in a {@link CSVRecord}, and
 * assumes that your CSV source does not contain a first record that also defines column names.
 *
 * If it does, then you are overriding this metadata with your names and you should skip the first record by calling
 * {@link #withSkipHeaderRecord(boolean)} with {@code true}.
 * </p>
 *
 * <h2>Parsing</h2>
 *
 * <p>
 * You can use a format directly to parse a reader. For example, to parse an Excel file with columns header, write:
 * </p>
 *
 * <pre>
 * Reader in = ...;
 * CSVFormat.EXCEL.withHeader(&quot;Col1&quot;, &quot;Col2&quot;, &quot;Col3&quot;).parse(in);
 * </pre>
 *
 * <p>
 * For other input types, like resources, files, and URLs, use the static methods on {@link CSVParser}.
 * </p>
 *
 * <h2>Referencing columns safely</h2>
 *
 * <p>
 * If your source contains a header record, you can simplify your code and safely reference columns, by using
 * {@link #withHeader(String...)} with no arguments:
 * </p>
 *
 * <pre>
 * CSVFormat.EXCEL.withHeader();
 * </pre>
 *
 * <p>
 * This causes the parser to read the first record and use its values as column names.
 *
 * Then, call one of the {@link CSVRecord} get method that takes a String column name argument:
 * </p>
 *
 * <pre>
 * String value = record.get(&quot;Col1&quot;);
 * </pre>
 *
 * <p>
 * This makes your code impervious to changes in column order in the CSV file.
 * </p>
 *
 * <h2>Notes</h2>
 *
 * <p>
 * This class is immutable.
 * </p>
 *
 * @version $Id$
 */
public final class CSVFormat implements Serializable {

    /**
     * Predefines formats.
     * 
     * @since 1.2
     */
    public static enum Predefined {

        /**
         * @see CSVFormat#DEFAULT
         */
        Default(CSVFormat.DEFAULT), 

        /**
         * @see CSVFormat#EXCEL
         */
        Excel(CSVFormat.EXCEL), 

        /**
         * @see CSVFormat#MYSQL
         */
        MySQL(CSVFormat.MYSQL), 

        /**
         * @see CSVFormat#RFC4180
         */
        RFC4180(CSVFormat.RFC4180),

        /**
         * @see CSVFormat#TDF
         */
        TDF(CSVFormat.TDF);

        private final CSVFormat format;

        private Predefined(final CSVFormat format) {
            this.format = format;
        }
        
        /**
         * Gets the format.
         * 
         * @return the format.
         */
        public CSVFormat getFormat() {
            return format;
        }
    };
    
    private static final long serialVersionUID = 1L;

    private final char delimiter;
    private final Character quoteCharacter; // null if quoting is disabled
    private final QuoteMode quoteMode;
    private final Character commentMarker; // null if commenting is disabled
    private final Character escapeCharacter; // null if escaping is disabled
    private final boolean ignoreSurroundingSpaces; // Should leading/trailing spaces be ignored around values?
    private final boolean allowMissingColumnNames;
    private final boolean ignoreEmptyLines;
    private final String recordSeparator; // for outputs
    private final String nullString; // the string to be used for null values
    private final String[] header; // array of header column names
    private final String[] headerComments; // array of header comment lines
    private final boolean skipHeaderRecord;
    private final boolean ignoreHeaderCase; // should ignore header names case

    /**
     * Standard comma separated format, as for {@link #RFC4180} but allowing empty lines.
     *
     * <p>
     * Settings are:
     * </p>
     * <ul>
     * <li>withDelimiter(',')</li>
     * <li>withQuote('"')</li>
     * <li>withRecordSeparator("\r\n")</li>
     * <li>withIgnoreEmptyLines(true)</li>
     * </ul>
     * @see Predefined#Default
     */
    public static final CSVFormat DEFAULT = new CSVFormat(COMMA, DOUBLE_QUOTE_CHAR, null, null, null, false, true,
            CRLF, null, null, null, false, false, false);

    /**
     * Comma separated format as defined by <a href="http://tools.ietf.org/html/rfc4180">RFC 4180</a>.
     *
     * <p>
     * Settings are:
     * </p>
     * <ul>
     * <li>withDelimiter(',')</li>
     * <li>withQuote('"')</li>
     * <li>withRecordSeparator("\r\n")</li>
     * <li>withIgnoreEmptyLines(false)</li>
     * </ul>
     * @see Predefined#RFC4180
     */
    public static final CSVFormat RFC4180 = DEFAULT.withIgnoreEmptyLines(false);

    /**
     * Excel file format (using a comma as the value delimiter). Note that the actual value delimiter used by Excel is
     * locale dependent, it might be necessary to customize this format to accommodate to your regional settings.
     *
     * <p>
     * For example for parsing or generating a CSV file on a French system the following format will be used:
     * </p>
     *
     * <pre>
     * CSVFormat fmt = CSVFormat.EXCEL.withDelimiter(';');
     * </pre>
     *
     * <p>
     * Settings are:
     * </p>
     * <ul>
     * <li>{@link #withDelimiter(char) withDelimiter(',')}</li>
     * <li>{@link #withQuote(char) withQuote('"')}</li>
     * <li>{@link #withRecordSeparator(String) withRecordSeparator("\r\n")}</li>
     * <li>{@link #withIgnoreEmptyLines(boolean) withIgnoreEmptyLines(false)}</li>
     * <li>{@link #withAllowMissingColumnNames(boolean) withAllowMissingColumnNames(true)}</li>
     * </ul>
     * <p>
     * Note: this is currently like {@link #RFC4180} plus {@link #withAllowMissingColumnNames(boolean)
     * withAllowMissingColumnNames(true)}.
     * </p>
     * @see Predefined#Excel
     */
    public static final CSVFormat EXCEL = DEFAULT.withIgnoreEmptyLines(false).withAllowMissingColumnNames();

    /**
     * Tab-delimited format.
     *
     * <p>
     * Settings are:
     * </p>
     * <ul>
     * <li>withDelimiter('\t')</li>
     * <li>withQuote('"')</li>
     * <li>withRecordSeparator("\r\n")</li>
     * <li>withIgnoreSurroundingSpaces(true)</li>
     * </ul>
     * @see Predefined#TDF
     */
    public static final CSVFormat TDF = DEFAULT.withDelimiter(TAB).withIgnoreSurroundingSpaces();

    /**
     * Default MySQL format used by the {@code SELECT INTO OUTFILE} and {@code LOAD DATA INFILE} operations.
     *
     * <p>
     * This is a tab-delimited format with a LF character as the line separator. Values are not quoted and special
     * characters are escaped with {@code '\'}. The default NULL string is {@code "\\N"}.
     * </p>
     *
     * <p>
     * Settings are:
     * </p>
     * <ul>
     * <li>withDelimiter('\t')</li>
     * <li>withQuote(null)</li>
     * <li>withRecordSeparator('\n')</li>
     * <li>withIgnoreEmptyLines(false)</li>
     * <li>withEscape('\\')</li>
     * <li>withNullString("\\N")</li>
     * </ul>
     *
     * @see Predefined#MySQL
     * @see <a href="http://dev.mysql.com/doc/refman/5.1/en/load-data.html">
     *      http://dev.mysql.com/doc/refman/5.1/en/load-data.html</a>
     */
    public static final CSVFormat MYSQL = DEFAULT.withDelimiter(TAB).withEscape(BACKSLASH).withIgnoreEmptyLines(false)
            .withQuote(null).withRecordSeparator(LF);

    /**
     * Returns true if the given character is a line break character.
     *
     * @param c
     *            the character to check
     *
     * @return true if <code>c</code> is a line break character
     */
    private static boolean isLineBreak(final char c) {
        return c == LF || c == CR;
    }

    /**
     * Returns true if the given character is a line break character.
     *
     * @param c
     *            the character to check, may be null
     *
     * @return true if <code>c</code> is a line break character (and not null)
     */
    private static boolean isLineBreak(final Character c) {
        return c != null && isLineBreak(c.charValue());
    }

    /**
     * Creates a new CSV format with the specified delimiter.
     *
     * <p>
     * Use this method if you want to create a CSVFormat from scratch. All fields but the delimiter will be initialized
     * with null/false.
     * </p>
     *
     * @param delimiter
     *            the char used for value separation, must not be a line break character
     * @return a new CSV format.
     * @throws IllegalArgumentException
     *             if the delimiter is a line break character
     *
     * @see #DEFAULT
     * @see #RFC4180
     * @see #MYSQL
     * @see #EXCEL
     * @see #TDF
     */
    public static CSVFormat newFormat(final char delimiter) {
        return new CSVFormat(delimiter, null, null, null, null, false, false, null, null, null, null, false, false, false);
    }

    /**
     * Gets one of the predefined formats from {@link CSVFormat.Predefined}.
     * 
     * @param format
     *            name
     * @return one of the predefined formats
     * @since 1.2
     */
    public static CSVFormat valueOf(final String format) {
        return CSVFormat.Predefined.valueOf(format).getFormat();
    }

    /**
     * Creates a customized CSV format.
     *
     * @param delimiter
     *            the char used for value separation, must not be a line break character
     * @param quoteChar
     *            the Character used as value encapsulation marker, may be {@code null} to disable
     * @param quoteMode
     *            the quote mode
     * @param commentStart
     *            the Character used for comment identification, may be {@code null} to disable
     * @param escape
     *            the Character used to escape special characters in values, may be {@code null} to disable
     * @param ignoreSurroundingSpaces
     *            {@code true} when whitespaces enclosing values should be ignored
     * @param ignoreEmptyLines
     *            {@code true} when the parser should skip empty lines
     * @param recordSeparator
     *            the line separator to use for output
     * @param nullString
     *            the line separator to use for output
     * @param headerComments
     *            the comments to be printed by the Printer before the actual CSV data
     * @param header
     *            the header
     * @param skipHeaderRecord
     *            TODO
     * @param allowMissingColumnNames
     *            TODO
     * @param ignoreHeaderCase
     *            TODO
     * @throws IllegalArgumentException
     *             if the delimiter is a line break character
     */
    private CSVFormat(final char delimiter, final Character quoteChar, final QuoteMode quoteMode,
            final Character commentStart, final Character escape, final boolean ignoreSurroundingSpaces,
            final boolean ignoreEmptyLines, final String recordSeparator, final String nullString,
            final Object[] headerComments, final String[] header, final boolean skipHeaderRecord,
            final boolean allowMissingColumnNames, final boolean ignoreHeaderCase) {
        this.delimiter = delimiter;
        this.quoteCharacter = quoteChar;
        this.quoteMode = quoteMode;
        this.commentMarker = commentStart;
        this.escapeCharacter = escape;
        this.ignoreSurroundingSpaces = ignoreSurroundingSpaces;
        this.allowMissingColumnNames = allowMissingColumnNames;
        this.ignoreEmptyLines = ignoreEmptyLines;
        this.recordSeparator = recordSeparator;
        this.nullString = nullString;
        this.headerComments = toStringArray(headerComments);
        this.header = header == null ? null : header.clone();
        this.skipHeaderRecord = skipHeaderRecord;
        this.ignoreHeaderCase = ignoreHeaderCase;
        validate();
    }

    private String[] toStringArray(final Object[] values) {
        if (values == null) {
            return null;
        }
        final String[] strings = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            final Object value = values[i];
            strings[i] = value == null ? null : value.toString();
        }
        return strings;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final CSVFormat other = (CSVFormat) obj;
        if (delimiter != other.delimiter) {
            return false;
        }
        if (quoteMode != other.quoteMode) {
            return false;
        }
        if (quoteCharacter == null) {
            if (other.quoteCharacter != null) {
                return false;
            }
        } else if (!quoteCharacter.equals(other.quoteCharacter)) {
            return false;
        }
        if (commentMarker == null) {
            if (other.commentMarker != null) {
                return false;
            }
        } else if (!commentMarker.equals(other.commentMarker)) {
            return false;
        }
        if (escapeCharacter == null) {
            if (other.escapeCharacter != null) {
                return false;
            }
        } else if (!escapeCharacter.equals(other.escapeCharacter)) {
            return false;
        }
        if (nullString == null) {
            if (other.nullString != null) {
                return false;
            }
        } else if (!nullString.equals(other.nullString)) {
            return false;
        }
        if (!Arrays.equals(header, other.header)) {
            return false;
        }
        if (ignoreSurroundingSpaces != other.ignoreSurroundingSpaces) {
            return false;
        }
        if (ignoreEmptyLines != other.ignoreEmptyLines) {
            return false;
        }
        if (skipHeaderRecord != other.skipHeaderRecord) {
            return false;
        }
        if (recordSeparator == null) {
            if (other.recordSeparator != null) {
                return false;
            }
        } else if (!recordSeparator.equals(other.recordSeparator)) {
            return false;
        }
        return true;
    }

    /**
     * Formats the specified values.
     *
     * @param values
     *            the values to format
     * @return the formatted values
     */
    public String format(final Object... values) {
        final StringWriter out = new StringWriter();
        try {
            new CSVPrinter(out, this).printRecord(values);
            return out.toString().trim();
        } catch (final IOException e) {
            // should not happen because a StringWriter does not do IO.
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the character marking the start of a line comment.
     *
     * @return the comment start marker, may be {@code null}
     */
    public Character getCommentMarker() {
        return commentMarker;
    }

    /**
     * Returns the character delimiting the values (typically ';', ',' or '\t').
     *
     * @return the delimiter character
     */
    public char getDelimiter() {
        return delimiter;
    }

    /**
     * Returns the escape character.
     *
     * @return the escape character, may be {@code null}
     */
    public Character getEscapeCharacter() {
        return escapeCharacter;
    }

    /**
     * Returns a copy of the header array.
     *
     * @return a copy of the header array; {@code null} if disabled, the empty array if to be read from the file
     */
    public String[] getHeader() {
        return header != null ? header.clone() : null;
    }

    /**
     * Returns a copy of the header comment array.
     *
     * @return a copy of the header comment array; {@code null} if disabled.
     */
    public String[] getHeaderComments() {
        return headerComments != null ? headerComments.clone() : null;
    }

    /**
     * Specifies whether missing column names are allowed when parsing the header line.
     *
     * @return {@code true} if missing column names are allowed when parsing the header line, {@code false} to throw an
     *         {@link IllegalArgumentException}.
     */
    public boolean getAllowMissingColumnNames() {
        return allowMissingColumnNames;
    }

    /**
     * Specifies whether empty lines between records are ignored when parsing input.
     *
     * @return {@code true} if empty lines between records are ignored, {@code false} if they are turned into empty
     *         records.
     */
    public boolean getIgnoreEmptyLines() {
        return ignoreEmptyLines;
    }

    /**
     * Specifies whether spaces around values are ignored when parsing input.
     *
     * @return {@code true} if spaces around values are ignored, {@code false} if they are treated as part of the value.
     */
    public boolean getIgnoreSurroundingSpaces() {
        return ignoreSurroundingSpaces;
    }

    /**
     * Specifies whether header names will be accessed ignoring case.
     *
     * @return {@code true} if header names cases are ignored, {@code false} if they are case sensitive.
     */
    public boolean getIgnoreHeaderCase() {
        return ignoreHeaderCase;
    }

    /**
     * Gets the String to convert to and from {@code null}.
     * <ul>
     * <li>
     * <strong>Reading:</strong> Converts strings equal to the given {@code nullString} to {@code null} when reading
     * records.</li>
     * <li>
     * <strong>Writing:</strong> Writes {@code null} as the given {@code nullString} when writing records.</li>
     * </ul>
     *
     * @return the String to convert to and from {@code null}. No substitution occurs if {@code null}
     */
    public String getNullString() {
        return nullString;
    }

    /**
     * Returns the character used to encapsulate values containing special characters.
     *
     * @return the quoteChar character, may be {@code null}
     */
    public Character getQuoteCharacter() {
        return quoteCharacter;
    }

    /**
     * Returns the quote policy output fields.
     *
     * @return the quote policy
     */
    public QuoteMode getQuoteMode() {
        return quoteMode;
    }

    /**
     * Returns the record separator delimiting output records.
     *
     * @return the record separator
     */
    public String getRecordSeparator() {
        return recordSeparator;
    }

    /**
     * Returns whether to skip the header record.
     *
     * @return whether to skip the header record.
     */
    public boolean getSkipHeaderRecord() {
        return skipHeaderRecord;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = prime * result + delimiter;
        result = prime * result + ((quoteMode == null) ? 0 : quoteMode.hashCode());
        result = prime * result + ((quoteCharacter == null) ? 0 : quoteCharacter.hashCode());
        result = prime * result + ((commentMarker == null) ? 0 : commentMarker.hashCode());
        result = prime * result + ((escapeCharacter == null) ? 0 : escapeCharacter.hashCode());
        result = prime * result + ((nullString == null) ? 0 : nullString.hashCode());
        result = prime * result + (ignoreSurroundingSpaces ? 1231 : 1237);
        result = prime * result + (ignoreHeaderCase ? 1231 : 1237);
        result = prime * result + (ignoreEmptyLines ? 1231 : 1237);
        result = prime * result + (skipHeaderRecord ? 1231 : 1237);
        result = prime * result + ((recordSeparator == null) ? 0 : recordSeparator.hashCode());
        result = prime * result + Arrays.hashCode(header);
        return result;
    }

    /**
     * Specifies whether comments are supported by this format.
     *
     * Note that the comment introducer character is only recognized at the start of a line.
     *
     * @return {@code true} is comments are supported, {@code false} otherwise
     */
    public boolean isCommentMarkerSet() {
        return commentMarker != null;
    }

    /**
     * Returns whether escape are being processed.
     *
     * @return {@code true} if escapes are processed
     */
    public boolean isEscapeCharacterSet() {
        return escapeCharacter != null;
    }

    /**
     * Returns whether a nullString has been defined.
     *
     * @return {@code true} if a nullString is defined
     */
    public boolean isNullStringSet() {
        return nullString != null;
    }

    /**
     * Returns whether a quoteChar has been defined.
     *
     * @return {@code true} if a quoteChar is defined
     */
    public boolean isQuoteCharacterSet() {
        return quoteCharacter != null;
    }

    /**
     * Parses the specified content.
     *
     * <p>
     * See also the various static parse methods on {@link CSVParser}.
     * </p>
     *
     * @param in
     *            the input stream
     * @return a parser over a stream of {@link CSVRecord}s.
     * @throws IOException
     *             If an I/O error occurs
     */
    public CSVParser parse(final Reader in) throws IOException {
        return new CSVParser(in, this);
    }

    /**
     * Prints to the specified output.
     *
     * <p>
     * See also {@link CSVPrinter}.
     * </p>
     *
     * @param out
     *            the output
     * @return a printer to an output
     * @throws IOException
     *             thrown if the optional header cannot be printed.
     */
    public CSVPrinter print(final Appendable out) throws IOException {
        return new CSVPrinter(out, this);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Delimiter=<").append(delimiter).append('>');
        if (isEscapeCharacterSet()) {
            sb.append(' ');
            sb.append("Escape=<").append(escapeCharacter).append('>');
        }
        if (isQuoteCharacterSet()) {
            sb.append(' ');
            sb.append("QuoteChar=<").append(quoteCharacter).append('>');
        }
        if (isCommentMarkerSet()) {
            sb.append(' ');
            sb.append("CommentStart=<").append(commentMarker).append('>');
        }
        if (isNullStringSet()) {
            sb.append(' ');
            sb.append("NullString=<").append(nullString).append('>');
        }
        if (recordSeparator != null) {
            sb.append(' ');
            sb.append("RecordSeparator=<").append(recordSeparator).append('>');
        }
        if (getIgnoreEmptyLines()) {
            sb.append(" EmptyLines:ignored");
        }
        if (getIgnoreSurroundingSpaces()) {
            sb.append(" SurroundingSpaces:ignored");
        }
        if (getIgnoreHeaderCase()) {
            sb.append(" IgnoreHeaderCase:ignored");
        }
        sb.append(" SkipHeaderRecord:").append(skipHeaderRecord);
        if (headerComments != null) {
            sb.append(' ');
            sb.append("HeaderComments:").append(Arrays.toString(headerComments));
        }
        if (header != null) {
            sb.append(' ');
            sb.append("Header:").append(Arrays.toString(header));
        }
        return sb.toString();
    }

    /**
     * Verifies the consistency of the parameters and throws an IllegalArgumentException if necessary.
     *
     * @throws IllegalArgumentException
     */
    private void validate() throws IllegalArgumentException {
        if (isLineBreak(delimiter)) {
            throw new IllegalArgumentException("The delimiter cannot be a line break");
        }
        
        if (quoteCharacter != null && delimiter == quoteCharacter.charValue()) {
            throw new IllegalArgumentException("The quoteChar character and the delimiter cannot be the same ('" +
                    quoteCharacter + "')");
        }

        if (escapeCharacter != null && delimiter == escapeCharacter.charValue()) {
            throw new IllegalArgumentException("The escape character and the delimiter cannot be the same ('" +
                    escapeCharacter + "')");
        }

        if (commentMarker != null && delimiter == commentMarker.charValue()) {
            throw new IllegalArgumentException("The comment start character and the delimiter cannot be the same ('" +
                    commentMarker + "')");
        }

        if (quoteCharacter != null && quoteCharacter.equals(commentMarker)) {
            throw new IllegalArgumentException("The comment start character and the quoteChar cannot be the same ('" +
                    commentMarker + "')");
        }

        if (escapeCharacter != null && escapeCharacter.equals(commentMarker)) {
            throw new IllegalArgumentException("The comment start and the escape character cannot be the same ('" +
                    commentMarker + "')");
        }

        if (escapeCharacter == null && quoteMode == QuoteMode.NONE) {
            throw new IllegalArgumentException("No quotes mode set but no escape character is set");
        }
        
        // validate header
        if (header != null) {
            final Set<String> dupCheck = new HashSet<String>();
            for (final String hdr : header) {
                if (!dupCheck.add(hdr)) {
                    throw new IllegalArgumentException("The header contains a duplicate entry: '" + hdr + "' in " +
                            Arrays.toString(header));
                }
            }
        }
    }

    /**
     * Sets the comment start marker of the format to the specified character.
     *
     * Note that the comment start character is only recognized at the start of a line.
     *
     * @param commentMarker
     *            the comment start marker
     * @return A new CSVFormat that is equal to this one but with the specified character as the comment start marker
     * @throws IllegalArgumentException
     *             thrown if the specified character is a line break
     */
    public CSVFormat withCommentMarker(final char commentMarker) {
        return withCommentMarker(Character.valueOf(commentMarker));
    }

    /**
     * Sets the comment start marker of the format to the specified character.
     *
     * Note that the comment start character is only recognized at the start of a line.
     *
     * @param commentMarker
     *            the comment start marker, use {@code null} to disable
     * @return A new CSVFormat that is equal to this one but with the specified character as the comment start marker
     * @throws IllegalArgumentException
     *             thrown if the specified character is a line break
     */
    public CSVFormat withCommentMarker(final Character commentMarker) {
        if (isLineBreak(commentMarker)) {
            throw new IllegalArgumentException("The comment start marker character cannot be a line break");
        }
        return new CSVFormat(delimiter, quoteCharacter, quoteMode, commentMarker, escapeCharacter,
                ignoreSurroundingSpaces, ignoreEmptyLines, recordSeparator, nullString, headerComments, header,
                skipHeaderRecord, allowMissingColumnNames, ignoreHeaderCase);
    }

    /**
     * Sets the delimiter of the format to the specified character.
     *
     * @param delimiter
     *            the delimiter character
     * @return A new CSVFormat that is equal to this with the specified character as delimiter
     * @throws IllegalArgumentException
     *             thrown if the specified character is a line break
     */
    public CSVFormat withDelimiter(final char delimiter) {
        if (isLineBreak(delimiter)) {
            throw new IllegalArgumentException("The delimiter cannot be a line break");
        }
        return new CSVFormat(delimiter, quoteCharacter, quoteMode, commentMarker, escapeCharacter,
                ignoreSurroundingSpaces, ignoreEmptyLines, recordSeparator, nullString, headerComments, header,
                skipHeaderRecord, allowMissingColumnNames, ignoreHeaderCase);
    }

    /**
     * Sets the escape character of the format to the specified character.
     *
     * @param escape
     *            the escape character
     * @return A new CSVFormat that is equal to his but with the specified character as the escape character
     * @throws IllegalArgumentException
     *             thrown if the specified character is a line break
     */
    public CSVFormat withEscape(final char escape) {
        return withEscape(Character.valueOf(escape));
    }

    /**
     * Sets the escape character of the format to the specified character.
     *
     * @param escape
     *            the escape character, use {@code null} to disable
     * @return A new CSVFormat that is equal to this but with the specified character as the escape character
     * @throws IllegalArgumentException
     *             thrown if the specified character is a line break
     */
    public CSVFormat withEscape(final Character escape) {
        if (isLineBreak(escape)) {
            throw new IllegalArgumentException("The escape character cannot be a line break");
        }
        return new CSVFormat(delimiter, quoteCharacter, quoteMode, commentMarker, escape, ignoreSurroundingSpaces,
                ignoreEmptyLines, recordSeparator, nullString, headerComments, header, skipHeaderRecord,
                allowMissingColumnNames, ignoreHeaderCase);
    }

    /**
     * Sets the header of the format. The header can either be parsed automatically from the input file with:
     *
     * <pre>
     * CSVFormat format = aformat.withHeader();
     * </pre>
     *
     * or specified manually with:
     *
     * <pre>
     * CSVFormat format = aformat.withHeader(&quot;name&quot;, &quot;email&quot;, &quot;phone&quot;);
     * </pre>
     * <p>
     * The header is also used by the {@link CSVPrinter}..
     * </p>
     *
     * @param header
     *            the header, {@code null} if disabled, empty if parsed automatically, user specified otherwise.
     *
     * @return A new CSVFormat that is equal to this but with the specified header
     * @see #withSkipHeaderRecord(boolean)
     */
    public CSVFormat withHeader(final String... header) {
        return new CSVFormat(delimiter, quoteCharacter, quoteMode, commentMarker, escapeCharacter,
                ignoreSurroundingSpaces, ignoreEmptyLines, recordSeparator, nullString, headerComments, header,
                skipHeaderRecord, allowMissingColumnNames, ignoreHeaderCase);
    }

    /**
     * Sets the header of the format. The header can either be parsed automatically from the input file with:
     *
     * <pre>
     * CSVFormat format = aformat.withHeader();
     * </pre>
     *
     * or specified manually with:
     *
     * <pre>
     * CSVFormat format = aformat.withHeader(resultSet);
     * </pre>
     * <p>
     * The header is also used by the {@link CSVPrinter}..
     * </p>
     *
     * @param resultSet
     *            the resultSet for the header, {@code null} if disabled, empty if parsed automatically, user specified
     *            otherwise.
     *
     * @return A new CSVFormat that is equal to this but with the specified header
     * @throws SQLException
     *             SQLException if a database access error occurs or this method is called on a closed result set.
     * @since 1.1
     */
    public CSVFormat withHeader(final ResultSet resultSet) throws SQLException {
        return withHeader(resultSet != null ? resultSet.getMetaData() : null);
    }

    /**
     * Sets the header of the format. The header can either be parsed automatically from the input file with:
     *
     * <pre>
     * CSVFormat format = aformat.withHeader();
     * </pre>
     *
     * or specified manually with:
     *
     * <pre>
     * CSVFormat format = aformat.withHeader(metaData);
     * </pre>
     * <p>
     * The header is also used by the {@link CSVPrinter}..
     * </p>
     *
     * @param metaData
     *            the metaData for the header, {@code null} if disabled, empty if parsed automatically, user specified
     *            otherwise.
     *
     * @return A new CSVFormat that is equal to this but with the specified header
     * @throws SQLException
     *             SQLException if a database access error occurs or this method is called on a closed result set.
     * @since 1.1
     */
    public CSVFormat withHeader(final ResultSetMetaData metaData) throws SQLException {
        String[] labels = null;
        if (metaData != null) {
            final int columnCount = metaData.getColumnCount();
            labels = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                labels[i] = metaData.getColumnLabel(i + 1);
            }
        }
        return new CSVFormat(delimiter, quoteCharacter, quoteMode, commentMarker, escapeCharacter,
                ignoreSurroundingSpaces, ignoreEmptyLines, recordSeparator, nullString, headerComments, labels,
                skipHeaderRecord, allowMissingColumnNames, ignoreHeaderCase);
    }

    /**
     * Sets the header comments of the format. The comments will be printed first, before the headers. This setting is
     * ignored by the parser.
     *
     * <pre>
     * CSVFormat format = aformat.withHeaderComments(&quot;Generated by Apache Commons CSV 1.1.&quot;, new Date());
     * </pre>
     *
     * @param headerComments
     *            the headerComments which will be printed by the Printer before the actual CSV data.
     *
     * @return A new CSVFormat that is equal to this but with the specified header
     * @see #withSkipHeaderRecord(boolean)
     * @since 1.1
     */
    public CSVFormat withHeaderComments(final Object... headerComments) {
        return new CSVFormat(delimiter, quoteCharacter, quoteMode, commentMarker, escapeCharacter,
                ignoreSurroundingSpaces, ignoreEmptyLines, recordSeparator, nullString, headerComments, header,
                skipHeaderRecord, allowMissingColumnNames, ignoreHeaderCase);
    }

    /**
     * Sets the missing column names behavior of the format to {@code true}
     *
     * @return A new CSVFormat that is equal to this but with the specified missing column names behavior.
     * @see #withAllowMissingColumnNames(boolean)
     * @since 1.1
     */
    public CSVFormat withAllowMissingColumnNames() {
        return this.withAllowMissingColumnNames(true);
    }

    /**
     * Sets the missing column names behavior of the format.
     *
     * @param allowMissingColumnNames
     *            the missing column names behavior, {@code true} to allow missing column names in the header line,
     *            {@code false} to cause an {@link IllegalArgumentException} to be thrown.
     * @return A new CSVFormat that is equal to this but with the specified missing column names behavior.
     */
    public CSVFormat withAllowMissingColumnNames(final boolean allowMissingColumnNames) {
        return new CSVFormat(delimiter, quoteCharacter, quoteMode, commentMarker, escapeCharacter,
                ignoreSurroundingSpaces, ignoreEmptyLines, recordSeparator, nullString, headerComments, header,
                skipHeaderRecord, allowMissingColumnNames, ignoreHeaderCase);
    }

    /**
     * Sets the empty line skipping behavior of the format to {@code true}.
     *
     * @return A new CSVFormat that is equal to this but with the specified empty line skipping behavior.
     * @since {@link #withIgnoreEmptyLines(boolean)}
     * @since 1.1
     */
    public CSVFormat withIgnoreEmptyLines() {
        return this.withIgnoreEmptyLines(true);
    }

    /**
     * Sets the empty line skipping behavior of the format.
     *
     * @param ignoreEmptyLines
     *            the empty line skipping behavior, {@code true} to ignore the empty lines between the records,
     *            {@code false} to translate empty lines to empty records.
     * @return A new CSVFormat that is equal to this but with the specified empty line skipping behavior.
     */
    public CSVFormat withIgnoreEmptyLines(final boolean ignoreEmptyLines) {
        return new CSVFormat(delimiter, quoteCharacter, quoteMode, commentMarker, escapeCharacter,
                ignoreSurroundingSpaces, ignoreEmptyLines, recordSeparator, nullString, headerComments, header,
                skipHeaderRecord, allowMissingColumnNames, ignoreHeaderCase);
    }

    /**
     * Sets the trimming behavior of the format to {@code true}.
     *
     * @return A new CSVFormat that is equal to this but with the specified trimming behavior.
     * @see #withIgnoreSurroundingSpaces(boolean)
     * @since 1.1
     */
    public CSVFormat withIgnoreSurroundingSpaces() {
        return this.withIgnoreSurroundingSpaces(true);
    }

    /**
     * Sets the trimming behavior of the format.
     *
     * @param ignoreSurroundingSpaces
     *            the trimming behavior, {@code true} to remove the surrounding spaces, {@code false} to leave the
     *            spaces as is.
     * @return A new CSVFormat that is equal to this but with the specified trimming behavior.
     */
    public CSVFormat withIgnoreSurroundingSpaces(final boolean ignoreSurroundingSpaces) {
        return new CSVFormat(delimiter, quoteCharacter, quoteMode, commentMarker, escapeCharacter,
                ignoreSurroundingSpaces, ignoreEmptyLines, recordSeparator, nullString, headerComments, header,
                skipHeaderRecord, allowMissingColumnNames, ignoreHeaderCase);
    }

    /**
     * Sets the header ignore case behavior to {@code true}.
     *
     * @return A new CSVFormat that will ignore case header name.
     * @see #withIgnoreHeaderCase(boolean)
     * @since ?
     */
    public CSVFormat withIgnoreHeaderCase() {
        return this.withIgnoreHeaderCase(true);
    }

    /**
     * Sets if header names should be accessed ignoring case.
     *
     * @param ignoreHeaderCase
     *            the case mapping behavior, {@code true} to access name/values, {@code false} to leave the
     *            mapping as is.
     * @return A new CSVFormat that will ignore case header name if specified as {@code true}
     */
    public CSVFormat withIgnoreHeaderCase(final boolean ignoreHeaderCase) {
        return new CSVFormat(delimiter, quoteCharacter, quoteMode, commentMarker, escapeCharacter,
                ignoreSurroundingSpaces, ignoreEmptyLines, recordSeparator, nullString, headerComments, header,
                skipHeaderRecord, allowMissingColumnNames, ignoreHeaderCase);
    }

    /**
     * Performs conversions to and from null for strings on input and output.
     * <ul>
     * <li>
     * <strong>Reading:</strong> Converts strings equal to the given {@code nullString} to {@code null} when reading
     * records.</li>
     * <li>
     * <strong>Writing:</strong> Writes {@code null} as the given {@code nullString} when writing records.</li>
     * </ul>
     *
     * @param nullString
     *            the String to convert to and from {@code null}. No substitution occurs if {@code null}
     *
     * @return A new CSVFormat that is equal to this but with the specified null conversion string.
     */
    public CSVFormat withNullString(final String nullString) {
        return new CSVFormat(delimiter, quoteCharacter, quoteMode, commentMarker, escapeCharacter,
                ignoreSurroundingSpaces, ignoreEmptyLines, recordSeparator, nullString, headerComments, header,
                skipHeaderRecord, allowMissingColumnNames, ignoreHeaderCase);
    }

    /**
     * Sets the quoteChar of the format to the specified character.
     *
     * @param quoteChar
     *            the quoteChar character
     * @return A new CSVFormat that is equal to this but with the specified character as quoteChar
     * @throws IllegalArgumentException
     *             thrown if the specified character is a line break
     */
    public CSVFormat withQuote(final char quoteChar) {
        return withQuote(Character.valueOf(quoteChar));
    }

    /**
     * Sets the quoteChar of the format to the specified character.
     *
     * @param quoteChar
     *            the quoteChar character, use {@code null} to disable
     * @return A new CSVFormat that is equal to this but with the specified character as quoteChar
     * @throws IllegalArgumentException
     *             thrown if the specified character is a line break
     */
    public CSVFormat withQuote(final Character quoteChar) {
        if (isLineBreak(quoteChar)) {
            throw new IllegalArgumentException("The quoteChar cannot be a line break");
        }
        return new CSVFormat(delimiter, quoteChar, quoteMode, commentMarker, escapeCharacter, ignoreSurroundingSpaces,
                ignoreEmptyLines, recordSeparator, nullString, headerComments, header, skipHeaderRecord,
                allowMissingColumnNames, ignoreHeaderCase);
    }

    /**
     * Sets the output quote policy of the format to the specified value.
     *
     * @param quoteModePolicy
     *            the quote policy to use for output.
     *
     * @return A new CSVFormat that is equal to this but with the specified quote policy
     */
    public CSVFormat withQuoteMode(final QuoteMode quoteModePolicy) {
        return new CSVFormat(delimiter, quoteCharacter, quoteModePolicy, commentMarker, escapeCharacter,
                ignoreSurroundingSpaces, ignoreEmptyLines, recordSeparator, nullString, headerComments, header,
                skipHeaderRecord, allowMissingColumnNames, ignoreHeaderCase);
    }

    /**
     * Sets the record separator of the format to the specified character.
     *
     * <p>
     * <strong>Note:</strong> This setting is only used during printing and does not affect parsing. Parsing currently
     * only works for inputs with '\n', '\r' and "\r\n"
     * </p>
     *
     * @param recordSeparator
     *            the record separator to use for output.
     *
     * @return A new CSVFormat that is equal to this but with the the specified output record separator
     */
    public CSVFormat withRecordSeparator(final char recordSeparator) {
        return withRecordSeparator(String.valueOf(recordSeparator));
    }

    /**
     * Sets the record separator of the format to the specified String.
     *
     * <p>
     * <strong>Note:</strong> This setting is only used during printing and does not affect parsing. Parsing currently
     * only works for inputs with '\n', '\r' and "\r\n"
     * </p>
     *
     * @param recordSeparator
     *            the record separator to use for output.
     *
     * @return A new CSVFormat that is equal to this but with the the specified output record separator
     * @throws IllegalArgumentException
     *             if recordSeparator is none of CR, LF or CRLF
     */
    public CSVFormat withRecordSeparator(final String recordSeparator) {
        return new CSVFormat(delimiter, quoteCharacter, quoteMode, commentMarker, escapeCharacter,
                ignoreSurroundingSpaces, ignoreEmptyLines, recordSeparator, nullString, headerComments, header,
                skipHeaderRecord, allowMissingColumnNames, ignoreHeaderCase);
    }

    /**
     * Sets skipping the header record to {@code true}.
     *
     * @return A new CSVFormat that is equal to this but with the the specified skipHeaderRecord setting.
     * @see #withSkipHeaderRecord(boolean)
     * @see #withHeader(String...)
     * @since 1.1
     */
    public CSVFormat withSkipHeaderRecord() {
        return this.withSkipHeaderRecord(true);
    }

    /**
     * Sets whether to skip the header record.
     *
     * @param skipHeaderRecord
     *            whether to skip the header record.
     *
     * @return A new CSVFormat that is equal to this but with the the specified skipHeaderRecord setting.
     * @see #withHeader(String...)
     */
    public CSVFormat withSkipHeaderRecord(final boolean skipHeaderRecord) {
        return new CSVFormat(delimiter, quoteCharacter, quoteMode, commentMarker, escapeCharacter,
                ignoreSurroundingSpaces, ignoreEmptyLines, recordSeparator, nullString, headerComments, header,
                skipHeaderRecord, allowMissingColumnNames, ignoreHeaderCase);
    }
}
