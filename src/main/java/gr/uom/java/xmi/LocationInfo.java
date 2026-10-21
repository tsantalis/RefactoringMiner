package gr.uom.java.xmi;

import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTImageLocation;
import org.eclipse.cdt.core.dom.ast.IASTMacroExpansionLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.kotlin.com.intellij.psi.FileViewProvider;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFile;

import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAst;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstClassMethod;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstPrivateMethod;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstConstructor;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstClassProp;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstPrivateProp;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstAutoAccessor;
import com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsPropertySignature;
import com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsIndexSignature;
import com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsMethodSignature;
import com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsGetterSignature;
import com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsSetterSignature;
import com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsCallSignatureDecl;
import com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsConstructSignatureDecl;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstVarDecl;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstFnDecl;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstClassDecl;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstTsInterfaceDecl;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstTsTypeAliasDecl;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstTsEnumDecl;
import com.caoccao.javet.swc4j.span.Swc4jSpan;

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
		this.startLine = node.getStartLine();
		this.endLine = node.getEndLine();
		//columns are 0-based
		this.startColumn = node.getStartColumn();
		//convert to 1-based
		if(this.startColumn > 0) {
			this.startColumn += 1;
		}
		this.endColumn = node.getEndColumn();
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

	public LocationInfo(String sourceFolder, String filePath, com.github.gumtreediff.tree.Tree tree, CodeElementType codeElementType, String fileContent) {
		this(sourceFolder, filePath, tree.getPos(), tree.getLength(), tree.getEndPos(), codeElementType, fileContent);
	}

	public LocationInfo(String sourceFolder, String filePath, int startOffset, int length, int endOffset, CodeElementType codeElementType, String fileContent) {
		this.sourceFolder = sourceFolder;
		this.filePath = filePath;
		this.codeElementType = codeElementType;
		this.startOffset = startOffset;
		this.length = length;
		this.endOffset = endOffset;
		//1-based lines, also for a location starting at the beginning of a line
		this.startLine = computeLine(fileContent, startOffset);
		this.endLine = computeLine(fileContent, Math.max(startOffset, endOffset - 1));
	}

	public LocationInfo(String sourceFolder, String filePath, ISwc4jAst node, CodeElementType codeElementType, String fileContent) {
		this(sourceFolder, filePath, node.getSpan(), codeElementType, fileContent);
		if(isAmbient(node)) {
			int start = afterDeclareKeyword(fileContent, this.startOffset);
			if(start != this.startOffset) {
				String skipped = fileContent.substring(this.startOffset, start);
				if(skipped.indexOf('\n') >= 0) {
					this.startLine = computeLine(fileContent, start);
					this.startColumn = start - (fileContent.lastIndexOf('\n', start - 1) + 1);
				}
				else {
					this.startColumn += start - this.startOffset;
				}
				this.startOffset = start;
				this.length = this.endOffset - start;
			}
		}
		if(endsBeforeSeparator(node)) {
			int end = endBeforeSeparator(fileContent, this.startOffset, this.endOffset);
			if(end != this.endOffset) {
				this.endOffset = end;
				this.length = end - this.startOffset;
				this.endLine = computeLine(fileContent, Math.max(this.startOffset, end - 1));
			}
		}
	}

	//the ambient declarations start after declare, as in the AST, where declare is a sibling, i.e., declare interface A {}
	private static boolean isAmbient(ISwc4jAst node) {
		return node instanceof Swc4jAstVarDecl declaration && declaration.isDeclare() ||
				node instanceof Swc4jAstFnDecl function && function.isDeclare() ||
				node instanceof Swc4jAstClassDecl clazz && clazz.isDeclare() ||
				node instanceof Swc4jAstTsInterfaceDecl declaration && declaration.isDeclare() ||
				node instanceof Swc4jAstTsTypeAliasDecl declaration && declaration.isDeclare() ||
				node instanceof Swc4jAstTsEnumDecl declaration && declaration.isDeclare();
	}

	private static int afterDeclareKeyword(String content, int start) {
		if(!content.startsWith("declare", start) || start + 7 >= content.length() || !Character.isWhitespace(content.charAt(start + 7))) {
			return start;
		}
		int position = start + 7;
		while(position < content.length() && Character.isWhitespace(content.charAt(position))) {
			position++;
		}
		return position;
	}

	//the class and interface members without a body end before their trailing ; or , as in the AST, i.e., x: number; and f(): void;
	private static boolean endsBeforeSeparator(ISwc4jAst node) {
		if(node instanceof Swc4jAstClassMethod method) {
			return method.getFunction().getBody().isEmpty();
		}
		if(node instanceof Swc4jAstPrivateMethod method) {
			return method.getFunction().getBody().isEmpty();
		}
		if(node instanceof Swc4jAstConstructor constructor) {
			return constructor.getBody().isEmpty();
		}
		return node instanceof Swc4jAstClassProp || node instanceof Swc4jAstPrivateProp || node instanceof Swc4jAstAutoAccessor ||
				node instanceof Swc4jAstTsPropertySignature || node instanceof Swc4jAstTsIndexSignature || node instanceof Swc4jAstTsMethodSignature ||
				node instanceof Swc4jAstTsGetterSignature || node instanceof Swc4jAstTsSetterSignature ||
				node instanceof Swc4jAstTsCallSignatureDecl || node instanceof Swc4jAstTsConstructSignatureDecl;
	}

	//the end before a trailing ; or , and the whitespace before it
	public static int endBeforeSeparator(String content, int start, int end) {
		int separator = end - 1;
		while(separator > start && Character.isWhitespace(content.charAt(separator))) {
			separator--;
		}
		if(content.charAt(separator) == ';' || content.charAt(separator) == ',') {
			int trimmed = separator;
			while(trimmed > start && Character.isWhitespace(content.charAt(trimmed - 1))) {
				trimmed--;
			}
			return trimmed;
		}
		return end;
	}

	//only for the TypeScript comments, which are not AST nodes; the AST nodes use the constructor with the node, which aligns the members with the AST
	LocationInfo(String sourceFolder, String filePath, Swc4jSpan span, CodeElementType codeElementType, String fileContent) {
		this.sourceFolder = sourceFolder;
		this.filePath = filePath;
		this.codeElementType = codeElementType;
		this.startOffset = span.getStart();
		this.length = span.getEnd() - span.getStart();
		this.endOffset = span.getEnd();
		this.startLine = span.getLine();
		this.startColumn = span.getColumn();
		//compute end-line
		String text = fileContent.substring(this.startOffset, this.endOffset);
		long lines = text.lines().count();
		if(lines == 0) {
			this.endLine = this.startLine;
		}
		else {
			this.endLine = (int) (this.startLine + lines - 1);
		}
	}

	public LocationInfo(String sourceFolder, String filePath, IASTNode node, CodeElementType codeElementType, String fileContent) {
		this.sourceFolder = sourceFolder;
		this.filePath = filePath;
		this.codeElementType = codeElementType;
		IASTNodeLocation[] locations = node.getNodeLocations();
		if (locations.length == 1 && locations[0] instanceof IASTMacroExpansionLocation) {
			IASTMacroExpansionLocation macroLocation = (IASTMacroExpansionLocation) locations[0];
			IASTImageLocation imageLocation = getImageLocation(node);
			if(isSourceMacroArgument(node, imageLocation)) {
				init(fileContent, imageLocation);
			}
			else {
				init(fileContent, macroLocation.asFileLocation());
			}
		}
		else {
			init(fileContent, node.getFileLocation());
		}
	}

	//the declarator without its initializer or pure specifier, i.e., m_flag in bool m_flag = false; and f() in virtual void f() = 0; whose initializer and pure specifier are siblings of the declarator in the AST
	public LocationInfo(String sourceFolder, String filePath, IASTDeclarator declarator, CodeElementType codeElementType, String fileContent, boolean withoutInitializer) {
		this(sourceFolder, filePath, declarator, codeElementType, fileContent);
		int end = -1;
		if(withoutInitializer && declarator.getInitializer() != null) {
			end = new LocationInfo(sourceFolder, filePath, declarator.getInitializer(), codeElementType, fileContent).startOffset;
		}
		//the pure specifier is the last = of the declarator, after the = of the parameter defaults, i.e., f(int x = 1) = 0
		else if(withoutInitializer && declarator instanceof ICPPASTFunctionDeclarator functionDeclarator && functionDeclarator.isPureVirtual()) {
			end = fileContent.lastIndexOf('=', this.endOffset - 1);
		}
		if(end > this.startOffset) {
			while(end > this.startOffset && Character.isWhitespace(fileContent.charAt(end - 1))) {
				end--;
			}
			if(end > this.startOffset && end < this.endOffset) {
				this.endOffset = end;
				this.length = end - this.startOffset;
				this.endLine = computeLine(fileContent, Math.max(this.startOffset, end - 1));
				this.endColumn = computeColumn(fileContent, Math.max(this.startOffset, end - 1));
			}
		}
	}

	private LocationInfo() {
	}

	//the same location starting earlier in the file, i.e., at the macros expanded to nothing before a C++ declaration
	public LocationInfo withStartOffset(int startOffset, String fileContent) {
		LocationInfo locationInfo = new LocationInfo();
		locationInfo.sourceFolder = sourceFolder;
		locationInfo.filePath = filePath;
		locationInfo.codeElementType = codeElementType;
		locationInfo.startOffset = startOffset;
		locationInfo.endOffset = endOffset;
		locationInfo.length = endOffset - startOffset;
		locationInfo.startLine = computeLine(fileContent, startOffset);
		locationInfo.startColumn = computeColumn(fileContent, startOffset);
		locationInfo.endLine = endLine;
		locationInfo.endColumn = endColumn;
		locationInfo.compilationUnitLength = compilationUnitLength;
		return locationInfo;
	}

	private IASTImageLocation getImageLocation(IASTNode node) {
		if(node instanceof org.eclipse.cdt.internal.core.dom.parser.ASTNode astNode) {
			return astNode.getImageLocation();
		}
		return null;
	}

	private boolean isSourceMacroArgument(IASTNode node, IASTImageLocation imageLocation) {
		if(imageLocation == null || imageLocation.getLocationKind() != IASTImageLocation.ARGUMENT_TO_MACRO_EXPANSION) {
			return false;
		}
		IASTTranslationUnit translationUnit = node.getTranslationUnit();
		return translationUnit != null && translationUnit.getFilePath().equals(imageLocation.getFileName());
	}

	private void init(String fileContent, IASTFileLocation fileLocation) {
		if(fileLocation != null) {
			// Stores where node starts
			this.startOffset = fileLocation.getNodeOffset();
			// Stores characters belonging to node.
			this.length = fileLocation.getNodeLength();
			this.endOffset = startOffset + length;
			this.startLine = fileLocation.getStartingLineNumber();
			this.endLine = fileLocation.getEndingLineNumber();
			if(this.startLine <= 0) {
				this.startLine = computeLine(fileContent, startOffset);
			}
			if(this.endLine <= 0) {
				this.endLine = computeLine(fileContent, Math.max(startOffset, endOffset - 1));
			}
			// Columns are derived from the original file content so CodeRange remains UI-friendly.
			this.startColumn = computeColumn(fileContent, startOffset);
			this.endColumn = computeColumn(fileContent, Math.max(startOffset, endOffset - 1));
			this.compilationUnitLength = computeCompilationUnitLength(fileContent);
		}
	}

	// Like the line ends table of JDT CompilationUnit: the offsets of the line terminators (\n, \r, or the \n of \r\n) are computed once per file,
	// and kept per thread, as concurrent analyses (i.e., parallel tests) parse different files
	private record LineEnds(String fileContent, int[] offsets) {}
	private static final ThreadLocal<LineEnds> lastLineEnds = new ThreadLocal<>();

	private static int[] lineEnds(String fileContent) {
		LineEnds cached = lastLineEnds.get();
		if(cached != null && cached.fileContent() == fileContent) {
			return cached.offsets();
		}
		int[] offsets = new int[16];
		int count = 0;
		for(int i = 0; i < fileContent.length(); i++) {
			char character = fileContent.charAt(i);
			if(character == '\r' && i + 1 < fileContent.length() && fileContent.charAt(i + 1) == '\n') {
				i++;
			}
			else if(character != '\r' && character != '\n') {
				continue;
			}
			if(count == offsets.length) {
				offsets = Arrays.copyOf(offsets, count * 2);
			}
			offsets[count++] = i;
		}
		offsets = Arrays.copyOf(offsets, count);
		lastLineEnds.set(new LineEnds(fileContent, offsets));
		return offsets;
	}

	// The number of line terminators before the offset
	private static int lineEndsBefore(int[] lineEnds, int offset) {
		int index = Arrays.binarySearch(lineEnds, offset);
		return index >= 0 ? index : -index - 1;
	}

	private int computeLine(String fileContent, int offset) {
		if(fileContent == null || fileContent.isEmpty() || offset < 0) {
			return 0;
		}
		int safeOffset = Math.min(offset, fileContent.length());
		// Lines are 1-based.
		return lineEndsBefore(lineEnds(fileContent), safeOffset) + 1;
	}

	private int computeColumn(String fileContent, int offset) {
		if(fileContent == null || fileContent.isEmpty() || offset < 0) {
			return 0;
		}
		// Prevent searching past the end of the source text.
		int safeOffset = Math.min(offset, fileContent.length());
		int[] lineEnds = lineEnds(fileContent);
		int line = lineEndsBefore(lineEnds, safeOffset);
		int lineStart = line == 0 ? 0 : lineEnds[line - 1] + 1;
		// Columns are 1-based.
		return safeOffset - lineStart + 1;
	}

	private int computeCompilationUnitLength(String fileContent) {
		if(fileContent == null || fileContent.isEmpty()) {
			return 0;
		}
		int[] lineEnds = lineEnds(fileContent);
		// Like String.lines(), the last line is counted if it has no line terminator
		boolean lastLineTerminated = lineEnds.length > 0 && lineEnds[lineEnds.length - 1] == fileContent.length() - 1;
		return lineEnds.length + (lastLineTerminated ? 0 : 1);
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
		FORWARD_DECLARATION,
		PROBLEM_DECLARATION,
		STATIC_ASSERTION_DECLARATION,
		TYPE_ALIAS,
		OBJECT_DECLARATION,
		PACKAGE_DECLARATION,
		IMPORT_DECLARATION,
		NAMED_EXPORT,
		METHOD_DECLARATION,
		ANNOTATION_TYPE_MEMBER_DECLARATION,
		FIELD_DECLARATION,
		SINGLE_VARIABLE_DECLARATION,
		VARIABLE_DECLARATION_STATEMENT,
		VARIABLE_DECLARATION_EXPRESSION,
		VARIABLE_DECLARATION_INITIALIZER,
		TRAILING_RETURN_TYPE,
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
		WHEN_STATEMENT("when"),
		WHEN_SUBJECT_EXPRESSION,
		WHEN_ELSE_EXPRESSION,
		WHEN_ENTRY(""),
		WHEN_ENTRY_CONDITION,
		WHEN_ENTRY_EXPRESSION,
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
		EXPORT_STATEMENT,
		SWITCH_CASE("case"),
		SWITCH_CASE_TEST,
		YIELD_STATEMENT,
		ASSERT_STATEMENT,
		RETURN_STATEMENT,
		THROW_STATEMENT,
		CONSTRUCTOR_INVOCATION,
		SUPER_CONSTRUCTOR_INVOCATION,
		BREAK_STATEMENT,
		CONTINUE_STATEMENT,
		GOTO_STATEMENT,
		PROBLEM_STATEMENT,
		DEL_STATEMENT,
		EMPTY_STATEMENT,
		DEBUGGER_STATEMENT,
		PREPROCESSOR_DIRECTIVE,
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
		STRING_LITERAL, REGEX, TEXT_BLOCK, CHAR_LITERAL, ARRAY_ACCESS, PREFIX_EXPRESSION, POSTFIX_EXPRESSION, INFIX_EXPRESSION, THIS_EXPRESSION, NUMBER_LITERAL, NULL_LITERAL, BOOLEAN_LITERAL, TYPE_LITERAL, FIELD_ACCESS, SIMPLE_NAME, EXPRESSION, QUALIFIED_NAME, CAST_EXPRESSION, PARENTHESIZED_EXPRESSION, ASSIGNMENT, INSTANCEOF_EXPRESSION, PATTERN_INSTANCEOF_EXPRESSION,
		MODULE_DECLARATION,
		REQUIRES_DIRECTIVE, PROVIDES_DIRECTIVE, USES_DIRECTIVE, EXPORTS_DIRECTIVE, OPENS_DIRECTIVE,
		DIRECTIVE_NAME,
		IMPLEMENTATION_NAME,
		YIELD_EXPRESSION,
		FUNCTION_INITIALIZER_EXPRESSION,
		PRIMARY_CONSTRUCTOR,
		SUPER_TYPE_CALL_ENTRY,
		KEY_EXPRESSION,
		TUPLE_LITERAL;
		
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
