package gr.uom.java.xmi.decomposition.replacement;

import java.util.LinkedHashSet;
import java.util.Set;

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
}
