package gr.uom.java.xmi;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import org.eclipse.cdt.core.dom.ast.IASTArrayDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
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
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionWithTryBlock;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
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
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.CppOperationBody;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class CppFileProcessor {
	private String filePath;
	private String fileContent;
	private UMLModel umlModel;
	private List<int[]> conditionalBranches = new ArrayList<>();
	private final Map<UMLOperation, IASTNode> operationOrigins = new IdentityHashMap<>();
	private final Map<UMLOperation, String> operationTemplateIdentities = new IdentityHashMap<>();
	private final Map<UMLOperation, List<String>> operationCanonicalParameterTypes = new IdentityHashMap<>();
	private final Map<UMLOperation, String> operationQualifierIdentities = new IdentityHashMap<>();
	private final Map<UMLAttribute, IASTNode> attributeOrigins = new IdentityHashMap<>();
	private final Map<UMLImport, IASTNode> importOrigins = new IdentityHashMap<>();
	private final Map<UMLTypeAlias, IASTNode> typeAliasOrigins = new IdentityHashMap<>();

	private static class DeclarationGroup {
		private final IASTDeclaration[] declarations;
		private final Visibility initialVisibility;

		private DeclarationGroup(IASTDeclaration[] declarations, Visibility initialVisibility) {
			this.declarations = declarations;
			this.initialVisibility = initialVisibility;
		}
	}

	public CppFileProcessor(UMLModel umlModel) {
		this.umlModel = umlModel;
	}

	public void processCppFile(String filePath, String fileContent, boolean astDiff) {
		this.filePath = filePath;
		this.fileContent = fileContent;
		this.conditionalBranches = new ArrayList<>();
		this.operationOrigins.clear();
		this.operationTemplateIdentities.clear();
		this.operationCanonicalParameterTypes.clear();
		this.operationQualifierIdentities.clear();
		this.attributeOrigins.clear();
		this.importOrigins.clear();
		this.typeAliasOrigins.clear();
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
						GPPLanguage.OPTION_IS_SOURCE_UNIT | GPPLanguage.OPTION_PARSE_INACTIVE_CODE,
						new DefaultLogService()
						);
				String sourceFolder = extractCppSourceFolder();
				List<UMLComment> comments = extractInternalComments(ast.getComments(), sourceFolder, filePath, fileContent);
				this.umlModel.getCommentMap().put(filePath, comments);
				UMLClass moduleClass = createModuleClass(ast, sourceFolder);
				this.conditionalBranches = buildConditionalBranches(ast.getAllPreprocessorStatements());
				processPreprocessorStatements(sourceFolder, moduleClass, ast.getAllPreprocessorStatements());
				processDeclarations(moduleClass.getName(), sourceFolder, moduleClass, ast.getDeclarations(true), comments, new ICPPASTTemplateParameter[0]);
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
				this.conditionalBranches = new ArrayList<>();
				processPreprocessorStatements(sourceFolder, moduleClass, ast.getAllPreprocessorStatements());
				processDeclarations(moduleClass.getName(), sourceFolder, moduleClass, ast.getDeclarations(), comments, new ICPPASTTemplateParameter[0]);
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

	private void processDeclarations(String packageName, String sourceFolder, UMLAbstractClass parentContainer, IASTDeclaration[] declarations, List<UMLComment> comments, ICPPASTTemplateParameter[] templateParameters) {
		processDeclarationGroups(packageName, sourceFolder, parentContainer,
				Collections.singletonList(new DeclarationGroup(declarations, null)), comments, templateParameters);
	}

	private void processDeclarationGroups(String packageName, String sourceFolder, UMLAbstractClass parentContainer,
			List<DeclarationGroup> declarationGroups, List<UMLComment> comments, ICPPASTTemplateParameter[] templateParameters) {
		List<IASTDeclaration> allDeclarations = new ArrayList<>();
		for(DeclarationGroup group : declarationGroups) {
			Collections.addAll(allDeclarations, group.declarations);
		}
		Map<IASTDeclaration, List<IASTDeclaration>> alternatives = new IdentityHashMap<>();
		Set<IASTDeclaration> mergedInactiveContainers = Collections.newSetFromMap(new IdentityHashMap<>());
		collectInactiveContainerAlternatives(allDeclarations.toArray(new IASTDeclaration[0]), alternatives, mergedInactiveContainers);
		for(DeclarationGroup group : declarationGroups) {
			Visibility currentVisibility = group.initialVisibility;
			for(IASTDeclaration declaration : group.declarations) {
				if(!mergedInactiveContainers.contains(declaration)) {
					currentVisibility = processDeclaration(packageName, sourceFolder, parentContainer, comments, currentVisibility,
							declaration, templateParameters, alternatives.getOrDefault(declaration, Collections.emptyList()));
				}
			}
		}
	}

	private void collectInactiveContainerAlternatives(IASTDeclaration[] declarations,
			Map<IASTDeclaration, List<IASTDeclaration>> alternatives, Set<IASTDeclaration> mergedInactiveContainers) {
		Map<String, List<IASTDeclaration>> activeContainers = new LinkedHashMap<>();
		for(IASTDeclaration declaration : declarations) {
			String key = containerKey(declaration);
			IASTDeclaration unwrapped = unwrapDeclaration(declaration);
			if(key != null && unwrapped.isActive()) {
				activeContainers.computeIfAbsent(key, ignored -> new ArrayList<>()).add(declaration);
			}
		}
		for(IASTDeclaration declaration : declarations) {
			String key = containerKey(declaration);
			IASTDeclaration unwrapped = unwrapDeclaration(declaration);
			if(key == null || unwrapped.isActive()) {
				continue;
			}
			for(IASTDeclaration activeContainer : activeContainers.getOrDefault(key, Collections.emptyList())) {
				if(areSiblingBranches(unwrapped, unwrapDeclaration(activeContainer))) {
					alternatives.computeIfAbsent(activeContainer, ignored -> new ArrayList<>()).add(declaration);
					mergedInactiveContainers.add(declaration);
					break;
				}
			}
		}
	}

	private String containerKey(IASTDeclaration declaration) {
		IASTDeclaration unwrapped = unwrapDeclaration(declaration);
		if(unwrapped instanceof IASTSimpleDeclaration simpleDeclaration &&
				simpleDeclaration.getDeclSpecifier() instanceof IASTCompositeTypeSpecifier compositeTypeSpecifier &&
				compositeTypeSpecifier.getName() != null && !compositeTypeSpecifier.getName().toString().isBlank()) {
			// Keep class-template parameter names in the key. Merging alpha-renamed class alternatives
			// would otherwise attach inactive members that refer to U to an active class that declares T.
			return "type " + compositeTypeSpecifier.getName() + templateParameterIdentity(declaration, true);
		}
		if(unwrapped instanceof CPPASTNamespaceDefinition namespaceDefinition) {
			return "namespace " + namespaceDefinition.getName();
		}
		if(unwrapped instanceof CPPASTLinkageSpecification linkageSpecification) {
			return "linkage " + linkageSpecification.getLiteral();
		}
		return null;
	}

	private String templateParameterIdentity(IASTDeclaration declaration, boolean includeNames) {
		if(declaration instanceof CPPASTTemplateDeclaration templateDeclaration) {
			return templateParameterIdentity(templateDeclaration.getTemplateParameters(), includeNames) +
					templateParameterIdentity(templateDeclaration.getDeclaration(), includeNames);
		}
		if(declaration instanceof CPPASTTemplateSpecialization templateSpecialization) {
			return templateParameterIdentity(templateSpecialization.getTemplateParameters(), includeNames) +
					templateParameterIdentity(templateSpecialization.getDeclaration(), includeNames);
		}
		return "";
	}

	private String templateParameterIdentity(ICPPASTTemplateParameter[] parameters, boolean includeNames) {
		if(parameters.length == 0) {
			return "";
		}
		StringBuilder identity = new StringBuilder("<");
		for(ICPPASTTemplateParameter parameter : parameters) {
			if(parameter instanceof ICPPASTSimpleTypeTemplateParameter typeParameter) {
				identity.append("type");
				if(includeNames && typeParameter.getName() != null) {
					identity.append(':').append(normalizeTemplateIdentity(typeParameter.getName().toString()));
				}
			}
			else if(parameter instanceof ICPPASTParameterDeclaration nonTypeParameter) {
				identity.append("value:")
						.append(normalizeTemplateIdentity(nonTypeParameter.getDeclSpecifier().getRawSignature()))
						.append(':')
						.append(declaratorTypeIdentity(nonTypeParameter.getDeclarator()));
				if(includeNames && nonTypeParameter.getDeclarator() != null && nonTypeParameter.getDeclarator().getName() != null) {
					identity.append(':').append(normalizeTemplateIdentity(nonTypeParameter.getDeclarator().getName().toString()));
				}
			}
			else if(parameter instanceof ICPPASTTemplatedTypeTemplateParameter templateTemplateParameter) {
				identity.append("template")
						.append(templateParameterIdentity(templateTemplateParameter.getTemplateParameters(), includeNames));
				if(includeNames && templateTemplateParameter.getName() != null) {
					identity.append(':').append(normalizeTemplateIdentity(templateTemplateParameter.getName().toString()));
				}
			}
			else {
				identity.append(normalizeTemplateIdentity(parameter.getRawSignature()));
			}
			if(parameter.isParameterPack()) {
				identity.append("...");
			}
			identity.append(';');
		}
		return identity.append('>').toString();
	}

	private String declaratorTypeIdentity(IASTDeclarator declarator) {
		if(declarator == null) {
			return "";
		}
		String rawSignature = declarator.getRawSignature();
		IASTFileLocation declaratorLocation = declarator.getFileLocation();
		if(declaratorLocation == null) {
			return normalizeTemplateIdentity(rawSignature);
		}
		List<IASTNode> excludedNodes = new ArrayList<>();
		if(declarator.getName() != null) {
			excludedNodes.add(declarator.getName());
		}
		if(declarator.getInitializer() != null) {
			excludedNodes.add(declarator.getInitializer());
		}
		excludedNodes.sort((first, second) -> Integer.compare(
				second.getFileLocation() != null ? second.getFileLocation().getNodeOffset() : Integer.MIN_VALUE,
				first.getFileLocation() != null ? first.getFileLocation().getNodeOffset() : Integer.MIN_VALUE));
		for(IASTNode excludedNode : excludedNodes) {
			IASTFileLocation excludedLocation = excludedNode.getFileLocation();
			if(excludedLocation == null) {
				continue;
			}
			int start = excludedLocation.getNodeOffset() - declaratorLocation.getNodeOffset();
			int end = start + excludedLocation.getNodeLength();
			if(start >= 0 && end <= rawSignature.length()) {
				rawSignature = rawSignature.substring(0, start) + rawSignature.substring(end);
			}
		}
		return normalizeTemplateIdentity(rawSignature);
	}

	private String normalizeTemplateIdentity(String text) {
		return text.replaceAll("\\s+", "");
	}

	private IASTDeclaration unwrapDeclaration(IASTDeclaration declaration) {
		if(declaration instanceof CPPASTTemplateDeclaration templateDeclaration) {
			return unwrapDeclaration(templateDeclaration.getDeclaration());
		}
		if(declaration instanceof CPPASTTemplateSpecialization templateSpecialization) {
			return unwrapDeclaration(templateSpecialization.getDeclaration());
		}
		return declaration;
	}

	private List<IASTDeclaration> unwrapAlternatives(List<IASTDeclaration> alternatives) {
		List<IASTDeclaration> unwrapped = new ArrayList<>();
		for(IASTDeclaration alternative : alternatives) {
			unwrapped.add(unwrapDeclaration(alternative));
		}
		return unwrapped;
	}

	private List<DeclarationGroup> containerDeclarationGroups(IASTDeclaration declaration, List<IASTDeclaration> alternatives) {
		List<DeclarationGroup> groups = new ArrayList<>();
		groups.add(containerDeclarationGroup(unwrapDeclaration(declaration)));
		for(IASTDeclaration alternative : alternatives) {
			groups.add(containerDeclarationGroup(unwrapDeclaration(alternative)));
		}
		return groups;
	}

	private DeclarationGroup containerDeclarationGroup(IASTDeclaration declaration) {
		Visibility initialVisibility = null;
		if(declaration instanceof IASTSimpleDeclaration simpleDeclaration &&
				simpleDeclaration.getDeclSpecifier() instanceof IASTCompositeTypeSpecifier compositeTypeSpecifier) {
			initialVisibility = compositeTypeSpecifier.getKey() == ICPPASTCompositeTypeSpecifier.k_class ?
					Visibility.PRIVATE : Visibility.PUBLIC;
		}
		return new DeclarationGroup(containerDeclarations(declaration), initialVisibility);
	}

	private IASTDeclaration[] containerDeclarations(IASTDeclaration declaration) {
		if(declaration instanceof IASTSimpleDeclaration simpleDeclaration &&
				simpleDeclaration.getDeclSpecifier() instanceof IASTCompositeTypeSpecifier compositeTypeSpecifier) {
			return compositeTypeSpecifier.getDeclarations(true);
		}
		if(declaration instanceof CPPASTNamespaceDefinition namespaceDefinition) {
			return namespaceDefinition.getDeclarations(true);
		}
		if(declaration instanceof CPPASTLinkageSpecification linkageSpecification) {
			return linkageSpecification.getDeclarations(true);
		}
		return new IASTDeclaration[0];
	}

	private Map<Integer, Integer> enclosingConditionalBranches(IASTNode node) {
		Map<Integer, Integer> branches = new HashMap<>();
		IASTFileLocation location = node.getFileLocation();
		if(location == null || !node.isPartOfTranslationUnitFile()) {
			return branches;
		}
		int offset = location.getNodeOffset();
		for(int[] branch : conditionalBranches) {
			if(offset >= branch[2] && offset < branch[3]) {
				branches.put(branch[0], branch[1]);
			}
		}
		return branches;
	}

	private boolean areSiblingBranches(IASTNode first, IASTNode second) {
		Map<Integer, Integer> firstBranches = enclosingConditionalBranches(first);
		Map<Integer, Integer> secondBranches = enclosingConditionalBranches(second);
		for(Map.Entry<Integer, Integer> branch : firstBranches.entrySet()) {
			Integer secondBranchIndex = secondBranches.get(branch.getKey());
			if(secondBranchIndex != null && !secondBranchIndex.equals(branch.getValue())) {
				return true;
			}
		}
		return false;
	}

	//Each entry is {conditional group id, branch index within the group, branch start offset, branch end offset}.
	private List<int[]> buildConditionalBranches(IASTPreprocessorStatement[] statements) {
		List<int[]> branches = new ArrayList<>();
		Deque<int[]> openBranches = new ArrayDeque<>();
		int nextGroupId = 0;
		for(IASTPreprocessorStatement statement : statements) {
			IASTFileLocation location = statement.getFileLocation();
			if(location == null || !statement.isPartOfTranslationUnitFile()) {
				continue;
			}
			int start = location.getNodeOffset();
			int end = start + location.getNodeLength();
			if(statement instanceof IASTPreprocessorIfStatement ||
					statement instanceof IASTPreprocessorIfdefStatement ||
					statement instanceof IASTPreprocessorIfndefStatement) {
				openBranches.push(new int[] {nextGroupId++, 0, end});
			}
			else if(statement instanceof IASTPreprocessorElifStatement ||
					statement instanceof IASTPreprocessorElseStatement) {
				if(!openBranches.isEmpty()) {
					int[] open = openBranches.pop();
					branches.add(new int[] {open[0], open[1], open[2], start});
					openBranches.push(new int[] {open[0], open[1] + 1, end});
				}
			}
			else if(statement instanceof IASTPreprocessorEndifStatement) {
				if(!openBranches.isEmpty()) {
					int[] open = openBranches.pop();
					branches.add(new int[] {open[0], open[1], open[2], start});
				}
			}
		}
		return branches;
	}

	private void addOperation(UMLAbstractClass parentContainer, UMLOperation operation, IASTNode origin,
			ICPPASTTemplateParameter[] templateParameters) {
		operationTemplateIdentities.put(operation, templateParameterIdentity(templateParameters, false));
		operationCanonicalParameterTypes.put(operation, canonicalParameterTypes(operation, templateParameters, origin));
		operationQualifierIdentities.put(operation, operationQualifierIdentity(origin));
		if(retainModelElement(operation, origin, parentContainer.getOperations(), operationOrigins,
				this::sameOperationIdentity, inactiveSibling -> {
					parentContainer.removeOperation(inactiveSibling);
					operationTemplateIdentities.remove(inactiveSibling);
					operationCanonicalParameterTypes.remove(inactiveSibling);
					operationQualifierIdentities.remove(inactiveSibling);
				})) {
			parentContainer.addOperation(operation);
		}
		else {
			operationTemplateIdentities.remove(operation);
			operationCanonicalParameterTypes.remove(operation);
			operationQualifierIdentities.remove(operation);
		}
	}

	private boolean sameOperationIdentity(UMLOperation first, UMLOperation second) {
		return first.getClassName().equals(second.getClassName()) &&
				first.getName().equals(second.getName()) &&
				Objects.equals(operationTemplateIdentities.get(first), operationTemplateIdentities.get(second)) &&
				Objects.equals(operationCanonicalParameterTypes.get(first), operationCanonicalParameterTypes.get(second)) &&
				Objects.equals(operationQualifierIdentities.get(first), operationQualifierIdentities.get(second));
	}

	private String operationQualifierIdentity(IASTNode origin) {
		IASTFunctionDeclarator declarator = functionDeclarator(origin);
		if(declarator instanceof ICPPASTFunctionDeclarator cppDeclarator) {
			return "const=" + cppDeclarator.isConst() +
					",volatile=" + cppDeclarator.isVolatile() +
					",ref=" + cppDeclarator.getRefQualifier();
		}
		return "";
	}

	private IASTFunctionDeclarator functionDeclarator(IASTNode origin) {
		if(origin instanceof IASTFunctionDefinition functionDefinition) {
			return functionDefinition.getDeclarator();
		}
		if(origin instanceof IASTFunctionDeclarator functionDeclarator) {
			return functionDeclarator;
		}
		return null;
	}

	private List<String> canonicalParameterTypes(UMLOperation operation, ICPPASTTemplateParameter[] templateParameters,
			IASTNode origin) {
		Map<String, Integer> parameterPositions = templateParameterPositions(templateParameters);
		List<IASTParameterDeclaration> parameterDeclarations = modeledParameterDeclarations(origin);
		List<String> canonicalTypes = new ArrayList<>();
		List<UMLType> parameterTypes = operation.getParameterTypeList();
		for(int i = 0; i < parameterTypes.size(); i++) {
			UMLType parameterType = parameterTypes.get(i);
			if(parameterType == null) {
				canonicalTypes.add("{unknown-parameter-type}");
				continue;
			}
			String canonicalType = parameterType.toQualifiedString();
			if(i < parameterDeclarations.size()) {
				canonicalType = normalizeTopLevelParameterCv(canonicalType, parameterDeclarations.get(i));
			}
			canonicalTypes.add(canonicalizeTemplateParameterNames(canonicalType, parameterPositions));
		}
		return canonicalTypes;
	}

	private List<IASTParameterDeclaration> modeledParameterDeclarations(IASTNode origin) {
		IASTFunctionDeclarator declarator = functionDeclarator(origin);
		if(!(declarator instanceof IASTStandardFunctionDeclarator standardDeclarator)) {
			return Collections.emptyList();
		}
		IASTParameterDeclaration[] parameters = standardDeclarator.getParameters();
		if(parameters.length == 1 && UMLType.cleanTypeText(parameters[0].getDeclSpecifier().getRawSignature()).equals("void")) {
			return Collections.emptyList();
		}
		return List.of(parameters);
	}

	private String normalizeTopLevelParameterCv(String type, IASTParameterDeclaration parameter) {
		IASTDeclarator declarator = parameter.getDeclarator();
		if(declarator == null) {
			return type;
		}
		boolean hasPointerOrReference = false;
		boolean hasArray = false;
		IASTDeclarator nestedDeclarator = declarator;
		while(nestedDeclarator != null) {
			hasPointerOrReference |= nestedDeclarator.getPointerOperators().length > 0;
			hasArray |= nestedDeclarator instanceof IASTArrayDeclarator;
			nestedDeclarator = nestedDeclarator.getNestedDeclarator();
		}
		if(hasPointerOrReference) {
			return removeTrailingCvQualifiers(type);
		}
		if(hasArray) {
			return type;
		}
		String normalized = type;
		if(parameter.getDeclSpecifier().isConst()) {
			normalized = removeTopLevelQualifier(normalized, "const");
		}
		if(parameter.getDeclSpecifier().isVolatile()) {
			normalized = removeTopLevelQualifier(normalized, "volatile");
		}
		return normalized;
	}

	private String removeTrailingCvQualifiers(String type) {
		String normalized = type.trim();
		while(normalized.matches(".*\\b(?:const|volatile)\\s*$")) {
			normalized = normalized.replaceFirst("\\s*\\b(?:const|volatile)\\s*$", "").trim();
		}
		return normalized;
	}

	private String removeTopLevelQualifier(String type, String qualifier) {
		int templateDepth = 0;
		for(int offset = 0; offset < type.length();) {
			int codePoint = type.codePointAt(offset);
			if(codePoint == '<') {
				templateDepth++;
			}
			else if(codePoint == '>' && templateDepth > 0) {
				templateDepth--;
			}
			else if(templateDepth == 0 && (codePoint == '_' || Character.isUnicodeIdentifierStart(codePoint))) {
				int end = offset + Character.charCount(codePoint);
				while(end < type.length()) {
					int nextCodePoint = type.codePointAt(end);
					if(!Character.isUnicodeIdentifierPart(nextCodePoint)) {
						break;
					}
					end += Character.charCount(nextCodePoint);
				}
				if(type.substring(offset, end).equals(qualifier)) {
					return (type.substring(0, offset) + type.substring(end)).replaceAll("\\s+", " ").trim();
				}
				offset = end;
				continue;
			}
			offset += Character.charCount(codePoint);
		}
		return type;
	}

	private Map<String, Integer> templateParameterPositions(ICPPASTTemplateParameter[] templateParameters) {
		Map<String, Integer> positions = new HashMap<>();
		for(int i = 0; i < templateParameters.length; i++) {
			String name = templateParameterName(templateParameters[i]);
			if(name != null && !name.isBlank()) {
				positions.put(name, i);
			}
		}
		return positions;
	}

	private String templateParameterName(ICPPASTTemplateParameter parameter) {
		if(parameter instanceof ICPPASTSimpleTypeTemplateParameter typeParameter && typeParameter.getName() != null) {
			return typeParameter.getName().toString();
		}
		if(parameter instanceof ICPPASTParameterDeclaration nonTypeParameter && nonTypeParameter.getDeclarator() != null &&
				nonTypeParameter.getDeclarator().getName() != null) {
			return nonTypeParameter.getDeclarator().getName().toString();
		}
		if(parameter instanceof ICPPASTTemplatedTypeTemplateParameter templateTemplateParameter && templateTemplateParameter.getName() != null) {
			return templateTemplateParameter.getName().toString();
		}
		return null;
	}

	private String canonicalizeTemplateParameterNames(String type, Map<String, Integer> parameterPositions) {
		StringBuilder canonicalType = new StringBuilder();
		for(int offset = 0; offset < type.length();) {
			int codePoint = type.codePointAt(offset);
			if(codePoint == '_' || Character.isUnicodeIdentifierStart(codePoint)) {
				int end = offset + Character.charCount(codePoint);
				while(end < type.length()) {
					int nextCodePoint = type.codePointAt(end);
					if(!Character.isUnicodeIdentifierPart(nextCodePoint)) {
						break;
					}
					end += Character.charCount(nextCodePoint);
				}
				String identifier = type.substring(offset, end);
				Integer position = parameterPositions.get(identifier);
				canonicalType.append(position != null ? "{template-parameter:" + position + "}" : identifier);
				offset = end;
			}
			else {
				canonicalType.appendCodePoint(codePoint);
				offset += Character.charCount(codePoint);
			}
		}
		return canonicalType.toString();
	}

	private void addAttribute(UMLAbstractClass parentContainer, UMLAttribute attribute, IASTNode origin) {
		if(retainModelElement(attribute, origin, parentContainer.getAttributes(), attributeOrigins,
				(first, second) -> first.getName().equals(second.getName()),
				inactiveSibling -> removeByIdentity(parentContainer.getAttributes(), inactiveSibling))) {
			parentContainer.addAttribute(attribute);
		}
	}

	private void addImport(UMLAbstractClass parentContainer, UMLImport umlImport, IASTNode origin) {
		if(retainModelElement(umlImport, origin, parentContainer.getImportedTypes(), importOrigins, UMLImport::equals,
				inactiveSibling -> removeByIdentity(parentContainer.getImportedTypes(), inactiveSibling))) {
			parentContainer.getImportedTypes().add(umlImport);
		}
	}

	private void addTypeAlias(UMLClass umlClass, UMLTypeAlias typeAlias, IASTNode origin) {
		if(retainModelElement(typeAlias, origin, umlClass.getTypeAliasList(), typeAliasOrigins,
				(first, second) -> first.getName().equals(second.getName()),
				inactiveSibling -> removeByIdentity(umlClass.getTypeAliasList(), inactiveSibling))) {
			umlClass.addTypeAlias(typeAlias);
		}
	}

	private <T> boolean retainModelElement(T element, IASTNode origin, List<T> existingElements,
			Map<T, IASTNode> origins, BiPredicate<T, T> sameIdentity, Consumer<T> removeElement) {
		boolean activeSiblingExists = false;
		List<T> inactiveSiblings = new ArrayList<>();
		for(T existing : existingElements) {
			IASTNode existingOrigin = origins.get(existing);
			if(existingOrigin != null && sameIdentity.test(existing, element) && areSiblingBranches(existingOrigin, origin)) {
				if(existingOrigin.isActive()) {
					activeSiblingExists = true;
				}
				else if(origin.isActive()) {
					inactiveSiblings.add(existing);
				}
			}
		}
		if(!origin.isActive() && activeSiblingExists) {
			return false;
		}
		for(T inactiveSibling : inactiveSiblings) {
			removeElement.accept(inactiveSibling);
			origins.remove(inactiveSibling);
		}
		origins.put(element, origin);
		return true;
	}

	private <T> void removeByIdentity(List<T> elements, T element) {
		for(int i = 0; i < elements.size(); i++) {
			if(elements.get(i) == element) {
				elements.remove(i);
				return;
			}
		}
	}

	private Visibility processDeclaration(String packageName, String sourceFolder, UMLAbstractClass parentContainer,
			List<UMLComment> comments, Visibility currentVisibility, IASTDeclaration declaration, ICPPASTTemplateParameter[] templateParameters,
			List<IASTDeclaration> inactiveContainerAlternatives) {
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
				addAttribute(parentContainer, umlAttribute, name);
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
			addOperation(parentContainer, operation, cFunctionDefinition, templateParameters);
		}
		else if(declaration instanceof CPPASTFunctionDefinition cppFunctionDefinition) {
			//org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionWithTryBlock is a subclass
			//Function-Try-Block should be handled similar to Kotlin, which allows functions to have a try-expression as a body
			UMLOperation operation = processFunctionDefinition(cppFunctionDefinition, packageName, sourceFolder, parentContainer, currentVisibility, comments, templateParameters);
			addOperation(parentContainer, operation, cppFunctionDefinition, templateParameters);
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
					cppTemplateDeclaration.getTemplateParameters(), unwrapAlternatives(inactiveContainerAlternatives));
		}
		else if(declaration instanceof CPPASTTemplateSpecialization cppTemplateSpecialization) {
			//Template specialization allows you to override the generic behavior of a C++ template and define a custom implementation for specific data types or conditions.
			//While primary templates provide a blueprint for all types, specialization handles unique edge cases—such as treating const char* or bool differently for performance or behavioral optimizations.
			//C++ supports two types of template specialization: Explicit (Full) Specialization and Partial Specialization.
			IASTDeclaration nestedDeclaration = cppTemplateSpecialization.getDeclaration();
			processDeclaration(packageName, sourceFolder, parentContainer, comments, currentVisibility, nestedDeclaration,
					cppTemplateSpecialization.getTemplateParameters(), unwrapAlternatives(inactiveContainerAlternatives));
		}
		else if(declaration instanceof CPPASTInitCapture cppInitCapture) {
			//Init capture (also called generalized lambda capture) was introduced in C++14 to let you declare and initialize new variables directly inside a lambda's capture brackets [...]
		}
		else if(declaration instanceof CPPASTLinkageSpecification cppLinkageSpecification) {
			//C++ linkage specifications (extern "C") direct the compiler to use specific linkage and calling conventions for different programming languages. By preventing C++ name mangling, it allows seamless calls between C++ and C code.
			//C++ supports features like function overloading, which requires the compiler to "mangle" (decorate) function names with argument types so the linker can tell them apart.
			//A C compiler doesn't do this, meaning C++ object code cannot normally find a C library's function. A linkage specification disables C++ mangling for that block of code, ensuring the exact function name is emitted for the linker.
			processDeclarationGroups(packageName, sourceFolder, parentContainer,
					containerDeclarationGroups(cppLinkageSpecification, inactiveContainerAlternatives), comments, templateParameters);
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
			processDeclarationGroups(qualifiedNamespace, sourceFolder, parentContainer,
					containerDeclarationGroups(cppNamespaceDefinition, inactiveContainerAlternatives), comments, templateParameters);
		}
		else if(declaration instanceof CPPASTStaticAssertionDeclaration cppStaticAssertionDeclaration) {
			//In C++, a static_assert declaration tests a software condition at compile time. If the condition evaluates to false, the compiler stops and issues a compilation error.
			//Because it is evaluated entirely during compilation, it incurs zero runtime performance or size cost
		}
		else if(declaration instanceof CPPASTUsingDeclaration cppUsingDeclaration) {
			//A using-declaration in C++ introduces a specific member from another namespace or a base class into the current scope. It allows you to use that specific name without explicitly typing its fully qualified path or prefix every time.
			if(parentContainer instanceof UMLClass umlClass && umlClass.isModule()) {
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
			if(className.isBlank()) {
				return;
			}
			LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, compositeTypeSpecifier, CodeElementType.TYPE_DECLARATION, fileContent);
			UMLClass umlClass = new UMLClass(packageName, className, locationInfo, true, new ArrayList<>());
			umlClass.setVisibility(currentVisibility != null ? currentVisibility : Visibility.PUBLIC);
			if(compositeTypeSpecifier instanceof ICPPASTCompositeTypeSpecifier cppCompositeTypeSpecifier) {
				umlClass.setFinal(cppCompositeTypeSpecifier.isFinal());
				if(cppCompositeTypeSpecifier.toString().contains("struct")) {
					umlClass.setStruct(true);
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
			processDeclarationGroups(packageName + "." + className, sourceFolder, umlClass,
					containerDeclarationGroups(simpleDeclaration, inactiveContainerAlternatives), comments, templateParameters);
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
					addOperation(parentContainer, operation, functionDeclarator, templateParameters);
				}
			}
		}
		else if(declSpecifier instanceof ICPPASTNamedTypeSpecifier namedTypeSpecifier) {
			for(IASTDeclarator declarator : simpleDeclaration.getDeclarators()) {
				if(!(declarator instanceof IASTFunctionDeclarator)) {
					processAttribute(packageName, sourceFolder, parentContainer, currentVisibility, comments, declSpecifier, declarator, templateParameters);
				}
			}
		}
	}

	private void processAttribute(String packageName, String sourceFolder, UMLAbstractClass parentContainer,
			Visibility currentVisibility, List<UMLComment> comments, IASTDeclSpecifier declSpecifier, IASTDeclarator declarator, ICPPASTTemplateParameter[] templateParameters) {
		LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, declarator, CodeElementType.FIELD_DECLARATION, fileContent);
		String fieldName = declarator.getName().toString();
		UMLType type = UMLType.extractTypeObject(sourceFolder, filePath, fileContent, declSpecifier, declarator, 0);
		UMLAttribute umlAttribute = new UMLAttribute(fieldName, type, locationInfo, packageName);
		umlAttribute.setVisibility(currentVisibility != null ? currentVisibility : Visibility.PUBLIC);
		VariableDeclaration variableDeclaration = new VariableDeclaration(sourceFolder, filePath, declarator, declSpecifier, umlAttribute, new LinkedHashMap<>(), fileContent);
		variableDeclaration.setAttribute(true);
		umlAttribute.setVariableDeclaration(variableDeclaration);
		addTemplateParameters(umlAttribute, templateParameters, sourceFolder);
		addAttribute(parentContainer, umlAttribute, declarator);
		distributeComments(comments, locationInfo, umlAttribute.getComments());
	}

	private UMLOperation processFunctionDeclSpecifier(IASTDeclSpecifier declSpecifier, IASTFunctionDeclarator declarator, String className, String sourceFolder, UMLAbstractClass parentContainer, Visibility currentVisibility, List<UMLComment> comments, ICPPASTTemplateParameter[] templateParameters) {
		IASTName functionName = declarator.getName();
		LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, declarator, CodeElementType.METHOD_DECLARATION, fileContent);
		UMLOperation operation = new UMLOperation(functionName.toString(), locationInfo, className);
		operation.setVisibility(currentVisibility != null ? currentVisibility : Visibility.PUBLIC);
		operation.setStatic(declSpecifier.getStorageClass() == IASTDeclSpecifier.sc_static);
		operation.setInline(declSpecifier.isInline());
		distributeComments(comments, locationInfo, operation.getComments());

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
		addTemplateParameters(operation, templateParameters, sourceFolder);

		int start = declSpecifier.getFileLocation().getNodeOffset();
		int end = declarator.getFileLocation().getNodeOffset() + declarator.getFileLocation().getNodeLength();
		operation.setActualSignature(fileContent.substring(start, end));
		return operation;
	}

	private UMLOperation processFunctionDefinition(IASTFunctionDefinition functionDefinition, String className, String sourceFolder, UMLAbstractClass parentContainer, Visibility currentVisibility, List<UMLComment> comments, ICPPASTTemplateParameter[] templateParameters) {
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
		addTemplateParameters(operation, templateParameters, sourceFolder);

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
		if(functionDefinition instanceof ICPPASTFunctionDefinition cppFunctionDefinition) {
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
		addImport(parentContainer, new UMLImport(importName, onDemand, false, locationInfo), declaration);
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
		addTypeAlias(umlClass, umlTypeAlias, aliasDeclaration);
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
