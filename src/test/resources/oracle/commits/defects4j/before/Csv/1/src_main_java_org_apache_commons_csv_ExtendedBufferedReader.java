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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * ExtendedBufferedReader
 *
 * A special reader decorator which supports more
 * sophisticated access to the underlying reader object.
 *
 * In particular the reader supports a look-ahead option,
 * which allows you to see the next char returned by
 * next().
 */
class ExtendedBufferedReader extends BufferedReader {

    /** The end of stream symbol */
    static final int END_OF_STREAM = -1;

    /** Undefined state for the lookahead char */
    static final int UNDEFINED = -2;

    /** The last char returned */
    private int lastChar = UNDEFINED;

    /** The line counter */
    private int lineCounter = 0;

    /**
     * Created extended buffered reader using default buffer-size
     */
    ExtendedBufferedReader(Reader r) {
        super(r);
    }

    @Override
    public int read() throws IOException {
        int current = super.read();
        if (current == '\n') {
            lineCounter++;
        }
        lastChar = current;
        return lastChar;
    }

    /**
     * Returns the last character that was read as an integer (0 to 65535). This
     * will be the last character returned by any of the read methods. This will
     * not include a character read using the {@link #peek()} method. If no
     * character has been read then this will return {@link #UNDEFINED}. If the
     * end of the stream was reached on the last read then this will return
     * {@link #END_OF_STREAM}.
     * 
     * @return the last character that was read
     */
    int readAgain() {
        return lastChar;
    }

    @Override
    public int read(char[] buf, int offset, int length) throws IOException {
        if (length == 0) {
            return 0;
        }
        
        int len = super.read(buf, offset, length);
        
        if (len > 0) {

            for (int i = offset; i < offset + len; i++) {
                char ch = buf[i];
                if (ch == '\n') {
                    if ('\r' != (i > 0 ? buf[i-1]: lastChar)) {
                        lineCounter++;                        
                    }
                } else if (ch == '\r') {
                    lineCounter++;
                }
            }

            lastChar = buf[offset + len - 1];

        } else if (len == -1) {
            lastChar = END_OF_STREAM;
        }
        
        return len;
    }

    /**
     * Calls {@link BufferedReader#readLine()} which drops the line terminator(s).
     * This method should only be called when processing a comment, otherwise
     * information can be lost.
     * <p>
     * Increments  {@link #lineCounter}
     * <p>
     * Sets {@link #lastChar} to {@link #END_OF_STREAM} at EOF, 
     * otherwise to last character on the line (won't be CR or LF) 
     * 
     * @return the line that was read, or null if reached EOF.
     */
    @Override
    public String readLine() throws IOException {
        String line = super.readLine();

        if (line != null) {
            if (line.length() > 0) {
                lastChar = line.charAt(line.length() - 1);
            }
            lineCounter++;
        } else {
            lastChar = END_OF_STREAM;
        }

        return line;
    }

    /**
     * Returns the next character in the current reader without consuming it. So
     * the next call to {@link #read()} will still return this value.
     * 
     * @return the next character
     * 
     * @throws IOException if there is an error in reading
     */
    int lookAhead() throws IOException {
        super.mark(1);
        int c = super.read();
        super.reset();

        return c;
    }

    /**
     * Returns the nof line read
     *
     * @return the current-line-number (or -1)
     */
    int getLineNumber() {
        return lineCounter;
    }
}
