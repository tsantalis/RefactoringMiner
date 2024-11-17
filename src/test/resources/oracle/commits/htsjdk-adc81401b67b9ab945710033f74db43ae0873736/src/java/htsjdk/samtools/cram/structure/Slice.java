/**
 * ****************************************************************************
 * Copyright 2013 EMBL-EBI
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */
package htsjdk.samtools.cram.structure;

import htsjdk.samtools.SAMBinaryTagAndUnsignedArrayValue;
import htsjdk.samtools.SAMBinaryTagAndValue;
import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMTagUtil;
import htsjdk.samtools.util.Log;
import htsjdk.samtools.util.SequenceUtil;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;

/**
 * CRAM slice is a logical union of blocks into for example alignment slices.
 */
public class Slice {
    public static final int UNMAPPED_OR_NO_REFERENCE = -1;
    public static final int MULTI_REFERENCE = -2;
    private static final Log log = Log.getInstance(Slice.class);

    // as defined in the specs:
    public int sequenceId = -1;
    public int alignmentStart = -1;
    public int alignmentSpan = -1;
    public int nofRecords = -1;
    public long globalRecordCounter = -1;
    public int nofBlocks = -1;
    public int[] contentIDs;
    public int embeddedRefBlockContentID = -1;
    public byte[] refMD5 = new byte[16];

    // content associated with ids:
    public Block headerBlock;
    public Block coreBlock;
    public Block embeddedRefBlock;
    public Map<Integer, Block> external;

    // for indexing purposes:
    public int offset = -1;
    public long containerOffset = -1;
    public int size = -1;
    public int index = -1;

    // to pass this to the container:
    public long bases;

    public SAMBinaryTagAndValue sliceTags;

    private void alignmentBordersSanityCheck(final byte[] ref) {
        if (alignmentStart > 0 && sequenceId >= 0 && ref == null) throw new NullPointerException("Mapped slice reference is null.");

        if (alignmentStart > ref.length) {
            log.error(String.format("Slice mapped outside of reference: seqID=%d, start=%d, counter=%d.", sequenceId, alignmentStart,
                    globalRecordCounter));
            throw new RuntimeException("Slice mapped outside of the reference.");
        }

        if (alignmentStart - 1 + alignmentSpan > ref.length) {
            log.warn(String.format("Slice partially mapped outside of reference: seqID=%d, start=%d, span=%d, counter=%d.",
                    sequenceId, alignmentStart, alignmentSpan, globalRecordCounter));
        }
    }

    public boolean validateRefMD5(final byte[] ref) {
        alignmentBordersSanityCheck(ref);

        if (!validateRefMD5(ref, alignmentStart, alignmentSpan, refMD5)) {
            final int shoulderLength = 10;
            final String excerpt = getBrief(alignmentStart, alignmentSpan, ref, shoulderLength);

            if (validateRefMD5(ref, alignmentStart, alignmentSpan - 1, refMD5)) {
                log.warn(String.format("Reference MD5 matches partially for slice %d:%d-%d, %s", sequenceId, alignmentStart,
                        alignmentStart + alignmentSpan - 1, excerpt));
                return true;
            }

            log.error(String.format("Reference MD5 mismatch for slice %d:%d-%d, %s", sequenceId, alignmentStart, alignmentStart +
                    alignmentSpan - 1, excerpt));
            return false;
        }

        return true;
    }

    private static boolean validateRefMD5(final byte[] ref, final int alignmentStart, final int alignmentSpan, final byte[] expectedMD5) {
        final int span = Math.min(alignmentSpan, ref.length - alignmentStart + 1);
        final String md5 = SequenceUtil.calculateMD5String(ref, alignmentStart - 1, span);
        return md5.equals(String.format("%032x", new BigInteger(1, expectedMD5)));
    }

