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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.zip.CRC32;

import org.apache.commons.compress.utils.BoundedInputStream;
import org.apache.commons.compress.utils.CRC32VerifyingInputStream;
import org.apache.commons.compress.utils.CharsetNames;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Reads a 7z file, using RandomAccessFile under
 * the covers.
 * <p>
 * The 7z file format is a flexible container
 * that can contain many compression and
 * encryption types, but at the moment only
 * only Copy, LZMA, LZMA2, BZIP2, Deflate and AES-256 + SHA-256
 * are supported.
 * <p>
 * The format is very Windows/Intel specific,
 * so it uses little-endian byte order,
 * doesn't store user/group or permission bits,
 * and represents times using NTFS timestamps
 * (100 nanosecond units since 1 January 1601).
 * Hence the official tools recommend against
 * using it for backup purposes on *nix, and
 * recommend .tar.7z or .tar.lzma or .tar.xz
 * instead.  
 * <p>
 * Both the header and file contents may be
 * compressed and/or encrypted. With both
 * encrypted, neither file names nor file
 * contents can be read, but the use of
 * encryption isn't plausibly deniable.
 * 
 * @NotThreadSafe
 * @since 1.6
 */
public class SevenZFile implements Closeable {
    static final int SIGNATURE_HEADER_SIZE = 32;

    private final String fileName;
    private RandomAccessFile file;
    private final Archive archive;
    private int currentEntryIndex = -1;
    private int currentFolderIndex = -1;
    private InputStream currentFolderInputStream = null;
    private byte[] password;

    private final ArrayList<InputStream> deferredBlockStreams = new ArrayList<InputStream>();

    static final byte[] sevenZSignature = {
        (byte)'7', (byte)'z', (byte)0xBC, (byte)0xAF, (byte)0x27, (byte)0x1C
    };
    
    /**
     * Reads a file as 7z archive
     *
     * @param filename the file to read
     * @param password optional password if the archive is encrypted -
     * the byte array is supposed to be the UTF16-LE encoded
     * representation of the password.
     * @throws IOException if reading the archive fails
     */
    public SevenZFile(final File filename, final byte[] password) throws IOException {
        boolean succeeded = false;
        this.file = new RandomAccessFile(filename, "r");
        this.fileName = filename.getAbsolutePath();
        try {
            archive = readHeaders(password);
            if (password != null) {
                this.password = new byte[password.length];
                System.arraycopy(password, 0, this.password, 0, password.length);
            } else {
                this.password = null;
            }
            succeeded = true;
        } finally {
            if (!succeeded) {
                this.file.close();
            }
        }
    }
    
    /**
     * Reads a file as unencrypted 7z archive
     *
     * @param filename the file to read
     * @throws IOException if reading the archive fails
     */
    public SevenZFile(final File filename) throws IOException {
        this(filename, null);
    }

    /**
     * Closes the archive.
     * @throws IOException if closing the file fails
     */
    @Override
    public void close() throws IOException {
        if (file != null) {
            try {
                file.close();
            } finally {
                file = null;
                if (password != null) {
                    Arrays.fill(password, (byte) 0);
                }
                password = null;
            }
        }
    }
    
    /**
     * Returns the next Archive Entry in this archive.
     *
     * @return the next entry,
     *         or {@code null} if there are no more entries
     * @throws IOException if the next entry could not be read
     */
    public SevenZArchiveEntry getNextEntry() throws IOException {
        if (currentEntryIndex >= archive.files.length - 1) {
            return null;
        }
        ++currentEntryIndex;
        final SevenZArchiveEntry entry = archive.files[currentEntryIndex];
        buildDecodingStream();
        return entry;
    }
    
    /**
     * Returns meta-data of all archive entries.
     *
     * <p>This method only provides meta-data, the entries can not be
     * used to read the contents, you still need to process all
     * entries in order using {@link #getNextEntry} for that.</p>
     *
     * <p>The content methods are only available for entries that have
     * already been reached via {@link #getNextEntry}.</p>
     *
     * @return meta-data of all archive entries.
     * @since 1.11
     */
    public Iterable<SevenZArchiveEntry> getEntries() {
        return Arrays.asList(archive.files);
    }
    
