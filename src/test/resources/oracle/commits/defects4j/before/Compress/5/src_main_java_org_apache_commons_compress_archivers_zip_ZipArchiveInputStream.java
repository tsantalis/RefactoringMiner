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
package org.apache.commons.compress.archivers.zip;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;

/**
 * Implements an input stream that can read Zip archives.
 * <p>
 * Note that {@link ZipArchiveEntry#getSize()} may return -1 if the DEFLATE algorithm is used, as the size information
 * is not available from the header.
 * <p>
 * The {@link ZipFile} class is preferred when reading from files.
 *  
 * @see ZipFile
 * @NotThreadSafe
 */
public class ZipArchiveInputStream extends ArchiveInputStream {

    private static final int SHORT = 2;
    private static final int WORD = 4;

    /**
     * The zip encoding to use for filenames and the file comment.
     */
    private final ZipEncoding zipEncoding;

    /**
     * Whether to look for and use Unicode extra fields.
     */
    private final boolean useUnicodeExtraFields;

    private final InputStream in;

    private final Inflater inf = new Inflater(true);
    private final CRC32 crc = new CRC32();

    private final byte[] buf = new byte[ZipArchiveOutputStream.BUFFER_SIZE];

    private ZipArchiveEntry current = null;
    private boolean closed = false;
    private boolean hitCentralDirectory = false;
    private int readBytesOfEntry = 0, offsetInBuffer = 0;
    private int bytesReadFromStream = 0;
    private int lengthOfLastRead = 0;
    private boolean hasDataDescriptor = false;

    private static final int LFH_LEN = 30;
    /*
      local file header signature     4 bytes  (0x04034b50)
      version needed to extract       2 bytes
      general purpose bit flag        2 bytes
      compression method              2 bytes
      last mod file time              2 bytes
      last mod file date              2 bytes
      crc-32                          4 bytes
      compressed size                 4 bytes
      uncompressed size               4 bytes
      file name length                2 bytes
      extra field length              2 bytes
    */

    public ZipArchiveInputStream(InputStream inputStream) {
        this(inputStream, ZipEncodingHelper.UTF8, true);
    }

    /**
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     * @param useUnicodeExtraFields whether to use InfoZIP Unicode
     * Extra Fields (if present) to set the file names.
     */
    public ZipArchiveInputStream(InputStream inputStream,
                                 String encoding,
                                 boolean useUnicodeExtraFields) {
        zipEncoding = ZipEncodingHelper.getZipEncoding(encoding);
        this.useUnicodeExtraFields = useUnicodeExtraFields;
        in = new PushbackInputStream(inputStream, buf.length);
    }

    public ZipArchiveEntry getNextZipEntry() throws IOException {
        if (closed || hitCentralDirectory) {
            return null;
        }
        if (current != null) {
            closeEntry();
        }
        byte[] lfh = new byte[LFH_LEN];
        try {
            readFully(lfh);
        } catch (EOFException e) {
            return null;
        }
        ZipLong sig = new ZipLong(lfh);
        if (sig.equals(ZipLong.CFH_SIG)) {
            hitCentralDirectory = true;
            return null;
        }
        if (!sig.equals(ZipLong.LFH_SIG)) {
            return null;
        }

        int off = WORD;
        current = new ZipArchiveEntry();

        int versionMadeBy = ZipShort.getValue(lfh, off);
        off += SHORT;
        current.setPlatform((versionMadeBy >> ZipFile.BYTE_SHIFT)
                            & ZipFile.NIBLET_MASK);

        final int generalPurposeFlag = ZipShort.getValue(lfh, off);
        final boolean hasEFS = 
            (generalPurposeFlag & ZipArchiveOutputStream.EFS_FLAG) != 0;
        final ZipEncoding entryEncoding =
            hasEFS ? ZipEncodingHelper.UTF8_ZIP_ENCODING : zipEncoding;
        hasDataDescriptor = (generalPurposeFlag & 8) != 0;

        off += SHORT;

        current.setMethod(ZipShort.getValue(lfh, off));
        off += SHORT;

        long time = ZipUtil.dosToJavaTime(ZipLong.getValue(lfh, off));
        current.setTime(time);
        off += WORD;

        if (!hasDataDescriptor) {
            current.setCrc(ZipLong.getValue(lfh, off));
            off += WORD;

            current.setCompressedSize(ZipLong.getValue(lfh, off));
            off += WORD;

            current.setSize(ZipLong.getValue(lfh, off));
            off += WORD;
        } else {
            off += 3 * WORD;
        }

        int fileNameLen = ZipShort.getValue(lfh, off);

        off += SHORT;

        int extraLen = ZipShort.getValue(lfh, off);
        off += SHORT;

        byte[] fileName = new byte[fileNameLen];
        readFully(fileName);
        current.setName(entryEncoding.decode(fileName));

        byte[] extraData = new byte[extraLen];
        readFully(extraData);
        current.setExtra(extraData);

        if (!hasEFS && useUnicodeExtraFields) {
            ZipUtil.setNameAndCommentFromExtraFields(current, fileName, null);
        }
        return current;
    }

