package htsjdk.samtools.cram.build;

import htsjdk.HtsjdkTest;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.cram.structure.CRAMStructureTestUtil;
import htsjdk.samtools.cram.common.CramVersions;
import htsjdk.samtools.cram.common.Version;
import htsjdk.samtools.cram.ref.ReferenceContext;
import htsjdk.samtools.cram.structure.*;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by vadim on 11/01/2016.
 */
public class ContainerParserTest extends HtsjdkTest {
    private static final int TEST_RECORD_COUNT = 10;
    private static final int READ_LENGTH_FOR_TEST_RECORDS = CRAMStructureTestUtil.READ_LENGTH_FOR_TEST_RECORDS;

    private static final ContainerFactory FACTORY = new ContainerFactory(CRAMStructureTestUtil.getSAMFileHeaderForTests(), TEST_RECORD_COUNT);
    private static final ContainerParser PARSER = new ContainerParser(CRAMStructureTestUtil.getSAMFileHeaderForTests());

    @DataProvider(name = "cramVersions")
    private Object[][] cramVersions() {
        return new Object[][] {
                {CramVersions.CRAM_v2_1},
                {CramVersions.CRAM_v3}
        };
    }

    @Test(dataProvider = "cramVersions")
    public void testEOF(final Version version) throws IOException {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            CramIO.issueEOF(version, baos);
            final Container container = ContainerIO.readContainer(version, new ByteArrayInputStream(baos.toByteArray()));
            Assert.assertTrue(container.isEOF());
            Assert.assertTrue(PARSER.getRecords(container, null, ValidationStringency.STRICT).isEmpty());
        }
    }

    @DataProvider(name = "containersForRefTests")
    private Object[][] refTestData() {
        final int mappedSequenceId = 0;  // arbitrary
        final ReferenceContext mappedRefContext = new ReferenceContext(mappedSequenceId);
        return new Object[][] {
                {
                        CRAMStructureTestUtil.getSingleRefRecords(TEST_RECORD_COUNT, mappedSequenceId),
                        Collections.singleton(mappedRefContext)
                },
                {
                        CRAMStructureTestUtil.getUnmappedRecords(TEST_RECORD_COUNT),
                        Collections.singleton(ReferenceContext.UNMAPPED_UNPLACED_CONTEXT)
                },

                // these two sets of records are "half" unplaced: they have either a valid reference index or start position,
                // but not both.  We treat these weird edge cases as unplaced.

                {
                        CRAMStructureTestUtil.getHalfUnmappedNoRefRecords(TEST_RECORD_COUNT),
                        Collections.singleton(ReferenceContext.UNMAPPED_UNPLACED_CONTEXT)
                },
                {
                        CRAMStructureTestUtil.getHalfUnmappedNoStartRecords(TEST_RECORD_COUNT, mappedSequenceId),
                        Collections.singleton(ReferenceContext.UNMAPPED_UNPLACED_CONTEXT)
                },
        };
    }

    @Test(dataProvider = "containersForRefTests")
    public void paramTest(final List<CramCompressionRecord> records, final Set<ReferenceContext> expectedKeys) {
        final Container container = FACTORY.buildContainer(records);

        final Map<ReferenceContext, AlignmentSpan> spanMap = PARSER.getReferences(container, ValidationStringency.STRICT);
        Assert.assertEquals(spanMap.keySet(), expectedKeys);

        final List<CramCompressionRecord> roundTripRecords = PARSER.getRecords(container, null, ValidationStringency.STRICT);
        // TODO this fails.  return to this when refactoring Container and CramCompressionRecord
        //Assert.assertEquals(roundTripRecords, records);
        Assert.assertEquals(roundTripRecords.size(), TEST_RECORD_COUNT);
    }

   @Test
    public void testMultirefContainer() {
       final Map<ReferenceContext, AlignmentSpan> expectedSpans = new HashMap<>();
       for (int i = 0; i < TEST_RECORD_COUNT; i++) {
           expectedSpans.put(new ReferenceContext(i), new AlignmentSpan(i + 1, READ_LENGTH_FOR_TEST_RECORDS, 1));
       }

       final Container container = FACTORY.buildContainer(CRAMStructureTestUtil.getMultiRefRecords(TEST_RECORD_COUNT));

       final Map<ReferenceContext, AlignmentSpan> spanMap = PARSER.getReferences(container, ValidationStringency.STRICT);
       Assert.assertEquals(spanMap, expectedSpans);
   }

    @Test
    public void testMultirefContainerWithUnmapped() {
        final List<AlignmentSpan> expectedSpans = new ArrayList<>();
        expectedSpans.add(new AlignmentSpan(1, READ_LENGTH_FOR_TEST_RECORDS, 1));
        expectedSpans.add(new AlignmentSpan(2, READ_LENGTH_FOR_TEST_RECORDS, 1));

        final List<Container> containers = CRAMStructureTestUtil.getMultiRefContainersForStateTest();

        // first container is single-ref

        final Map<ReferenceContext, AlignmentSpan> spanMap0 = PARSER.getReferences(containers.get(0), ValidationStringency.STRICT);
        Assert.assertNotNull(spanMap0);
        Assert.assertEquals(spanMap0.size(), 1);

        Assert.assertEquals(spanMap0.get(new ReferenceContext(0)), expectedSpans.get(0));

        // when other refs are added, subsequent containers are multiref

        final Map<ReferenceContext, AlignmentSpan> spanMap1 = PARSER.getReferences(containers.get(1), ValidationStringency.STRICT);
        Assert.assertNotNull(spanMap1);
        Assert.assertEquals(spanMap1.size(), 2);

        // contains the span we checked earlier
        Assert.assertEquals(spanMap1.get(new ReferenceContext(0)), expectedSpans.get(0));
        Assert.assertEquals(spanMap1.get(new ReferenceContext(1)), expectedSpans.get(1));


        final Map<ReferenceContext, AlignmentSpan> spanMap2 = PARSER.getReferences(containers.get(2), ValidationStringency.STRICT);
        Assert.assertNotNull(spanMap2);
        Assert.assertEquals(spanMap2.size(), 3);

        // contains the spans we checked earlier
        Assert.assertEquals(spanMap2.get(new ReferenceContext(0)), expectedSpans.get(0));
        Assert.assertEquals(spanMap2.get(new ReferenceContext(1)), expectedSpans.get(1));

        Assert.assertTrue(spanMap2.containsKey(ReferenceContext.UNMAPPED_UNPLACED_CONTEXT));
        final AlignmentSpan unmappedSpan = spanMap2.get(ReferenceContext.UNMAPPED_UNPLACED_CONTEXT);

        // only checking count here because start and span aren't meaningful
        Assert.assertEquals(unmappedSpan.getCount(), 1);
    }
}
