package gr.uom.java.xmi;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import gr.uom.java.xmi.diff.CodeRange;

public class LocationInfo {
	private String filePath;
	private int startOffset;
	private int endOffset; 
	private int length;
	private int startLine;
	private int startColumn;
	private int endLine;
	private int endColumn;
	
	public LocationInfo(CompilationUnit cu, String filePath, ASTNode node) {
		this.filePath = filePath;
		this.startOffset = node.getStartPosition();
		this.length = node.getLength();
		this.endOffset = startOffset + length;
		
		//lines are 1-based
		this.startLine = cu.getLineNumber(startOffset);
		this.endLine = cu.getLineNumber(endOffset);
		//columns are 0-based
		this.startColumn = cu.getColumnNumber(startOffset);
		//convert to 1-based
		if(this.startColumn > 0) {
			this.startColumn += 1;
		}
		this.endColumn = cu.getColumnNumber(endOffset);
		//convert to 1-based
		if(this.endColumn > 0) {
			this.endColumn += 1;
		}
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

	public int getStartLine() {
		return startLine;
	}

	public int getStartColumn() {
		return startColumn;
	}

	public int getEndLine() {
		return endLine;
	}

	public int getEndColumn() {
		return endColumn;
	}

	public CodeRange codeRange() {
		return new CodeRange(getFilePath(),
				getStartLine(), getEndLine(),
				getStartColumn(), getEndColumn());
	}

	public boolean subsumes(LocationInfo other) {
		return this.filePath.equals(other.filePath) &&
				this.startOffset <= other.startOffset &&
				this.endOffset >= other.endOffset;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endColumn;
		result = prime * result + endLine;
		result = prime * result + endOffset;
		result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
		result = prime * result + length;
		result = prime * result + startColumn;
		result = prime * result + startLine;
		result = prime * result + startOffset;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocationInfo other = (LocationInfo) obj;
		if (endColumn != other.endColumn)
			return false;
		if (endLine != other.endLine)
			return false;
		if (endOffset != other.endOffset)
			return false;
		if (filePath == null) {
			if (other.filePath != null)
				return false;
		} else if (!filePath.equals(other.filePath))
			return false;
		if (length != other.length)
			return false;
		if (startColumn != other.startColumn)
			return false;
		if (startLine != other.startLine)
			return false;
		if (startOffset != other.startOffset)
			return false;
		return true;
	}
}
