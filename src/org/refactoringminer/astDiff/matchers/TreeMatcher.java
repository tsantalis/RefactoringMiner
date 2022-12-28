package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractStatement;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public interface TreeMatcher {
	void match(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore);
	default void match(Tree src, Tree dst, AbstractStatement statement1, AbstractStatement statement2, ExtendedMultiMappingStore mappingStore) {}
}