    private Archive readHeaders(final byte[] password) throws IOException {
        final byte[] signature = new byte[6];
        file.readFully(signature);
        if (!Arrays.equals(signature, sevenZSignature)) {
            throw new IOException("Bad 7z signature");
        }
        // 7zFormat.txt has it wrong - it's first major then minor
        final byte archiveVersionMajor = file.readByte();
        final byte archiveVersionMinor = file.readByte();
        if (archiveVersionMajor != 0) {
            throw new IOException(String.format("Unsupported 7z version (%d,%d)",
                    archiveVersionMajor, archiveVersionMinor));
        }

        final long startHeaderCrc = 0xffffFFFFL & Integer.reverseBytes(file.readInt());
        final StartHeader startHeader = readStartHeader(startHeaderCrc);
        
        final int nextHeaderSizeInt = (int) startHeader.nextHeaderSize;
        if (nextHeaderSizeInt != startHeader.nextHeaderSize) {
            throw new IOException("cannot handle nextHeaderSize " + startHeader.nextHeaderSize);
        }
        file.seek(SIGNATURE_HEADER_SIZE + startHeader.nextHeaderOffset);
        final byte[] nextHeader = new byte[nextHeaderSizeInt];
        file.readFully(nextHeader);
        final CRC32 crc = new CRC32();
        crc.update(nextHeader);
        if (startHeader.nextHeaderCrc != crc.getValue()) {
            throw new IOException("NextHeader CRC mismatch");
        }
        
        final ByteArrayInputStream byteStream = new ByteArrayInputStream(nextHeader);
        DataInputStream nextHeaderInputStream = new DataInputStream(
                byteStream);
        Archive archive = new Archive();
        int nid = nextHeaderInputStream.readUnsignedByte();
        if (nid == NID.kEncodedHeader) {
            nextHeaderInputStream =
                readEncodedHeader(nextHeaderInputStream, archive, password);
            // Archive gets rebuilt with the new header
            archive = new Archive();
            nid = nextHeaderInputStream.readUnsignedByte();
        }
        if (nid == NID.kHeader) {
            readHeader(nextHeaderInputStream, archive);
            nextHeaderInputStream.close();
        } else {
            throw new IOException("Broken or unsupported archive: no Header");
        }
        return archive;
    }
    
    private StartHeader readStartHeader(final long startHeaderCrc) throws IOException {
        final StartHeader startHeader = new StartHeader();
        DataInputStream dataInputStream = null;
        try {
             dataInputStream = new DataInputStream(new CRC32VerifyingInputStream(
                    new BoundedRandomAccessFileInputStream(file, 20), 20, startHeaderCrc));
             startHeader.nextHeaderOffset = Long.reverseBytes(dataInputStream.readLong());
             startHeader.nextHeaderSize = Long.reverseBytes(dataInputStream.readLong());
             startHeader.nextHeaderCrc = 0xffffFFFFL & Integer.reverseBytes(dataInputStream.readInt());
             return startHeader;
        } finally {
            if (dataInputStream != null) {
                dataInputStream.close();
            }
        }
    }
    
    private void readHeader(final DataInput header, final Archive archive) throws IOException {
        int nid = header.readUnsignedByte();
        
        if (nid == NID.kArchiveProperties) {
            readArchiveProperties(header);
            nid = header.readUnsignedByte();
        }
        
        if (nid == NID.kAdditionalStreamsInfo) {
            throw new IOException("Additional streams unsupported");
            //nid = header.readUnsignedByte();
        }
        
        if (nid == NID.kMainStreamsInfo) {
            readStreamsInfo(header, archive);
            nid = header.readUnsignedByte();
        }
        
        if (nid == NID.kFilesInfo) {
            readFilesInfo(header, archive);
            nid = header.readUnsignedByte();
        }
        
        if (nid != NID.kEnd) {
            throw new IOException("Badly terminated header, found " + nid);
        }
    }
    
    private void readArchiveProperties(final DataInput input) throws IOException {
        // FIXME: the reference implementation just throws them away?
        int nid =  input.readUnsignedByte();
        while (nid != NID.kEnd) {
            final long propertySize = readUint64(input);
            final byte[] property = new byte[(int)propertySize];
            input.readFully(property);
            nid = input.readUnsignedByte();
        }
    }
    
