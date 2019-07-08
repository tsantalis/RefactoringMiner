package gr.uom.java.xmi.decomposition;

import java.util.LinkedHashSet;
import java.util.Set;

public class VariableReferenceExtractor {

	public static Set<AbstractCodeMapping> findReferences(VariableDeclaration declaration1, VariableDeclaration declaration2, Set<AbstractCodeMapping> mappings) {
		Set<AbstractCodeMapping> references = new LinkedHashSet<AbstractCodeMapping>();
		VariableScope scope1 = declaration1.getScope();
		VariableScope scope2 = declaration2.getScope();
		for(AbstractCodeMapping mapping : mappings) {
			AbstractCodeFragment fragment1 = mapping.getFragment1();
			AbstractCodeFragment fragment2 = mapping.getFragment2();
			if(scope1.subsumes(fragment1.getLocationInfo()) && scope2.subsumes(fragment2.getLocationInfo()) &&
					fragment1.getVariables().contains(declaration1.getVariableName()) &&
					fragment2.getVariables().contains(declaration2.getVariableName())) {
				references.add(mapping);
			}
		}
		return references;
	}
}