package htsjdk.samtools.cram.ref;

public class GaveUpException extends RuntimeException {
    private static final long serialVersionUID = -8997576068346912410L;
    private final String md5;

    GaveUpException(final String md5) {
        this.md5 = md5;
    }

    public String getMd5() {
        return md5;
    }
}
