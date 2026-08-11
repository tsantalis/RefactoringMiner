package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLProblemDeclaration;

public class UMLProblemDeclarationListDiff {
	private Set<UMLProblemDeclaration> removedDeclarations;
	private Set<UMLProblemDeclaration> addedDeclarations;
	private Set<Pair<UMLProblemDeclaration, UMLProblemDeclaration>> commonDeclarations;

	public UMLProblemDeclarationListDiff(List<UMLProblemDeclaration> oldDeclarations, List<UMLProblemDeclaration> newDeclarations) {
		this.commonDeclarations = new LinkedHashSet<>();
		Set<UMLProblemDeclaration> oldDeclarationSet = new LinkedHashSet<>(oldDeclarations);
		Set<UMLProblemDeclaration> newDeclarationSet = new LinkedHashSet<>(newDeclarations);
		Set<UMLProblemDeclaration> intersection = new LinkedHashSet<>();
		intersection.addAll(oldDeclarationSet);
		intersection.retainAll(newDeclarationSet);
		for(UMLProblemDeclaration declaration : intersection) {
			UMLProblemDeclaration oldDeclaration = oldDeclarations.get(oldDeclarations.indexOf(declaration));
			UMLProblemDeclaration newDeclaration = newDeclarations.get(newDeclarations.indexOf(declaration));
			Pair<UMLProblemDeclaration, UMLProblemDeclaration> pair = Pair.of(oldDeclaration, newDeclaration);
			commonDeclarations.add(pair);
		}
		oldDeclarationSet.removeAll(intersection);
		this.removedDeclarations = oldDeclarationSet;
		newDeclarationSet.removeAll(intersection);
		this.addedDeclarations = newDeclarationSet;
	}

	public Set<UMLProblemDeclaration> getRemovedDeclarations() {
		return removedDeclarations;
	}

	public Set<UMLProblemDeclaration> getAddedDeclarations() {
		return addedDeclarations;
	}

	public Set<Pair<UMLProblemDeclaration, UMLProblemDeclaration>> getCommonDeclarations() {
		return commonDeclarations;
	}
}
