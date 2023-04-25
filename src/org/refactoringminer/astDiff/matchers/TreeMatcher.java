package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.LeafMapping;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public interface TreeMatcher {
	void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore);
}