    private DataInputStream readEncodedHeader(final DataInputStream header, final Archive archive,
                                              final byte[] password) throws IOException {
        readStreamsInfo(header, archive);
        
        // FIXME: merge with buildDecodingStream()/buildDecoderStack() at some stage?
        final Folder folder = archive.folders[0];
        final int firstPackStreamIndex = 0;
        final long folderOffset = SIGNATURE_HEADER_SIZE + archive.packPos +
                0;
        
        file.seek(folderOffset);
        InputStream inputStreamStack = new BoundedRandomAccessFileInputStream(file,
                archive.packSizes[firstPackStreamIndex]);
        for (final Coder coder : folder.getOrderedCoders()) {
            if (coder.numInStreams != 1 || coder.numOutStreams != 1) {
                throw new IOException("Multi input/output stream coders are not yet supported");
            }
            inputStreamStack = Coders.addDecoder(fileName, inputStreamStack,
                    folder.getUnpackSizeForCoder(coder), coder, password);
        }
        if (folder.hasCrc) {
            inputStreamStack = new CRC32VerifyingInputStream(inputStreamStack,
                    folder.getUnpackSize(), folder.crc);
        }
        final byte[] nextHeader = new byte[(int)folder.getUnpackSize()];
        final DataInputStream nextHeaderInputStream = new DataInputStream(inputStreamStack);
        try {
            nextHeaderInputStream.readFully(nextHeader);
        } finally {
            nextHeaderInputStream.close();
        }
        return new DataInputStream(new ByteArrayInputStream(nextHeader));
    }
    
    private void readStreamsInfo(final DataInput header, final Archive archive) throws IOException {
        int nid = header.readUnsignedByte();
        
        if (nid == NID.kPackInfo) {
            readPackInfo(header, archive);
            nid = header.readUnsignedByte();
        }
        
        if (nid == NID.kUnpackInfo) {
            readUnpackInfo(header, archive);
            nid = header.readUnsignedByte();
        } else {
            // archive without unpack/coders info
            archive.folders = new Folder[0];
        }
        
        if (nid == NID.kSubStreamsInfo) {
            readSubStreamsInfo(header, archive);
            nid = header.readUnsignedByte();
        }
        
        if (nid != NID.kEnd) {
            throw new IOException("Badly terminated StreamsInfo");
        }
    }
    
    private void readPackInfo(final DataInput header, final Archive archive) throws IOException {
        archive.packPos = readUint64(header);
        final long numPackStreams = readUint64(header);
        int nid = header.readUnsignedByte();
        if (nid == NID.kSize) {
            archive.packSizes = new long[(int)numPackStreams];
            for (int i = 0; i < archive.packSizes.length; i++) {
                archive.packSizes[i] = readUint64(header);
            }
            nid = header.readUnsignedByte();
        }
        
        if (nid == NID.kCRC) {
            archive.packCrcsDefined = readAllOrBits(header, (int)numPackStreams);
            archive.packCrcs = new long[(int)numPackStreams];
            for (int i = 0; i < (int)numPackStreams; i++) {
                if (archive.packCrcsDefined.get(i)) {
                    archive.packCrcs[i] = 0xffffFFFFL & Integer.reverseBytes(header.readInt());
                }
            }
            
            nid = header.readUnsignedByte();
        }
        
        if (nid != NID.kEnd) {
            throw new IOException("Badly terminated PackInfo (" + nid + ")");
        }
    }
    
    private void readUnpackInfo(final DataInput header, final Archive archive) throws IOException {
        int nid = header.readUnsignedByte();
        if (nid != NID.kFolder) {
            throw new IOException("Expected kFolder, got " + nid);
        }
        final long numFolders = readUint64(header);
        final Folder[] folders = new Folder[(int)numFolders];
        archive.folders = folders;
        final int external = header.readUnsignedByte();
        if (external != 0) {
            throw new IOException("External unsupported");
        }
        for (int i = 0; i < (int)numFolders; i++) {
            folders[i] = readFolder(header);
        }
        
        nid = header.readUnsignedByte();
        if (nid != NID.kCodersUnpackSize) {
            throw new IOException("Expected kCodersUnpackSize, got " + nid);
        }
        for (final Folder folder : folders) {
            folder.unpackSizes = new long[(int)folder.totalOutputStreams];
            for (int i = 0; i < folder.totalOutputStreams; i++) {
                folder.unpackSizes[i] = readUint64(header);
            }
        }
        
        nid = header.readUnsignedByte();
        if (nid == NID.kCRC) {
            final BitSet crcsDefined = readAllOrBits(header, (int)numFolders);
            for (int i = 0; i < (int)numFolders; i++) {
                if (crcsDefined.get(i)) {
                    folders[i].hasCrc = true;
                    folders[i].crc = 0xffffFFFFL & Integer.reverseBytes(header.readInt());
                } else {
                    folders[i].hasCrc = false;
                }
            }
            
            nid = header.readUnsignedByte();
        }
        
        if (nid != NID.kEnd) {
            throw new IOException("Badly terminated UnpackInfo");
        }
    }
    
