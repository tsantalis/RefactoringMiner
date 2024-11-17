package htsjdk.samtools.cram.structure;

import htsjdk.samtools.SAMRecord;

/**
 * A span of reads on a single reference.
 */
public class AlignmentSpan {
    /**
     * A constant to represent an unmapped span.
     */
    public static final AlignmentSpan UNMAPPED_SPAN = new AlignmentSpan(SAMRecord.NO_ALIGNMENT_START, 0);

    private int start;
    private int span;
    private int count;

    /**
     * Create a new span with a single read in it.
     *
     * @param start alignment start of the span
     * @param span  alignment span
     */
    public AlignmentSpan(final int start, final int span) {
        this.setStart(start);
        this.setSpan(span);
        this.count = 1;
    }

    /**
     * Create a new span with a multiple reads in it.
     *
     * @param start alignment start of the span
     * @param span  alignment span
     * @param count number of reads in the span
     */
    public AlignmentSpan(final int start, final int span, final int count) {
        this.setStart(start);
        this.setSpan(span);
        this.count = count;
    }

    /**
     * Add multiple reads to the span.
     *
     * @param start alignment start
     * @param span  alignment span
     * @param count number of reads to add
     */
    public void add(final int start, final int span, final int count) {
        if (this.getStart() > start) {
            this.setSpan(Math.max(this.getStart() + this.getSpan(), start + span) - start);
            this.setStart(start);
        } else if (this.getStart() < start) {
            this.setSpan(Math.max(this.getStart() + this.getSpan(), start + span) - this.getStart());
        } else {
            this.setSpan(Math.max(this.getSpan(), span));
        }

        this.count += count;
    }

    /**
     * Add a single read to the span
     *
     * @param start alignment start
     * @param span  read span on the reference
     */
    public void addSingle(final int start, final int span) {
        add(start, span, 1);
    }

    public int getStart() {
        return start;
    }

    public void setStart(final int start) {
        this.start = start;
    }

    public int getSpan() {
        return span;
    }

    public void setSpan(final int span) {
        this.span = span;
    }

    public int getCount() {
        return count;
    }
}
