/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.archivers.sevenz;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.zip.CRC32;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.utils.CountingOutputStream;

/**
 * Writes a 7z file.
 * @since 1.6
 */
public class SevenZOutputFile {
    private final RandomAccessFile file;
    private final List<SevenZArchiveEntry> files = new ArrayList<SevenZArchiveEntry>();
    private int numNonEmptyStreams = 0;
    private CRC32 crc32 = new CRC32();
    private CRC32 compressedCrc32 = new CRC32();
    private long fileBytesWritten = 0;
    private boolean finished = false;
    private CountingOutputStream currentOutputStream;
    private SevenZMethod contentCompression = SevenZMethod.LZMA2;
    
    /**
     * Opens file to write a 7z archive to.
     *
     * @param filename name of the file to write to
     * @throws IOException if opening the file fails
     */
    public SevenZOutputFile(final File filename) throws IOException {
        file = new RandomAccessFile(filename, "rw");
        file.seek(SevenZFile.SIGNATURE_HEADER_SIZE);
    }
    
    /**
     * Sets the compression method to use for entry contents - the
     * default is LZMA2.
     *
     * <p>Currently only {@link SevenZMethod#COPY}, {@link
     * SevenZMethod#LZMA2}, {@link SevenZMethod#BZIP2} and {@link
     * SevenZMethod#DEFLATE} are supported.</p>
     */
    public void setContentCompression(SevenZMethod method) {
        this.contentCompression = method;
    }

    /**
     * Closes the archive, calling {@link #finish} if necessary.
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        if (!finished) {
            finish();
        }
        file.close();
    }
    
    /**
     * Create an archive entry using the inputFile and entryName provided.
     * 
     * @param inputFile
     * @param entryName 
     * @return the ArchiveEntry set up with details from the file
     * 
     * @throws IOException
     */
    public SevenZArchiveEntry createArchiveEntry(final File inputFile,
            final String entryName) throws IOException {
        final SevenZArchiveEntry entry = new SevenZArchiveEntry();
        entry.setDirectory(inputFile.isDirectory());
        entry.setName(entryName);
        entry.setLastModifiedDate(new Date(inputFile.lastModified()));
        return entry;
    }

    /**
     * Records an archive entry to add.
     *
     * The caller must then write the content to the archive and call
     * {@link #closeArchiveEntry()} to complete the process.
     * 
     * @param archiveEntry describes the entry
     * @throws IOException
     */
    public void putArchiveEntry(final ArchiveEntry archiveEntry) throws IOException {
        final SevenZArchiveEntry entry = (SevenZArchiveEntry) archiveEntry;
        files.add(entry);
    }
    
    /**
     * Closes the archive entry.
     * @throws IOException
     */
    public void closeArchiveEntry() throws IOException {
        if (currentOutputStream != null) {
            currentOutputStream.flush();
            currentOutputStream.close();
        }

        final SevenZArchiveEntry entry = files.get(files.size() - 1);
        if (fileBytesWritten > 0) {
            entry.setHasStream(true);
            ++numNonEmptyStreams;
            entry.setSize(currentOutputStream.getBytesWritten());
            entry.setCompressedSize(fileBytesWritten);
            entry.setCrcValue(crc32.getValue());
            entry.setCompressedCrcValue(compressedCrc32.getValue());
            entry.setHasCrc(true);
        } else {
            entry.setHasStream(false);
            entry.setSize(0);
            entry.setCompressedSize(0);
            entry.setHasCrc(false);
        }
        currentOutputStream = null;
        crc32.reset();
        compressedCrc32.reset();
        fileBytesWritten = 0;
    }
    
    /**
     * Writes a byte to the current archive entry.
     * @param b The byte to be written.
     * @throws IOException on error
     */
    public void write(final int b) throws IOException {
        getCurrentOutputStream().write(b);
    }
    
