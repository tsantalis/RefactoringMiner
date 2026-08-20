package sortpom.parameter;

import sortpom.exception.FailureException;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Encapsulates LineSeparation logic.
 *
 * @author bjorn
 */
public class LineSeparatorUtil {
    private final String string;

    /**
     * Creates a line separator and makes sure that it is either &#92;n, &#92;r or &#92;r&#92;n
     *
     * @param lineSeparatorString The line separator characters
     */
    LineSeparatorUtil(final String lineSeparatorString) {
        string = lineSeparatorString.replace("\\r", "\r").replace("\\n", "\n");
        if (isIllegalString()) {
            throw new FailureException(
                    "LineSeparator must be either \\n, \\r or \\r\\n, but separator characters were "
                            + Arrays.toString(lineSeparatorString.getBytes(Charset.defaultCharset())));
        }
    }

    private boolean isIllegalString() {
        return !("\n".equalsIgnoreCase(string) ||
                "\r".equalsIgnoreCase(string) ||
                "\r\n".equalsIgnoreCase(string));
    }

    @Override
    public String toString() {
        return string;
    }

}
