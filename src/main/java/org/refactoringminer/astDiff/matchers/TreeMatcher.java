package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.utils.Constants;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public interface TreeMatcher {
	void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore);
	default ExtendedMultiMappingStore matchAndMake(Tree srcTree, Tree dstTree, Constants LANG1, Constants LANG2) {
		ExtendedMultiMappingStore mappingStore = new ExtendedMultiMappingStore(srcTree, dstTree, LANG1, LANG2);
		match(srcTree, dstTree, mappingStore);
		return mappingStore;
	}
}
