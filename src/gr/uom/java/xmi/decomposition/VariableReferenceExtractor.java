package gr.uom.java.xmi.decomposition;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.VariableDeclarationContainer;

public class VariableReferenceExtractor {

	public static Set<AbstractCodeMapping> findReferences(VariableDeclaration declaration1, VariableDeclaration declaration2, Set<AbstractCodeMapping> mappings) {
		Set<AbstractCodeMapping> references = new LinkedHashSet<AbstractCodeMapping>();
		VariableScope scope1 = declaration1.getScope();
		VariableScope scope2 = declaration2.getScope();
		for(AbstractCodeMapping mapping : mappings) {
			AbstractCodeFragment fragment1 = mapping.getFragment1();
			AbstractCodeFragment fragment2 = mapping.getFragment2();
			if(scope1.subsumes(fragment1.getLocationInfo()) && scope2.subsumes(fragment2.getLocationInfo()) &&
					usesVariable(fragment1, declaration1) && !matchingLocalVariable(declaration1, fragment1, mapping.getOperation1()) &&
					usesVariable(fragment2, declaration2) && !matchingLocalVariable(declaration2, fragment2, mapping.getOperation2())) {
				references.add(mapping);
			}
			AbstractCall invocation1 = fragment1.invocationCoveringEntireFragment();
			AbstractCall invocation2 = fragment2.invocationCoveringEntireFragment();
			if(invocation1 != null && invocation2 != null) {
				//add recursive calls to the mappings
				if(invocation1.matchesOperation(mapping.getOperation1(), mapping.getOperation1(), null) &&
						invocation2.matchesOperation(mapping.getOperation2(), mapping.getOperation2(), null)) {
					references.add(mapping);
				}
			}
		}
		return references;
	}

	private static boolean matchingLocalVariable(VariableDeclaration declaration, AbstractCodeFragment fragment, VariableDeclarationContainer operation) {
		if(declaration.isAttribute()) {
			List<VariableDeclaration> variableDeclarations = operation.getAllVariableDeclarations();
			for(VariableDeclaration localVariableDeclaration : variableDeclarations) {
				if(localVariableDeclaration.getVariableName().equals(declaration.getVariableName())) {
					VariableScope scope = localVariableDeclaration.getScope();
					if(scope.subsumes(fragment.getLocationInfo()) && fragment.getVariables().contains(declaration.getVariableName())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean usesVariable(AbstractCodeFragment fragment, VariableDeclaration declaration) {
		List<String> variables = fragment.getVariables();
		return variables.contains(declaration.getVariableName()) ||
				(declaration.isAttribute() && variables.contains("this." + declaration.getVariableName()));
	}

	public static Set<AbstractCodeMapping> findReturnReferences(Set<AbstractCodeMapping> mappings) {
		Set<AbstractCodeMapping> references = new LinkedHashSet<AbstractCodeMapping>();
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.RETURN_STATEMENT) &&
					mapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.RETURN_STATEMENT)) {
				references.add(mapping);
			}
		}
		return references;
	}

	public static Set<AbstractCodeMapping> findReferences(VariableDeclaration declaration1, VariableDeclaration declaration2, List<UMLOperationBodyMapper> operationBodyMapperList) {
		Set<AbstractCodeMapping> references = new LinkedHashSet<AbstractCodeMapping>();
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			references.addAll(findReferences(declaration1, declaration2, mapper.getMappings()));
		}
		return references;
	}
}