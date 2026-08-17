package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLProblemDeclaration;

public class UMLProblemDeclarationListDiff {
	private Set<UMLProblemDeclaration> removedDeclarations;
	private Set<UMLProblemDeclaration> addedDeclarations;
	private Set<Pair<UMLProblemDeclaration, UMLProblemDeclaration>> commonDeclarations;
	private Set<Pair<UMLProblemDeclaration, UMLProblemDeclaration>> changedDeclarations;

	public UMLProblemDeclarationListDiff(List<UMLProblemDeclaration> oldDeclarations, List<UMLProblemDeclaration> newDeclarations) {
		this.commonDeclarations = new LinkedHashSet<>();
		this.changedDeclarations = new LinkedHashSet<>();
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
		newDeclarationSet.removeAll(intersection);
		Set<UMLProblemDeclaration> oldDeclarationsToBeRemoved = new LinkedHashSet<>();
		Set<UMLProblemDeclaration> newDeclarationsToBeRemoved = new LinkedHashSet<>();
		if(oldDeclarationSet.size() <= newDeclarationSet.size()) {
			for(UMLProblemDeclaration oldDeclaration : oldDeclarationSet) {
				Stream<String> lineStream1 = oldDeclaration.getFullText().lines();
				Set<String> set1 = lineStream1.collect(Collectors.toSet());
				TreeMap<Integer, List<UMLProblemDeclaration>> map = new TreeMap<>();
				for(UMLProblemDeclaration newDeclaration : newDeclarationSet) {
					Stream<String> lineStream2 = newDeclaration.getFullText().lines();
					Set<String> set2 = lineStream2.collect(Collectors.toSet());
					set2.retainAll(set1);
					int size = set2.size();
					if(map.containsKey(size)) {
						map.get(size).add(newDeclaration);
					}
					else {
						List<UMLProblemDeclaration> declarations = new ArrayList<UMLProblemDeclaration>();
						declarations.add(newDeclaration);
						map.put(size, declarations);
					}
				}
				if(!map.isEmpty() && map.lastEntry().getValue().size() == 1) {
					oldDeclarationsToBeRemoved.add(oldDeclaration);
					UMLProblemDeclaration newDeclaration = map.lastEntry().getValue().get(0);
					newDeclarationsToBeRemoved.add(newDeclaration);
					Pair<UMLProblemDeclaration, UMLProblemDeclaration> pair = Pair.of(oldDeclaration, newDeclaration);
					changedDeclarations.add(pair);
					newDeclarationSet.remove(newDeclaration);
				}
			}
		}
		else {
			for(UMLProblemDeclaration newDeclaration : newDeclarationSet) {
				Stream<String> lineStream2 = newDeclaration.getFullText().lines();
				Set<String> set2 = lineStream2.collect(Collectors.toSet());
				TreeMap<Integer, List<UMLProblemDeclaration>> map = new TreeMap<>();
				for(UMLProblemDeclaration oldDeclaration : oldDeclarationSet) {
					Stream<String> lineStream1 = oldDeclaration.getFullText().lines();
					Set<String> set1 = lineStream1.collect(Collectors.toSet());
					set1.retainAll(set2);
					int size = set1.size();
					if(map.containsKey(size)) {
						map.get(size).add(oldDeclaration);
					}
					else {
						List<UMLProblemDeclaration> declarations = new ArrayList<UMLProblemDeclaration>();
						declarations.add(oldDeclaration);
						map.put(size, declarations);
					}
				}
				if(!map.isEmpty() && map.lastEntry().getValue().size() == 1) {
					UMLProblemDeclaration oldDeclaration = map.lastEntry().getValue().get(0);
					oldDeclarationsToBeRemoved.add(oldDeclaration);
					newDeclarationsToBeRemoved.add(newDeclaration);
					Pair<UMLProblemDeclaration, UMLProblemDeclaration> pair = Pair.of(oldDeclaration, newDeclaration);
					changedDeclarations.add(pair);
					oldDeclarationSet.remove(oldDeclaration);
				}
			}
		}
		oldDeclarationSet.removeAll(oldDeclarationsToBeRemoved);
		newDeclarationSet.removeAll(newDeclarationsToBeRemoved);
		this.removedDeclarations = oldDeclarationSet;
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

	public Set<Pair<UMLProblemDeclaration, UMLProblemDeclaration>> getChangedDeclarations() {
		return changedDeclarations;
	}
}
