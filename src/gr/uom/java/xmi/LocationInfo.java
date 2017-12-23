package gr.uom.java.xmi;

public class LocationInfo {
	private String filePath;
	private int startOffset;
	private int endOffset; 
	private int length;
	private int startLine;
	private int startColumn;
	private int endLine;
	private int endColumn;
	
	public LocationInfo(String fileContents, String filePath, int startOffset, int endOffset) {
		this.filePath = filePath;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.length = endOffset - startOffset + 1;
		if (!"".equals(fileContents) && startOffset >= 0 && endOffset <= fileContents.length() - 1) {
			if (startOffset == 0) {
				this.startLine = 0;
				this.startColumn = 0;
			} else {
				String contentsBeforeStartOffset = fileContents.substring(0, startOffset); // The second parameter is endOffset - 1
				this.startLine = getNumberOfLines(contentsBeforeStartOffset);
				this.startColumn = this.startOffset - getNumberOfCharsForLines(contentsBeforeStartOffset, this.startLine);
			}
			String contentsBeforeEndOffset = fileContents.substring(0, endOffset);
			this.endLine = getNumberOfLines(contentsBeforeEndOffset);
			this.endColumn = endOffset - getNumberOfCharsForLines(contentsBeforeEndOffset, this.endLine);
		}
	}

	private int getNumberOfLines(String string) {
		int numberOfLines = 0;
		int stringLength = string.length();
		for (int i = 0; i < stringLength; i++) {
			if (string.charAt(i) == '\r') { // Handle CRLF and old UNIX style files, where CR is the only line feed character
				if (i + 1 <= string.length() - 1 && string.charAt(i + 1) == '\n') {
					i++; // Skip the next LF for CRLF
				}
				numberOfLines++;
			} else if (string.charAt(i) == '\n') {
				numberOfLines++;
			}
		}
		return numberOfLines;
	}
	
	/**
	 * Returns the number of characters in the first n lines of the given string
	 * @param string
	 * @param lines
	 * @return
	 */
	private int getNumberOfCharsForLines(String string, int lines) {
		int charsBeforeLine = 0;
		int stringLength = string.length();
		for (int i = 0; i < stringLength && lines > 0; i++) {
			if (string.charAt(i) == '\r') {
				if (i + 1 <= string.length() - 1 && string.charAt(i + 1) == '\n') {
					i++;
					charsBeforeLine++;
				}
				lines--;
			} else if (string.charAt(i) == '\n') {
				lines--;
			}
			charsBeforeLine++;
		}
		return charsBeforeLine;
	}

	public String getFilePath() {
		return filePath;
	}

	public int getStartOffset() {
		return startOffset;
	}

	public int getEndOffset() {
		return endOffset;
	}

	public int getLength() {
		return length;
	}

	/**
	 * @return 0-based start line number
	 */
	public int getStartLine() {
		return startLine;
	}

	public int getStartColumn() {
		return startColumn;
	}

	/**
	 * @return 0-based end line number
	 */
	public int getEndLine() {
		return endLine;
	}

	public int getEndColumn() {
		return endColumn;
	}
}
