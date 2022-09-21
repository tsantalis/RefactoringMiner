package gr.uom.java.xmi.diff;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLImport;

public class UMLImportListDiff {
	private Set<UMLImport> removedImports;
	private Set<UMLImport> addedImports;
	private Set<UMLImport> commonImports;
	private Set<Pair<UMLImport, UMLImport>> changedImports;
	private Map<Set<UMLImport>, UMLImport> groupedImports;
	private Map<UMLImport, Set<UMLImport>> unGroupedImports;
	
	public UMLImportListDiff(List<UMLImport> oldImports, List<UMLImport> newImports) {
		this.changedImports = new LinkedHashSet<>();
		Set<UMLImport> oldImportSet = new LinkedHashSet<>(oldImports);
		Set<UMLImport> newImportSet = new LinkedHashSet<>(newImports);
		Set<UMLImport> intersection = new LinkedHashSet<>();
		intersection.addAll(oldImportSet);
		intersection.retainAll(newImportSet);
		this.commonImports = intersection;
		oldImportSet.removeAll(intersection);
		this.removedImports = oldImportSet;
		newImportSet.removeAll(intersection);
		this.addedImports = newImportSet;
		
		this.groupedImports = new HashMap<>();
		Map<String, Set<UMLImport>> groupedRemovedImportsByPrefix = groupImportsByPrefix(this.removedImports);
		for(String key : groupedRemovedImportsByPrefix.keySet()) {
			UMLImport matchingImport = findMatchingImport(addedImports, key);
			if(matchingImport != null) {
				Set<UMLImport> value = groupedRemovedImportsByPrefix.get(key);
				this.groupedImports.put(value, matchingImport);
				this.addedImports.remove(matchingImport);
				this.removedImports.removeAll(value);
			}
		}
		
		this.unGroupedImports = new HashMap<>();
		Map<String, Set<UMLImport>> groupedAddedImportsByPrefix = groupImportsByPrefix(this.addedImports);
		for(String key : groupedAddedImportsByPrefix.keySet()) {
			UMLImport matchingImport = findMatchingImport(removedImports, key);
			if(matchingImport != null) {
				Set<UMLImport> value = groupedAddedImportsByPrefix.get(key);
				this.unGroupedImports.put(matchingImport, value);
				this.removedImports.remove(matchingImport);
				this.addedImports.removeAll(value);
			}
		}
	}

	private UMLImport findMatchingImport(Set<UMLImport> imports, String name) {
		for(UMLImport imported : imports) {
			if(imported.getName().equals(name)) {
				return imported;
			}
		}
		return null;
	}

	private Map<String, Set<UMLImport>> groupImportsByPrefix(Set<UMLImport> imports) {
		Map<String, Set<UMLImport>> groupedImportsByPrefix = new HashMap<>();
		for(UMLImport imported : imports) {
			String importString = imported.getName();
			if(importString.contains(".")) {
				String prefix = importString.substring(0, importString.lastIndexOf("."));
				if(groupedImportsByPrefix.containsKey(prefix)) {
					groupedImportsByPrefix.get(prefix).add(imported);
				}
				else {
					Set<UMLImport> set = new LinkedHashSet<>();
					set.add(imported);
					groupedImportsByPrefix.put(prefix, set);
				}
			}
		}
		return groupedImportsByPrefix;
	}

	public void findImportChanges(String nameBefore, String nameAfter) {
		UMLImport removedImport = findMatchingImport(removedImports, nameBefore);
		UMLImport addedImport = findMatchingImport(addedImports, nameAfter);
		if(removedImport != null && addedImport != null) {
			Pair<UMLImport, UMLImport> pair = Pair.of(removedImport, addedImport);
			changedImports.add(pair);
			removedImports.remove(removedImport);
			addedImports.remove(addedImport);
		}
		Set<UMLImport> matchedRemovedStaticImports = new LinkedHashSet<>();
		for(UMLImport removed : removedImports) {
			if(removed.getName().startsWith(nameBefore + ".")) {
				matchedRemovedStaticImports.add(removed);
			}
		}
		Set<UMLImport> matchedAddedStaticImports = new LinkedHashSet<>();
		for(UMLImport added : addedImports) {
			if(added.getName().startsWith(nameAfter + ".")) {
				matchedAddedStaticImports.add(added);
			}
		}
		for(UMLImport removed : matchedRemovedStaticImports) {
			for(UMLImport added : matchedAddedStaticImports) {
				String suffix1 = removed.getName().substring(nameBefore.length());
				String suffix2 = added.getName().substring(nameAfter.length());
				if(suffix1.equals(suffix2)) {
					Pair<UMLImport, UMLImport> pair = Pair.of(removed, added);
					changedImports.add(pair);
					removedImports.remove(removed);
					addedImports.remove(added);
					break;
				}
			}
		}
	}

	public Set<UMLImport> getRemovedImports() {
		return removedImports;
	}

	public Set<UMLImport> getAddedImports() {
		return addedImports;
	}

	public Set<UMLImport> getCommonImports() {
		return commonImports;
	}

	public Set<Pair<UMLImport, UMLImport>> getChangedImports() {
		return changedImports;
	}

	public Map<Set<UMLImport>, UMLImport> getGroupedImports() {
		return groupedImports;
	}

	public Map<UMLImport, Set<UMLImport>> getUnGroupedImports() {
		return unGroupedImports;
	}
}
