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
		if (!"".equals(fileContents)) {
			if (this.length < fileContents.length()) {			
				String linesBeforeAndIncludingOffset = fileContents.substring(0, startOffset - 1);
				this.startLine = getLines(linesBeforeAndIncludingOffset).length;
				this.startColumn = this.startOffset - getNumberOfCharsForLines(linesBeforeAndIncludingOffset, this.startLine - 1);
				
				linesBeforeAndIncludingOffset = fileContents.substring(0, endOffset - 1);
				this.endLine = getLines(linesBeforeAndIncludingOffset).length;
				this.endColumn = endOffset - getNumberOfCharsForLines(linesBeforeAndIncludingOffset, this.endLine - 1);
			}
		}
	}

	private String[] getLines(String string) {
		if (string.indexOf("\n") >= 0) {
			return string.split("\n");
		} else if (string.indexOf("\r") >= 0) {
			return string.split("\r");
		}
		return new String[] { string };
	}
	
	private int getNumberOfCharsForLines(String fileContents, int line) {
		int charsBeforeLine = 0;
		String[] lines = getLines(fileContents);
		for (int i = 0; i < line && i < lines.length; i++) {
			charsBeforeLine += lines[i].length() + 1; // 1 for Line Feed character
		}
		// Happens when the last char of the document is not a line feed character
		if (charsBeforeLine > fileContents.length() - 1) {
			charsBeforeLine = fileContents.length() - 1;
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
