package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public class UMLImportListDiff {
	private Set<String> removedImports;
	private Set<String> addedImports;
	private Set<String> commonImports;
	private Set<Pair<String, String>> changedImports;
	
	public UMLImportListDiff(List<String> oldImports, List<String> newImports) {
		this.changedImports = new LinkedHashSet<>();
		Set<String> oldImportSet = new LinkedHashSet<>(oldImports);
		Set<String> newImportSet = new LinkedHashSet<>(newImports);
		Set<String> intersection = new LinkedHashSet<>();
		intersection.addAll(oldImportSet);
		intersection.retainAll(newImportSet);
		this.commonImports = intersection;
		oldImportSet.removeAll(intersection);
		this.removedImports = oldImportSet;
		newImportSet.removeAll(intersection);
		this.addedImports = newImportSet;
	}

	public void findImportChanges(String nameBefore, String nameAfter) {
		if(removedImports.contains(nameBefore) && addedImports.contains(nameAfter)) {
			Pair<String, String> pair = Pair.of(nameBefore, nameAfter);
			changedImports.add(pair);
			removedImports.remove(nameBefore);
			addedImports.remove(nameAfter);
		}
		//TODO handle static imports
	}

	public Set<String> getRemovedImports() {
		return removedImports;
	}

	public Set<String> getAddedImports() {
		return addedImports;
	}

	public Set<String> getCommonImports() {
		return commonImports;
	}

	public Set<Pair<String, String>> getChangedImports() {
		return changedImports;
	}
}
