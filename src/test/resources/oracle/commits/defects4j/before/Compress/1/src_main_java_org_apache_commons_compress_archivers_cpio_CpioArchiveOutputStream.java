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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;

/**
 * CPIOArchiveOutputStream is a stream for writing CPIO streams. All formats of
 * CPIO are supported (old ASCII, old binary, new portable format and the new
 * portable format with CRC).
 * <p/>
 * <p/>
 * An entry can be written by creating an instance of CpioArchiveEntry and fill
 * it with the necessary values and put it into the CPIO stream. Afterwards
 * write the contents of the file into the CPIO stream. Either close the stream
 * by calling finish() or put a next entry into the cpio stream.
 * <p/>
 * <code><pre>
 * CpioArchiveOutputStream out = new CpioArchiveOutputStream(
 *         new FileOutputStream(new File("test.cpio")));
 * CpioArchiveEntry entry = new CpioArchiveEntry();
 * entry.setName(&quot;testfile&quot;);
 * String contents = &quot;12345&quot;;
 * entry.setFileSize(contents.length());
 * out.putNextEntry(entry);
 * out.write(testContents.getBytes());
 * out.close();
 * </pre></code>
 * <p/>
 * Note: This implementation should be compatible to cpio 2.5
 * 
 * This class uses mutable fields and is not considered threadsafe.
 * 
 * based on code from the jRPM project (jrpm.sourceforge.net)
 */
