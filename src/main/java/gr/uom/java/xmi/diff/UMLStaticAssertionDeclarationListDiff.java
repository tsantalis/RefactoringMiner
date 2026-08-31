package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLStaticAssertionDeclaration;

public class UMLStaticAssertionDeclarationListDiff {
	private Set<UMLStaticAssertionDeclaration> removedDeclarations;
	private Set<UMLStaticAssertionDeclaration> addedDeclarations;
	private Set<Pair<UMLStaticAssertionDeclaration, UMLStaticAssertionDeclaration>> commonDeclarations;
	
	public UMLStaticAssertionDeclarationListDiff(List<UMLStaticAssertionDeclaration> oldDeclarations, List<UMLStaticAssertionDeclaration> newDeclarations) {
		this.commonDeclarations = new LinkedHashSet<>();
		Set<UMLStaticAssertionDeclaration> oldDeclarationSet = new LinkedHashSet<>(oldDeclarations);
		Set<UMLStaticAssertionDeclaration> newDeclarationSet = new LinkedHashSet<>(newDeclarations);
		Set<UMLStaticAssertionDeclaration> intersection = new LinkedHashSet<>();
		intersection.addAll(oldDeclarationSet);
		intersection.retainAll(newDeclarationSet);
		for(UMLStaticAssertionDeclaration declaration : intersection) {
			UMLStaticAssertionDeclaration oldDeclaration = oldDeclarations.get(oldDeclarations.indexOf(declaration));
			UMLStaticAssertionDeclaration newDeclaration = newDeclarations.get(newDeclarations.indexOf(declaration));
			Pair<UMLStaticAssertionDeclaration, UMLStaticAssertionDeclaration> pair = Pair.of(oldDeclaration, newDeclaration);
			commonDeclarations.add(pair);
		}
		oldDeclarationSet.removeAll(intersection);
		newDeclarationSet.removeAll(intersection);
		this.removedDeclarations = oldDeclarationSet;
		this.addedDeclarations = newDeclarationSet;
	}

	public Set<UMLStaticAssertionDeclaration> getRemovedDeclarations() {
		return removedDeclarations;
	}

	public Set<UMLStaticAssertionDeclaration> getAddedDeclarations() {
		return addedDeclarations;
	}

	public Set<Pair<UMLStaticAssertionDeclaration, UMLStaticAssertionDeclaration>> getCommonDeclarations() {
		return commonDeclarations;
	}
}
