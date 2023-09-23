package gr.uom.java.xmi.decomposition;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class VariableReferenceExtractor {

	public static Set<AbstractCodeMapping> findReferences(VariableDeclaration declaration1, VariableDeclaration declaration2, Set<AbstractCodeMapping> mappings,
			UMLAbstractClassDiff classDiff, UMLModelDiff modelDiff) {
		Set<AbstractCodeMapping> references = new LinkedHashSet<AbstractCodeMapping>();
		Set<AbstractCodeFragment> statementsUsingVariable1 = declaration1.getStatementsInScopeUsingVariable();
		Set<AbstractCodeFragment> statementsUsingVariable2 = declaration2.getStatementsInScopeUsingVariable();
		for(AbstractCodeMapping mapping : mappings) {
			AbstractCodeFragment fragment1 = mapping.getFragment1();
			AbstractCodeFragment fragment2 = mapping.getFragment2();
			if(statementsUsingVariable1.contains(fragment1) && statementsUsingVariable2.contains(fragment2)) {
				references.add(mapping);
			}
			AbstractCall invocation1 = fragment1.invocationCoveringEntireFragment();
			AbstractCall invocation2 = fragment2.invocationCoveringEntireFragment();
			if(invocation1 != null && invocation2 != null) {
				//add recursive calls to the mappings
				if(invocation1.matchesOperation(mapping.getOperation1(), mapping.getOperation1(), classDiff, modelDiff) &&
						invocation2.matchesOperation(mapping.getOperation2(), mapping.getOperation2(), classDiff, modelDiff)) {
					if(statementsUsingVariable1.contains(fragment1) && statementsUsingVariable2.contains(fragment2)) {
						references.add(mapping);
					}
				}
			}
		}
		return references;
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

	public static Set<AbstractCodeMapping> findReferences(VariableDeclaration declaration1, VariableDeclaration declaration2, List<UMLOperationBodyMapper> operationBodyMapperList,
			UMLAbstractClassDiff classDiff, UMLModelDiff modelDiff) {
		Set<AbstractCodeMapping> references = new LinkedHashSet<AbstractCodeMapping>();
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			references.addAll(findReferences(declaration1, declaration2, mapper.getMappings(), classDiff, modelDiff));
		}
		return references;
	}
}