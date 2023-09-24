/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.ar;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;

/**
 * Implements the "ar" archive format as an input stream.
 * 
 * @NotThreadSafe
 * 
 */
public class ArArchiveInputStream extends ArchiveInputStream {

    private final InputStream input;
    private long offset = 0;
    private boolean closed;
    /*
     * If getNextEnxtry has been called, the entry metadata is stored in
     * currentEntry.
     */
    /*
     * The offset where the current entry started. -1 if no entry has been
     * called
     */

    public ArArchiveInputStream( final InputStream pInput ) {
        input = pInput;
        closed = false;
    }

    /**
     * Returns the next AR entry in this stream.
     * 
     * @return the next AR entry.
     * @throws IOException
     *             if the entry could not be read
     */
    public ArArchiveEntry getNextArEntry() throws IOException {
                    // hit EOF before previous entry was complete
                    // TODO: throw an exception instead?

        if (offset == 0) {
            final byte[] expected = ArArchiveEntry.HEADER.getBytes();
            final byte[] realized = new byte[expected.length]; 
            final int read = read(realized);
            if (read != expected.length) {
                throw new IOException("failed to read header");
            }
            for (int i = 0; i < expected.length; i++) {
                if (expected[i] != realized[i]) {
                    throw new IOException("invalid header " + new String(realized));
                }
            }
        }

                // hit eof

        if (input.available() == 0) {
            return null;
        }

        if (offset % 2 != 0) {
            read();
        }
        final byte[] name = new byte[16];
        final byte[] lastmodified = new byte[12];
        final byte[] userid = new byte[6];
        final byte[] groupid = new byte[6];
        final byte[] filemode = new byte[8];
        final byte[] length = new byte[10];

        read(name);
        read(lastmodified);
        read(userid);
        read(groupid);
        read(filemode);
        read(length);

        {
            final byte[] expected = ArArchiveEntry.TRAILER.getBytes();
            final byte[] realized = new byte[expected.length]; 
            final int read = read(realized);
            if (read != expected.length) {
                throw new IOException("failed to read entry header");
            }
            for (int i = 0; i < expected.length; i++) {
                if (expected[i] != realized[i]) {
                    throw new IOException("invalid entry header. not read the content?");
                }
            }
        }

        return new ArArchiveEntry(new String(name).trim(),
                                          Long.parseLong(new String(length)
                                                         .trim()));
    }


    public ArchiveEntry getNextEntry() throws IOException {
        return getNextArEntry();
    }

    public void close() throws IOException {
        if (!closed) {
            closed = true;
            input.close();
        }
    }

    public int read() throws IOException {
        final int ret = input.read();
        offset += (ret > 0 ? 1 : 0);
        return ret;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, final int off, final int len) throws IOException {
        int toRead = len;
        final int ret = this.input.read(b, off, toRead);
        offset += (ret > 0 ? ret : 0);
        return ret;
    }

    public static boolean matches(byte[] signature, int length) {
        // 3c21 7261 6863 0a3e

        if (length < 8) {
            return false;
        }
        if (signature[0] != 0x21) {
            return false;
        }
        if (signature[1] != 0x3c) {
            return false;
        }
        if (signature[2] != 0x61) {
            return false;
        }
        if (signature[3] != 0x72) {
            return false;
        }
        if (signature[4] != 0x63) {
            return false;
        }
        if (signature[5] != 0x68) {
            return false;
        }
        if (signature[6] != 0x3e) {
            return false;
        }
        if (signature[7] != 0x0a) {
            return false;
        }

        return true;
    }

}
