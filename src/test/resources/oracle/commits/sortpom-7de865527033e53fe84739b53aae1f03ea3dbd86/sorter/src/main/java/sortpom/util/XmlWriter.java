package sortpom.util;

import java.io.Writer;

/**
 * Base class for the writers used when outputting XML. Limits some of the methods
 * that wont be used in the writers (less testing for me). Removes IOException from
 * the signature for some methods (less coding for me).
 */
public abstract class XmlWriter extends Writer {
    /**
     * Only write string or write single character is allowed
     */
    @Override
    public void write(char[] cbuf) {
        throw new UnsupportedOperationException();
    }

    /**
     * Only write string or write single character is allowed
     */
    @Override
    public void write(String str, int off, int len) {
        throw new UnsupportedOperationException();
    }

    /**
     * Only write string or write single character is allowed
     */
    @Override
    public void write(char[] cbuf, int off, int len) {
        throw new UnsupportedOperationException();
    }

    /**
     * Only write string or write single character is allowed
     */
    @Override
    public Writer append(CharSequence csq) {
        throw new UnsupportedOperationException();
    }

    /**
     * Only write string or write single character is allowed
     */
    @Override
    public Writer append(CharSequence csq, int start, int end) {
        throw new UnsupportedOperationException();
    }

    /**
     * Only write string or write single character is allowed
     */
    @Override
    public Writer append(char c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove everything that has happened since last line break.
     */
    public abstract void clearLineBuffer();

    /**
     * Writes a string. Does not throw IOException
     *
     * @param str String to be written
     */
    @Override
    public abstract void write(String str);

    /**
     * Writes a single character. Does not throw IOException
     *
     * @param c int specifying a character to be written
     */
    @Override
    public abstract void write(int c);

    /**
     * Flushes the stream. Does not throw IOException
     */
    @Override
    public abstract void flush();

    /**
     * Closes the stream, flushing it first. Does not throw IOException
     */
    @Override
    public abstract void close();
}
