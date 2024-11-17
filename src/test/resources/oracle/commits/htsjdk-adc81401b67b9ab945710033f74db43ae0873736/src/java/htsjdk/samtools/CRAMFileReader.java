/*******************************************************************************
 * Copyright 2013 EMBL-EBI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package htsjdk.samtools;

import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SamReader.Type;
import htsjdk.samtools.cram.CRAIIndex;
import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.cram.structure.Container;
import htsjdk.samtools.cram.structure.ContainerIO;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.CoordMath;
import htsjdk.samtools.util.RuntimeEOFException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;

/**
 * {@link htsjdk.samtools.BAMFileReader BAMFileReader} analogue for CRAM files.
 * Supports random access using BAI index file formats.
 *
 * @author vadim
 */
@SuppressWarnings("UnusedDeclaration")
public class CRAMFileReader extends SamReader.ReaderImplementation implements SamReader.Indexing {
    private File cramFile;
    private final ReferenceSource referenceSource;
    private InputStream inputStream;
    private CRAMIterator iterator;
    private BAMIndex mIndex;
    private File mIndexFile;
    private boolean mEnableIndexCaching;
    private boolean mEnableIndexMemoryMapping;

    private ValidationStringency validationStringency;

    /**
     * Open CRAM data for reading using either the file or the input stream
     * supplied in the arguments. The
     * {@link htsjdk.samtools.Defaults#REFERENCE_FASTA default} reference fasta
     * file will be used.
     *
     * @param cramFile CRAM file to open
     * @param inputStream   CRAM stream to read
     */
    public CRAMFileReader(final File cramFile, final InputStream inputStream) {
        this(cramFile, inputStream, new ReferenceSource(Defaults.REFERENCE_FASTA));
    }

    /**
     * Open CRAM data for reading using either the file or the input stream
     * supplied in the arguments.
     *
     * @param cramFile            CRAM file to read
     * @param inputStream              index file to be used for random access
     * @param referenceSource a {@link htsjdk.samtools.cram.ref.ReferenceSource source} of
     *                        reference sequences
     */
    public CRAMFileReader(final File cramFile, final InputStream inputStream,
                          final ReferenceSource referenceSource) {
        if (cramFile == null && inputStream == null)
            throw new IllegalArgumentException(
                    "Either file or input stream is required.");

        this.cramFile = cramFile;
        this.inputStream = inputStream;
        this.referenceSource = referenceSource;
        getIterator();
    }

    /**
     * Open CRAM file for reading. If index file is supplied than random access
     * will be available.
     *
     * @param cramFile        CRAM file to read
     * @param indexFile       index file to be used for random access
     * @param referenceSource a {@link htsjdk.samtools.cram.ref.ReferenceSource source} of
     *                        reference sequences
     */
    public CRAMFileReader(final File cramFile, final File indexFile,
                          final ReferenceSource referenceSource) {
        if (cramFile == null)
            throw new IllegalArgumentException("File is required.");

        this.cramFile = cramFile;
        this.mIndexFile = indexFile;
        this.referenceSource = referenceSource;

        getIterator();
    }

    public CRAMFileReader(final File cramFile, final ReferenceSource referenceSource) {
        if (cramFile == null && inputStream == null)
            throw new IllegalArgumentException(
                    "Either file or input stream is required.");

        this.cramFile = cramFile;
        this.referenceSource = referenceSource;

        getIterator();
    }

    public CRAMFileReader(final InputStream inputStream, final SeekableStream indexInputStream,
                          final ReferenceSource referenceSource, final ValidationStringency validationStringency) throws IOException {
        this.inputStream = inputStream;
        this.referenceSource = referenceSource;
        this.validationStringency = validationStringency;

        iterator = new CRAMIterator(inputStream, referenceSource, validationStringency);
        if (indexInputStream != null) {
            try {
                mIndex = new CachingBAMFileIndex(indexInputStream, iterator.getSAMFileHeader().getSequenceDictionary());
            } catch (Exception e) {
                // try CRAI instead:
                indexInputStream.seek(0);
                final SeekableStream baiStream = CRAIIndex.openCraiFileAsBaiStream(indexInputStream, iterator.getSAMFileHeader().getSequenceDictionary());
                mIndex = new CachingBAMFileIndex(baiStream, iterator.getSAMFileHeader().getSequenceDictionary());
            }
        }
    }