    private void readSubStreamsInfo(final DataInput header, final Archive archive) throws IOException {
        for (final Folder folder : archive.folders) {
            folder.numUnpackSubStreams = 1;
        }
        int totalUnpackStreams = archive.folders.length;
        
        int nid = header.readUnsignedByte();
        if (nid == NID.kNumUnpackStream) {
            totalUnpackStreams = 0;
            for (final Folder folder : archive.folders) {
                final long numStreams = readUint64(header);
                folder.numUnpackSubStreams = (int)numStreams;
                totalUnpackStreams += numStreams;
            }
            nid = header.readUnsignedByte();
        }
        
        final SubStreamsInfo subStreamsInfo = new SubStreamsInfo();
        subStreamsInfo.unpackSizes = new long[totalUnpackStreams];
        subStreamsInfo.hasCrc = new BitSet(totalUnpackStreams);
        subStreamsInfo.crcs = new long[totalUnpackStreams];
        
        int nextUnpackStream = 0;
        for (final Folder folder : archive.folders) {
            if (folder.numUnpackSubStreams == 0) {
                continue;
            }
            long sum = 0;
            if (nid == NID.kSize) {
                for (int i = 0; i < folder.numUnpackSubStreams - 1; i++) {
                    final long size = readUint64(header);
                    subStreamsInfo.unpackSizes[nextUnpackStream++] = size;
                    sum += size;
                }
            }
            subStreamsInfo.unpackSizes[nextUnpackStream++] = folder.getUnpackSize() - sum;
        }
        if (nid == NID.kSize) {
            nid = header.readUnsignedByte();
        }
        
        int numDigests = 0;
        for (final Folder folder : archive.folders) {
            if (folder.numUnpackSubStreams != 1 || !folder.hasCrc) {
                numDigests += folder.numUnpackSubStreams;
            }
        }
        
        if (nid == NID.kCRC) {
            final BitSet hasMissingCrc = readAllOrBits(header, numDigests);
            final long[] missingCrcs = new long[numDigests];
            for (int i = 0; i < numDigests; i++) {
                if (hasMissingCrc.get(i)) {
                    missingCrcs[i] = 0xffffFFFFL & Integer.reverseBytes(header.readInt());
                }
            }
            int nextCrc = 0;
            int nextMissingCrc = 0;
            for (final Folder folder: archive.folders) {
                if (folder.numUnpackSubStreams == 1 && folder.hasCrc) {
                    subStreamsInfo.hasCrc.set(nextCrc, true);
                    subStreamsInfo.crcs[nextCrc] = folder.crc;
                    ++nextCrc;
                } else {
                    for (int i = 0; i < folder.numUnpackSubStreams; i++) {
                        subStreamsInfo.hasCrc.set(nextCrc, hasMissingCrc.get(nextMissingCrc));
                        subStreamsInfo.crcs[nextCrc] = missingCrcs[nextMissingCrc];
                        ++nextCrc;
                        ++nextMissingCrc;
                    }
                }
            }
            
            nid = header.readUnsignedByte();
        }
        
        if (nid != NID.kEnd) {
            throw new IOException("Badly terminated SubStreamsInfo");
        }
        
        archive.subStreamsInfo = subStreamsInfo;
    }
    
