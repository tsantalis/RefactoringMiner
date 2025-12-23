package gr.uom.java.xmi;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.kotlin.com.intellij.psi.FileViewProvider;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFile;

import extension.ast.node.LangASTNode;
import extension.ast.node.unit.LangCompilationUnit;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.diff.CodeRange;

public class LocationInfo {
	private String sourceFolder;
	private String filePath;
	private int startOffset;
	private int endOffset; 
	private int length;
	private int startLine;
	private int startColumn;
	private int endLine;
	private int endColumn;
	private int compilationUnitLength;
	private CodeElementType codeElementType;

	public LocationInfo(LangCompilationUnit cu, String sourceFolder, String filePath, LangASTNode node, CodeElementType codeElementType) {
		this.sourceFolder = sourceFolder;
		this.filePath = filePath;
		this.codeElementType = codeElementType;
		this.startOffset = node.getStartChar();
		this.length = node.getLength();
		this.endOffset = startOffset + length;

		//lines are 1-based
		this.startLine = cu.getStartLine();
		this.endLine = cu.getEndLine();
		//columns are 0-based
		this.startColumn = cu.getStartColumn();
		//convert to 1-based
		if(this.startColumn > 0) {
			this.startColumn += 1;
		}
		this.endColumn = cu.getEndColumn();
		//convert to 1-based
		if(this.endColumn > 0) {
			this.endColumn += 1;
		}
	}

