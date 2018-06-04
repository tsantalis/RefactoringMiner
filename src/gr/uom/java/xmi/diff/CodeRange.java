package gr.uom.java.xmi.diff;

public class CodeRange {
	private String filePath;
	private int startLine;
	private int endLine;
	private int startColumn;
	private int endColumn;

	public CodeRange(String filePath, int startLine, int endLine,
			int startColumn, int endColumn) {
		this.filePath = filePath;
		this.startLine = startLine;
		this.endLine = endLine;
		this.startColumn = startColumn;
		this.endColumn = endColumn;
	}

	public String getFilePath() {
		return filePath;
	}

	public int getStartLine() {
		return startLine;
	}

	public int getEndLine() {
		return endLine;
	}

	public int getStartColumn() {
		return startColumn;
	}

	public int getEndColumn() {
		return endColumn;
	}

	public String toString() {
		return startLine + "-" + endLine;
	}
}