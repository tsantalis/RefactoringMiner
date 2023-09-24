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
package org.apache.commons.compress.archivers.cpio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.utils.ArchiveUtils;

/**
 * CPIOArchiveInputStream is a stream for reading cpio streams. All formats of
 * cpio are supported (old ascii, old binary, new portable format and the new
 * portable format with crc).
 * <p/>
 * <p/>
 * The stream can be read by extracting a cpio entry (containing all
 * informations about a entry) and afterwards reading from the stream the file
 * specified by the entry.
 * <p/>
 * <code><pre>
 * CPIOArchiveInputStream cpioIn = new CPIOArchiveInputStream(
 *         new FileInputStream(new File(&quot;test.cpio&quot;)));
 * CPIOArchiveEntry cpioEntry;
 * <p/>
 * while ((cpioEntry = cpioIn.getNextEntry()) != null) {
 *     System.out.println(cpioEntry.getName());
 *     int tmp;
 *     StringBuilder buf = new StringBuilder();
 *     while ((tmp = cpIn.read()) != -1) {
 *         buf.append((char) tmp);
 *     }
 *     System.out.println(buf.toString());
 * }
 * cpioIn.close();
 * </pre></code>
 * <p/>
 * Note: This implementation should be compatible to cpio 2.5
 * 
 * This class uses mutable fields and is not considered to be threadsafe.
 * 
 * Based on code from the jRPM project (jrpm.sourceforge.net)
 */

