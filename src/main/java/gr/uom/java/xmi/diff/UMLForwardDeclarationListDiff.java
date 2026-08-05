package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLForwardDeclaration;

public class UMLForwardDeclarationListDiff {
	private Set<UMLForwardDeclaration> removedDeclarations;
	private Set<UMLForwardDeclaration> addedDeclarations;
	private Set<Pair<UMLForwardDeclaration, UMLForwardDeclaration>> commonDeclarations;

	public UMLForwardDeclarationListDiff(List<UMLForwardDeclaration> oldDeclarations, List<UMLForwardDeclaration> newDeclarations) {
		this.commonDeclarations = new LinkedHashSet<>();
		Set<UMLForwardDeclaration> oldDeclarationSet = new LinkedHashSet<>(oldDeclarations);
		Set<UMLForwardDeclaration> newDeclarationSet = new LinkedHashSet<>(newDeclarations);
		Set<UMLForwardDeclaration> intersection = new LinkedHashSet<>();
		intersection.addAll(oldDeclarationSet);
		intersection.retainAll(newDeclarationSet);
		for(UMLForwardDeclaration declaration : intersection) {
			UMLForwardDeclaration oldImport = oldDeclarations.get(oldDeclarations.indexOf(declaration));
			UMLForwardDeclaration newImport = newDeclarations.get(newDeclarations.indexOf(declaration));
			Pair<UMLForwardDeclaration, UMLForwardDeclaration> pair = Pair.of(oldImport, newImport);
			commonDeclarations.add(pair);
		}
		oldDeclarationSet.removeAll(intersection);
		this.removedDeclarations = oldDeclarationSet;
		newDeclarationSet.removeAll(intersection);
		this.addedDeclarations = newDeclarationSet;
	}

	public Set<UMLForwardDeclaration> getRemovedDeclarations() {
		return removedDeclarations;
	}

	public Set<UMLForwardDeclaration> getAddedDeclarations() {
		return addedDeclarations;
	}

	public Set<Pair<UMLForwardDeclaration, UMLForwardDeclaration>> getCommonDeclarations() {
		return commonDeclarations;
	}
}
