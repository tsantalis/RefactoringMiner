package gr.uom.java.xmi.decomposition;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.kotlin.com.intellij.psi.FileViewProvider;
import org.jetbrains.kotlin.psi.KtFile;

import extension.ast.node.unit.LangCompilationUnit;
import gr.uom.java.xmi.LocationInfo;

public class VariableScope {
	private String filePath;
	private int startOffset;
	private int endOffset;
	private int startLine;
	private int startColumn;
	private int endLine;
	private int endColumn;
	private Set<AbstractCodeFragment> statementsInScopeUsingVariable = new LinkedHashSet<>();
	private String parentSignature = "";

	public VariableScope(
			LangCompilationUnit cu,
			String filePath, int startOffset, int endOffset
	) {
		this.filePath = filePath;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		/*
		this.startLine = cu.getStartLine();
		this.endLine = cu.getEndLine();
		this.startColumn = cu.getStartColumn();
		this.endColumn = cu.getEndColumn();
		//lines are 1-based
		//columns are 0-based
		if(this.startColumn > 0) {
			this.startColumn += 1;
		}
		if(this.endColumn > 0) {
			this.endColumn += 1;
		}
		*/
	}

	public VariableScope(CompilationUnit cu, String filePath, int startOffset, int endOffset) {
		this.filePath = filePath;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		
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

	public VariableScope(KtFile ktFile, String filePath, int startOffset, int endOffset) {
		this.filePath = filePath;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		
		FileViewProvider fileViewProvider = ktFile.getViewProvider();
		Document document = fileViewProvider.getDocument();

		if (document != null) {
			this.startLine = document.getLineNumber(startOffset) + 1;
			this.endLine = document.getLineNumber(endOffset) + 1;
			this.startColumn = StringUtil.offsetToLineColumn(document.getCharsSequence(), startOffset).column + 1;
			this.endColumn = StringUtil.offsetToLineColumn(document.getCharsSequence(), endOffset).column + 1;
		} else {
			this.startLine = 0;
			this.endLine = 0;
			this.startColumn = 0;
			this.endColumn = 0;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endColumn;
		result = prime * result + endLine;
		result = prime * result + endOffset;
		result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
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
		VariableScope other = (VariableScope) obj;
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
		if (startColumn != other.startColumn)
			return false;
		if (startLine != other.startLine)
			return false;
		if (startOffset != other.startOffset)
			return false;
		return true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(startLine != 0 && endLine != 0) {
			sb.append(startLine).append(":").append(startColumn);
			sb.append("-");
			sb.append(endLine).append(":").append(endColumn);
		}
		else {
			sb.append(startOffset).append("-").append(endOffset);
		}
		return sb.toString();
	}

	public void addStatementUsingVariable(AbstractCodeFragment statement) {
		this.statementsInScopeUsingVariable.add(statement);
	}

	public Set<AbstractCodeFragment> getStatementsInScopeUsingVariable() {
		return statementsInScopeUsingVariable;
	}

	public boolean subsumes(LocationInfo other) {
		return this.filePath.equals(other.getFilePath()) &&
				this.startOffset <= other.getStartOffset() &&
				this.endOffset >= other.getEndOffset();
	}
	
	public boolean overlaps(VariableScope other) {
		return this.filePath.equals(other.filePath) &&
				this.startOffset <= other.endOffset && this.endOffset >= other.startOffset;
	}
	
	public int getStartOffset() {
		return startOffset;
	}

	public int getEndOffset() {
		return endOffset;
	}

	public String getParentSignature() {
		return parentSignature;
	}

	public void setParentSignature(String parentSignature) {
		this.parentSignature = parentSignature;
	}
}