    private Folder readFolder(final DataInput header) throws IOException {
        final Folder folder = new Folder();
        
        final long numCoders = readUint64(header);
        final Coder[] coders = new Coder[(int)numCoders];
        long totalInStreams = 0;
        long totalOutStreams = 0;
        for (int i = 0; i < coders.length; i++) {
            coders[i] = new Coder();
            final int bits = header.readUnsignedByte();
            final int idSize = bits & 0xf;
            final boolean isSimple = (bits & 0x10) == 0;
            final boolean hasAttributes = (bits & 0x20) != 0;
            final boolean moreAlternativeMethods = (bits & 0x80) != 0;
            
            coders[i].decompressionMethodId = new byte[idSize];
            header.readFully(coders[i].decompressionMethodId);
            if (isSimple) {
                coders[i].numInStreams = 1;
                coders[i].numOutStreams = 1;
            } else {
                coders[i].numInStreams = readUint64(header);
                coders[i].numOutStreams = readUint64(header);
            }
            totalInStreams += coders[i].numInStreams;
            totalOutStreams += coders[i].numOutStreams;
            if (hasAttributes) {
                final long propertiesSize = readUint64(header);
                coders[i].properties = new byte[(int)propertiesSize];
                header.readFully(coders[i].properties);
            }
            // would need to keep looping as above:
            while (moreAlternativeMethods) {
                throw new IOException("Alternative methods are unsupported, please report. " +
                        "The reference implementation doesn't support them either.");
            }
        }
        folder.coders = coders;
        folder.totalInputStreams = totalInStreams;
        folder.totalOutputStreams = totalOutStreams;
        
        if (totalOutStreams == 0) {
            throw new IOException("Total output streams can't be 0");
        }
        final long numBindPairs = totalOutStreams - 1;
        final BindPair[] bindPairs = new BindPair[(int)numBindPairs];
        for (int i = 0; i < bindPairs.length; i++) {
            bindPairs[i] = new BindPair();
            bindPairs[i].inIndex = readUint64(header);
            bindPairs[i].outIndex = readUint64(header);
        }
        folder.bindPairs = bindPairs;
        
        if (totalInStreams < numBindPairs) {
            throw new IOException("Total input streams can't be less than the number of bind pairs");
        }
        final long numPackedStreams = totalInStreams - numBindPairs;
        final long packedStreams[] = new long[(int)numPackedStreams];
        if (numPackedStreams == 1) {
            int i;
            for (i = 0; i < (int)totalInStreams; i++) {
                if (folder.findBindPairForInStream(i) < 0) {
                    break;
                }
            }
            if (i == (int)totalInStreams) {
                throw new IOException("Couldn't find stream's bind pair index");
            }
            packedStreams[0] = i;
        } else {
            for (int i = 0; i < (int)numPackedStreams; i++) {
                packedStreams[i] = readUint64(header);
            }
        }
        folder.packedStreams = packedStreams;
        
        return folder;
    }
    
    private BitSet readAllOrBits(final DataInput header, final int size) throws IOException {
        final int areAllDefined = header.readUnsignedByte();
        final BitSet bits;
        if (areAllDefined != 0) {
            bits = new BitSet(size);
            for (int i = 0; i < size; i++) {
                bits.set(i, true);
            }
        } else {
            bits = readBits(header, size);
        }
        return bits;
    }
    
    private BitSet readBits(final DataInput header, final int size) throws IOException {
        final BitSet bits = new BitSet(size);
        int mask = 0;
        int cache = 0;
        for (int i = 0; i < size; i++) {
            if (mask == 0) {
                mask = 0x80;
                cache = header.readUnsignedByte();
            }
            bits.set(i, (cache & mask) != 0);
            mask >>>= 1;
        }
        return bits;
    }
    
