package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.SimilarityMetrics;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.utils.SequenceAlgorithms;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;

import java.util.*;

import static org.refactoringminer.astDiff.matchers.LeafMatcher.extracted;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class BasicTreeMatcher implements TreeMatcher {

	@Override
	public void match(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
		basicMatcher(src, dst, mappingStore);
	}
	@Override
	public void match(Tree src, Tree dst, AbstractCodeFragment abstractCodeFragment1, AbstractCodeFragment abstractCodeFragment2, ExtendedMultiMappingStore mappingStore) {
		basicMatcher(src, dst, mappingStore);
	}


	private void basicMatcher(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
		try {
			MappingStore match = extracted(src, dst);
			mappingStore.add(match);
		}
		catch (Exception exception)
		{
			System.out.println(exception.getMessage());
		}
	}
}
