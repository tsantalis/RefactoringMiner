package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLAbstractModuleDirective;

public class UMLModuleDirectiveListDiff {
	private Set<UMLAbstractModuleDirective> removedDirectives;
	private Set<UMLAbstractModuleDirective> addedDirectives;
	private Set<Pair<UMLAbstractModuleDirective, UMLAbstractModuleDirective>> commonDirectives;
	private Set<Pair<UMLAbstractModuleDirective, UMLAbstractModuleDirective>> changedDirectives;
	
	public UMLModuleDirectiveListDiff(List<UMLAbstractModuleDirective> oldDirectives, List<UMLAbstractModuleDirective> newDirectives) {
		this.removedDirectives = new LinkedHashSet<UMLAbstractModuleDirective>(oldDirectives);
		this.addedDirectives = new LinkedHashSet<UMLAbstractModuleDirective>(newDirectives);
		this.commonDirectives = new LinkedHashSet<Pair<UMLAbstractModuleDirective,UMLAbstractModuleDirective>>();
		this.changedDirectives = new LinkedHashSet<Pair<UMLAbstractModuleDirective,UMLAbstractModuleDirective>>();
		for(UMLAbstractModuleDirective oldDirective : oldDirectives) {
			int index = newDirectives.indexOf(oldDirective);
			if(index != -1) {
				UMLAbstractModuleDirective newDirective = newDirectives.get(index);
				if(oldDirective.equalProperies(newDirective)) {
					commonDirectives.add(Pair.of(oldDirective, newDirective));
				}
				else {
					changedDirectives.add(Pair.of(oldDirective, newDirective));
				}
				removedDirectives.remove(oldDirective);
				addedDirectives.remove(newDirective);
			}
		}
		//handle directives with changes in names
		List<UMLAbstractModuleDirective> remainingRemoved = new ArrayList<>(this.removedDirectives);
		List<UMLAbstractModuleDirective> remainingAdded = new ArrayList<>(this.addedDirectives);
		if(remainingRemoved.size() == remainingAdded.size()) {
			for(UMLAbstractModuleDirective oldDirective : remainingRemoved) {
				List<String> tokens1 = Arrays.asList(oldDirective.getName().getString().split("\\."));
				int j=0;
				int maxIntersectionSize = 0;
				int maxIntersectionIndex = -1;
				for(UMLAbstractModuleDirective newDirective : remainingAdded) {
					List<String> tokens2 = Arrays.asList(newDirective.getName().getString().split("\\."));
					Set<String> intersection = new LinkedHashSet<>(tokens1);
					intersection.retainAll(tokens2);
					if(intersection.size() > maxIntersectionSize) {
						maxIntersectionIndex = j;
						maxIntersectionSize = intersection.size();
					}
					j++;
				}
				if(maxIntersectionSize > 1 && maxIntersectionIndex != -1) {
					UMLAbstractModuleDirective newDirective = remainingAdded.get(maxIntersectionIndex);
					changedDirectives.add(Pair.of(oldDirective, newDirective));
					removedDirectives.remove(oldDirective);
					addedDirectives.remove(newDirective);
				}
			}
		}
	}

	public Set<UMLAbstractModuleDirective> getRemovedDirectives() {
		return removedDirectives;
	}

	public Set<UMLAbstractModuleDirective> getAddedDirectives() {
		return addedDirectives;
	}

	public Set<Pair<UMLAbstractModuleDirective, UMLAbstractModuleDirective>> getCommonDirectives() {
		return commonDirectives;
	}

	public Set<Pair<UMLAbstractModuleDirective, UMLAbstractModuleDirective>> getChangedDirectives() {
		return changedDirectives;
	}
}