    public ArchiveEntry getNextEntry() throws IOException {
        return getNextZipEntry();
    }

    public int read(byte[] buffer, int start, int length) throws IOException {
        if (closed) {
            throw new IOException("The stream is closed");
        }
        if (inf.finished() || current == null) {
            return -1;
        }

        // avoid int overflow, check null buffer
        if (start <= buffer.length && length >= 0 && start >= 0
            && buffer.length - start >= length) {
            if (current.getMethod() == ZipArchiveOutputStream.STORED) {
                int csize = (int) current.getSize();
                if (readBytesOfEntry >= csize) {
                    return -1;
                }
                if (offsetInBuffer >= lengthOfLastRead) {
                    offsetInBuffer = 0;
                    if ((lengthOfLastRead = in.read(buf)) == -1) {
                        return -1;
                    }
                    count(lengthOfLastRead);
                    bytesReadFromStream += lengthOfLastRead;
                }
                int toRead = length > lengthOfLastRead
                    ? lengthOfLastRead - offsetInBuffer
                    : length;
                if ((csize - readBytesOfEntry) < toRead) {
                    toRead = csize - readBytesOfEntry;
                }
                System.arraycopy(buf, offsetInBuffer, buffer, start, toRead);
                offsetInBuffer += toRead;
                readBytesOfEntry += toRead;
                crc.update(buffer, start, toRead);
                return toRead;
            }
            if (inf.needsInput()) {
                fill();
                if (lengthOfLastRead > 0) {
                    bytesReadFromStream += lengthOfLastRead;
                }
            }
            int read = 0;
            try {
                read = inf.inflate(buffer, start, length);
            } catch (DataFormatException e) {
                throw new ZipException(e.getMessage());
            }
            if (read == 0 && inf.finished()) {
                return -1;
            }
            crc.update(buffer, start, read);
            return read;
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    public void close() throws IOException {
        if (!closed) {
            closed = true;
            in.close();
        }
    }

    public long skip(long value) throws IOException {
        if (value >= 0) {
            long skipped = 0;
            byte[] b = new byte[1024];
            while (skipped != value) {
                long rem = value - skipped;
                int x = read(b, 0, (int) (b.length > rem ? rem : b.length));
                if (x == -1) {
                    return skipped;
                }
                skipped += x;
            }
            return skipped;
        }
        throw new IllegalArgumentException();
    }

    /*
     *  This test assumes that the zip file does not have any additional leading content,
     *  which is something that is allowed by the specification (e.g. self-extracting zips)
     */
    public static boolean matches(byte[] signature, int length) {
        if (length < ZipArchiveOutputStream.LFH_SIG.length) {
            return false;
        }

        return checksig(signature, ZipArchiveOutputStream.LFH_SIG) // normal file
            || checksig(signature, ZipArchiveOutputStream.EOCD_SIG); // empty zip
    }

    private static boolean checksig(byte[] signature, byte[] expected){
        for (int i = 0; i < expected.length; i++) {
            if (signature[i] != expected[i]) {
                return false;
            }
        }
        return true;        
    }

    private void closeEntry() throws IOException {
        if (closed) {
            throw new IOException("The stream is closed");
        }
        if (current == null) {
            return;
        }
        // Ensure all entry bytes are read
        skip(Long.MAX_VALUE);
        int inB;
        if (current.getMethod() == ZipArchiveOutputStream.DEFLATED) {
            inB = inf.getTotalIn();
        } else {
            inB = readBytesOfEntry;
        }
        int diff = 0;

        // Pushback any required bytes
        if ((diff = bytesReadFromStream - inB) != 0) {
            ((PushbackInputStream) in).unread(buf,
                                              lengthOfLastRead - diff, diff);
        }

        if (hasDataDescriptor) {
            readFully(new byte[4 * WORD]);
        }

        inf.reset();
        readBytesOfEntry = offsetInBuffer = bytesReadFromStream =
            lengthOfLastRead = 0;
        crc.reset();
        current = null;
    }

    private void fill() throws IOException {
        if (closed) {
            throw new IOException("The stream is closed");
        }
        if ((lengthOfLastRead = in.read(buf)) > 0) {
            inf.setInput(buf, 0, lengthOfLastRead);
        }
    }

    private void readFully(byte[] b) throws IOException {
        int count = 0, x = 0;
        while (count != b.length) {
            count += x = in.read(b, count, b.length - count);
            if (x == -1) {
                throw new EOFException();
            }
        }
    }
}
