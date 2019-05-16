package gr.uom.java.xmi.diff;

import java.util.Set;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;

public class CodeRange {
	private String filePath;
	private int startLine;
	private int endLine;
	private int startColumn;
	private int endColumn;
	private CodeElementType codeElementType;
	private String description;
	private String codeElement;

	public CodeRange(String filePath, int startLine, int endLine,
			int startColumn, int endColumn, CodeElementType codeElementType) {
		this.filePath = filePath;
		this.startLine = startLine;
		this.endLine = endLine;
		this.startColumn = startColumn;
		this.endColumn = endColumn;
		this.codeElementType = codeElementType;
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

	public CodeElementType getCodeElementType() {
		return codeElementType;
	}

	public String getDescription() {
		return description;
	}

	public CodeRange setDescription(String description) {
		this.description = description;
		return this;
	}

	public String getCodeElement() {
		return codeElement;
	}

	public CodeRange setCodeElement(String codeElement) {
		this.codeElement = codeElement;
		return this;
	}

	public String toString() {
		return startLine + "-" + endLine;
	}

	public static CodeRange computeRange(Set<AbstractCodeFragment> codeFragments) {
		String filePath = null;
		int minStartLine = 0;
		int maxEndLine = 0;
		int startColumn = 0;
		int endColumn = 0;
		
		for(AbstractCodeFragment fragment : codeFragments) {
			LocationInfo info = fragment.getLocationInfo();
			filePath = info.getFilePath();
			if(minStartLine == 0 || info.getStartLine() < minStartLine) {
				minStartLine = info.getStartLine();
				startColumn = info.getStartColumn();
			}
			if(info.getEndLine() > maxEndLine) {
				maxEndLine = info.getEndLine();
				endColumn = info.getEndColumn();
			}
		}
		return new CodeRange(filePath, minStartLine, maxEndLine, startColumn, endColumn, CodeElementType.LIST_OF_STATEMENTS);
	}
}