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

import static org.apache.commons.csv.Constants.END_OF_STREAM;
import static org.apache.commons.csv.Constants.UNDEFINED;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.StringReader;

import org.junit.Test;

/**
 *
 */
public class ExtendedBufferedReaderTest {

    @Test
    public void testEmptyInput() throws Exception {
        try (final ExtendedBufferedReader br = createBufferedReader("")) {
            assertEquals(END_OF_STREAM, br.read());
            assertEquals(END_OF_STREAM, br.lookAhead());
            assertEquals(END_OF_STREAM, br.getLastChar());
            assertNull(br.readLine());
            assertEquals(0, br.read(new char[10], 0, 0));
        }
    }

    @Test
    public void testReadLookahead1() throws Exception {
        try (final ExtendedBufferedReader br = createBufferedReader("1\n2\r3\n")) {
            assertEquals(0, br.getCurrentLineNumber());
            assertEquals('1', br.lookAhead());
            assertEquals(UNDEFINED, br.getLastChar());
            assertEquals(0, br.getCurrentLineNumber());
            assertEquals('1', br.read()); // Start line 1
            assertEquals('1', br.getLastChar());

            assertEquals(1, br.getCurrentLineNumber());
            assertEquals('\n', br.lookAhead());
            assertEquals(1, br.getCurrentLineNumber());
            assertEquals('1', br.getLastChar());
            assertEquals('\n', br.read());
            assertEquals(1, br.getCurrentLineNumber());
            assertEquals('\n', br.getLastChar());
            assertEquals(1, br.getCurrentLineNumber());

            assertEquals('2', br.lookAhead());
            assertEquals(1, br.getCurrentLineNumber());
            assertEquals('\n', br.getLastChar());
            assertEquals(1, br.getCurrentLineNumber());
            assertEquals('2', br.read()); // Start line 2
            assertEquals(2, br.getCurrentLineNumber());
            assertEquals('2', br.getLastChar());

            assertEquals('\r', br.lookAhead());
            assertEquals(2, br.getCurrentLineNumber());
            assertEquals('2', br.getLastChar());
            assertEquals('\r', br.read());
            assertEquals('\r', br.getLastChar());
            assertEquals(2, br.getCurrentLineNumber());

            assertEquals('3', br.lookAhead());
            assertEquals('\r', br.getLastChar());
            assertEquals('3', br.read()); // Start line 3
            assertEquals('3', br.getLastChar());
            assertEquals(3, br.getCurrentLineNumber());

            assertEquals('\n', br.lookAhead());
            assertEquals(3, br.getCurrentLineNumber());
            assertEquals('3', br.getLastChar());
            assertEquals('\n', br.read());
            assertEquals(3, br.getCurrentLineNumber());
            assertEquals('\n', br.getLastChar());
            assertEquals(3, br.getCurrentLineNumber());

            assertEquals(END_OF_STREAM, br.lookAhead());
            assertEquals('\n', br.getLastChar());
            assertEquals(END_OF_STREAM, br.read());
            assertEquals(END_OF_STREAM, br.getLastChar());
            assertEquals(END_OF_STREAM, br.read());
            assertEquals(END_OF_STREAM, br.lookAhead());
            assertEquals(3, br.getCurrentLineNumber());

        }
    }

    @Test
    public void testReadLookahead2() throws Exception {
        final char[] ref = new char[5];
        final char[] res = new char[5];

        try (final ExtendedBufferedReader br = createBufferedReader("abcdefg")) {
            ref[0] = 'a';
            ref[1] = 'b';
            ref[2] = 'c';
            assertEquals(3, br.read(res, 0, 3));
            assertArrayEquals(ref, res);
            assertEquals('c', br.getLastChar());

            assertEquals('d', br.lookAhead());
            ref[4] = 'd';
            assertEquals(1, br.read(res, 4, 1));
            assertArrayEquals(ref, res);
            assertEquals('d', br.getLastChar());
        }
    }

    @Test
    public void testReadLine() throws Exception {
        try (final ExtendedBufferedReader br = createBufferedReader("")) {
            assertNull(br.readLine());
        }
        try (final ExtendedBufferedReader br = createBufferedReader("\n")) {
            assertEquals("", br.readLine());
            assertNull(br.readLine());
        }
        try (final ExtendedBufferedReader br = createBufferedReader("foo\n\nhello")) {
            assertEquals(0, br.getCurrentLineNumber());
            assertEquals("foo", br.readLine());
            assertEquals(1, br.getCurrentLineNumber());
            assertEquals("", br.readLine());
            assertEquals(2, br.getCurrentLineNumber());
            assertEquals("hello", br.readLine());
            assertEquals(3, br.getCurrentLineNumber());
            assertNull(br.readLine());
            assertEquals(3, br.getCurrentLineNumber());
        }
        try (final ExtendedBufferedReader br = createBufferedReader("foo\n\nhello")) {
            assertEquals('f', br.read());
            assertEquals('o', br.lookAhead());
            assertEquals("oo", br.readLine());
            assertEquals(1, br.getCurrentLineNumber());
            assertEquals('\n', br.lookAhead());
            assertEquals("", br.readLine());
            assertEquals(2, br.getCurrentLineNumber());
            assertEquals('h', br.lookAhead());
            assertEquals("hello", br.readLine());
            assertNull(br.readLine());
            assertEquals(3, br.getCurrentLineNumber());
        }
        try (final ExtendedBufferedReader br = createBufferedReader("foo\rbaar\r\nfoo")) {
            assertEquals("foo", br.readLine());
            assertEquals('b', br.lookAhead());
            assertEquals("baar", br.readLine());
            assertEquals('f', br.lookAhead());
            assertEquals("foo", br.readLine());
            assertNull(br.readLine());
        }
    }

    /*
     * Test to illustrate https://issues.apache.org/jira/browse/CSV-75
     *
     */
    @Test
    public void testReadChar() throws Exception {
        final String LF = "\n";
        final String CR = "\r";
        final String CRLF = CR + LF;
        final String LFCR = LF + CR;// easier to read the string below
        final String test = "a" + LF + "b" + CR + "c" + LF + LF + "d" + CR + CR + "e" + LFCR + "f " + CRLF;
        // EOL eol EOL EOL eol eol EOL+CR EOL
        final int EOLeolct = 9;

        try (final ExtendedBufferedReader br = createBufferedReader(test)) {
            assertEquals(0, br.getCurrentLineNumber());
            while (br.readLine() != null) {
                // consume all
            }
            assertEquals(EOLeolct, br.getCurrentLineNumber());
        }
        try (final ExtendedBufferedReader br = createBufferedReader(test)) {
            assertEquals(0, br.getCurrentLineNumber());
            while (br.read() != -1) {
                // consume all
            }
            assertEquals(EOLeolct, br.getCurrentLineNumber());
        }
        try (final ExtendedBufferedReader br = createBufferedReader(test)) {
            assertEquals(0, br.getCurrentLineNumber());
            final char[] buff = new char[10];
            while (br.read(buff, 0, 3) != -1) {
                // consume all
            }
            assertEquals(EOLeolct, br.getCurrentLineNumber());
        }
    }

    private ExtendedBufferedReader createBufferedReader(final String s) {
        return new ExtendedBufferedReader(new StringReader(s));
    }
}
