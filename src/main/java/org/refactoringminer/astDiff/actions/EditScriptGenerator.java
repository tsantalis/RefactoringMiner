package org.refactoringminer.astDiff.actions;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.tree.TreeContext;
import org.refactoringminer.astDiff.matchers.ExtendedMultiMappingStore;

import java.util.Map;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public interface EditScriptGenerator {
	/**
	 * Compute and return the edit script for the provided mappings.
	 * @param mappings the mappings to compute the edit script for (see {@link ExtendedMultiMappingStore})
	 * @param parentContextMap the parent context map
	 * @param childContextMap the child context map
	 * @return The generated Edit script
	 */
	EditScript computeActions(ExtendedMultiMappingStore mappings, Map<String, TreeContext> parentContextMap, Map<String, TreeContext> childContextMap);
}
