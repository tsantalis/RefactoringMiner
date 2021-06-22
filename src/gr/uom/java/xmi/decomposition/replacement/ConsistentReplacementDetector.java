package gr.uom.java.xmi.decomposition.replacement;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import gr.uom.java.xmi.diff.CandidateAttributeRefactoring;

public class ConsistentReplacementDetector {

	private static <T extends Replacement> Set<T> inconsistentRenames(
			Set<T> currentRenames, T newRename) {
		Set<T> inconsistentRenames = new LinkedHashSet<T>();
		for(T rename : currentRenames) {
			if(rename.getBefore().equals(newRename.getBefore()) && !rename.getAfter().equals(newRename.getAfter())) {
				inconsistentRenames.add(rename);
			}
			else if(!rename.getBefore().equals(newRename.getBefore()) && rename.getAfter().equals(newRename.getAfter())) {
				inconsistentRenames.add(rename);
			}
		}
		return inconsistentRenames;
	}

	public static <T extends Replacement> void updateRenames(
			Set<T> allConsistentRenames,
			Set<T> allInconsistentRenames,
			Set<T> renames) {
		for(T newRename : renames) {
			Set<T> inconsistentRenames = inconsistentRenames(allConsistentRenames, newRename);
			if(inconsistentRenames.isEmpty()) {
				allConsistentRenames.add(newRename);
			}
			else {
				allInconsistentRenames.addAll(inconsistentRenames);
				allInconsistentRenames.add(newRename);
			}
		}
	}


	public static <T extends Replacement> void updateRenames(
			Set<T> allConsistentRenames,
			Set<T> allInconsistentRenames,
			Set<T> renames,
			Map<String, Set<String>> aliasedAttributesInOriginalClass,
			Map<String, Set<String>> aliasedAttributesInNextClass,
			Map<Replacement, Set<CandidateAttributeRefactoring>> renameMap) {
		for(T newRename : renames) {
			Set<T> inconsistentRenames = inconsistentRenames(allConsistentRenames, newRename);
			if(inconsistentRenames.size() > 0) {
				validInconsistencies(inconsistentRenames, newRename, renameMap);
			}
			filter(inconsistentRenames, aliasedAttributesInOriginalClass, aliasedAttributesInNextClass);
			if(inconsistentRenames.isEmpty()) {
				allConsistentRenames.add(newRename);
			}
			else {
				allInconsistentRenames.addAll(inconsistentRenames);
				allInconsistentRenames.add(newRename);
			}
		}
	}

	private static <T extends Replacement> void validInconsistencies(Set<T> inconsistentRenames, T newRename, Map<Replacement, Set<CandidateAttributeRefactoring>> renameMap) {
		if(involvesOnlyAttributes(newRename, renameMap)) {
			Set<T> toBeRemoved = new LinkedHashSet<T>();
			for(T rename : inconsistentRenames) {
				if(!involvesOnlyAttributes(rename, renameMap)) {
					toBeRemoved.add(rename);
				}
			}
			inconsistentRenames.removeAll(toBeRemoved);
		}
		else {
			inconsistentRenames.clear();
		}
	}

	private static <T extends Replacement> boolean involvesOnlyAttributes(T rename, Map<Replacement, Set<CandidateAttributeRefactoring>> renameMap) {
		Set<CandidateAttributeRefactoring> newCandidates = renameMap.get(rename);
		int candidatesWithBothAttributes = 0;
		for(CandidateAttributeRefactoring newCandidate : newCandidates) {
			if(newCandidate.getOriginalVariableDeclaration() == null && newCandidate.getRenamedVariableDeclaration() == null) {
				candidatesWithBothAttributes++;
			}
		}
		return candidatesWithBothAttributes == newCandidates.size();
	}

	private static <T extends Replacement> Set<T> filter(Set<T> inconsistentRenames,
			Map<String, Set<String>> aliasedAttributesInOriginalClass,
			Map<String, Set<String>> aliasedAttributesInNextClass) {
		Set<T> renamesToBeRemoved = new LinkedHashSet<T>();
		for(String key : aliasedAttributesInOriginalClass.keySet()) {
			Set<String> aliasedAttributes = aliasedAttributesInOriginalClass.get(key);
			for(T r : inconsistentRenames) {
				if(aliasedAttributes.contains(r.getBefore())) {
					renamesToBeRemoved.add(r);
				}
			}
		}
		for(String key : aliasedAttributesInNextClass.keySet()) {
			Set<String> aliasedAttributes = aliasedAttributesInNextClass.get(key);
			for(T r : inconsistentRenames) {
				if(aliasedAttributes.contains(r.getAfter())) {
					renamesToBeRemoved.add(r);
				}
			}
		}
		inconsistentRenames.removeAll(renamesToBeRemoved);
		return inconsistentRenames;
	}
}
