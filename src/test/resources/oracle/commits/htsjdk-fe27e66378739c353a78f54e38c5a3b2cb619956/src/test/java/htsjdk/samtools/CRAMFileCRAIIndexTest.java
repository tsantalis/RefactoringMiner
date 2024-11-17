package htsjdk.samtools;

import htsjdk.HtsjdkTest;
import htsjdk.samtools.cram.build.ContainerParser;
import htsjdk.samtools.cram.build.CramContainerIterator;
import htsjdk.samtools.cram.ref.ReferenceContext;
import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.cram.structure.AlignmentSpan;
import htsjdk.samtools.cram.structure.Container;
import htsjdk.samtools.reference.FakeReferenceSequenceFile;
import htsjdk.samtools.seekablestream.ByteArraySeekableStream;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.CoordMath;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.Log;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

/**
 * Companion tests for the ones in CRAMFileBAIIndexTest, but run against a .bai
 * that has been converted from a .crai.
 *
 * A collection of tests for CRAM CRAI index write/read that use BAMFileIndexTest/index_test.bam
 * file as the source of the test data. The scan* tests check that for every records in the
 * CRAM file the query returns the same records from the CRAM file.
 */
@Test(singleThreaded = true)
public class CRAMFileCRAIIndexTest extends HtsjdkTest {
    private final File BAM_FILE = new File("src/test/resources/htsjdk/samtools/BAMFileIndexTest/index_test.bam");

    private final int nofReads = 10000 ;
    private final int nofReadsPerContainer = 1000 ;
    private final int nofUnmappedReads = 279 ;
    private final int nofMappedReads = 9721;

    private File tmpCramFile;
    private File tmpCraiFile;
    private byte[] cramBytes;
    private byte[] craiBytes;
    private ReferenceSource source;

    @Test
    public void testFileFileConstructor () throws IOException {
        CRAMFileReader reader = new CRAMFileReader(
                tmpCramFile,
                tmpCraiFile,
                source,
                ValidationStringency.STRICT);
        CloseableIterator<SAMRecord> iterator = reader.queryAlignmentStart("chrM", 1500);

        Assert.assertTrue(iterator.hasNext());
        SAMRecord record = iterator.next();
        Assert.assertEquals(record.getReferenceName(), "chrM");
        Assert.assertTrue(record.getAlignmentStart() >= 1500);
        reader.close();
    }

    @Test
    public void testStreamFileConstructor () throws IOException {
        CRAMFileReader reader = new CRAMFileReader(
                new SeekableFileStream(tmpCramFile),
                tmpCraiFile,
                source,
                ValidationStringency.STRICT);
        CloseableIterator<SAMRecord> iterator = reader.queryAlignmentStart("chrM", 1500);
        Assert.assertTrue(iterator.hasNext());
        SAMRecord record = iterator.next();

        Assert.assertEquals(record.getReferenceName(), "chrM");
        Assert.assertTrue(record.getAlignmentStart() >= 1500);
        reader.close();
    }

    @Test
    public void testStreamStreamConstructor() throws IOException {
        CRAMFileReader reader = new CRAMFileReader(
                new SeekableFileStream(tmpCramFile),
                new SeekableFileStream(tmpCraiFile),
                source,
                ValidationStringency.STRICT);
        CloseableIterator<SAMRecord> iterator = reader.queryAlignmentStart("chrM", 1500);
        Assert.assertTrue(iterator.hasNext());
        SAMRecord record = iterator.next();

        Assert.assertEquals(record.getReferenceName(), "chrM");
        Assert.assertTrue(record.getAlignmentStart() >= 1500);
        reader.close();
    }

    @Test(expectedExceptions = SAMException.class)
    public void testFileFileConstructorNoIndex () throws IOException {
        CRAMFileReader reader = new CRAMFileReader(
                new SeekableFileStream(tmpCramFile),
                (File) null,
                source,
                ValidationStringency.STRICT);
        try {
            reader.queryAlignmentStart("chrM", 1500);
        }
        finally {
            reader.close();
        }
    }