    private void readFilesInfo(final DataInput header, final Archive archive) throws IOException {
        final long numFiles = readUint64(header);
        final SevenZArchiveEntry[] files = new SevenZArchiveEntry[(int)numFiles];
        for (int i = 0; i < files.length; i++) {
            files[i] = new SevenZArchiveEntry();
        }
        BitSet isEmptyStream = null;
        BitSet isEmptyFile = null; 
        BitSet isAnti = null;
        while (true) {
            final int propertyType = header.readUnsignedByte();
            if (propertyType == 0) {
                break;
            }
            final long size = readUint64(header);
            switch (propertyType) {
                case NID.kEmptyStream: {
                    isEmptyStream = readBits(header, files.length);
                    break;
                }
                case NID.kEmptyFile: {
                    if (isEmptyStream == null) { // protect against NPE
                        throw new IOException("Header format error: kEmptyStream must appear before kEmptyFile");
                    }
                    isEmptyFile = readBits(header, isEmptyStream.cardinality());
                    break;
                }
                case NID.kAnti: {
                    if (isEmptyStream == null) { // protect against NPE
                        throw new IOException("Header format error: kEmptyStream must appear before kAnti");
                    }
                    isAnti = readBits(header, isEmptyStream.cardinality());
                    break;
                }
                case NID.kName: {
                    final int external = header.readUnsignedByte();
                    if (external != 0) {
                        throw new IOException("Not implemented");
                    }
                    if (((size - 1) & 1) != 0) {
                        throw new IOException("File names length invalid");
                    }
                    final byte[] names = new byte[(int)(size - 1)];
                    header.readFully(names);
                    int nextFile = 0;
                    int nextName = 0;
                    for (int i = 0; i < names.length; i += 2) {
                        if (names[i] == 0 && names[i+1] == 0) {
                            files[nextFile++].setName(new String(names, nextName, i-nextName, CharsetNames.UTF_16LE));
                            nextName = i + 2;
                        }
                    }
                    if (nextName != names.length || nextFile != files.length) {
                        throw new IOException("Error parsing file names");
                    }
                    break;
                }
                case NID.kCTime: {
                    final BitSet timesDefined = readAllOrBits(header, files.length);
                    final int external = header.readUnsignedByte();
                    if (external != 0) {
                        throw new IOException("Unimplemented");
                    }
                    for (int i = 0; i < files.length; i++) {
                        files[i].setHasCreationDate(timesDefined.get(i));
                        if (files[i].getHasCreationDate()) {
                            files[i].setCreationDate(Long.reverseBytes(header.readLong()));
                        }
                    }
                    break;
                }
                case NID.kATime: {
                    final BitSet timesDefined = readAllOrBits(header, files.length);
                    final int external = header.readUnsignedByte();
                    if (external != 0) {
                        throw new IOException("Unimplemented");
                    }
                    for (int i = 0; i < files.length; i++) {
                        files[i].setHasAccessDate(timesDefined.get(i));
                        if (files[i].getHasAccessDate()) {
                            files[i].setAccessDate(Long.reverseBytes(header.readLong()));
                        }
                    }
                    break;
                }
                case NID.kMTime: {
                    final BitSet timesDefined = readAllOrBits(header, files.length);
                    final int external = header.readUnsignedByte();
                    if (external != 0) {
                        throw new IOException("Unimplemented");
                    }
                    for (int i = 0; i < files.length; i++) {
                        files[i].setHasLastModifiedDate(timesDefined.get(i));
                        if (files[i].getHasLastModifiedDate()) {
                            files[i].setLastModifiedDate(Long.reverseBytes(header.readLong()));
                        }
                    }
                    break;
                }
                case NID.kWinAttributes: {
                    final BitSet attributesDefined = readAllOrBits(header, files.length);
                    final int external = header.readUnsignedByte();
                    if (external != 0) {
                        throw new IOException("Unimplemented");
                    }
                    for (int i = 0; i < files.length; i++) {
                        files[i].setHasWindowsAttributes(attributesDefined.get(i));
                        if (files[i].getHasWindowsAttributes()) {
                            files[i].setWindowsAttributes(Integer.reverseBytes(header.readInt()));
                        }
                    }
                    break;
                }
                case NID.kStartPos: {
                    throw new IOException("kStartPos is unsupported, please report");
                }
                case NID.kDummy: {
                    // 7z 9.20 asserts the content is all zeros and ignores the property
                    // Compress up to 1.8.1 would throw an exception, now we ignore it (see COMPRESS-287
                    
                    if (skipBytesFully(header, size) < size) {
                        throw new IOException("Incomplete kDummy property");
                    }
                    break;
                }

                default: {
                    // Compress up to 1.8.1 would throw an exception, now we ignore it (see COMPRESS-287
                    if (skipBytesFully(header, size) < size) {
                        throw new IOException("Incomplete property of type " + propertyType);
                    }
                    break;
                }
            }
        }
        int nonEmptyFileCounter = 0;
        int emptyFileCounter = 0;
        for (int i = 0; i < files.length; i++) {
            files[i].setHasStream(isEmptyStream == null ? true : !isEmptyStream.get(i));
            if (files[i].hasStream()) {
                files[i].setDirectory(false);
                files[i].setAntiItem(false);
                files[i].setHasCrc(archive.subStreamsInfo.hasCrc.get(nonEmptyFileCounter));
                files[i].setCrcValue(archive.subStreamsInfo.crcs[nonEmptyFileCounter]);
                files[i].setSize(archive.subStreamsInfo.unpackSizes[nonEmptyFileCounter]);
                ++nonEmptyFileCounter;
            } else {
                files[i].setDirectory(isEmptyFile == null ? true : !isEmptyFile.get(emptyFileCounter));
                files[i].setAntiItem(isAnti == null ? false : isAnti.get(emptyFileCounter));
                files[i].setHasCrc(false);
                files[i].setSize(0);
                ++emptyFileCounter;
            }
        }
        archive.files = files;
        calculateStreamMap(archive);
    }
    
