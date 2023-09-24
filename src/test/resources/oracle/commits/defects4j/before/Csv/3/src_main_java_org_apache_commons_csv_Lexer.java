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

import static org.apache.commons.csv.Constants.BACKSPACE;
import static org.apache.commons.csv.Constants.CR;
import static org.apache.commons.csv.Constants.END_OF_STREAM;
import static org.apache.commons.csv.Constants.FF;
import static org.apache.commons.csv.Constants.LF;
import static org.apache.commons.csv.Constants.TAB;
import static org.apache.commons.csv.Constants.UNDEFINED;

import java.io.IOException;

/**
 * Abstract lexer class; contains common utility routines shared by lexers
 *
 * @version $Id$
 */
abstract class Lexer {

    /**
     * Constant char to use for disabling comments, escapes and encapsulation. The value -2 is used because it
     * won't be confused with an EOF signal (-1), and because the Unicode value {@code FFFE} would be encoded as two
     * chars (using surrogates) and thus there should never be a collision with a real text char.
     */
    private static final char DISABLED = '\ufffe';

    private final char delimiter;
    private final char escape;
    private final char quoteChar;
    private final char commmentStart;

    final boolean ignoreSurroundingSpaces;
    final boolean ignoreEmptyLines;

    final CSVFormat format;

    /** The input stream */
    final ExtendedBufferedReader in;

    Lexer(final CSVFormat format, final ExtendedBufferedReader in) {
        this.format = format;
        this.in = in;
        this.delimiter = format.getDelimiter();
        this.escape = mapNullToDisabled(format.getEscape());
        this.quoteChar = mapNullToDisabled(format.getQuoteChar());
        this.commmentStart = mapNullToDisabled(format.getCommentStart());
        this.ignoreSurroundingSpaces = format.getIgnoreSurroundingSpaces();
        this.ignoreEmptyLines = format.getIgnoreEmptyLines();
    }

    private final char mapNullToDisabled(final Character c) {
        return c == null ? DISABLED : c.charValue();
    }

    long getLineNumber() {
        return in.getLineNumber();
    }

    // TODO escape handling needs more work
    /**
     * Handle an escape sequence.
     * The current character must be the escape character.
     * On return, the next character is available by calling {@link ExtendedBufferedReader#getLastChar()}
     * on the input stream.
     * 
     * @return the unescaped character (as an int) or {@link END_OF_STREAM} if char following the escape is invalid. 
     * @throws IOException if there is a problem reading the stream or the end of stream is detected: 
     * the escape character is not allowed at end of strem
     */
    int readEscape() throws IOException {
        // the escape char has just been read (normally a backslash)
        final int c = in.read();
        switch (c) {
        case 'r':
            return CR;
        case 'n':
            return LF;
        case 't':
            return TAB;
        case 'b':
            return BACKSPACE;
        case 'f':
            return FF;
        case CR:
        case LF:
        case FF: // TODO is this correct?
        case TAB: // TODO is this correct? Do tabs need to be escaped?
        case BACKSPACE: // TODO is this correct?
            return c;
        case END_OF_STREAM:
            throw new IOException("EOF whilst processing escape sequence");
        default:
            // Now check for meta-characters
                return c;
            // indicate unexpected char - available from in.getLastChar()
        }
    }

    void trimTrailingSpaces(final StringBuilder buffer) {
        int length = buffer.length();
        while (length > 0 && Character.isWhitespace(buffer.charAt(length - 1))) {
            length = length - 1;
        }
        if (length != buffer.length()) {
            buffer.setLength(length);
        }
    }

    /**
     * Greedily accepts \n, \r and \r\n This checker consumes silently the second control-character...
     *
     * @return true if the given or next character is a line-terminator
     */
    boolean readEndOfLine(int c) throws IOException {
        // check if we have \r\n...
        if (c == CR && in.lookAhead() == LF) {
            // note: does not change c outside of this method!
            c = in.read();
        }
        return c == LF || c == CR;
    }

    abstract Token nextToken(Token reusableToken) throws IOException;

    /**
     * @return true if the given char is a whitespace character
     */
    boolean isWhitespace(final int c) {
        return c != format.getDelimiter() && Character.isWhitespace((char) c);
    }

    /**
     * Checks if the current character represents the start of a line: a CR, LF or is at the start of the file.
     *
     * @param c the character to check
     * @return true if the character is at the start of a line.
     */
    boolean isStartOfLine(final int c) {
        return c == LF || c == CR || c == UNDEFINED;
    }

    /**
     * @return true if the given character indicates end of file
     */
    boolean isEndOfFile(final int c) {
        return c == END_OF_STREAM;
    }

    boolean isDelimiter(final int c) {
        return c == delimiter;
    }

    boolean isEscape(final int c) {
        return c == escape;
    }

    boolean isQuoteChar(final int c) {
        return c == quoteChar;
    }

    boolean isCommentStart(final int c) {
        return c == commmentStart;
    }
}
