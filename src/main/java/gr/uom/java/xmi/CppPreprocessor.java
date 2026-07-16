package gr.uom.java.xmi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import org.eclipse.cdt.core.dom.ast.IASTArrayDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorElifStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorElseStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorEndifStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfdefStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfndefStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplatedTypeTemplateParameter;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTLinkageSpecification;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNamespaceDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTemplateDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTemplateSpecialization;

public class CppPreprocessor {
	private final CppFileProcessor fileProcessor;
	private List<int[]> conditionalBranches = new ArrayList<>();
	private final Map<UMLOperation, IASTNode> operationOrigins = new IdentityHashMap<>();
	private final Map<UMLOperation, String> operationTemplateIdentities = new IdentityHashMap<>();
	private final Map<UMLOperation, List<String>> operationCanonicalParameterTypes = new IdentityHashMap<>();
	private final Map<UMLOperation, String> operationQualifierIdentities = new IdentityHashMap<>();
	private final Map<UMLAttribute, IASTNode> attributeOrigins = new IdentityHashMap<>();
	private final Map<UMLImport, IASTNode> importOrigins = new IdentityHashMap<>();
	private final Map<UMLTypeAlias, IASTNode> typeAliasOrigins = new IdentityHashMap<>();

	public CppPreprocessor(CppFileProcessor fileProcessor) {
		this.fileProcessor = fileProcessor;
	}

	private static class DeclarationGroup {
		private final IASTDeclaration[] declarations;
		private final Visibility initialVisibility;

		private DeclarationGroup(IASTDeclaration[] declarations, Visibility initialVisibility) {
			this.declarations = declarations;
			this.initialVisibility = initialVisibility;
		}
	}

	void processDeclarations(String packageName, String sourceFolder, UMLAbstractClass parentContainer, IASTDeclaration[] declarations, List<UMLComment> comments, ICPPASTTemplateParameter[] templateParameters) {
		processDeclarationGroups(packageName, sourceFolder, parentContainer,
				Collections.singletonList(new DeclarationGroup(declarations, null)), comments, templateParameters);
	}

	void processDeclarationGroups(String packageName, String sourceFolder, UMLAbstractClass parentContainer, IASTDeclaration declaration, List<UMLComment> comments, ICPPASTTemplateParameter[] templateParameters, List<IASTDeclaration> inactiveContainerAlternatives) {
		processDeclarationGroups(packageName, sourceFolder, parentContainer,
				containerDeclarationGroups(declaration, inactiveContainerAlternatives), comments, templateParameters);
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
					currentVisibility = fileProcessor.processDeclaration(packageName, sourceFolder, parentContainer, comments, currentVisibility,
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

	List<IASTDeclaration> unwrapAlternatives(List<IASTDeclaration> alternatives) {
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
	void buildConditionalBranches(IASTPreprocessorStatement[] statements) {
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
		this.conditionalBranches = branches;
	}

	void addOperation(UMLAbstractClass parentContainer, UMLOperation operation, IASTNode origin,
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

	void addAttribute(UMLAbstractClass parentContainer, UMLAttribute attribute, IASTNode origin) {
		if(retainModelElement(attribute, origin, parentContainer.getAttributes(), attributeOrigins,
				(first, second) -> first.getName().equals(second.getName()),
				inactiveSibling -> removeByIdentity(parentContainer.getAttributes(), inactiveSibling))) {
			parentContainer.addAttribute(attribute);
		}
	}

	void addImport(UMLAbstractClass parentContainer, UMLImport umlImport, IASTNode origin) {
		if(retainModelElement(umlImport, origin, parentContainer.getImportedTypes(), importOrigins, UMLImport::equals,
				inactiveSibling -> removeByIdentity(parentContainer.getImportedTypes(), inactiveSibling))) {
			parentContainer.getImportedTypes().add(umlImport);
		}
	}

	void addTypeAlias(UMLClass umlClass, UMLTypeAlias typeAlias, IASTNode origin) {
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
}