    private void calculateStreamMap(final Archive archive) throws IOException {
        final StreamMap streamMap = new StreamMap();
        
        int nextFolderPackStreamIndex = 0;
        final int numFolders = archive.folders != null ? archive.folders.length : 0;
        streamMap.folderFirstPackStreamIndex = new int[numFolders];
        for (int i = 0; i < numFolders; i++) {
            streamMap.folderFirstPackStreamIndex[i] = nextFolderPackStreamIndex;
            nextFolderPackStreamIndex += archive.folders[i].packedStreams.length;
        }
        
        long nextPackStreamOffset = 0;
        final int numPackSizes = archive.packSizes != null ? archive.packSizes.length : 0;
        streamMap.packStreamOffsets = new long[numPackSizes];
        for (int i = 0; i < numPackSizes; i++) {
            streamMap.packStreamOffsets[i] = nextPackStreamOffset;
            nextPackStreamOffset += archive.packSizes[i]; 
        }
        
        streamMap.folderFirstFileIndex = new int[numFolders];
        streamMap.fileFolderIndex = new int[archive.files.length];
        int nextFolderIndex = 0;
        int nextFolderUnpackStreamIndex = 0;
        for (int i = 0; i < archive.files.length; i++) {
            if (!archive.files[i].hasStream() && nextFolderUnpackStreamIndex == 0) {
                streamMap.fileFolderIndex[i] = -1;
                continue;
            }
            if (nextFolderUnpackStreamIndex == 0) {
                for (; nextFolderIndex < archive.folders.length; ++nextFolderIndex) {
                    streamMap.folderFirstFileIndex[nextFolderIndex] = i;
                    if (archive.folders[nextFolderIndex].numUnpackSubStreams > 0) {
                        break;
                    }
                }
                if (nextFolderIndex >= archive.folders.length) {
                    throw new IOException("Too few folders in archive");
                }
            }
            streamMap.fileFolderIndex[i] = nextFolderIndex;
            if (!archive.files[i].hasStream()) {
                continue;
            }
            ++nextFolderUnpackStreamIndex;
            if (nextFolderUnpackStreamIndex >= archive.folders[nextFolderIndex].numUnpackSubStreams) {
                ++nextFolderIndex;
                nextFolderUnpackStreamIndex = 0;
            }
        }
        
        archive.streamMap = streamMap;
    }
    
    private void buildDecodingStream() throws IOException {
        final int folderIndex = archive.streamMap.fileFolderIndex[currentEntryIndex];
        if (folderIndex < 0) {
            deferredBlockStreams.clear();
            // TODO: previously it'd return an empty stream?
            // new BoundedInputStream(new ByteArrayInputStream(new byte[0]), 0);
            return;
        }
        final SevenZArchiveEntry file = archive.files[currentEntryIndex];
        if (currentFolderIndex == folderIndex) {
            // (COMPRESS-320).
            // The current entry is within the same (potentially opened) folder. The
            // previous stream has to be fully decoded before we can start reading
            // but don't do it eagerly -- if the user skips over the entire folder nothing
            // is effectively decompressed.

            file.setContentMethods(archive.files[currentEntryIndex - 1].getContentMethods());
        } else {
            // We're opening a new folder. Discard any queued streams/ folder stream.
            currentFolderIndex = folderIndex;
            deferredBlockStreams.clear();
            if (currentFolderInputStream != null) {
                currentFolderInputStream.close();
                currentFolderInputStream = null;
            }
            
            final Folder folder = archive.folders[folderIndex];
            final int firstPackStreamIndex = archive.streamMap.folderFirstPackStreamIndex[folderIndex];
            final long folderOffset = SIGNATURE_HEADER_SIZE + archive.packPos +
                    archive.streamMap.packStreamOffsets[firstPackStreamIndex];
            currentFolderInputStream = buildDecoderStack(folder, folderOffset, firstPackStreamIndex, file);
        }

        InputStream fileStream = new BoundedInputStream(currentFolderInputStream, file.getSize());
        if (file.getHasCrc()) {
            fileStream = new CRC32VerifyingInputStream(fileStream, file.getSize(), file.getCrcValue());
        }
        
        deferredBlockStreams.add(fileStream);
    }