    public CRAMFileReader(final InputStream stream,
                          final File indexFile, final ReferenceSource referenceSource,
                          final ValidationStringency validationStringency) throws IOException {
        this(stream, indexFile == null ? null: new SeekableFileStream(indexFile), referenceSource, validationStringency);
    }

    public CRAMFileReader(final File cramFile,
                          final File indexFile, final ReferenceSource referenceSource,
                          final ValidationStringency validationStringency) throws IOException {
        this(new FileInputStream(cramFile), indexFile, referenceSource, validationStringency);
        this.cramFile = cramFile;
    }

    @Override
    void enableIndexCaching(final boolean enabled) {
        // relevant to BAI only
        mEnableIndexCaching = enabled;
    }

    @Override
    void enableIndexMemoryMapping(final boolean enabled) {
        // relevant to BAI only
        mEnableIndexMemoryMapping = enabled;
    }

    @Override
    void enableCrcChecking(final boolean enabled) {
        // inapplicable to CRAM: do nothing
    }

    @Override
    void setSAMRecordFactory(final SAMRecordFactory factory) {
    }

    @Override
    public boolean hasIndex() {
        return mIndex != null || mIndexFile != null;
    }

    @Override
    public BAMIndex getIndex() {
        if (!hasIndex())
            throw new SAMException("No index is available for this BAM file.");
        if (mIndex == null) {
            final SAMSequenceDictionary dictionary = getFileHeader()
                    .getSequenceDictionary();
            if (mIndexFile.getName().endsWith(BAMIndex.BAMIndexSuffix)) {
                mIndex = mEnableIndexCaching ? new CachingBAMFileIndex(mIndexFile,
                        dictionary, mEnableIndexMemoryMapping)
                        : new DiskBasedBAMFileIndex(mIndexFile, dictionary,
                        mEnableIndexMemoryMapping);
                return mIndex;
            }

            if (!mIndexFile.getName().endsWith(CRAIIndex.CRAI_INDEX_SUFFIX)) return null;
            // convert CRAI into BAI:
            final SeekableStream baiStream;
            try {
                baiStream = CRAIIndex.openCraiFileAsBaiStream(mIndexFile, iterator.getSAMFileHeader().getSequenceDictionary());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            mIndex = mEnableIndexCaching ? new CachingBAMFileIndex(baiStream, getFileHeader().getSequenceDictionary()) :
                    new DiskBasedBAMFileIndex(baiStream, getFileHeader().getSequenceDictionary());
        }
        return mIndex;
    }

    @Override
    public boolean hasBrowseableIndex() {
        return false;
    }

    @Override
    public BrowseableBAMIndex getBrowseableIndex() {
        return null;
    }

    @Override
    public SAMRecordIterator iterator(final SAMFileSpan fileSpan) {
        // get the file coordinates for the span:
        final long[] coordinateArray = ((BAMFileSpan) fileSpan).toCoordinateArray();
        if (coordinateArray == null || coordinateArray.length == 0) return emptyIterator;
        try {
            // create an input stream that reads the source cram stream only within the coordinate pairs:
            final SeekableStream seekableStream = getSeekableStreamOrFailWithRTE();
            return new CRAMIterator(seekableStream, referenceSource, coordinateArray, validationStringency);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SAMFileHeader getFileHeader() {
        return iterator.getSAMFileHeader();
    }

    @Override
    public SAMRecordIterator getIterator() {
        if (iterator != null && cramFile == null)
            return iterator;
        try {
            final CRAMIterator newIterator;
            if (cramFile != null) {
                newIterator = new CRAMIterator(new FileInputStream(cramFile),
                        referenceSource, validationStringency);
            } else
                newIterator = new CRAMIterator(inputStream, referenceSource, validationStringency);

            iterator = newIterator;
            return iterator;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CloseableIterator<SAMRecord> getIterator(final SAMFileSpan fileSpan) {
        return iterator(fileSpan);
    }

    @Override
    public SAMFileSpan getFilePointerSpanningReads() {
        return new BAMFileSpan(new Chunk(iterator.firstContainerOffset << 16, Long.MAX_VALUE));
    }

    private static final SAMRecordIterator emptyIterator = new SAMRecordIterator() {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public SAMRecord next() {
            throw new RuntimeException("No records.");
        }

        @Override
        public void remove() {
            throw new RuntimeException("Remove not supported.");
        }

        @Override
        public void close() {
        }

        @Override
        public SAMRecordIterator assertSorted(final SortOrder sortOrder) {
            return this;
        }
    };

    @Override
    public CloseableIterator<SAMRecord> queryAlignmentStart(final String sequence,
                                                            final int start) {
        long[] filePointers = null;

        // Hit the index to determine the chunk boundaries for the required data.
        final SAMFileHeader fileHeader = getFileHeader();
        final int referenceIndex = fileHeader.getSequenceIndex(sequence);
        if (referenceIndex != -1) {
            final BAMIndex fileIndex = getIndex();
            final BAMFileSpan fileSpan = fileIndex.getSpanOverlapping(
                    referenceIndex, start, -1);
            filePointers = fileSpan != null ? fileSpan.toCoordinateArray()
                    : null;
        }

        if (filePointers == null || filePointers.length == 0)
            return emptyIterator;

        Container container;
        final SeekableStream seekableStream = getSeekableStreamOrFailWithRTE();
        for (int i = 0; i < filePointers.length; i += 2) {
            final long containerOffset = filePointers[i] >>> 16;

            try {
                if (seekableStream.position() != containerOffset || iterator.container == null) {
                    seekableStream.seek(containerOffset);
                    container = ContainerIO.readContainerHeader(iterator.getCramHeader().getVersion().major, seekableStream);
                    if (container.alignmentStart + container.alignmentSpan > start) {
                        seekableStream.seek(containerOffset);
                        iterator.jumpWithinContainerToPos(fileHeader.getSequenceIndex(sequence), start);
                        return new IntervalIterator(iterator, new QueryInterval(referenceIndex, start, -1));
                    }
                } else {
                    container = iterator.container;
                    if (container.alignmentStart + container.alignmentSpan > start) {
                        iterator.jumpWithinContainerToPos(fileHeader.getSequenceIndex(sequence), start);
                        return new IntervalIterator(iterator, new QueryInterval(referenceIndex, start, -1));
                    }
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
        return iterator;
    }

    CloseableIterator<SAMRecord> query(final int referenceIndex,
                                       final int start, final int end, final boolean overlap) throws IOException {
        long[] filePointers = null;

        // Hit the index to determine the chunk boundaries for the required data.
        if (referenceIndex != -1) {
            final BAMIndex fileIndex = getIndex();
            final BAMFileSpan fileSpan = fileIndex.getSpanOverlapping(
                    referenceIndex, start, -1);
            filePointers = fileSpan != null ? fileSpan.toCoordinateArray()
                    : null;
        }

        if (filePointers == null || filePointers.length == 0)
            return emptyIterator;

        final CRAMIterator newIterator = new CRAMIterator(getSeekableStreamOrFailWithRTE(), referenceSource, filePointers, validationStringency);
        return new IntervalIterator(newIterator, new QueryInterval(referenceIndex, start, end), overlap);
    }

    @Override
    public CloseableIterator<SAMRecord> queryUnmapped() {
        final long startOfLastLinearBin = getIndex().getStartOfLastLinearBin();

        final SeekableStream seekableStream = getSeekableStreamOrFailWithRTE();
        final CRAMIterator newIterator;
        try {
            seekableStream.seek(0);
            newIterator = new CRAMIterator(seekableStream, referenceSource, validationStringency);
            seekableStream.seek(startOfLastLinearBin >>> 16);
            final Container container = ContainerIO.readContainerHeader(newIterator.getCramHeader().getVersion().major, seekableStream);
            seekableStream.seek(seekableStream.position() + container.containerByteSize);
            iterator = newIterator;
            iterator.jumpWithinContainerToPos(SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX, SAMRecord.NO_ALIGNMENT_START);
        } catch (final IOException e) {
            throw new RuntimeEOFException(e);
        }

        return iterator;
    }

    private SeekableStream getSeekableStreamOrFailWithRTE() {
        SeekableStream seekableStream = null;
        if (cramFile != null) {
            try {
                seekableStream = new SeekableFileStream(cramFile);
            } catch (final FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else if (inputStream instanceof SeekableStream)
            seekableStream = (SeekableStream) inputStream;
        return seekableStream;
    }

    @Override
    public void close() {
        CloserUtil.close(iterator);
        CloserUtil.close(inputStream);
        CloserUtil.close(mIndex);
    }

    @Override
    void setValidationStringency(final ValidationStringency validationStringency) {
        this.validationStringency = validationStringency;
        if (iterator != null) iterator.setValidationStringency(validationStringency);
    }

    @Override
    public ValidationStringency getValidationStringency() {
        return validationStringency;
    }

    @Override
    public CloseableIterator<SAMRecord> query(final QueryInterval[] intervals,
                                              final boolean contained) {
        return new MultiIntervalIterator(Arrays.asList(intervals).iterator(), !contained);
    }

    @Override
    public Type type() {
        return Type.CRAM_TYPE;
    }

    @Override
    void enableFileSource(final SamReader reader, final boolean enabled) {
        if (iterator != null)
            iterator.setFileSource(enabled ? reader : null);
    }

    private class MultiIntervalIterator implements SAMRecordIterator {
        private final Iterator<QueryInterval> queries;
        private CloseableIterator<SAMRecord> iterator;
        private final boolean overlap;

        public MultiIntervalIterator(final Iterator<QueryInterval> queries, final boolean overlap) {
            this.queries = queries;
            this.overlap = overlap;
        }

        @Override
        public SAMRecordIterator assertSorted(final SortOrder sortOrder) {
            return null;
        }

        @Override
        public void close() {

        }

        @Override
        public boolean hasNext() {
            if (iterator == null || !iterator.hasNext()) {
                if (!queries.hasNext()) return false;
                do {
                    final QueryInterval query = queries.next();
                    try {
                        iterator = query(query.referenceIndex, query.start, query.end, overlap);
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                } while (!iterator.hasNext() && queries.hasNext());
            }
            return iterator.hasNext();
        }

        @Override
        public SAMRecord next() {
            return iterator.next();
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }

    public static class IntervalIterator implements SAMRecordIterator {
        private final CloseableIterator<SAMRecord> delegate;
        private final QueryInterval interval;
        private SAMRecord next;
        private boolean noMore = false;
        private final boolean overlap;

        public IntervalIterator(final CloseableIterator<SAMRecord> delegate, final QueryInterval interval) {
            this(delegate, interval, true);
        }

        public IntervalIterator(final CloseableIterator<SAMRecord> delegate, final QueryInterval interval, final boolean overlap) {
            this.delegate = delegate;
            this.interval = interval;
            this.overlap = overlap;
        }

        @Override
        public SAMRecordIterator assertSorted(final SortOrder sortOrder) {
            return null;
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public boolean hasNext() {
            if (next != null) return true;
            if (noMore) return false;

            while (delegate.hasNext()) {
                next = delegate.next();

                if (isWithinTheInterval(next)) break;
                if (isBeyondTheInterval(next)) {
                    next = null;
                    noMore = true;
                    return false;
                }
                next = null;
            }

            return next != null;
        }

        boolean isWithinTheInterval(final SAMRecord record) {
            final boolean refMatch = record.getReferenceIndex() == interval.referenceIndex;
            if (interval.start == -1) return refMatch;

            final int start = record.getAlignmentStart();
            final int end = record.getAlignmentEnd();
            if (overlap) {
                return CoordMath.overlaps(start, end, interval.start, interval.end < 0 ? Integer.MAX_VALUE : interval.end);
            } else {
                // contained:
                return CoordMath.encloses(interval.start, interval.end < 0 ? Integer.MAX_VALUE : interval.end, start, end);
            }

        }

        boolean isBeyondTheInterval(final SAMRecord record) {
            if (record.getReadUnmappedFlag()) return false;
            final boolean refMatch = record.getReferenceIndex() == interval.referenceIndex;
            return !refMatch || interval.end != -1 && record.getAlignmentStart() > interval.end;

        }

        @Override
        public SAMRecord next() {
            final SAMRecord result = next;
            next = null;
            return result;
        }

        @Override
        public void remove() {
            throw new RuntimeException("Not available.");
        }
    }
}
