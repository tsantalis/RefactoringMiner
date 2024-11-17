package htsjdk.samtools.cram.structure;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.util.SequenceUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by vadim on 07/12/2015.
 */
public class SliceTests {
    @Test
    public void testUnmappedValidateRef() {
        Slice slice = new Slice();
        slice.alignmentStart= SAMRecord.NO_ALIGNMENT_START;
        slice.sequenceId = SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX;

        Assert.assertTrue(slice.validateRefMD5(null));
        Assert.assertTrue(slice.validateRefMD5(new byte[0]));
        Assert.assertTrue(slice.validateRefMD5(new byte[1024]));
    }

    @Test
    public void test_validateRef() {
        byte[] ref = "AAAAA".getBytes();
        final byte[] md5 = SequenceUtil.calculateMD5(ref, 0, Math.min(5, ref.length));
        Slice slice = new Slice();
        slice.sequenceId=0;
        slice.alignmentSpan=5;
        slice.alignmentStart=1;
        slice.setRefMD5(ref);

        Assert.assertEquals(slice.refMD5, md5);
        Assert.assertTrue(slice.validateRefMD5(ref));
    }
}
