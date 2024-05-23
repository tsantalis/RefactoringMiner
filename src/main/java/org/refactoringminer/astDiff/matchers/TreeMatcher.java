package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public interface TreeMatcher {
	void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore);
	default ExtendedMultiMappingStore matchAndMake(Tree srcTree, Tree dstTree) {
		ExtendedMultiMappingStore mappingStore = new ExtendedMultiMappingStore(srcTree, dstTree);
		match(srcTree, dstTree, mappingStore);
		return mappingStore;
	}
}
