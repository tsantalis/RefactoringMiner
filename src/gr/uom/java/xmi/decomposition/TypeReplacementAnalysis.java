package gr.uom.java.xmi.decomposition;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.diff.ChangeVariableTypeRefactoring;

public class TypeReplacementAnalysis {
	private Set<AbstractCodeMapping> mappings;
	private Set<ChangeVariableTypeRefactoring> changedTypes = new LinkedHashSet<ChangeVariableTypeRefactoring>();

	public TypeReplacementAnalysis(Set<AbstractCodeMapping> mappings) {
		this.mappings = mappings;
		findTypeChanges();
	}

	public Set<ChangeVariableTypeRefactoring> getChangedTypes() {
		return changedTypes;
	}

	private void findTypeChanges() {
		for(AbstractCodeMapping mapping : mappings) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.TYPE)) {
					List<VariableDeclaration> declarations1 = mapping.getFragment1().getVariableDeclarations();
					List<VariableDeclaration> declarations2 = mapping.getFragment2().getVariableDeclarations();
					for(VariableDeclaration declaration1 : declarations1) {
						for(VariableDeclaration declaration2 : declarations2) {
							if(declaration1.getVariableName().equals(declaration2.getVariableName()) &&
									!declaration1.getType().equals(declaration2.getType())) {
								ChangeVariableTypeRefactoring ref = new ChangeVariableTypeRefactoring(declaration1, declaration2, mapping.getOperation1(), mapping.getOperation2(),
										findReferences(declaration1, declaration2));
								changedTypes.add(ref);
								break;
							}
						}
					}
				}
			}
		}
	}
	
	private Set<AbstractCodeMapping> findReferences(VariableDeclaration declaration1, VariableDeclaration declaration2) {
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
