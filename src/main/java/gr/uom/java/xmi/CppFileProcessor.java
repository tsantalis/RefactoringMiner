package gr.uom.java.xmi;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
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
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionWithTryBlock;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
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
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateId;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplatedTypeTemplateParameter;
import org.eclipse.cdt.internal.core.index.EmptyCIndex;
import org.eclipse.core.runtime.CoreException;
import org.refactoringminer.util.PathFileUtils;

import com.github.gumtreediff.gen.treesitterng.CTreeSitterNgTreeGenerator;
import com.github.gumtreediff.gen.treesitterng.CppTreeSitterNgTreeGenerator;
import com.github.gumtreediff.tree.TreeContext;

import extension.umladapter.UMLAdapterUtil;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLPreprocessorStatement.Directive;
import gr.uom.java.xmi.decomposition.CppOperationBody;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class CppFileProcessor {
	private String filePath;
	private String fileContent;
	private UMLModel umlModel;

	public CppFileProcessor(UMLModel umlModel) {
		this.umlModel = umlModel;
	}

	public void processCppFile(String filePath, String fileContent, boolean astDiff) {
		this.filePath = filePath;
		this.fileContent = fileContent;
		try {
			FileContent content = FileContent.create(filePath, fileContent.toCharArray());
			Map<String, String> predefinedMacros = new HashMap<>();
			ScannerInfo scanInfo = new ScannerInfo(predefinedMacros, getIncludePaths(filePath));
			IncludeFileContentProvider includeFiles = IncludeFileContentProvider.getSavedFilesProvider();

			if(PathFileUtils.isCppFile(filePath)) {
				if (astDiff) {
					ByteArrayInputStream is = new ByteArrayInputStream(fileContent.getBytes());
					try {
						TreeContext treeContext = new CppTreeSitterNgTreeGenerator().generateFrom().stream(is);
						this.umlModel.getTreeContextMap().put(filePath, treeContext);
					}
					catch(Exception e) {}
				}
				IASTTranslationUnit ast = GPPLanguage.getDefault().getASTTranslationUnit(
						content,
						scanInfo,
						includeFiles,
						EmptyCIndex.INSTANCE,
						GPPLanguage.OPTION_IS_SOURCE_UNIT,
						new DefaultLogService()
						);
				String sourceFolder = extractCppSourceFolder();
				List<UMLComment> comments = extractInternalComments(ast.getComments(), sourceFolder, filePath, fileContent);
				this.umlModel.getCommentMap().put(filePath, comments);
				UMLClass moduleClass = createModuleClass(ast, sourceFolder);
				processPreprocessorStatements(sourceFolder, moduleClass, ast.getAllPreprocessorStatements());
				processDeclarations(moduleClass.getName(), sourceFolder, moduleClass, ast.getDeclarations(), comments);
				this.umlModel.addClass(moduleClass);
				//add remaining comments to moduleClass
				//TODO consider assigning comments to individual preprocessor statements
				moduleClass.getComments().addAll(comments);
			}
			else if(PathFileUtils.isCFile(filePath)) {
				if (astDiff) {
					ByteArrayInputStream is = new ByteArrayInputStream(fileContent.getBytes());
					try {
						TreeContext treeContext = new CTreeSitterNgTreeGenerator().generateFrom().stream(is);
						this.umlModel.getTreeContextMap().put(filePath, treeContext);
					}
					catch(Exception e) {}
				}
				IASTTranslationUnit ast = GCCLanguage.getDefault().getASTTranslationUnit(
						content,
						scanInfo,
						includeFiles,
						EmptyCIndex.INSTANCE,
						GCCLanguage.OPTION_IS_SOURCE_UNIT,
						new DefaultLogService()
						);
				String sourceFolder = extractCppSourceFolder();
				List<UMLComment> comments = extractInternalComments(ast.getComments(), sourceFolder, filePath, fileContent);
				this.umlModel.getCommentMap().put(filePath, comments);
				UMLClass moduleClass = createModuleClass(ast, sourceFolder);
				processPreprocessorStatements(sourceFolder, moduleClass, ast.getAllPreprocessorStatements());
				processDeclarations(moduleClass.getName(), sourceFolder, moduleClass, ast.getDeclarations(), comments);
				this.umlModel.addClass(moduleClass);
				//add remaining comments to moduleClass
				moduleClass.getComments().addAll(comments);
			}
		}
		catch(CoreException e) {

		}
	}

	private List<UMLComment> extractInternalComments(IASTComment[] astComments, String sourceFolder, String sourceFile, String fileContent) {
		List<UMLComment> comments = new ArrayList<UMLComment>();
		for(IASTComment comment : astComments) {
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

	private String[] getIncludePaths(String filePath) {
		Set<String> includePaths = new LinkedHashSet<>();
		includePaths.add(".");
		includePaths.add("include");
		includePaths.add("src/include");
		includePaths.add("src");

		Path parent = Paths.get(filePath).normalize().getParent();
		if(parent != null) {
			includePaths.add(parent.toString());
			includePaths.add(parent.resolve("include").toString());
		}
		return includePaths.toArray(new String[0]);
	}

	private void processDeclarations(String packageName, String sourceFolder, UMLAbstractClass parentContainer, IASTDeclaration[] declarations, List<UMLComment> comments) {
		Visibility currentVisibility = null;
		for(IASTDeclaration declaration : declarations) {
			if(declaration instanceof CPPASTSimpleDeclaration cppSimpleDeclaration) {
				processSimpleDeclaration(cppSimpleDeclaration, packageName, sourceFolder, parentContainer, currentVisibility, comments);
			}
			else if(declaration instanceof CASTSimpleDeclaration cSimpleDeclaration) {
				processSimpleDeclaration(cSimpleDeclaration, packageName, sourceFolder, parentContainer, currentVisibility, comments);
			}
			else if(declaration instanceof CPPASTAmbiguousSimpleDeclaration cppAmbiguousSimpleDeclaration) {
				
			}
			else if(declaration instanceof CASTAmbiguousSimpleDeclaration cAmbiguousSimpleDeclaration) {
				
			}
			else if(declaration instanceof CPPASTStructuredBindingDeclaration cppStructuredBindingDeclaration) {
				//A structured binding declaration is a feature introduced in C++17 that allows you to unpack or decompose a target object into individual named variables.
				//Similar to destructuring or unpacking in languages like JavaScript and Python, it directly binds specified identifiers to the sub-objects, members, or elements of an initializer.
			}
			else if(declaration instanceof CASTFunctionDefinition cFunctionDefinition) {
				UMLOperation operation = processFunctionDefinition(cFunctionDefinition, packageName, sourceFolder, parentContainer, currentVisibility, comments);
				parentContainer.addOperation(operation);
			}
			else if(declaration instanceof CPPASTFunctionDefinition cppFunctionDefinition) {
				//org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionWithTryBlock is a subclass
				//Function-Try-Block should be handled similar to Kotlin, which allows functions to have a try-expression as a body
				UMLOperation operation = processFunctionDefinition(cppFunctionDefinition, packageName, sourceFolder, parentContainer, currentVisibility, comments);
				parentContainer.addOperation(operation);
			}
			else if(declaration instanceof CPPASTAliasDeclaration cppAliasDeclaration) {
				//A C++ alias declaration (introduced in C++11) uses the using keyword to create a readable, interchangeable synonym for an existing type or template. It is the modern, preferred alternative to typedef.
				processCppAliasDeclaration(cppAliasDeclaration, sourceFolder, parentContainer, null);
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
			}
			else if(declaration instanceof CPPASTExplicitTemplateInstantiation cppTemplateInstantiation) {
				//templates are typically defined in separate header files (.hpp) and are instantiated as a 'template class'
				//Explicit template instantiation forces the C++ compiler to generate code for a template with specific arguments. This allows you to split template definitions into separate .h and .cpp files.
			}
			else if(declaration instanceof CPPASTTemplateDeclaration cppTemplateDeclaration) {
				processCppTemplateDeclaration(cppTemplateDeclaration, packageName, sourceFolder, parentContainer, currentVisibility);
			}
			else if(declaration instanceof CPPASTTemplateSpecialization cppTemplateSpecialization) {
				//Template specialization allows you to override the generic behavior of a C++ template and define a custom implementation for specific data types or conditions.
				//While primary templates provide a blueprint for all types, specialization handles unique edge cases—such as treating const char* or bool differently for performance or behavioral optimizations.
				//C++ supports two types of template specialization: Explicit (Full) Specialization and Partial Specialization.
				processCppTemplateSpecialization(cppTemplateSpecialization, packageName, sourceFolder, parentContainer, currentVisibility);
			}
			else if(declaration instanceof CPPASTInitCapture cppInitCapture) {
				//Init capture (also called generalized lambda capture) was introduced in C++14 to let you declare and initialize new variables directly inside a lambda's capture brackets [...]
			}
			else if(declaration instanceof CPPASTLinkageSpecification cppLinkageSpecification) {
				//C++ linkage specifications (extern "C") direct the compiler to use specific linkage and calling conventions for different programming languages. By preventing C++ name mangling, it allows seamless calls between C++ and C code.
				//C++ supports features like function overloading, which requires the compiler to "mangle" (decorate) function names with argument types so the linker can tell them apart.
				//A C compiler doesn't do this, meaning C++ object code cannot normally find a C library's function. A linkage specification disables C++ mangling for that block of code, ensuring the exact function name is emitted for the linker.
				processDeclarations(packageName, sourceFolder, parentContainer, cppLinkageSpecification.getDeclarations());
			}
			else if(declaration instanceof CPPASTNamespaceAlias cppNamespaceAlias) {
				//In C++, a namespace alias allows you to create a shorter or alternative name for a long or deeply nested namespace. You define it using the syntax namespace alias_name = existing_namespace;
			}
			else if(declaration instanceof CPPASTNamespaceDefinition cppNamespaceDefinition) {
				//In C++, a namespace is a declarative region that provides a distinct scope to identifiers (such as names of types, functions, variables, and classes) to prevent naming collisions and organize code into logical groups.
				IASTName name = cppNamespaceDefinition.getName();
				String namespace = name.getRawSignature();
				IASTDeclaration[] nameSpaceDeclarations = cppNamespaceDefinition.getDeclarations();
				String qualifiedNamespace = packageName + "." + namespace;
				processDeclarations(qualifiedNamespace, sourceFolder, parentContainer, nameSpaceDeclarations, comments);
			}
			else if(declaration instanceof CPPASTStaticAssertionDeclaration cppStaticAssertionDeclaration) {
				//In C++, a static_assert declaration tests a software condition at compile time. If the condition evaluates to false, the compiler stops and issues a compilation error.
				//Because it is evaluated entirely during compilation, it incurs zero runtime performance or size cost
			}
			else if(declaration instanceof CPPASTUsingDeclaration cppUsingDeclaration) {
				//A using-declaration in C++ introduces a specific member from another namespace or a base class into the current scope. It allows you to use that specific name without explicitly typing its fully qualified path or prefix every time.
			}
			else if(declaration instanceof CPPASTUsingDirective cppUsingDirective) {
				//In C++, a using directive allows all identifiers within a specific namespace to be used without explicit qualification. It uses the syntax using namespace namespace_name;
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
		}
	}

	private UMLClass createModuleClass(IASTTranslationUnit ast, String sourceFolder) {
		String moduleName = moduleName(filePath);
		LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, ast, CodeElementType.TYPE_DECLARATION, fileContent);
		UMLClass umlClass = new UMLClass("", moduleName, locationInfo, true, Collections.emptyList());
		umlClass.setModule(true);
		umlClass.setStatic(true);
		umlClass.setVisibility(Visibility.PUBLIC);
		umlClass.setActualSignature(moduleName);
		return umlClass;
	}

	private void processSimpleDeclaration(IASTSimpleDeclaration simpleDeclaration, String packageName, String sourceFolder, UMLAbstractClass parentContainer, Visibility currentVisibility, List<UMLComment> comments) {
		IASTDeclSpecifier declSpecifier = simpleDeclaration.getDeclSpecifier();
		if(declSpecifier instanceof IASTCompositeTypeSpecifier compositeTypeSpecifier) {
			if(compositeTypeSpecifier.getName() == null) {
				return;
			}
			String className = compositeTypeSpecifier.getName().toString();
			if(className.isBlank()) {
				return;
			}
			LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, compositeTypeSpecifier, CodeElementType.TYPE_DECLARATION, fileContent);
			UMLClass umlClass = new UMLClass(packageName, className, locationInfo, true, Collections.emptyList());
			umlClass.setVisibility(Visibility.PUBLIC);
			if(compositeTypeSpecifier instanceof ICPPASTCompositeTypeSpecifier cppCompositeTypeSpecifier) {
				umlClass.setFinal(cppCompositeTypeSpecifier.isFinal());
				if(cppCompositeTypeSpecifier.toString().contains("struct")) {
					umlClass.setStruct(true);
				}
			}
			String rawSignature = simpleDeclaration.getRawSignature();
			if(rawSignature.contains("{"))
				rawSignature = rawSignature.substring(0, rawSignature.indexOf("{") + 1);
			umlClass.setActualSignature(rawSignature);
			processDeclarations(packageName + "." + className, sourceFolder, umlClass, compositeTypeSpecifier.getMembers(), comments);
			this.umlModel.addClass(umlClass);
			distributeComments(comments, locationInfo, umlClass.getComments());
		}
		else if(declSpecifier instanceof IASTSimpleDeclSpecifier simpleDeclSpecifier) {
			for(IASTDeclarator declarator : simpleDeclaration.getDeclarators()) {
				if(!(declarator instanceof IASTFunctionDeclarator)) {
					LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, declarator, CodeElementType.FIELD_DECLARATION, fileContent);
					String fieldName = declarator.getName().toString();
					UMLType type = UMLType.extractTypeObject(sourceFolder, filePath, fileContent, declSpecifier, declarator, 0);
					UMLAttribute umlAttribute = new UMLAttribute(fieldName, type, locationInfo, packageName);
					umlAttribute.setVisibility(currentVisibility != null ? currentVisibility : Visibility.PUBLIC);
					VariableDeclaration variableDeclaration = new VariableDeclaration(sourceFolder, filePath, declarator, simpleDeclSpecifier, umlAttribute, new LinkedHashMap<>(), fileContent);
					variableDeclaration.setAttribute(true);
					umlAttribute.setVariableDeclaration(variableDeclaration);
					parentContainer.addAttribute(umlAttribute);
					distributeComments(comments, locationInfo, umlAttribute.getComments());
				}
				else if(declarator instanceof IASTFunctionDeclarator functionDeclarator) {
					UMLOperation operation = processFunctionDeclSpecifier(simpleDeclSpecifier, functionDeclarator, packageName, sourceFolder, parentContainer, currentVisibility, comments);
					parentContainer.addOperation(operation);
				}
			}
		}
	}

	private UMLOperation processFunctionDeclSpecifier(IASTSimpleDeclSpecifier simpleDeclSpecifier, IASTFunctionDeclarator declarator, String className, String sourceFolder, UMLAbstractClass parentContainer, Visibility currentVisibility, List<UMLComment> comments) {
		IASTName functionName = declarator.getName();
		LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, declarator, CodeElementType.METHOD_DECLARATION, fileContent);
		UMLOperation operation = new UMLOperation(functionName.toString(), locationInfo, className);
		operation.setVisibility(currentVisibility != null ? currentVisibility : Visibility.PUBLIC);
		operation.setStatic(simpleDeclSpecifier.getStorageClass() == IASTDeclSpecifier.sc_static);
		operation.setInline(simpleDeclSpecifier.isInline());
		distributeComments(comments, locationInfo, operation.getComments());

		UMLType returnType = UMLType.extractTypeObject(sourceFolder, filePath, fileContent, simpleDeclSpecifier, declarator, 0);
		if(returnType != null) {
			operation.addParameter(new UMLParameter("return", returnType, "return", false));
		}
		else {
			operation.setConstructor(true);
		}

		if(declarator instanceof IASTStandardFunctionDeclarator standardDeclarator) {
			int index = 0;
			for(IASTParameterDeclaration parameter : standardDeclarator.getParameters()) {
				if(UMLType.cleanTypeText(parameter.getDeclSpecifier().getRawSignature()).equals("void") && standardDeclarator.getParameters().length == 1) {
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
			if(standardDeclarator.takesVarArgs()) {
				UMLType varargsType = UMLType.extractTypeObject("Object");
				varargsType.setVarargs();
				operation.addParameter(new UMLParameter("varargs", varargsType, "in", true));
			}
		}

		int start = simpleDeclSpecifier.getFileLocation().getNodeOffset();
		int end = declarator.getFileLocation().getNodeOffset() + declarator.getFileLocation().getNodeLength();
		operation.setActualSignature(fileContent.substring(start, end));
		return operation;
	}

	private UMLOperation processFunctionDefinition(IASTFunctionDefinition functionDefinition, String className, String sourceFolder, UMLAbstractClass parentContainer, Visibility currentVisibility, List<UMLComment> comments) {
		IASTFunctionDeclarator declarator = functionDefinition.getDeclarator();
		IASTName functionName = declarator.getName();
		LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, functionDefinition, CodeElementType.METHOD_DECLARATION, fileContent);
		UMLOperation operation = new UMLOperation(functionName.toString(), locationInfo, className);
		operation.setVisibility(currentVisibility != null ? currentVisibility : Visibility.PUBLIC);
		operation.setStatic(functionDefinition.getDeclSpecifier().getStorageClass() == IASTDeclSpecifier.sc_static);
		operation.setInline(functionDefinition.getDeclSpecifier().isInline());
		distributeComments(comments, locationInfo, operation.getComments());

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
				if(UMLType.cleanTypeText(parameter.getDeclSpecifier().getRawSignature()).equals("void") && standardDeclarator.getParameters().length == 1) {
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
			if(standardDeclarator.takesVarArgs()) {
				UMLType varargsType = UMLType.extractTypeObject("Object");
				varargsType.setVarargs();
				operation.addParameter(new UMLParameter("varargs", varargsType, "in", true));
			}
		}

		operation.setActualSignature(extractActualSignature(functionDefinition));
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
		}
		return operation;
	}

	private void processCppTemplateDeclaration(CPPASTTemplateDeclaration templateDeclaration, String packageName, String sourceFolder, UMLAbstractClass parentContainer, Visibility currentVisibility) {
		processNestedTemplateDeclaration(templateDeclaration.getDeclaration(), templateDeclaration.getRawSignature(), templateDeclaration.getTemplateParameters(), false, packageName, sourceFolder, parentContainer, currentVisibility);
	}

	private void processCppTemplateSpecialization(CPPASTTemplateSpecialization templateSpecialization, String packageName, String sourceFolder, UMLAbstractClass parentContainer, Visibility currentVisibility) {
		processNestedTemplateDeclaration(templateSpecialization.getDeclaration(), templateSpecialization.getRawSignature(), null, true, packageName, sourceFolder, parentContainer, currentVisibility);
	}

	private void processNestedTemplateDeclaration(IASTDeclaration nestedDeclaration, String actualSignature, ICPPASTTemplateParameter[] templateParameters, boolean fullTemplateSpecialization, String packageName, String sourceFolder, UMLAbstractClass parentContainer, Visibility currentVisibility) {
		if(nestedDeclaration instanceof IASTFunctionDefinition functionDefinition) {
			UMLOperation operation = processFunctionDefinition(functionDefinition, packageName, sourceFolder, parentContainer, currentVisibility);
			addTemplateParameters(operation, templateParameters, sourceFolder);
			operation.setActualSignature(actualSignature);
			parentContainer.addOperation(operation);
		}
		else if(nestedDeclaration instanceof CPPASTAliasDeclaration aliasDeclaration) {
			processCppAliasDeclaration(aliasDeclaration, sourceFolder, parentContainer, templateParameters);
		}
		else if(nestedDeclaration instanceof IASTSimpleDeclaration simpleDeclaration) {
			processNestedTemplateSimpleDeclaration(simpleDeclaration, actualSignature, templateParameters, fullTemplateSpecialization, packageName, sourceFolder, parentContainer, currentVisibility);
		}
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
		umlClass.addTypeAlias(umlTypeAlias);
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
			rightType = UMLType.extractTypeObject(UMLType.cleanTypeText(mappingTypeId.getRawSignature()), "<", ">", typeLocationInfo);
		}
		return new UMLTypeAlias(alias.toString(), rightType, locationInfo);
	}

	private void processNestedTemplateSimpleDeclaration(IASTSimpleDeclaration simpleDeclaration, String actualSignature, ICPPASTTemplateParameter[] templateParameters, boolean fullTemplateSpecialization, String packageName, String sourceFolder, UMLAbstractClass parentContainer, Visibility currentVisibility) {
		IASTDeclSpecifier declSpecifier = simpleDeclaration.getDeclSpecifier();
		if(declSpecifier instanceof IASTCompositeTypeSpecifier compositeTypeSpecifier) {
			IASTName name = compositeTypeSpecifier.getName();
			if(name == null || name.toString().isBlank()) {
				return;
			}
			String className = name.toString();
			LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, compositeTypeSpecifier, CodeElementType.TYPE_DECLARATION, fileContent);
			UMLClass umlClass = new UMLClass(packageName, className, locationInfo, true, Collections.emptyList());
			umlClass.setVisibility(currentVisibility != null ? currentVisibility : Visibility.PUBLIC);
			if(compositeTypeSpecifier instanceof ICPPASTCompositeTypeSpecifier cppCompositeTypeSpecifier) {
				umlClass.setFinal(cppCompositeTypeSpecifier.isFinal());
			}
			if(name instanceof ICPPASTTemplateId templateId) {
				umlClass.setTemplateSpecialization(templateId.getTemplateName().toString(), extractTemplateArguments(templateId), fullTemplateSpecialization);
			}
			addTemplateParameters(umlClass, templateParameters, sourceFolder);
			umlClass.setActualSignature(actualSignature);
			processDeclarations(packageName + "." + className, sourceFolder, umlClass, compositeTypeSpecifier.getMembers());
			this.umlModel.addClass(umlClass);
		}
		else if(declSpecifier instanceof IASTSimpleDeclSpecifier simpleDeclSpecifier) {
			for(IASTDeclarator declarator : simpleDeclaration.getDeclarators()) {
				if(declarator instanceof IASTFunctionDeclarator functionDeclarator) {
					UMLOperation operation = processFunctionDeclSpecifier(simpleDeclSpecifier, functionDeclarator, packageName, sourceFolder, parentContainer, currentVisibility);
					addTemplateParameters(operation, templateParameters, sourceFolder);
					operation.setActualSignature(actualSignature);
					parentContainer.addOperation(operation);
				}
			}
		}
	}

	private List<String> extractTemplateArguments(ICPPASTTemplateId templateId) {
		List<String> arguments = new ArrayList<>();
		for(IASTNode argument : templateId.getTemplateArguments()) {
			arguments.add(argument.getRawSignature());
		}
		return arguments;
	}
	//add parameters to a class
	private void addTemplateParameters(UMLClass umlClass, ICPPASTTemplateParameter[] templateParameters, String sourceFolder) {
		if(templateParameters == null || templateParameters.length == 0) {
			return;
		}
		for(ICPPASTTemplateParameter parameter : templateParameters) {
			UMLTypeParameter umlTypeParameter = createTemplateParameter(parameter, sourceFolder);
			if(umlTypeParameter != null) {
				umlClass.addTypeParameter(umlTypeParameter);
			}
		}
	}
	//add paramteters to an operation
	private void addTemplateParameters(UMLOperation operation, ICPPASTTemplateParameter[] templateParameters, String sourceFolder) {
		if(templateParameters == null || templateParameters.length == 0) {
			return;
		}
		for(ICPPASTTemplateParameter parameter : templateParameters) {
			UMLTypeParameter umlTypeParameter = createTemplateParameter(parameter, sourceFolder);
			if(umlTypeParameter != null) {
				operation.addTypeParameter(umlTypeParameter);
			}
		}
	}
	// adds template parameters to a UMLTypeAlias
	private void addTemplateParameters(UMLTypeAlias typeAlias, ICPPASTTemplateParameter[] templateParameters, String sourceFolder) {
		if(templateParameters == null || templateParameters.length == 0) {
			return;
		}
		for(ICPPASTTemplateParameter parameter : templateParameters) {
			UMLTypeParameter umlTypeParameter = createTemplateParameter(parameter, sourceFolder);
			if(umlTypeParameter != null) {
				typeAlias.addTypeParameter(umlTypeParameter);
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

	
	private String extractParameterName(IASTParameterDeclaration parameter, int index) {
		IASTDeclarator declarator = parameter.getDeclarator();
		if(declarator != null && declarator.getName() != null) {
			String name = declarator.getName().toString();
			if(!name.isBlank()) {
				return name;
			}
		}
		return "arg" + index;
	}

	private String extractActualSignature(IASTFunctionDefinition functionDefinition) {
		IASTFileLocation functionLocation = functionDefinition.getFileLocation();
		if(functionLocation == null) {
			return functionDefinition.getRawSignature();
		}
		int start = functionLocation.getNodeOffset();
		int end = start + functionLocation.getNodeLength();
		if(functionDefinition.getBody() != null && functionDefinition.getBody().getFileLocation() != null) {
			end = functionDefinition.getBody().getFileLocation().getNodeOffset() + 1;
		}
		return fileContent.substring(start, end);
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
