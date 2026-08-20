package sortpom.util;

/**
 * This Writer will remove the space before the slash in a single xml tag.
 * The rest of the methods will only be delegated.
 */
public class NoSpaceBeforeCloseWriter extends XmlWriter {
    private final XmlWriter out;

    NoSpaceBeforeCloseWriter(XmlWriter out) {
        this.out = out;
    }

    /**
     * The XmlOutputter will write always write a space before closing the tag
     */
    @Override
    public void write(String str) {
        if (" />".equals(str)) {
            out.write("/>");
        } else {
            out.write(str);
        }
    }

    @Override
    public void write(int c) {
        out.write(c);
    }

    @Override
    public void flush() {
        out.flush();
    }

    @Override
    public void close() {
        out.close();
    }

    @Override
    public String toString() {
        return out.toString();
    }

    @Override
    public void clearLineBuffer() {
        out.clearLineBuffer();
    }
}