    @Test(expectedExceptions = SAMException.class)
    public void testStreamStreamConstructorNoIndex () throws IOException {
        CRAMFileReader reader = new CRAMFileReader(
                new SeekableFileStream(tmpCramFile),
                (SeekableFileStream) null,
                source,
                ValidationStringency.STRICT);
        try {
            reader.queryAlignmentStart("chrM", 1500);
        }
        finally {
            reader.close();
        }
    }

    @Test
    public void testMappedReads() throws IOException {

        try (SamReader samReader = SamReaderFactory.makeDefault().open(BAM_FILE);
             SAMRecordIterator samRecordIterator = samReader.iterator())
        {
            Assert.assertEquals(samReader.getFileHeader().getSortOrder(), SAMFileHeader.SortOrder.coordinate);
            CRAMFileReader cramReader = new CRAMFileReader(
                    new ByteArraySeekableStream(cramBytes),
                    new ByteArraySeekableStream(craiBytes),
                    source,
                    ValidationStringency.STRICT);

            int counter = 0;
            while (samRecordIterator.hasNext()) {
                SAMRecord samRecord = samRecordIterator.next();
                if (samRecord.getReferenceIndex() == SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX) {
                    break;
                }
                if (counter++ % 100 > 1) { // test only 1st and 2nd in every 100 to speed the test up:
                    continue;
                }
                String sam1 = samRecord.getSAMString();

                CloseableIterator<SAMRecord> iterator = cramReader.queryAlignmentStart(
                        samRecord.getReferenceName(),
                        samRecord.getAlignmentStart());

                Assert.assertTrue(iterator.hasNext(), counter + ": " + sam1);
                SAMRecord cramRecord = iterator.next();
                String sam2 = cramRecord.getSAMString();
                Assert.assertEquals(samRecord.getReferenceName(), cramRecord.getReferenceName(), sam1 + sam2);

                // default 'overlap' is true, so test records intersect the query:
                Assert.assertTrue(CoordMath.overlaps(
                        cramRecord.getAlignmentStart(),
                        cramRecord.getAlignmentEnd(),
                        samRecord.getAlignmentStart(),
                        samRecord.getAlignmentEnd()),
                        sam1 + sam2);
            }
            Assert.assertEquals(counter, nofMappedReads);
            cramReader.close();
        }
    }

    @Test
    public void testQueryUnmapped() throws IOException {
        try (final SamReader samReader = SamReaderFactory.makeDefault().open(BAM_FILE);
             final SAMRecordIterator unmappedSamIterator = samReader.queryUnmapped())
        {
            CRAMFileReader reader = new CRAMFileReader(
                    new ByteArraySeekableStream(cramBytes),
                    new ByteArraySeekableStream(craiBytes),
                    source,
                    ValidationStringency.STRICT);
            int counter = 0;
            CloseableIterator<SAMRecord> unmappedCramIterator = reader.queryUnmapped();

            while (unmappedSamIterator.hasNext()) {
                Assert.assertTrue(unmappedCramIterator.hasNext());
                SAMRecord r1 = unmappedSamIterator.next();
                SAMRecord r2 = unmappedCramIterator.next();
                Assert.assertEquals(r1.getReadName(), r2.getReadName());
                Assert.assertEquals(r1.getBaseQualityString(), r2.getBaseQualityString());
                counter++;
            }

            Assert.assertFalse(unmappedCramIterator.hasNext());
            Assert.assertEquals(counter, nofUnmappedReads);
        }
    }

    @Test
    public void testIteratorConstructor() throws IOException {
        final File CRAMFile = new File("src/test/resources/htsjdk/samtools/cram/auxf#values.3.0.cram");
        final File refFile = new File("src/test/resources/htsjdk/samtools/cram/auxf.fa");
        ReferenceSource refSource = new ReferenceSource(refFile);

        long[] boundaries = new long[] {0, (CRAMFile.length() - 1) << 16};
        final CRAMIterator iterator = new CRAMIterator(
                new SeekableFileStream(CRAMFile),
                refSource, boundaries,
                ValidationStringency.STRICT);
        long count = getIteratorCount(iterator);
        Assert.assertEquals(count, 2);
    }