public class CpioArchiveInputStream extends ArchiveInputStream implements
        CpioConstants {

    private boolean closed = false;

    private CpioArchiveEntry entry;

    private long entryBytesRead = 0;

    private boolean entryEOF = false;

    private final byte tmpbuf[] = new byte[4096];

    private long crc = 0;

    private final InputStream in;

    // cached buffers - must only be used locally in the class (COMPRESS-172 - reduce garbage collection)
    private final byte[] TWO_BYTES_BUF = new byte[2];
    private final byte[] FOUR_BYTES_BUF = new byte[4];
    private final byte[] SIX_BYTES_BUF = new byte[6];

    private final int blockSize;

    /**
     * Construct the cpio input stream with a blocksize of {@link
     * CpioConstants#BLOCK_SIZE BLOCK_SIZE}.
     * 
     * @param in
     *            The cpio stream
     */
    public CpioArchiveInputStream(final InputStream in) {
        this(in, BLOCK_SIZE);
    }

    /**
     * Construct the cpio input stream with a blocksize of {@link CpioConstants#BLOCK_SIZE BLOCK_SIZE}.
     * Construct the cpio input stream.
     * 
     * @param in
     *            The cpio stream
     * @param blockSize
     *            The block size of the archive.
     * @since 1.5
     */
    public CpioArchiveInputStream(final InputStream in, int blockSize) {
        this.in = in;
        this.blockSize = blockSize;
    }

    /**
     * Returns 0 after EOF has reached for the current entry data, otherwise
     * always return 1.
     * <p/>
     * Programs should not count on this method to return the actual number of
     * bytes that could be read without blocking.
     * 
     * @return 1 before EOF and 0 after EOF has reached for current entry.
     * @throws IOException
     *             if an I/O error has occurred or if a CPIO file error has
     *             occurred
     */
    @Override
    public int available() throws IOException {
        ensureOpen();
        if (this.entryEOF) {
            return 0;
        }
        return 1;
    }

    /**
     * Closes the CPIO input stream.
     * 
     * @throws IOException
     *             if an I/O error has occurred
     */
    @Override
    public void close() throws IOException {
        if (!this.closed) {
            in.close();
            this.closed = true;
        }
    }

    /**
     * Closes the current CPIO entry and positions the stream for reading the
     * next entry.
     * 
     * @throws IOException
     *             if an I/O error has occurred or if a CPIO file error has
     *             occurred
     */
    private void closeEntry() throws IOException {
        ensureOpen();
        while (read(this.tmpbuf, 0, this.tmpbuf.length) != -1) { // NOPMD
            // do nothing
        }

        this.entryEOF = true;
    }

    /**
     * Check to make sure that this stream has not been closed
     * 
     * @throws IOException
     *             if the stream is already closed
     */
    private void ensureOpen() throws IOException {
        if (this.closed) {
            throw new IOException("Stream closed");
        }
    }

    /**
     * Reads the next CPIO file entry and positions stream at the beginning of
     * the entry data.
     * 
     * @return the CPIOArchiveEntry just read
     * @throws IOException
     *             if an I/O error has occurred or if a CPIO file error has
     *             occurred
     */
    public CpioArchiveEntry getNextCPIOEntry() throws IOException {
        ensureOpen();
        if (this.entry != null) {
            closeEntry();
        }
        readFully(TWO_BYTES_BUF, 0, TWO_BYTES_BUF.length);
        if (CpioUtil.byteArray2long(TWO_BYTES_BUF, false) == MAGIC_OLD_BINARY) {
            this.entry = readOldBinaryEntry(false);
        } else if (CpioUtil.byteArray2long(TWO_BYTES_BUF, true)
                   == MAGIC_OLD_BINARY) {
            this.entry = readOldBinaryEntry(true);
        } else {
            System.arraycopy(TWO_BYTES_BUF, 0, SIX_BYTES_BUF, 0,
                             TWO_BYTES_BUF.length);
            readFully(SIX_BYTES_BUF, TWO_BYTES_BUF.length,
                      FOUR_BYTES_BUF.length);
            String magicString = ArchiveUtils.toAsciiString(SIX_BYTES_BUF);
            if (magicString.equals(MAGIC_NEW)) {
                this.entry = readNewEntry(false);
            } else if (magicString.equals(MAGIC_NEW_CRC)) {
                this.entry = readNewEntry(true);
            } else if (magicString.equals(MAGIC_OLD_ASCII)) {
                this.entry = readOldAsciiEntry();
            } else {
                throw new IOException("Unknown magic [" + magicString + "]. Occured at byte: " + getBytesRead());
            }
        }

        this.entryBytesRead = 0;
        this.entryEOF = false;
        this.crc = 0;

        if (this.entry.getName().equals(CPIO_TRAILER)) {
            this.entryEOF = true;
            skipRemainderOfLastBlock();
            return null;
        }
        return this.entry;
    }

    private void skip(int bytes) throws IOException{
        // bytes cannot be more than 3 bytes
        if (bytes > 0) {
            readFully(FOUR_BYTES_BUF, 0, bytes);
        }
    }

    /**
     * Reads from the current CPIO entry into an array of bytes. Blocks until
     * some input is available.
     * 
     * @param b
     *            the buffer into which the data is read
     * @param off
     *            the start offset of the data
     * @param len
     *            the maximum number of bytes read
     * @return the actual number of bytes read, or -1 if the end of the entry is
     *         reached
     * @throws IOException
     *             if an I/O error has occurred or if a CPIO file error has
     *             occurred
     */
    @Override
    public int read(final byte[] b, final int off, final int len)
            throws IOException {
        ensureOpen();
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        if (this.entry == null || this.entryEOF) {
            return -1;
        }
        if (this.entryBytesRead == this.entry.getSize()) {
            skip(entry.getDataPadCount());
            this.entryEOF = true;
            if (this.entry.getFormat() == FORMAT_NEW_CRC
                && this.crc != this.entry.getChksum()) {
                throw new IOException("CRC Error. Occured at byte: "
                                      + getBytesRead());
            }
            return -1; // EOF for this entry
        }
        int tmplength = (int) Math.min(len, this.entry.getSize()
                - this.entryBytesRead);
        if (tmplength < 0) {
            return -1;
        }

        int tmpread = readFully(b, off, tmplength);
        if (this.entry.getFormat() == FORMAT_NEW_CRC) {
            for (int pos = 0; pos < tmpread; pos++) {
                this.crc += b[pos] & 0xFF;
            }
        }
        this.entryBytesRead += tmpread;

        return tmpread;
    }

    private final int readFully(final byte[] b, final int off, final int len)
            throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
        while (n < len) {
            int count = this.in.read(b, off + n, len - n);
            count(count);
            if (count < 0) {
                throw new EOFException();
            }
            n += count;
        }
        return n;
    }

    private long readBinaryLong(final int length, final boolean swapHalfWord)
            throws IOException {
        byte tmp[] = new byte[length];
        readFully(tmp, 0, tmp.length);
        return CpioUtil.byteArray2long(tmp, swapHalfWord);
    }

    private long readAsciiLong(final int length, final int radix)
            throws IOException {
        byte tmpBuffer[] = new byte[length];
        readFully(tmpBuffer, 0, tmpBuffer.length);
        return Long.parseLong(ArchiveUtils.toAsciiString(tmpBuffer), radix);
    }

    private CpioArchiveEntry readNewEntry(final boolean hasCrc)
            throws IOException {
        CpioArchiveEntry ret;
        if (hasCrc) {
            ret = new CpioArchiveEntry(FORMAT_NEW_CRC);
        } else {
            ret = new CpioArchiveEntry(FORMAT_NEW);
        }

        ret.setInode(readAsciiLong(8, 16));
        long mode = readAsciiLong(8, 16);
        if (mode != 0){
            ret.setMode(mode);
        }
        ret.setUID(readAsciiLong(8, 16));
        ret.setGID(readAsciiLong(8, 16));
        ret.setNumberOfLinks(readAsciiLong(8, 16));
        ret.setTime(readAsciiLong(8, 16));
        ret.setSize(readAsciiLong(8, 16));
        ret.setDeviceMaj(readAsciiLong(8, 16));
        ret.setDeviceMin(readAsciiLong(8, 16));
        ret.setRemoteDeviceMaj(readAsciiLong(8, 16));
        ret.setRemoteDeviceMin(readAsciiLong(8, 16));
        long namesize = readAsciiLong(8, 16);
        ret.setChksum(readAsciiLong(8, 16));
        String name = readCString((int) namesize);
        ret.setName(name);
        if (mode == 0 && !name.equals(CPIO_TRAILER)){
            throw new IOException("Mode 0 only allowed in the trailer. Found entry name: "+name + " Occured at byte: " + getBytesRead());
        }
        skip(ret.getHeaderPadCount());

        return ret;
    }

    private CpioArchiveEntry readOldAsciiEntry() throws IOException {
        CpioArchiveEntry ret = new CpioArchiveEntry(FORMAT_OLD_ASCII);

        ret.setDevice(readAsciiLong(6, 8));
        ret.setInode(readAsciiLong(6, 8));
        final long mode = readAsciiLong(6, 8);
        if (mode != 0) {
            ret.setMode(mode);
        }
        ret.setUID(readAsciiLong(6, 8));
        ret.setGID(readAsciiLong(6, 8));
        ret.setNumberOfLinks(readAsciiLong(6, 8));
        ret.setRemoteDevice(readAsciiLong(6, 8));
        ret.setTime(readAsciiLong(11, 8));
        long namesize = readAsciiLong(6, 8);
        ret.setSize(readAsciiLong(11, 8));
        final String name = readCString((int) namesize);
        ret.setName(name);
        if (mode == 0 && !name.equals(CPIO_TRAILER)){
            throw new IOException("Mode 0 only allowed in the trailer. Found entry: "+ name + " Occured at byte: " + getBytesRead());
        }

        return ret;
    }

    private CpioArchiveEntry readOldBinaryEntry(final boolean swapHalfWord)
            throws IOException {
        CpioArchiveEntry ret = new CpioArchiveEntry(FORMAT_OLD_BINARY);

        ret.setDevice(readBinaryLong(2, swapHalfWord));
        ret.setInode(readBinaryLong(2, swapHalfWord));
        final long mode = readBinaryLong(2, swapHalfWord);
        if (mode != 0){
            ret.setMode(mode);
        }
        ret.setUID(readBinaryLong(2, swapHalfWord));
        ret.setGID(readBinaryLong(2, swapHalfWord));
        ret.setNumberOfLinks(readBinaryLong(2, swapHalfWord));
        ret.setRemoteDevice(readBinaryLong(2, swapHalfWord));
        ret.setTime(readBinaryLong(4, swapHalfWord));
        long namesize = readBinaryLong(2, swapHalfWord);
        ret.setSize(readBinaryLong(4, swapHalfWord));
        final String name = readCString((int) namesize);
        ret.setName(name);
        if (mode == 0 && !name.equals(CPIO_TRAILER)){
            throw new IOException("Mode 0 only allowed in the trailer. Found entry: "+name + "Occured at byte: " + getBytesRead());
        }
        skip(ret.getHeaderPadCount());

        return ret;
    }

    private String readCString(final int length) throws IOException {
        byte tmpBuffer[] = new byte[length];
        readFully(tmpBuffer, 0, tmpBuffer.length);
        return new String(tmpBuffer, 0, tmpBuffer.length - 1); // TODO default charset?
    }

    /**
     * Skips specified number of bytes in the current CPIO entry.
     * 
     * @param n
     *            the number of bytes to skip
     * @return the actual number of bytes skipped
     * @throws IOException
     *             if an I/O error has occurred
     * @throws IllegalArgumentException
     *             if n < 0
     */
    @Override
    public long skip(final long n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException("negative skip length");
        }
        ensureOpen();
        int max = (int) Math.min(n, Integer.MAX_VALUE);
        int total = 0;

        while (total < max) {
            int len = max - total;
            if (len > this.tmpbuf.length) {
                len = this.tmpbuf.length;
            }
            len = read(this.tmpbuf, 0, len);
            if (len == -1) {
                this.entryEOF = true;
                break;
            }
            total += len;
        }
        return total;
    }

    @Override
    public CpioArchiveEntry getNextEntry() throws IOException {
        return getNextCPIOEntry();
    }

    /**
     * Skips the padding zeros written after the TRAILER!!! entry.
     */
    private void skipRemainderOfLastBlock() throws IOException {
        long readFromLastBlock = getBytesRead() % blockSize;
        long remainingBytes = readFromLastBlock == 0 ? 0
            : blockSize - readFromLastBlock;
        while (remainingBytes > 0) {
            long skipped = skip(blockSize - readFromLastBlock);
            if (skipped <= 0) {
                break;
            }
            remainingBytes -= skipped;
        }
    }

    /**
     * Checks if the signature matches one of the following magic values:
     * 
     * Strings:
     *
     * "070701" - MAGIC_NEW
     * "070702" - MAGIC_NEW_CRC
     * "070707" - MAGIC_OLD_ASCII
     * 
     * Octal Binary value:
     * 
     * 070707 - MAGIC_OLD_BINARY (held as a short) = 0x71C7 or 0xC771
     */
    public static boolean matches(byte[] signature, int length) {
        if (length < 6) {
            return false;
        }

        // Check binary values
        if (signature[0] == 0x71 && (signature[1] & 0xFF) == 0xc7) {
            return true;
        }
        if (signature[1] == 0x71 && (signature[0] & 0xFF) == 0xc7) {
            return true;
        }

        // Check Ascii (String) values
        // 3037 3037 30nn
        if (signature[0] != 0x30) {
            return false;
        }
        if (signature[1] != 0x37) {
            return false;
        }
        if (signature[2] != 0x30) {
            return false;
        }
        if (signature[3] != 0x37) {
            return false;
        }
        if (signature[4] != 0x30) {
            return false;
        }
        // Check last byte
        if (signature[5] == 0x31) {
            return true;
        }
        if (signature[5] == 0x32) {
            return true;
        }
        if (signature[5] == 0x37) {
            return true;
        }

        return false;
    }
}
