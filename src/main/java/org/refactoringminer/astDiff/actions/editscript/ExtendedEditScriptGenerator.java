package org.refactoringminer.astDiff.actions.editscript;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.tree.TreeContext;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;

import java.util.Map;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public interface ExtendedEditScriptGenerator {
	/**
	 * Compute and return the edit script for the provided mappings.
	 * @param mappings the mappings to compute the edit script for (see {@link ExtendedMultiMappingStore})
	 * @param parentContextMap the parent context map
	 * @param childContextMap the child context map
	 * @return The generated Edit script
	 */
	EditScript computeActions(ExtendedMultiMappingStore mappings, Map<String, TreeContext> parentContextMap, Map<String, TreeContext> childContextMap);


	/**
	 * Compute and return the edit script for the provided mappings considering there is no inter-file mappings
	 * @param mappings the mappings to compute the edit script for (see {@link ExtendedMultiMappingStore})
	 * @return The generated Edit script
	 */
	default EditScript computeActions(ExtendedMultiMappingStore mappings){
		return computeActions(mappings, null, null);
	}
}