    /**
     * Writes a byte array to the current archive entry.
     * @param b The byte array to be written.
     * @throws IOException on error
     */
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }
    
    /**
     * Writes part of a byte array to the current archive entry.
     * @param b The byte array to be written.
     * @param off offset into the array to start writing from
     * @param len number of bytes to write
     * @throws IOException on error
     */
    public void write(final byte[] b, final int off, final int len) throws IOException {
        if (len > 0) {
            getCurrentOutputStream().write(b, off, len);
        }
    }
    
    /**
     * Finishes the addition of entries to this archive, without closing it.
     * 
     * @throws IOException if archive is already closed.
     */
    public void finish() throws IOException {
        if (finished) {
            throw new IOException("This archive has already been finished");
        }
        finished = true;
        
        final long headerPosition = file.getFilePointer();
        
        final ByteArrayOutputStream headerBaos = new ByteArrayOutputStream();
        final DataOutputStream header = new DataOutputStream(headerBaos);
        
        writeHeader(header);
        header.flush();
        final byte[] headerBytes = headerBaos.toByteArray();
        file.write(headerBytes);
        
        final CRC32 crc32 = new CRC32();
        
        // signature header
        file.seek(0);
        file.write(SevenZFile.sevenZSignature);
        // version
        file.write(0);
        file.write(2);
        
        // start header
        final ByteArrayOutputStream startHeaderBaos = new ByteArrayOutputStream();
        final DataOutputStream startHeaderStream = new DataOutputStream(startHeaderBaos);
        startHeaderStream.writeLong(Long.reverseBytes(headerPosition - SevenZFile.SIGNATURE_HEADER_SIZE));
        startHeaderStream.writeLong(Long.reverseBytes(0xffffFFFFL & headerBytes.length));
        crc32.reset();
        crc32.update(headerBytes);
        startHeaderStream.writeInt(Integer.reverseBytes((int)crc32.getValue()));
        startHeaderStream.flush();
        final byte[] startHeaderBytes = startHeaderBaos.toByteArray();
        crc32.reset();
        crc32.update(startHeaderBytes);
        file.writeInt(Integer.reverseBytes((int) crc32.getValue()));
        file.write(startHeaderBytes);
    }
    
    /*
     * Creation of output stream is deferred until data is actually
     * written as some codecs might write header information even for
     * empty streams and directories otherwise.
     */
    private OutputStream getCurrentOutputStream() throws IOException {
        if (currentOutputStream == null) {
            currentOutputStream = setupFileOutputStream();
        }
        return currentOutputStream;
    }

    private CountingOutputStream setupFileOutputStream() throws IOException {
        OutputStream out = new OutputStreamWrapper();
        return new CountingOutputStream(Coders
                                        .addEncoder(out,
                                                    contentCompression,
                                                    null)) {
            @Override
            public void write(final int b) throws IOException {
                super.write(b);
                crc32.update(b);
            }
    
            @Override
            public void write(final byte[] b) throws IOException {
                super.write(b);
                crc32.update(b);
            }
    
            @Override
            public void write(final byte[] b, final int off, final int len)
                throws IOException {
                super.write(b, off, len);
                crc32.update(b, off, len);
            }
        };
    }

    private void writeHeader(final DataOutput header) throws IOException {
        header.write(NID.kHeader);
        
        header.write(NID.kMainStreamsInfo);
        writeStreamsInfo(header);
        writeFilesInfo(header);
        header.write(NID.kEnd);
    }
    
    private void writeStreamsInfo(final DataOutput header) throws IOException {
        if (numNonEmptyStreams > 0) {
            writePackInfo(header);
            writeUnpackInfo(header);
        }
        
        writeSubStreamsInfo(header);
        
        header.write(NID.kEnd);
    }
    
    private void writePackInfo(final DataOutput header) throws IOException {
        header.write(NID.kPackInfo);
        
        writeUint64(header, 0);
        writeUint64(header, 0xffffFFFFL & numNonEmptyStreams);
        
        header.write(NID.kSize);
        for (final SevenZArchiveEntry entry : files) {
            if (entry.hasStream()) {
                writeUint64(header, entry.getCompressedSize());
            }
        }
        
        header.write(NID.kCRC);
        header.write(1);
        for (final SevenZArchiveEntry entry : files) {
            if (entry.hasStream()) {
                header.writeInt(Integer.reverseBytes((int) entry.getCompressedCrcValue()));
            }
        }
        
        header.write(NID.kEnd);
    }
    
    private void writeUnpackInfo(final DataOutput header) throws IOException {
        header.write(NID.kUnpackInfo);
        
        header.write(NID.kFolder);
        writeUint64(header, numNonEmptyStreams);
        header.write(0);
        for (int i = 0; i < numNonEmptyStreams; i++) {
            writeFolder(header);
        }
        
        header.write(NID.kCodersUnpackSize);
        for (final SevenZArchiveEntry entry : files) {
            if (entry.hasStream()) {
                writeUint64(header, entry.getSize());
            }
        }
        
        header.write(NID.kCRC);
        header.write(1);
        for (final SevenZArchiveEntry entry : files) {
            if (entry.hasStream()) {
                header.writeInt(Integer.reverseBytes((int) entry.getCrcValue()));
            }
        }
        
        header.write(NID.kEnd);
    }
    
    private void writeFolder(final DataOutput header) throws IOException {
        // one coder
        writeUint64(header, 1);
        byte[] id = contentCompression.getId();
        byte[] properties = contentCompression.getProperties();

        int codecFlags = id.length;
        if (properties.length > 0) {
            codecFlags |= 0x20;
        }
        header.write(codecFlags);
        header.write(id);

        if (properties.length > 0) {
            header.write(properties.length);
            header.write(properties);
        }
    }
    
    private void writeSubStreamsInfo(final DataOutput header) throws IOException {
        header.write(NID.kSubStreamsInfo);
//        
//        header.write(NID.kCRC);
//        header.write(1);
//        for (final SevenZArchiveEntry entry : files) {
//            if (entry.getHasCrc()) {
//                header.writeInt(Integer.reverseBytes(entry.getCrc()));
//            }
//        }
//        
        header.write(NID.kEnd);
    }
    
    private void writeFilesInfo(final DataOutput header) throws IOException {
        header.write(NID.kFilesInfo);
        
        writeUint64(header, files.size());

        writeFileEmptyStreams(header);
        writeFileEmptyFiles(header);
        writeFileAntiItems(header);
        writeFileNames(header);
        writeFileCTimes(header);
        writeFileATimes(header);
        writeFileMTimes(header);
        writeFileWindowsAttributes(header);
        header.write(0);
    }
    
    private void writeFileEmptyStreams(final DataOutput header) throws IOException {
        boolean hasEmptyStreams = false;
        for (final SevenZArchiveEntry entry : files) {
            if (!entry.hasStream()) {
                hasEmptyStreams = true;
                break;
            }
        }
        if (hasEmptyStreams) {
            header.write(NID.kEmptyStream);
            final BitSet emptyStreams = new BitSet(files.size());
            for (int i = 0; i < files.size(); i++) {
                emptyStreams.set(i, !files.get(i).hasStream());
            }
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream out = new DataOutputStream(baos);
            writeBits(out, emptyStreams, files.size());
            out.flush();
            final byte[] contents = baos.toByteArray();
            writeUint64(header, contents.length);
            header.write(contents);
        }
    }
    
    private void writeFileEmptyFiles(final DataOutput header) throws IOException {
        boolean hasEmptyFiles = false;
        int emptyStreamCounter = 0;
        final BitSet emptyFiles = new BitSet(0);
        for (int i = 0; i < files.size(); i++) {
            if (!files.get(i).hasStream()) {
                boolean isDir = files.get(i).isDirectory();
                emptyFiles.set(emptyStreamCounter++, !isDir);
                hasEmptyFiles |= !isDir;
            }
        }
        if (hasEmptyFiles) {
            header.write(NID.kEmptyFile);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream out = new DataOutputStream(baos);
            writeBits(out, emptyFiles, emptyStreamCounter);
            out.flush();
            final byte[] contents = baos.toByteArray();
            writeUint64(header, contents.length);
            header.write(contents);
        }
    }
    
    private void writeFileAntiItems(final DataOutput header) throws IOException {
        boolean hasAntiItems = false;
        final BitSet antiItems = new BitSet(0);
        int antiItemCounter = 0;
        for (int i = 0; i < files.size(); i++) {
            if (!files.get(i).hasStream()) {
                boolean isAnti = files.get(i).isAntiItem();
                antiItems.set(antiItemCounter++, isAnti);
                hasAntiItems |= isAnti;
            }
        }
        if (hasAntiItems) {
            header.write(NID.kAnti);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream out = new DataOutputStream(baos);
            writeBits(out, antiItems, antiItemCounter);
            out.flush();
            final byte[] contents = baos.toByteArray();
            writeUint64(header, contents.length);
            header.write(contents);
        }
    }
    
    private void writeFileNames(final DataOutput header) throws IOException {
        header.write(NID.kName);
        
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(baos);
        out.write(0);
        for (final SevenZArchiveEntry entry : files) {
            out.write(entry.getName().getBytes("UTF-16LE"));
            out.writeShort(0);
        }
        out.flush();
        final byte[] contents = baos.toByteArray();
        writeUint64(header, contents.length);
        header.write(contents);
    }

    private void writeFileCTimes(final DataOutput header) throws IOException {
        int numCreationDates = 0;
        for (final SevenZArchiveEntry entry : files) {
            if (entry.getHasCreationDate()) {
                ++numCreationDates;
            }
        }
        if (numCreationDates > 0) {
            header.write(NID.kCTime);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream out = new DataOutputStream(baos);
            if (numCreationDates != files.size()) {
                out.write(0);
                final BitSet cTimes = new BitSet(files.size());
                for (int i = 0; i < files.size(); i++) {
                    cTimes.set(i, files.get(i).getHasCreationDate());
                }
                writeBits(out, cTimes, files.size());
            } else {
                out.write(1);
            }
            out.write(0);
            for (final SevenZArchiveEntry entry : files) {
                if (entry.getHasCreationDate()) {
                    out.writeLong(Long.reverseBytes(
                            SevenZArchiveEntry.javaTimeToNtfsTime(entry.getCreationDate())));
                }
            }
            out.flush();
            final byte[] contents = baos.toByteArray();
            writeUint64(header, contents.length);
            header.write(contents);
        }
    }

    private void writeFileATimes(final DataOutput header) throws IOException {
        int numAccessDates = 0;
        for (final SevenZArchiveEntry entry : files) {
            if (entry.getHasAccessDate()) {
                ++numAccessDates;
            }
        }
        if (numAccessDates > 0) {
            header.write(NID.kATime);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream out = new DataOutputStream(baos);
            if (numAccessDates != files.size()) {
                out.write(0);
                final BitSet aTimes = new BitSet(files.size());
                for (int i = 0; i < files.size(); i++) {
                    aTimes.set(i, files.get(i).getHasAccessDate());
                }
                writeBits(out, aTimes, files.size());
            } else {
                out.write(1);
            }
            out.write(0);
            for (final SevenZArchiveEntry entry : files) {
                if (entry.getHasAccessDate()) {
                    out.writeLong(Long.reverseBytes(
                            SevenZArchiveEntry.javaTimeToNtfsTime(entry.getAccessDate())));
                }
            }
            out.flush();
            final byte[] contents = baos.toByteArray();
            writeUint64(header, contents.length);
            header.write(contents);
        }
    }

    private void writeFileMTimes(final DataOutput header) throws IOException {
        int numLastModifiedDates = 0;
        for (final SevenZArchiveEntry entry : files) {
            if (entry.getHasLastModifiedDate()) {
                ++numLastModifiedDates;
            }
        }
        if (numLastModifiedDates > 0) {
            header.write(NID.kMTime);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream out = new DataOutputStream(baos);
            if (numLastModifiedDates != files.size()) {
                out.write(0);
                final BitSet mTimes = new BitSet(files.size());
                for (int i = 0; i < files.size(); i++) {
                    mTimes.set(i, files.get(i).getHasLastModifiedDate());
                }
                writeBits(out, mTimes, files.size());
            } else {
                out.write(1);
            }
            out.write(0);
            for (final SevenZArchiveEntry entry : files) {
                if (entry.getHasLastModifiedDate()) {
                    out.writeLong(Long.reverseBytes(
                            SevenZArchiveEntry.javaTimeToNtfsTime(entry.getLastModifiedDate())));
                }
            }
            out.flush();
            final byte[] contents = baos.toByteArray();
            writeUint64(header, contents.length);
            header.write(contents);
        }
    }

    private void writeFileWindowsAttributes(final DataOutput header) throws IOException {
        int numWindowsAttributes = 0;
        for (final SevenZArchiveEntry entry : files) {
            if (entry.getHasWindowsAttributes()) {
                ++numWindowsAttributes;
            }
        }
        if (numWindowsAttributes > 0) {
            header.write(NID.kWinAttributes);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream out = new DataOutputStream(baos);
            if (numWindowsAttributes != files.size()) {
                out.write(0);
                final BitSet attributes = new BitSet(files.size());
                for (int i = 0; i < files.size(); i++) {
                    attributes.set(i, files.get(i).getHasWindowsAttributes());
                }
                writeBits(out, attributes, files.size());
            } else {
                out.write(1);
            }
            out.write(0);
            for (final SevenZArchiveEntry entry : files) {
                if (entry.getHasWindowsAttributes()) {
                    out.writeInt(Integer.reverseBytes(entry.getWindowsAttributes()));
                }
            }
            out.flush();
            final byte[] contents = baos.toByteArray();
            writeUint64(header, contents.length);
            header.write(contents);
        }
    }

    private void writeUint64(final DataOutput header, long value) throws IOException {
        int firstByte = 0;
        int mask = 0x80;
        int i;
        for (i = 0; i < 8; i++) {
            if (value < ((1L << ( 7  * (i + 1))))) {
                firstByte |= (value >>> (8 * i));
                break;
            }
            firstByte |= mask;
            mask >>>= 1;
        }
        header.write(firstByte);
        for (; i > 0; i--) {
            header.write((int) (0xff & value));
            value >>>= 8;
        }
    }

    private void writeBits(final DataOutput header, final BitSet bits, final int length) throws IOException {
        int cache = 0;
        int shift = 7;
        for (int i = 0; i < length; i++) {
            cache |= ((bits.get(i) ? 1 : 0) << shift);
            --shift;
            if (shift == 0) {
                header.write(cache);
                shift = 7;
                cache = 0;
            }
        }
        if (length > 0 && shift > 0) {
            header.write(cache);
        }
    }

    private class OutputStreamWrapper extends OutputStream {
        @Override
        public void write(final int b) throws IOException {
            file.write(b);
            compressedCrc32.update(b);
            fileBytesWritten++;
        }
    
        @Override
        public void write(final byte[] b) throws IOException {
            OutputStreamWrapper.this.write(b, 0, b.length);
        }
    
        @Override
        public void write(final byte[] b, final int off, final int len)
            throws IOException {
            file.write(b, off, len);
            compressedCrc32.update(b, off, len);
            fileBytesWritten += len;
        }

        @Override
        public void flush() throws IOException {
            // no reason to flush a RandomAccessFile
        }

        @Override
        public void close() throws IOException {
            // the file will be closed by the containing class's close method
        }
    }
}