    @Test
    public void testNoStringencyIteratorConstructor() throws IOException {
        final File CRAMFile = new File("src/test/resources/htsjdk/samtools/cram/auxf#values.3.0.cram");
        final File refFile = new File("src/test/resources/htsjdk/samtools/cram/auxf.fa");
        ReferenceSource refSource = new ReferenceSource(refFile);

        long[] boundaries = new long[] {0, (CRAMFile.length() - 1) << 16};
        final CRAMIterator iterator = new CRAMIterator(new SeekableFileStream(CRAMFile), refSource, boundaries);

        long count = getIteratorCount(iterator);
        Assert.assertEquals(count, 2);
    }

    @Test
    public void testIteratorWholeFileSpan() throws IOException {
        CRAMFileReader reader = new CRAMFileReader(
                new ByteArraySeekableStream(cramBytes),
                new ByteArraySeekableStream(craiBytes),
                source,
                ValidationStringency.STRICT);

        final SAMFileSpan allContainers = reader.getFilePointerSpanningReads();
        final CloseableIterator<SAMRecord> iterator = reader.getIterator(allContainers);
        Assert.assertTrue(iterator.hasNext());
        long count = getIteratorCount(iterator);
        Assert.assertEquals(count, nofReads);
    }

    @Test
    public void testIteratorSecondContainerSpan() throws IOException {
        CramContainerIterator it = new CramContainerIterator(new ByteArrayInputStream(cramBytes));
        it.hasNext();
        it.next();
        it.hasNext();
        Container secondContainer = it.next();
        Assert.assertNotNull(secondContainer);
        final Map<ReferenceContext, AlignmentSpan> references =
                new ContainerParser(it.getCramHeader().getSamFileHeader()).getReferences(secondContainer, ValidationStringency.STRICT);
        it.close();

        final ReferenceContext referenceContext = new TreeSet<>(references.keySet()).iterator().next();
        final AlignmentSpan alignmentSpan = references.get(referenceContext);

        CRAMFileReader reader = new CRAMFileReader(
                new ByteArraySeekableStream(cramBytes),
                new ByteArraySeekableStream(craiBytes),
                source,
                ValidationStringency.STRICT);

        final BAMIndex index = reader.getIndex();
        final SAMFileSpan spanOfSecondContainer = index.getSpanOverlapping(referenceContext.getSequenceId(), alignmentSpan.getStart(), alignmentSpan.getStart()+ alignmentSpan.getSpan());
        Assert.assertNotNull(spanOfSecondContainer);
        Assert.assertFalse(spanOfSecondContainer.isEmpty());
        Assert.assertTrue(spanOfSecondContainer instanceof BAMFileSpan);

        final CloseableIterator<SAMRecord> iterator = reader.getIterator(spanOfSecondContainer);
        Assert.assertTrue(iterator.hasNext());
        int counter = 0;
        boolean matchFound = false;
        while (iterator.hasNext()) {
            final SAMRecord record = iterator.next();
            if (record.getReferenceIndex() == referenceContext.getSequenceId()) {
                boolean overlaps = CoordMath.overlaps(record.getAlignmentStart(), record.getAlignmentEnd(), alignmentSpan.getStart(), alignmentSpan.getStart()+ alignmentSpan.getSpan());
                if (overlaps) matchFound = true;
            }
            counter++;
        }
        Assert.assertTrue(matchFound);
        Assert.assertTrue(counter <= CRAMContainerStreamWriter.DEFAULT_RECORDS_PER_SLICE);
    }