	public LocationInfo(KtFile ktFile, String sourceFolder, String filePath, KtElement node, CodeElementType codeElementType) {
		this.sourceFolder = sourceFolder;
		this.filePath = filePath;
		this.codeElementType = codeElementType;
		TextRange range = node.getTextRange();
		this.startOffset = range.getStartOffset();
		this.length = range.getLength();
		this.endOffset = range.getEndOffset();

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

	public LocationInfo(KtFile ktFile, String sourceFolder, String filePath, PsiElement node, CodeElementType codeElementType) {
		this.sourceFolder = sourceFolder;
		this.filePath = filePath;
		this.codeElementType = codeElementType;
		TextRange range = node.getTextRange();
		this.startOffset = range.getStartOffset();
		this.length = range.getLength();
		this.endOffset = range.getEndOffset();

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

	public LocationInfo(CompilationUnit cu, String sourceFolder, String filePath, ASTNode node, CodeElementType codeElementType) {
		this.sourceFolder = sourceFolder;
		this.filePath = filePath;
		this.codeElementType = codeElementType;
		this.startOffset = node.getStartPosition();
		this.length = node.getLength();
		this.endOffset = startOffset + length;
		
		//lines are 1-based
		this.startLine = cu.getLineNumber(startOffset);
		this.endLine = cu.getLineNumber(endOffset);
		if(this.endLine == -1) {
			this.endLine = cu.getLineNumber(endOffset-1);
		}
		//columns are 0-based
		this.startColumn = cu.getColumnNumber(startOffset);
		//convert to 1-based
		if(this.startColumn > 0) {
			this.startColumn += 1;
		}
		this.endColumn = cu.getColumnNumber(endOffset);
		if(this.endColumn == -1) {
			this.endColumn = cu.getColumnNumber(endOffset-1);
		}
		//convert to 1-based
		if(this.endColumn > 0) {
			this.endColumn += 1;
		}
		
		int cuEndOffset = cu.getStartPosition() + cu.getLength();
		this.compilationUnitLength = cu.getLineNumber(cuEndOffset);
		if(this.compilationUnitLength == -1) {
			this.compilationUnitLength = cu.getLineNumber(cuEndOffset-1);
		}
	}

	public LocationInfo(String sourceFolder, String filePath, LangASTNode node, CodeElementType codeElementType) {
		this.sourceFolder = sourceFolder;
		this.filePath = filePath;
		this.codeElementType = codeElementType;
		this.startOffset = node.getStartChar(); // Character offset (start)
		this.length = node.getLength();
		this.endOffset = node.getEndChar();     // Character offset (end)
		this.startLine = node.getStartLine();   // 1-based
		this.endLine = node.getEndLine();   	// 1-based
		this.startColumn = 1;
		this.endColumn = 1;
	}

	public String getSourceFolder() {
		return sourceFolder;
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

	public int getCompilationUnitLength() {
		return compilationUnitLength;
	}

	public CodeElementType getCodeElementType() {
		return codeElementType;
	}

	public CodeRange codeRange() {
		return new CodeRange(getFilePath(),
				getStartLine(), getEndLine(),
				getStartColumn(), getEndColumn(), getCodeElementType(),
				getStartOffset(), getEndOffset());
	}

	public boolean before(LocationInfo other) {
		return this.filePath.equals(other.filePath) &&
				this.startOffset <= other.startOffset &&
				this.endOffset <= other.startOffset;
	}

	public boolean subsumes(LocationInfo other) {
		return this.filePath.equals(other.filePath) &&
				this.startOffset <= other.startOffset &&
				this.endOffset >= other.endOffset;
	}

	public boolean subsumes(List<? extends AbstractCodeFragment> statements) {
		int subsumedStatements = 0;
		for(AbstractCodeFragment statement : statements) {
			if(subsumes(statement.getLocationInfo())) {
				subsumedStatements++;
			}
		}
		return subsumedStatements == statements.size() && statements.size() > 0;
	}

	public boolean sameLine(LocationInfo other) {
		return this.filePath.equals(other.filePath) &&
				this.startLine == other.startLine &&
				this.endLine == other.endLine;
	}

	public boolean nextLine(LocationInfo other) {
		return this.filePath.equals(other.filePath) &&
				this.startLine == other.endLine + 1;
	}

	public boolean rightAfterNextLine(LocationInfo other) {
		return this.filePath.equals(other.filePath) &&
				this.startLine == other.endLine + 2;
	}

	public boolean startsAtTheEndLineOf(LocationInfo other) {
		return this.filePath.equals(other.filePath) &&
				this.startLine == other.endLine;
	}

	public String toString() {
		return "line range:" + startLine + "-" + endLine;
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
	
	public enum CodeElementType {
		ANNOTATION_TYPE_DECLARATION,
		ENUM_DECLARATION,
		RECORD_DECLARATION,
		TYPE_DECLARATION,
		PACKAGE_DECLARATION,
		IMPORT_DECLARATION,
		METHOD_DECLARATION,
		ANNOTATION_TYPE_MEMBER_DECLARATION,
		FIELD_DECLARATION,
		SINGLE_VARIABLE_DECLARATION,
		VARIABLE_DECLARATION_STATEMENT,
		VARIABLE_DECLARATION_EXPRESSION,
		VARIABLE_DECLARATION_INITIALIZER,
		ANONYMOUS_CLASS_DECLARATION,
		LAMBDA_EXPRESSION,
		LAMBDA_EXPRESSION_BODY,
		CLASS_INSTANCE_CREATION,
		ARRAY_CREATION,
		METHOD_INVOCATION,
		SUPER_METHOD_INVOCATION,
		TERNARY_OPERATOR,
		COMPREHENSION,
		COMPREHENSION_EXPRESSION,
		COMPREHENSION_KEY_EXPRESSION,
		COMPREHENSION_VALUE_EXPRESSION,
		COMPREHENSION_CLAUSE,
		COMPREHENSION_CLAUSE_TARGET,
		COMPREHENSION_CLAUSE_ITERABLE,
		COMPREHENSION_CLAUSE_FILTER,
		TERNARY_OPERATOR_CONDITION,
		TERNARY_OPERATOR_THEN_EXPRESSION,
		TERNARY_OPERATOR_ELSE_EXPRESSION,
		LABELED_STATEMENT,
		FOR_STATEMENT("for"),
		FOR_STATEMENT_CONDITION,
		FOR_STATEMENT_INITIALIZER,
		FOR_STATEMENT_UPDATER,
		ENHANCED_FOR_STATEMENT("for"),
		ENHANCED_FOR_STATEMENT_PARAMETER_NAME,
		ENHANCED_FOR_STATEMENT_DESTRUCTURING_DECLARATION,
		ENHANCED_FOR_STATEMENT_EXPRESSION,
		WHILE_STATEMENT("while"),
		WHILE_STATEMENT_CONDITION,
		IF_STATEMENT("if"),
		IF_STATEMENT_CONDITION,
		DO_STATEMENT("do"),
		DO_STATEMENT_CONDITION,
		SWITCH_STATEMENT("switch"),
		SWITCH_STATEMENT_CONDITION,
		SYNCHRONIZED_STATEMENT("synchronized"),
		SYNCHRONIZED_STATEMENT_EXPRESSION,
		TRY_STATEMENT("try"),
		TRY_STATEMENT_RESOURCE,
		CATCH_CLAUSE("catch"),
		CATCH_CLAUSE_EXCEPTION_NAME,
		EXPRESSION_STATEMENT,
		SWITCH_CASE,
		YIELD_STATEMENT,
		ASSERT_STATEMENT,
		RETURN_STATEMENT,
		THROW_STATEMENT,
		CONSTRUCTOR_INVOCATION,
		SUPER_CONSTRUCTOR_INVOCATION,
		BREAK_STATEMENT,
		CONTINUE_STATEMENT,
		DEL_STATEMENT,
		EMPTY_STATEMENT,
		BLOCK("{"),
		FINALLY_BLOCK("finally"),
		WITH_STATEMENT("with"),
		TYPE,
		LIST_OF_STATEMENTS,
		ANNOTATION,
		MODIFIER,
		MODULE_MODIFIER,
		SINGLE_MEMBER_ANNOTATION_VALUE,
		NORMAL_ANNOTATION_MEMBER_VALUE_PAIR,
		ENUM_CONSTANT_DECLARATION,
		RECORD_COMPONENT,
		JAVADOC, TAG_ELEMENT, DOC_ELEMENT,
		LINE_COMMENT,
		BLOCK_COMMENT,
		LAMBDA_EXPRESSION_PARAMETER,
		METHOD_REFERENCE,
		CREATION_REFERENCE,
		INITIALIZER,
		TYPE_PARAMETER,
		ANNOTATION_TYPE_MEMBER_DEFAULT_EXPRESSION,
		//expressions
		STRING_LITERAL, TEXT_BLOCK, CHAR_LITERAL, ARRAY_ACCESS, PREFIX_EXPRESSION, POSTFIX_EXPRESSION, INFIX_EXPRESSION, THIS_EXPRESSION, NUMBER_LITERAL, NULL_LITERAL, BOOLEAN_LITERAL, TYPE_LITERAL, FIELD_ACCESS, SIMPLE_NAME, EXPRESSION, QUALIFIED_NAME, CAST_EXPRESSION, PARENTHESIZED_EXPRESSION, ASSIGNMENT, INSTANCEOF_EXPRESSION, PATTERN_INSTANCEOF_EXPRESSION,
		MODULE_DECLARATION,
		REQUIRES_DIRECTIVE, PROVIDES_DIRECTIVE, USES_DIRECTIVE, EXPORTS_DIRECTIVE, OPENS_DIRECTIVE,
		DIRECTIVE_NAME,
		IMPLEMENTATION_NAME,
		YIELD_EXPRESSION,
		FUNCTION_INITIALIZER_EXPRESSION,
		PRIMARY_CONSTRUCTOR;
		
		private String name;
		
		private CodeElementType() {
			
		}
		
		private CodeElementType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
		
		public CodeElementType setName(String name) {
			this.name = name;
			return this;
		}
	}
}
