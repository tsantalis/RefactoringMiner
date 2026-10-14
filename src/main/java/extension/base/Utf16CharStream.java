package extension.base;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.misc.Interval;

/**
 * A character stream that gives the code points to the ANTLR lexer, like CharStreams.fromString,
 * but whose indices are UTF-16 offsets in the string, so that the token offsets are the same as the offsets of Java strings,
 * i.e., an emoji, which is a surrogate pair, takes two offsets instead of one.
 */
public class Utf16CharStream implements CharStream {
	private final String data;
	private final String name;
	private int index;

	public Utf16CharStream(String data) {
		this(data, IntStream.UNKNOWN_SOURCE_NAME);
	}

	public Utf16CharStream(String data, String name) {
		this.data = data;
		this.name = name;
	}

	@Override
	public void consume() {
		if(index >= data.length()) {
			throw new IllegalStateException("cannot consume EOF");
		}
		index += Character.charCount(data.codePointAt(index));
	}

	@Override
	public int LA(int i) {
		if(i == 0) {
			return 0;
		}
		int position = index;
		if(i > 0) {
			for(int k = 1; k < i; k++) {
				if(position >= data.length()) {
					return IntStream.EOF;
				}
				position += Character.charCount(data.codePointAt(position));
			}
			return position < data.length() ? data.codePointAt(position) : IntStream.EOF;
		}
		for(int k = 0; k < -i; k++) {
			if(position <= 0) {
				return IntStream.EOF;
			}
			position -= Character.charCount(data.codePointBefore(position));
		}
		return data.codePointAt(position);
	}

	@Override
	public int mark() {
		return -1;
	}

	@Override
	public void release(int marker) {
	}

	@Override
	public int index() {
		return index;
	}

	@Override
	public void seek(int index) {
		this.index = Math.min(Math.max(index, 0), data.length());
	}

	@Override
	public int size() {
		return data.length();
	}

	@Override
	public String getSourceName() {
		return name;
	}

	@Override
	public String getText(Interval interval) {
		int start = Math.max(interval.a, 0);
		int stop = Math.min(interval.b, data.length() - 1);
		if(start > stop) {
			return "";
		}
		return data.substring(start, stop + 1);
	}

	//the UTF-16 column of the offset, i.e., the offset from the start of its line
	public int column(int offset) {
		int safeOffset = Math.min(Math.max(offset, 0), data.length());
		return safeOffset - (data.lastIndexOf('\n', safeOffset - 1) + 1);
	}

	@Override
	public String toString() {
		return data;
	}
}
