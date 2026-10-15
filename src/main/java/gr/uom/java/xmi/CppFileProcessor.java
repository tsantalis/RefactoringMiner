package gr.uom.java.xmi;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorMacroExpansion;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTProblemDeclaration;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IProblemBinding;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLinkageSpecification;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNameSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateId;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNamespace;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorElifStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorElseStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorEndifStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorErrorStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfdefStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfndefStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorMacroDefinition;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorUndefStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConversionName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionWithTryBlock;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.ExtendedScannerInfo;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTASMDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTAmbiguousSimpleDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTProblemDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTSimpleDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTASMDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTAliasDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTAmbiguousSimpleDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTExplicitTemplateInstantiation;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTInitCapture;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTLinkageSpecification;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNamespaceAlias;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNamespaceDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTProblemDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTStaticAssertionDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTStructuredBindingDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTemplateDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTemplateSpecialization;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTUsingDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTUsingDirective;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTVisibilityLabel;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplatedTypeTemplateParameter;
import org.eclipse.cdt.internal.core.index.EmptyCIndex;
import org.eclipse.cdt.internal.core.parser.IMacroDictionary;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider;
import org.eclipse.core.runtime.CoreException;
import org.refactoringminer.astDiff.utils.CppTreeGenerator;
import org.refactoringminer.util.PathFileUtils;

import com.github.gumtreediff.tree.TreeContext;