    private static String getBrief(final int startOneBased, final int span, final byte[] bases, final int shoulderLength) {
        if (span >= bases.length)
            return new String(bases);

        final StringBuilder sb = new StringBuilder();
        final int fromInc = startOneBased - 1;

        int toExc = startOneBased + span - 1;
        toExc = Math.min(toExc, bases.length);

        if (toExc - fromInc <= 2 * shoulderLength) {
            sb.append(new String(Arrays.copyOfRange(bases, fromInc, toExc)));
        } else {
            sb.append(new String(Arrays.copyOfRange(bases, fromInc, fromInc + shoulderLength)));
            sb.append("...");
            sb.append(new String(Arrays.copyOfRange(bases, toExc - shoulderLength, toExc)));
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("slice: seqID %d, start %d, span %d, records %d.", sequenceId, alignmentStart, alignmentSpan, nofRecords);
    }

    public void setRefMD5(final byte[] ref) {
        alignmentBordersSanityCheck(ref);

        if (sequenceId < 0 && alignmentStart < 1) {
            refMD5 = new byte[16];
            Arrays.fill(refMD5, (byte) 0);

            log.debug("Empty slice ref md5 is set.");
        } else {

            final int span = Math.min(alignmentSpan, ref.length - alignmentStart + 1);

            if (alignmentStart + span > ref.length + 1)
                throw new RuntimeException("Invalid alignment boundaries.");

            refMD5 = SequenceUtil.calculateMD5(ref, alignmentStart - 1, span);

            if (log.isEnabled(Log.LogLevel.DEBUG)) {
                final StringBuilder sb = new StringBuilder();
                final int shoulder = 10;
                if (ref.length <= shoulder * 2)
                    sb.append(new String(ref));
                else {
                    sb.append(new String(Arrays.copyOfRange(ref,
                            alignmentStart - 1, alignmentStart + shoulder)));
                    sb.append("...");
                    sb.append(new String(Arrays.copyOfRange(ref, alignmentStart
                            - 1 + span - shoulder, alignmentStart + span)));
                }

                log.debug(String.format("Slice md5: %s for %d:%d-%d, %s",
                        String.format("%032x", new BigInteger(1, refMD5)),
                        sequenceId, alignmentStart, alignmentStart + span - 1,
                        sb.toString()));
            }
        }
    }

    /**
     * Hijacking attributes-related methods from SAMRecord:
     */

    /**
     * Get tag value attached to the slice.
     * @param tag tag ID as a short integer as returned by {@link htsjdk.samtools.SAMTagUtil#makeBinaryTag(java.lang.String)}
     * @return a value of the tag
     */
    public Object getAttribute(final short tag) {
        if (this.sliceTags == null) return null;
        else {
            final SAMBinaryTagAndValue tmp = this.sliceTags.find(tag);
            if (tmp != null) return tmp.value;
            else return null;
        }
    }

    /**
     * Set a value for the tag.
     * @param tag tag ID as a short integer as returned by {@link htsjdk.samtools.SAMTagUtil#makeBinaryTag(java.lang.String)}
     * @param value tag value
     */
    public void setAttribute(final String tag, final Object value) {
        if (value != null && value.getClass().isArray() && Array.getLength(value) == 0) {
            throw new IllegalArgumentException("Empty value passed for tag " + tag);
        }
        setAttribute(SAMTagUtil.getSingleton().makeBinaryTag(tag), value);
    }

    public void setUnsignedArrayAttribute(final String tag, final Object value) {
        if (!value.getClass().isArray()) {
            throw new IllegalArgumentException("Non-array passed to setUnsignedArrayAttribute for tag " + tag);
        }
        if (Array.getLength(value) == 0) {
            throw new IllegalArgumentException("Empty array passed to setUnsignedArrayAttribute for tag " + tag);
        }
        setAttribute(SAMTagUtil.getSingleton().makeBinaryTag(tag), value, true);
    }

    void setAttribute(final short tag, final Object value) {
        setAttribute(tag, value, false);
    }

    void setAttribute(final short tag, final Object value, final boolean isUnsignedArray) {
        if (value != null && !(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof String ||
                value instanceof Character || value instanceof Float || value instanceof byte[] || value instanceof short[] || value
                instanceof int[] || value instanceof float[])) {
            throw new SAMException("Attribute type " + value.getClass() + " not supported. Tag: " + SAMTagUtil.getSingleton()
                    .makeStringTag(tag));
        }
        if (value == null) {
            if (this.sliceTags != null) this.sliceTags = this.sliceTags.remove(tag);
        } else {
            final SAMBinaryTagAndValue tmp;
            if (!isUnsignedArray) {
                tmp = new SAMBinaryTagAndValue(tag, value);
            } else {
                if (!value.getClass().isArray() || value instanceof float[]) {
                    throw new SAMException("Attribute type " + value.getClass() + " cannot be encoded as an unsigned array. Tag: " +
                            SAMTagUtil.getSingleton().makeStringTag(tag));
                }
                tmp = new SAMBinaryTagAndUnsignedArrayValue(tag, value);
            }
            if (this.sliceTags == null) this.sliceTags = tmp;
            else this.sliceTags = this.sliceTags.insert(tmp);
        }
    }
}