    @Test
    public void testQueryInterval() throws IOException {
        CRAMFileReader reader = new CRAMFileReader(
                new ByteArraySeekableStream(cramBytes),
                new ByteArraySeekableStream(craiBytes),
                source,
                ValidationStringency.STRICT);
        QueryInterval[] query = new QueryInterval[]{new QueryInterval(0, 1519, 1520), new QueryInterval(1, 470535, 470536)};
        final CloseableIterator<SAMRecord> iterator = reader.query(query, false);
        Assert.assertTrue(iterator.hasNext());
        SAMRecord r1 = iterator.next();
        Assert.assertEquals(r1.getReadName(), "3968040");

        Assert.assertTrue(iterator.hasNext());
        SAMRecord r2 = iterator.next();
        Assert.assertEquals(r2.getReadName(), "140419");

        Assert.assertFalse(iterator.hasNext());
        iterator.close();
        reader.close();
    }

    @Test
    public void testQueryIntervalWithFilePointers() throws IOException {
        CRAMFileReader reader = new CRAMFileReader(
                new ByteArraySeekableStream(cramBytes),
                new ByteArraySeekableStream(craiBytes),
                source,
                ValidationStringency.STRICT);
        QueryInterval[] query = new QueryInterval[]{new QueryInterval(0, 1519, 1520), new QueryInterval(1, 470535, 470536)};
        BAMFileSpan fileSpan = BAMFileReader.getFileSpan(query, reader.getIndex());
        final CloseableIterator<SAMRecord> iterator = reader.createIndexIterator(query, false, fileSpan.toCoordinateArray());
        Assert.assertTrue(iterator.hasNext());
        SAMRecord r1 = iterator.next();
        Assert.assertEquals(r1.getReadName(), "3968040");

        Assert.assertTrue(iterator.hasNext());
        SAMRecord r2 = iterator.next();
        Assert.assertEquals(r2.getReadName(), "140419");

        Assert.assertFalse(iterator.hasNext());
        iterator.close();
        reader.close();
    }

    @BeforeTest
    public void prepare() throws IOException {
        Log.setGlobalLogLevel(Log.LogLevel.ERROR);
        source = new ReferenceSource(new FakeReferenceSequenceFile(
                SamReaderFactory.makeDefault().getFileHeader(BAM_FILE).getSequenceDictionary().getSequences()));

        tmpCramFile = File.createTempFile(BAM_FILE.getName(), ".cram") ;
        tmpCramFile.deleteOnExit();
        tmpCraiFile = new File (tmpCramFile.getAbsolutePath() + ".crai");
        tmpCraiFile.deleteOnExit();
        cramBytes = cramFromBAM(BAM_FILE, source);

        FileOutputStream fos = new FileOutputStream(tmpCramFile);
        fos.write(cramBytes);
        fos.close();

        FileOutputStream fios = new FileOutputStream(tmpCraiFile);
        CRAMCRAIIndexer.writeIndex(new SeekableFileStream(tmpCramFile), fios);
        craiBytes = readFile(tmpCraiFile);
    }

    private static byte[] readFile(File file) throws FileNotFoundException {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtil.copyStream(fis, baos);
        return baos.toByteArray();
    }

    private byte[] cramFromBAM(File bamFile, ReferenceSource source) throws IOException {

        int previousValue = CRAMContainerStreamWriter.DEFAULT_RECORDS_PER_SLICE;
        CRAMContainerStreamWriter.DEFAULT_RECORDS_PER_SLICE = nofReadsPerContainer;

        try (final SamReader reader = SamReaderFactory.makeDefault().open(bamFile);
             final SAMRecordIterator iterator = reader.iterator();
             final ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            CRAMFileWriter writer = new CRAMFileWriter(
                    baos,
                    source,
                    reader.getFileHeader(),
                    bamFile.getName());
            while (iterator.hasNext()) {
                SAMRecord record = iterator.next();
                writer.addAlignment(record);
            }
            writer.close();
            return baos.toByteArray();
        }
        finally {
            // failing to reset this can cause unrelated tests to fail if this test fails
            CRAMContainerStreamWriter.DEFAULT_RECORDS_PER_SLICE = previousValue;
        }
    }

    private long getIteratorCount(Iterator<SAMRecord> it) {
        long count = 0;
        while (it.hasNext()) {
            count++;
            it.next();
        }
        return count;
    }
}
