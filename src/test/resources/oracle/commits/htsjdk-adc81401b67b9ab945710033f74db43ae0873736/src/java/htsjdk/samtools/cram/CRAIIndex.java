package htsjdk.samtools.cram;

import htsjdk.samtools.CRAMIndexer;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.cram.structure.Slice;
import htsjdk.samtools.seekablestream.SeekableMemoryStream;
import htsjdk.samtools.seekablestream.SeekableStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

/**
 * A collection of static methods to read, write and convert CRAI index.
 */
public class CRAIIndex {
    public static final String CRAI_INDEX_SUFFIX = ".crai";

    public static void writeIndex(final OutputStream os, final List<CRAIEntry> index) throws IOException {
        for (final CRAIEntry e : index) {
            os.write(e.toString().getBytes());
            os.write('\n');
        }
    }

    public static List<CRAIEntry> readIndex(final InputStream is) throws CRAIIndexException {
        final List<CRAIEntry> list = new LinkedList<CRAIEntry>();
        final Scanner scanner = new Scanner(is);

        try {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                final CRAIEntry entry = CRAIEntry.fromCraiLine(line);
                list.add(entry);
            }
        } finally {
            scanner.close();
        }

        return list;
    }

    public static List<CRAIEntry> find(final List<CRAIEntry> list, final int seqId, final int start, final int span) {
        final boolean whole = start < 1 || span < 1;
        final CRAIEntry query = new CRAIEntry();
        query.sequenceId = seqId;
        query.alignmentStart = start < 1 ? 1 : start;
        query.alignmentSpan = span < 1 ? Integer.MAX_VALUE : span;
        query.containerStartOffset = Long.MAX_VALUE;
        query.sliceOffset = Integer.MAX_VALUE;
        query.sliceSize = Integer.MAX_VALUE;

        final List<CRAIEntry> l = new ArrayList<CRAIEntry>();
        for (final CRAIEntry e : list) {
            if (e.sequenceId != seqId) {
                continue;
            }
            if (whole || CRAIEntry.intersect(e, query)) {
                l.add(e);
            }
        }
        Collections.sort(l, CRAIEntry.byStart);
        return l;
    }

    public static CRAIEntry getLeftmost(final List<CRAIEntry> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        CRAIEntry left = list.get(0);

        for (final CRAIEntry e : list) {
            if (e.alignmentStart < left.alignmentStart) {
                left = e;
            }
        }

        return left;
    }

    /**
     * Find index of the last aligned entry in the list. Assumes the index is sorted by coordinate and unmapped entries (with sequence id = -1) follow the mapped entries.
     *
     * @param list a list of CRAI entries
     * @return integer index of the last entry with sequence id not equal to -1
     */
    public static int findLastAlignedEntry(final List<CRAIEntry> list) {
        if (list.isEmpty()) {
            return -1;
        }

        int low = 0;
        int high = list.size() - 1;

        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final CRAIEntry midVal = list.get(mid);

            if (midVal.sequenceId >= 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        if (low >= list.size()) {
            return list.size() - 1;
        }
        for (; low >= 0 && list.get(low).sequenceId == -1; low--) {
        }
        return low;
    }

    public static SeekableStream openCraiFileAsBaiStream(final File cramIndexFile, final SAMSequenceDictionary dictionary) throws IOException {
        return openCraiFileAsBaiStream(new FileInputStream(cramIndexFile), dictionary);
    }

    public static SeekableStream openCraiFileAsBaiStream(final InputStream indexStream, final SAMSequenceDictionary dictionary) throws IOException, CRAIIndexException {
        final List<CRAIEntry> full = CRAIIndex.readIndex(new GZIPInputStream(indexStream));
        Collections.sort(full);

        final SAMFileHeader header = new SAMFileHeader();
        header.setSequenceDictionary(dictionary);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final CRAMIndexer indexer = new CRAMIndexer(baos, header);

        for (final CRAIEntry entry : full) {
            final Slice slice = new Slice();
            slice.containerOffset = entry.containerStartOffset;
            slice.alignmentStart = entry.alignmentStart;
            slice.alignmentSpan = entry.alignmentSpan;
            slice.sequenceId = entry.sequenceId;
            slice.nofRecords = entry.sliceSize;
            slice.index = entry.sliceIndex;
            slice.offset = entry.sliceOffset;

            indexer.processAlignment(slice);
        }
        indexer.finish();

        return new SeekableMemoryStream(baos.toByteArray(), null);
    }

    public static class CRAIIndexException extends RuntimeException {

        public CRAIIndexException(final String s) {
            super(s);
        }

        public CRAIIndexException(final NumberFormatException e) {
            super(e);
        }
    }
}
