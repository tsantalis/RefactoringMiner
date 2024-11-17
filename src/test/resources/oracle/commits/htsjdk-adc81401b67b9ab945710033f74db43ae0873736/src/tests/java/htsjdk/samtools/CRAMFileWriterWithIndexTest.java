package htsjdk.samtools;

import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.reference.InMemoryReferenceSequenceFile;
import htsjdk.samtools.seekablestream.ByteArraySeekableStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.Log;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by vadim on 23/03/2015.
 */
public class CRAMFileWriterWithIndexTest {
    private byte[] cramBytes;
    private byte[] indexBytes;
    private InMemoryReferenceSequenceFile rsf;
    private ReferenceSource source;
    private SAMFileHeader header;

    @Test
    public void test() throws IOException {
        CRAMFileReader reader = new CRAMFileReader(new ByteArraySeekableStream(cramBytes), new ByteArraySeekableStream(indexBytes), source, ValidationStringency.SILENT);
        for (SAMSequenceRecord sequenceRecord : reader.getFileHeader().getSequenceDictionary().getSequences()) {
            final CloseableIterator<SAMRecord> iterator = reader.queryAlignmentStart(sequenceRecord.getSequenceName(), 1);
            Assert.assertNotNull(iterator);
            Assert.assertTrue(iterator.hasNext());
            SAMRecord record = iterator.next();
            Assert.assertEquals(record.getReferenceName(), sequenceRecord.getSequenceName());
            Assert.assertEquals(record.getAlignmentStart(), 1);
        }
    }

    private static class TabuRegionInputStream extends SeekableStream {
        private SeekableStream delegate;
        private List<Chunk> tabuChunks;

        public TabuRegionInputStream(List<Chunk> tabuChunks, SeekableStream delegate) {
            this.tabuChunks = tabuChunks;
            this.delegate = delegate;
        }

        private boolean isTabu(long position) {

            for (Chunk chunk : tabuChunks) {
                if ((chunk.getChunkStart() >> 16) < position && position < (chunk.getChunkEnd() >> 16)) return true;
            }

            return false;
        }

        @Override
        public long length() {
            return delegate.length();
        }

        @Override
        public long position() throws IOException {
            return delegate.position();
        }

        @Override
        public void seek(long position) throws IOException {
            if (isTabu(position)) throw new TabuError();
            delegate.seek(position);
        }

        @Override
        public int read() throws IOException {
            if (isTabu(position())) throw new TabuError();
            return delegate.read();
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            for (long pos = position(); pos < position() + length; pos++)
                if (isTabu(pos)) throw new TabuError();
            return delegate.read(buffer, offset, length);
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Override
        public boolean eof() throws IOException {
            return delegate.eof();
        }

        @Override
        public String getSource() {
            return delegate.getSource();
        }
    }

    private static class TabuError extends RuntimeException {

    }

    /**
     * This is to check that the indexing actually works and not just skips records. The approach is to forbid reading of the first
     * container and try accessing reads from the first and the second containers. The first attempt should fail but the second should succeed.
     *
     * @throws IOException
     */
    @Test
    public void testUnnecessaryIO() throws IOException {
        BAMIndex index = new CachingBAMFileIndex(new ByteArraySeekableStream(indexBytes), header.getSequenceDictionary());
        int refID = 0;
        long start = index.getSpanOverlapping(refID, 1, Integer.MAX_VALUE).getFirstOffset();
        long end = index.getSpanOverlapping(refID + 1, 1, Integer.MAX_VALUE).getFirstOffset();
        TabuRegionInputStream tabuIS = new TabuRegionInputStream(Arrays.asList(new Chunk[]{new Chunk(start, end)}), new ByteArraySeekableStream(cramBytes));

        CRAMFileReader reader = new CRAMFileReader(tabuIS, new ByteArraySeekableStream(indexBytes), source, ValidationStringency.SILENT);
        try {
            reader.queryAlignmentStart(header.getSequence(refID).getSequenceName(), 1);
            // attempt to read 1st container must fail:
            Assert.fail();
        } catch (TabuError e) {

        }

        // reading after the 1st container should be ok:
        refID = 1;
        final CloseableIterator<SAMRecord> iterator = reader.queryAlignmentStart(header.getSequence(refID).getSequenceName(), 1);
        Assert.assertNotNull(iterator);
        Assert.assertTrue(iterator.hasNext());
    }

    @BeforeTest
    public void beforeTest() throws Exception {
        Log.setGlobalLogLevel(Log.LogLevel.ERROR);

        header = new SAMFileHeader();
        header.setSortOrder(SAMFileHeader.SortOrder.coordinate);
        SAMReadGroupRecord readGroupRecord = new SAMReadGroupRecord("1");

        rsf = new InMemoryReferenceSequenceFile();
        int nofSequencesInDictionary = 30;
        int sequenceLength = 1024 * 1024;
        for (int i = 0; i < nofSequencesInDictionary; i++)
            addRandomSequence(header, sequenceLength, rsf);

        source = new ReferenceSource(rsf);

        final SAMRecordSetBuilder builder = new SAMRecordSetBuilder(false, SAMFileHeader.SortOrder.coordinate);
        builder.setHeader(header);
        builder.setReadGroup(readGroupRecord);
        header.addReadGroup(readGroupRecord);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ByteArrayOutputStream indexOS = new ByteArrayOutputStream();
        CRAMFileWriter writer = new CRAMFileWriter(os, indexOS, source, header, null);


        int readPairsPerSequence = 100;

        for (SAMSequenceRecord sequenceRecord : header.getSequenceDictionary().getSequences()) {
            int alignmentStart = 1;
            for (int i = 0; i < readPairsPerSequence / 2; i++) {
                builder.addPair(Integer.toString(i), sequenceRecord.getSequenceIndex(), alignmentStart, alignmentStart + 2);
                alignmentStart++;
            }

        }

        List<SAMRecord> list = new ArrayList<SAMRecord>(readPairsPerSequence);
        list.addAll(builder.getRecords());
        Collections.sort(list, new SAMRecordCoordinateComparator());

        for (SAMRecord record : list)
            writer.addAlignment(record);

        list.clear();
        writer.finish();
        writer.close();
        cramBytes = os.toByteArray();
        indexBytes = indexOS.toByteArray();
    }

    private static void addRandomSequence(SAMFileHeader header, int length, InMemoryReferenceSequenceFile rsf) {
        String name = String.valueOf(header.getSequenceDictionary().size() + 1);
        header.addSequence(new SAMSequenceRecord(name, length));
        byte[] refBases = new byte[length];
        Random random = new Random();
        byte[] alphabet = "ACGTN".getBytes();
        for (int i = 0; i < refBases.length; i++)
            refBases[i] = alphabet[random.nextInt(alphabet.length)];

        rsf.add(name, refBases);
    }
}