public class CpioArchiveOutputStream extends ArchiveOutputStream implements
        CpioConstants {

    private CpioArchiveEntry cpioEntry;

    private boolean closed = false;

    private boolean finished;

    private short entryFormat = FORMAT_NEW;

    private final HashMap names = new HashMap();

    private long crc = 0;

    private long written;

    private final OutputStream out;

    /**
     * Construct the cpio output stream with a specified format
     * 
     * @param out
     *            The cpio stream
     * @param format
     *            The format of the stream
     */
    public CpioArchiveOutputStream(final OutputStream out, final short format) {
        this.out = new FilterOutputStream(out);
        setFormat(format);
    }

    /**
     * Construct the cpio output stream. The format for this CPIO stream is the
     * "new" format
     * 
     * @param out
     *            The cpio stream
     */
    public CpioArchiveOutputStream(final OutputStream out) {
        this(out, FORMAT_NEW);
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
     * Set a default header format. This will be used if no format is defined in
     * the cpioEntry given to putNextEntry().
     * 
     * @param format
     *            A CPIO format
     */
    private void setFormat(final short format) {
        switch (format) {
        case FORMAT_NEW:
        case FORMAT_NEW_CRC:
        case FORMAT_OLD_ASCII:
        case FORMAT_OLD_BINARY:
            break;
        default:
            throw new IllegalArgumentException("Unknown header type");

        }
        synchronized (this) {
            this.entryFormat = format;
        }
    }

    /**
     * Begins writing a new CPIO file entry and positions the stream to the
     * start of the entry data. Closes the current entry if still active. The
     * current time will be used if the entry has no set modification time and
     * the default header format will be used if no other format is specified in
     * the entry.
     * 
     * @param e
     *            the CPIO cpioEntry to be written
     * @throws IOException
     *             if an I/O error has occurred or if a CPIO file error has
     *             occurred
     */
    public void putNextEntry(final CpioArchiveEntry e) throws IOException {
        ensureOpen();
        if (this.cpioEntry != null) {
            closeArchiveEntry(); // close previous entry
        }
        if (e.getTime() == -1) {
            e.setTime(System.currentTimeMillis());
        }

        // TODO what happens if an entry has an other format than the
        // outputstream?
        if (e.getFormat() == -1) {
            e.setFormat(this.entryFormat);
        }

        if (this.names.put(e.getName(), e) != null) {
            throw new IOException("duplicate entry: " + e.getName());
        }

        writeHeader(e);
        this.cpioEntry = e;
        this.written = 0;
    }

    private void writeHeader(final CpioArchiveEntry e) throws IOException {
        switch (e.getFormat()) {
        case FORMAT_NEW:
            out.write(MAGIC_NEW.getBytes());
            writeNewEntry(e);
            break;
        case FORMAT_NEW_CRC:
            out.write(MAGIC_NEW_CRC.getBytes());
            writeNewEntry(e);
            break;
        case FORMAT_OLD_ASCII:
            out.write(MAGIC_OLD_ASCII.getBytes());
            writeOldAsciiEntry(e);
            break;
        case FORMAT_OLD_BINARY:
            boolean swapHalfWord = true;
            writeBinaryLong(MAGIC_OLD_BINARY, 2, swapHalfWord);
            writeOldBinaryEntry(e, swapHalfWord);
            break;
        }
    }

    private void writeNewEntry(final CpioArchiveEntry entry) throws IOException {
        writeAsciiLong(entry.getInode(), 8, 16);
        writeAsciiLong(entry.getMode(), 8, 16);
        writeAsciiLong(entry.getUID(), 8, 16);
        writeAsciiLong(entry.getGID(), 8, 16);
        writeAsciiLong(entry.getNumberOfLinks(), 8, 16);
        writeAsciiLong(entry.getTime(), 8, 16);
        writeAsciiLong(entry.getSize(), 8, 16);
        writeAsciiLong(entry.getDeviceMaj(), 8, 16);
        writeAsciiLong(entry.getDeviceMin(), 8, 16);
        writeAsciiLong(entry.getRemoteDeviceMaj(), 8, 16);
        writeAsciiLong(entry.getRemoteDeviceMin(), 8, 16);
        writeAsciiLong(entry.getName().length() + 1, 8, 16);
        writeAsciiLong(entry.getChksum(), 8, 16);
        writeCString(entry.getName());
        pad(entry.getHeaderSize() + entry.getName().length() + 1, 4);
    }

    private void writeOldAsciiEntry(final CpioArchiveEntry entry)
            throws IOException {
        writeAsciiLong(entry.getDevice(), 6, 8);
        writeAsciiLong(entry.getInode(), 6, 8);
        writeAsciiLong(entry.getMode(), 6, 8);
        writeAsciiLong(entry.getUID(), 6, 8);
        writeAsciiLong(entry.getGID(), 6, 8);
        writeAsciiLong(entry.getNumberOfLinks(), 6, 8);
        writeAsciiLong(entry.getRemoteDevice(), 6, 8);
        writeAsciiLong(entry.getTime(), 11, 8);
        writeAsciiLong(entry.getName().length() + 1, 6, 8);
        writeAsciiLong(entry.getSize(), 11, 8);
        writeCString(entry.getName());
    }

    private void writeOldBinaryEntry(final CpioArchiveEntry entry,
            final boolean swapHalfWord) throws IOException {
        writeBinaryLong(entry.getDevice(), 2, swapHalfWord);
        writeBinaryLong(entry.getInode(), 2, swapHalfWord);
        writeBinaryLong(entry.getMode(), 2, swapHalfWord);
        writeBinaryLong(entry.getUID(), 2, swapHalfWord);
        writeBinaryLong(entry.getGID(), 2, swapHalfWord);
        writeBinaryLong(entry.getNumberOfLinks(), 2, swapHalfWord);
        writeBinaryLong(entry.getRemoteDevice(), 2, swapHalfWord);
        writeBinaryLong(entry.getTime(), 4, swapHalfWord);
        writeBinaryLong(entry.getName().length() + 1, 2, swapHalfWord);
        writeBinaryLong(entry.getSize(), 4, swapHalfWord);
        writeCString(entry.getName());
        pad(entry.getHeaderSize() + entry.getName().length() + 1, 2);
    }

    /*(non-Javadoc)
     * 
     * @see
     * org.apache.commons.compress.archivers.ArchiveOutputStream#closeArchiveEntry
     * ()
     */
    public void closeArchiveEntry() throws IOException {
        ensureOpen();

        if (this.cpioEntry.getSize() != this.written) {
            throw new IOException("invalid entry size (expected "
                    + this.cpioEntry.getSize() + " but got " + this.written
                    + " bytes)");
        }
        if ((this.cpioEntry.getFormat() | FORMAT_NEW_MASK) == FORMAT_NEW_MASK) {
            pad(this.cpioEntry.getSize(), 4);
        } else if ((this.cpioEntry.getFormat() | FORMAT_OLD_BINARY) == FORMAT_OLD_BINARY) {
            pad(this.cpioEntry.getSize(), 2);
        }
        if ((this.cpioEntry.getFormat() | FORMAT_NEW_CRC) == FORMAT_NEW_CRC) {
            if (this.crc != this.cpioEntry.getChksum()) {
                throw new IOException("CRC Error");
            }
        }
        this.cpioEntry = null;
        this.crc = 0;
        this.written = 0;
    }

    /**
     * Writes an array of bytes to the current CPIO entry data. This method will
     * block until all the bytes are written.
     * 
     * @param b
     *            the data to be written
     * @param off
     *            the start offset in the data
     * @param len
     *            the number of bytes that are written
     * @throws IOException
     *             if an I/O error has occurred or if a CPIO file error has
     *             occurred
     */
    public void write(final byte[] b, final int off, final int len)
            throws IOException {
        ensureOpen();
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        if (this.cpioEntry == null) {
            throw new IOException("no current CPIO entry");
        }
        if (this.written + len > this.cpioEntry.getSize()) {
            throw new IOException("attempt to write past end of STORED entry");
        }
        out.write(b, off, len);
        this.written += len;
        if ((this.cpioEntry.getFormat() | FORMAT_NEW_CRC) == FORMAT_NEW_CRC) {
            for (int pos = 0; pos < len; pos++) {
                this.crc += b[pos] & 0xFF;
            }
        }
    }

    /**
     * Finishes writing the contents of the CPIO output stream without closing
     * the underlying stream. Use this method when applying multiple filters in
     * succession to the same output stream.
     * 
     * @throws IOException
     *             if an I/O exception has occurred or if a CPIO file error has
     *             occurred
     */
    public void finish() throws IOException {
        ensureOpen();

        if (this.finished) {
            return;
        }
        if (this.cpioEntry != null) {
            closeArchiveEntry();
        }
        this.cpioEntry = new CpioArchiveEntry(this.entryFormat);
        this.cpioEntry.setMode(0);
        this.cpioEntry.setName("TRAILER!!!");
        this.cpioEntry.setNumberOfLinks(1);
        writeHeader(this.cpioEntry);
        closeArchiveEntry();
    }

    /**
     * Closes the CPIO output stream as well as the stream being filtered.
     * 
     * @throws IOException
     *             if an I/O error has occurred or if a CPIO file error has
     *             occurred
     */
    public void close() throws IOException {
        if (!this.closed) {
            super.close();
            this.closed = true;
        }
    }

    private void pad(final long count, final int border) throws IOException {
        long skip = count % border;
        if (skip > 0) {
            byte tmp[] = new byte[(int) (border - skip)];
            out.write(tmp);
        }
    }

    private void writeBinaryLong(final long number, final int length,
            final boolean swapHalfWord) throws IOException {
        byte tmp[] = CpioUtil.long2byteArray(number, length, swapHalfWord);
        out.write(tmp);
    }

    private void writeAsciiLong(final long number, final int length,
            final int radix) throws IOException {
        StringBuffer tmp = new StringBuffer();
        String tmpStr;
        if (radix == 16) {
            tmp.append(Long.toHexString(number));
        } else if (radix == 8) {
            tmp.append(Long.toOctalString(number));
        } else {
            tmp.append(Long.toString(number));
        }

        if (tmp.length() <= length) {
            long insertLength = length - tmp.length();
            for (int pos = 0; pos < insertLength; pos++) {
                tmp.insert(0, "0");
            }
            tmpStr = tmp.toString();
        } else {
            tmpStr = tmp.substring(tmp.length() - length);
        }
        out.write(tmpStr.getBytes());
    }

    private void writeCString(final String str) throws IOException {
        out.write(str.getBytes());
        out.write('\0');
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.commons.compress.archivers.ArchiveOutputStream#putArchiveEntry
     * (org.apache.commons.compress.archivers.ArchiveEntry)
     */
    public void putArchiveEntry(ArchiveEntry entry) throws IOException {
        this.putNextEntry((CpioArchiveEntry) entry);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.OutputStream#write(int)
     */
    public void write(int b) throws IOException {
        out.write(b);
    }
}