    private InputStream buildDecoderStack(final Folder folder, final long folderOffset,
                final int firstPackStreamIndex, final SevenZArchiveEntry entry) throws IOException {
        file.seek(folderOffset);
        InputStream inputStreamStack =
            new BufferedInputStream(
              new BoundedRandomAccessFileInputStream(file,
                  archive.packSizes[firstPackStreamIndex]));
        final LinkedList<SevenZMethodConfiguration> methods = new LinkedList<SevenZMethodConfiguration>();
        for (final Coder coder : folder.getOrderedCoders()) {
            if (coder.numInStreams != 1 || coder.numOutStreams != 1) {
                throw new IOException("Multi input/output stream coders are not yet supported");
            }
            final SevenZMethod method = SevenZMethod.byId(coder.decompressionMethodId);
            inputStreamStack = Coders.addDecoder(fileName, inputStreamStack,
                    folder.getUnpackSizeForCoder(coder), coder, password);
            methods.addFirst(new SevenZMethodConfiguration(method,
                     Coders.findByMethod(method).getOptionsFromCoder(coder, inputStreamStack)));
        }
        entry.setContentMethods(methods);
        if (folder.hasCrc) {
            return new CRC32VerifyingInputStream(inputStreamStack,
                    folder.getUnpackSize(), folder.crc);
        }
        return inputStreamStack;
    }
    
    /**
     * Reads a byte of data.
     * 
     * @return the byte read, or -1 if end of input is reached
     * @throws IOException
     *             if an I/O error has occurred
     */
    public int read() throws IOException {
        return getCurrentStream().read();
    }
    
    private InputStream getCurrentStream() throws IOException {
        if (deferredBlockStreams.isEmpty()) {
            throw new IllegalStateException("No current 7z entry (call getNextEntry() first).");
        }
        
        while (deferredBlockStreams.size() > 1) {
            // In solid compression mode we need to decompress all leading folder'
            // streams to get access to an entry. We defer this until really needed
            // so that entire blocks can be skipped without wasting time for decompression.
            final InputStream stream = deferredBlockStreams.remove(0);
            IOUtils.skip(stream, Long.MAX_VALUE);
            stream.close();
        }

        return deferredBlockStreams.get(0);
    }

    /**
     * Reads data into an array of bytes.
     * 
     * @param b the array to write data to
     * @return the number of bytes read, or -1 if end of input is reached
     * @throws IOException
     *             if an I/O error has occurred
     */
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
    
    /**
     * Reads data into an array of bytes.
     * 
     * @param b the array to write data to
     * @param off offset into the buffer to start filling at
     * @param len of bytes to read
     * @return the number of bytes read, or -1 if end of input is reached
     * @throws IOException
     *             if an I/O error has occurred
     */
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return getCurrentStream().read(b, off, len);
    }
    
    private static long readUint64(final DataInput in) throws IOException {
        // long rather than int as it might get shifted beyond the range of an int
        final long firstByte = in.readUnsignedByte();
        int mask = 0x80;
        long value = 0;
        for (int i = 0; i < 8; i++) {
            if ((firstByte & mask) == 0) {
                return value | ((firstByte & (mask - 1)) << (8 * i));
            }
            final long nextByte = in.readUnsignedByte();
            value |= nextByte << (8 * i);
            mask >>>= 1;
        }
        return value;
    }

    /**
     * Checks if the signature matches what is expected for a 7z file.
     *
     * @param signature
     *            the bytes to check
     * @param length
     *            the number of bytes to check
     * @return true, if this is the signature of a 7z archive.
     * @since 1.8
     */
    public static boolean matches(final byte[] signature, final int length) {
        if (length < sevenZSignature.length) {
            return false;
        }

        for (int i = 0; i < sevenZSignature.length; i++) {
            if (signature[i] != sevenZSignature[i]) {
                return false;
            }
        }
        return true;
    }

    private static long skipBytesFully(final DataInput input, long bytesToSkip) throws IOException {
        if (bytesToSkip < 1) {
            return 0;
        }
        long skipped = 0;
        while (bytesToSkip > Integer.MAX_VALUE) {
            final long skippedNow = skipBytesFully(input, Integer.MAX_VALUE);
            if (skippedNow == 0) {
                return skipped;
            }
            skipped += skippedNow;
            bytesToSkip -= skippedNow;
        }
        while (bytesToSkip > 0) {
            final int skippedNow = input.skipBytes((int) bytesToSkip);
            if (skippedNow == 0) {
                return skipped;
            }
            skipped += skippedNow;
            bytesToSkip -= skippedNow;
        }
        return skipped;
    }
    
    @Override
    public String toString() {
      return archive.toString();
    }
}
