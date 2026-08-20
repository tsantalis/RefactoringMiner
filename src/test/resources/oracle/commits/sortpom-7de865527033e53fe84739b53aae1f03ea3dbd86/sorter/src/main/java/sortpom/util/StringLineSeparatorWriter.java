package sortpom.util;

import java.io.StringWriter;

/**
 * Makes sure that all line endings are written in the same way. Keeps a buffer
 * which is flushed when newline is encountered.
 * Removes the final trailing newline by delaying it.
 *
 * @author Bjorn
 */
public class StringLineSeparatorWriter extends XmlWriter {
    private static final char NEWLINE = '\n';
    private final String lineSeparator;
    private boolean wasNewLine = false;
    private final StringBuilder lineBuffer = new StringBuilder();
    private final StringWriter out;

    StringLineSeparatorWriter(StringWriter out, final String lineSeparator) {
        this.out = out;
        this.lineSeparator = lineSeparator;
    }

    @Override
    public void write(String str) {
        char[] chars = str.toCharArray();
        for (char ch : chars) {
            writeOneCharacter(ch);
        }
    }

    @Override
    public void write(int c) {
        writeOneCharacter((char) c);
    }

    @Override
    public void close() {
        writeCharacterBuffer();
    }

    private void writeOneCharacter(char ch) {
        writeDelayedNewline();
        if (ch == NEWLINE) {
            writeCharacterBuffer();
            wasNewLine = true;
        } else {
            lineBuffer.append(ch);
        }
    }

    private void writeDelayedNewline() {
        if (wasNewLine) {
            writeLineSeparator();
            wasNewLine = false;
        }
    }

    private void writeCharacterBuffer() {
        out.write(lineBuffer.toString());
        clearLineBuffer();
    }

    private void writeLineSeparator() {
        out.write(lineSeparator);
    }

    /**
     * Remove everything that has happened since last line break. Used to clear empty lines in the XML
     **/
    public void clearLineBuffer() {
        lineBuffer.delete(0, lineBuffer.length());
    }

    @Override
    public String toString() {
        writeCharacterBuffer();
        return out.toString();
    }

    /**
     * This method will not perform anything. Flushing is only done when toString method is called
     */
    @Override
    public void flush() {
        // Nope, no manual flushing
    }
}