import extension.umladapter.UMLAdapterUtil;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLPreprocessorStatement.Directive;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.CppOperationBody;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class CppFileProcessor {
	private String filePath;
	private String fileContent;
	private UMLModel umlModel;
	private CppPreprocessor preprocessor;
	//the start offsets of the macros expanded to nothing in the file by their end offsets, i.e., DUCKDB_API in DUCKDB_API virtual ~A() = default;
	private TreeMap<Integer, Integer> emptyMacroExpansions = new TreeMap<>();

	public CppFileProcessor(UMLModel umlModel) {
		this.umlModel = umlModel;
	}

	public void processCppFile(String filePath, String fileContent, boolean astDiff) {
		this.filePath = filePath;
		this.fileContent = fileContent;
		this.preprocessor = new CppPreprocessor(this);
		try {
			if(PathFileUtils.isCppFile(filePath) || PathFileUtils.isCFile(filePath)) {
				IASTTranslationUnit ast = parse(filePath, fileContent, astDiff);
				this.emptyMacroExpansions = emptyMacroExpansions(ast);
				if (astDiff) {
					//a single parse for the model and the AST diff tree
					TreeContext treeContext = CppTreeGenerator.generate(fileContent, ast);
					this.umlModel.getTreeContextMap().put(filePath, treeContext);
				}
				String sourceFolder = extractCppSourceFolder();
				List<UMLComment> comments = extractInternalComments(ast.getComments(), sourceFolder, filePath, fileContent);
				this.umlModel.getCommentMap().put(filePath, comments);
				UMLClass moduleClass = createModuleClass(ast, sourceFolder);
				preprocessor.buildConditionalBranches(ast.getAllPreprocessorStatements());
				processPreprocessorStatements(sourceFolder, moduleClass, ast.getAllPreprocessorStatements());
				preprocessor.processDeclarations(moduleClass.getName(), sourceFolder, moduleClass,
						ast.getDeclarations(true), comments, new ICPPASTTemplateParameter[0]);
				this.umlModel.addClass(moduleClass);
				//add remaining comments to moduleClass
				//TODO consider assigning comments to individual preprocessor statements
				moduleClass.getComments().addAll(comments);
			}
		}
		catch(CoreException e) {

		}
	}

	private List<UMLComment> extractInternalComments(IASTComment[] astComments, String sourceFolder, String sourceFile, String fileContent) {
		List<UMLComment> comments = new ArrayList<UMLComment>();
		for(IASTComment comment : astComments) {
			if(!isPartOfFile(comment)) {
				continue;
			}
			LocationInfo locationInfo = null;
			if(comment.isBlockComment()) {
				locationInfo = new LocationInfo(sourceFolder, sourceFile, comment, CodeElementType.BLOCK_COMMENT, fileContent);
			}
			else {
				locationInfo = new LocationInfo(sourceFolder, sourceFile, comment, CodeElementType.LINE_COMMENT, fileContent);
			}
			if(locationInfo != null) {
				String text = comment.getRawSignature();
				UMLComment umlComment = new UMLComment(text, locationInfo);
				comments.add(umlComment);
			}
		}
		return comments;
	}

	private void distributeComments(List<UMLComment> compilationUnitComments, LocationInfo codeElementLocationInfo, List<UMLComment> codeElementComments) {
		ListIterator<UMLComment> listIterator = compilationUnitComments.listIterator(compilationUnitComments.size());
		while(listIterator.hasPrevious()) {
			UMLComment comment = listIterator.previous();
			LocationInfo commentLocationInfo = comment.getLocationInfo();
			if(codeElementLocationInfo.subsumes(commentLocationInfo) ||
					codeElementLocationInfo.sameLine(commentLocationInfo) ||
					(commentLocationInfo.startsAtTheEndLineOf(codeElementLocationInfo) && !codeElementLocationInfo.getCodeElementType().equals(CodeElementType.ANONYMOUS_CLASS_DECLARATION)) ||
					(codeElementLocationInfo.nextLine(commentLocationInfo) && !codeElementLocationInfo.getCodeElementType().equals(CodeElementType.ANONYMOUS_CLASS_DECLARATION)) ||
					(codeElementComments.size() > 0 && codeElementComments.get(0).getLocationInfo().nextLine(commentLocationInfo))) {
				codeElementComments.add(0, comment);
			}
			if(commentLocationInfo.nextLine(codeElementLocationInfo) || commentLocationInfo.rightAfterNextLine(codeElementLocationInfo)) {
				comment.addPreviousLocation(codeElementLocationInfo);
			}
		}
		compilationUnitComments.removeAll(codeElementComments);
	}

	private void processPreprocessorStatements(String sourceFolder, UMLClass moduleClass, IASTPreprocessorStatement[] allPreprocessorStatements) {
		for(IASTPreprocessorStatement statement : allPreprocessorStatements) {
			if(!isPartOfFile(statement)) {
				continue;
			}
			LocationInfo locationInfo = new LocationInfo(
				sourceFolder,
				filePath,
				statement,
				CodeElementType.PREPROCESSOR_DIRECTIVE,
				fileContent
			);	
			IASTName nameNode;
			String name;
			String value;
			UMLPreprocessorStatement preprocessorStatement = null;
			
			if(statement instanceof IASTPreprocessorMacroDefinition macroDefinition) {
				nameNode = macroDefinition.getName();
				name = nameNode.toString();
				value = macroDefinition.getExpansion();
				preprocessorStatement = new UMLPreprocessorStatement(locationInfo,Directive.DEFINE,value,name);
			}
			else if(statement instanceof IASTPreprocessorIncludeStatement includeStatement) {
				nameNode = includeStatement.getName();
				name = nameNode.toString();
				value = includeStatement.getPath();
				preprocessorStatement = new UMLPreprocessorStatement(locationInfo,Directive.INCLUDE,value,name);
			}
			else if(statement instanceof IASTPreprocessorUndefStatement undefStatement) {
				nameNode = undefStatement.getMacroName();
				name = nameNode.toString();
				preprocessorStatement = new UMLPreprocessorStatement(locationInfo,Directive.UNDEF,name);
			}
			else if(statement instanceof IASTPreprocessorIfStatement ifStatement) {
				//convert char[] to string
				value = new String(ifStatement.getCondition());
				preprocessorStatement = new UMLPreprocessorStatement(locationInfo,Directive.IF,value);
			}
			else if(statement instanceof IASTPreprocessorIfdefStatement ifdefStatement) {
				//convert char[] to string
				value = new String(ifdefStatement.getCondition());
				preprocessorStatement = new UMLPreprocessorStatement(locationInfo,Directive.IFDEF,value);
			}
			else if(statement instanceof IASTPreprocessorIfndefStatement ifndefStatement) {
				//convert char[] to string
				value = new String(ifndefStatement.getCondition());
				preprocessorStatement = new UMLPreprocessorStatement(locationInfo,Directive.IFNDEF,value);
			}
			else if(statement instanceof IASTPreprocessorElifStatement elifStatement) {
				//convert char[] to string
				value = new String(elifStatement.getCondition());
				preprocessorStatement = new UMLPreprocessorStatement(locationInfo,Directive.ELIF,value);
				attachRootIf(preprocessorStatement, moduleClass);
			}
			else if(statement instanceof IASTPreprocessorElseStatement elseStatement) {
				preprocessorStatement = new UMLPreprocessorStatement(locationInfo,Directive.ELSE);
				attachRootIf(preprocessorStatement, moduleClass);
			}
			else if(statement instanceof IASTPreprocessorEndifStatement endifStatement) {
				preprocessorStatement = new UMLPreprocessorStatement(locationInfo,Directive.ENDIF);
				attachRootIf(preprocessorStatement, moduleClass);
			}
			else if(statement instanceof IASTPreprocessorPragmaStatement pragmaStatement) {
				//convert char[] to string
				value = new String(pragmaStatement.getMessage());
				preprocessorStatement = new UMLPreprocessorStatement(locationInfo,Directive.PRAGMA,value);
			}
			else if(statement instanceof IASTPreprocessorErrorStatement errorStatement) {
				//convert char[] to string
				value = new String(errorStatement.getMessage());
				preprocessorStatement = new UMLPreprocessorStatement(locationInfo,Directive.ERROR,value);
			}
			if(preprocessorStatement != null) {
				moduleClass.addPreprocessorStatement(preprocessorStatement);
			}
		}
	}

	private void attachRootIf(UMLPreprocessorStatement preprocessorStatement, UMLClass moduleClass) {
		if(moduleClass.getPreprocessorStatements().size() > 0) {
			for(int i = moduleClass.getPreprocessorStatements().size()-1; i>0; i--) {
				UMLPreprocessorStatement statement = moduleClass.getPreprocessorStatements().get(i);
				if(statement.getType().equals(Directive.IF) || statement.getType().equals(Directive.IFDEF) || statement.getType().equals(Directive.IFNDEF)) {
					preprocessorStatement.setPreviousStatement(statement);
					break;
				}
			}
		}
	}

	private TreeMap<Integer, Integer> emptyMacroExpansions(IASTTranslationUnit translationUnit) {
		TreeMap<Integer, Integer> expansions = new TreeMap<>();
		for(IASTPreprocessorMacroExpansion expansion : translationUnit.getMacroExpansions()) {
			IASTFileLocation location = expansion.getFileLocation();
			if(location != null && isPartOfFile(expansion) && expansion.getMacroDefinition().getExpansion().isBlank()) {
				expansions.put(location.getNodeOffset() + location.getNodeLength(), location.getNodeOffset());
			}
		}
		return expansions;
	}

	//the location starting at the macros expanded to nothing right before it, which CDT excludes from the location, like the AST diff tree
	private LocationInfo withEmptyMacros(LocationInfo locationInfo) {
		int start = locationInfo.getStartOffset();
		Map.Entry<Integer, Integer> expansion = emptyMacroExpansions.floorEntry(start);
		while(expansion != null && fileContent.substring(expansion.getKey(), start).isBlank() && !inDirective(expansion.getValue())) {
			start = expansion.getValue();
			expansion = emptyMacroExpansions.floorEntry(start);
		}
		if(start == locationInfo.getStartOffset()) {
			return locationInfo;
		}
		return locationInfo.withStartOffset(start, fileContent);
	}

	//whether the offset is in a preprocessor directive, i.e., X in #if X
	private boolean inDirective(int offset) {
		int lineStart = fileContent.lastIndexOf('\n', offset - 1) + 1;
		return fileContent.substring(lineStart, offset).strip().startsWith("#");
	}

	public static IASTTranslationUnit parse(String filePath, String fileContent, boolean astDiff) throws CoreException {
		IASTTranslationUnit translationUnit = parse(filePath, fileContent, astDiff, "");
		//CDT parses the declarations starting with the macros defined in the headers it does not have, i.e., DUCKDB_API virtual void f();,
		//as problems or as declarations of variables whose type is the macro, so the file is parsed again with these macros defined as empty
		//in an included file, which does not change the offsets of the file
		String macros = unknownDeclarationMacros(translationUnit, fileContent);
		if(!macros.isEmpty()) {
			IASTTranslationUnit reparsed = parse(filePath, fileContent, astDiff, macros);
			if(problems(reparsed) <= problems(translationUnit)) {
				translationUnit = reparsed;
			}
			else {
				macros = "";
			}
		}
		//CDT parses the calls of the function templates it does not know as comparisons, i.e., std::make_unique<T>(x) as (std::make_unique < T) > (x),
		//so the file is parsed again with the declarations of these function templates in the included file
		String preamble = macros;
		String templates = misparsedFunctionTemplates(translationUnit, fileContent);
		if(!templates.isEmpty()) {
			String extendedPreamble = preamble.isEmpty() ? templates : preamble + "\n" + templates;
			IASTTranslationUnit reparsed = parse(filePath, fileContent, astDiff, extendedPreamble);
			if(problems(reparsed) <= problems(translationUnit)) {
				translationUnit = reparsed;
				preamble = extendedPreamble;
			}
		}
		//CDT parses the declarations of the variables whose type is a class template it does not know as comparisons,
		//i.e., std::vector<T> x = e; as (std::vector < T) > x = e, so the file is parsed again with the declarations of these types in the included file
		String types = misparsedTemplateTypes(translationUnit, fileContent, preamble);
		if(!types.isEmpty()) {
			IASTTranslationUnit reparsed = parse(filePath, fileContent, astDiff, preamble.isEmpty() ? types : preamble + "\n" + types);
			if(problems(reparsed) <= problems(translationUnit)) {
				translationUnit = reparsed;
			}
		}
		return translationUnit;
	}

	//the start of a declaration of a variable whose type is a class template, i.e., ns::T<A> x = e;
	private static final Pattern TEMPLATE_TYPE_DECLARATION = Pattern.compile("[A-Za-z_][\\w:]*\\s*<[^;{}()]*>\\s*[*&]*\\s*[A-Za-z_]\\w*\\s*(=|;|\\{|\\()");

	//the declarations of the unknown class templates and of the unknown types in their template arguments in the declarations of variables
	//that CDT parsed as comparisons, in their namespaces or in the unknown classes containing them
	private static String misparsedTemplateTypes(IASTTranslationUnit translationUnit, String fileContent, String preamble) {
		List<int[]> ranges = new ArrayList<>();
		Map<String, IASTName> names = new HashMap<>();
		Set<String> owners = new LinkedHashSet<>();
		Map<String, Boolean> types = new HashMap<>();
		translationUnit.accept(new ASTVisitor() {
			{
				shouldVisitStatements = true;
			}

			@Override
			public int visit(IASTStatement statement) {
				IASTFileLocation location = statement.getFileLocation();
				if(statement instanceof IASTExpressionStatement expressionStatement && expressionStatement.getExpression() instanceof IASTBinaryExpression
						&& location != null && isPartOfFile(statement)) {
					int start = location.getNodeOffset();
					int end = start + location.getNodeLength();
					Matcher matcher = TEMPLATE_TYPE_DECLARATION.matcher(fileContent);
					matcher.region(start, Math.min(end, fileContent.length()));
					if(matcher.lookingAt()) {
						//the type of the declaration, without the initializer, i.e., ns::T<A> in ns::T<A> x = f<B>(y);
						ranges.add(new int[] {start, matcher.end()});
						statement.accept(new ASTVisitor() {
							{
								shouldVisitNames = true;
							}

							@Override
							public int visit(IASTName name) {
								names.putIfAbsent(name.toString().replaceAll("\\s+", ""), name);
								return PROCESS_SKIP;
							}
						});
						String owner = ownerClassDeclaration(expressionStatement.getExpression(), fileContent, types);
						if(owner != null && !preamble.contains(owner)) {
							owners.add(owner);
						}
					}
				}
				return PROCESS_CONTINUE;
			}
		});
		if(ranges.isEmpty()) {
			return "";
		}
		Scope root = new Scope(true);
		for(Map.Entry<String, Boolean> entry : CppTreeGenerator.templateTypes(fileContent, ranges).entrySet()) {
			IASTName name = names.get(entry.getKey());
			if(name == null || name instanceof ICPPASTTemplateId || !(name.getLastName().resolveBinding() instanceof IProblemBinding)
					|| preamble.contains(" " + name.getLastName() + "(...);")) {
				continue;
			}
			Scope scope = root;
			if(name instanceof ICPPASTQualifiedName qualifiedName) {
				for(ICPPASTNameSpecifier qualifier : qualifiedName.getQualifier()) {
					if(!(qualifier instanceof IASTName qualifierName) || qualifierName instanceof ICPPASTTemplateId) {
						scope = null;
						break;
					}
					//a namespace, or a class that CDT does not know
					boolean namespace = isNamespace(qualifierName, fileContent, types);
					if(!namespace && !(qualifierName.resolveBinding() instanceof IProblemBinding)) {
						scope = null;
						break;
					}
					scope = scope.child(qualifierName.toString(), namespace);
					if(scope == null) {
						break;
					}
				}
			}
			if(scope != null) {
				scope.declarations.add(entry.getValue() ? "template<class... __rm_t__> struct " + name.getLastName() + ";" : "struct " + name.getLastName() + ";");
			}
		}
		StringBuilder code = new StringBuilder();
		root.append(code);
		for(String owner : owners) {
			code.append(owner).append('\n');
		}
		return code.toString().strip();
	}

	//a namespace or a class with the declarations of the types it contains
	private static class Scope {
		private final boolean namespace;
		private final Set<String> declarations = new LinkedHashSet<>();
		private final Map<String, Scope> children = new LinkedHashMap<>();

		private Scope(boolean namespace) {
			this.namespace = namespace;
		}

		//the scope with the name, or null when the name is both a namespace and a class
		private Scope child(String name, boolean namespace) {
			Scope child = children.computeIfAbsent(name, key -> new Scope(namespace));
			return child.namespace == namespace ? child : null;
		}

		private void append(StringBuilder code) {
			for(String declaration : declarations) {
				code.append(declaration).append('\n');
			}
			for(Map.Entry<String, Scope> entry : children.entrySet()) {
				code.append(entry.getValue().namespace ? "namespace " + entry.getKey() + " {\n" : "struct " + entry.getKey() + " {\n");
				entry.getValue().append(code);
				code.append(entry.getValue().namespace ? "}\n" : "};\n");
			}
		}
	}

	//the included file with the definitions of the macros and the declarations of the function templates that CDT does not know
	private static final String TEMPLATES_FILE = "/__rm_templates__.h";

	//the declaration specifiers that cannot follow the type of a declaration, i.e., virtual in DUCKDB_API virtual void f();
	private static final Pattern DECLARATION_MACRO = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s+(virtual|static|inline|explicit|friend|extern|constexpr|consteval|typedef|mutable|void|bool|char|int|float|double|short|long|signed|unsigned|auto|class|struct|enum|union|template|typename)\\b");

	private static final Set<String> KEYWORDS = Set.of("alignas", "alignof", "asm", "auto", "bool", "break", "case", "catch", "char", "char8_t", "char16_t", "char32_t",
			"class", "concept", "const", "consteval", "constexpr", "constinit", "const_cast", "continue", "co_await", "co_return", "co_yield", "decltype", "default",
			"delete", "do", "double", "dynamic_cast", "else", "enum", "explicit", "export", "extern", "false", "float", "for", "friend", "goto", "if", "inline", "int",
			"long", "mutable", "namespace", "new", "noexcept", "nullptr", "operator", "private", "protected", "public", "register", "reinterpret_cast", "requires",
			"return", "short", "signed", "sizeof", "static", "static_assert", "static_cast", "struct", "switch", "template", "this", "thread_local", "throw", "true",
			"try", "typedef", "typeid", "typename", "union", "unsigned", "using", "virtual", "void", "volatile", "wchar_t", "while");

	//the definitions of the unknown macros at the start of declarations, which are followed by a declaration specifier that cannot follow a type
	private static String unknownDeclarationMacros(IASTTranslationUnit translationUnit, String fileContent) {
		Set<String> expanded = new HashSet<>();
		for(IASTPreprocessorMacroExpansion expansion : translationUnit.getMacroExpansions()) {
			expanded.add(expansion.getMacroReference().toString());
		}
		Set<String> definitions = new LinkedHashSet<>();
		translationUnit.accept(new ASTVisitor() {
			{
				shouldVisitDeclarations = true;
			}

			@Override
			public int visit(IASTDeclaration declaration) {
				IASTFileLocation location = declaration.getFileLocation();
				//a declaration that CDT did not parse, or parsed without its end, i.e., DUCKDB_API virtual T without f(); in DUCKDB_API virtual T f();
				if(location != null && isPartOfFile(declaration) && (declaration instanceof IASTProblemDeclaration || !endsDeclaration(fileContent, location))) {
					Matcher matcher = DECLARATION_MACRO.matcher(fileContent);
					matcher.region(location.getNodeOffset(), fileContent.length());
					if(matcher.lookingAt()) {
						String name = matcher.group(1);
						if(!expanded.contains(name) && !KEYWORDS.contains(name)) {
							definitions.add("#define " + name);
						}
					}
				}
				return PROCESS_CONTINUE;
			}
		});
		return String.join("\n", definitions);
	}

	private static boolean endsDeclaration(String fileContent, IASTFileLocation location) {
		int end = location.getNodeOffset() + location.getNodeLength();
		if(end <= 0 || end > fileContent.length()) {
			return true;
		}
		char last = fileContent.charAt(end - 1);
		return last == ';' || last == '}';
	}

	private static IASTTranslationUnit parse(String filePath, String fileContent, boolean astDiff, String templates) throws CoreException {
		FileContent content = FileContent.create(filePath, fileContent.toCharArray());
		Map<String, String> predefinedMacros = new HashMap<>();
		ScannerInfo scanInfo = templates.isEmpty() ? new ExtendedScannerInfo(predefinedMacros, getIncludePaths(filePath, fileContent)) :
			new ExtendedScannerInfo(predefinedMacros, getIncludePaths(filePath, fileContent), new String[0], new String[] {TEMPLATES_FILE});
		//IncludeFileContentProvider includeProvider = IncludeFileContentProvider.getSavedFilesProvider();
		IncludeFileContentProvider includeProvider = new InternalFileContentProvider() {
			@Override
			public InternalFileContent getContentForInclusion(String path, IMacroDictionary macroDictionary) {
				if (path.equals(TEMPLATES_FILE)) {
					return (InternalFileContent) FileContent.create(TEMPLATES_FILE, templates.toCharArray());
				}
				File file = new File(path);
				if (file.exists()) {
					return (InternalFileContent) FileContent.createForExternalFileLocation(path);
				}
				return null;
			}

			@Override
			public InternalFileContent getContentForInclusion(IIndexFileLocation location, String astPath) {
				return (InternalFileContent) FileContent.createForExternalFileLocation(astPath);
			}
		};
		int options = GPPLanguage.OPTION_IS_SOURCE_UNIT | GPPLanguage.OPTION_PARSE_INACTIVE_CODE;
		if(!astDiff) {
			options |= GPPLanguage.OPTION_NO_IMAGE_LOCATIONS;
		}
		return GPPLanguage.getDefault().getASTTranslationUnit(
				content,
				scanInfo,
				includeProvider,
				EmptyCIndex.INSTANCE,
				options,
				new DefaultLogService()
				);
	}

	//the declarations of the unknown functions called with template arguments, which CDT parsed as comparisons, i.e., name < T > (arguments)
	private static String misparsedFunctionTemplates(IASTTranslationUnit translationUnit, String fileContent) {
		Set<String> declarations = new LinkedHashSet<>();
		//the names used as types in the file, which are computed once per name
		Map<String, Boolean> types = new HashMap<>();
		translationUnit.accept(new ASTVisitor() {
			{
				shouldVisitExpressions = true;
			}

			@Override
			public int visit(IASTExpression expression) {
				if(expression instanceof IASTBinaryExpression binary && binary.getOperator() == IASTBinaryExpression.op_lessThan
						&& binary.getOperand1() instanceof IASTIdExpression id && id.isPartOfTranslationUnitFile()) {
					String declaration = functionTemplateDeclaration(id, fileContent, types);
					if(declaration != null) {
						declarations.add(declaration);
						//the names are not looked up in the bodies of the member functions of the classes that CDT does not know, i.e., void A::f() {...}
						String owner = ownerClassDeclaration(expression, fileContent, types);
						if(owner != null) {
							declarations.add(owner);
						}
					}
				}
				return PROCESS_CONTINUE;
			}
		});
		return String.join("\n", declarations);
	}

	//the declaration of the function template, when the name is unknown and followed by template arguments and the arguments of a call in the file
	private static String functionTemplateDeclaration(IASTIdExpression id, String fileContent, Map<String, Boolean> types) {
		IASTFileLocation location = id.getFileLocation();
		if(location == null) {
			return null;
		}
		int start = location.getNodeOffset();
		int end = start + location.getNodeLength();
		IASTName name = id.getName();
		//not the expansion of a macro
		if(end > fileContent.length() || !fileContent.substring(start, end).replaceAll("\\s+", "").equals(name.toString())) {
			return null;
		}
		IASTName lastName = name.getLastName();
		if(lastName instanceof ICPPASTTemplateId || !(lastName.resolveBinding() instanceof IProblemBinding)) {
			return null;
		}
		List<String> namespaces = new ArrayList<>();
		if(name instanceof ICPPASTQualifiedName qualifiedName) {
			for(ICPPASTNameSpecifier qualifier : qualifiedName.getQualifier()) {
				//the qualifiers are namespaces that are unknown or known as namespaces
				if(!(qualifier instanceof IASTName qualifierName) || qualifierName instanceof ICPPASTTemplateId) {
					return null;
				}
				if(!isNamespace(qualifierName, fileContent, types)) {
					return null;
				}
				namespaces.add(qualifierName.toString());
			}
		}
		int close = closingAngleBracket(fileContent, end);
		if(close < 0 || nextNonWhitespace(fileContent, close + 1) != '(' || types.computeIfAbsent("<>" + lastName, key -> usedAsTemplateType(lastName.toString(), fileContent))) {
			return null;
		}
		return inNamespaces(namespaces, "template<class... __rm_t__> int " + lastName + "(...);");
	}

	//the definition of the unknown class of the member function that contains the expression, i.e., A in void A::f() {...}, in the namespaces of the member function
	private static String ownerClassDeclaration(IASTExpression expression, String fileContent, Map<String, Boolean> types) {
		IASTNode parent = expression.getParent();
		while(parent != null && !(parent instanceof IASTFunctionDefinition)) {
			parent = parent.getParent();
		}
		if(!(parent instanceof IASTFunctionDefinition functionDefinition) || !(functionDefinition.getDeclarator().getName() instanceof ICPPASTQualifiedName qualifiedName)
				|| qualifiedName.isFullyQualified()) {
			return null;
		}
		ICPPASTNameSpecifier[] qualifiers = qualifiedName.getQualifier();
		if(qualifiers.length == 0 || !(qualifiers[qualifiers.length - 1] instanceof IASTName owner) || owner instanceof ICPPASTTemplateId
				|| !(owner.resolveBinding() instanceof IProblemBinding)) {
			return null;
		}
		List<String> namespaces = new ArrayList<>();
		for(IASTNode ancestor = functionDefinition.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
			if(ancestor instanceof ICPPASTNamespaceDefinition namespaceDefinition) {
				namespaces.add(0, namespaceDefinition.getName().toString());
			}
			else if(!(ancestor instanceof IASTTranslationUnit) && !(ancestor instanceof ICPPASTLinkageSpecification)) {
				return null;
			}
		}
		for(int i = 0; i < qualifiers.length - 1; i++) {
			if(!(qualifiers[i] instanceof IASTName qualifier) || qualifier instanceof ICPPASTTemplateId) {
				return null;
			}
			if(!isNamespace(qualifier, fileContent, types)) {
				return null;
			}
			namespaces.add(qualifier.toString());
		}
		return inNamespaces(namespaces, "struct " + owner + " {};");
	}

	private static String inNamespaces(List<String> namespaces, String declaration) {
		StringBuilder code = new StringBuilder();
		for(String namespace : namespaces) {
			code.append("namespace ").append(namespace).append(" { ");
		}
		code.append(declaration);
		code.append(" }".repeat(namespaces.size()));
		return code.toString();
	}

	//whether the qualifier is a known namespace, or an unknown name that is not used as a type in the file, i.e., std in std::make_unique, but not Literal in Literal::f<T>(x),
	//when the file contains Literal x, Literal*, <Literal> or new Literal
	private static boolean isNamespace(IASTName qualifier, String fileContent, Map<String, Boolean> types) {
		IBinding binding = qualifier.resolveBinding();
		if(binding instanceof ICPPNamespace) {
			return true;
		}
		return binding instanceof IProblemBinding && !types.computeIfAbsent(qualifier.toString(), key -> usedAsType(qualifier, fileContent));
	}

	//whether the name is used as a type in the file, outside the comments
	private static boolean usedAsType(IASTName qualifier, String fileContent) {
		String name = Pattern.quote(qualifier.toString());
		Matcher matcher = Pattern.compile("(?<![\\w:.>])" + name + "(?=\\s*[*&>,)]|\\s+[A-Za-z_])|\\bnew\\s+" + name + "\\b").matcher(fileContent);
		IASTComment[] comments = qualifier.getTranslationUnit().getComments();
		while(matcher.find()) {
			if(!inComment(matcher.start(), comments)) {
				return true;
			}
		}
		return false;
	}

	private static boolean inComment(int offset, IASTComment[] comments) {
		for(IASTComment comment : comments) {
			IASTFileLocation location = comment.getFileLocation();
			if(location != null && isPartOfFile(comment) && offset >= location.getNodeOffset() && offset < location.getNodeOffset() + location.getNodeLength()) {
				return true;
			}
		}
		return false;
	}

	//whether the name is followed by template arguments that are not followed by the arguments of a call in the file, i.e., SP in std::vector<SP<T>>,
	//so it is a class template, which is called like a function in SP<T>(x)
	private static boolean usedAsTemplateType(String name, String fileContent) {
		for(int i = fileContent.indexOf(name); i >= 0; i = fileContent.indexOf(name, i + 1)) {
			int end = i + name.length();
			if(i > 0 && Character.isJavaIdentifierPart(fileContent.charAt(i - 1)) || end < fileContent.length() && Character.isJavaIdentifierPart(fileContent.charAt(end))) {
				continue;
			}
			int close = closingAngleBracket(fileContent, end);
			if(close >= 0 && nextNonWhitespace(fileContent, close + 1) != '(') {
				return true;
			}
		}
		return false;
	}

	//the offset of the angle bracket that closes the angle bracket after the offset, or -1
	private static int closingAngleBracket(String fileContent, int offset) {
		int i = offset;
		while(i < fileContent.length() && Character.isWhitespace(fileContent.charAt(i))) {
			i++;
		}
		if(i >= fileContent.length() || fileContent.charAt(i) != '<') {
			return -1;
		}
		int depth = 0;
		int nesting = 0;
		for(; i < fileContent.length(); i++) {
			char c = fileContent.charAt(i);
			switch(c) {
				case '<' -> {
					if(nesting == 0) {
						depth++;
					}
				}
				case '>' -> {
					if(nesting == 0 && --depth == 0) {
						return i;
					}
				}
				case '(', '[' -> nesting++;
				case ')', ']' -> {
					if(--nesting < 0) {
						return -1;
					}
				}
				case ';', '{', '}' -> {
					return -1;
				}
				default -> {
				}
			}
		}
		return -1;
	}

	private static char nextNonWhitespace(String fileContent, int offset) {
		for(int i = offset; i < fileContent.length(); i++) {
			if(!Character.isWhitespace(fileContent.charAt(i))) {
				return fileContent.charAt(i);
			}
		}
		return 0;
	}

	//the number of syntax problems in the file
	private static int problems(IASTTranslationUnit translationUnit) {
		int[] problems = {0};
		translationUnit.accept(new ASTVisitor() {
			{
				shouldVisitProblems = true;
			}

			@Override
			public int visit(IASTProblem problem) {
				if(isPartOfFile(problem)) {
					problems[0]++;
				}
				return PROCESS_CONTINUE;
			}
		});
		return problems[0];
	}

	private static String[] getIncludePaths(String filePath, String fileContent) {
		Set<String> includePaths = new LinkedHashSet<>();
		includePaths.add(".");
		if(fileContent.contains("#include \"gtest/") || fileContent.contains("#include \"gmock/") ||
			fileContent.contains("#include <gtest/") || fileContent.contains("#include <gmock/")) {
			includePaths.add(System.getProperty("user.dir") + "/include");
		}
		includePaths.add("src/include");
		includePaths.add("src");

		Path parent = Paths.get(filePath).normalize().getParent();
		if(parent != null) {
			includePaths.add(parent.toString());
			includePaths.add(parent.resolve("include").toString());
		}
		return includePaths.toArray(new String[0]);
	}

	public Visibility processDeclaration(String packageName, String sourceFolder, UMLAbstractClass parentContainer,
			List<UMLComment> comments, Visibility currentVisibility, IASTDeclaration declaration, ICPPASTTemplateParameter[] templateParameters,
			List<IASTDeclaration> inactiveContainerAlternatives) {
		if(!shouldProcessDeclaration(declaration)) {
			return currentVisibility;
		}
		if(declaration instanceof CPPASTStructuredBindingDeclaration cppStructuredBindingDeclaration) {
			//A structured binding declaration is a feature introduced in C++17 that allows you to unpack or decompose a target object into individual named variables.
			//Similar to destructuring or unpacking in languages like JavaScript and Python, it directly binds specified identifiers to the sub-objects, members, or elements of an initializer.
			for(IASTDeclarator declarator : cppStructuredBindingDeclaration.getDeclarators()) {
				processAttribute(packageName, sourceFolder, parentContainer, currentVisibility, comments, cppStructuredBindingDeclaration.getDeclSpecifier(), declarator, templateParameters);
			}
			UMLType type = UMLType.extractTypeObject(sourceFolder, filePath, fileContent, cppStructuredBindingDeclaration.getDeclSpecifier(), null, 0);
			for(IASTName name : cppStructuredBindingDeclaration.getNames()) {
				LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, name, CodeElementType.FIELD_DECLARATION, fileContent);
				UMLAttribute umlAttribute = new UMLAttribute(name.toString(), type, locationInfo, packageName);
				umlAttribute.setVisibility(currentVisibility != null ? currentVisibility : Visibility.PUBLIC);
				VariableDeclaration variableDeclaration = new VariableDeclaration(sourceFolder, filePath, name, cppStructuredBindingDeclaration.getDeclSpecifier(), umlAttribute, new LinkedHashMap<>(), fileContent);
				variableDeclaration.setAttribute(true);
				umlAttribute.setVariableDeclaration(variableDeclaration);
				addTemplateParameters(umlAttribute, templateParameters, sourceFolder);
				preprocessor.addAttribute(parentContainer, umlAttribute, name);
				distributeComments(comments, locationInfo, umlAttribute.getComments());
			}
		}
		else if(declaration instanceof CPPASTSimpleDeclaration cppSimpleDeclaration) {
			processSimpleDeclaration(cppSimpleDeclaration, packageName, sourceFolder, parentContainer, currentVisibility, comments,
					templateParameters, inactiveContainerAlternatives);
		}
		else if(declaration instanceof CASTSimpleDeclaration cSimpleDeclaration) {
			processSimpleDeclaration(cSimpleDeclaration, packageName, sourceFolder, parentContainer, currentVisibility, comments,
					templateParameters, Collections.emptyList());
		}
		else if(declaration instanceof CPPASTAmbiguousSimpleDeclaration cppAmbiguousSimpleDeclaration) {
			
		}
		else if(declaration instanceof CASTAmbiguousSimpleDeclaration cAmbiguousSimpleDeclaration) {
			
		}
		else if(declaration instanceof CASTFunctionDefinition cFunctionDefinition) {
			UMLOperation operation = processFunctionDefinition(cFunctionDefinition, packageName, sourceFolder, parentContainer, currentVisibility, comments, templateParameters);
			preprocessor.addOperation(parentContainer, operation, cFunctionDefinition, templateParameters);
		}
		else if(declaration instanceof CPPASTFunctionDefinition cppFunctionDefinition) {
			//org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionWithTryBlock is a subclass
			//Function-Try-Block should be handled similar to Kotlin, which allows functions to have a try-expression as a body
			UMLOperation operation = processFunctionDefinition(cppFunctionDefinition, packageName, sourceFolder, parentContainer, currentVisibility, comments, templateParameters);
			preprocessor.addOperation(parentContainer, operation, cppFunctionDefinition, templateParameters);
		}
		else if(declaration instanceof CPPASTAliasDeclaration cppAliasDeclaration) {
			//A C++ alias declaration (introduced in C++11) uses the using keyword to create a readable, interchangeable synonym for an existing type or template. It is the modern, preferred alternative to typedef.
			processCppAliasDeclaration(cppAliasDeclaration, sourceFolder, parentContainer, templateParameters);
		}
		else if(declaration instanceof CASTASMDeclaration cASMDeclaration) {
			//An asm statement (or asm declaration) in C and C++ allows developers to embed raw assembly language source code directly into a high-level program.
		}
		else if(declaration instanceof CPPASTASMDeclaration cppASMDeclaration) {
			//An asm statement (or asm declaration) in C and C++ allows developers to embed raw assembly language source code directly into a high-level program.
		}
		else if(declaration instanceof CASTProblemDeclaration cProblemDeclaration) {
			
		}
		else if(declaration instanceof CPPASTProblemDeclaration cppProblemDeclaration) {
			//The CPPASTProblemDeclaration is an internal class in Eclipse CDT that represents a syntax error or unresolved code construct within the C/C++ Abstract Syntax Tree (AST).
			//When the CDT parser encounters unrecognized code, it captures the issue inside this declaration so parsing can continue.
			LocationInfo location = new LocationInfo(sourceFolder, filePath, cppProblemDeclaration, CodeElementType.PROBLEM_DECLARATION, fileContent);
			String raw = cppProblemDeclaration.getRawSignature();
			boolean isFunctionDeclarator = false;
			boolean isClassDeclaration = false;
			boolean isFunctionDefinition = raw.contains("{") && !raw.contains("{.") && !raw.contains("{}");
			String signature = isFunctionDefinition ? raw.substring(0, raw.indexOf("{")) : raw.lines().findFirst().orElse(raw);
			if(signature.equals(")")) {
				String targetLine = fileContent.lines().skip(location.getStartLine()-1).findFirst().orElse(")");
				signature = targetLine.strip();
				isFunctionDeclarator = true;
			}
			else if(signature.contains("class ")) {
				isClassDeclaration = true;
			}
			UMLProblemDeclaration problemDeclaration = new UMLProblemDeclaration(packageName, sourceFolder, location, signature, raw, isFunctionDefinition, isFunctionDeclarator, isClassDeclaration);
			distributeComments(comments, location, problemDeclaration.getComments());
			if(parentContainer instanceof UMLClass)
				((UMLClass)parentContainer).addProblemDeclaration(problemDeclaration);
		}
		else if(declaration instanceof CPPASTExplicitTemplateInstantiation cppTemplateInstantiation) {
			//templates are typically defined in separate header files (.hpp) and are instantiated as a 'template class'
			//Explicit template instantiation forces the C++ compiler to generate code for a template with specific arguments. This allows you to split template definitions into separate .h and .cpp files.
			IASTDeclaration nestedDeclaration = cppTemplateInstantiation.getDeclaration();
			processDeclaration(packageName, sourceFolder, parentContainer, comments, currentVisibility, nestedDeclaration,
					new ICPPASTTemplateParameter[0], Collections.emptyList());
		}
		else if(declaration instanceof CPPASTTemplateDeclaration cppTemplateDeclaration) {
			IASTDeclaration nestedDeclaration = cppTemplateDeclaration.getDeclaration();
			processDeclaration(packageName, sourceFolder, parentContainer, comments, currentVisibility, nestedDeclaration,
					cppTemplateDeclaration.getTemplateParameters(), preprocessor.unwrapAlternatives(inactiveContainerAlternatives));
		}
		else if(declaration instanceof CPPASTTemplateSpecialization cppTemplateSpecialization) {
			//Template specialization allows you to override the generic behavior of a C++ template and define a custom implementation for specific data types or conditions.
			//While primary templates provide a blueprint for all types, specialization handles unique edge cases—such as treating const char* or bool differently for performance or behavioral optimizations.
			//C++ supports two types of template specialization: Explicit (Full) Specialization and Partial Specialization.
			IASTDeclaration nestedDeclaration = cppTemplateSpecialization.getDeclaration();
			processDeclaration(packageName, sourceFolder, parentContainer, comments, currentVisibility, nestedDeclaration,
					cppTemplateSpecialization.getTemplateParameters(), preprocessor.unwrapAlternatives(inactiveContainerAlternatives));
		}
		else if(declaration instanceof CPPASTInitCapture cppInitCapture) {
			//Init capture (also called generalized lambda capture) was introduced in C++14 to let you declare and initialize new variables directly inside a lambda's capture brackets [...]
		}
		else if(declaration instanceof CPPASTLinkageSpecification cppLinkageSpecification) {
			//C++ linkage specifications (extern "C") direct the compiler to use specific linkage and calling conventions for different programming languages. By preventing C++ name mangling, it allows seamless calls between C++ and C code.
			//C++ supports features like function overloading, which requires the compiler to "mangle" (decorate) function names with argument types so the linker can tell them apart.
			//A C compiler doesn't do this, meaning C++ object code cannot normally find a C library's function. A linkage specification disables C++ mangling for that block of code, ensuring the exact function name is emitted for the linker.
			preprocessor.processDeclarationGroups(packageName, sourceFolder, parentContainer, declaration, comments,
					templateParameters, inactiveContainerAlternatives);
		}
		else if(declaration instanceof CPPASTNamespaceAlias cppNamespaceAlias) {
			//In C++, a namespace alias allows you to create a shorter or alternative name for a long or deeply nested namespace. You define it using the syntax namespace alias_name = existing_namespace;
			processCppImport(cppNamespaceAlias.getMappingName(), cppNamespaceAlias, sourceFolder, parentContainer, false);
		}
		else if(declaration instanceof CPPASTNamespaceDefinition cppNamespaceDefinition) {
			//In C++, a namespace is a declarative region that provides a distinct scope to identifiers (such as names of types, functions, variables, and classes) to prevent naming collisions and organize code into logical groups.
			IASTName name = cppNamespaceDefinition.getName();
			String namespace = name.getRawSignature();
			String qualifiedNamespace = packageName + "." + namespace;
			if(parentContainer instanceof UMLClass umlClass) {
				LocationInfo namespaceLocation = new LocationInfo(sourceFolder, filePath, cppNamespaceDefinition, CodeElementType.PACKAGE_DECLARATION, fileContent);
				for(UMLPreprocessorStatement statement : umlClass.getPreprocessorStatements()) {
					if(namespaceLocation.subsumes(statement.getLocationInfo())) {
						statement.addNamespace(namespace);
					}
				}
			}
			preprocessor.processDeclarationGroups(qualifiedNamespace, sourceFolder, parentContainer, declaration, comments,
					templateParameters, inactiveContainerAlternatives);
		}
		else if(declaration instanceof CPPASTStaticAssertionDeclaration cppStaticAssertionDeclaration) {
			//In C++, a static_assert declaration tests a software condition at compile time. If the condition evaluates to false, the compiler stops and issues a compilation error.
			//Because it is evaluated entirely during compilation, it incurs zero runtime performance or size cost
			LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, cppStaticAssertionDeclaration, CodeElementType.STATIC_ASSERTION_DECLARATION, fileContent);
			UMLStaticAssertionDeclaration assertionDeclaration = new UMLStaticAssertionDeclaration(locationInfo, cppStaticAssertionDeclaration.getRawSignature());
			if(parentContainer instanceof UMLClass)
				preprocessor.addStaticAssertionDeclaration((UMLClass)parentContainer, assertionDeclaration, cppStaticAssertionDeclaration);
		}
		else if(declaration instanceof CPPASTUsingDeclaration cppUsingDeclaration) {
			//A using-declaration in C++ introduces a specific member from another namespace or a base class into the current scope. It allows you to use that specific name without explicitly typing its fully qualified path or prefix every time.
			if(parentContainer instanceof UMLClass) {
				processCppImport(cppUsingDeclaration.getName(), cppUsingDeclaration, sourceFolder, parentContainer, false);
			}
		}
		else if(declaration instanceof CPPASTUsingDirective cppUsingDirective) {
			//In C++, a using directive allows all identifiers within a specific namespace to be used without explicit qualification. It uses the syntax using namespace namespace_name;
			processCppImport(cppUsingDirective.getQualifiedName(), cppUsingDirective, sourceFolder, parentContainer, true);
		}
		else if(declaration instanceof CPPASTVisibilityLabel cppVisibilityLabel) {
			//In C++, visibility labels (more commonly referred to as access specifiers) are keywords used inside a class or struct to control the accessibility of its data members and functions from external code.
			//There are exactly three visibility labels in C++: public, private, and protected
			//All data members/functions that follow should have this access modifier
			int visibility = cppVisibilityLabel.getVisibility();
			if(visibility == ICPPASTVisibilityLabel.v_private) {
				currentVisibility = Visibility.PRIVATE;
			}
			else if(visibility == ICPPASTVisibilityLabel.v_public) {
				currentVisibility = Visibility.PUBLIC;
			}
			else if(visibility == ICPPASTVisibilityLabel.v_protected) {
				currentVisibility = Visibility.PROTECTED;
			}
		}
		return currentVisibility;
	}

	boolean shouldProcessDeclaration(IASTDeclaration declaration) {
		return isPartOfFile(declaration) || declaration instanceof ICPPASTVisibilityLabel;
	}

	//whether the binding is declared only in the included file with the declarations that CDT does not know
	public static boolean declaredOnlyBeforeFile(IASTTranslationUnit translationUnit, IBinding binding) {
		if(translationUnit == null || binding == null) {
			return false;
		}
		IASTName[] declarations = translationUnit.getDeclarationsInAST(binding);
		if(declarations.length == 0) {
			return false;
		}
		for(IASTName declaration : declarations) {
			if(!TEMPLATES_FILE.equals(declaration.getContainingFilename())) {
				return false;
			}
		}
		return true;
	}

	//whether the node is in the parsed file, and not in the included file with the declarations of the function templates, which CDT considers part of the file
	public static boolean isPartOfFile(IASTNode node) {
		return node.isPartOfTranslationUnitFile() && !TEMPLATES_FILE.equals(node.getContainingFilename());
	}

	private UMLClass createModuleClass(IASTTranslationUnit ast, String sourceFolder) {
		String moduleName = moduleName(filePath);
		LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, ast, CodeElementType.TYPE_DECLARATION, fileContent);
		UMLClass umlClass = new UMLClass("", moduleName, locationInfo, true, new ArrayList<>());
		umlClass.setModule(true);
		umlClass.setStatic(true);
		umlClass.setVisibility(Visibility.PUBLIC);
		umlClass.setActualSignature(moduleName);
		return umlClass;
	}

	private void processSimpleDeclaration(IASTSimpleDeclaration simpleDeclaration, String packageName, String sourceFolder,
			UMLAbstractClass parentContainer, Visibility currentVisibility, List<UMLComment> comments,
			ICPPASTTemplateParameter[] templateParameters, List<IASTDeclaration> inactiveContainerAlternatives) {
		IASTDeclSpecifier declSpecifier = simpleDeclaration.getDeclSpecifier();
		if(declSpecifier instanceof IASTCompositeTypeSpecifier compositeTypeSpecifier) {
			if(compositeTypeSpecifier.getName() == null) {
				return;
			}
			String className = compositeTypeSpecifier.getName().toString();
			//handle unnamed struct and unnamed union
			if(className.isBlank() && !compositeTypeSpecifier.toString().equals("struct") && !compositeTypeSpecifier.toString().equals("union")) {
				return;
			}
			else if(compositeTypeSpecifier.toString().equals("struct") || compositeTypeSpecifier.toString().equals("union")) {
				IASTDeclarator[] declarators = simpleDeclaration.getDeclarators();
				if(declarators.length == 1) {
					className = declarators[0].getName().toString();
				}
			}
			LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, compositeTypeSpecifier, CodeElementType.TYPE_DECLARATION, fileContent);
			UMLClass umlClass = new UMLClass(packageName, className, locationInfo, true, new ArrayList<>());
			umlClass.setVisibility(currentVisibility != null ? currentVisibility : Visibility.PUBLIC);
			if(compositeTypeSpecifier instanceof ICPPASTCompositeTypeSpecifier cppCompositeTypeSpecifier) {
				umlClass.setFinal(cppCompositeTypeSpecifier.isFinal());
				if(cppCompositeTypeSpecifier.getKey() == IASTCompositeTypeSpecifier.k_struct) {
					umlClass.setStruct(true);
				}
				if(cppCompositeTypeSpecifier.getKey() == IASTCompositeTypeSpecifier.k_union) {
					umlClass.setUnion(true);
				}
				ICPPASTBaseSpecifier[] baseSpecifiers = cppCompositeTypeSpecifier.getBaseSpecifiers();
				int index = 0;
				for(ICPPASTBaseSpecifier base : baseSpecifiers) {
					UMLType umlType = UMLType.extractTypeObject(sourceFolder, filePath, fileContent, base, null, 0);
					if(index == 0) {
						UMLGeneralization umlGeneralization = new UMLGeneralization(umlClass, umlType.getClassType());
						umlClass.setSuperclass(umlType);
						if(umlModel != null)
							umlModel.addGeneralization(umlGeneralization);
					}
					else {
						UMLRealization umlRealization = new UMLRealization(umlClass, umlType.getClassType());
						umlClass.addImplementedInterface(umlType);
						if(umlModel != null)
							umlModel.addRealization(umlRealization);
					}
					index++;
				}
			}
			addTemplateParameters(umlClass, templateParameters, sourceFolder);
			String rawSignature = simpleDeclaration.getRawSignature();
			if(rawSignature.contains("{"))
				rawSignature = rawSignature.substring(0, rawSignature.indexOf("{") + 1);
			umlClass.setActualSignature(rawSignature);
			preprocessor.processDeclarationGroups(packageName + "." + className, sourceFolder, umlClass, simpleDeclaration, comments,
					templateParameters, inactiveContainerAlternatives);
			this.umlModel.addClass(umlClass);
			distributeComments(comments, locationInfo, umlClass.getComments());
		}
		else if(declSpecifier instanceof ICPPASTElaboratedTypeSpecifier elaboratedTypeSpecifier) {
			//forward declaration: class, struct, union, enum
			// struct SReadableWaiter;
			// class CWLSurfaceResource;
			//friend class F;
			UMLOperation operation = null;
			IASTName name = elaboratedTypeSpecifier.getName();
			String fullName = name.toString();
			LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, elaboratedTypeSpecifier, CodeElementType.FORWARD_DECLARATION, fileContent);
			String[] tokens = elaboratedTypeSpecifier.toString().split("\s");
			if(simpleDeclaration.getDeclarators().length > 0 && simpleDeclaration.getDeclarators()[0] instanceof IASTFunctionDeclarator functionDeclarator) {
				fullName = fullName + "." + functionDeclarator.getName().toString();
				operation = processFunctionDeclSpecifier(elaboratedTypeSpecifier, functionDeclarator, packageName, sourceFolder, parentContainer, currentVisibility, comments, templateParameters);
			}
			else if(simpleDeclaration.getDeclarators().length > 0 && simpleDeclaration.getDeclarators()[0] instanceof ICPPASTDeclarator declarator && declarator.getName() != null && !declarator.getName().toString().isBlank()) {
				fullName = fullName + "." + declarator.getName().toString();
			}
			String type = tokens.length > 1 ? tokens[0] : "";
			if(tokens[0].equals("const"))
				type = type + " " + tokens[1];
			UMLForwardDeclaration decl = new UMLForwardDeclaration(locationInfo, type, fullName, elaboratedTypeSpecifier.isFriend());
			if(operation != null) {
				decl.setFunction(operation);
			}
			if(parentContainer instanceof UMLClass)
				preprocessor.addForwardDeclaration((UMLClass)parentContainer, decl, elaboratedTypeSpecifier);
		}
		else if(declSpecifier instanceof ICPPASTEnumerationSpecifier enumSpecifier) {
			if(enumSpecifier.getName() == null) {
				return;
			}
			String className = enumSpecifier.getName().toString();
			if(className.isBlank()) {
				return;
			}
			LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, enumSpecifier, CodeElementType.ENUM_DECLARATION, fileContent);
			UMLClass umlClass = new UMLClass(packageName, className, locationInfo, true, new ArrayList<>());
			umlClass.setEnum(true);
			umlClass.setVisibility(currentVisibility != null ? currentVisibility : Visibility.PUBLIC);
			if(enumSpecifier.getBaseType() != null) {
				UMLType umlType = UMLType.extractTypeObject(sourceFolder, filePath, fileContent, enumSpecifier.getBaseType(), null, 0);
				umlClass.setSuperclass(umlType);
			}
			addTemplateParameters(umlClass, templateParameters, sourceFolder);
			String rawSignature = simpleDeclaration.getRawSignature();
			if(rawSignature.contains("{"))
				rawSignature = rawSignature.substring(0, rawSignature.indexOf("{") + 1);
			umlClass.setActualSignature(rawSignature);
			preprocessor.processDeclarationGroups(packageName + "." + className, sourceFolder, umlClass, simpleDeclaration, comments,
					templateParameters, inactiveContainerAlternatives);
			IASTEnumerator[] enumerators = enumSpecifier.getEnumerators();
			//no LocationInfo is attached on purpose, since this type is shared by all enum constants and does not correspond to a single program element
			UMLType type = UMLType.extractTypeObject(className, PathFileUtils.getLang(filePath));
			for(IASTEnumerator enumerator : enumerators) {
				IASTName name = enumerator.getName();
				LocationInfo enumConstantLocation = new LocationInfo(sourceFolder, filePath, name, CodeElementType.ENUM_CONSTANT_DECLARATION, fileContent);
				UMLEnumConstant umlAttribute = new UMLEnumConstant(name.toString(), type, enumConstantLocation, packageName);
				umlAttribute.setVisibility(currentVisibility != null ? currentVisibility : Visibility.PUBLIC);
				VariableDeclaration variableDeclaration = new VariableDeclaration(sourceFolder, filePath, name, enumSpecifier, umlAttribute, new LinkedHashMap<>(), fileContent);
				variableDeclaration.setEnumConstant(true);
				if(enumerator.getValue() != null) {
					AbstractExpression expr = new AbstractExpression(sourceFolder, filePath, enumerator.getValue(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER, umlAttribute, new LinkedHashMap<>(), fileContent);
					variableDeclaration.setInitializer(expr);
				}
				umlAttribute.setVariableDeclaration(variableDeclaration);
				addTemplateParameters(umlAttribute, templateParameters, sourceFolder);
				preprocessor.addEnumConstant(umlClass, umlAttribute, name);
				distributeComments(comments, locationInfo, umlAttribute.getComments());
			}
			this.umlModel.addClass(umlClass);
			distributeComments(comments, locationInfo, umlClass.getComments());
		}
		else if(declSpecifier instanceof IASTSimpleDeclSpecifier simpleDeclSpecifier) {
			for(IASTDeclarator declarator : simpleDeclaration.getDeclarators()) {
				if(!(declarator instanceof IASTFunctionDeclarator)) {
					processAttribute(packageName, sourceFolder, parentContainer, currentVisibility, comments, declSpecifier, declarator, templateParameters);
				}
				else if(declarator instanceof IASTFunctionDeclarator functionDeclarator) {
					UMLOperation operation = processFunctionDeclSpecifier(simpleDeclSpecifier, functionDeclarator, packageName, sourceFolder, parentContainer, currentVisibility, comments, templateParameters);
					preprocessor.addOperation(parentContainer, operation, functionDeclarator, templateParameters);
				}
			}
		}
		else if(declSpecifier instanceof ICPPASTNamedTypeSpecifier namedTypeSpecifier) {
			for(IASTDeclarator declarator : simpleDeclaration.getDeclarators()) {
				if(!(declarator instanceof IASTFunctionDeclarator)) {
					processAttribute(packageName, sourceFolder, parentContainer, currentVisibility, comments, declSpecifier, declarator, templateParameters);
				}
				else if(declarator instanceof IASTFunctionDeclarator functionDeclarator) {
					UMLOperation operation = processFunctionDeclSpecifier(namedTypeSpecifier, functionDeclarator, packageName, sourceFolder, parentContainer, currentVisibility, comments, templateParameters);
					preprocessor.addOperation(parentContainer, operation, functionDeclarator, templateParameters);
				}
			}
		}
	}

	private void processAttribute(String packageName, String sourceFolder, UMLAbstractClass parentContainer,
			Visibility currentVisibility, List<UMLComment> comments, IASTDeclSpecifier declSpecifier, IASTDeclarator declarator, ICPPASTTemplateParameter[] templateParameters) {
		LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, declarator, CodeElementType.FIELD_DECLARATION, fileContent);
		String fieldName = declarator.getName().toString();
		if(fieldName.isEmpty() && declarator.getNestedDeclarator() != null)
			fieldName = declarator.getNestedDeclarator().getRawSignature();
		if(!fieldName.isEmpty()) {
			UMLType type = UMLType.extractTypeObject(sourceFolder, filePath, fileContent, declSpecifier, declarator, 0);
			boolean allUpperCase = type != null ? type.toString().equals(type.toString().toUpperCase()) : false;
			//avoid creating an attribute when type seems like a preprocessor function call, e.g., ABSL_EXCLUSIVE_LOCKS_REQUIRED
			boolean skip = allUpperCase && parentContainer.containsAttributeWithName(fieldName);
			if(!skip) {
				UMLAttribute umlAttribute = new UMLAttribute(fieldName, type, locationInfo, packageName);
				umlAttribute.setVisibility(currentVisibility != null ? currentVisibility : Visibility.PUBLIC);
				VariableDeclaration variableDeclaration = new VariableDeclaration(sourceFolder, filePath, declarator, declSpecifier, umlAttribute, new LinkedHashMap<>(), fileContent);
				variableDeclaration.setAttribute(true);
				umlAttribute.setVariableDeclaration(variableDeclaration);
				addTemplateParameters(umlAttribute, templateParameters, sourceFolder);
				preprocessor.addAttribute(parentContainer, umlAttribute, declarator);
				distributeComments(comments, locationInfo, umlAttribute.getComments());
			}
		}
	}

	private UMLOperation processFunctionDeclSpecifier(IASTDeclSpecifier declSpecifier, IASTFunctionDeclarator declarator, String className, String sourceFolder, UMLAbstractClass parentContainer, Visibility currentVisibility, List<UMLComment> comments, ICPPASTTemplateParameter[] templateParameters) {
		IASTName declaratorName = declarator.getName();
		LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, declarator, CodeElementType.METHOD_DECLARATION, fileContent);
		String functionName = declaratorName.toString();
		if(functionName.isEmpty()) {
			IASTDeclarator nested = declarator.getNestedDeclarator();
			functionName = nested.getName().toString();
		}
		UMLOperation operation = new UMLOperation(functionName, locationInfo, className);
		operation.setVisibility(currentVisibility != null ? currentVisibility : Visibility.PUBLIC);
		operation.setStatic(declSpecifier.getStorageClass() == IASTDeclSpecifier.sc_static);
		operation.setInline(declSpecifier.isInline());
		distributeComments(comments, locationInfo, operation.getComments());
		if(declarator instanceof ICPPASTFunctionDeclarator cppFunctionDeclarator) {
			operation.setConst(cppFunctionDeclarator.isConst());
			operation.setPureVirtual(cppFunctionDeclarator.isPureVirtual());
		}

		UMLType returnType = UMLType.extractTypeObject(sourceFolder, filePath, fileContent, declSpecifier, declarator, 0);
		if(returnType != null) {
			operation.addParameter(new UMLParameter("return", returnType, "return", false));
		}
		else {
			operation.setConstructor(true);
		}

		if(declarator instanceof IASTStandardFunctionDeclarator standardDeclarator) {
			int index = 0;
			for(IASTParameterDeclaration parameter : standardDeclarator.getParameters()) {
				String parameterDeclaratorName = parameter.getDeclarator().getName().toString();
				if(parameterDeclaratorName.isBlank() && UMLType.cleanTypeText(parameter.getDeclSpecifier().getRawSignature()).equals("void") && standardDeclarator.getParameters().length == 1) {
					continue;
				}
				String parameterName = extractParameterName(parameter, index);
				UMLType parameterType = UMLType.extractTypeObject(sourceFolder, filePath, fileContent, parameter.getDeclSpecifier(), parameter.getDeclarator(), 0);
				UMLParameter umlParameter = new UMLParameter(parameterName, parameterType, "in", false);
				VariableDeclaration variableDeclaration = new VariableDeclaration(sourceFolder, filePath, parameter, parameter.getDeclSpecifier(), operation, new LinkedHashMap<>(), fileContent);
				variableDeclaration.setParameter(true);
				umlParameter.setVariableDeclaration(variableDeclaration);
				operation.addParameter(umlParameter);
				index++;
			}
			/*if(standardDeclarator.takesVarArgs()) {
				UMLType varargsType = UMLType.extractTypeObject("Object");
				varargsType.setVarargs();
				operation.addParameter(new UMLParameter("varargs", varargsType, "in", true));
			}*/
		}
		addTemplateParameters(operation, templateParameters, sourceFolder);
		if(declarator instanceof ICPPASTFunctionDeclarator cppFunctionDeclarator) {
			IASTTypeId trailingReturnType = cppFunctionDeclarator.getTrailingReturnType();
			if(trailingReturnType != null) {
				Map<String, Set<VariableDeclaration>> activeVariableDeclarations = new HashMap<String, Set<VariableDeclaration>>();;
				for(VariableDeclaration v : operation.getParameterDeclarationList()) {
					if(activeVariableDeclarations.containsKey(v.getVariableName())) {
						activeVariableDeclarations.get(v.getVariableName()).add(v);
					}
					else {
						Set<VariableDeclaration> set = new HashSet<VariableDeclaration>();
						set.add(v);
						activeVariableDeclarations.put(v.getVariableName(), set);
					}
				}
				AbstractExpression expression = new AbstractExpression(sourceFolder, filePath, trailingReturnType, CodeElementType.TRAILING_RETURN_TYPE, operation, activeVariableDeclarations, fileContent);
				operation.setTrailingReturnType(expression);
			}
		}

		//int start = declSpecifier.getFileLocation().getNodeOffset();
		//int end = declarator.getFileLocation().getNodeOffset() + declarator.getFileLocation().getNodeLength();
		operation.setActualSignature(declarator.getRawSignature());
		return operation;
	}

	private UMLOperation processFunctionDefinition(IASTFunctionDefinition functionDefinition, String className, String sourceFolder, UMLAbstractClass parentContainer, Visibility currentVisibility, List<UMLComment> comments, ICPPASTTemplateParameter[] templateParameters) {
		IASTFunctionDeclarator declarator = functionDefinition.getDeclarator();
		IASTName functionName = declarator.getName();
		String operationName = functionName.toString();
		if(functionName instanceof ICPPASTConversionName conversionName) {
			IASTTypeId typeId = conversionName.getTypeId();
			if(typeId != null) {
				IASTDeclSpecifier specifier = typeId.getDeclSpecifier();
				operationName = specifier.toString();
			}
		}
		LocationInfo locationInfo = withEmptyMacros(new LocationInfo(sourceFolder, filePath, functionDefinition, CodeElementType.METHOD_DECLARATION, fileContent));
		UMLOperation operation = new UMLOperation(operationName, locationInfo, className);
		operation.setVisibility(currentVisibility != null ? currentVisibility : Visibility.PUBLIC);
		operation.setStatic(functionDefinition.getDeclSpecifier().getStorageClass() == IASTDeclSpecifier.sc_static);
		operation.setInline(functionDefinition.getDeclSpecifier().isInline());
		if(functionDefinition.getDeclSpecifier() instanceof ICPPASTDeclSpecifier cppDeclSpecifier) {
			operation.setExplicit(cppDeclSpecifier.isExplicit());
		}
		distributeComments(comments, locationInfo, operation.getComments());
		if(declarator instanceof ICPPASTFunctionDeclarator cppFunctionDeclarator) {
			operation.setConst(cppFunctionDeclarator.isConst());
			operation.setPureVirtual(cppFunctionDeclarator.isPureVirtual());
		}

		UMLType returnType = UMLType.extractTypeObject(sourceFolder, filePath, fileContent, functionDefinition.getDeclSpecifier(), declarator, 0);
		if(returnType != null) {
			operation.addParameter(new UMLParameter("return", returnType, "return", false));
		}
		else {
			operation.setConstructor(true);
		}

		if(declarator instanceof IASTStandardFunctionDeclarator standardDeclarator) {
			int index = 0;
			for(IASTParameterDeclaration parameter : standardDeclarator.getParameters()) {
				String parameterDeclaratorName = parameter.getDeclarator().getName().toString();
				if(parameterDeclaratorName.isBlank() && UMLType.cleanTypeText(parameter.getDeclSpecifier().getRawSignature()).equals("void") && standardDeclarator.getParameters().length == 1) {
					continue;
				}
				String parameterName = extractParameterName(parameter, index);
				UMLType parameterType = UMLType.extractTypeObject(sourceFolder, filePath, fileContent, parameter.getDeclSpecifier(), parameter.getDeclarator(), 0);
				UMLParameter umlParameter = new UMLParameter(parameterName, parameterType, "in", false);
				VariableDeclaration variableDeclaration = new VariableDeclaration(sourceFolder, filePath, parameter, parameter.getDeclSpecifier(), operation, new LinkedHashMap<>(), fileContent);
				variableDeclaration.setParameter(true);
				umlParameter.setVariableDeclaration(variableDeclaration);
				operation.addParameter(umlParameter);
				index++;
			}
			/*if(standardDeclarator.takesVarArgs()) {
				UMLType varargsType = UMLType.extractTypeObject("Object");
				varargsType.setVarargs();
				operation.addParameter(new UMLParameter("varargs", varargsType, "in", true));
			}*/
		}
		addTemplateParameters(operation, templateParameters, sourceFolder);

		String actualSignature = functionDefinition.getRawSignature().contains("{") ? functionDefinition.getRawSignature().substring(0, functionDefinition.getRawSignature().indexOf("{") + 1) : functionDefinition.getRawSignature();
		operation.setActualSignature(actualSignature);
		IASTStatement body = functionDefinition.getBody();
		if(body instanceof IASTCompoundStatement compoundStatement) {
			CppOperationBody operationBody = new CppOperationBody(sourceFolder, filePath, compoundStatement, operation, parentContainer.getAttributes(), fileContent);
			operation.setBody(operationBody);
			if(functionDefinition instanceof ICPPASTFunctionWithTryBlock withTryBlock) {
				ICPPASTCatchHandler[] catchHandlers = withTryBlock.getCatchHandlers();
				for(ICPPASTCatchHandler catchHandler : catchHandlers) {
					operationBody.addCatchHandlerToFunction(sourceFolder, filePath, catchHandler, fileContent);
				}
			}
			for(IASTSimpleDeclaration simpleDeclaration : operationBody.getNestedSimpleDeclarations()) {
				processSimpleDeclaration(simpleDeclaration, className + "." + functionName.toString(), sourceFolder, parentContainer, currentVisibility, comments, templateParameters, Collections.emptyList());
			}
			//clear after processing to avoid keeping AST nodes in memory
			operationBody.getNestedSimpleDeclarations().clear();
			if(declarator instanceof ICPPASTFunctionDeclarator cppFunctionDeclarator) {
				IASTTypeId trailingReturnType = cppFunctionDeclarator.getTrailingReturnType();
				if(trailingReturnType != null) {
					AbstractExpression expression = new AbstractExpression(sourceFolder, filePath, trailingReturnType, CodeElementType.TRAILING_RETURN_TYPE, operation, operationBody.getActiveVariableDeclarations(), fileContent);
					operation.setTrailingReturnType(expression);
				}
			}
		}
		if(functionDefinition instanceof ICPPASTFunctionDefinition cppFunctionDefinition) {
			if(cppFunctionDefinition.isDeleted()) {
				operation.setDeleteClause(true);
			}
			if(cppFunctionDefinition.isDefaulted()) {
				operation.setDefaultClause(true);
			}
			ICPPASTConstructorChainInitializer[] initializers = cppFunctionDefinition.getMemberInitializers();
			for (ICPPASTConstructorChainInitializer initializer : initializers) {
				// The name of the member or base class being initialized
				IASTName memberName = initializer.getMemberInitializerId();
				boolean found = false;
				for(UMLParameter parameter : operation.getParameters()) {
					if(parameter.getName().equals(memberName.toString())) {
						AbstractExpression expression = new AbstractExpression(sourceFolder, filePath, initializer, CodeElementType.VARIABLE_DECLARATION_INITIALIZER, operation, operation.getBody().getActiveVariableDeclarations(), fileContent);
						parameter.getVariableDeclaration().setInitializer(expression);
						found = true;
						break;
					}
				}
				if(!found) {
					UMLAttribute attr = parentContainer.attributeWithName(memberName.toString());
					if(attr != null) {
						AbstractExpression expression = new AbstractExpression(sourceFolder, filePath, initializer, CodeElementType.VARIABLE_DECLARATION_INITIALIZER, operation, operation.getBody().getActiveVariableDeclarations(), fileContent);
						Set<VariableDeclaration> candidates = new LinkedHashSet<>();
						for(UMLParameter parameter : operation.getParameters()) {
							if(expression.getAllVariables().contains(parameter.getName())) {
								candidates.add(parameter.getVariableDeclaration());
							}
						}
						if(candidates.size() == 1) {
							candidates.iterator().next().setInitializer(expression);
						}
					}
					else {
						//this is a super constructor call
						AbstractExpression callEntry = new AbstractExpression(sourceFolder, filePath, initializer, CodeElementType.SUPER_TYPE_CALL_ENTRY, operation, operation.getBody().getActiveVariableDeclarations(), fileContent);
						if(parentContainer instanceof UMLClass)
							((UMLClass)parentContainer).addSuperTypeCallEntry(callEntry);
					}
				}
			}
		}
		return operation;
	}

	private void processCppImport(IASTName name, IASTDeclaration declaration, String sourceFolder, UMLAbstractClass parentContainer, boolean onDemand) {
		if(name == null || name.toString().isBlank()) {
			return;
		}
		String importName = name.toString().replace("::", ".");
		LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, declaration, CodeElementType.IMPORT_DECLARATION, fileContent);
		preprocessor.addImport(parentContainer, new UMLImport(importName, onDemand, false, locationInfo), declaration);
	}

	private void processCppAliasDeclaration(CPPASTAliasDeclaration aliasDeclaration, String sourceFolder, UMLAbstractClass parentContainer, ICPPASTTemplateParameter[] templateParameters) {
		if(!(parentContainer instanceof UMLClass umlClass)) {
			return;
		}
		UMLTypeAlias umlTypeAlias = createCppTypeAlias(aliasDeclaration, sourceFolder);
		if(umlTypeAlias == null) {
			return;
		}
		addTemplateParameters(umlTypeAlias, templateParameters, sourceFolder);
		preprocessor.addTypeAlias(umlClass, umlTypeAlias, aliasDeclaration);
	}

	private UMLTypeAlias createCppTypeAlias(CPPASTAliasDeclaration aliasDeclaration, String sourceFolder) {
		IASTName alias = aliasDeclaration.getAlias();
		IASTTypeId mappingTypeId = aliasDeclaration.getMappingTypeId();
		if(alias == null || alias.toString().isBlank() || mappingTypeId == null) {
			return null;
		}
		LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, aliasDeclaration, CodeElementType.TYPE_ALIAS, fileContent);
		UMLType rightType = null;
		if(mappingTypeId.getDeclSpecifier() != null) {
			rightType = UMLType.extractTypeObject(sourceFolder, filePath, fileContent, mappingTypeId.getDeclSpecifier(), mappingTypeId.getAbstractDeclarator(), 0);
		}
		//if structured extraction can’t model that perfectly, the fallback stores something based on the raw text
		if(rightType == null) {
			LocationInfo typeLocationInfo = new LocationInfo(sourceFolder, filePath, mappingTypeId, CodeElementType.TYPE, fileContent);
			rightType = UMLType.extractTypeObject(UMLType.cleanTypeText(mappingTypeId.getRawSignature()), "<", ">", typeLocationInfo, PathFileUtils.getLang(filePath));
		}
		return new UMLTypeAlias(alias.toString(), rightType, locationInfo);
	}

	private void addTemplateParameters(TypeParameterProvider provider, ICPPASTTemplateParameter[] templateParameters, String sourceFolder) {
		if(templateParameters == null || templateParameters.length == 0) {
			return;
		}
		for(ICPPASTTemplateParameter parameter : templateParameters) {
			UMLTypeParameter umlTypeParameter = createTemplateParameter(parameter, sourceFolder);
			if(umlTypeParameter != null) {
				provider.addTypeParameter(umlTypeParameter);
			}
		}
	}

	// Skip unnamed template parameters, such as template <int> class Buffer {};
	private UMLTypeParameter createTemplateParameter(ICPPASTTemplateParameter parameter, String sourceFolder) {
		String name = null;
		// A normal type parameter: typename/class T
		if(parameter instanceof ICPPASTSimpleTypeTemplateParameter typeParameter && typeParameter.getName() != null) {
			name = typeParameter.getName().toString();
		}
		// A non-type template parameter: int N
		else if(parameter instanceof ICPPASTParameterDeclaration nonTypeParameter && nonTypeParameter.getDeclarator() != null && nonTypeParameter.getDeclarator().getName() != null) {
			name = nonTypeParameter.getDeclarator().getName().toString();
		}
		// A template-template parameter: template <typename> class Container
		else if(parameter instanceof ICPPASTTemplatedTypeTemplateParameter templateTemplateParameter && templateTemplateParameter.getName() != null) {
			name = templateTemplateParameter.getName().toString();
		}
		// Unnamed parameters have no UMLTypeParameter name to store.
		if(name == null || name.isBlank()) {
			return null;
		}
		LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, parameter, CodeElementType.TYPE_PARAMETER, fileContent);
		return new UMLTypeParameter(name, locationInfo);
	}

	public static String extractParameterName(IASTParameterDeclaration parameter, int index) {
		IASTDeclarator declarator = parameter.getDeclarator();
		if(declarator != null && declarator.getName() != null) {
			String name = declarator.getName().toString();
			if(!name.isBlank()) {
				return name;
			}
		}
		return "arg" + index;
	}

	private String moduleName(String path) {
		String fileName = Paths.get(path).getFileName().toString();
		int dot = fileName.lastIndexOf('.');
		if(dot > 0) {
			fileName = fileName.substring(0, dot);
		}
		return fileName;
	}

	private String extractCppSourceFolder() {
		return UMLAdapterUtil.extractSourceFolder(filePath,
				Set.of("src", "source", "lib", "include", "inc", "test", "tests", "unittest", "unittests"));
	}
}
